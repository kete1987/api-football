package es.com.kete1987.apifootball.library;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import es.com.kete1987.apifootball.library.common.model.Page;
import es.com.kete1987.apifootball.library.common.model.Paging;
import es.com.kete1987.apifootball.library.common.model.RateLimit;
import es.com.kete1987.apifootball.library.common.query.Query;
import es.com.kete1987.apifootball.library.common.util.ApiFootballException;
import es.com.kete1987.apifootball.library.common.util.Constants;
import es.com.kete1987.apifootball.library.common.util.LenientNumberAdapters;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Shared plumbing of every sub-API: authentication, HTTP execution, envelope parsing, error
 * detection, pagination and rate-limit tracking.
 *
 * <p>Every API-Football response is an envelope
 * {@code {get, parameters, errors, results, paging, response}}. The helpers here unwrap it, so
 * sub-APIs only deal with the type found inside {@code response}.
 */
abstract class ApiFootballBase {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Integer.class, LenientNumberAdapters.INTEGER)
            .registerTypeAdapter(int.class, LenientNumberAdapters.INTEGER)
            .registerTypeAdapter(Double.class, LenientNumberAdapters.DOUBLE)
            .registerTypeAdapter(double.class, LenientNumberAdapters.DOUBLE)
            .registerTypeAdapter(Long.class, LenientNumberAdapters.LONG)
            .registerTypeAdapter(long.class, LenientNumberAdapters.LONG)
            .create();

    final OkHttpClient httpClient;
    final String baseUrl;
    final RateLimitTracker rateLimitTracker;

    ApiFootballBase(OkHttpClient httpClient, String baseUrl, RateLimitTracker rateLimitTracker) {
        this.httpClient = httpClient;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        this.rateLimitTracker = rateLimitTracker;
    }

    /** Maximum number of body characters included in an exception message. */
    static final int MAX_BODY_IN_MESSAGE = 500;

    /**
     * Builds the HTTP client of the public constructors, which sends {@code apiKey} in the
     * {@code x-apisports-key} header of every request.
     *
     * @throws IllegalArgumentException when the key is {@code null} or blank
     */
    static OkHttpClient buildHttpClient(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("apiKey must not be null or blank");
        }
        return new OkHttpClient.Builder()
                .addInterceptor(chain -> chain.proceed(
                        chain.request().newBuilder()
                                .header(Constants.API_KEY_HEADER, apiKey)
                                .build()))
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /** The single Gson instance of the library. Never build another one. */
    static Gson gson() {
        return GSON;
    }

    // ------------------------------------------------------------------ URL building

    /** Starts a URL for an endpoint path such as {@code "fixtures/events"}. */
    HttpUrl.Builder url(String path) {
        HttpUrl parsed = HttpUrl.parse(baseUrl + path);
        if (parsed == null) throw new IllegalArgumentException("Invalid URL: " + baseUrl + path);
        return parsed.newBuilder();
    }

    /** Starts a URL for an endpoint path carrying every parameter set in {@code query}. */
    HttpUrl.Builder url(String path, Query<?> query) {
        HttpUrl.Builder builder = url(path);
        if (query != null) {
            query.toParams().forEach(builder::addQueryParameter);
        }
        return builder;
    }

    /** Adds a query parameter unless its value is {@code null}. */
    static HttpUrl.Builder param(HttpUrl.Builder builder, String name, Object value) {
        if (value != null) {
            builder.addQueryParameter(name, String.valueOf(value));
        }
        return builder;
    }

    // ------------------------------------------------------------------ execution

    /**
     * Performs the request and returns the raw body, after recording the rate-limit headers and
     * turning non-2xx responses and non-empty {@code errors} into an {@link ApiFootballException}.
     * An empty body (e.g. HTTP 204) or a body that is not a JSON object is also reported as an
     * {@link ApiFootballException}.
     */
    String execute(HttpUrl url) throws IOException, ApiFootballException {
        return executeEnvelope(url).body;
    }

    private Envelope executeEnvelope(HttpUrl url) throws IOException, ApiFootballException {
        Request request = new Request.Builder().url(url).build();
        try (Response response = httpClient.newCall(request).execute()) {
            rateLimitTracker.track(response);
            String body = response.body() != null ? response.body().string() : "";
            JsonObject root = parse(body);
            Map<String, String> errors = root != null ? errorsOf(root) : Collections.emptyMap();
            if (!response.isSuccessful() || !errors.isEmpty()) {
                throw new ApiFootballException(response.code(), describe(response.code(), body, errors), errors);
            }
            if (root == null) {
                throw new ApiFootballException(response.code(),
                        response.code() + " - unexpected body: " + truncate(body), null);
            }
            return new Envelope(response.code(), body, root);
        }
    }

    private static JsonObject parse(String body) {
        if (body == null || body.isEmpty()) return null;
        try {
            JsonElement element = JsonParser.parseString(body);
            return element.isJsonObject() ? element.getAsJsonObject() : null;
        } catch (JsonParseException e) {
            return null;
        }
    }

    /**
     * Normalises the {@code errors} member, which is an empty array when there is nothing to
     * report and an object ({@code {"field": "message"}}) or a non-empty array otherwise.
     */
    static Map<String, String> errorsOf(JsonElement errors) {
        return errorsOfMember(errors);
    }

    /**
     * Errors of a response body: its {@code errors} member or, for a body that is not an envelope
     * (e.g. {@code {"message": "..."}} with HTTP 499/500), its {@code message} under the key
     * {@code "message"}.
     */
    private static Map<String, String> errorsOf(JsonObject root) {
        if (!root.has("errors") && !root.has("response")) {
            Map<String, String> result = new LinkedHashMap<>();
            JsonElement message = root.get("message");
            if (message != null && !message.isJsonNull()) result.put("message", text(message));
            return result;
        }
        return errorsOfMember(root.get("errors"));
    }

    private static Map<String, String> errorsOfMember(JsonElement errors) {
        Map<String, String> result = new LinkedHashMap<>();
        if (errors == null || errors.isJsonNull()) return result;
        if (errors.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : errors.getAsJsonObject().entrySet()) {
                result.put(entry.getKey(), text(entry.getValue()));
            }
        } else if (errors.isJsonArray()) {
            JsonArray array = errors.getAsJsonArray();
            for (int i = 0; i < array.size(); i++) {
                result.put(String.valueOf(i), text(array.get(i)));
            }
        } else {
            result.put("error", text(errors));
        }
        return result;
    }

    private static String text(JsonElement element) {
        return element.isJsonPrimitive() ? element.getAsString() : element.toString();
    }

    private static String describe(int code, String body, Map<String, String> errors) {
        if (errors.isEmpty()) return code + " - " + truncate(body);
        StringBuilder sb = new StringBuilder().append(code).append(" - ");
        boolean first = true;
        for (Map.Entry<String, String> entry : errors.entrySet()) {
            if (!first) sb.append("; ");
            sb.append(entry.getKey()).append(": ").append(entry.getValue());
            first = false;
        }
        return sb.toString();
    }

    /** Cuts a body to {@value #MAX_BODY_IN_MESSAGE} characters for exception messages. */
    static String truncate(String body) {
        if (body == null || body.length() <= MAX_BODY_IN_MESSAGE) return body;
        return body.substring(0, MAX_BODY_IN_MESSAGE) + "... (" + body.length() + " characters)";
    }

    /** Parsing step of a typed helper; it may fail with a Gson runtime exception. */
    private interface Mapping<R> {
        R map();
    }

    /**
     * Runs a Gson mapping of the {@code response} member and turns parse failures (a JSON type the
     * model cannot hold, an out-of-range number...) into an {@link ApiFootballException}, so the
     * public methods never leak Gson runtime exceptions.
     */
    private static <R> R mapped(Envelope envelope, Class<?> type, Mapping<R> mapping) throws ApiFootballException {
        try {
            return mapping.map();
        } catch (JsonParseException | IllegalStateException | NumberFormatException e) {
            throw new ApiFootballException(envelope.code, envelope.code + " - could not map the response to "
                    + type.getSimpleName() + ": " + e.getMessage(), null, e);
        }
    }

    // ------------------------------------------------------------------ typed helpers

    /** Fetches an endpoint whose {@code response} is an array, with a single request. */
    <T> List<T> list(HttpUrl.Builder url, Class<T> type) throws IOException, ApiFootballException {
        Envelope envelope = executeEnvelope(url.build());
        return mapped(envelope, type, () -> toList(envelope.root.get("response"), type));
    }

    /**
     * Fetches an endpoint whose {@code response} is a single object (e.g. {@code status},
     * {@code teams/statistics}). Returns {@code null} when the API answers with an empty array,
     * which is how it reports "no data" on those endpoints.
     */
    <T> T object(HttpUrl.Builder url, Class<T> type) throws IOException, ApiFootballException {
        Envelope envelope = executeEnvelope(url.build());
        return mapped(envelope, type, () -> {
            JsonElement response = envelope.root.get("response");
            if (response == null || response.isJsonNull()) return null;
            if (response.isJsonArray()) {
                JsonArray array = response.getAsJsonArray();
                return array.size() == 0 ? null : gson().fromJson(array.get(0), type);
            }
            return gson().fromJson(response, type);
        });
    }

    /** Fetches an array endpoint and returns its first element, or {@code null} when empty. */
    <T> T first(HttpUrl.Builder url, Class<T> type) throws IOException, ApiFootballException {
        List<T> items = list(url, type);
        return items.isEmpty() ? null : items.get(0);
    }

    /**
     * Fetches one page (1-based) of a paginated endpoint: exactly one request.
     *
     * @throws IllegalArgumentException when {@code page} is lower than 1
     */
    <T> Page<T> page(HttpUrl.Builder url, Class<T> type, int page) throws IOException, ApiFootballException {
        if (page < 1) throw new IllegalArgumentException("page must be >= 1, was " + page);
        HttpUrl target = url.build().newBuilder().setQueryParameter("page", String.valueOf(page)).build();
        Envelope envelope = executeEnvelope(target);
        JsonObject root = envelope.root;
        return mapped(envelope, type, () -> {
            Paging paging = root.has("paging") ? gson().fromJson(root.get("paging"), Paging.class) : null;
            int results = root.has("results") && root.get("results").isJsonPrimitive()
                    ? root.get("results").getAsInt() : 0;
            return new Page<>(toList(root.get("response"), type), paging, results);
        });
    }

    /**
     * Walks a paginated endpoint from page 1 and returns every item. <b>Each page is one request
     * against the daily quota</b>, so the walk is always capped: it stops after {@code maxPages}
     * pages, returning what was gathered. There is deliberately no "unlimited" value.
     *
     * <p>If a later page fails, the exception propagates and the pages already fetched are lost
     * (their requests still counted against the quota).
     *
     * @throws IllegalArgumentException if {@code maxPages} is lower than 1
     */
    <T> List<T> allPages(HttpUrl.Builder url, Class<T> type, int maxPages) throws IOException, ApiFootballException {
        if (maxPages < 1) throw new IllegalArgumentException("maxPages must be >= 1");
        List<T> all = new ArrayList<>();
        int current = 1;
        while (true) {
            Page<T> result = page(url, type, current);
            all.addAll(result.getItems());
            // An empty page means we ran past the end (the API answers that without an error).
            if (result.getItems().isEmpty() || !result.hasMore() || current >= maxPages) break;
            // Trust the page the API says it served; fall back to the local counter.
            int served = result.getPaging().getCurrent();
            current = Math.max(served, current) + 1;
        }
        return all;
    }

    private static <T> List<T> toList(JsonElement response, Class<T> type) {
        if (response == null || response.isJsonNull()) return new ArrayList<>();
        if (!response.isJsonArray()) {
            List<T> single = new ArrayList<>();
            single.add(gson().fromJson(response, type));
            return single;
        }
        List<T> items = gson().fromJson(response, TypeToken.getParameterized(List.class, type).getType());
        return items != null ? items : new ArrayList<>();
    }

    private static final class Envelope {
        final int code;
        final String body;
        final JsonObject root;

        Envelope(int code, String body, JsonObject root) {
            this.code = code;
            this.body = body;
            this.root = root;
        }
    }

    // ------------------------------------------------------------------ rate limits

    /**
     * Rate-limit headers of the most recent response made through this client (or through any
     * sub-API sharing its tracker), or {@code null} before the first request.
     */
    public RateLimit getRateLimit() {
        return rateLimitTracker.last();
    }

    /** {@code x-ratelimit-requests-limit} of the last response: requests allocated per day. */
    public Integer getDailyRequestsLimit() {
        RateLimit last = rateLimitTracker.last();
        return last != null ? last.getDailyLimit() : null;
    }

    /** {@code x-ratelimit-requests-remaining} of the last response: requests left today. */
    public Integer getDailyRequestsRemaining() {
        RateLimit last = rateLimitTracker.last();
        return last != null ? last.getDailyRemaining() : null;
    }

    /** {@code X-RateLimit-Limit} of the last response: maximum calls per minute. */
    public Integer getMinuteRequestsLimit() {
        RateLimit last = rateLimitTracker.last();
        return last != null ? last.getMinuteLimit() : null;
    }

    /** {@code X-RateLimit-Remaining} of the last response: calls left in the current minute. */
    public Integer getMinuteRequestsRemaining() {
        RateLimit last = rateLimitTracker.last();
        return last != null ? last.getMinuteRemaining() : null;
    }

    /**
     * Number of HTTP responses this client has received since it was created, error responses
     * included. Requests that never got a response (timeouts, connection failures) and retries
     * OkHttp performs transparently are not counted.
     */
    public long getRequestCount() {
        return rateLimitTracker.requestCount();
    }
}

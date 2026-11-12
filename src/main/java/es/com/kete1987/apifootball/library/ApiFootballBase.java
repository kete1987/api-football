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

    private static final Gson GSON = new GsonBuilder().create();

    final OkHttpClient httpClient;
    final String baseUrl;
    final RateLimitTracker rateLimitTracker;

    ApiFootballBase(OkHttpClient httpClient, String baseUrl, RateLimitTracker rateLimitTracker) {
        this.httpClient = httpClient;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        this.rateLimitTracker = rateLimitTracker;
    }

    static OkHttpClient buildHttpClient(String apiKey) {
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
            Map<String, String> errors = root != null ? errorsOf(root.get("errors")) : Collections.emptyMap();
            if (!response.isSuccessful() || !errors.isEmpty()) {
                throw new ApiFootballException(response.code(), describe(response.code(), body, errors), errors);
            }
            if (root == null) {
                throw new ApiFootballException(response.code(), response.code() + " - unexpected body: " + body, null);
            }
            return new Envelope(body, root);
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
        if (errors.isEmpty()) return code + " - " + body;
        StringBuilder sb = new StringBuilder().append(code).append(" - ");
        boolean first = true;
        for (Map.Entry<String, String> entry : errors.entrySet()) {
            if (!first) sb.append("; ");
            sb.append(entry.getKey()).append(": ").append(entry.getValue());
            first = false;
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ typed helpers

    /** Fetches an endpoint whose {@code response} is an array, with a single request. */
    <T> List<T> list(HttpUrl.Builder url, Class<T> type) throws IOException, ApiFootballException {
        return toList(executeEnvelope(url.build()).root.get("response"), type);
    }

    /**
     * Fetches an endpoint whose {@code response} is a single object (e.g. {@code status},
     * {@code teams/statistics}). Returns {@code null} when the API answers with an empty array,
     * which is how it reports "no data" on those endpoints.
     */
    <T> T object(HttpUrl.Builder url, Class<T> type) throws IOException, ApiFootballException {
        JsonElement response = executeEnvelope(url.build()).root.get("response");
        if (response == null || response.isJsonNull()) return null;
        if (response.isJsonArray()) {
            JsonArray array = response.getAsJsonArray();
            return array.size() == 0 ? null : gson().fromJson(array.get(0), type);
        }
        return gson().fromJson(response, type);
    }

    /** Fetches an array endpoint and returns its first element, or {@code null} when empty. */
    <T> T first(HttpUrl.Builder url, Class<T> type) throws IOException, ApiFootballException {
        List<T> items = list(url, type);
        return items.isEmpty() ? null : items.get(0);
    }

    /** Fetches one page (1-based) of a paginated endpoint: exactly one request. */
    <T> Page<T> page(HttpUrl.Builder url, Class<T> type, int page) throws IOException, ApiFootballException {
        HttpUrl target = url.build().newBuilder().setQueryParameter("page", String.valueOf(Math.max(page, 1))).build();
        JsonObject root = executeEnvelope(target).root;
        Paging paging = root.has("paging") ? gson().fromJson(root.get("paging"), Paging.class) : null;
        int results = root.has("results") && root.get("results").isJsonPrimitive() ? root.get("results").getAsInt() : 0;
        return new Page<>(toList(root.get("response"), type), paging, results);
    }

    /**
     * Walks a paginated endpoint from page 1 and returns every item. <b>Each page is one request
     * against the daily quota</b>, so {@code maxPages} caps the walk: a positive value stops after
     * that many pages (returning what was gathered), a non-positive one means "all pages".
     */
    <T> List<T> allPages(HttpUrl.Builder url, Class<T> type, int maxPages) throws IOException, ApiFootballException {
        List<T> all = new ArrayList<>();
        int current = 1;
        while (true) {
            Page<T> result = page(url, type, current);
            all.addAll(result.getItems());
            if (!result.hasMore() || (maxPages > 0 && current >= maxPages)) break;
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
        final String body;
        final JsonObject root;

        Envelope(String body, JsonObject root) {
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

    /** Number of HTTP requests this client has sent since it was created. */
    public long getRequestCount() {
        return rateLimitTracker.requestCount();
    }
}

package es.com.kete1987.apifootball.library.common.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Thrown when API-Football rejects a request.
 *
 * <p>The API reports most problems (wrong parameter, plan restriction, exhausted quota...) with an
 * HTTP 200 whose body carries a non-empty {@code errors} member, so this exception covers both
 * non-2xx responses and logical errors. {@link #getErrors()} exposes the {@code errors} entries
 * keyed as the API returned them (e.g. {@code "season" -> "The Season field is required."}).
 */
public class ApiFootballException extends Exception {
    private static final long serialVersionUID = 1L;

    private final int httpCode;
    private final transient Map<String, String> errors;

    public ApiFootballException(int httpCode, String message, Map<String, String> errors) {
        super(message);
        this.httpCode = httpCode;
        this.errors = errors == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(errors));
    }

    /** HTTP status code of the response (often 200 for logical errors). */
    public int getHttpCode() {
        return httpCode;
    }

    /** The {@code errors} entries of the response body; empty when the body carried none. */
    public Map<String, String> getErrors() {
        return errors == null ? Collections.emptyMap() : errors;
    }

    @Override
    public String toString() {
        return getMessage();
    }
}

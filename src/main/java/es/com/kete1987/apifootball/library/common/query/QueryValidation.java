package es.com.kete1987.apifootball.library.common.query;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Client-side checks for parameters the API accepts silently when malformed: an invalid
 * {@code date} overflows into another day ({@code 2026-13-45} returns 2027-02-14) and an invalid
 * {@code timezone} is replaced by UTC. Every method throws {@link IllegalArgumentException} for an
 * invalid value, {@code null} included; query setters handle {@code null} themselves (it removes
 * the parameter) before calling these checks.
 */
public final class QueryValidation {

    /** Maximum number of ids of the plural id parameters ({@code ids}, {@code players}, {@code coachs}). */
    public static final int MAX_IDS = 20;

    private QueryValidation() {
        // utility class
    }

    /**
     * Returns the date unchanged when it is a real {@code YYYY-MM-DD} calendar date.
     *
     * @param name parameter name, used in the error message
     * @throws IllegalArgumentException when the value is {@code null}, malformed or not a real date
     */
    public static String date(String name, String value) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
        try {
            if (LocalDate.parse(value).toString().equals(value)) return value;
        } catch (DateTimeParseException e) {
            // reported below
        }
        throw new IllegalArgumentException(
                "Invalid " + name + " '" + value + "': expected a real date in YYYY-MM-DD format");
    }

    /**
     * Returns the date as {@code YYYY-MM-DD}.
     *
     * @throws IllegalArgumentException when the value is {@code null}
     */
    public static String date(String name, LocalDate value) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
        return value.toString();
    }

    /**
     * Returns the timezone unchanged when it has the form of an API-Football timezone: exactly
     * {@code "UTC"}, or a region id such as {@code "Europe/Madrid"} that {@link ZoneId#of(String)}
     * knows. Offsets and prefixed offsets ({@code "+02:00"}, {@code "Z"}, {@code "GMT+1"},
     * {@code "UTC+1"}, {@code "Etc/GMT+1"}) are rejected.
     *
     * <p>The valid names are those returned by the {@code /timezone} endpoint
     * ({@code LeaguesApi.getTimezones()}); the API silently falls back to UTC on an unknown one,
     * which is why the value is checked here. The check uses the JDK zone database rather than a
     * bundled list, so a name both know is accepted.
     *
     * @throws IllegalArgumentException when the value is {@code null} or not a valid timezone
     */
    public static String timezone(String value) {
        if (value == null) {
            throw new IllegalArgumentException("timezone must not be null");
        }
        if ("UTC".equals(value)) return value;
        if (!value.contains("/")) {
            throw new IllegalArgumentException("Invalid timezone '" + value
                    + "': expected \"UTC\" or a region such as \"Europe/Madrid\" (see /timezone)");
        }
        try {
            ZoneId zone = ZoneId.of(value);
            // "Etc/GMT+1" is a region id in the JDK but an offset in disguise
            if (zone instanceof ZoneOffset || !zone.getId().equals(value) || value.matches(".*(GMT|UTC|UT)[+-].*")) {
                throw new IllegalArgumentException("Invalid timezone '" + value + "': offsets are not accepted");
            }
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("Unknown timezone '" + value + "' (see /timezone)", e);
        }
        return value;
    }

    /**
     * Returns the value when it lies within {@code [min, max]}.
     *
     * @throws IllegalArgumentException otherwise
     */
    public static int range(String name, int value, int min, int max) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(name + " must be between " + min + " and " + max + ", was " + value);
        }
        return value;
    }

    /**
     * Joins 1 to {@value #MAX_IDS} ids with {@code -}, the format of the plural id parameters.
     *
     * @param name parameter name, used in the error message
     * @throws IllegalArgumentException when the collection is {@code null}, empty, larger than
     *                                  {@value #MAX_IDS} or contains {@code null}
     */
    public static String ids(String name, Collection<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException(name + " must contain at least one id");
        }
        if (ids.size() > MAX_IDS) {
            throw new IllegalArgumentException("At most " + MAX_IDS + " " + name + " are allowed, was " + ids.size());
        }
        // contains(null) may throw on collections that reject null queries; iterate instead
        for (Integer id : ids) {
            if (id == null) throw new IllegalArgumentException(name + " must not contain null");
        }
        return ids.stream().map(String::valueOf).collect(Collectors.joining("-"));
    }

    /**
     * Validates a pre-joined {@code id-id-id} string (digits and dashes, 1 to {@value #MAX_IDS} ids).
     *
     * @throws IllegalArgumentException when the value is {@code null}, malformed or has too many ids
     */
    public static String ids(String name, String joined) {
        if (joined == null || !joined.matches("\\d+(-\\d+)*")) {
            throw new IllegalArgumentException(name + " must look like 'id-id-id', was '" + joined + "'");
        }
        if (joined.split("-").length > MAX_IDS) {
            throw new IllegalArgumentException("At most " + MAX_IDS + " " + name + " are allowed");
        }
        return joined;
    }
}

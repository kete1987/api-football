package es.com.kete1987.apifootball.library.common.query;

import java.text.Normalizer;
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

    /** Minimum length of a {@code search} term (catalog: "min 3"). */
    public static final int SEARCH_MIN_LENGTH = 3;

    /** Minimum length of the {@code search} term of {@code /players} (catalog section 11.24: "min 4 chars"). */
    public static final int PLAYERS_SEARCH_MIN_LENGTH = 4;

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

    /**
     * Validates a {@code search} term with the general minimum of {@value #SEARCH_MIN_LENGTH}
     * characters; see {@link #search(String, int)}.
     *
     * @throws IllegalArgumentException when the API would reject the term
     */
    public static String search(String value) {
        return search(value, SEARCH_MIN_LENGTH);
    }

    /**
     * Returns the {@code search} term unchanged when API-Football accepts it, so a request that
     * would only come back as an error (and still cost quota) is never sent.
     *
     * <p>The rule, taken from the API's own error messages: the term may only contain ASCII letters
     * ({@code A-Z}, {@code a-z}), digits and spaces ("The Search field may only contain
     * alpha-numeric characters and spaces.", observed for {@code Leganés}), and must have at least
     * {@code minLength} characters ("The Search field must be at least 3 characters in length.").
     * The minimum is 3 everywhere except {@code /players} (4, see {@link #PLAYERS_SEARCH_MIN_LENGTH}).
     * Leading and trailing spaces do not count towards the length (conservative: the API's
     * treatment of them is not documented). Digits are allowed: the API names its rule
     * "alpha-numeric".
     *
     * <p>To search a name with accents or punctuation use {@link #toSearchTerm(String)} first.
     *
     * @param value     the term; {@code null} is rejected here (query setters treat {@code null}
     *                  as "remove the parameter" before calling this)
     * @param minLength minimum length required by the endpoint
     * @throws IllegalArgumentException when the term is {@code null}, too short or has other characters
     */
    public static String search(String value, int minLength) {
        if (value == null) {
            throw new IllegalArgumentException("search must not be null");
        }
        if (!value.matches("[A-Za-z0-9 ]*")) {
            throw new IllegalArgumentException("Invalid search '" + value
                    + "': only letters A-Z, digits and spaces are accepted (no accents or symbols); "
                    + "see QueryValidation.toSearchTerm");
        }
        if (value.trim().length() < minLength) {
            throw new IllegalArgumentException("Invalid search '" + value + "': at least " + minLength
                    + " characters are required");
        }
        return value;
    }

    /**
     * Turns arbitrary text into a term {@link #search(String, int)} accepts: diacritics are removed
     * ({@code "Leganés"} becomes {@code "Leganes"}), any other character the API rejects is
     * dropped, runs of spaces collapse into one and the ends are trimmed. This is an explicit
     * opt-in; the query setters never apply it silently. Characters with no ASCII decomposition
     * ({@code ø}, {@code ß}, {@code đ}) are dropped, so check the result still has the minimum
     * length. The result may be empty or too short; the setter will then reject it.
     *
     * @return the cleaned term, or an empty string for {@code null}
     */
    public static String toSearchTerm(String text) {
        if (text == null) return "";
        String decomposed = Normalizer.normalize(text, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}+", "").replaceAll("[^A-Za-z0-9 ]", "").replaceAll(" +", " ").trim();
    }
}

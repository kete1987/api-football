package es.com.kete1987.apifootball.library.fixtures.query;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Client-side checks for parameters the API accepts silently when malformed: an invalid
 * {@code date} overflows into another day ({@code 2026-13-45} returns 2027-02-14) and an invalid
 * {@code timezone} is replaced by UTC.
 */
final class QueryValidation {

    /** Maximum number of fixture ids of the {@code ids} parameter. */
    static final int MAX_IDS = 20;

    private QueryValidation() {
        // helpers only
    }

    /** Returns the date unchanged when it is a real {@code YYYY-MM-DD} calendar date. */
    static String date(String name, String value) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
        try {
            return LocalDate.parse(value).toString().equals(value) ? value : reject(name, value);
        } catch (DateTimeParseException e) {
            return reject(name, value);
        }
    }

    private static String reject(String name, String value) {
        throw new IllegalArgumentException(
                "Invalid " + name + " '" + value + "': expected a real date in YYYY-MM-DD format");
    }

    static String date(String name, LocalDate value) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
        return value.toString();
    }

    /** Returns the timezone unchanged when {@link ZoneId} knows it (e.g. {@code Europe/Madrid}). */
    static String timezone(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("timezone must not be blank");
        }
        try {
            ZoneId.of(value);
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("Unknown timezone '" + value + "'", e);
        }
        return value;
    }

    /** At most two digits: 1 to 99. */
    static int twoDigits(String name, int value) {
        if (value < 1 || value > 99) {
            throw new IllegalArgumentException(name + " must be between 1 and 99 (max 2 digits), was " + value);
        }
        return value;
    }

    /** Joins 1 to {@value #MAX_IDS} ids with {@code -}. */
    static String ids(Collection<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("ids must not be empty");
        }
        if (ids.size() > MAX_IDS) {
            throw new IllegalArgumentException("At most " + MAX_IDS + " ids are allowed, was " + ids.size());
        }
        if (ids.contains(null)) {
            throw new IllegalArgumentException("ids must not contain null");
        }
        return ids.stream().map(String::valueOf).collect(Collectors.joining("-"));
    }

    /** Validates a pre-joined {@code id-id-id} string (digits and dashes, at most {@value #MAX_IDS} ids). */
    static String ids(String joined) {
        if (joined == null || !joined.matches("\\d+(-\\d+)*")) {
            throw new IllegalArgumentException("ids must look like 'id-id-id', was '" + joined + "'");
        }
        if (joined.split("-").length > MAX_IDS) {
            throw new IllegalArgumentException("At most " + MAX_IDS + " ids are allowed");
        }
        return joined;
    }
}

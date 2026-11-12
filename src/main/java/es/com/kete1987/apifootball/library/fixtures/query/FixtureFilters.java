package es.com.kete1987.apifootball.library.fixtures.query;

import es.com.kete1987.apifootball.library.common.query.Query;

import java.time.LocalDate;

/**
 * Filters shared by {@code /fixtures} and {@code /fixtures/headtohead}. Dates are validated when set:
 * the API would silently turn {@code 2026-13-45} into another day, so a malformed value throws
 * {@link IllegalArgumentException} here instead.
 *
 * @param <Q> the concrete query type
 */
public abstract class FixtureFilters<Q extends FixtureFilters<Q>> extends Query<Q> {

    /** Day of the fixtures, {@code YYYY-MM-DD}; interpreted in {@link #timezone(String)} when given. */
    public Q date(String date) {
        return param("date", QueryValidation.date("date", date));
    }

    /** Day of the fixtures. */
    public Q date(LocalDate date) {
        return param("date", QueryValidation.date("date", date));
    }

    /** League id. Usually combined with {@link #season(int)}. */
    public Q league(int league) {
        return param("league", league);
    }

    /** Season as the 4-digit starting year ({@code 2025} for 2025-2026). */
    public Q season(int season) {
        return param("season", season);
    }

    /** The X last fixtures; at most two digits (1-99). */
    public Q last(int last) {
        return param("last", QueryValidation.twoDigits("last", last));
    }

    /** The X next fixtures; at most two digits (1-99). */
    public Q next(int next) {
        return param("next", QueryValidation.twoDigits("next", next));
    }

    /** Start of a date range, {@code YYYY-MM-DD}; use together with {@link #to(String)}. */
    public Q from(String from) {
        return param("from", QueryValidation.date("from", from));
    }

    /** Start of a date range; use together with {@link #to(LocalDate)}. */
    public Q from(LocalDate from) {
        return param("from", QueryValidation.date("from", from));
    }

    /** End of a date range, {@code YYYY-MM-DD}. */
    public Q to(String to) {
        return param("to", QueryValidation.date("to", to));
    }

    /** End of a date range. */
    public Q to(LocalDate to) {
        return param("to", QueryValidation.date("to", to));
    }

    /**
     * Status filter: one short code ({@code "NS"}) or several joined with {@code -}
     * ({@code "FT-AET-PEN"}); see {@code FixtureStatuses}.
     */
    public Q status(String status) {
        if (status == null || !status.trim().matches("[A-Za-z0-9]+(-[A-Za-z0-9]+)*")) {
            throw new IllegalArgumentException("status must be one or more codes joined by '-', e.g. FT-AET-PEN");
        }
        return param("status", status.trim());
    }

    /** Status filter with several short codes, joined with {@code -} (e.g. {@code "FT", "AET", "PEN"}). */
    public Q status(String... statuses) {
        if (statuses == null || statuses.length == 0) {
            throw new IllegalArgumentException("At least one status code is required");
        }
        return status(String.join("-", statuses));
    }

    /** Venue id. */
    public Q venue(int venue) {
        return param("venue", venue);
    }

    /**
     * IANA timezone such as {@code "Europe/Madrid"} (see {@code /timezone}). It changes the rendered
     * dates and the calendar day {@code date} refers to. An unknown name is rejected here because the
     * API would silently fall back to UTC.
     */
    public Q timezone(String timezone) {
        return param("timezone", QueryValidation.timezone(timezone));
    }
}

package es.com.kete1987.apifootball.library.fixtures.query;

import es.com.kete1987.apifootball.library.common.query.Query;
import es.com.kete1987.apifootball.library.common.query.QueryValidation;

import java.time.LocalDate;

/**
 * Filters shared by {@code /fixtures} and {@code /fixtures/headtohead}. Dates are validated when set:
 * the API would silently turn {@code 2026-13-45} into another day, so a malformed value throws
 * {@link IllegalArgumentException} here instead. Passing {@code null} to a setter removes the
 * parameter.
 *
 * @param <Q> the concrete query type
 */
public abstract class FixtureFilters<Q extends FixtureFilters<Q>> extends Query<Q> {

    /** Day of the fixtures, {@code YYYY-MM-DD}; interpreted in {@link #timezone(String)} when given. */
    public Q date(String date) {
        return param("date", date == null ? null : QueryValidation.date("date", date));
    }

    /** Day of the fixtures. */
    public Q date(LocalDate date) {
        return param("date", date == null ? null : QueryValidation.date("date", date));
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
        return param("last", QueryValidation.range("last", last, 1, 99));
    }

    /** The X next fixtures; at most two digits (1-99). */
    public Q next(int next) {
        return param("next", QueryValidation.range("next", next, 1, 99));
    }

    /** Start of a date range, {@code YYYY-MM-DD}; use together with {@link #to(String)}. */
    public Q from(String from) {
        return param("from", from == null ? null : QueryValidation.date("from", from));
    }

    /** Start of a date range; use together with {@link #to(LocalDate)}. */
    public Q from(LocalDate from) {
        return param("from", from == null ? null : QueryValidation.date("from", from));
    }

    /** End of a date range, {@code YYYY-MM-DD}. */
    public Q to(String to) {
        return param("to", to == null ? null : QueryValidation.date("to", to));
    }

    /** End of a date range. */
    public Q to(LocalDate to) {
        return param("to", to == null ? null : QueryValidation.date("to", to));
    }

    /**
     * Status filter: one short code ({@code "NS"}) or several joined with {@code -}
     * ({@code "FT-AET-PEN"}); see {@code FixtureStatuses}.
     */
    public Q status(String status) {
        if (status == null) return param("status", null);
        if (!status.trim().matches("[A-Za-z0-9]+(-[A-Za-z0-9]+)*")) {
            throw new IllegalArgumentException("status must be one or more codes joined by '-', e.g. FT-AET-PEN");
        }
        return param("status", status.trim());
    }

    /** Status filter with several short codes, joined with {@code -} (e.g. {@code "FT", "AET", "PEN"}). */
    public Q status(String... statuses) {
        if (statuses == null) return param("status", null);
        if (statuses.length == 0) {
            throw new IllegalArgumentException("At least one status code is required");
        }
        return status(String.join("-", statuses));
    }

    /** Venue id. */
    public Q venue(int venue) {
        return param("venue", venue);
    }

    /**
     * Timezone such as {@code "Europe/Madrid"}: one of the names returned by {@code /timezone}. It
     * changes the rendered dates and the calendar day {@code date} refers to. The value is checked
     * with {@link QueryValidation#timezone(String)} because the API silently falls back to UTC on an
     * unknown name.
     */
    public Q timezone(String timezone) {
        return param("timezone", timezone == null ? null : QueryValidation.timezone(timezone));
    }
}

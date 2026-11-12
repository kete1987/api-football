package es.com.kete1987.apifootball.library.odds.query;

import es.com.kete1987.apifootball.library.common.query.Query;
import es.com.kete1987.apifootball.library.common.query.QueryValidation;

import java.time.LocalDate;

/**
 * Parameters of {@code GET /odds} (pre-match odds). The {@code page} parameter is deliberately not
 * here: the API methods take it as an argument ({@code getOddsPage}) or walk it ({@code getAllOdds}).
 * The API requires at least one filter.
 */
public class OddsQuery extends Query<OddsQuery> {

    /** Fixture id. */
    public OddsQuery fixture(int fixture) {
        return param("fixture", fixture);
    }

    /** League id; use together with {@link #season(int)}. */
    public OddsQuery league(int league) {
        return param("league", league);
    }

    /** Season as a 4-digit starting year (YYYY). */
    public OddsQuery season(int season) {
        return param("season", season);
    }

    /**
     * Date as {@code YYYY-MM-DD}; {@code null} removes it. Validated here because the API silently
     * overflows an invalid date into another day.
     *
     * @throws IllegalArgumentException when the date is malformed or not a real calendar date
     */
    public OddsQuery date(String date) {
        return param("date", date == null ? null : QueryValidation.date("date", date));
    }

    /** Date, sent as {@code YYYY-MM-DD}; {@code null} removes it. */
    public OddsQuery date(LocalDate date) {
        return param("date", date == null ? null : QueryValidation.date("date", date));
    }

    /**
     * Timezone such as {@code "Europe/Madrid"} (a name returned by {@code /timezone}); {@code null}
     * removes it. Checked with {@link QueryValidation#timezone(String)} because the API silently
     * falls back to UTC on an unknown name.
     *
     * @throws IllegalArgumentException when the name is not a valid timezone
     */
    public OddsQuery timezone(String timezone) {
        return param("timezone", timezone == null ? null : QueryValidation.timezone(timezone));
    }

    /** Bookmaker id (see {@code /odds/bookmakers}). */
    public OddsQuery bookmaker(int bookmaker) {
        return param("bookmaker", bookmaker);
    }

    /** <b>Pre-match</b> bet id (see {@code /odds/bets}); NOT an in-play bet id. */
    public OddsQuery bet(int bet) {
        return param("bet", bet);
    }
}

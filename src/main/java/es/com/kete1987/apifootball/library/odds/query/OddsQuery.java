package es.com.kete1987.apifootball.library.odds.query;

import es.com.kete1987.apifootball.library.common.query.Query;

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

    /** Date as {@code YYYY-MM-DD}. Not validated by the API (invalid dates overflow silently). */
    public OddsQuery date(String date) {
        return param("date", date);
    }

    /** Date, sent as {@code YYYY-MM-DD}. */
    public OddsQuery date(LocalDate date) {
        return param("date", date);
    }

    /** IANA timezone name from {@code /timezone} (e.g. {@code Europe/Madrid}); invalid values silently become UTC. */
    public OddsQuery timezone(String timezone) {
        return param("timezone", timezone);
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

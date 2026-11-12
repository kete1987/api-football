package es.com.kete1987.apifootball.library.fixtures.query;

import es.com.kete1987.apifootball.library.common.query.Query;

import java.time.LocalDate;
import java.util.Collection;

/**
 * Parameters of {@code GET /injuries} (data from April 2021). <b>At least one parameter is
 * required</b> (the API answers with an error otherwise). All parameters can be combined;
 * {@code league}, {@code team} and {@code player} need {@code season}.
 */
public class InjuriesQuery extends Query<InjuriesQuery> {

    /** League id; requires {@link #season(int)}. */
    public InjuriesQuery league(int league) {
        return param("league", league);
    }

    /** Season as the 4-digit starting year; required with league, team and player. */
    public InjuriesQuery season(int season) {
        return param("season", season);
    }

    /** Fixture id. */
    public InjuriesQuery fixture(int fixture) {
        return param("fixture", fixture);
    }

    /** Team id; requires {@link #season(int)}. */
    public InjuriesQuery team(int team) {
        return param("team", team);
    }

    /** Player id; requires {@link #season(int)}. */
    public InjuriesQuery player(int player) {
        return param("player", player);
    }

    /** Day, {@code YYYY-MM-DD}. */
    public InjuriesQuery date(String date) {
        return param("date", QueryValidation.date("date", date));
    }

    /** Day. */
    public InjuriesQuery date(LocalDate date) {
        return param("date", QueryValidation.date("date", date));
    }

    /** Several fixture ids (1 to 20), sent as {@code id-id-id}. */
    public InjuriesQuery ids(Collection<Integer> ids) {
        return param("ids", QueryValidation.ids(ids));
    }

    /** Several fixture ids already joined as {@code "id-id-id"} (at most 20). */
    public InjuriesQuery ids(String ids) {
        return param("ids", QueryValidation.ids(ids));
    }

    /** IANA timezone such as {@code "Europe/Madrid"}; an unknown name is rejected (the API would fall back to UTC). */
    public InjuriesQuery timezone(String timezone) {
        return param("timezone", QueryValidation.timezone(timezone));
    }
}

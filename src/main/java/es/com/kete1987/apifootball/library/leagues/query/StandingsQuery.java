package es.com.kete1987.apifootball.library.leagues.query;

import es.com.kete1987.apifootball.library.common.query.Query;

/**
 * Parameters of {@code /standings}. {@code season} is required, and at least one of
 * {@code league} or {@code team} must be given.
 */
public class StandingsQuery extends Query<StandingsQuery> {

    /** League id (required unless {@link #team(int)} is set). */
    public StandingsQuery league(int league) {
        return param("league", league);
    }

    /** Season, four-digit year (required). */
    public StandingsQuery season(int season) {
        return param("season", season);
    }

    /** Team id (required unless {@link #league(int)} is set). */
    public StandingsQuery team(int team) {
        return param("team", team);
    }
}

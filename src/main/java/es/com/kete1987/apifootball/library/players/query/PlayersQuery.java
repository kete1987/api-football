package es.com.kete1987.apifootball.library.players.query;

import es.com.kete1987.apifootball.library.common.query.Query;

/**
 * Parameters of {@code /players} (paginated, 20 per page; the {@code page} parameter is supplied
 * to the page methods of {@code PlayersApi}). At least one of {@code id}, {@code team} or
 * {@code league} is needed, and {@code season} requires one of them.
 */
public class PlayersQuery extends Query<PlayersQuery> {

    /** Player id. */
    public PlayersQuery id(int id) {
        return param("id", id);
    }

    /** Team id. */
    public PlayersQuery team(int team) {
        return param("team", team);
    }

    /** League id. */
    public PlayersQuery league(int league) {
        return param("league", league);
    }

    /** Season, 4-digit start year (YYYY). Requires {@code id}, {@code league} or {@code team}. */
    public PlayersQuery season(int season) {
        return param("season", season);
    }

    /** Player name search, at least 4 characters. Requires {@code league} or {@code team}. */
    public PlayersQuery search(String search) {
        return param("search", search);
    }
}

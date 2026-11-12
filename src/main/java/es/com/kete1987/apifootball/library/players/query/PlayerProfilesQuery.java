package es.com.kete1987.apifootball.library.players.query;

import es.com.kete1987.apifootball.library.common.query.Query;

/**
 * Parameters of {@code /players/profiles} (paginated, 250 per page; 2,771 pages without filters).
 * The {@code page} parameter is supplied to the page methods of {@code PlayersApi}.
 */
public class PlayerProfilesQuery extends Query<PlayerProfilesQuery> {

    /** Player id. */
    public PlayerProfilesQuery player(int player) {
        return param("player", player);
    }

    /** Player last name search, at least 3 characters. */
    public PlayerProfilesQuery search(String search) {
        return param("search", search);
    }
}

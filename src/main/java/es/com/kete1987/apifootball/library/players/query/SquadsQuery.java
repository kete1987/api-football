package es.com.kete1987.apifootball.library.players.query;

import es.com.kete1987.apifootball.library.common.query.Query;

/** Parameters of {@code /players/squads}. At least one of {@code team} or {@code player} is required. */
public class SquadsQuery extends Query<SquadsQuery> {

    /** Team id: returns its current squad. */
    public SquadsQuery team(int team) {
        return param("team", team);
    }

    /** Player id: returns the teams (and their squads) the player belongs to. */
    public SquadsQuery player(int player) {
        return param("player", player);
    }
}

package es.com.kete1987.apifootball.library.fixtures.query;

import es.com.kete1987.apifootball.library.common.query.Query;

/** Optional filters of {@code GET /fixtures/players} (the {@code fixture} id is an API method argument). */
public class FixturePlayersQuery extends Query<FixturePlayersQuery> {

    /** Only the players of this team. */
    public FixturePlayersQuery team(int team) {
        return param("team", team);
    }
}

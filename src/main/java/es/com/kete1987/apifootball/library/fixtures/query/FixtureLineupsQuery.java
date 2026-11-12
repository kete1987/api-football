package es.com.kete1987.apifootball.library.fixtures.query;

import es.com.kete1987.apifootball.library.common.query.Query;

/** Optional filters of {@code GET /fixtures/lineups} (the {@code fixture} id is an API method argument). */
public class FixtureLineupsQuery extends Query<FixtureLineupsQuery> {

    /** Only the lineup of this team. */
    public FixtureLineupsQuery team(int team) {
        return param("team", team);
    }

    /** Only the lineup containing this player. */
    public FixtureLineupsQuery player(int player) {
        return param("player", player);
    }

    /** Lineup section, e.g. {@code "startXI"}. */
    public FixtureLineupsQuery type(String type) {
        return param("type", type);
    }
}

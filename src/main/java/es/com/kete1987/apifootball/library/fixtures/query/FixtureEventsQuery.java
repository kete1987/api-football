package es.com.kete1987.apifootball.library.fixtures.query;

import es.com.kete1987.apifootball.library.common.query.Query;

/** Optional filters of {@code GET /fixtures/events} (the {@code fixture} id is an API method argument). */
public class FixtureEventsQuery extends Query<FixtureEventsQuery> {

    /** Only events of this team. */
    public FixtureEventsQuery team(int team) {
        return param("team", team);
    }

    /** Only events of this player. */
    public FixtureEventsQuery player(int player) {
        return param("player", player);
    }

    /** Only this event type, lower case: {@code goal}, {@code card}, {@code subst} or {@code var} (see {@code EventTypes.FILTER_*}). */
    public FixtureEventsQuery type(String type) {
        return param("type", type);
    }
}

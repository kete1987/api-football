package es.com.kete1987.apifootball.library.fixtures.query;

import es.com.kete1987.apifootball.library.common.query.Query;

/** Optional filters of {@code GET /fixtures/statistics} (the {@code fixture} id is an API method argument). */
public class FixtureStatisticsQuery extends Query<FixtureStatisticsQuery> {

    /** Only this team. */
    public FixtureStatisticsQuery team(int team) {
        return param("team", team);
    }

    /** Only this statistic, by name, e.g. {@code "Total Shots"} (see {@code StatisticTypes}). */
    public FixtureStatisticsQuery type(String type) {
        return param("type", type);
    }

    /** {@code half=true} adds first- and second-half statistics (data from the 2024 season). */
    public FixtureStatisticsQuery half(boolean half) {
        return param("half", half);
    }
}

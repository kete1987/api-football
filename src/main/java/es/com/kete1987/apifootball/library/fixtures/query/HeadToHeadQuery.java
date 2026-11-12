package es.com.kete1987.apifootball.library.fixtures.query;

/**
 * Optional filters of {@code GET /fixtures/headtohead}: {@code date, league, season, last, next,
 * from, to, status, venue, timezone}, all inherited from {@link FixtureFilters}. The required
 * {@code h2h} parameter (the two team ids) is an argument of the API method.
 */
public class HeadToHeadQuery extends FixtureFilters<HeadToHeadQuery> {
}

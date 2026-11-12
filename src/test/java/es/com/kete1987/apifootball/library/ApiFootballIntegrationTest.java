package es.com.kete1987.apifootball.library;

import es.com.kete1987.apifootball.library.account.model.Status;
import es.com.kete1987.apifootball.library.common.util.ApiFootballException;
import es.com.kete1987.apifootball.library.fixtures.model.Fixture;
import es.com.kete1987.apifootball.library.leagues.model.League;
import es.com.kete1987.apifootball.library.leagues.model.Standings;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Hits the real API. Excluded from the default surefire run; runs only with
 * {@code mvn test -Pintegration} and skips itself unless {@code API_FOOTBALL_KEY} is set.
 *
 * <p><b>Every run spends real daily quota</b>: 4 counted requests (the {@code status} call is
 * free). Keep it that small.
 */
class ApiFootballIntegrationTest {

    private static final int LA_LIGA = 140;
    private static final int SEASON = 2025;
    private static final int FINISHED_FIXTURE = 1570443;

    private static ApiFootball api;

    @BeforeAll
    static void setUp() {
        String key = System.getenv("API_FOOTBALL_KEY");
        assumeTrue(key != null && !key.isEmpty(), "API_FOOTBALL_KEY not set; skipping integration tests");
        api = new ApiFootball(key);
    }

    @Test
    void status_reportsSubscriptionAndRateLimitHeaders() throws Exception {
        Status status = api.getStatus();

        assertNotNull(status.getSubscription().getPlan());
        assertTrue(status.getRequests().getLimitDay() > 0);
        assertNotNull(api.getRateLimit());
        assertEquals(status.getRequests().getLimitDay(), api.getDailyRequestsLimit());
        assertNotNull(api.getDailyRequestsRemaining());
        assertNotNull(api.getMinuteRequestsLimit());
        assertNotNull(api.getMinuteRequestsRemaining());
    }

    @Test
    void league_byId() throws Exception {
        League league = api.leagues().getLeagueById(LA_LIGA);

        assertEquals(LA_LIGA, league.getLeague().getId());
        assertFalse(league.getSeasons().isEmpty());
    }

    @Test
    void standings_ofAFinishedSeason() throws Exception {
        Standings standings = api.leagues().getStandings(LA_LIGA, SEASON);

        assertEquals(LA_LIGA, standings.getId());
        assertEquals(20, standings.getTable().size());
    }

    @Test
    void fixture_byId_embedsDetails() throws Exception {
        Fixture fixture = api.fixtures().getFixtureById(FINISHED_FIXTURE);

        assertEquals(FINISHED_FIXTURE, fixture.getFixture().getId());
        assertTrue(fixture.getFixture().getStatus().isFinished());
        assertFalse(fixture.getEvents().isEmpty());
        assertFalse(fixture.getLineups().isEmpty());
        assertFalse(fixture.getStatistics().isEmpty());
        assertFalse(fixture.getPlayers().isEmpty());
    }

    @Test
    void logicalError_isReportedAsException() {
        // The library does not validate the length of "search": this call simply provokes the
        // API's own error envelope (HTTP 200 with an "errors" object).
        ApiFootballException e = assertThrows(ApiFootballException.class,
                () -> api.teams().searchTeams("ma"));

        assertEquals(200, e.getHttpCode());
        assertTrue(e.getErrors().containsKey("search"));
    }
}

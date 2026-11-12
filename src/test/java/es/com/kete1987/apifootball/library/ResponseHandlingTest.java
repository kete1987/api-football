package es.com.kete1987.apifootball.library;

import com.google.gson.JsonSyntaxException;
import es.com.kete1987.apifootball.library.common.util.ApiFootballException;
import es.com.kete1987.apifootball.library.fixtures.model.Fixture;
import es.com.kete1987.apifootball.library.leagues.model.Standings;
import es.com.kete1987.apifootball.library.odds.model.OddsMapping;
import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Error and parsing contract of {@link ApiFootballBase}: public methods only throw
 * {@code IOException}, {@link ApiFootballException} and {@link IllegalArgumentException}.
 */
class ResponseHandlingTest extends BaseApiTest {

    private FixturesApi fixtures;
    private OddsApi odds;
    private PlayersApi players;
    private LeaguesApi leagues;

    @BeforeEach
    void setUp() {
        fixtures = new FixturesApi(client, baseUrl, tracker);
        odds = new OddsApi(client, baseUrl, tracker);
        players = new PlayersApi(client, baseUrl, tracker);
        leagues = new LeaguesApi(client, baseUrl, tracker);
    }

    private static String envelope(String response) {
        return "{\"get\":\"x\",\"parameters\":[],\"errors\":[],\"results\":1,"
                + "\"paging\":{\"current\":1,\"total\":1},\"response\":" + response + "}";
    }

    // ------------------------------------------------------------------ lenient numbers

    @Test
    void longField_acceptsNumberNumericStringAndEmptyString() throws Exception {
        enqueueBody(envelope("[{\"fixture\":{\"id\":1,\"timestamp\":\"\"}},"
                + "{\"fixture\":{\"id\":\"2\",\"timestamp\":\"1767225600\"}},"
                + "{\"fixture\":{\"id\":3,\"timestamp\":4102444800}}]"), 200);

        List<Fixture> result = fixtures.getFixturesByIds(List.of(1, 2, 3));

        assertNull(result.get(0).getFixture().getTimestamp());
        assertEquals(2, result.get(1).getFixture().getId());
        assertEquals(1767225600L, result.get(1).getFixture().getTimestamp());
        assertEquals(4102444800L, result.get(2).getFixture().getTimestamp());
    }

    @Test
    void longField_inOddsMappingAcceptsStrings() throws Exception {
        enqueueBody(envelope("[{\"league\":{\"id\":\"140\",\"season\":2025},"
                + "\"fixture\":{\"id\":1,\"date\":\"x\",\"timestamp\":\"\"},\"update\":\"u\"}]"), 200);

        OddsMapping mapping = odds.getOddsMappingPage(1).getItems().get(0);

        assertEquals(140, mapping.getLeague().getId());
        assertNull(mapping.getFixture().getTimestamp());
    }

    @Test
    void integerAdapter_rejectsOutOfRangeAndFractionalValues() {
        assertThrows(JsonSyntaxException.class,
                () -> ApiFootballBase.gson().fromJson("3000000000", Integer.class));
        assertThrows(JsonSyntaxException.class,
                () -> ApiFootballBase.gson().fromJson("\"-2147483649\"", Integer.class));
        assertThrows(JsonSyntaxException.class,
                () -> ApiFootballBase.gson().fromJson("1.5", Integer.class));
        assertThrows(JsonSyntaxException.class,
                () -> ApiFootballBase.gson().fromJson("1e30", Long.class));
        assertEquals(2147483647, ApiFootballBase.gson().fromJson("2147483647", Integer.class));
        assertEquals(5, ApiFootballBase.gson().fromJson("5.0", Integer.class));
        assertNull(ApiFootballBase.gson().fromJson("\" \"", Integer.class));
    }

    // ------------------------------------------------------------------ parse failures

    @Test
    void outOfRangeInteger_isWrappedInApiFootballException() {
        enqueueBody(envelope("[{\"fixture\":{\"id\":3000000000}}]"), 200);

        ApiFootballException e = assertThrows(ApiFootballException.class, () -> fixtures.getFixtureById(1));

        assertEquals(200, e.getHttpCode());
        assertInstanceOf(JsonSyntaxException.class, e.getCause());
        assertTrue(e.getMessage().contains("Fixture"), e.getMessage());
    }

    @Test
    void wrongJsonType_isWrappedInApiFootballException() {
        enqueueBody(envelope("[{\"fixture\":{\"id\":\"abc\"}}]"), 200);
        assertThrows(ApiFootballException.class, () -> fixtures.getFixtureById(1));

        enqueueBody(envelope("[{\"fixture\":[1,2]}]"), 200);
        assertThrows(ApiFootballException.class, () -> fixtures.getLiveFixtures());

        enqueueBody(envelope("\"text\""), 200);
        assertThrows(ApiFootballException.class, () -> new AccountApi(client, baseUrl, tracker).getStatus());
    }

    @Test
    void pageWithBrokenPaging_isWrappedInApiFootballException() {
        enqueueBody("{\"get\":\"x\",\"parameters\":[],\"errors\":[],\"results\":\"many\","
                + "\"paging\":{\"current\":1,\"total\":1},\"response\":[]}", 200);

        ApiFootballException e = assertThrows(ApiFootballException.class, () -> odds.getOddsMappingPage(1));

        assertInstanceOf(NumberFormatException.class, e.getCause());
    }

    // ------------------------------------------------------------------ error bodies

    @Test
    void http500WithMessage_isExposedInErrors() {
        enqueueBody("{\"message\":\"Something went wrong while fetching details. Try again later.\"}", 500);

        ApiFootballException e = assertThrows(ApiFootballException.class, () -> fixtures.getFixtureById(1));

        assertEquals(500, e.getHttpCode());
        assertEquals("Something went wrong while fetching details. Try again later.", e.getErrors().get("message"));
        assertTrue(e.getMessage().startsWith("500 - message: Something went wrong"));
    }

    @Test
    void http204WithoutBody_isAnApiFootballException() {
        server.enqueue(new MockResponse().setResponseCode(204));

        ApiFootballException e = assertThrows(ApiFootballException.class, () -> fixtures.getFixtureById(1));

        assertEquals(204, e.getHttpCode());
    }

    @Test
    void longBody_isTruncatedInTheMessage() {
        StringBuilder html = new StringBuilder("<html>");
        for (int i = 0; i < 2000; i++) html.append('x');
        enqueueBody(html.toString(), 502);

        ApiFootballException e = assertThrows(ApiFootballException.class, () -> fixtures.getFixtureById(1));

        assertTrue(e.getMessage().length() < 600, "message length " + e.getMessage().length());
        assertTrue(e.getMessage().contains("2006 characters"));
    }

    @Test
    void unknownEndpointFixture_isReportedWithItsError() throws Exception {
        enqueue("error_unknown_endpoint.json");

        ApiFootballException e = assertThrows(ApiFootballException.class, () -> leagues.getTimezones());

        assertEquals(200, e.getHttpCode());
        assertEquals("The Foo endpoint does not exist.", e.getErrors().get("endpoint"));
    }

    // ------------------------------------------------------------------ arguments

    @Test
    void pageBelowOne_isRejectedWithoutARequest() {
        assertThrows(IllegalArgumentException.class, () -> players.getPlayersByLeaguePage(140, 2025, 0));
        assertThrows(IllegalArgumentException.class, () -> odds.getOddsMappingPage(-1));
        assertThrows(IllegalArgumentException.class, () -> odds.getOddsByLeaguePage(140, 2025, 0));
        assertEquals(0, server.getRequestCount());
    }

    @Test
    void nullOrBlankApiKey_isRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ApiFootball(null));
        assertThrows(IllegalArgumentException.class, () -> new ApiFootball("  "));
        assertThrows(IllegalArgumentException.class, () -> new FixturesApi(""));
        assertThrows(IllegalArgumentException.class, () -> new AccountApi(null));
        assertThrows(IllegalArgumentException.class, () -> new LeaguesApi(" "));
        assertThrows(IllegalArgumentException.class, () -> new TeamsApi(null));
        assertThrows(IllegalArgumentException.class, () -> new PlayersApi(""));
        assertThrows(IllegalArgumentException.class, () -> new OddsApi("\t"));
    }

    // ------------------------------------------------------------------ models

    @Test
    void standingsWithoutTables_returnEmptyLists() throws Exception {
        enqueueBody(envelope("[{\"league\":{\"id\":140,\"season\":2025}}]"), 200);

        Standings standings = leagues.getStandings(140, 2025);

        assertTrue(standings.getTables().isEmpty());
        assertTrue(standings.getTable().isEmpty());
    }
}

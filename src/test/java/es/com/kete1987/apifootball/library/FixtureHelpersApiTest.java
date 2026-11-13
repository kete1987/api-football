package es.com.kete1987.apifootball.library;

import es.com.kete1987.apifootball.library.fixtures.model.Fixture;
import es.com.kete1987.apifootball.library.fixtures.query.FixturesQuery;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Convenience methods and client-side validation added to {@code FixturesApi}: path and query
 * assertions through the mock server, plus the proof that rejected calls send no request at all.
 */
class FixtureHelpersApiTest extends BaseApiTest {

    private FixturesApi api;

    @BeforeEach
    void setUp() {
        api = new FixturesApi(client, baseUrl, tracker);
    }

    private HttpUrl lastRequestUrl() throws Exception {
        RecordedRequest r = takeRequest();
        return r.getRequestUrl();
    }

    private static List<Integer> ids(int from, int count) {
        List<Integer> l = new ArrayList<>();
        for (int i = 0; i < count; i++) l.add(from + i);
        return l;
    }

    private void assertNoRequestSent() {
        assertEquals(0, server.getRequestCount());
    }

    // ------------------------------------------------------------------ league / team

    @Test
    void getFixturesByLeague_withDateRange() throws Exception {
        enqueue("fixtures_empty.json");
        api.getFixturesByLeague(140, 2026, LocalDate.of(2026, 11, 7), LocalDate.of(2026, 11, 9));
        HttpUrl u = lastRequestUrl();
        assertEquals("/fixtures", u.encodedPath());
        assertEquals("140", u.queryParameter("league"));
        assertEquals("2026", u.queryParameter("season"));
        assertEquals("2026-11-07", u.queryParameter("from"));
        assertEquals("2026-11-09", u.queryParameter("to"));
        assertNull(u.queryParameter("timezone"));
        assertEquals(4, u.querySize());
    }

    @Test
    void getFixturesByLeague_withStringsAndTimezone() throws Exception {
        enqueue("fixtures_empty.json");
        api.getFixturesByLeague(141, 2026, "2026-11-07", "2026-11-09", "Europe/Madrid");
        HttpUrl u = lastRequestUrl();
        assertEquals("141", u.queryParameter("league"));
        assertEquals("Europe/Madrid", u.queryParameter("timezone"));
        assertEquals("2026-11-07", u.queryParameter("from"));
        assertEquals(5, u.querySize());

        enqueue("fixtures_empty.json");
        api.getFixturesByLeague(141, 2026, LocalDate.of(2026, 11, 7), LocalDate.of(2026, 11, 9), "UTC");
        assertEquals("UTC", lastRequestUrl().queryParameter("timezone"));
    }

    @Test
    void getFixturesByLeague_invalidArgumentsAreRejectedBeforeAnyRequest() {
        assertThrows(IllegalArgumentException.class, () -> api.getFixturesByLeague(140, 2026, "2026-02-30", "2026-03-01"));
        assertThrows(IllegalArgumentException.class, () -> api.getFixturesByLeague(140, 2026, (String) null, "2026-03-01"));
        assertThrows(IllegalArgumentException.class,
                () -> api.getFixturesByLeague(140, 2026, (LocalDate) null, LocalDate.of(2026, 3, 1)));
        assertThrows(IllegalArgumentException.class,
                () -> api.getFixturesByLeague(140, 2026, "2026-03-01", "2026-03-02", "+02:00"));
        assertThrows(IllegalArgumentException.class,
                () -> api.getFixturesByLeague(140, 2026, "2026-03-01", "2026-03-02", (String) null));
        assertNoRequestSent();
    }

    @Test
    void getFixturesByTeam_seasonAndRange() throws Exception {
        enqueue("fixtures_empty.json");
        api.getFixturesByTeam(541, 2026);
        HttpUrl u = lastRequestUrl();
        assertEquals("/fixtures", u.encodedPath());
        assertEquals("541", u.queryParameter("team"));
        assertEquals("2026", u.queryParameter("season"));
        assertEquals(2, u.querySize());

        enqueue("fixtures_empty.json");
        api.getFixturesByTeam(541, 2026, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));
        u = lastRequestUrl();
        assertEquals("2026-08-01", u.queryParameter("from"));
        assertEquals("2026-08-31", u.queryParameter("to"));
        assertEquals(4, u.querySize());

        enqueue("fixtures_empty.json");
        api.getFixturesByTeam(541, 2026, "2026-08-01", "2026-08-31");
        assertEquals("2026-08-31", lastRequestUrl().queryParameter("to"));

        assertThrows(IllegalArgumentException.class, () -> api.getFixturesByTeam(541, 2026, "2026-8-1", "2026-08-31"));
        assertEquals(3, server.getRequestCount());
    }

    // ------------------------------------------------------------------ next / last with timezone

    @Test
    void nextAndLastFixtures_withTimezone() throws Exception {
        enqueue("fixtures_empty.json");
        api.getNextFixtures(541, 3, "Europe/Madrid");
        HttpUrl u = lastRequestUrl();
        assertEquals("541", u.queryParameter("team"));
        assertEquals("3", u.queryParameter("next"));
        assertEquals("Europe/Madrid", u.queryParameter("timezone"));
        assertEquals(3, u.querySize());

        enqueue("fixtures_empty.json");
        api.getLastFixtures(541, 5, "Europe/Madrid");
        u = lastRequestUrl();
        assertEquals("5", u.queryParameter("last"));
        assertEquals("Europe/Madrid", u.queryParameter("timezone"));

        enqueue("fixtures_empty.json");
        api.getNextFixturesByLeague(140, 10, "America/Argentina/Buenos_Aires");
        u = lastRequestUrl();
        assertEquals("140", u.queryParameter("league"));
        assertEquals("10", u.queryParameter("next"));
        assertEquals("America/Argentina/Buenos_Aires", u.queryParameter("timezone"));

        enqueue("fixtures_empty.json");
        api.getLastFixturesByLeague(140, 10, "UTC");
        u = lastRequestUrl();
        assertEquals("10", u.queryParameter("last"));
        assertEquals("UTC", u.queryParameter("timezone"));
    }

    @Test
    void nextAndLast_withBadTimezoneOrCount_sendNothing() {
        assertThrows(IllegalArgumentException.class, () -> api.getNextFixtures(541, 3, "GMT+1"));
        assertThrows(IllegalArgumentException.class, () -> api.getLastFixtures(541, 3, null));
        assertThrows(IllegalArgumentException.class, () -> api.getNextFixturesByLeague(140, 3, "Mars/Olympus"));
        assertThrows(IllegalArgumentException.class, () -> api.getLastFixturesByLeague(140, 100, "UTC"));
        assertNoRequestSent();
    }

    // ------------------------------------------------------------------ batches of ids

    @Test
    void getFixturesByIdsInBatches_upToTwentyIsOneRequest() throws Exception {
        enqueue("fixtures_empty.json");
        api.getFixturesByIdsInBatches(ids(100, 20));
        assertEquals(1, server.getRequestCount());
        assertEquals(20, lastRequestUrl().queryParameter("ids").split("-").length);
    }

    @Test
    void getFixturesByIdsInBatches_splitsInBatchesOfTwenty_costOneRequestPerBatch() throws Exception {
        for (int i = 0; i < 3; i++) enqueue("fixtures_empty.json");
        List<Integer> all = ids(1000, 45);

        api.getFixturesByIdsInBatches(all);

        assertEquals(3, server.getRequestCount());
        String b1 = lastRequestUrl().queryParameter("ids");
        String b2 = lastRequestUrl().queryParameter("ids");
        String b3 = lastRequestUrl().queryParameter("ids");
        assertEquals(20, b1.split("-").length);
        assertEquals(20, b2.split("-").length);
        assertEquals(5, b3.split("-").length);
        assertTrue(b1.startsWith("1000-1001-"));
        assertTrue(b3.endsWith("-1044"));
    }

    @Test
    void getFixturesByIdsInBatches_21IdsCostTwoRequests_andDuplicatesAreSentOnce() throws Exception {
        enqueue("fixtures_empty.json");
        enqueue("fixtures_empty.json");
        List<Integer> withDuplicates = new ArrayList<>(ids(1, 21));
        withDuplicates.addAll(ids(1, 21));
        api.getFixturesByIdsInBatches(withDuplicates);
        assertEquals(2, server.getRequestCount());
        assertEquals(20, lastRequestUrl().queryParameter("ids").split("-").length);
        assertEquals("21", lastRequestUrl().queryParameter("ids"));
    }

    @Test
    void getFixturesByIdsInBatches_concatenatesTheBatches() throws Exception {
        enqueue("fixtures_by_id.json");
        enqueue("fixtures_by_id.json");
        // the single-element real response stands in for each batch
        List<Fixture> merged = api.getFixturesByIdsInBatches(ids(1, 25));
        assertEquals(2, merged.size());
        assertEquals(1570443, merged.get(1).getFixture().getId());
    }

    @Test
    void getFixturesByIdsInBatches_validatesEverythingBeforeTheFirstRequest() {
        assertThrows(IllegalArgumentException.class, () -> api.getFixturesByIdsInBatches(null));
        assertThrows(IllegalArgumentException.class, () -> api.getFixturesByIdsInBatches(new ArrayList<>()));
        assertThrows(IllegalArgumentException.class, () -> api.getFixturesByIdsInBatches(Arrays.asList(1, null, 3)));
        List<Integer> badInSecondBatch = ids(1, 25);
        badInSecondBatch.set(24, -7);
        assertThrows(IllegalArgumentException.class, () -> api.getFixturesByIdsInBatches(badInSecondBatch));
        assertNoRequestSent();
    }

    // ------------------------------------------------------------------ from/to without league or team

    @Test
    void dateRangeWithoutLeagueOrTeam_isRejectedBeforeAnyRequest_whateverTheSetterOrder() {
        assertThrows(IllegalArgumentException.class,
                () -> api.getFixtures(new FixturesQuery().from("2026-11-07").to("2026-11-09")));
        assertThrows(IllegalArgumentException.class,
                () -> api.getFixtures(new FixturesQuery().to("2026-11-09")));
        assertThrows(IllegalArgumentException.class,
                () -> api.getFixtures(new FixturesQuery().from("2026-11-07").season(2026).timezone("UTC")));
        assertNoRequestSent();
    }

    @Test
    void dateRangeWithLeagueOrTeam_isSentInAnyOrder() throws Exception {
        enqueue("fixtures_empty.json");
        api.getFixtures(new FixturesQuery().to("2026-11-09").from("2026-11-07").season(2026).league(140));
        assertEquals("140", lastRequestUrl().queryParameter("league"));

        enqueue("fixtures_empty.json");
        api.getFixtures(new FixturesQuery().from("2026-11-07").to("2026-11-09").team(541));
        assertEquals("541", lastRequestUrl().queryParameter("team"));

        enqueue("fixtures_empty.json");
        api.getFixtures(new FixturesQuery().from("2026-11-07").season(2026).league(140));
        assertEquals("2026-11-07", lastRequestUrl().queryParameter("from"));
    }

    @Test
    void validation_doesNotRejectWhatTheCatalogAllows() throws Exception {
        // league+season, date alone, next, live, id: no from/to, so nothing to reject
        enqueue("fixtures_empty.json");
        api.getFixtures(new FixturesQuery().league(140).season(2026));
        enqueue("fixtures_empty.json");
        api.getFixtures(new FixturesQuery().date("2026-11-12"));
        enqueue("fixtures_empty.json");
        api.getFixtures(new FixturesQuery().next(5));
        enqueue("fixtures_empty.json");
        api.getFixtures(new FixturesQuery().liveAll());
        assertEquals(4, server.getRequestCount());
    }

    @Test
    void nullQuery_isStillSentAsBefore() throws Exception {
        enqueueBody("{\"get\":\"fixtures\",\"parameters\":[],\"errors\":{\"required\":\"At least one parameter is required\"},"
                + "\"results\":0,\"paging\":{\"current\":1,\"total\":1},\"response\":[]}", 200);
        assertThrows(es.com.kete1987.apifootball.library.common.util.ApiFootballException.class, () -> api.getFixtures(null));
        assertEquals(1, server.getRequestCount());
    }
}

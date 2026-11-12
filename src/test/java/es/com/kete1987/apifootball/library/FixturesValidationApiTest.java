package es.com.kete1987.apifootball.library;

import es.com.kete1987.apifootball.library.common.util.ApiFootballException;
import es.com.kete1987.apifootball.library.fixtures.query.FixturesQuery;
import es.com.kete1987.apifootball.library.fixtures.query.HeadToHeadQuery;
import es.com.kete1987.apifootball.library.fixtures.query.InjuriesQuery;
import es.com.kete1987.apifootball.library.fixtures.query.RoundsQuery;
import es.com.kete1987.apifootball.library.fixtures.util.EventTypes;
import es.com.kete1987.apifootball.library.fixtures.util.FixtureStatuses;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Error fixtures, client-side validation and status helpers of the fixtures domain. */
class FixturesValidationApiTest extends BaseApiTest {

    private FixturesApi api;

    @BeforeEach
    void setUp() {
        api = new FixturesApi(client, baseUrl, tracker);
    }

    // ------------------------------------------------------------------ API errors

    @Test
    void invalidInteger_throwsWithBothErrors() throws Exception {
        enqueue("error_invalid_integer.json");

        ApiFootballException e = assertThrows(ApiFootballException.class,
                () -> api.getFixtures(new FixturesQuery().param("league", "abc")));

        assertEquals(200, e.getHttpCode());
        assertEquals(2, e.getErrors().size());
        assertEquals("The League field must contain an integer.", e.getErrors().get("league"));
        assertEquals("The Season field is required.", e.getErrors().get("season"));
        assertEquals("/fixtures?league=abc", takeRequest().getPath());
    }

    @Test
    void unknownParam_throws() throws Exception {
        enqueue("error_unknown_param.json");

        ApiFootballException e = assertThrows(ApiFootballException.class,
                () -> api.getFixtures(new FixturesQuery().param("foo", "bar")));

        assertEquals("The Foo field do not exist.", e.getErrors().get("foo"));
        assertEquals("/fixtures?foo=bar", takeRequest().getPath());
    }

    @Test
    void lastTooLarge_isReportedByTheApiWhenBypassingTheValidation() throws Exception {
        enqueue("error_last_too_large.json");

        ApiFootballException e = assertThrows(ApiFootballException.class,
                () -> api.getFixtures(new FixturesQuery().team(541).param("last", 100)));

        assertEquals(1, e.getErrors().size());
        assertTrue(e.getErrors().get("last").contains("Last field"));
        assertEquals("/fixtures?team=541&last=100", takeRequest().getPath());
    }

    @Test
    void tooManyIds_isReportedByTheApiWhenBypassingTheValidation() throws Exception {
        enqueue("error_too_many_ids.json");

        ApiFootballException e = assertThrows(ApiFootballException.class,
                () -> api.getFixtures(new FixturesQuery().param("ids", "1-2-3-4-5-6-7-8-9-10-11-12-13-14-15-16-17-18-19-20-21")));

        assertTrue(e.getErrors().get("ids").contains("Maximum of 20 ids allowed"));
    }

    // ------------------------------------------------------------------ client-side validation

    @Test
    void invalidDates_areRejectedBeforeAnyRequest() {
        for (String bad : new String[]{"2026-13-45", "2026-02-30", "2026-1-5", "12/11/2026", "", "tomorrow", "2026-11-12T00:00"}) {
            assertThrows(IllegalArgumentException.class, () -> new FixturesQuery().date(bad), bad);
            assertThrows(IllegalArgumentException.class, () -> new FixturesQuery().from(bad), bad);
            assertThrows(IllegalArgumentException.class, () -> new FixturesQuery().to(bad), bad);
            assertThrows(IllegalArgumentException.class, () -> new HeadToHeadQuery().date(bad), bad);
            assertThrows(IllegalArgumentException.class, () -> new InjuriesQuery().date(bad), bad);
        }
        assertThrows(IllegalArgumentException.class, () -> api.getFixturesByDate((String) null));
        assertThrows(IllegalArgumentException.class, () -> api.getFixturesByDate((LocalDate) null));
        assertThrows(IllegalArgumentException.class, () -> api.getFixturesByDate("2026-13-45"));
        assertEquals(0, server.getRequestCount());
    }

    @Test
    void validDates_areAccepted() {
        assertEquals("2026-02-28", new FixturesQuery().date("2026-02-28").toParams().get("date"));
        assertEquals("2028-02-29", new FixturesQuery().date("2028-02-29").toParams().get("date"));
        assertEquals("2026-11-12", new FixturesQuery().date(LocalDate.of(2026, 11, 12)).toParams().get("date"));
    }

    @Test
    void unknownTimezone_isRejected() {
        assertThrows(IllegalArgumentException.class, () -> new FixturesQuery().timezone("Mars/Olympus"));
        assertThrows(IllegalArgumentException.class, () -> new HeadToHeadQuery().timezone(""));
        assertThrows(IllegalArgumentException.class, () -> new RoundsQuery().timezone("Nope"));
        assertThrows(IllegalArgumentException.class, () -> api.getFixturesByDate(LocalDate.of(2026, 1, 1), null));
        assertEquals("Europe/Madrid", new FixturesQuery().timezone("Europe/Madrid").toParams().get("timezone"));
    }

    @Test
    void lastAndNext_acceptOnlyTwoDigits() {
        assertThrows(IllegalArgumentException.class, () -> new FixturesQuery().last(100));
        assertThrows(IllegalArgumentException.class, () -> new FixturesQuery().last(0));
        assertThrows(IllegalArgumentException.class, () -> new FixturesQuery().next(-1));
        assertThrows(IllegalArgumentException.class, () -> new HeadToHeadQuery().next(100));
        assertThrows(IllegalArgumentException.class, () -> api.getLastFixtures(1, 100));
        assertEquals("99", new FixturesQuery().last(99).toParams().get("last"));
        assertEquals("1", new FixturesQuery().next(1).toParams().get("next"));
    }

    @Test
    void ids_areLimitedToTwentyAndJoinedWithDashes() {
        List<Integer> twenty = new ArrayList<>();
        for (int i = 1; i <= 20; i++) twenty.add(i);
        List<Integer> twentyOne = new ArrayList<>(twenty);
        twentyOne.add(21);

        assertEquals(String.join("-", twenty.stream().map(String::valueOf).toArray(String[]::new)),
                new FixturesQuery().ids(twenty).toParams().get("ids"));
        assertThrows(IllegalArgumentException.class, () -> new FixturesQuery().ids(twentyOne));
        assertThrows(IllegalArgumentException.class, () -> new InjuriesQuery().ids(twentyOne));
        assertThrows(IllegalArgumentException.class, () -> new FixturesQuery().ids(new ArrayList<Integer>()));
        assertThrows(IllegalArgumentException.class, () -> new FixturesQuery().ids(Arrays.asList(1, null)));
        assertThrows(IllegalArgumentException.class, () -> new FixturesQuery().ids("1,2"));
        assertThrows(IllegalArgumentException.class, () -> api.getFixturesByIds(twentyOne));
        assertEquals("1-2", new FixturesQuery().ids("1-2").toParams().get("ids"));
    }

    @Test
    void live_acceptsAllOrLeagueIds() {
        assertEquals("all", new FixturesQuery().liveAll().toParams().get("live"));
        assertEquals("all", new FixturesQuery().live("all").toParams().get("live"));
        assertEquals("39-140", new FixturesQuery().live("39-140").toParams().get("live"));
        assertEquals("39-140", new FixturesQuery().live(Arrays.asList(39, 140)).toParams().get("live"));
        assertThrows(IllegalArgumentException.class, () -> new FixturesQuery().live("everything"));
        assertThrows(IllegalArgumentException.class, () -> new FixturesQuery().live(new ArrayList<Integer>()));
    }

    @Test
    void status_acceptsCodesJoinedByDash() {
        assertEquals("NS", new FixturesQuery().status("NS").toParams().get("status"));
        assertEquals("FT-AET-PEN", new FixturesQuery().status("FT-AET-PEN").toParams().get("status"));
        assertEquals("FT-AET-PEN", new FixturesQuery().status("FT", "AET", "PEN").toParams().get("status"));
        assertEquals("FT-AET-PEN", new FixturesQuery().status(FixtureStatuses.FINISHED_GROUP).toParams().get("status"));
        assertThrows(IllegalArgumentException.class, () -> new FixturesQuery().status("FT,AET"));
        assertThrows(IllegalArgumentException.class, () -> new FixturesQuery().status(""));
        assertThrows(IllegalArgumentException.class, () -> new FixturesQuery().status(new String[0]));
    }

    @Test
    void blankRound_isRejected() {
        assertThrows(IllegalArgumentException.class, () -> new FixturesQuery().round(" "));
        assertThrows(IllegalArgumentException.class, () -> api.getFixturesByRound(140, 2026, null));
    }

    @Test
    void query_neverSendsUnsetParameters() {
        assertTrue(new FixturesQuery().toParams().isEmpty());
        assertTrue(new InjuriesQuery().toParams().isEmpty());
        assertEquals(1, new FixturesQuery().league(1).toParams().size());
    }

    // ------------------------------------------------------------------ status helpers

    @Test
    void statusGroups_followTheCatalog() {
        for (String s : new String[]{"TBD", "NS"}) {
            assertTrue(FixtureStatuses.isScheduled(s), s);
            assertFalse(FixtureStatuses.isLive(s), s);
            assertFalse(FixtureStatuses.isFinished(s), s);
        }
        for (String s : new String[]{"1H", "HT", "2H", "ET", "BT", "P", "SUSP", "INT", "LIVE"}) {
            assertTrue(FixtureStatuses.isLive(s), s);
            assertFalse(FixtureStatuses.isScheduled(s), s);
            assertFalse(FixtureStatuses.isFinished(s), s);
        }
        for (String s : new String[]{"FT", "AET", "PEN"}) {
            assertTrue(FixtureStatuses.isFinished(s), s);
            assertFalse(FixtureStatuses.isLive(s), s);
        }
        assertTrue(FixtureStatuses.isPostponed("PST"));
        assertTrue(FixtureStatuses.isCancelled("CANC"));
        assertTrue(FixtureStatuses.isAbandoned("ABD"));
        assertTrue(FixtureStatuses.isNotPlayed("AWD"));
        assertTrue(FixtureStatuses.isNotPlayed("WO"));
        assertFalse(FixtureStatuses.isFinished("PST"));
        assertFalse(FixtureStatuses.isScheduled("PST"));
    }

    @Test
    void statusHelpers_areCaseInsensitiveAndNullSafe() {
        assertTrue(FixtureStatuses.isFinished("ft"));
        assertTrue(FixtureStatuses.isLive(" 1h "));
        assertFalse(FixtureStatuses.isLive(null));
        assertFalse(FixtureStatuses.isFinished(null));
        assertFalse(FixtureStatuses.isPostponed(null));
        assertFalse(FixtureStatuses.isScheduled("SOMETHING_NEW"));
        assertFalse(FixtureStatuses.isNotPlayed(null));
    }

    @Test
    void isPlaying_excludesSuspendedAndInterrupted() {
        assertTrue(FixtureStatuses.isPlaying("2H"));
        assertTrue(FixtureStatuses.isPlaying("HT"));
        assertFalse(FixtureStatuses.isPlaying("SUSP"));
        assertFalse(FixtureStatuses.isPlaying("INT"));
        assertFalse(FixtureStatuses.isPlaying("FT"));
        assertFalse(FixtureStatuses.isPlaying(null));
    }

    @Test
    void statusConstants_andJoin() {
        assertEquals("FT-AET-PEN", FixtureStatuses.join(FixtureStatuses.FT, FixtureStatuses.AET, FixtureStatuses.PEN));
        assertEquals("TBD-NS", FixtureStatuses.SCHEDULED_GROUP);
        assertThrows(IllegalArgumentException.class, FixtureStatuses::join);
        assertEquals("1H", FixtureStatuses.FIRST_HALF);
        assertEquals("subst", EventTypes.SUBSTITUTION);
    }
}

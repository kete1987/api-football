package es.com.kete1987.apifootball.library;

import es.com.kete1987.apifootball.library.fixtures.model.Fixture;
import es.com.kete1987.apifootball.library.fixtures.model.FixtureEvent;
import es.com.kete1987.apifootball.library.fixtures.model.FixtureStatus;
import es.com.kete1987.apifootball.library.fixtures.util.FixtureMinutes;
import es.com.kete1987.apifootball.library.fixtures.util.FixtureStatuses;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Truth table of the status classifiers over every documented short code (catalog section 8) and the
 * display-minute helpers. Inline JSON is hand-written from the documented shape unless it says recorded.
 */
class FixtureHelpersStatusTest extends BaseApiTest {

    private static final List<String> ALL = List.of("TBD", "NS", "1H", "HT", "2H", "ET", "BT", "P", "SUSP",
            "INT", "LIVE", "FT", "AET", "PEN", "PST", "CANC", "ABD", "AWD", "WO");

    private FixturesApi api;

    @BeforeEach
    void setUp() {
        api = new FixturesApi(client, baseUrl, tracker);
    }

    private static void check(String name, Set<String> expectedTrue, Predicate<String> p) {
        for (String code : ALL) {
            assertEquals(expectedTrue.contains(code), p.test(code), name + " for " + code);
        }
        assertFalse(p.test(null), name + " null");
        assertFalse(p.test("NEW_CODE"), name + " unknown");
    }

    @Test
    void isLive_includesSuspendedAndInterrupted() {
        check("isLive", Set.of("1H", "HT", "2H", "ET", "BT", "P", "SUSP", "INT", "LIVE"), FixtureStatuses::isLive);
    }

    @Test
    void isPlaying_excludesSuspendedAndInterruptedOnly() {
        check("isPlaying", Set.of("1H", "HT", "2H", "ET", "BT", "P", "LIVE"), FixtureStatuses::isPlaying);
    }

    @Test
    void isInterrupted_isSuspAndIntOnly() {
        check("isInterrupted", Set.of("SUSP", "INT"), FixtureStatuses::isInterrupted);
        assertTrue(FixtureStatuses.isInterrupted(" susp "));
    }

    @Test
    void isPendingResolution_isTheAppPostponedSet() {
        check("isPendingResolution", Set.of("PST", "SUSP", "INT", "CANC", "ABD", "AWD", "WO"),
                FixtureStatuses::isPendingResolution);
    }

    @Test
    void everyCodeIsInExactlyOnePrimaryGroup_exceptThatInterruptedAreAlsoLive() {
        for (String c : ALL) {
            int primary = (FixtureStatuses.isScheduled(c) ? 1 : 0) + (FixtureStatuses.isLive(c) ? 1 : 0)
                    + (FixtureStatuses.isFinished(c) ? 1 : 0) + (FixtureStatuses.isPostponed(c) ? 1 : 0)
                    + (FixtureStatuses.isCancelled(c) ? 1 : 0) + (FixtureStatuses.isAbandoned(c) ? 1 : 0)
                    + (FixtureStatuses.isNotPlayed(c) ? 1 : 0);
            assertEquals(1, primary, c);
        }
    }

    private FixtureStatus status(String json) throws Exception {
        enqueueBody("{\"errors\":[],\"results\":1,\"paging\":{\"current\":1,\"total\":1},\"response\":"
                + "[{\"fixture\":{\"status\":" + json + "}}]}", 200);
        Fixture f = api.getFixtureById(1);
        takeRequest();
        return f.getFixture().getStatus();
    }

    @Test
    void fixtureStatus_delegatesAndKeepsTheDocumentedSemantics() throws Exception {
        FixtureStatus susp = status("{\"long\":\"Match Suspended\",\"short\":\"SUSP\",\"elapsed\":67,\"extra\":null}");
        assertTrue(susp.isLive());
        assertFalse(susp.isPlaying());
        assertTrue(susp.isInterrupted());
        assertTrue(susp.isPendingResolution());
        FixtureStatus second = status("{\"long\":\"Second Half\",\"short\":\"2H\",\"elapsed\":70}");
        assertTrue(second.isLive());
        assertTrue(second.isPlaying());
        assertFalse(second.isInterrupted());
        assertFalse(second.isPendingResolution());
    }

    @Test
    void displayMinute_combinesElapsedAndExtra() throws Exception {
        FixtureStatus s = status("{\"long\":\"Second Half\",\"short\":\"2H\",\"elapsed\":90,\"extra\":4}");
        assertEquals(94, s.getDisplayMinute());
        assertEquals("90+4", s.getDisplayMinuteText());
        FixtureStatus plain = status("{\"long\":\"First Half\",\"short\":\"1H\",\"elapsed\":14,\"extra\":null}");
        assertEquals(14, plain.getDisplayMinute());
        assertEquals("14", plain.getDisplayMinuteText());
        FixtureStatus ns = status("{\"long\":\"Not Started\",\"short\":\"NS\"}");
        assertNull(ns.getDisplayMinute());
        assertNull(ns.getDisplayMinuteText());
        FixtureStatus bt = status("{\"long\":\"Break Time\",\"short\":\"BT\",\"elapsed\":null,\"extra\":7}");
        assertNull(bt.getDisplayMinute());
    }

    @Test
    void fixtureMinutes_edgeCases() {
        assertEquals(45, FixtureMinutes.total(45, 0));
        assertEquals("45", FixtureMinutes.format(45, 0));
        assertEquals(120, FixtureMinutes.total(120, -3));
        assertEquals("105+2", FixtureMinutes.format(105, 2));
        assertNull(FixtureMinutes.total(null, 3));
        assertNull(FixtureMinutes.format(null, null));
    }

    @Test
    void eventTime_displayMinute_eventsOfTheLiveCapture() throws Exception {
        // times recorded live (Navalcarnero-Betis, 1644363): goal 90+2, red card 118, yellow 120+2
        enqueueBody("{\"errors\":[],\"results\":1,\"paging\":{\"current\":1,\"total\":1},\"response\":[{\"events\":["
                + "{\"time\":{\"elapsed\":90,\"extra\":2},\"type\":\"Goal\",\"detail\":\"Normal Goal\"},"
                + "{\"time\":{\"elapsed\":118},\"type\":\"Card\",\"detail\":\"Red Card\"},"
                + "{\"time\":{\"elapsed\":120,\"extra\":2},\"type\":\"Card\",\"detail\":\"Yellow Card\"},"
                + "{\"type\":\"Card\",\"detail\":\"Yellow Card\"}]}]}", 200);
        List<FixtureEvent> ev = api.getFixtureById(1).getEvents();
        assertEquals(92, ev.get(0).getTime().getDisplayMinute());
        assertEquals("90+2", ev.get(0).getTime().getDisplayMinuteText());
        assertEquals(118, ev.get(1).getTime().getDisplayMinute());
        assertEquals("118", ev.get(1).getTime().getDisplayMinuteText());
        assertEquals("120+2", ev.get(2).getTime().getDisplayMinuteText());
        assertNull(ev.get(3).getTime());
    }
}

package es.com.kete1987.apifootball.library;

import es.com.kete1987.apifootball.library.fixtures.model.Fixture;
import es.com.kete1987.apifootball.library.fixtures.model.FixtureEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code Fixture.getEventsSorted()} and {@code FixtureEvent.BY_TIME}. The real fixtures
 * ({@code fixtures_by_id_penalty_shootout.json}, {@code fixtures_by_id_own_goal.json}) document that the
 * order of the API is not chronological; the small inline bodies are hand-written from the documented shape.
 */
class FixtureEventOrderTest extends BaseApiTest {

    private FixturesApi api;

    @BeforeEach
    void setUp() {
        api = new FixturesApi(client, baseUrl, tracker);
    }

    private Fixture real(String file, int id) throws Exception {
        enqueue(file);
        Fixture f = api.getFixtureById(id);
        takeRequest();
        return f;
    }

    private static String ev(String time, String type, String detail) {
        return "{\"time\":" + time + ",\"team\":{\"id\":1,\"name\":\"T\",\"logo\":\"l\"},"
                + "\"player\":{\"id\":9,\"name\":\"P\"},\"assist\":{\"id\":null,\"name\":null},"
                + "\"type\":\"" + type + "\",\"detail\":\"" + detail + "\",\"comments\":null}";
    }

    private Fixture inline(String... events) throws Exception {
        enqueueBody("{\"get\":\"fixtures\",\"parameters\":{\"id\":\"1\"},\"errors\":[],\"results\":1,"
                + "\"paging\":{\"current\":1,\"total\":1},\"response\":[{\"events\":["
                + String.join(",", events) + "]}]}", 200);
        Fixture f = api.getFixtureById(1);
        takeRequest();
        return f;
    }

    private static int extra(FixtureEvent e) {
        return e.getTime().getExtra() == null ? 0 : e.getTime().getExtra();
    }

    private static boolean chronological(List<FixtureEvent> events) {
        for (int i = 1; i < events.size(); i++) {
            FixtureEvent a = events.get(i - 1);
            FixtureEvent b = events.get(i);
            int c = Integer.compare(a.getTime().getElapsed(), b.getTime().getElapsed());
            if (c > 0 || (c == 0 && extra(a) > extra(b))) return false;
        }
        return true;
    }

    @Test
    void realFixtures_rawOrderIsNotChronological_sortedIsAndHasTheSameEvents() throws Exception {
        boolean someRawUnsorted = false;
        for (Fixture f : List.of(real("fixtures_by_id_penalty_shootout.json", 1544371),
                real("fixtures_by_id_own_goal.json", 1390944))) {
            List<FixtureEvent> raw = f.getEvents();
            List<FixtureEvent> rawCopy = new ArrayList<>(raw);
            someRawUnsorted |= !chronological(raw);

            List<FixtureEvent> sorted = f.getEventsSorted();

            assertTrue(chronological(sorted));
            assertEquals(raw.size(), sorted.size());
            List<FixtureEvent> remaining = new ArrayList<>(raw);
            for (FixtureEvent e : sorted) {
                int idx = -1;
                for (int i = 0; i < remaining.size(); i++) {
                    if (remaining.get(i) == e) {
                        idx = i;
                        break;
                    }
                }
                assertTrue(idx >= 0, "sorted contains an event that is not in getEvents()");
                remaining.remove(idx);
            }
            assertTrue(remaining.isEmpty());
            assertEquals(rawCopy, f.getEvents());
        }
        assertTrue(someRawUnsorted, "expected a non-chronological order in at least one real fixture");
    }

    @Test
    void sameMinute_yellowThenRedKeepTheApiOrder() throws Exception {
        Fixture f = inline(
                ev("{\"elapsed\":70,\"extra\":null}", "Card", "Yellow Card"),
                ev("{\"elapsed\":70,\"extra\":null}", "Card", "Red Card"),
                ev("{\"elapsed\":10,\"extra\":null}", "Goal", "Normal Goal"));
        List<FixtureEvent> s = f.getEventsSorted();
        assertTrue(s.get(0).isGoal());
        assertTrue(s.get(1).isYellowCard());
        assertTrue(s.get(2).isRedCard());

        // the reverse API order is kept as well: the sort never reorders ties
        Fixture g = inline(
                ev("{\"elapsed\":70,\"extra\":null}", "Card", "Red Card"),
                ev("{\"elapsed\":70,\"extra\":null}", "Card", "Yellow Card"));
        assertTrue(g.getEventsSorted().get(0).isRedCard());
        assertTrue(g.getEventsSorted().get(1).isYellowCard());
    }

    @Test
    void nullExtraSortsBeforeExtraOneAtTheSameMinute() throws Exception {
        Fixture f = inline(
                ev("{\"elapsed\":90,\"extra\":6}", "Card", "Yellow Card"),
                ev("{\"elapsed\":90,\"extra\":1}", "Goal", "Normal Goal"),
                ev("{\"elapsed\":90}", "subst", "Substitution 1"),
                ev("{\"elapsed\":90,\"extra\":null}", "subst", "Substitution 2"));
        List<FixtureEvent> s = f.getEventsSorted();
        assertEquals("Substitution 1", s.get(0).getDetail());
        assertEquals("Substitution 2", s.get(1).getDetail());
        assertEquals(1, s.get(2).getTime().getExtra());
        assertEquals(6, s.get(3).getTime().getExtra());
    }

    @Test
    void eventsWithoutTimeOrElapsedGoLast_andNothingThrows() throws Exception {
        Fixture f = inline(
                ev("null", "Var", "Goal cancelled"),
                ev("{\"elapsed\":null,\"extra\":3}", "Card", "Yellow Card"),
                ev("{\"elapsed\":5,\"extra\":null}", "Goal", "Normal Goal"),
                ev("{\"elapsed\":2,\"extra\":null}", "Card", "Yellow Card"));
        List<FixtureEvent> s = f.getEventsSorted();
        assertEquals(2, s.get(0).getTime().getElapsed());
        assertEquals(5, s.get(1).getTime().getElapsed());
        assertTrue(s.get(2).isVar());
        assertTrue(s.get(3).isYellowCard());
        assertNull(s.get(3).getTime().getElapsed());
        assertTrue(FixtureEvent.BY_TIME.compare(null, s.get(0)) > 0);
        assertEquals(0, FixtureEvent.BY_TIME.compare(null, null));
    }

    @Test
    void nullEvents_returnNull() throws Exception {
        enqueueBody("{\"get\":\"fixtures\",\"parameters\":{\"id\":\"1\"},\"errors\":[],\"results\":1,"
                + "\"paging\":{\"current\":1,\"total\":1},\"response\":[{\"fixture\":{\"id\":1}}]}", 200);
        Fixture f = api.getFixtureById(1);
        takeRequest();
        assertNull(f.getEvents());
        assertNull(f.getEventsSorted());
    }

    @Test
    void sortedListIsUnmodifiableAndANewList() throws Exception {
        Fixture f = inline(ev("{\"elapsed\":1,\"extra\":null}", "Goal", "Normal Goal"));
        List<FixtureEvent> s = f.getEventsSorted();
        assertThrows(UnsupportedOperationException.class, () -> s.add(s.get(0)));
        assertThrows(UnsupportedOperationException.class, () -> s.remove(0));
        assertThrows(UnsupportedOperationException.class, () -> s.sort(FixtureEvent.BY_TIME));
        assertFalse(s == f.getEvents());
        assertSame(s.get(0), f.getEvents().get(0));
    }
}

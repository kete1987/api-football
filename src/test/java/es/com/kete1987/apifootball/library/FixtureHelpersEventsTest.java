package es.com.kete1987.apifootball.library;

import es.com.kete1987.apifootball.library.fixtures.model.Fixture;
import es.com.kete1987.apifootball.library.fixtures.model.FixtureEvent;
import es.com.kete1987.apifootball.library.fixtures.util.FixtureEventAnalysis;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Second-yellow detection and running score. The cards come from the live capture of 2026-11-12
 * (events trimmed to the relevant ones, copied as the API sent them); fixtures_by_id_* files are real.
 */
class FixtureHelpersEventsTest extends BaseApiTest {

    private static final String RUIBAL = "{\"id\":47119,\"name\":\"A. Ruibal\"}";

    private FixturesApi api;

    @BeforeEach
    void setUp() {
        api = new FixturesApi(client, baseUrl, tracker);
    }

    private static String card(String time, int team, String player, String detail) {
        return "{\"time\":" + time + ",\"team\":{\"id\":" + team + ",\"name\":\"T\",\"logo\":\"l\"},\"player\":"
                + player + ",\"assist\":{},\"type\":\"Card\",\"detail\":\"" + detail + "\"}";
    }

    private static String goal(String time, int team, String detail) {
        return "{\"time\":" + time + ",\"team\":{\"id\":" + team + ",\"name\":\"T\",\"logo\":\"l\"},"
                + "\"player\":{\"id\":9,\"name\":\"P\"},\"assist\":{},\"type\":\"Goal\",\"detail\":\"" + detail + "\"}";
    }

    private Fixture inline(String... events) throws Exception {
        enqueueBody("{\"errors\":[],\"results\":1,\"paging\":{\"current\":1,\"total\":1},\"response\":[{\"events\":["
                + String.join(",", events) + "]}]}", 200);
        Fixture f = api.getFixtureById(1);
        takeRequest();
        return f;
    }

    /** Navalcarnero-Betis 1644363 as recorded: yellow 90+4, ..., yellow 118, red 118. */
    private Fixture ruibalMatch() throws Exception {
        return inline(
                card("{\"elapsed\":90,\"extra\":4}", 543, RUIBAL, "Yellow Card"),
                card("{\"elapsed\":90,\"extra\":4}", 5271, "{\"id\":609774,\"name\":\"M. Jimenez\"}", "Yellow Card"),
                card("{\"elapsed\":118}", 543, RUIBAL, "Yellow Card"),
                card("{\"elapsed\":118}", 543, RUIBAL, "Red Card"),
                card("{\"elapsed\":120,\"extra\":2}", 5271, "{\"id\":560951,\"name\":\"K. Vazquez\"}", "Yellow Card"));
    }

    @Test
    void ruibal118_redIsASecondYellow_andItsYellowIsAbsorbed() throws Exception {
        Fixture f = ruibalMatch();
        List<FixtureEvent> ev = f.getEvents();
        FixtureEvent red = ev.get(3);

        assertTrue(f.isSecondYellowRed(red));
        assertFalse(f.isSecondYellowRed(ev.get(2)), "the yellow of the pair is not the red");
        assertFalse(f.isSecondYellowRed(ev.get(0)));

        List<FixtureEvent> merged = f.getCardEventsMerged();
        assertEquals(4, merged.size());
        assertTrue(merged.contains(ev.get(0)), "the earlier yellow at 90+4 stays");
        assertFalse(merged.contains(ev.get(2)), "the yellow of the second yellow is absorbed");
        assertSame(red, merged.get(2));
    }

    @Test
    void straightRed_isNotASecondYellow() throws Exception {
        // Krejci, Czech Republic-Spain 1528961, 72': red card with no yellow at all (recorded live)
        Fixture f = inline(card("{\"elapsed\":72}", 770, "{\"id\":66407,\"name\":\"L. Krejci\"}", "Red Card"));
        assertFalse(f.isSecondYellowRed(f.getEvents().get(0)));
        assertEquals(1, f.getCardEventsMerged().size());
        assertTrue(FixtureEventAnalysis.secondYellowReds(f.getEvents()).isEmpty());
    }

    @Test
    void yellowAtAnEarlierMinute_doesNotMakeALaterRedASecondYellow() throws Exception {
        Fixture f = inline(card("{\"elapsed\":30}", 1, "{\"id\":5,\"name\":\"X\"}", "Yellow Card"),
                card("{\"elapsed\":60}", 1, "{\"id\":5,\"name\":\"X\"}", "Red Card"));
        assertFalse(f.isSecondYellowRed(f.getEvents().get(1)));
        assertEquals(2, f.getCardEventsMerged().size());
    }

    @Test
    void sameMinuteDifferentPlayer_isNotASecondYellow() throws Exception {
        Fixture f = inline(card("{\"elapsed\":60}", 1, "{\"id\":5,\"name\":\"X\"}", "Yellow Card"),
                card("{\"elapsed\":60}", 1, "{\"id\":6,\"name\":\"Y\"}", "Red Card"));
        assertFalse(f.isSecondYellowRed(f.getEvents().get(1)));
    }

    @Test
    void nullPlayerIds_matchByNameOrNotAtAll() throws Exception {
        // the polls right after an event can carry a player without id (seen live)
        Fixture byName = inline(card("{\"elapsed\":116}", 1, "{\"name\":\"V. Rodriguez\"}", "Yellow Card"),
                card("{\"elapsed\":116}", 1, "{\"name\":\"v. rodriguez\"}", "Red Card"));
        assertTrue(byName.isSecondYellowRed(byName.getEvents().get(1)));
        Fixture anonymous = inline(card("{\"elapsed\":116}", 1, "{}", "Yellow Card"),
                card("{\"elapsed\":116}", 1, "{}", "Red Card"));
        assertFalse(anonymous.isSecondYellowRed(anonymous.getEvents().get(1)));
    }

    @Test
    void secondYellow_nullSafety() throws Exception {
        Fixture f = ruibalMatch();
        assertFalse(f.isSecondYellowRed(null));
        assertFalse(FixtureEventAnalysis.isSecondYellowRed(f.getEvents().get(3), null));
        assertTrue(FixtureEventAnalysis.cardEventsMerged(null).isEmpty());
        assertTrue(FixtureEventAnalysis.secondYellowReds(null).isEmpty());
        assertEquals(1, FixtureEventAnalysis.secondYellowReds(f.getEvents()).size());
        Fixture noEvents = new com.google.gson.Gson().fromJson("{}", Fixture.class);
        assertFalse(noEvents.isSecondYellowRed(f.getEvents().get(3)));
        assertTrue(noEvents.getCardEventsMerged().isEmpty());
        assertTrue(noEvents.getRunningScore().isEmpty());
    }

    private Fixture real(String file, int id) throws Exception {
        enqueue(file);
        Fixture f = api.getFixtureById(id);
        takeRequest();
        return f;
    }

    private static void assertLastStepEqualsGoals(Fixture f) {
        List<Fixture.ScoreStep> steps = f.getRunningScore();
        Fixture.ScoreStep last = steps.get(steps.size() - 1);
        assertEquals(f.getGoals().getHome(), last.getHome());
        assertEquals(f.getGoals().getAway(), last.getAway());
        for (int i = 1; i < steps.size(); i++) {
            assertEquals(1, (steps.get(i).getHome() - steps.get(i - 1).getHome())
                    + (steps.get(i).getAway() - steps.get(i - 1).getAway()));
        }
    }

    @Test
    void runningScore_realFixtures_lastStepEqualsGoals() throws Exception {
        Fixture oneOne = real("fixtures_by_id_penalty_shootout.json", 1544371);
        assertLastStepEqualsGoals(oneOne);
        assertEquals(2, oneOne.getRunningScore().size());
        assertEquals("1-1", oneOne.getRunningScore().get(1).getText());

        Fixture ownGoal = real("fixtures_by_id_own_goal.json", 1390944);
        assertLastStepEqualsGoals(ownGoal);
        assertEquals(1, ownGoal.getRunningScore().size());
        assertEquals("0-1", ownGoal.getRunningScore().get(0).getText());
        assertTrue(ownGoal.getRunningScore().get(0).getEvent().isOwnGoal());

    }

    @Test
    void runningScore_skipsMissedPenaltiesAndFollowsChronology() throws Exception {
        enqueueBody("{\"errors\":[],\"results\":1,\"paging\":{\"current\":1,\"total\":1},\"response\":[{"
                + "\"teams\":{\"home\":{\"id\":10,\"name\":\"H\"},\"away\":{\"id\":20,\"name\":\"A\"}},\"events\":["
                + goal("{\"elapsed\":90,\"extra\":2}", 10, "Normal Goal") + ","
                + goal("{\"elapsed\":117}", 10, "Missed Penalty") + ","
                + goal("{\"elapsed\":5}", 20, "Normal Goal") + ","
                + goal("{\"elapsed\":50}", 10, "Own Goal") + ","
                + goal("{\"elapsed\":96}", 20, "Penalty")
                + "]}]}", 200);
        Fixture f = api.getFixtureById(1);
        List<String> texts = new ArrayList<>();
        for (Fixture.ScoreStep s : f.getRunningScore()) texts.add(s.getText());
        assertEquals(List.of("0-1", "1-1", "2-1", "2-2"), texts);
        assertNull(f.getRunningScore().get(0).getEvent().getTime().getExtra());
    }

    @Test
    void runningScore_withoutTeams_skipsTheGoals() throws Exception {
        Fixture f = inline(goal("{\"elapsed\":5}", 10, "Normal Goal"));
        assertTrue(f.getRunningScore().isEmpty());
    }
}

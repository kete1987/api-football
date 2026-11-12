package es.com.kete1987.apifootball.library;

import es.com.kete1987.apifootball.library.fixtures.model.FixtureEvent;
import es.com.kete1987.apifootball.library.fixtures.util.EventTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Semantics of the {@link FixtureEvent} helpers: what each one answers for every documented
 * {@code type}/{@code detail} pair and for the real shapes seen live.
 *
 * <p>The captured fixtures contain only "Normal Goal", "Yellow Card" and "Substitution N" events, so
 * the bodies here are inline:
 * <ul>
 *   <li>events marked <b>hand-written</b> are built from the documented shape (docs/API_CATALOG.md
 *       sections 9 and 11.16), not captured from the API;</li>
 *   <li>events marked <b>real</b> were recorded from {@code /fixtures?live=all} during the
 *       2026-11-12 comparison run (fixtures 1644363 Navalcarnero - Real Betis and 1528961 Czech
 *       Republic - Spain). They were stored after parsing, with {@code null} members dropped, so
 *       they are real values but not byte-for-byte API responses, and therefore not fixture files.</li>
 * </ul>
 */
class FixtureEventSemanticsTest extends BaseApiTest {

    private static final String BETIS = "{\"id\":543,\"name\":\"Real Betis\",\"logo\":\"https://media.api-sports.io/football/teams/543.png\"}";
    private static final String CZE = "{\"id\":770,\"name\":\"Czech Republic\",\"logo\":\"https://media.api-sports.io/football/teams/770.png\"}";

    /** One row per event: the JSON of the event and the expected answer of every helper. */
    private static final class Row {
        final String label;
        final String json;
        final boolean goal, penaltyGoal, ownGoal, missedPenalty, card, yellow, red, subst, var;

        Row(String label, String json, boolean goal, boolean penaltyGoal, boolean ownGoal, boolean missedPenalty,
            boolean card, boolean yellow, boolean red, boolean subst, boolean var) {
            this.label = label;
            this.json = json;
            this.goal = goal;
            this.penaltyGoal = penaltyGoal;
            this.ownGoal = ownGoal;
            this.missedPenalty = missedPenalty;
            this.card = card;
            this.yellow = yellow;
            this.red = red;
            this.subst = subst;
            this.var = var;
        }
    }

    private static String event(int minute, String team, String player, String assist, String type, String detail) {
        return "{\"time\":{\"elapsed\":" + minute + ",\"extra\":null},\"team\":" + team
                + ",\"player\":" + player + ",\"assist\":" + assist
                + ",\"type\":" + quote(type) + ",\"detail\":" + quote(detail) + ",\"comments\":null}";
    }

    private static String quote(String s) {
        return s == null ? "null" : "\"" + s + "\"";
    }

    private static String player(int id, String name) {
        return "{\"id\":" + id + ",\"name\":\"" + name + "\"}";
    }

    private static final String NOBODY = "{\"id\":null,\"name\":null}";

    //                                       goal   penG   own    miss   card   yel    red    sub    var
    private static final List<Row> ROWS = List.of(
            // ---- hand-written from the documented type/detail list (catalog section 9)
            new Row("hand-written Normal Goal", event(10, BETIS, player(1, "A"), player(2, "B"), "Goal", "Normal Goal"),
                    true, false, false, false, false, false, false, false, false),
            new Row("hand-written Penalty", event(11, BETIS, player(1, "A"), NOBODY, "Goal", "Penalty"),
                    true, true, false, false, false, false, false, false, false),
            new Row("hand-written Own Goal", event(12, BETIS, player(1, "A"), NOBODY, "Goal", "Own Goal"),
                    true, false, true, false, false, false, false, false, false),
            new Row("hand-written Missed Penalty", event(13, BETIS, player(1, "A"), NOBODY, "Goal", "Missed Penalty"),
                    false, false, false, true, false, false, false, false, false),
            new Row("hand-written Yellow Card", event(14, BETIS, player(1, "A"), NOBODY, "Card", "Yellow Card"),
                    false, false, false, false, true, true, false, false, false),
            new Row("hand-written Red card (docs spelling)", event(15, BETIS, player(1, "A"), NOBODY, "Card", "Red card"),
                    false, false, false, false, true, false, true, false, false),
            new Row("hand-written Subst (docs casing)", event(16, BETIS, player(1, "A"), player(2, "B"), "Subst", "Substitution 1"),
                    false, false, false, false, false, false, false, true, false),
            new Row("hand-written Var Goal cancelled", event(17, BETIS, player(1, "A"), NOBODY, "Var", "Goal cancelled"),
                    false, false, false, false, false, false, false, false, true),
            new Row("hand-written Var Penalty confirmed", event(18, BETIS, player(1, "A"), NOBODY, "Var", "Penalty confirmed"),
                    false, false, false, false, false, false, false, false, true),
            // casing and whitespace variants, missing detail
            new Row("hand-written MISSED PENALTY in upper case", event(19, BETIS, NOBODY, NOBODY, "GOAL", " MISSED PENALTY "),
                    false, false, false, true, false, false, false, false, false),
            new Row("hand-written Goal without detail", event(20, BETIS, NOBODY, NOBODY, "Goal", null),
                    true, false, false, false, false, false, false, false, false),
            new Row("hand-written event with every member null", "{\"time\":null,\"team\":null,\"player\":null,\"assist\":null,\"type\":null,\"detail\":null,\"comments\":null}",
                    false, false, false, false, false, false, false, false, false),

            // ---- real, fixture 1644363, poll of 21:38:00 (Europe/Madrid): first appearance, no player yet
            new Row("real Missed Penalty, player not yet known", "{\"time\":{\"elapsed\":117},\"team\":" + BETIS + ",\"player\":{},\"assist\":{},\"type\":\"Goal\",\"detail\":\"Missed Penalty\"}",
                    false, false, false, true, false, false, false, false, false),
            // real, same event from 21:39:00 on: the taker is in both player and assist
            new Row("real Missed Penalty", "{\"time\":{\"elapsed\":117},\"team\":" + BETIS + ",\"player\":{\"id\":745,\"name\":\"Isco\"},\"assist\":{\"id\":745,\"name\":\"Isco\"},\"type\":\"Goal\",\"detail\":\"Missed Penalty\"}",
                    false, false, false, true, false, false, false, false, false),
            // real, 21:40:30 only: the goal without player that briefly made the API's own score 2-4
            new Row("real transient Normal Goal without player", "{\"time\":{\"elapsed\":119},\"team\":" + BETIS + ",\"player\":{},\"assist\":{},\"type\":\"Goal\",\"detail\":\"Normal Goal\"}",
                    true, false, false, false, false, false, false, false, false),
            // real, second yellow: two events at the same minute
            new Row("real second Yellow Card", "{\"time\":{\"elapsed\":118},\"team\":" + BETIS + ",\"player\":{\"id\":47119,\"name\":\"A. Ruibal\"},\"assist\":{},\"type\":\"Card\",\"detail\":\"Yellow Card\"}",
                    false, false, false, false, true, true, false, false, false),
            new Row("real Red Card after second yellow", "{\"time\":{\"elapsed\":118},\"team\":" + BETIS + ",\"player\":{\"id\":47119,\"name\":\"A. Ruibal\"},\"assist\":{},\"type\":\"Card\",\"detail\":\"Red Card\"}",
                    false, false, false, false, true, false, true, false, false),
            // real substitution: player = off (Antony, starting XI), assist = on (Isco, bench)
            new Row("real subst", "{\"time\":{\"elapsed\":58},\"team\":" + BETIS + ",\"player\":{\"id\":1,\"name\":\"Antony\"},\"assist\":{\"id\":745,\"name\":\"Isco\"},\"type\":\"subst\",\"detail\":\"Substitution 1\"}",
                    false, false, false, false, false, false, false, true, false),
            // real, fixture 1528961: VAR event first without detail (22:16:00), then with an undocumented one
            new Row("real Var without detail yet", "{\"time\":{\"elapsed\":69},\"team\":" + CZE + ",\"player\":{\"id\":66407,\"name\":\"Ladislav Krej\u010d\u00ed\"},\"assist\":{},\"type\":\"Var\"}",
                    false, false, false, false, false, false, false, false, true),
            new Row("real Var Card upgrade confirmed", "{\"time\":{\"elapsed\":69},\"team\":" + CZE + ",\"player\":{\"id\":66407,\"name\":\"Ladislav Krej\u010d\u00ed\"},\"assist\":{},\"type\":\"Var\",\"detail\":\"Card upgrade confirmed\"}",
                    false, false, false, false, false, false, false, false, true));

    private FixturesApi api;

    @BeforeEach
    void setUp() {
        api = new FixturesApi(client, baseUrl, tracker);
    }

    private List<FixtureEvent> parse(List<String> events) throws Exception {
        String body = "{\"get\":\"fixtures/events\",\"parameters\":{\"fixture\":\"1644363\"},\"errors\":[],\"results\":"
                + events.size() + ",\"paging\":{\"current\":1,\"total\":1},\"response\":[" + String.join(",", events) + "]}";
        enqueueBody(body, 200);
        List<FixtureEvent> parsed = api.getFixtureEvents(1644363);
        takeRequest();
        return parsed;
    }

    @Test
    void everyHelperAnswersAsDocumentedForEveryEventShape() throws Exception {
        List<String> json = new ArrayList<>();
        for (Row row : ROWS) json.add(row.json);
        List<FixtureEvent> events = parse(json);
        assertEquals(ROWS.size(), events.size());

        for (int i = 0; i < ROWS.size(); i++) {
            Row r = ROWS.get(i);
            FixtureEvent e = events.get(i);
            assertEquals(r.goal, e.isGoal(), r.label + ": isGoal");
            assertEquals(r.penaltyGoal, e.isPenaltyGoal(), r.label + ": isPenaltyGoal");
            assertEquals(r.ownGoal, e.isOwnGoal(), r.label + ": isOwnGoal");
            assertEquals(r.missedPenalty, e.isMissedPenalty(), r.label + ": isMissedPenalty");
            assertEquals(r.card, e.isCard(), r.label + ": isCard");
            assertEquals(r.yellow, e.isYellowCard(), r.label + ": isYellowCard");
            assertEquals(r.red, e.isRedCard(), r.label + ": isRedCard");
            assertEquals(r.subst, e.isSubstitution(), r.label + ": isSubstitution");
            assertEquals(r.var, e.isVar(), r.label + ": isVar");
            // invariants: a missed penalty is never a goal; specific goal kinds imply isGoal
            assertFalse(e.isGoal() && e.isMissedPenalty(), r.label);
            if (e.isPenaltyGoal() || e.isOwnGoal()) assertTrue(e.isGoal(), r.label);
            if (e.isYellowCard() || e.isRedCard()) assertTrue(e.isCard(), r.label);
        }
    }

    /**
     * Safeguard: every detail constant of {@link EventTypes} must have a row above, so adding a new
     * detail forces a decision about what each helper answers for it.
     */
    @Test
    void everyDetailConstantIsCoveredByTheTable() throws Exception {
        Set<String> covered = new HashSet<>();
        for (Row row : ROWS) {
            FixtureEvent e = parse(List.of(row.json)).get(0);
            if (e.getDetail() != null) covered.add(e.getDetail().trim().toLowerCase(Locale.ROOT));
        }
        Set<String> notDetails = Set.of("GOAL", "CARD", "SUBSTITUTION", "VAR", "SUBSTITUTION_DETAIL_PREFIX");
        for (Field f : EventTypes.class.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers()) || f.getType() != String.class) continue;
            if (f.getName().startsWith("FILTER_") || notDetails.contains(f.getName())) continue;
            String value = ((String) f.get(null)).toLowerCase(Locale.ROOT);
            assertTrue(covered.contains(value), "EventTypes." + f.getName() + " has no row in ROWS");
        }
    }

    @Test
    void substitutionAccessorsNameTheDirection() throws Exception {
        List<FixtureEvent> events = parse(List.of(
                ROWS.stream().filter(r -> r.label.equals("real subst")).findFirst().get().json,
                ROWS.stream().filter(r -> r.label.equals("real Missed Penalty")).findFirst().get().json));

        FixtureEvent subst = events.get(0);
        assertEquals("Antony", subst.getPlayerOut().getName());
        assertEquals(745, subst.getPlayerIn().getId());
        assertEquals("Isco", subst.getPlayerIn().getName());

        FixtureEvent missed = events.get(1);
        assertNull(missed.getPlayerOut());
        assertNull(missed.getPlayerIn());
        assertEquals("Isco", missed.getPlayer().getName());
    }

    @Test
    void countingGoalsFromRealEventsMatchesTheApiScore() throws Exception {
        // real event list of fixture 1644363 at 21:39:30 (goals and cards only; the API's own score was 2-3)
        String nav = "{\"id\":5271,\"name\":\"Navalcarnero\",\"logo\":\"https://media.api-sports.io/football/teams/5271.png\"}";
        List<FixtureEvent> events = parse(List.of(
                "{\"time\":{\"elapsed\":5},\"team\":" + BETIS + ",\"player\":{\"id\":136117,\"name\":\"R. Riquelme\"},\"assist\":{\"id\":47119,\"name\":\"A. Ruibal\"},\"type\":\"Goal\",\"detail\":\"Normal Goal\"}",
                "{\"time\":{\"elapsed\":49},\"team\":" + nav + ",\"player\":{\"id\":644022,\"name\":\"A. Leis\"},\"assist\":{},\"type\":\"Goal\",\"detail\":\"Normal Goal\"}",
                "{\"time\":{\"elapsed\":79},\"team\":" + nav + ",\"player\":{\"id\":560951,\"name\":\"K. Vazquez\"},\"assist\":{\"id\":644022,\"name\":\"A. Leis\"},\"type\":\"Goal\",\"detail\":\"Normal Goal\"}",
                "{\"time\":{\"elapsed\":90,\"extra\":2},\"team\":" + BETIS + ",\"player\":{\"id\":400949,\"name\":\"R. Marina\"},\"assist\":{\"id\":47119,\"name\":\"A. Ruibal\"},\"type\":\"Goal\",\"detail\":\"Normal Goal\"}",
                "{\"time\":{\"elapsed\":96},\"team\":" + BETIS + ",\"player\":{\"id\":136117,\"name\":\"R. Riquelme\"},\"assist\":{},\"type\":\"Goal\",\"detail\":\"Normal Goal\"}",
                "{\"time\":{\"elapsed\":117},\"team\":" + BETIS + ",\"player\":{\"id\":745,\"name\":\"Isco\"},\"assist\":{\"id\":745,\"name\":\"Isco\"},\"type\":\"Goal\",\"detail\":\"Missed Penalty\"}"));

        int home = 0, away = 0;
        for (FixtureEvent e : events) {
            if (!e.isGoal()) continue;
            if (e.getTeam().getId() == 5271) home++;
            else away++;
        }
        assertEquals(2, home);
        assertEquals(3, away); // the missed penalty does not make it 2-4
    }
}

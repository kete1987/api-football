package es.com.kete1987.apifootball.library;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import es.com.kete1987.apifootball.library.competition.model.CompetitionStructure;
import es.com.kete1987.apifootball.library.competition.model.RoundName;
import es.com.kete1987.apifootball.library.competition.model.Stage;
import es.com.kete1987.apifootball.library.competition.util.Groups;
import es.com.kete1987.apifootball.library.competition.util.StageStatuses;
import es.com.kete1987.apifootball.library.competition.util.StageTypes;
import es.com.kete1987.apifootball.library.fixtures.model.Fixture;
import es.com.kete1987.apifootball.library.fixtures.model.RoundDates;
import es.com.kete1987.apifootball.library.leagues.model.Standings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link RoundName}, {@link StageTypes}, {@link CompetitionStructure} and {@link Groups} on real
 * responses: Champions League 2025-26 (rounds with dates and fixtures), World Cup 2026 (rounds,
 * fixtures, standings) and LaLiga 2026-27 (rounds), plus every round text of the other fixtures.
 */
class CompetitionStructureTest extends BaseApiTest {

    /**
     * Truth table: every round text found in a real response (the fixtures of this repository and
     * the migration samples) -> stage name, matchday, name-based stage type.
     */
    private static final Object[][] ROUND_TRUTH_TABLE = {
            {"1st Qualifying Round", "1st Qualifying Round", null, StageTypes.QUALIFYING},
            {"2nd Qualifying Round", "2nd Qualifying Round", null, StageTypes.QUALIFYING},
            {"3rd Qualifying Round", "3rd Qualifying Round", null, StageTypes.QUALIFYING},
            {"Play-offs", "Play-offs", null, StageTypes.KNOCKOUT},   // QUALIFYING by position in the CL
            {"League Stage - 1", "League Stage", 1, StageTypes.LEAGUE},
            {"League Stage - 8", "League Stage", 8, StageTypes.LEAGUE},
            {"Regular Season - 1", "Regular Season", 1, StageTypes.LEAGUE},
            {"Regular Season - 38", "Regular Season", 38, StageTypes.LEAGUE},
            {"Group Stage - 1", "Group Stage", 1, StageTypes.GROUP},
            {"Group Stage - 3", "Group Stage", 3, StageTypes.GROUP},
            {"Group Stage", "Group Stage", null, StageTypes.GROUP},
            {"Round of 32", "Round of 32", null, StageTypes.KNOCKOUT},
            {"Round of 16", "Round of 16", null, StageTypes.KNOCKOUT},
            {"Quarter-finals", "Quarter-finals", null, StageTypes.KNOCKOUT},
            {"Semi-finals", "Semi-finals", null, StageTypes.KNOCKOUT},
            {"3rd Place Final", "3rd Place Final", null, StageTypes.KNOCKOUT},
            {"Final", "Final", null, StageTypes.KNOCKOUT},
            {"Apertura - 16", "Apertura", 16, StageTypes.LEAGUE},
            {"Apertura - 20", "Apertura", 20, StageTypes.LEAGUE},
            {"Clausura - Quadrangular - 4", "Clausura - Quadrangular", 4, StageTypes.LEAGUE},
            {"League A - 5", "League A", 5, StageTypes.LEAGUE},
            {"Friendly International", "Friendly International", null, StageTypes.UNKNOWN},
    };

    /** Never seen in a response: answered by the documented rules (StageTypes Javadoc). */
    private static final Object[][] UNSEEN_ROUNDS = {
            {"Preliminary Round", "Preliminary Round", null, StageTypes.QUALIFYING},
            {"Group A", "Group A", null, StageTypes.GROUP},
            {"1st Round", "1st Round", null, StageTypes.KNOCKOUT},
            {"Round 2", "Round 2", null, StageTypes.KNOCKOUT},
            {"8th Finals", "8th Finals", null, StageTypes.KNOCKOUT},
            {"Promotion Play-offs", "Promotion Play-offs", null, StageTypes.KNOCKOUT},
            {"Knockout Round Play-offs", "Knockout Round Play-offs", null, StageTypes.KNOCKOUT},
            {"Championship Round - 3", "Championship Round", 3, StageTypes.LEAGUE},
            {"Relegation Round - 2", "Relegation Round", 2, StageTypes.LEAGUE},
            {"  Regular Season -  7 ", "Regular Season", 7, StageTypes.LEAGUE},
            {"Season - 99999999999", "Season - 99999999999", null, StageTypes.UNKNOWN},
    };

    private FixturesApi fixtures;
    private LeaguesApi leagues;

    @BeforeEach
    void setUp() {
        fixtures = new FixturesApi(client, baseUrl, tracker);
        leagues = new LeaguesApi(client, baseUrl, tracker);
    }

    // ------------------------------------------------------------------ round names

    @Test
    void roundName_truthTable() {
        for (Object[][] table : new Object[][][] {ROUND_TRUTH_TABLE, UNSEEN_ROUNDS}) {
            for (Object[] row : table) {
                String round = (String) row[0];
                RoundName name = RoundName.parse(round);
                assertEquals(round, name.getRound());
                assertEquals(row[1], name.getStage(), round);
                assertEquals(row[2], name.getMatchday(), round);
                assertEquals(row[2] != null, name.hasMatchday(), round);
                assertEquals(row[3], name.getStageType(), round);
                assertEquals(row[3], StageTypes.ofRound(round), round);
                assertEquals(row[1], RoundName.stageOf(round), round);
            }
        }
        assertNull(RoundName.parse(null));
        assertNull(RoundName.parse("  "));
        assertNull(RoundName.stageOf(null));
        assertEquals(StageTypes.UNKNOWN, StageTypes.ofRound(null));
        assertEquals(StageTypes.UNKNOWN, StageTypes.ofStageName(null, true));
    }

    /** Like FixtureEventSemanticsTest: every round text in every fixture file must have a row. */
    @Test
    void roundName_everyRoundTextInTheFixturesHasATruthTableRow() throws IOException, URISyntaxException {
        // numbered rounds are covered by their stage: "Regular Season - 13" by the "Regular Season" rows
        Map<String, String> typeOfStage = new LinkedHashMap<>();
        for (Object[] row : ROUND_TRUTH_TABLE) typeOfStage.put((String) row[1], (String) row[3]);
        Set<String> found = new TreeSet<>();
        Path dir = Paths.get(getClass().getClassLoader().getResource("fixtures").toURI());
        List<Path> files;
        try (Stream<Path> list = Files.list(dir)) {
            files = list.filter(p -> p.toString().endsWith(".json")).collect(Collectors.toList());
        }
        for (Path file : files) {
            JsonElement root = JsonParser.parseString(new String(Files.readAllBytes(file), "UTF-8"));
            boolean roundsEndpoint = file.getFileName().toString().startsWith("fixtures_rounds");
            collectRounds(root, roundsEndpoint, found);
        }
        assertEquals(65, found.size());   // 38 Regular Season + 8 League Stage + 3 Group Stage + 16 others
        Set<String> missing = new TreeSet<>();
        Set<String> usedStages = new TreeSet<>();
        for (String round : found) {
            RoundName name = RoundName.parse(round);
            if (!typeOfStage.containsKey(name.getStage())) {
                missing.add(round);
                continue;
            }
            usedStages.add(name.getStage());
            assertEquals(typeOfStage.get(name.getStage()), name.getStageType(), round);
        }
        assertTrue(missing.isEmpty(), "round texts without a truth-table row: " + missing);
        Set<String> unused = new TreeSet<>(typeOfStage.keySet());
        unused.removeAll(usedStages);
        assertTrue(unused.isEmpty(), "truth-table rows not found in any fixture: " + unused);
    }

    private static void collectRounds(JsonElement element, boolean roundsEndpoint, Set<String> found) {
        if (element.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                if (entry.getKey().equals("round") && entry.getValue().isJsonPrimitive()) {
                    found.add(entry.getValue().getAsString());
                } else if (roundsEndpoint && entry.getKey().equals("response") && entry.getValue().isJsonArray()) {
                    for (JsonElement item : entry.getValue().getAsJsonArray()) {
                        if (item.isJsonPrimitive()) found.add(item.getAsString());
                        else collectRounds(item, false, found);
                    }
                } else {
                    collectRounds(entry.getValue(), roundsEndpoint, found);
                }
            }
        } else if (element.isJsonArray()) {
            for (JsonElement item : element.getAsJsonArray()) collectRounds(item, roundsEndpoint, found);
        }
    }

    // ------------------------------------------------------------------ stages

    private CompetitionStructure championsLeagueWithDates() throws Exception {
        enqueue("fixtures_rounds_dates_ucl_2025.json");
        List<RoundDates> rounds = fixtures.getRoundsWithDates(2, 2025);
        assertEquals("/fixtures/rounds?league=2&season=2025&dates=true", takeRequest().getPath());
        return CompetitionStructure.fromRoundDates(rounds);
    }

    @Test
    void championsLeague_tenStagesWithTypesAndDates() throws Exception {
        CompetitionStructure cl = championsLeagueWithDates();

        assertEquals(17, cl.getRounds().size());
        List<String> names = new ArrayList<>();
        List<String> types = new ArrayList<>();
        for (Stage stage : cl.getStages()) {
            names.add(stage.getName());
            types.add(stage.getType());
            assertEquals(names.size() - 1, stage.getOrder());
        }
        // Sportmonks has 10 stages for this season too
        assertEquals(Arrays.asList("1st Qualifying Round", "2nd Qualifying Round", "3rd Qualifying Round",
                "Play-offs", "League Stage", "Round of 32", "Round of 16", "Quarter-finals", "Semi-finals",
                "Final"), names);
        assertEquals(Arrays.asList(StageTypes.QUALIFYING, StageTypes.QUALIFYING, StageTypes.QUALIFYING,
                StageTypes.QUALIFYING, StageTypes.LEAGUE, StageTypes.KNOCKOUT, StageTypes.KNOCKOUT,
                StageTypes.KNOCKOUT, StageTypes.KNOCKOUT, StageTypes.KNOCKOUT), types);

        Stage league = cl.getStage("League Stage");
        assertTrue(league.isTablePhase());
        assertFalse(league.isElimination());
        assertEquals(8, league.getRounds().size());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8), league.getMatchdays());
        assertEquals(LocalDate.of(2025, 9, 16), league.getFirstDate());
        assertEquals(LocalDate.of(2026, 1, 28), league.getLastDate());
        assertEquals(league, cl.getStageOfRound("League Stage - 5"));

        Stage playOffs = cl.getStage("Play-offs");      // August play-offs: qualifying by position
        assertTrue(playOffs.isElimination());
        assertEquals(LocalDate.of(2025, 8, 19), playOffs.getFirstDate());
        assertEquals(LocalDate.of(2025, 8, 27), playOffs.getLastDate());
        assertTrue(cl.getStage("Final").getMatchdays().isEmpty());
        assertEquals(LocalDate.of(2026, 5, 30), cl.getStage("Final").getLastDate());

        assertEquals(4, cl.getStages(StageTypes.QUALIFYING).size());
        assertEquals(5, cl.getStages(StageTypes.KNOCKOUT).size());
        assertNull(cl.getStage("Group Stage"));
        assertNull(cl.getStageOfRound(null));
    }

    @Test
    void championsLeague_stageStatusRelativeToADate() throws Exception {
        CompetitionStructure cl = championsLeagueWithDates();
        Stage league = cl.getStage("League Stage");
        Stage r32 = cl.getStage("Round of 32");
        Stage r16 = cl.getStage("Round of 16");

        LocalDate betweenLegs = LocalDate.of(2026, 2, 20);   // R32 first legs 17-18 Feb, second 24-25 Feb
        assertEquals(StageStatuses.FINISHED, league.getStatus(betweenLegs));
        assertEquals(StageStatuses.CURRENT, r32.getStatus(betweenLegs));
        assertEquals(StageStatuses.UPCOMING, r16.getStatus(betweenLegs));
        assertTrue(r32.isCurrent(betweenLegs));
        assertTrue(league.isFinished(betweenLegs));
        assertTrue(r16.isUpcoming(betweenLegs));
        assertEquals(r32, cl.getActiveStage(betweenLegs));

        // boundaries are inclusive
        assertTrue(r32.isCurrent(LocalDate.of(2026, 2, 17)));
        assertTrue(r32.isCurrent(LocalDate.of(2026, 2, 25)));
        assertTrue(r32.isFinished(LocalDate.of(2026, 2, 26)));

        // in a break, the active stage is the next one; after the final there is none
        assertEquals(r16, cl.getActiveStage(LocalDate.of(2026, 3, 1)));
        assertEquals("1st Qualifying Round", cl.getActiveStage(LocalDate.of(2025, 6, 1)).getName());
        assertNull(cl.getActiveStage(LocalDate.of(2026, 11, 13)));
        assertNull(cl.getActiveStage(null));
        assertEquals(StageStatuses.UNKNOWN, r32.getStatus(null));
    }

    @Test
    void worldCup_stagesWithoutDates() throws Exception {
        enqueue("fixtures_rounds_world_cup_2026.json");
        List<String> rounds = fixtures.getRounds(1, 2026);
        assertEquals("/fixtures/rounds?league=1&season=2026", takeRequest().getPath());
        CompetitionStructure wc = CompetitionStructure.fromRounds(rounds);

        List<String> names = new ArrayList<>();
        for (Stage stage : wc.getStages()) names.add(stage.getName() + "/" + stage.getType());
        assertEquals(Arrays.asList("Group Stage/GROUP", "Round of 32/KNOCKOUT", "Round of 16/KNOCKOUT",
                "Quarter-finals/KNOCKOUT", "Semi-finals/KNOCKOUT", "3rd Place Final/KNOCKOUT", "Final/KNOCKOUT"),
                names);
        Stage groups = wc.getStage("Group Stage");
        assertEquals(Arrays.asList(1, 2, 3), groups.getMatchdays());
        assertNull(groups.getFirstDate());
        assertEquals(StageStatuses.UNKNOWN, groups.getStatus(LocalDate.of(2026, 6, 20)));
        assertFalse(groups.isCurrent(LocalDate.of(2026, 6, 20)));
        assertNull(wc.getActiveStage(LocalDate.of(2026, 6, 20)));
    }

    @Test
    void laLiga_oneStageInTheApiOrderWhichIsNotNumeric() throws Exception {
        enqueue("fixtures_rounds_laliga_2026.json");
        CompetitionStructure liga = CompetitionStructure.fromRounds(fixtures.getRounds(140, 2026));

        assertEquals(1, liga.getStages().size());
        Stage season = liga.getStages().get(0);
        assertEquals("Regular Season", season.getName());
        assertEquals(StageTypes.LEAGUE, season.getType());
        assertEquals(38, season.getRounds().size());
        assertEquals(Arrays.asList(1, 2, 3, 6, 4, 5, 7), season.getMatchdays().subList(0, 7));
        assertEquals("Regular Season - 6", liga.getRounds().get(3));
    }

    @Test
    void structure_missingDataNeverThrows() {
        assertTrue(CompetitionStructure.fromRounds(null).getStages().isEmpty());
        assertTrue(CompetitionStructure.fromRoundDates(null).getStages().isEmpty());
        CompetitionStructure s = CompetitionStructure.fromRounds(Arrays.asList(null, "", "Final", "Final"));
        assertEquals(Collections.singletonList("Final"), s.getRounds());
        assertTrue(s.groupByRound(null).isEmpty());
        assertTrue(s.groupByStage(null).isEmpty());
        assertNull(s.getStage(null));
    }

    // ------------------------------------------------------------------ fixtures by round / stage

    @Test
    void championsLeague_groupByRoundFollowsTheOfficialOrder() throws Exception {
        CompetitionStructure cl = championsLeagueWithDates();
        enqueue("fixtures_by_league_ucl_2025.json");
        List<Fixture> all = fixtures.getFixturesByLeague(2, 2025);
        takeRequest();
        Collections.shuffle(all = new ArrayList<>(all), new java.util.Random(7));

        Map<String, List<Fixture>> byRound = cl.groupByRound(all);
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Map.Entry<String, List<Fixture>> entry : byRound.entrySet()) {
            counts.put(entry.getKey(), entry.getValue().size());
            List<Fixture> list = entry.getValue();
            for (int i = 1; i < list.size(); i++) {
                assertTrue(list.get(i - 1).getFixture().getTimestamp() <= list.get(i).getFixture().getTimestamp());
            }
        }
        Map<String, Integer> expected = new LinkedHashMap<>();
        expected.put("1st Qualifying Round", 28);
        expected.put("2nd Qualifying Round", 30);
        expected.put("3rd Qualifying Round", 20);
        expected.put("Play-offs", 14);
        expected.put("League Stage - 1", 2);   // the fixture file keeps 2 of 18 per kept matchday
        expected.put("League Stage - 8", 2);
        expected.put("Round of 32", 16);
        expected.put("Round of 16", 16);
        expected.put("Quarter-finals", 8);
        expected.put("Semi-finals", 4);
        expected.put("Final", 1);
        assertEquals(expected, counts);

        Map<String, List<Fixture>> byStage = cl.groupByStage(all);
        assertEquals(Arrays.asList("1st Qualifying Round", "2nd Qualifying Round", "3rd Qualifying Round",
                "Play-offs", "League Stage", "Round of 32", "Round of 16", "Quarter-finals", "Semi-finals",
                "Final"), new ArrayList<>(byStage.keySet()));
        assertEquals(4, byStage.get("League Stage").size());
    }

    @Test
    void groupByRound_keepsFixturesOfUnknownRoundsAtTheEnd() throws Exception {
        enqueue("fixtures_rounds_world_cup_2026.json");
        CompetitionStructure wc = CompetitionStructure.fromRounds(fixtures.getRounds(1, 2026));
        enqueue("fixtures_by_league_ucl_2025.json");
        List<Fixture> cl = fixtures.getFixturesByLeague(2, 2025);

        Map<String, List<Fixture>> byRound = wc.groupByRound(cl);
        // the WC rounds that the CL shares come first in the WC order, then the CL-only rounds
        assertEquals(Arrays.asList("Round of 32", "Round of 16", "Quarter-finals", "Semi-finals", "Final",
                "1st Qualifying Round", "2nd Qualifying Round", "3rd Qualifying Round", "Play-offs",
                "League Stage - 1", "League Stage - 8"), new ArrayList<>(byRound.keySet()));
        int total = 0;
        for (List<Fixture> list : byRound.values()) total += list.size();
        assertEquals(cl.size(), total);
    }

    @Test
    void groupByRound_fixtureWithoutRoundGoesUnderNullAtTheEnd() throws Exception {
        // edited: a real World Cup final with its league.round removed
        JsonObject envelope = JsonParser.parseString(load("fixtures_by_league_world_cup_2026.json")).getAsJsonObject();
        JsonArray response = envelope.getAsJsonArray("response");
        JsonObject last = response.get(response.size() - 1).getAsJsonObject().deepCopy();
        last.getAsJsonObject("league").remove("round");
        JsonArray two = new JsonArray();
        two.add(last);
        two.add(response.get(0));
        envelope.add("response", two);
        enqueueBody(envelope.toString(), 200);
        List<Fixture> parsed = fixtures.getFixturesByLeague(1, 2026);

        Map<String, List<Fixture>> byRound = CompetitionStructure.fromRounds(Collections.singletonList("Group Stage - 1"))
                .groupByRound(parsed);
        assertEquals(Arrays.asList("Group Stage - 1", null), new ArrayList<>(byRound.keySet()));
    }

    @Test
    void worldCup_groupByRoundAndStage() throws Exception {
        enqueue("fixtures_rounds_world_cup_2026.json");
        CompetitionStructure wc = CompetitionStructure.fromRounds(fixtures.getRounds(1, 2026));
        enqueue("fixtures_by_league_world_cup_2026.json");
        List<Fixture> all = fixtures.getFixturesByLeague(1, 2026);

        List<Integer> sizes = new ArrayList<>();
        for (List<Fixture> list : wc.groupByRound(all).values()) sizes.add(list.size());
        assertEquals(Arrays.asList(24, 24, 24, 16, 8, 4, 2, 1, 1), sizes);
        assertEquals(new ArrayList<>(wc.getRounds()), new ArrayList<>(wc.groupByRound(all).keySet()));
        assertEquals(72, wc.groupByStage(all).get("Group Stage").size());
    }

    // ------------------------------------------------------------------ groups of fixtures

    @Test
    void worldCup_everyGroupMatchMapsToExactlyOneGroup() throws Exception {
        enqueue("standings_world_cup_2026.json");
        Standings standings = leagues.getStandings(1, 2026);
        enqueue("fixtures_by_league_world_cup_2026.json");
        List<Fixture> all = fixtures.getFixturesByLeague(1, 2026);

        Map<String, List<Fixture>> byGroup = Groups.byGroup(standings, all);
        assertEquals(12, byGroup.size());
        assertEquals("Group A", byGroup.keySet().iterator().next());
        int total = 0;
        for (Map.Entry<String, List<Fixture>> entry : byGroup.entrySet()) {
            assertEquals(6, entry.getValue().size(), entry.getKey());   // 4 teams, single round-robin
            for (Fixture f : entry.getValue()) assertTrue(f.getLeague().getRound().startsWith("Group Stage - "));
            total += entry.getValue().size();
        }
        assertEquals(72, total);
        for (Fixture f : all) {
            boolean group = f.getLeague().getRound().startsWith("Group Stage");
            assertEquals(group, Groups.groupOf(standings, f) != null, f.getLeague().getRound());
        }
        assertNull(Groups.groupOf(null, all.get(0)));
        assertNull(Groups.groupOf(standings, null));
        assertTrue(Groups.byGroup(null, all).isEmpty());
    }
}

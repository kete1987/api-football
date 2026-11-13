package es.com.kete1987.apifootball.library;

import es.com.kete1987.apifootball.library.leagues.model.League;
import es.com.kete1987.apifootball.library.leagues.model.Season;
import es.com.kete1987.apifootball.library.leagues.model.StandingEntry;
import es.com.kete1987.apifootball.library.leagues.model.Standings;
import es.com.kete1987.apifootball.library.leagues.util.StandingZones;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Standings structure helpers (group tables vs cross-group rankings, zones) and season dates, on
 * real responses: World Cup 2026, Champions League 2025-26, LaLiga 2026-27 and the existing
 * {@code standings_*.json} fixtures.
 */
class StandingsStructureTest extends BaseApiTest {

    private LeaguesApi api;

    @BeforeEach
    void setUp() {
        api = new LeaguesApi(client, baseUrl, tracker);
    }

    private Standings standings(String fixture, int league, int season) throws Exception {
        enqueue(fixture);
        Standings standings = api.getStandings(league, season);
        assertEquals("/standings?league=" + league + "&season=" + season, takeRequest().getPath());
        return standings;
    }

    // ------------------------------------------------------------------ group tables vs rankings

    @Test
    void worldCup_separatesTwelveGroupsFromTheThirdPlacedRanking() throws Exception {
        Standings wc = standings("standings_world_cup_2026.json", 1, 2026);

        assertEquals(13, wc.getTables().size());
        List<List<StandingEntry>> groups = wc.getGroupTables();
        assertEquals(12, groups.size());
        List<String> labels = new ArrayList<>();
        for (List<StandingEntry> table : groups) {
            assertEquals(4, table.size());
            labels.add(table.get(0).getGroup());
        }
        assertEquals(Arrays.asList("Group A", "Group B", "Group C", "Group D", "Group E", "Group F",
                "Group G", "Group H", "Group I", "Group J", "Group K", "Group L"), labels);

        assertEquals(1, wc.getCrossGroupRankings().size());
        List<StandingEntry> thirds = wc.getCrossGroupRanking();
        assertEquals(wc.getTables().get(12), thirds);
        assertEquals(12, thirds.size());
        assertEquals("Group Stage", thirds.get(0).getGroup());
        assertEquals("Congo DR", thirds.get(0).getTeam().getName());
        assertEquals("Round of 32", thirds.get(7).getDescription()); // Senegal, 8th: last qualifier
        assertNull(thirds.get(8).getDescription());                  // Iran, 9th: out

        // every team of the ranking is the 3rd of its own group, and the 12 groups are all different
        Set<String> sourceGroups = new LinkedHashSet<>();
        for (StandingEntry third : thirds) {
            String group = wc.getGroupOf(third.getTeam().getId());
            sourceGroups.add(group);
            assertEquals(third.getTeam().getId(), wc.getTable(group).get(2).getTeam().getId());
        }
        assertEquals(12, sourceGroups.size());
    }

    @Test
    void worldCup_groupOfTeamIgnoresTheRanking() throws Exception {
        Standings wc = standings("standings_world_cup_2026.json", 1, 2026);

        assertEquals("Group H", wc.getGroupOf(9));     // Spain
        assertEquals("Group H", wc.getGroupOf(7));     // Uruguay: also in the ranking, 12th
        assertEquals("Group A", wc.getGroupOf(wc.getTable("Group A").get(0).getTeam().getId()));
        assertNull(wc.getGroupOf(-1));
    }

    @Test
    void championsLeague_singleLeaguePhaseTableIsAGroupTable() throws Exception {
        Standings cl = standings("standings_ucl_2025.json", 2, 2025);

        assertEquals(1, cl.getGroupTables().size());
        assertEquals(36, cl.getGroupTables().get(0).size());
        assertEquals("UEFA Champions League", cl.getGroupTables().get(0).get(0).getGroup());
        assertTrue(cl.getCrossGroupRankings().isEmpty());
        assertTrue(cl.getCrossGroupRanking().isEmpty());
    }

    @Test
    void laLiga_singleTableIsAGroupTable() throws Exception {
        Standings liga = standings("standings_laliga_2026.json", 140, 2026);

        assertEquals(1, liga.getGroupTables().size());
        assertEquals(20, liga.getGroupTables().get(0).size());
        assertTrue(liga.getCrossGroupRankings().isEmpty());
    }

    @Test
    void existingFixtures_haveNoCrossGroupRanking() throws Exception {
        Standings groups = standings("standings_groups.json", 1, 2026); // 5 trimmed World Cup groups
        assertEquals(5, groups.getGroupTables().size());
        assertTrue(groups.getCrossGroupRankings().isEmpty());

        Standings league = standings("standings_league.json", 140, 2026);
        assertEquals(1, league.getGroupTables().size());
        assertTrue(league.getCrossGroupRankings().isEmpty());
    }

    // ------------------------------------------------------------------ zones

    /**
     * Truth table over every {@code description} found in real standings (LaLiga 2026-27, Champions
     * League 2025-26 and 2026-27, World Cup 2026), plus {@code null}.
     */
    @Test
    void zones_truthTableOverEveryObservedDescription() throws Exception {
        // description -> {category, target, isPromotion, isRelegation}
        Map<String, Object[]> expected = new LinkedHashMap<>();
        expected.put("Promotion - Champions League (League phase)",
                new Object[] {"Promotion", "Champions League (League phase)", true, false});
        expected.put("Promotion - Europa League (League phase)",
                new Object[] {"Promotion", "Europa League (League phase)", true, false});
        expected.put("Promotion - Conference League (Qualification)",
                new Object[] {"Promotion", "Conference League (Qualification)", true, false});
        expected.put("Promotion - Champions League (Play Offs: 1/8-finals)",
                new Object[] {"Promotion", "Champions League (Play Offs: 1/8-finals)", true, false});
        expected.put("Promotion - Champions League (Play Offs: 1/16-finals)",
                new Object[] {"Promotion", "Champions League (Play Offs: 1/16-finals)", true, false});
        expected.put("Relegation - LaLiga2", new Object[] {"Relegation", "LaLiga2", false, true});
        expected.put("Round of 32", new Object[] {null, null, false, false});
        expected.put(null, new Object[] {null, null, false, false});

        Set<String> seen = new LinkedHashSet<>();
        String[][] sources = {
                {"standings_laliga_2026.json", "140", "2026"},
                {"standings_ucl_2025.json", "2", "2025"},
                {"standings_world_cup_2026.json", "1", "2026"},
                {"standings_league.json", "140", "2026"},
                {"standings_groups.json", "1", "2026"}};
        for (String[] source : sources) {
            Standings standings = standings(source[0], Integer.parseInt(source[1]), Integer.parseInt(source[2]));
            for (List<StandingEntry> table : standings.getTables()) {
                for (StandingEntry row : table) {
                    String description = row.getDescription();
                    seen.add(description);
                    Object[] row4 = expected.get(description);
                    assertTrue(expected.containsKey(description), "no truth-table row for " + description);
                    assertEquals(row4[0], row.getZoneCategory(), description);
                    assertEquals(row4[1], row.getZoneTarget(), description);
                    assertEquals(row4[2], row.isPromotion(), description);
                    assertEquals(row4[3], row.isRelegation(), description);
                }
            }
        }
        // the 2026-27 Champions League response (not a fixture) has the same two descriptions as 2025-26
        assertEquals(expected.keySet(), seen);
        assertEquals("Promotion", StandingZones.PROMOTION);
        assertEquals("Relegation", StandingZones.RELEGATION);
    }

    @Test
    void zones_laLigaRows() throws Exception {
        List<StandingEntry> table = standings("standings_laliga_2026.json", 140, 2026).getTable();

        int promotion = 0;
        int relegation = 0;
        for (StandingEntry row : table) {
            if (row.isPromotion()) promotion++;
            if (row.isRelegation()) relegation++;
        }
        assertEquals(6, promotion);   // 4 Champions League, 1 Europa League, 1 Conference League
        assertEquals(3, relegation);  // 18th to 20th
        assertEquals("Conference League (Qualification)", table.get(5).getZoneTarget());
    }

    // ------------------------------------------------------------------ seasons

    @Test
    void season_worldCupIsCurrentButOver() throws Exception {
        enqueue("leagues_by_id_world_cup.json");
        League wc = api.getLeagueById(1);
        assertEquals("/leagues?id=1", takeRequest().getPath());

        Season s2026 = wc.getCurrentSeason();
        assertEquals(2026, s2026.getYear());
        assertTrue(s2026.getCurrent());
        assertEquals(LocalDate.of(2026, 6, 11), s2026.getStartDate());
        assertEquals(LocalDate.of(2026, 7, 19), s2026.getEndDate());

        LocalDate today = LocalDate.of(2026, 11, 13);   // the day the response was captured
        assertTrue(s2026.hasStarted(today));
        assertTrue(s2026.isOver(today));
        assertFalse(s2026.isInProgress(today));
        assertNull(wc.getSeasonInProgress(today));

        assertFalse(s2026.isOver(LocalDate.of(2026, 7, 19)));        // final day
        assertTrue(s2026.isInProgress(LocalDate.of(2026, 7, 19)));
        assertFalse(s2026.hasStarted(LocalDate.of(2026, 6, 10)));
        assertTrue(s2026.hasStarted(LocalDate.of(2026, 6, 11)));
        assertEquals(2026, wc.getSeasonInProgress(LocalDate.of(2026, 6, 11)).getYear());
        assertFalse(s2026.isOver(null));
        assertFalse(s2026.hasStarted(null));
    }

    @Test
    void season_championsLeagueEndIsTheLastKnownFixture() throws Exception {
        enqueue("leagues_by_id_ucl.json");
        League cl = api.getLeagueById(2);

        Season s2025 = cl.getSeason(2025);
        assertFalse(s2025.getCurrent());
        assertTrue(s2025.isOver(LocalDate.of(2026, 11, 13)));

        Season s2026 = cl.getSeason(2026);
        assertTrue(s2026.getCurrent());
        // "end" is the last scheduled league-phase date, not the final in May 2027
        assertEquals(LocalDate.of(2027, 1, 27), s2026.getEndDate());
        assertTrue(s2026.isInProgress(LocalDate.of(2026, 11, 13)));
        assertSame(s2026, cl.getSeasonInProgress(LocalDate.of(2026, 11, 13)));
        assertNull(cl.getSeasonInProgress(LocalDate.of(2026, 6, 15)));  // between two seasons
    }
}

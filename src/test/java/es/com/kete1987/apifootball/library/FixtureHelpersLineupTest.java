package es.com.kete1987.apifootball.library;

import es.com.kete1987.apifootball.library.common.util.MediaUrls;
import es.com.kete1987.apifootball.library.fixtures.model.Fixture;
import es.com.kete1987.apifootball.library.fixtures.model.FixtureLineup;
import es.com.kete1987.apifootball.library.fixtures.model.FixtureLineup.LineupPlayer;
import es.com.kete1987.apifootball.library.fixtures.model.FixtureStatistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lineup grid, media URLs and lookups inside a fixture. The Italy-France lineups are the real values
 * recorded on 2026-11-12 (API-Football fixture 1528960, Sportmonks 19676662), reduced to the fields the
 * helpers read; the expected Sportmonks columns are the {@code formation_field} values of that capture,
 * matched by shirt number. Everything else is hand-written from the documented shape.
 */
class FixtureHelpersLineupTest extends BaseApiTest {

    private FixturesApi api;

    @BeforeEach
    void setUp() {
        api = new FixturesApi(client, baseUrl, tracker);
    }

    private static String entry(int id, String name, int number, String grid) {
        return "{\"player\":{\"id\":" + id + ",\"name\":\"" + name + "\",\"number\":" + number
                + ",\"pos\":\"M\",\"grid\":" + (grid == null ? "null" : "\"" + grid + "\"") + "}}";
    }

    // shirt number -> {API grid, Sportmonks formation_field}, Italy (home, 768), 4-3-3
    private static final Object[][] ITALY = {
            {1622, "G. Donnarumma", 1, "1:1", "1:1"},
            {342022, "M. Kayode", 2, "2:4", "2:1"},
            {289761, "G. Scalvini", 3, "2:3", "2:2"},
            {31009, "A. Bastoni", 6, "2:2", "2:3"},
            {157052, "R. Calafiori", 5, "2:1", "2:4"},
            {31173, "D. Frattesi", 16, "3:3", "3:1"},
            {31146, "S. Tonali", 8, "3:2", "3:2"},
            {30558, "N. Barella", 18, "3:1", "3:3"},
            {215, "S. Esposito", 7, "4:3", "4:1"},
            {345808, "F. Esposito", 9, "4:2", "4:2"},
            {30438, "N. Cambiaghi", 19, "4:1", "4:3"}};

    // France (away, 2), 4-2-3-1: API grid == Sportmonks formation_field
    private static final Object[][] FRANCE = {
            {22221, "M. Maignan", 16, "1:1", "1:1"},
            {161907, "M. Gusto", 2, "2:4", "2:4"},
            {367636, "J. Jacquet", 22, "2:3", "2:3"},
            {1149, "D. Upamecano", 4, "2:2", "2:2"},
            {162267, "A. Truffert", 3, "2:1", "2:1"},
            {162266, "L. da Cunha", 21, "3:2", "3:2"},
            {272, "A. Rabiot", 14, "3:1", "3:1"},
            {19617, "M. Olise", 11, "4:3", "4:3"},
            {156477, "R. Cherki", 7, "4:2", "4:2"},
            {343027, "D. Doue", 20, "4:1", "4:1"},
            {161904, "B. Barcola", 12, "5:1", "5:1"}};

    private static String lineup(int team, String formation, Object[][] xi) {
        StringBuilder sb = new StringBuilder("{\"team\":{\"id\":" + team + ",\"name\":\"T\",\"logo\":\"l\"},"
                + "\"formation\":\"" + formation + "\",\"startXI\":[");
        for (int i = 0; i < xi.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(entry((Integer) xi[i][0], (String) xi[i][1], (Integer) xi[i][2], (String) xi[i][3]));
        }
        return sb.append("],\"substitutes\":[").append(entry(999, "Sub One", 12, null)).append("]}").toString();
    }

    private Fixture italyFrance() throws Exception {
        enqueueBody("{\"errors\":[],\"results\":1,\"paging\":{\"current\":1,\"total\":1},\"response\":[{"
                + "\"teams\":{\"home\":{\"id\":768,\"name\":\"Italy\"},\"away\":{\"id\":2,\"name\":\"France\"}},"
                + "\"lineups\":[" + lineup(768, "4-3-3", ITALY) + "," + lineup(2, "4-2-3-1", FRANCE) + "]}]}", 200);
        Fixture f = api.getFixtureById(1528960);
        takeRequest();
        return f;
    }

    @Test
    void gridRowAndColumn_areParsed() throws Exception {
        LineupPlayer kayode = italyFrance().getHomeLineup().getStartXI().get(1).getPlayer();
        assertEquals("2:4", kayode.getGrid());
        assertEquals(2, kayode.getGridRow());
        assertEquals(4, kayode.getGridColumn());
    }

    @Test
    void grid_isNullSafe() throws Exception {
        FixtureLineup l = italyFrance().getHomeLineup();
        LineupPlayer sub = l.getSubstitutes().get(0).getPlayer();
        assertNull(sub.getGrid());
        assertNull(sub.getGridRow());
        assertNull(sub.getGridColumn());
        assertNull(l.getGridColumnMirrored(sub));
        assertNull(l.getGridColumnMirrored(null));
        for (String bad : new String[]{"", " ", "2", "2:", ":3", "a:b", "2:3:4", "0:1", "1:-1", "2:x"}) {
            LineupPlayer p = new com.google.gson.Gson().fromJson("{\"grid\":\"" + bad + "\"}", LineupPlayer.class);
            assertNull(p.getGridRow(), bad);
            assertNull(p.getGridColumn(), bad);
        }
        LineupPlayer spaced = new com.google.gson.Gson().fromJson("{\"grid\":\" 3 : 2 \"}", LineupPlayer.class);
        assertEquals(3, spaced.getGridRow());
        assertEquals(2, spaced.getGridColumn());
    }

    @Test
    void homeTeam_mirroredColumnEqualsSportmonksFormationField_forAllElevenPlayers() throws Exception {
        Fixture f = italyFrance();
        FixtureLineup italy = f.getHomeLineup();
        assertEquals(11, italy.getStartXI().size());
        assertEquals(1, italy.getPlayersInGridRow(1));
        assertEquals(4, italy.getPlayersInGridRow(2));
        assertEquals(3, italy.getPlayersInGridRow(3));
        assertEquals(3, italy.getPlayersInGridRow(4));
        assertEquals(0, italy.getPlayersInGridRow(5));
        for (int i = 0; i < ITALY.length; i++) {
            LineupPlayer p = italy.getStartXI().get(i).getPlayer();
            String[] sm = ((String) ITALY[i][4]).split(":");
            assertEquals(Integer.parseInt(sm[0]), p.getGridRow(), (String) ITALY[i][1]);
            assertEquals(Integer.parseInt(sm[1]), italy.getGridColumnMirrored(p), (String) ITALY[i][1]);
            assertEquals(Integer.parseInt(sm[1]), f.getGridColumnAsSportmonks(italy, p), (String) ITALY[i][1]);
        }
        // the API grid itself matches Sportmonks for only the central players (3 of 11)
        int same = 0;
        for (int i = 0; i < ITALY.length; i++) {
            if (ITALY[i][3].equals(ITALY[i][4])) same++;
        }
        assertEquals(3, same);
    }

    @Test
    void awayTeam_gridAlreadyEqualsSportmonks_andNeedsNoMirroring() throws Exception {
        Fixture f = italyFrance();
        FixtureLineup france = f.getAwayLineup();
        for (int i = 0; i < FRANCE.length; i++) {
            LineupPlayer p = france.getStartXI().get(i).getPlayer();
            assertEquals(FRANCE[i][4], p.getGridRow() + ":" + p.getGridColumn());
            assertEquals(Integer.parseInt(((String) FRANCE[i][4]).split(":")[1]), f.getGridColumnAsSportmonks(france, p));
        }
    }

    @Test
    void mirroring_isItsOwnInverse_andGoalkeeperNeverMoves() throws Exception {
        FixtureLineup italy = italyFrance().getHomeLineup();
        for (FixtureLineup.Entry e : italy.getStartXI()) {
            LineupPlayer p = e.getPlayer();
            int once = italy.getGridColumnMirrored(p);
            int inRow = italy.getPlayersInGridRow(p.getGridRow());
            assertEquals(p.getGridColumn(), inRow + 1 - once);
        }
        assertEquals(1, italy.getGridColumnMirrored(italy.getStartXI().get(0).getPlayer()));
    }

    @Test
    void gridColumnAsSportmonks_unknownLineupOrPlayerIsNull() throws Exception {
        Fixture f = italyFrance();
        assertNull(f.getGridColumnAsSportmonks(null, f.getHomeLineup().getStartXI().get(0).getPlayer()));
        assertNull(f.getGridColumnAsSportmonks(f.getHomeLineup(), null));
        FixtureLineup stranger = new com.google.gson.Gson().fromJson(
                "{\"team\":{\"id\":77},\"startXI\":[]}", FixtureLineup.class);
        assertNull(f.getGridColumnAsSportmonks(stranger, f.getHomeLineup().getStartXI().get(1).getPlayer()));
        // a player of another lineup has a row the lineup may not know
        assertNull(stranger.getGridColumnMirrored(f.getHomeLineup().getStartXI().get(1).getPlayer()));
        assertEquals(0, stranger.getPlayersInGridRow(2));
    }

    // ------------------------------------------------------------------ lookups

    @Test
    void lineupLookups_byTeamAndSide() throws Exception {
        Fixture f = italyFrance();
        assertEquals(768, f.findLineup(768).getTeam().getId());
        assertEquals(2, f.findLineup(2).getTeam().getId());
        assertEquals("4-3-3", f.getHomeLineup().getFormation());
        assertEquals("4-2-3-1", f.getAwayLineup().getFormation());
        assertNull(f.findLineup(1));
        assertNull(f.findLineup(null));
        assertNull(f.getHomeStatistics());
        assertNull(f.getHomePlayers());
    }

    @Test
    void findPlayerName_searchesStartersThenSubstitutes() throws Exception {
        Fixture f = italyFrance();
        assertEquals("M. Kayode", f.findPlayerName(342022));
        assertEquals("M. Maignan", f.findPlayerName(22221));
        assertEquals("Sub One", f.findPlayerName(999));
        assertNull(f.findPlayerName(1));
        assertNull(f.findPlayerName(null));
    }

    @Test
    void realDetailFixture_lookupsFindStatisticsLineupsAndPlayersByTeam() throws Exception {
        enqueue("fixtures_by_id.json");
        Fixture f = api.getFixtureById(1570443);
        takeRequest();
        int home = f.getTeams().getHome().getId();
        int away = f.getTeams().getAway().getId();
        assertEquals(home, f.getHomeStatistics().getTeam().getId());
        assertEquals(away, f.getAwayStatistics().getTeam().getId());
        assertEquals(home, f.getHomeLineup().getTeam().getId());
        assertEquals(away, f.getAwayPlayers().getTeam().getId());
        assertEquals(f.getPlayers().get(0), f.findPlayers(f.getPlayers().get(0).getTeam().getId()));
        boolean any = false;
        for (var block : f.getPlayers()) {
            for (var entry : block.getPlayers()) {
                String name = f.findPlayerName(entry.getPlayer().getId());
                assertTrue(name != null, "every listed player resolves to a name");
                any = true;
            }
        }
        assertTrue(any);
    }

    @Test
    void statisticsGetIntValue_withDefault() throws Exception {
        enqueue("fixtures_statistics.json");
        List<FixtureStatistics> stats = api.getFixtureStatistics(1);
        takeRequest();
        FixtureStatistics s = stats.get(0);
        Integer shots = s.find("Total Shots").getIntValue();
        assertEquals(shots, s.getIntValue("Total Shots", -1));
        assertEquals(shots, s.getIntValue("total shots"));
        assertEquals(-1, s.getIntValue("No Such Statistic", -1));
        assertEquals(0, s.getIntValue(null, 0));
        assertNull(s.getIntValue("No Such Statistic"));
    }

    @Test
    void statisticsGetIntValue_percentagesNullsAndDecimals() throws Exception {
        enqueueBody("{\"errors\":[],\"results\":1,\"paging\":{\"current\":1,\"total\":1},\"response\":[{"
                + "\"team\":{\"id\":1},\"statistics\":[{\"type\":\"Ball Possession\",\"value\":\"37%\"},"
                + "{\"type\":\"Red Cards\",\"value\":null},{\"type\":\"expected_goals\",\"value\":\"1.71\"},"
                + "{\"type\":\"Corner Kicks\",\"value\":5}]}]}", 200);
        FixtureStatistics s = api.getFixtureStatistics(1).get(0);
        assertEquals(37, s.getIntValue("Ball Possession", 0));
        assertEquals(0, s.getIntValue("Red Cards", 0));
        assertEquals(7, s.getIntValue("Red Cards", 7));
        assertEquals(9, s.getIntValue("expected_goals", 9));
        assertEquals(5, s.getIntValue("Corner Kicks", 0));
    }

    // ------------------------------------------------------------------ media

    @Test
    void mediaUrls_followCatalogSection7() {
        assertEquals("https://media.api-sports.io/football/players/342022.png", MediaUrls.playerPhoto(342022));
        assertEquals("https://media.api-sports.io/football/teams/530.png", MediaUrls.teamLogo(530));
        assertEquals("https://media.api-sports.io/football/leagues/140.png", MediaUrls.leagueLogo(140));
        assertEquals("https://media.api-sports.io/football/venues/1456.png", MediaUrls.venueImage(1456));
        assertEquals("https://media.api-sports.io/football/coachs/2.png", MediaUrls.coachPhoto(2));
        assertEquals("https://media.api-sports.io/flags/es.svg", MediaUrls.countryFlag("ES"));
        assertEquals("https://media.api-sports.io/flags/gb-eng.svg", MediaUrls.countryFlag(" GB-ENG "));
    }

    @Test
    void mediaUrls_nullAndInvalidInputsGiveNull() {
        assertNull(MediaUrls.playerPhoto(null));
        assertNull(MediaUrls.teamLogo(0));
        assertNull(MediaUrls.venueImage(0));
        assertNull(MediaUrls.leagueLogo(-4));
        assertNull(MediaUrls.coachPhoto(null));
        assertNull(MediaUrls.countryFlag(null));
        assertNull(MediaUrls.countryFlag(" "));
        assertNull(MediaUrls.countryFlag("../x"));
        assertNull(MediaUrls.countryFlag("es/ES"));
    }

    @Test
    void mediaUrls_matchTheUrlsOfRealResponses() throws Exception {
        // the existing real fixture carries team logos and player photos: the builders reproduce them
        enqueue("fixtures_by_id.json");
        Fixture f = api.getFixtureById(1570443);
        takeRequest();
        assertEquals(f.getTeams().getHome().getLogo(), MediaUrls.teamLogo(f.getTeams().getHome().getId()));
        assertEquals(f.getLeague().getLogo(), MediaUrls.leagueLogo(f.getLeague().getId()));
        var info = f.getPlayers().get(0).getPlayers().get(0).getPlayer();
        assertEquals(info.getPhoto(), MediaUrls.playerPhoto(info.getId()));
    }

    @Test
    void playerAccessors_buildPhotoFromTheId() throws Exception {
        Fixture f = italyFrance();
        assertEquals("https://media.api-sports.io/football/players/342022.png",
                f.getHomeLineup().getStartXI().get(1).getPlayer().getPhotoUrl());
        assertTrue(f.getHomeLineup().getSubstitutes().get(0).getPlayer().getPhotoUrl().endsWith("/999.png"));
        enqueueBody("{\"errors\":[],\"results\":1,\"paging\":{\"current\":1,\"total\":1},\"response\":[{\"events\":["
                + "{\"time\":{\"elapsed\":1},\"player\":{\"id\":47119,\"name\":\"A. Ruibal\"},\"type\":\"Card\"},"
                + "{\"time\":{\"elapsed\":2},\"player\":{\"name\":\"V. Rodriguez\"},\"type\":\"Card\"}]}]}", 200);
        var ev = api.getFixtureById(1).getEvents();
        assertEquals("https://media.api-sports.io/football/players/47119.png", ev.get(0).getPlayer().getPhotoUrl());
        assertNull(ev.get(1).getPlayer().getPhotoUrl());
    }
}

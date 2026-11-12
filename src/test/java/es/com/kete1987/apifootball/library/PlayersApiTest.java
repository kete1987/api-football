package es.com.kete1987.apifootball.library;

import es.com.kete1987.apifootball.library.common.model.Page;
import es.com.kete1987.apifootball.library.common.util.ApiFootballException;
import es.com.kete1987.apifootball.library.common.util.NumberParsing;
import es.com.kete1987.apifootball.library.players.model.Player;
import es.com.kete1987.apifootball.library.players.model.PlayerProfile;
import es.com.kete1987.apifootball.library.players.model.PlayerStatistics;
import es.com.kete1987.apifootball.library.players.model.PlayerTeam;
import es.com.kete1987.apifootball.library.players.model.PlayerTransfers;
import es.com.kete1987.apifootball.library.players.model.PlayerWithStatistics;
import es.com.kete1987.apifootball.library.players.model.SidelinedGroup;
import es.com.kete1987.apifootball.library.players.model.SidelinedPeriod;
import es.com.kete1987.apifootball.library.players.model.Squad;
import es.com.kete1987.apifootball.library.players.model.Trophy;
import es.com.kete1987.apifootball.library.players.model.TrophyGroup;
import es.com.kete1987.apifootball.library.players.query.PlayerProfilesQuery;
import es.com.kete1987.apifootball.library.players.query.PlayersQuery;
import es.com.kete1987.apifootball.library.players.query.SquadsQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class PlayersApiTest extends BaseApiTest {
    private PlayersApi api;

    @BeforeEach
    void setUp() {
        api = new PlayersApi(client, baseUrl, tracker);
    }

    // ---------------------------------------------------------------- /players

    @Test
    void getPlayerById_parsesEveryLevel() throws Exception {
        enqueue("players_by_id_season.json");

        PlayerWithStatistics result = api.getPlayerById(278, 2026);

        Player p = result.getPlayer();
        assertEquals(278, p.getId());
        assertEquals("Kylian Mbappé", p.getName());
        assertEquals("Mbappé Lottin", p.getLastname());
        assertEquals(27, p.getAge());
        assertEquals("1998-12-20", p.getBirth().getDate());
        assertEquals("Paris", p.getBirth().getPlace());
        assertEquals("178", p.getHeight());
        assertEquals(178, p.getHeightCm());
        assertEquals(75, p.getWeightKg());
        assertFalse(p.getInjured());
        assertNull(p.getNumber());

        assertFalse(result.getStatistics().isEmpty());
        PlayerStatistics s = result.getStatistics().get(0);
        assertEquals("France", s.getTeam().getName());
        assertEquals(1, s.getLeague().getId());
        assertEquals("World Cup", s.getLeague().getName());
        assertNull(s.getLeague().getFlag());
        assertEquals(2026, s.getLeague().getSeason());
        assertEquals(8, s.getGames().getAppearances());
        assertEquals(698, s.getGames().getMinutes());
        assertEquals("8.21", s.getGames().getRating());
        assertEquals(8.21, s.getGames().getRatingValue(), 0.0001);
        assertFalse(s.getGames().getCaptain());
        assertEquals(0, s.getSubstitutes().getIn());
        assertEquals(4, s.getSubstitutes().getOut());
        assertEquals(36, s.getShots().getTotal());
        assertEquals(10, s.getGoals().getTotal());
        assertNull(s.getGoals().getSaves());
        assertEquals("85", s.getPasses().getAccuracy());
        assertEquals(85, s.getPasses().getAccuracyValue());
        assertNull(s.getTackles().getBlocks());
        assertEquals(23, s.getDuels().getWon());
        assertEquals(13, s.getDribbles().getSuccess());
        assertEquals(6, s.getFouls().getCommitted());
        assertEquals(1, s.getCards().getYellow());
        assertEquals(1, s.getPenalty().getScored());
        assertNull(s.getPenalty().getCommitted());
        assertEquals("/players?id=278&season=2026", takeRequest().getPath());
    }

    @Test
    void getPlayersPage_page1() throws Exception {
        enqueue("players_page1.json");

        Page<PlayerWithStatistics> page = api.getPlayersPage(new PlayersQuery().league(140).season(2026), 1);

        assertEquals(20, page.getResults());
        assertEquals(1, page.getPaging().getCurrent());
        assertEquals(31, page.getPaging().getTotal());
        assertTrue(page.hasMore());
        assertEquals(5, page.getItems().size());
        assertEquals("Sergio Gómez", page.getItems().get(0).getPlayer().getName());
        assertEquals("/players?league=140&season=2026&page=1", takeRequest().getPath());
    }

    @Test
    void getPlayersPage_page2() throws Exception {
        enqueue("players_page2.json");

        Page<PlayerWithStatistics> page = api.getPlayersByLeaguePage(140, 2026, 2);

        assertEquals(2, page.getPaging().getCurrent());
        assertEquals(5, page.getItems().size());
        assertEquals("/players?league=140&season=2026&page=2", takeRequest().getPath());
    }

    @Test
    void getPlayersPage_outOfRangeIsEmptyNotError() throws Exception {
        enqueue("players_page_out_of_range.json");

        Page<PlayerWithStatistics> page = api.getPlayersByLeaguePage(140, 2026, 99);

        assertTrue(page.getItems().isEmpty());
        assertEquals(0, page.getResults());
        assertEquals(99, page.getPaging().getCurrent());
        assertFalse(page.hasMore());
        assertEquals("/players?league=140&season=2026&page=99", takeRequest().getPath());
    }

    @Test
    void getPlayersByTeamPage_buildsQuery() throws Exception {
        enqueue("players_page1.json");

        api.getPlayersByTeamPage(541, 2025, 1);

        assertEquals("/players?team=541&season=2025&page=1", takeRequest().getPath());
    }

    @Test
    void searchPlayersPage_buildsQuery() throws Exception {
        enqueue("players_page1.json");

        api.searchPlayersPage("gome", 140, 2026, 1);

        assertEquals("/players?search=gome&league=140&season=2026&page=1", takeRequest().getPath());
    }

    @Test
    void getAllPlayers_stopsAtMaxPages() throws Exception {
        enqueue("players_page1.json");
        enqueue("players_page2.json");

        List<PlayerWithStatistics> all = api.getAllPlayers(new PlayersQuery().league(140).season(2026), 2);

        assertEquals(10, all.size());
        assertEquals(2, server.getRequestCount());
        assertEquals("/players?league=140&season=2026&page=1", takeRequest().getPath());
        assertEquals("/players?league=140&season=2026&page=2", takeRequest().getPath());
    }

    @Test
    void getAllPlayers_rejectsNonPositiveMaxPages() {
        assertThrows(IllegalArgumentException.class, () -> api.getAllPlayers(new PlayersQuery().league(1), 0));
        assertThrows(IllegalArgumentException.class,
                () -> api.getAllPlayerProfiles(new PlayerProfilesQuery(), -1));
        assertEquals(0, server.getRequestCount());
    }

    @Test
    void searchWithoutScope_throwsWithErrors() throws Exception {
        enqueue("error_players_search_without_scope.json");

        ApiFootballException e = assertThrows(ApiFootballException.class,
                () -> api.getPlayersPage(new PlayersQuery().search("mbappe"), 1));

        assertEquals("The League or Team field is required with the Search field.", e.getErrors().get("league"));
        assertEquals("The League or Team field is required with the Search field.", e.getErrors().get("team"));
        assertEquals("/players?search=mbappe&page=1", takeRequest().getPath());
    }

    // ---------------------------------------------------------------- /players/seasons

    @Test
    void getPlayerSeasons_all() throws Exception {
        enqueue("players_seasons.json");

        List<Integer> seasons = api.getPlayerSeasons();

        assertEquals(Arrays.asList(1962, 2026, 2027), seasons);
        assertEquals("/players/seasons", takeRequest().getPath());
    }

    @Test
    void getPlayerSeasons_byPlayer() throws Exception {
        enqueue("players_seasons_by_player.json");

        assertEquals(Arrays.asList(2014, 2025, 2026), api.getPlayerSeasons(278));
        assertEquals("/players/seasons?player=278", takeRequest().getPath());
    }

    // ---------------------------------------------------------------- /players/profiles

    @Test
    void getPlayerProfileById_byPlayer() throws Exception {
        enqueue("players_profiles_by_player.json");

        Player p = api.getPlayerProfileById(278);

        assertEquals("Kylian Mbappé", p.getName());
        assertEquals(10, p.getNumber());
        assertEquals("Attacker", p.getPosition());
        assertEquals("France", p.getBirth().getCountry());
        assertNull(p.getInjured());
        assertEquals("/players/profiles?player=278", takeRequest().getPath());
    }

    @Test
    void getPlayerProfilesPage_page1() throws Exception {
        enqueue("players_profiles_page1.json");

        Page<PlayerProfile> page = api.getPlayerProfilesPage(new PlayerProfilesQuery(), 1);

        assertEquals(2771, page.getPaging().getTotal());
        assertTrue(page.hasMore());
        Player first = page.getItems().get(0).getPlayer();
        assertEquals("R. Bürki", first.getName());
        assertEquals("Goalkeeper", first.getPosition());
        assertEquals(1, first.getNumber());
        assertEquals(187, first.getHeightCm());
        assertEquals(85, first.getWeightKg());
        assertEquals("/players/profiles?page=1", takeRequest().getPath());
    }

    @Test
    void getPlayerProfilesPage_nullHeightAndWeight() throws Exception {
        enqueue("players_profiles_page1.json");

        Page<PlayerProfile> page = api.searchPlayerProfilesPage("buerki", 1);

        List<Player> noMeasures = page.getItems().stream().map(PlayerProfile::getPlayer)
                .filter(p -> p.getHeight() == null).collect(Collectors.toList());
        assertFalse(noMeasures.isEmpty());
        assertNull(noMeasures.get(0).getHeightCm());
        assertNull(noMeasures.get(0).getWeightKg());
        assertEquals("/players/profiles?search=buerki&page=1", takeRequest().getPath());
    }

    @Test
    void getAllPlayerProfiles_isCappedByMaxPages() throws Exception {
        enqueue("players_profiles_page1.json");

        List<PlayerProfile> all = api.getAllPlayerProfiles(new PlayerProfilesQuery(), 1);

        assertFalse(all.isEmpty());
        assertEquals(1, server.getRequestCount());
        assertEquals("/players/profiles?page=1", takeRequest().getPath());
    }

    @Test
    void statValues_neverThrow() {
        assertEquals(185, NumberParsing.parseLeadingInteger("185 cm"));
        assertEquals(37, NumberParsing.parseLeadingInteger("37%"));
        assertEquals(7.11, NumberParsing.parseDouble("7.11"), 0.0001);
        assertNull(NumberParsing.parseInteger("N/A"));
        assertNull(NumberParsing.parseDouble(""));
        assertNull(NumberParsing.parseDouble(null));
    }

    // ---------------------------------------------------------------- /players/squads

    @Test
    void getSquadByTeam_byTeam() throws Exception {
        enqueue("players_squads.json");

        Squad squad = api.getSquadByTeam(541);

        assertEquals("Real Madrid", squad.getTeam().getName());
        assertEquals(541, squad.getTeam().getId());
        Squad.SquadPlayer gk = squad.getPlayers().get(0);
        assertEquals(730, gk.getId());
        assertEquals("T. Courtois", gk.getName());
        assertEquals(33, gk.getAge());
        assertEquals(1, gk.getNumber());
        assertEquals("Goalkeeper", gk.getPosition());
        assertEquals("/players/squads?team=541", takeRequest().getPath());
    }

    @Test
    void getSquadsByPlayer_returnsTeams() throws Exception {
        enqueue("players_squads_by_player.json");

        List<Squad> squads = api.getSquadsByPlayer(278);

        assertEquals(2, squads.size());
        assertEquals("France", squads.get(0).getTeam().getName());
        assertEquals(278, squads.get(1).getPlayers().get(0).getId());
        assertEquals("/players/squads?player=278", takeRequest().getPath());
    }

    @Test
    void getSquads_withQuery() throws Exception {
        enqueue("players_squads.json");

        api.getSquads(new SquadsQuery().team(541).player(730));

        assertEquals("/players/squads?team=541&player=730", takeRequest().getPath());
    }

    // ---------------------------------------------------------------- /players/teams

    @Test
    void getPlayerTeams_parsesSeasons() throws Exception {
        enqueue("players_teams.json");

        List<PlayerTeam> teams = api.getPlayerTeams(278);

        assertEquals(3, teams.size());
        assertEquals("Real Madrid", teams.get(0).getTeam().getName());
        assertEquals(Arrays.asList(2026, 2025, 2024), teams.get(0).getSeasons());
        assertEquals(Collections.singletonList(2016), teams.get(1).getSeasons());
        assertTrue(teams.get(2).getSeasons().isEmpty());
        assertEquals("/players/teams?player=278", takeRequest().getPath());
    }

    // ---------------------------------------------------------------- top lists

    @Test
    void getTopScorers_parses() throws Exception {
        enqueue("players_topscorers.json");

        List<PlayerWithStatistics> list = api.getTopScorers(140, 2025);

        assertEquals(5, list.size());
        assertEquals("Kylian Mbappé", list.get(0).getPlayer().getName());
        assertEquals("Real Madrid", list.get(0).getStatistics().get(0).getTeam().getName());
        assertNotNull(list.get(0).getStatistics().get(0).getGoals().getTotal());
        assertEquals("/players/topscorers?league=140&season=2025", takeRequest().getPath());
    }

    @Test
    void getTopAssists_parses() throws Exception {
        enqueue("players_topassists.json");

        List<PlayerWithStatistics> list = api.getTopAssists(140, 2025);

        assertEquals("Lamine Yamal", list.get(0).getPlayer().getName());
        assertEquals("Barcelona", list.get(0).getStatistics().get(0).getTeam().getName());
        assertEquals("/players/topassists?league=140&season=2025", takeRequest().getPath());
    }

    @Test
    void getTopYellowCards_parses() throws Exception {
        enqueue("players_topyellowcards.json");

        List<PlayerWithStatistics> list = api.getTopYellowCards(140, 2025);

        assertEquals("José Ángel Carmona", list.get(0).getPlayer().getName());
        assertNotNull(list.get(0).getStatistics().get(0).getCards().getYellow());
        assertEquals("/players/topyellowcards?league=140&season=2025", takeRequest().getPath());
    }

    @Test
    void getTopRedCards_parses() throws Exception {
        enqueue("players_topredcards.json");

        List<PlayerWithStatistics> list = api.getTopRedCards(140, 2025);

        assertEquals("F. Viñas", list.get(0).getPlayer().getName());
        assertEquals("Oviedo", list.get(0).getStatistics().get(0).getTeam().getName());
        assertEquals("/players/topredcards?league=140&season=2025", takeRequest().getPath());
    }

    // ---------------------------------------------------------------- /transfers

    @Test
    void getTransfersByPlayer_parses() throws Exception {
        enqueue("transfers_by_player.json");

        List<PlayerTransfers> result = api.getTransfersByPlayer(278);

        PlayerTransfers t = result.get(0);
        assertEquals(278, t.getPlayer().getId());
        assertEquals("Kylian Mbappé", t.getPlayer().getName());
        assertEquals("2025-07-29T11:26:16+00:00", t.getUpdate());
        assertEquals(3, t.getTransfers().size());
        assertEquals("2024-07-01", t.getTransfers().get(0).getDate());
        assertEquals("Free", t.getTransfers().get(0).getType());
        assertEquals("Real Madrid", t.getTransfers().get(0).getTeams().getIn().getName());
        assertEquals(85, t.getTransfers().get(0).getTeams().getOut().getId());
        assertEquals("€ 180M", t.getTransfers().get(1).getType());
        assertEquals("Loan", t.getTransfers().get(2).getType());
        assertEquals("/transfers?player=278", takeRequest().getPath());
    }

    @Test
    void getTransfersByTeam_buildsQuery() throws Exception {
        enqueue("transfers_by_player.json");

        api.getTransfersByTeam(541);

        assertEquals("/transfers?team=541", takeRequest().getPath());
    }

    // ---------------------------------------------------------------- /trophies

    @Test
    void getTrophiesByPlayer_flatList() throws Exception {
        enqueue("trophies_by_player.json");

        List<Trophy> trophies = api.getTrophiesByPlayer(278);

        assertEquals("Super Cup", trophies.get(0).getLeague());
        assertEquals("Spain", trophies.get(0).getCountry());
        assertEquals("2025", trophies.get(0).getSeason());
        assertEquals("2nd Place", trophies.get(0).getPlace());
        assertNull(trophies.get(1).getSeason());
        assertEquals("/trophies?player=278", takeRequest().getPath());
    }

    @Test
    void getTrophiesByCoach_buildsQuery() throws Exception {
        enqueue("trophies_by_player.json");

        api.getTrophiesByCoach(2462);

        assertEquals("/trophies?coach=2462", takeRequest().getPath());
    }

    @Test
    void getTrophiesByCoaches_wrappers() throws Exception {
        enqueue("trophies_by_coachs.json");

        List<TrophyGroup> groups = api.getTrophiesByCoaches(Arrays.asList(2462, 1584));

        assertEquals(2, groups.size());
        assertEquals(1584, groups.get(0).getId());
        assertEquals("1998 France", groups.get(0).getTrophies().get(0).getSeason());
        assertEquals("FIFA World Cup", groups.get(0).getTrophies().get(0).getLeague());
        assertEquals(2462, groups.get(1).getId());
        assertEquals("2011/2012", groups.get(1).getTrophies().get(0).getSeason());
        assertEquals("/trophies?coachs=2462-1584", takeRequest().getPath());
    }

    @Test
    void getTrophiesByPlayers_buildsQuery() throws Exception {
        enqueue("trophies_by_coachs.json");

        api.getTrophiesByPlayers(Arrays.asList(278, 730));

        assertEquals("/trophies?players=278-730", takeRequest().getPath());
    }

    @Test
    void idLists_areValidated() {
        List<Integer> tooMany = IntStream.rangeClosed(1, 21).boxed().collect(Collectors.toList());
        assertThrows(IllegalArgumentException.class, () -> api.getTrophiesByPlayers(tooMany));
        assertThrows(IllegalArgumentException.class, () -> api.getSidelinedByCoaches(Collections.emptyList()));
        assertEquals(0, server.getRequestCount());
    }

    // ---------------------------------------------------------------- /sidelined

    @Test
    void getSidelinedByPlayer_flatList() throws Exception {
        enqueue("sidelined_by_player.json");

        List<SidelinedPeriod> periods = api.getSidelinedByPlayer(278);

        assertEquals("Bowel Disease", periods.get(0).getType());
        assertEquals("2025-06-18", periods.get(0).getStart());
        assertEquals("2025-06-28", periods.get(0).getEnd());
        assertEquals("/sidelined?player=278", takeRequest().getPath());
    }

    @Test
    void getSidelinedByPlayers_wrappers() throws Exception {
        enqueue("sidelined_by_players.json");

        List<SidelinedGroup> groups = api.getSidelinedByPlayers(Arrays.asList(278, 730));

        assertEquals(2, groups.size());
        assertEquals(278, groups.get(0).getId());
        assertEquals("Ankle Injury", groups.get(0).getSidelined().get(1).getType());
        assertEquals(730, groups.get(1).getId());
        assertEquals("Back Injury", groups.get(1).getSidelined().get(0).getType());
        assertEquals("/sidelined?players=278-730", takeRequest().getPath());
    }

    @Test
    void getSidelinedByCoach_emptyResult() throws Exception {
        enqueue("sidelined_by_coach.json");

        assertTrue(api.getSidelinedByCoach(2462).isEmpty());
        assertEquals("/sidelined?coach=2462", takeRequest().getPath());
    }

    @Test
    void getSidelinedByCoaches_buildsQuery() throws Exception {
        enqueue("sidelined_by_players.json");

        api.getSidelinedByCoaches(Arrays.asList(1, 2));

        assertEquals("/sidelined?coachs=1-2", takeRequest().getPath());
    }
}

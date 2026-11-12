package es.com.kete1987.apifootball.library;

import es.com.kete1987.apifootball.library.common.util.ApiFootballException;
import es.com.kete1987.apifootball.library.leagues.model.Country;
import es.com.kete1987.apifootball.library.leagues.model.League;
import es.com.kete1987.apifootball.library.leagues.model.Season;
import es.com.kete1987.apifootball.library.leagues.model.StandingEntry;
import es.com.kete1987.apifootball.library.leagues.model.Standings;
import es.com.kete1987.apifootball.library.leagues.query.CountriesQuery;
import es.com.kete1987.apifootball.library.leagues.query.LeaguesQuery;
import es.com.kete1987.apifootball.library.leagues.query.StandingsQuery;
import es.com.kete1987.apifootball.library.leagues.util.LeagueTypes;
import es.com.kete1987.apifootball.library.leagues.util.StandingStatuses;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeaguesApiTest extends BaseApiTest {
    private LeaguesApi api;

    @BeforeEach
    void setUp() {
        api = new LeaguesApi(client, baseUrl, tracker);
    }

    @Test
    void getTimezones_parsesStrings() throws Exception {
        enqueue("timezone.json");

        List<String> zones = api.getTimezones();

        assertEquals(3, zones.size());
        assertEquals("Africa/Abidjan", zones.get(0));
        assertEquals("Pacific/Wallis", zones.get(2));
        assertEquals("/timezone", takeRequest().getPath());
    }

    @Test
    void getCountries_parsesCountriesIncludingNullCodeAndFlag() throws Exception {
        enqueue("countries.json");

        List<Country> countries = api.getCountries();

        assertEquals(3, countries.size());
        assertEquals("Albania", countries.get(0).getName());
        assertEquals("AL", countries.get(0).getCode());
        assertEquals("https://media.api-sports.io/flags/al.svg", countries.get(0).getFlag());
        Country world = countries.stream().filter(c -> "World".equals(c.getName())).findFirst().orElseThrow();
        assertNull(world.getCode());
        assertNull(world.getFlag());
        assertEquals("/countries", takeRequest().getPath());
    }

    @Test
    void getCountryByName_sendsNameAndReturnsFirst() throws Exception {
        enqueue("countries_by_name.json");

        Country country = api.getCountryByName("england");

        assertEquals("England", country.getName());
        assertEquals("GB-ENG", country.getCode());
        assertEquals("https://media.api-sports.io/flags/gb-eng.svg", country.getFlag());
        assertEquals("/countries?name=england", takeRequest().getPath());
    }

    @Test
    void getCountryByCode_sendsCode() throws Exception {
        enqueue("countries_by_name.json");

        assertEquals("England", api.getCountryByCode("GB-ENG").getName());
        assertEquals("/countries?code=GB-ENG", takeRequest().getPath());
    }

    @Test
    void searchCountries_sendsSearch() throws Exception {
        enqueue("countries_by_name.json");

        assertEquals(1, api.searchCountries("engl").size());
        assertEquals("/countries?search=engl", takeRequest().getPath());
    }

    @Test
    void getCountries_withQuery_sendsEveryParameter() throws Exception {
        enqueue("countries_by_name.json");

        api.getCountries(new CountriesQuery().name("england").code("GB-ENG").search("eng"));

        assertEquals("/countries?name=england&code=GB-ENG&search=eng", takeRequest().getPath());
    }

    @Test
    void getLeagueById_parsesLeagueSeasonsAndCoverage() throws Exception {
        enqueue("leagues_by_id.json");

        League league = api.getLeagueById(140);

        assertEquals(140, league.getLeague().getId());
        assertEquals("La Liga", league.getLeague().getName());
        assertEquals(LeagueTypes.LEAGUE, league.getLeague().getType());
        assertEquals("https://media.api-sports.io/football/leagues/140.png", league.getLeague().getLogo());
        assertEquals("Spain", league.getCountry().getName());
        assertEquals("ES", league.getCountry().getCode());
        assertEquals(3, league.getSeasons().size());

        Season first = league.getSeasons().get(0);
        assertEquals(2010, first.getYear());
        assertEquals("2010-08-28", first.getStart());
        assertEquals("2011-05-21", first.getEnd());
        assertFalse(first.getCurrent());
        assertTrue(first.getCoverage().getFixtures().getEvents());
        assertFalse(first.getCoverage().getFixtures().getStatisticsFixtures());
        assertFalse(first.getCoverage().getFixtures().getStatisticsPlayers());
        assertTrue(first.getCoverage().getStandings());
        assertTrue(first.getCoverage().getTopScorers());
        assertTrue(first.getCoverage().getTopAssists());
        assertTrue(first.getCoverage().getTopCards());
        assertFalse(first.getCoverage().getInjuries());
        assertTrue(first.getCoverage().getPredictions());
        assertFalse(first.getCoverage().getOdds());
        assertTrue(first.getCoverage().getPlayers());
        assertTrue(first.getCoverage().getFixtures().getLineups());

        Season current = league.getCurrentSeason();
        assertEquals(2026, current.getYear());
        assertTrue(current.getCurrent());
        assertEquals(2010, league.getSeason(2010).getYear());
        assertNull(league.getSeason(1900));
        assertEquals("/leagues?id=140", takeRequest().getPath());
    }

    @Test
    void getLeagueByIdAndSeason_sendsBoth() throws Exception {
        enqueue("leagues_by_id.json");

        assertNotNull(api.getLeagueById(140, 2026));

        assertEquals("/leagues?id=140&season=2026", takeRequest().getPath());
    }

    @Test
    void getCurrentLeaguesByCountry_parsesCupsAndCountryFlag() throws Exception {
        enqueue("leagues_current_country.json");

        List<League> leagues = api.getCurrentLeaguesByCountry("Spain");

        assertEquals(3, leagues.size());
        assertEquals(437, leagues.get(0).getLeague().getId());
        assertEquals(LeagueTypes.CUP, leagues.get(1).getLeague().getType());
        assertEquals("https://media.api-sports.io/flags/es.svg", leagues.get(1).getCountry().getFlag());
        assertEquals(1, leagues.get(1).getSeasons().size());
        assertFalse(leagues.get(1).getSeasons().get(0).getCoverage().getStandings());
        assertEquals("/leagues?country=Spain&current=true", takeRequest().getPath());
    }

    @Test
    void leagueConvenienceMethods_buildExpectedQueries() throws Exception {
        for (int i = 0; i < 6; i++) enqueue("leagues_by_id.json");

        api.getLeaguesByCountry("Spain");
        api.getLeaguesByCountry("Spain", 2025);
        api.getLeaguesBySeason(2025);
        api.getLeaguesByTeam(541);
        api.getCurrentLeagues();
        api.searchLeagues("liga");

        assertEquals("/leagues?country=Spain", takeRequest().getPath());
        assertEquals("/leagues?country=Spain&season=2025", takeRequest().getPath());
        assertEquals("/leagues?season=2025", takeRequest().getPath());
        assertEquals("/leagues?team=541", takeRequest().getPath());
        assertEquals("/leagues?current=true", takeRequest().getPath());
        assertEquals("/leagues?search=liga", takeRequest().getPath());
    }

    @Test
    void getLeagues_withQuery_sendsEveryParameter() throws Exception {
        enqueue("leagues_by_id.json");

        api.getLeagues(new LeaguesQuery().id(140).name("La Liga").country("Spain").code("ES").season(2025)
                .team(541).type("Cup").current(false).search("liga").last(5));

        assertEquals("/leagues?id=140&name=La%20Liga&country=Spain&code=ES&season=2025&team=541"
                + "&type=cup&current=false&search=liga&last=5", takeRequest().getPath());
    }

    @Test
    void getLeagueSeasons_parsesIntegers() throws Exception {
        enqueue("leagues_seasons.json");

        List<Integer> seasons = api.getLeagueSeasons();

        assertEquals(List.of(2008, 2027, 2028), seasons);
        assertEquals("/leagues/seasons", takeRequest().getPath());
    }

    @Test
    void getStandings_parsesSingleTable() throws Exception {
        enqueue("standings_league.json");

        Standings standings = api.getStandings(140, 2026);

        assertEquals(140, standings.getId());
        assertEquals("La Liga", standings.getName());
        assertEquals("Spain", standings.getCountry());
        assertEquals("https://media.api-sports.io/football/leagues/140.png", standings.getLogo());
        assertEquals("https://media.api-sports.io/flags/es.svg", standings.getFlag());
        assertEquals(2026, standings.getSeason());
        assertEquals(1, standings.getTables().size());
        assertEquals(3, standings.getTable().size());

        StandingEntry top = standings.getTable().get(0);
        assertEquals(1, top.getRank());
        assertEquals(529, top.getTeam().getId());
        assertEquals("Barcelona", top.getTeam().getName());
        assertEquals("https://media.api-sports.io/football/teams/529.png", top.getTeam().getLogo());
        assertEquals(33, top.getPoints());
        assertEquals(35, top.getGoalsDiff());
        assertEquals("WWLWW", top.getForm());
        assertEquals(StandingStatuses.SAME, top.getStatus());
        assertEquals("Promotion - Champions League (League phase)", top.getDescription());
        assertEquals(12, top.getAll().getPlayed());
        assertEquals(11, top.getAll().getWin());
        assertEquals(0, top.getAll().getDraw());
        assertEquals(1, top.getAll().getLose());
        assertEquals(48, top.getAll().getGoals().getFor());
        assertEquals(13, top.getAll().getGoals().getAgainst());
        assertEquals(6, top.getHome().getPlayed());
        assertEquals(24, top.getHome().getGoals().getFor());
        assertEquals(6, top.getAway().getWin());
        assertEquals("2026-11-10T00:00:00+00:00", top.getUpdate());

        StandingEntry last = standings.getTable().get(2);
        assertEquals(20, last.getRank());
        assertEquals(-14, last.getGoalsDiff());
        assertEquals("Relegation - LaLiga2", last.getDescription());
        assertEquals("/standings?league=140&season=2026", takeRequest().getPath());
    }

    @Test
    void getStandings_parsesGroupsAsArrayOfArrays() throws Exception {
        enqueue("standings_groups.json");

        Standings standings = api.getStandings(1, 2026);

        assertEquals("World Cup", standings.getName());
        assertEquals("World", standings.getCountry());
        assertNull(standings.getFlag());
        assertEquals(5, standings.getTables().size());
        assertEquals(3, standings.getTables().get(0).size());
        assertEquals("Group A", standings.getTables().get(0).get(0).getGroup());
        assertEquals("Group E", standings.getTable("Group E").get(0).getGroup());
        assertEquals("LLWW", standings.getTable("Group E").get(0).getForm());
        assertEquals("Round of 32", standings.getTable("Group B").get(0).getDescription());
        assertEquals("Mexico", standings.getTable("Group A").get(0).getTeam().getName());
        assertTrue(standings.getTable("Group Z").isEmpty());
        assertEquals("/standings?league=1&season=2026", takeRequest().getPath());
    }

    @Test
    void getStandingsByTeam_returnsOnePerCompetition() throws Exception {
        enqueue("standings_league.json");

        List<Standings> result = api.getStandingsByTeam(529, 2026);

        assertEquals(1, result.size());
        assertEquals("/standings?team=529&season=2026", takeRequest().getPath());
    }

    @Test
    void getStandings_withQuery_sendsEveryParameter() throws Exception {
        enqueue("standings_league.json");

        api.getStandings(new StandingsQuery().league(140).season(2026).team(529));

        assertEquals("/standings?league=140&season=2026&team=529", takeRequest().getPath());
    }

    @Test
    void getStandings_missingSeason_throwsWithErrors() throws Exception {
        enqueue("error_standings_missing_season.json");

        ApiFootballException e = assertThrows(ApiFootballException.class,
                () -> api.getStandings(new StandingsQuery().league(140)));

        assertEquals("The Season field is required.", e.getErrors().get("season"));
        assertEquals("The Team field is required.", e.getErrors().get("team"));
        assertEquals("/standings?league=140", takeRequest().getPath());
    }
}

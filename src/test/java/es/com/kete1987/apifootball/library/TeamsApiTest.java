package es.com.kete1987.apifootball.library;

import es.com.kete1987.apifootball.library.common.util.ApiFootballException;
import es.com.kete1987.apifootball.library.leagues.model.Country;
import es.com.kete1987.apifootball.library.teams.model.Coach;
import es.com.kete1987.apifootball.library.teams.model.MinuteStat;
import es.com.kete1987.apifootball.library.teams.model.Team;
import es.com.kete1987.apifootball.library.teams.model.TeamStatistics;
import es.com.kete1987.apifootball.library.teams.model.Venue;
import es.com.kete1987.apifootball.library.teams.query.CoachesQuery;
import es.com.kete1987.apifootball.library.teams.query.TeamsQuery;
import es.com.kete1987.apifootball.library.teams.query.VenuesQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamsApiTest extends BaseApiTest {
    private TeamsApi api;

    @BeforeEach
    void setUp() {
        api = new TeamsApi(client, baseUrl, tracker);
    }

    @Test
    void getTeamById_parsesTeamAndVenue() throws Exception {
        enqueue("teams_by_id.json");

        Team team = api.getTeamById(541);

        assertEquals(541, team.getTeam().getId());
        assertEquals("Real Madrid", team.getTeam().getName());
        assertEquals("REA", team.getTeam().getCode());
        assertEquals("Spain", team.getTeam().getCountry());
        assertEquals(1902, team.getTeam().getFounded());
        assertFalse(team.getTeam().getNational());
        assertEquals("https://media.api-sports.io/football/teams/541.png", team.getTeam().getLogo());
        assertEquals(1456, team.getVenue().getId());
        assertEquals("Avenida de Concha Espina 1, Chamartín", team.getVenue().getAddress());
        assertEquals("Madrid", team.getVenue().getCity());
        assertEquals(85454, team.getVenue().getCapacity());
        assertEquals("grass", team.getVenue().getSurface());
        assertNull(team.getVenue().getCountry());
        assertEquals("https://media.api-sports.io/football/venues/1456.png", team.getVenue().getImage());
        assertEquals("/teams?id=541", takeRequest().getPath());
    }

    @Test
    void getTeamsByLeagueSeason_parsesList() throws Exception {
        enqueue("teams_by_league_season.json");

        List<Team> teams = api.getTeams(140, 2026);

        assertEquals(3, teams.size());
        assertEquals("Barcelona", teams.get(0).getTeam().getName());
        assertEquals("/teams?league=140&season=2026", takeRequest().getPath());
    }

    @Test
    void teamConvenienceMethods_buildExpectedQueries() throws Exception {
        for (int i = 0; i < 3; i++) enqueue("teams_by_id.json");

        api.getTeamsByCountry("Spain");
        api.getTeamsByVenue(1456);
        api.searchTeams("rea");

        assertEquals("/teams?country=Spain", takeRequest().getPath());
        assertEquals("/teams?venue=1456", takeRequest().getPath());
        assertEquals("/teams?search=rea", takeRequest().getPath());
    }

    @Test
    void getTeams_withQuery_sendsEveryParameter() throws Exception {
        enqueue("teams_by_id.json");

        api.getTeams(new TeamsQuery().id(541).name("Real Madrid").league(140).season(2025).country("Spain")
                .code("REA").venue(1456).search("real"));

        assertEquals("/teams?id=541&name=Real%20Madrid&league=140&season=2025&country=Spain&code=REA"
                + "&venue=1456&search=real", takeRequest().getPath());
    }

    @Test
    void getTeams_noParams_throwsWithErrors() throws Exception {
        enqueue("error_no_params.json");

        ApiFootballException e = assertThrows(ApiFootballException.class, () -> api.getTeams(new TeamsQuery()));

        assertEquals("At least one parameter is required.", e.getErrors().get("required"));
        assertEquals("/teams", takeRequest().getPath());
    }

    @Test
    void searchTeams_tooShort_throwsWithErrors() throws Exception {
        enqueue("error_search_too_short.json");

        ApiFootballException e = assertThrows(ApiFootballException.class, () -> api.searchTeams("ma"));

        assertEquals("The Search field must be at least 3 characters in length.", e.getErrors().get("search"));
        assertEquals("/teams?search=ma", takeRequest().getPath());
    }

    @Test
    void getTeamStatistics_parsesEveryLevel() throws Exception {
        enqueue("teams_statistics.json");

        TeamStatistics s = api.getTeamStatistics(140, 2025, 541);

        assertEquals(140, s.getLeague().getId());
        assertEquals("La Liga", s.getLeague().getName());
        assertEquals("Spain", s.getLeague().getCountry());
        assertEquals("https://media.api-sports.io/flags/es.svg", s.getLeague().getFlag());
        assertEquals(2025, s.getLeague().getSeason());
        assertEquals(541, s.getTeam().getId());
        assertEquals("Real Madrid", s.getTeam().getName());
        assertEquals("WWWWWWLWWWWDDDWLWWWWWWWWLLWWWLDWDWLWWW", s.getForm());

        assertEquals(19, s.getFixtures().getPlayed().getHome());
        assertEquals(38, s.getFixtures().getPlayed().getTotal());
        assertEquals(11, s.getFixtures().getWins().getAway());
        assertEquals(5, s.getFixtures().getDraws().getTotal());
        assertEquals(6, s.getFixtures().getLoses().getTotal());

        assertEquals(77, s.getGoals().getFor().getTotal().getTotal());
        assertEquals("2.4", s.getGoals().getFor().getAverage().getHome());
        assertEquals("2.0", s.getGoals().getFor().getAverage().getTotal());
        MinuteStat bucket = s.getGoals().getFor().getMinute().get("0-15");
        assertEquals(7, bucket.getTotal());
        assertEquals("8.97%", bucket.getPercentage());
        MinuteStat empty = s.getGoals().getFor().getMinute().get("106-120");
        assertNull(empty.getTotal());
        assertNull(empty.getPercentage());
        assertEquals(34, s.getGoals().getFor().getUnderOver().get("0.5").getOver());
        assertEquals(37, s.getGoals().getFor().getUnderOver().get("4.5").getUnder());
        assertEquals(35, s.getGoals().getAgainst().getTotal().getTotal());
        assertEquals("0.9", s.getGoals().getAgainst().getAverage().getTotal());
        assertEquals(8, s.getGoals().getAgainst().getMinute().get("76-90").getTotal());

        assertEquals(8, s.getBiggest().getStreak().getWins());
        assertEquals(3, s.getBiggest().getStreak().getDraws());
        assertEquals(2, s.getBiggest().getStreak().getLoses());
        assertEquals("5-1", s.getBiggest().getWins().getHome());
        assertEquals("1-4", s.getBiggest().getWins().getAway());
        assertEquals("0-2", s.getBiggest().getLoses().getHome());
        assertEquals(5, s.getBiggest().getGoals().getFor().getHome());
        assertEquals(5, s.getBiggest().getGoals().getAgainst().getAway());

        assertEquals(14, s.getCleanSheet().getTotal());
        assertEquals(6, s.getCleanSheet().getHome());
        assertEquals(4, s.getFailedToScore().getTotal());

        assertEquals(12, s.getPenalty().getScored().getTotal());
        assertEquals("100.00%", s.getPenalty().getScored().getPercentage());
        assertEquals("0%", s.getPenalty().getMissed().getPercentage());
        assertEquals(12, s.getPenalty().getTotal());

        assertEquals("4-4-2", s.getLineups().get(0).getFormation());
        assertEquals(18, s.getLineups().get(0).getPlayed());

        assertEquals(5, s.getCards().getYellow().get("16-30").getTotal());
        assertEquals("1.43%", s.getCards().getYellow().get("").getPercentage());
        assertTrue(s.getCards().getYellow().containsKey(""));
        assertEquals(1, s.getCards().getRed().get("31-45").getTotal());
        assertNull(s.getCards().getRed().get("0-15").getTotal());

        assertEquals("/teams/statistics?league=140&season=2025&team=541", takeRequest().getPath());
    }

    @Test
    void getTeamStatistics_withDate_sendsDate() throws Exception {
        enqueue("teams_statistics.json");

        api.getTeamStatistics(140, 2025, 541, "2026-01-31");

        assertEquals("/teams/statistics?league=140&season=2025&team=541&date=2026-01-31",
                takeRequest().getPath());
    }

    @Test
    void getTeamStatistics_emptyArrayResponse_returnsNull() throws Exception {
        enqueueBody("{\"get\":\"teams/statistics\",\"parameters\":[],\"errors\":[],\"results\":0,"
                + "\"paging\":{\"current\":1,\"total\":1},\"response\":[]}", 200);

        assertNull(api.getTeamStatistics(140, 2025, 541));
    }

    @Test
    void getTeamStatistics_missingTeam_throwsWithErrors() throws Exception {
        enqueue("error_missing_param.json");

        ApiFootballException e = assertThrows(ApiFootballException.class,
                () -> api.getTeamStatistics(140, 2025, 0));

        assertEquals("The Team field is required.", e.getErrors().get("team"));
        assertEquals("/teams/statistics?league=140&season=2025&team=0", takeRequest().getPath());
    }

    @Test
    void getTeamSeasons_parsesIntegers() throws Exception {
        enqueue("teams_seasons.json");

        assertEquals(List.of(2010, 2025, 2026), api.getTeamSeasons(541));
        assertEquals("/teams/seasons?team=541", takeRequest().getPath());
    }

    @Test
    void getTeamCountries_parsesNullableCodeAndFlag() throws Exception {
        enqueue("teams_countries.json");

        List<Country> countries = api.getTeamCountries();

        assertEquals(3, countries.size());
        assertEquals("Afghanistan", countries.get(0).getName());
        assertNull(countries.get(0).getCode());
        assertNull(countries.get(0).getFlag());
        assertEquals("AL", countries.get(1).getCode());
        assertEquals("https://media.api-sports.io/flags/al.svg", countries.get(1).getFlag());
        assertEquals("/teams/countries", takeRequest().getPath());
    }

    @Test
    void getVenueById_parsesVenue() throws Exception {
        enqueue("venues_by_id.json");

        Venue venue = api.getVenueById(1456);

        assertEquals(1456, venue.getId());
        assertEquals("Madrid", venue.getCity());
        assertEquals("Spain", venue.getCountry());
        assertEquals(85454, venue.getCapacity());
        assertEquals("grass", venue.getSurface());
        assertEquals("https://media.api-sports.io/football/venues/1456.png", venue.getImage());
        assertEquals("/venues?id=1456", takeRequest().getPath());
    }

    @Test
    void searchVenues_parsesNullAddress() throws Exception {
        enqueue("venues_search.json");

        List<Venue> venues = api.searchVenues("madrid");

        assertEquals(3, venues.size());
        assertNull(venues.get(1).getAddress());
        assertEquals("artificial turf", venues.get(1).getSurface());
        assertEquals(4000, venues.get(1).getCapacity());
        assertEquals("/venues?search=madrid", takeRequest().getPath());
    }

    @Test
    void venueConvenienceMethods_andQuery_buildExpectedQueries() throws Exception {
        for (int i = 0; i < 3; i++) enqueue("venues_search.json");

        api.getVenuesByCity("Madrid");
        api.getVenuesByCountry("Spain");
        api.getVenues(new VenuesQuery().id(1).name("Camp Nou").city("Barcelona").country("Spain").search("nou"));

        assertEquals("/venues?city=Madrid", takeRequest().getPath());
        assertEquals("/venues?country=Spain", takeRequest().getPath());
        assertEquals("/venues?id=1&name=Camp%20Nou&city=Barcelona&country=Spain&search=nou",
                takeRequest().getPath());
    }

    @Test
    void getCoachesByTeam_parsesCareerAndNullables() throws Exception {
        enqueue("coachs_by_team.json");

        List<Coach> coaches = api.getCoachesByTeam(541);

        assertEquals(2, coaches.size());
        Coach zidane = coaches.get(0);
        assertEquals(1584, zidane.getId());
        assertEquals("Z. Zidane", zidane.getName());
        assertEquals("Zidane", zidane.getLastname());
        assertEquals(53, zidane.getAge());
        assertEquals("1972-06-23", zidane.getBirth().getDate());
        assertEquals("Marseille", zidane.getBirth().getPlace());
        assertEquals("France", zidane.getBirth().getCountry());
        assertEquals("France", zidane.getNationality());
        assertEquals("185 cm", zidane.getHeight());
        assertEquals("78 kg", zidane.getWeight());
        assertEquals("https://media.api-sports.io/football/coachs/1584.png", zidane.getPhoto());
        assertEquals(541, zidane.getTeam().getId());
        assertEquals(3, zidane.getCareer().size());
        assertEquals("2019-03-01", zidane.getCareer().get(0).getStart());
        assertEquals("2021-05-01", zidane.getCareer().get(0).getEnd());
        assertEquals(9575, zidane.getCareer().get(2).getTeam().getId());

        Coach other = coaches.get(1);
        assertEquals(2462, other.getId());
        assertNull(other.getHeight());
        assertNull(other.getWeight());
        assertNull(other.getCareer().get(0).getEnd());
        assertEquals("/coachs?team=541", takeRequest().getPath());
    }

    @Test
    void coachConvenienceMethods_andQuery_buildExpectedQueries() throws Exception {
        for (int i = 0; i < 3; i++) enqueue("coachs_by_team.json");

        assertEquals(1584, api.getCoachById(1584).getId());
        api.searchCoaches("zid");
        api.getCoaches(new CoachesQuery().id(1).team(541).search("zid"));

        assertEquals("/coachs?id=1584", takeRequest().getPath());
        assertEquals("/coachs?search=zid", takeRequest().getPath());
        assertEquals("/coachs?id=1&team=541&search=zid", takeRequest().getPath());
    }
}

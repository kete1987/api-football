package es.com.kete1987.apifootball.library;

import es.com.kete1987.apifootball.library.fixtures.model.Fixture;
import es.com.kete1987.apifootball.library.fixtures.model.FixtureEvent;
import es.com.kete1987.apifootball.library.fixtures.model.FixtureLineup;
import es.com.kete1987.apifootball.library.fixtures.model.FixturePlayers;
import es.com.kete1987.apifootball.library.fixtures.model.FixtureStatistics;
import es.com.kete1987.apifootball.library.fixtures.query.FixturesQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** {@code /fixtures} methods of {@link FixturesApi}. */
class FixturesApiTest extends BaseApiTest {

    private FixturesApi api;

    @BeforeEach
    void setUp() {
        api = new FixturesApi(client, baseUrl, tracker);
    }

    @Test
    void getFixtureById_parsesEveryLevelIncludingEmbeddedDetails() throws Exception {
        enqueue("fixtures_by_id.json");

        Fixture f = api.getFixtureById(1570443);

        assertEquals("/fixtures?id=1570443", takeRequest().getPath());
        assertEquals(1, server.getRequestCount());
        // fixture
        assertEquals(1570443, f.getFixture().getId());
        assertEquals("J. Alberola", f.getFixture().getReferee());
        assertEquals("UTC", f.getFixture().getTimezone());
        assertEquals("2026-11-08T20:00:00+00:00", f.getFixture().getDate());
        assertEquals(2026, f.getFixture().getDateTime().getYear());
        assertEquals(1794168000L, f.getFixture().getTimestamp());
        assertEquals(1794168000L, f.getFixture().getPeriods().getFirst());
        assertEquals(1794171600L, f.getFixture().getPeriods().getSecond());
        assertEquals(19217, f.getFixture().getVenue().getId());
        assertEquals("Metropolitano Stadium", f.getFixture().getVenue().getName());
        assertEquals("Madrid", f.getFixture().getVenue().getCity());
        assertEquals("Match Finished", f.getFixture().getStatus().getLong());
        assertEquals("FT", f.getFixture().getStatus().getShort());
        assertEquals(90, f.getFixture().getStatus().getElapsed());
        assertNull(f.getFixture().getStatus().getExtra());
        assertTrue(f.getFixture().getStatus().isFinished());
        assertFalse(f.getFixture().getStatus().isLive());
        // league
        assertEquals(140, f.getLeague().getId());
        assertEquals("La Liga", f.getLeague().getName());
        assertEquals("Spain", f.getLeague().getCountry());
        assertEquals("https://media.api-sports.io/flags/es.svg", f.getLeague().getFlag());
        assertEquals(2026, f.getLeague().getSeason());
        assertEquals("Regular Season - 12", f.getLeague().getRound());
        assertTrue(f.getLeague().getStandings());
        // teams, goals, score
        assertEquals(530, f.getTeams().getHome().getId());
        assertEquals("Atletico Madrid", f.getTeams().getHome().getName());
        assertEquals(Boolean.FALSE, f.getTeams().getHome().getWinner());
        assertEquals(Boolean.TRUE, f.getTeams().getAway().getWinner());
        assertEquals(1, f.getGoals().getHome());
        assertEquals(2, f.getGoals().getAway());
        assertEquals(1, f.getScore().getHalftime().getHome());
        assertEquals(2, f.getScore().getFulltime().getAway());
        assertNull(f.getScore().getExtratime().getHome());
        assertNull(f.getScore().getPenalty().getAway());
        // embedded events / lineups / statistics / players reuse the dedicated classes
        assertEquals(5, f.getEvents().size());
        FixtureEvent goal = f.getEvents().get(0);
        assertTrue(goal.isGoal());
        assertEquals("Giuliano Simeone Baldini", goal.getPlayer().getName());
        assertEquals(2, f.getLineups().size());
        assertEquals("4-4-2", f.getLineups().get(0).getFormation());
        assertEquals(2, f.getStatistics().size());
        assertEquals("37%", f.getStatistics().get(0).find("Ball Possession").getValue());
        assertEquals(2, f.getPlayers().size());
        assertEquals("M. Hjulmand", f.getPlayers().get(0).getPlayers().get(0).getPlayer().getName());
    }

    @Test
    void getFixtureById_returnsNullWhenEmpty() throws Exception {
        enqueue("fixtures_empty.json");

        assertNull(api.getFixtureById(1));
        assertEquals("/fixtures?id=1", takeRequest().getPath());
    }

    @Test
    void getFixturesByIds_joinsWithDashAndEmbedsDetails() throws Exception {
        enqueue("fixtures_by_ids.json");

        List<Fixture> fixtures = api.getFixturesByIds(Arrays.asList(1570443, 1570451));

        assertEquals("/fixtures?ids=1570443-1570451", takeRequest().getPath());
        assertEquals(2, fixtures.size());
        assertEquals(1570451, fixtures.get(1).getFixture().getId());
        assertEquals("Valencia", fixtures.get(1).getTeams().getHome().getName());
        assertEquals(3, fixtures.get(1).getGoals().getAway());
        assertEquals(2, fixtures.get(1).getScore().getHalftime().getAway());
        assertNotNull(fixtures.get(1).getEvents());
        assertNotNull(fixtures.get(1).getLineups());
        assertNotNull(fixtures.get(1).getStatistics());
        assertNotNull(fixtures.get(1).getPlayers());
    }

    @Test
    void getLiveFixtures_embedsOnlyEventsAndParsesNulls() throws Exception {
        enqueue("fixtures_live.json");

        List<Fixture> fixtures = api.getLiveFixtures();

        assertEquals("/fixtures?live=all", takeRequest().getPath());
        Fixture f = fixtures.get(0);
        assertEquals(1649008, f.getFixture().getId());
        assertNull(f.getFixture().getReferee());
        assertNull(f.getFixture().getVenue().getId());
        assertEquals("Pleiku Stadium", f.getFixture().getVenue().getName());
        assertNull(f.getFixture().getPeriods().getSecond());
        assertEquals("First Half", f.getFixture().getStatus().getLong());
        assertEquals("1H", f.getFixture().getStatus().getShort());
        assertEquals(14, f.getFixture().getStatus().getElapsed());
        assertTrue(f.getFixture().getStatus().isLive());
        assertNull(f.getLeague().getFlag());
        assertEquals("Friendlies", f.getLeague().getName());
        assertFalse(f.getLeague().getStandings());
        assertEquals("Thailand U23", f.getTeams().getHome().getName());
        assertEquals(0, f.getGoals().getHome());
        assertEquals(1, f.getGoals().getAway());
        assertNull(f.getScore().getFulltime().getHome());
        assertNotNull(f.getEvents());
        FixtureEvent e = f.getEvents().get(0);
        assertNull(e.getPlayer().getId());
        assertNull(e.getPlayer().getName());
        assertNull(e.getAssist().getId());
        assertNull(e.getComments());
        assertNull(f.getLineups());
        assertNull(f.getStatistics());
        assertNull(f.getPlayers());
    }

    @Test
    void getLiveFixtures_byLeagues() throws Exception {
        enqueue("fixtures_live.json");

        api.getLiveFixtures(Arrays.asList(39, 140));

        assertEquals("/fixtures?live=39-140", takeRequest().getPath());
    }

    @Test
    void getFixturesByDate_withTimezone() throws Exception {
        enqueue("fixtures_by_date.json");

        List<Fixture> fixtures = api.getFixturesByDate(LocalDate.of(2026, 11, 12), "Europe/Madrid");

        assertEquals("/fixtures?date=2026-11-12&timezone=Europe%2FMadrid", takeRequest().getPath());
        assertEquals(5, fixtures.size());
        Fixture f = fixtures.get(0);
        assertEquals("Europe/Madrid", f.getFixture().getTimezone());
        assertEquals("2026-11-12T00:00:00+01:00", f.getFixture().getDate());
        assertEquals(3600, f.getFixture().getDateTime().getOffset().getTotalSeconds());
        assertNull(f.getFixture().getVenue().getId());
        assertNull(f.getFixture().getVenue().getName());
        assertNull(f.getFixture().getVenue().getCity());
        assertEquals("Eerste Divisie", f.getLeague().getName());
        assertEquals("Group Stage", f.getLeague().getRound());
        // winner null on a draw
        assertNull(fixtures.get(1).getTeams().getHome().getWinner());
        assertEquals(8, fixtures.get(1).getFixture().getStatus().getExtra());
    }

    @Test
    void getFixturesByDate_stringAndLocalDate() throws Exception {
        enqueue("fixtures_by_date_sunday.json");
        enqueue("fixtures_by_date_sunday.json");

        assertEquals(5, api.getFixturesByDate("2026-11-08").size());
        api.getFixturesByDate(LocalDate.of(2026, 11, 8));

        assertEquals("/fixtures?date=2026-11-08", takeRequest().getPath());
        assertEquals("/fixtures?date=2026-11-08", takeRequest().getPath());
    }

    @Test
    void getFixturesByLeague() throws Exception {
        enqueue("fixtures_last.json");

        List<Fixture> fixtures = api.getFixturesByLeague(140, 2026);

        assertEquals("/fixtures?league=140&season=2026", takeRequest().getPath());
        assertEquals(3, fixtures.size());
    }

    @Test
    void getFixturesByRound_encodesRound() throws Exception {
        enqueue("fixtures_next.json");

        List<Fixture> fixtures = api.getFixturesByRound(140, 2026, "Regular Season - 13");

        assertEquals("/fixtures?league=140&season=2026&round=Regular%20Season%20-%2013", takeRequest().getPath());
        assertEquals("Regular Season - 13", fixtures.get(0).getLeague().getRound());
    }

    @Test
    void getLastFixtures_forTeam_parsesPlayedFixtures() throws Exception {
        enqueue("fixtures_last.json");

        List<Fixture> fixtures = api.getLastFixtures(530, 10);

        assertEquals("/fixtures?team=530&last=10", takeRequest().getPath());
        assertEquals(1570444, fixtures.get(1).getFixture().getId());
        assertEquals("M. Ortiz", fixtures.get(1).getFixture().getReferee());
        assertEquals("Celta Vigo", fixtures.get(1).getTeams().getHome().getName());
        assertNull(fixtures.get(1).getTeams().getHome().getWinner());
        assertEquals(0, fixtures.get(1).getGoals().getAway());
        assertEquals("Municipal de Balaídos", fixtures.get(1).getFixture().getVenue().getName());
    }

    @Test
    void getNextFixtures_parsesScheduledFixtures() throws Exception {
        enqueue("fixtures_next.json");

        List<Fixture> fixtures = api.getNextFixtures(530, 5);

        assertEquals("/fixtures?team=530&next=5", takeRequest().getPath());
        Fixture f = fixtures.get(0);
        assertEquals("NS", f.getFixture().getStatus().getShort());
        assertEquals("Not Started", f.getFixture().getStatus().getLong());
        assertTrue(f.getFixture().getStatus().isScheduled());
        assertNull(f.getFixture().getStatus().getElapsed());
        assertNull(f.getFixture().getPeriods().getFirst());
        assertNull(f.getGoals().getHome());
        assertNull(f.getScore().getHalftime().getHome());
        assertNull(f.getFixture().getVenue().getName());
        assertEquals("Bilbao", f.getFixture().getVenue().getCity());
    }

    @Test
    void nextAndLastByLeague_buildQueries() throws Exception {
        enqueue("fixtures_next.json");
        enqueue("fixtures_last.json");

        api.getNextFixturesByLeague(140, 5);
        api.getLastFixturesByLeague(140, 10);

        assertEquals("/fixtures?league=140&next=5", takeRequest().getPath());
        assertEquals("/fixtures?league=140&last=10", takeRequest().getPath());
    }

    @Test
    void getFixtures_withStatusFilter_parsesExtraTimeAndPenalties() throws Exception {
        enqueue("fixtures_status_multi.json");

        List<Fixture> fixtures = api.getFixtures(new FixturesQuery().league(1).season(2026).status("PEN", "AET"));

        assertEquals("/fixtures?league=1&season=2026&status=PEN-AET", takeRequest().getPath());
        assertEquals(3, fixtures.size());
        Fixture pen = fixtures.get(0);
        assertEquals("PEN", pen.getFixture().getStatus().getShort());
        assertEquals(120, pen.getFixture().getStatus().getElapsed());
        assertEquals(3, pen.getScore().getPenalty().getHome());
        assertEquals(4, pen.getScore().getPenalty().getAway());
        assertEquals(1, pen.getScore().getFulltime().getHome());
        assertEquals("Jalal Jayed, Morocco", pen.getFixture().getReferee());
        assertEquals("World Cup", pen.getLeague().getName());
        Fixture aet = fixtures.get(1);
        assertEquals(0, aet.getFixture().getVenue().getId());
        assertEquals(2, aet.getScore().getExtratime().getHome());
        assertEquals(Boolean.TRUE, aet.getTeams().getHome().getWinner());
        assertTrue(aet.getFixture().getStatus().isFinished());
    }

    @Test
    void getFixtures_emptyResult() throws Exception {
        enqueue("fixtures_empty.json");

        List<Fixture> fixtures = api.getFixtures(
                new FixturesQuery().league(140).season(2026).date(LocalDate.of(2026, 11, 12)));

        assertTrue(fixtures.isEmpty());
        assertEquals("/fixtures?league=140&season=2026&date=2026-11-12", takeRequest().getPath());
    }

    @Test
    void getFixtures_overflowFixtureStillParses() throws Exception {
        enqueue("fixtures_invalid_date_overflow.json");

        // sent through the raw param escape hatch: the API accepts the bogus date silently
        List<Fixture> fixtures = api.getFixtures(new FixturesQuery().param("date", "2026-13-45"));

        assertEquals("/fixtures?date=2026-13-45", takeRequest().getPath());
        assertEquals("2027-02-14T00:00:00+00:00", fixtures.get(0).getFixture().getDate());
        assertEquals(2027, fixtures.get(1).getLeague().getSeason());
    }

    @Test
    void getFixtures_timezoneFallbackEchoedAsUtc() throws Exception {
        enqueue("fixtures_invalid_timezone_fallback.json");

        List<Fixture> fixtures = api.getFixtures(new FixturesQuery().date("2026-11-12").timezone("Europe/Madrid"));

        assertEquals("/fixtures?date=2026-11-12&timezone=Europe%2FMadrid", takeRequest().getPath());
        assertEquals("UTC", fixtures.get(0).getFixture().getTimezone());
        assertEquals(Boolean.TRUE, fixtures.get(0).getTeams().getHome().getWinner());
        assertEquals(3, fixtures.get(0).getFixture().getStatus().getExtra());
        assertNull(fixtures.get(0).getFixture().getVenue().getCity());
    }

    @Test
    void getFixtures_everyParameterIsSent() throws Exception {
        enqueue("fixtures_empty.json");

        api.getFixtures(new FixturesQuery().id(1).date("2026-11-12").league(140).season(2026).team(529)
                .last(3).next(4).from(LocalDate.of(2026, 1, 1)).to("2026-12-31").round("Final")
                .status("NS").venue(19939).timezone("UTC"));

        assertEquals("/fixtures?id=1&date=2026-11-12&league=140&season=2026&team=529&last=3&next=4"
                + "&from=2026-01-01&to=2026-12-31&round=Final&status=NS&venue=19939&timezone=UTC",
                takeRequest().getPath());
    }

    @Test
    void embeddedDetailClassesAreTheDedicatedOnes() throws Exception {
        enqueue("fixtures_by_id.json");

        Fixture f = api.getFixtureById(1570443);

        FixtureLineup lineup = f.getLineups().get(0);
        FixtureStatistics stats = f.getStatistics().get(0);
        FixturePlayers players = f.getPlayers().get(0);
        assertEquals("D. Simeone", lineup.getCoach().getName());
        assertEquals("Atletico Madrid", stats.getTeam().getName());
        assertEquals(530, players.getTeam().getId());
    }
}

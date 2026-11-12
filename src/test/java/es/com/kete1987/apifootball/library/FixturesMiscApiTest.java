package es.com.kete1987.apifootball.library;

import es.com.kete1987.apifootball.library.fixtures.model.Fixture;
import es.com.kete1987.apifootball.library.fixtures.model.Injury;
import es.com.kete1987.apifootball.library.fixtures.model.Prediction;
import es.com.kete1987.apifootball.library.fixtures.model.PredictionTeamLeague;
import es.com.kete1987.apifootball.library.fixtures.model.RoundDates;
import es.com.kete1987.apifootball.library.fixtures.query.HeadToHeadQuery;
import es.com.kete1987.apifootball.library.fixtures.query.InjuriesQuery;
import es.com.kete1987.apifootball.library.fixtures.query.RoundsQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Rounds, head to head, injuries and predictions of {@link FixturesApi}. */
class FixturesMiscApiTest extends BaseApiTest {

    private FixturesApi api;

    @BeforeEach
    void setUp() {
        api = new FixturesApi(client, baseUrl, tracker);
    }

    // ------------------------------------------------------------------ rounds

    @Test
    void getRounds_returnsPlainStrings() throws Exception {
        enqueue("fixtures_rounds.json");

        List<String> rounds = api.getRounds(140, 2026);

        assertEquals("/fixtures/rounds?league=140&season=2026", takeRequest().getPath());
        assertEquals(Arrays.asList("Regular Season - 1", "Regular Season - 37", "Regular Season - 38"), rounds);
    }

    @Test
    void getCurrentRound_returnsFirstString() throws Exception {
        enqueue("fixtures_rounds_current.json");

        assertEquals("Regular Season - 13", api.getCurrentRound(140, 2026));
        assertEquals("/fixtures/rounds?league=140&season=2026&current=true", takeRequest().getPath());
    }

    @Test
    void getRounds_withQuery() throws Exception {
        enqueue("fixtures_rounds_current.json");

        List<String> rounds = api.getRounds(140, 2026, new RoundsQuery().current(true).timezone("Europe/Madrid"));

        assertEquals("/fixtures/rounds?league=140&season=2026&current=true&timezone=Europe%2FMadrid",
                takeRequest().getPath());
        assertEquals(1, rounds.size());
    }

    @Test
    void getRoundsWithDates_returnsObjects() throws Exception {
        enqueue("fixtures_rounds_dates.json");

        List<RoundDates> rounds = api.getRoundsWithDates(140, 2026);

        assertEquals("/fixtures/rounds?league=140&season=2026&dates=true", takeRequest().getPath());
        assertEquals(3, rounds.size());
        assertEquals("Regular Season - 1", rounds.get(0).getRound());
        assertEquals(Arrays.asList("2026-08-15", "2026-08-26", "2026-08-27"), rounds.get(0).getDates());
        assertEquals(Arrays.asList("2027-05-30"), rounds.get(2).getDates());
    }

    @Test
    void getRoundsWithDates_withQuery() throws Exception {
        enqueue("fixtures_rounds_dates.json");

        api.getRoundsWithDates(140, 2026, new RoundsQuery().timezone("Europe/Madrid"));

        assertEquals("/fixtures/rounds?league=140&season=2026&timezone=Europe%2FMadrid&dates=true",
                takeRequest().getPath());
    }

    // ------------------------------------------------------------------ head to head

    @Test
    void getHeadToHead_parsesFixtures() throws Exception {
        enqueue("fixtures_headtohead.json");

        List<Fixture> fixtures = api.getHeadToHead(541, 529, new HeadToHeadQuery().last(3));

        assertEquals("/fixtures/headtohead?h2h=541-529&last=3", takeRequest().getPath());
        assertEquals(3, fixtures.size());
        Fixture f = fixtures.get(0);
        assertEquals(1570426, f.getFixture().getId());
        assertEquals("Camp Nou", f.getFixture().getVenue().getName());
        assertEquals("FT", f.getFixture().getStatus().getShort());
        assertEquals("Regular Season - 10", f.getLeague().getRound());
        assertEquals("Barcelona", f.getTeams().getHome().getName());
        assertEquals(Boolean.FALSE, f.getTeams().getHome().getWinner());
        assertEquals(Boolean.TRUE, f.getTeams().getAway().getWinner());
        assertEquals(2, f.getGoals().getAway());
        assertEquals(1, f.getScore().getHalftime().getAway());
        assertNull(f.getEvents());
        assertNull(f.getLineups());
        assertEquals(2025, fixtures.get(1).getLeague().getSeason());
        assertEquals(1, fixtures.get(1).getFixture().getStatus().getExtra());
    }

    @Test
    void getHeadToHead_plainAndAllFilters() throws Exception {
        enqueue("fixtures_headtohead.json");
        enqueue("fixtures_headtohead.json");

        api.getHeadToHead(541, 529);
        api.getHeadToHead(541, 529, new HeadToHeadQuery().date("2026-10-25").league(140).season(2026).last(5)
                .next(2).from("2026-01-01").to(LocalDate.of(2026, 12, 31)).status("FT", "AET").venue(19939)
                .timezone("Europe/Madrid"));

        assertEquals("/fixtures/headtohead?h2h=541-529", takeRequest().getPath());
        assertEquals("/fixtures/headtohead?h2h=541-529&date=2026-10-25&league=140&season=2026&last=5&next=2"
                + "&from=2026-01-01&to=2026-12-31&status=FT-AET&venue=19939&timezone=Europe%2FMadrid",
                takeRequest().getPath());
    }

    // ------------------------------------------------------------------ injuries

    @Test
    void getInjuriesByLeague_parsesInjuries() throws Exception {
        enqueue("injuries_by_league_season.json");

        List<Injury> injuries = api.getInjuriesByLeague(140, 2026);

        assertEquals("/injuries?league=140&season=2026", takeRequest().getPath());
        assertEquals(3, injuries.size());
        Injury i = injuries.get(0);
        assertEquals(6638, i.getPlayer().getId());
        assertEquals("F. Garces", i.getPlayer().getName());
        assertEquals("https://media.api-sports.io/football/players/6638.png", i.getPlayer().getPhoto());
        assertEquals("Missing Fixture", i.getPlayer().getType());
        assertEquals("Suspended", i.getPlayer().getReason());
        assertEquals(542, i.getTeam().getId());
        assertEquals("Alaves", i.getTeam().getName());
        assertEquals(1570333, i.getFixture().getId());
        assertEquals("UTC", i.getFixture().getTimezone());
        assertEquals("2026-08-15T17:30:00+00:00", i.getFixture().getDate());
        assertEquals(1786815000L, i.getFixture().getTimestamp());
        assertEquals(140, i.getLeague().getId());
        assertEquals(2026, i.getLeague().getSeason());
        assertEquals("La Liga", i.getLeague().getName());
        assertEquals("Spain", i.getLeague().getCountry());
        assertEquals("https://media.api-sports.io/flags/es.svg", i.getLeague().getFlag());
        assertNull(i.getLeague().getRound());
        assertEquals("Ankle Injury", injuries.get(2).getPlayer().getReason());
    }

    @Test
    void getInjuriesByFixture_emptyResult() throws Exception {
        enqueue("injuries_by_fixture.json");

        assertTrue(api.getInjuriesByFixture(1570443).isEmpty());
        assertEquals("/injuries?fixture=1570443", takeRequest().getPath());
    }

    @Test
    void injuriesConvenienceMethods_buildQueries() throws Exception {
        for (int i = 0; i < 5; i++) enqueue("injuries_by_fixture.json");

        api.getInjuriesByFixtures(Arrays.asList(1, 2, 3));
        api.getInjuriesByTeam(529, 2026);
        api.getInjuriesByPlayer(6638, 2026);
        api.getInjuriesByDate(LocalDate.of(2026, 11, 12));
        api.getInjuries(new InjuriesQuery().league(140).season(2026).fixture(1).team(529).player(2)
                .date("2026-11-12").ids("1-2").timezone("UTC"));

        assertEquals("/injuries?ids=1-2-3", takeRequest().getPath());
        assertEquals("/injuries?team=529&season=2026", takeRequest().getPath());
        assertEquals("/injuries?player=6638&season=2026", takeRequest().getPath());
        assertEquals("/injuries?date=2026-11-12", takeRequest().getPath());
        assertEquals("/injuries?league=140&season=2026&fixture=1&team=529&player=2&date=2026-11-12&ids=1-2&timezone=UTC",
                takeRequest().getPath());
    }

    // ------------------------------------------------------------------ predictions

    @Test
    void getPrediction_parsesEveryLevel() throws Exception {
        enqueue("predictions.json");

        Prediction p = api.getPrediction(1570461);

        assertEquals("/predictions?fixture=1570461", takeRequest().getPath());
        // predictions
        assertEquals(541, p.getPredictions().getWinner().getId());
        assertEquals("Real Madrid", p.getPredictions().getWinner().getName());
        assertNull(p.getPredictions().getWinner().getComment());
        assertEquals(Boolean.FALSE, p.getPredictions().getWinOrDraw());
        assertEquals("-3.5", p.getPredictions().getUnderOver());
        assertEquals("-3.5", p.getPredictions().getGoals().getHome());
        assertEquals("-1.5", p.getPredictions().getGoals().getAway());
        assertEquals("Combo Winner : Real Madrid and -3.5 goals", p.getPredictions().getAdvice());
        assertEquals("50%", p.getPredictions().getPercent().getHome());
        assertEquals("50%", p.getPredictions().getPercent().getDraw());
        assertEquals("0%", p.getPredictions().getPercent().getAway());
        // league
        assertEquals(140, p.getLeague().getId());
        assertEquals("La Liga", p.getLeague().getName());
        assertEquals(2026, p.getLeague().getSeason());
        // team + last_5
        Prediction.PredictionTeam home = p.getTeams().getHome();
        assertEquals(541, home.getId());
        assertEquals("Real Madrid", home.getName());
        assertEquals(5, home.getLast5().getPlayed());
        assertEquals("100%", home.getLast5().getForm());
        assertEquals("100%", home.getLast5().getAtt());
        assertEquals("75%", home.getLast5().getDef());
        assertEquals(13, home.getLast5().getGoals().getFor().getTotal());
        assertEquals("2.6", home.getLast5().getGoals().getFor().getAverage());
        assertEquals(2, home.getLast5().getGoals().getAgainst().getTotal());
        assertEquals("0.4", home.getLast5().getGoals().getAgainst().getAverage());
        // team league statistics
        PredictionTeamLeague league = home.getLeague();
        assertEquals("WWWLWWLWWWWW", league.getForm());
        assertEquals(12, league.getFixtures().getPlayed().getTotal());
        assertEquals(5, league.getFixtures().getWins().getHome());
        assertEquals(2, league.getFixtures().getLoses().getAway());
        assertEquals(31, league.getGoals().getFor().getTotal().getTotal());
        assertEquals("3.4", league.getGoals().getFor().getAverage().getHome());
        assertEquals(8, league.getGoals().getFor().getMinute().size());
        assertNotNull(league.getGoals().getFor().getMinute().get("0-15").getTotal());
        assertNull(league.getGoals().getFor().getMinute().get("91-105").getTotal());
        assertNull(league.getGoals().getFor().getMinute().get("106-120").getPercentage());
        assertEquals(5, league.getGoals().getFor().getUnderOver().size());
        assertNotNull(league.getGoals().getFor().getUnderOver().get("2.5").getOver());
        assertEquals(3, league.getBiggest().getStreak().getWins());
        assertEquals("4-0", league.getBiggest().getWins().getHome());
        assertNull(league.getBiggest().getLoses().getHome());
        assertEquals("2-1", league.getBiggest().getLoses().getAway());
        assertEquals(4, league.getBiggest().getGoals().getFor().getHome());
        assertNotNull(league.getCleanSheet().getTotal());
        assertNotNull(league.getFailedToScore().getTotal());
        assertNotNull(league.getPenalty().getTotal());
        assertNotNull(league.getPenalty().getScored().getPercentage());
        assertEquals("4-2-3-1", league.getLineups().get(0).getFormation());
        assertEquals(12, league.getLineups().get(0).getPlayed());
        assertEquals(8, league.getCards().getYellow().size());
        assertNull(league.getCards().getRed().get("0-15").getTotal());
        // comparison
        assertEquals("71%", p.getComparison().getForm().getHome());
        assertEquals("13%", p.getComparison().getAtt().getAway());
        assertEquals("67%", p.getComparison().getDef().getHome());
        assertEquals("95%", p.getComparison().getPoissonDistribution().getHome());
        assertEquals("20%", p.getComparison().getH2h().getAway());
        assertEquals("60%", p.getComparison().getGoals().getHome());
        assertEquals("76.7%", p.getComparison().getTotal().getHome());
        // h2h reuse Fixture
        assertEquals(3, p.getH2h().size());
        assertEquals(1391081, p.getH2h().get(0).getFixture().getId());
        assertNotNull(p.getH2h().get(0).getTeams().getHome().getName());
        assertNull(p.getH2h().get(0).getEvents());
    }

    @Test
    void getPrediction_returnsNullWhenEmpty() throws Exception {
        enqueue("fixtures_empty.json");

        assertNull(api.getPrediction(1));
    }
}

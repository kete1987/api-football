package es.com.kete1987.apifootball.library;

import es.com.kete1987.apifootball.library.fixtures.model.FixtureEvent;
import es.com.kete1987.apifootball.library.fixtures.model.FixtureLineup;
import es.com.kete1987.apifootball.library.fixtures.model.FixturePlayers;
import es.com.kete1987.apifootball.library.fixtures.model.FixtureStatistics;
import es.com.kete1987.apifootball.library.fixtures.model.Statistic;
import es.com.kete1987.apifootball.library.fixtures.query.FixtureEventsQuery;
import es.com.kete1987.apifootball.library.fixtures.query.FixtureLineupsQuery;
import es.com.kete1987.apifootball.library.fixtures.query.FixturePlayersQuery;
import es.com.kete1987.apifootball.library.fixtures.query.FixtureStatisticsQuery;
import es.com.kete1987.apifootball.library.fixtures.util.EventTypes;
import es.com.kete1987.apifootball.library.fixtures.util.StatisticTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Statistics, events, lineups and player statistics of {@link FixturesApi}. */
class FixturesDetailApiTest extends BaseApiTest {

    private FixturesApi api;

    @BeforeEach
    void setUp() {
        api = new FixturesApi(client, baseUrl, tracker);
    }

    // ------------------------------------------------------------------ statistics

    @Test
    void getFixtureStatistics_parsesVaryingValueTypes() throws Exception {
        enqueue("fixtures_statistics.json");

        List<FixtureStatistics> stats = api.getFixtureStatistics(1570443);

        assertEquals("/fixtures/statistics?fixture=1570443", takeRequest().getPath());
        assertEquals(2, stats.size());
        FixtureStatistics home = stats.get(0);
        assertEquals(530, home.getTeam().getId());
        assertEquals("Atletico Madrid", home.getTeam().getName());
        assertEquals("https://media.api-sports.io/football/teams/530.png", home.getTeam().getLogo());
        assertEquals(17, home.getStatistics().size());
        assertNull(home.getFirstHalfStatistics());
        assertNull(home.getSecondHalfStatistics());

        Statistic passes = home.getStatistics().get(0);
        assertEquals("Passes accurate", passes.getType());
        assertEquals("317", passes.getValue());
        assertEquals(317, passes.getIntValue());
        assertEquals(317.0, passes.getDoubleValue());
        assertFalse(passes.isPercentage());

        Statistic possession = home.find(StatisticTypes.BALL_POSSESSION);
        assertEquals("37%", possession.getValue());
        assertTrue(possession.isPercentage());
        assertEquals(37, possession.getIntValue());
        assertEquals(37.0, possession.getDoubleValue());

        Statistic xg = home.find("EXPECTED_GOALS");
        assertEquals(StatisticTypes.EXPECTED_GOALS, xg.getType());
        assertEquals("1.71", xg.getValue());
        assertEquals(1.71, xg.getDoubleValue());
        assertNull(xg.getIntValue());

        assertNull(home.find("No such statistic"));
        assertEquals("Barcelona", stats.get(1).getTeam().getName());
        assertEquals("615", stats.get(1).getStatistics().get(0).getValue());
    }

    @Test
    void statistic_typedGettersNeverThrow() {
        Statistic nullValue = new com.google.gson.Gson().fromJson("{\"type\":\"x\",\"value\":null}", Statistic.class);
        Statistic text = new com.google.gson.Gson().fromJson("{\"type\":\"x\",\"value\":\"n/a\"}", Statistic.class);
        Statistic number = new com.google.gson.Gson().fromJson("{\"type\":\"x\",\"value\":5}", Statistic.class);

        assertNull(nullValue.getValue());
        assertNull(nullValue.getIntValue());
        assertNull(nullValue.getDoubleValue());
        assertFalse(nullValue.isPercentage());
        assertEquals("n/a", text.getValue());
        assertNull(text.getIntValue());
        assertNull(text.getDoubleValue());
        assertEquals("5", number.getValue());
        assertEquals(5, number.getIntValue());
    }

    @Test
    void getFixtureStatistics_withFilters() throws Exception {
        enqueue("fixtures_statistics.json");

        api.getFixtureStatistics(1570443,
                new FixtureStatisticsQuery().team(530).type(StatisticTypes.TOTAL_SHOTS));

        assertEquals("/fixtures/statistics?fixture=1570443&team=530&type=Total%20Shots", takeRequest().getPath());
    }

    @Test
    void getFixtureStatisticsByHalf_parsesHalves() throws Exception {
        enqueue("fixtures_statistics_half.json");

        List<FixtureStatistics> stats = api.getFixtureStatisticsByHalf(1570443);

        assertEquals("/fixtures/statistics?fixture=1570443&half=true", takeRequest().getPath());
        FixtureStatistics home = stats.get(0);
        assertEquals(17, home.getStatistics().size());
        assertEquals(17, home.getFirstHalfStatistics().size());
        assertEquals(17, home.getSecondHalfStatistics().size());
        assertEquals(187, home.getFirstHalfStatistics().get(0).getIntValue());
        assertEquals(6, home.findFirstHalf(StatisticTypes.SHOTS_INSIDE_BOX).getIntValue());
        assertEquals("32%", home.findSecondHalf(StatisticTypes.BALL_POSSESSION).getValue());
        assertEquals("0.23", home.findSecondHalf(StatisticTypes.EXPECTED_GOALS).getValue());
        assertEquals("68%", stats.get(1).findSecondHalf(StatisticTypes.BALL_POSSESSION).getValue());
    }

    // ------------------------------------------------------------------ events

    @Test
    void getFixtureEvents_parsesEveryEventKind() throws Exception {
        enqueue("fixtures_events.json");

        List<FixtureEvent> events = api.getFixtureEvents(1570443);

        assertEquals("/fixtures/events?fixture=1570443", takeRequest().getPath());
        assertEquals(5, events.size());

        FixtureEvent goal = events.get(0);
        assertEquals(10, goal.getTime().getElapsed());
        assertNull(goal.getTime().getExtra());
        assertEquals(530, goal.getTeam().getId());
        assertEquals("Atletico Madrid", goal.getTeam().getName());
        assertEquals(323935, goal.getPlayer().getId());
        assertEquals("Julián Álvarez", goal.getAssist().getName());
        assertEquals("Goal", goal.getType());
        assertEquals(EventTypes.NORMAL_GOAL, goal.getDetail());
        assertNull(goal.getComments());
        assertTrue(goal.isGoal());
        assertFalse(goal.isCard());

        FixtureEvent subst = events.get(1);
        assertEquals("subst", subst.getType());
        assertEquals("Substitution 1", subst.getDetail());
        assertEquals(538, subst.getAssist().getId());
        assertTrue(subst.isSubstitution());

        FixtureEvent card = events.get(4);
        assertEquals(90, card.getTime().getElapsed());
        assertEquals(4, card.getTime().getExtra());
        assertEquals("Card", card.getType());
        assertEquals(EventTypes.YELLOW_CARD, card.getDetail());
        assertEquals("Dissent", card.getComments());
        assertNull(card.getAssist().getId());
        assertNull(card.getAssist().getName());
        assertTrue(card.isCard());
        assertFalse(card.isVar());
    }

    @Test
    void getFixtureEvents_withFilters() throws Exception {
        enqueue("fixtures_events.json");

        api.getFixtureEvents(1570443, new FixtureEventsQuery().team(530).player(7712).type(EventTypes.FILTER_CARD));

        assertEquals("/fixtures/events?fixture=1570443&team=530&player=7712&type=card", takeRequest().getPath());
    }

    // ------------------------------------------------------------------ lineups

    @Test
    void getFixtureLineups_parsesLineups() throws Exception {
        enqueue("fixtures_lineups.json");

        List<FixtureLineup> lineups = api.getFixtureLineups(1570443);

        assertEquals("/fixtures/lineups?fixture=1570443", takeRequest().getPath());
        assertEquals(2, lineups.size());
        FixtureLineup home = lineups.get(0);
        assertEquals(530, home.getTeam().getId());
        assertEquals("Atletico Madrid", home.getTeam().getName());
        assertNotNull(home.getTeam().getLogo());
        assertNull(home.getTeam().getColors());
        assertEquals("4-4-2", home.getFormation());
        assertEquals(1595, home.getCoach().getId());
        assertEquals("D. Simeone", home.getCoach().getName());
        assertEquals("https://media.api-sports.io/football/coachs/1595.png", home.getCoach().getPhoto());
        assertEquals(4, home.getStartXI().size());
        assertEquals(4, home.getSubstitutes().size());

        FixtureLineup.LineupPlayer starter = home.getStartXI().get(1).getPlayer();
        assertEquals(753, starter.getId());
        assertEquals("Marcos Llorente", starter.getName());
        assertEquals(14, starter.getNumber());
        assertEquals("D", starter.getPos());
        assertEquals("2:4", starter.getGrid());

        FixtureLineup.LineupPlayer sub = home.getSubstitutes().get(0).getPlayer();
        assertEquals("M. Hjulmand", sub.getName());
        assertEquals("M", sub.getPos());
        assertNull(sub.getGrid());

        assertEquals("4-1-4-1", lineups.get(1).getFormation());
        assertEquals("H. Flick", lineups.get(1).getCoach().getName());
    }

    @Test
    void getFixtureLineups_withFilters() throws Exception {
        enqueue("fixtures_lineups.json");

        api.getFixtureLineups(1570443, new FixtureLineupsQuery().team(530).player(29).type("startXI"));

        assertEquals("/fixtures/lineups?fixture=1570443&team=530&player=29&type=startXI", takeRequest().getPath());
    }

    // ------------------------------------------------------------------ players

    @Test
    void getFixturePlayers_parsesNullableStatistics() throws Exception {
        enqueue("fixtures_players.json");

        List<FixturePlayers> teams = api.getFixturePlayers(1570443);

        assertEquals("/fixtures/players?fixture=1570443", takeRequest().getPath());
        assertEquals(2, teams.size());
        FixturePlayers home = teams.get(0);
        assertEquals(530, home.getTeam().getId());
        assertEquals("Atletico Madrid", home.getTeam().getName());
        assertEquals("1970-01-01T00:00:00+00:00", home.getTeam().getUpdate());
        assertEquals(4, home.getPlayers().size());

        FixturePlayers.PlayerEntry entry = home.getPlayers().get(0);
        assertEquals(7712, entry.getPlayer().getId());
        assertEquals("M. Hjulmand", entry.getPlayer().getName());
        assertEquals("https://media.api-sports.io/football/players/7712.png", entry.getPlayer().getPhoto());
        assertEquals(1, entry.getStatistics().size());
        FixturePlayers.PlayerStatistics s = entry.getFirstStatistics();
        assertEquals(18, s.getGames().getMinutes());
        assertEquals(23, s.getGames().getNumber());
        assertEquals("M", s.getGames().getPosition());
        assertEquals("5.92", s.getGames().getRating());
        assertEquals(5.92, s.getGames().getRatingValue());
        assertEquals(Boolean.FALSE, s.getGames().getCaptain());
        assertEquals(Boolean.FALSE, s.getGames().getSubstitute());
        assertEquals(0, s.getOffsides());
        assertEquals(0, s.getShots().getTotal());
        assertEquals(0, s.getShots().getOn());
        assertNull(s.getGoals().getConceded());
        assertEquals(0, s.getGoals().getAssists());
        assertEquals(0, s.getGoals().getSaves());
        assertEquals(11, s.getPasses().getTotal());
        assertEquals(0, s.getPasses().getKey());
        assertEquals("9", s.getPasses().getAccuracy());
        assertEquals(2, s.getTackles().getTotal());
        assertEquals(0, s.getTackles().getBlocks());
        assertEquals(0, s.getTackles().getInterceptions());
        assertEquals(3, s.getDuels().getTotal());
        assertEquals(3, s.getDuels().getWon());
        assertEquals(0, s.getDribbles().getAttempts());
        assertEquals(0, s.getDribbles().getSuccess());
        assertNull(s.getDribbles().getPast());
        assertEquals(0, s.getFouls().getDrawn());
        assertEquals(0, s.getFouls().getCommitted());
        assertEquals(1, s.getCards().getYellow());
        assertEquals(0, s.getCards().getRed());
        assertNull(s.getPenalty().getWon());
        assertNull(s.getPenalty().getCommitted());
        assertEquals(0, s.getPenalty().getScored());
        assertNull(s.getPenalty().getMissed());
        assertNull(s.getPenalty().getSaved());

        assertEquals("K. Adeyemi", teams.get(1).getPlayers().get(0).getPlayer().getName());
        assertEquals(6.47, teams.get(1).getPlayers().get(0).getFirstStatistics().getGames().getRatingValue());
    }

    @Test
    void getFixturePlayers_withTeamFilter() throws Exception {
        enqueue("fixtures_players.json");

        api.getFixturePlayers(1570443, new FixturePlayersQuery().team(529));

        assertEquals("/fixtures/players?fixture=1570443&team=529", takeRequest().getPath());
    }
}

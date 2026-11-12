package es.com.kete1987.apifootball.library;

import es.com.kete1987.apifootball.library.common.model.Page;
import es.com.kete1987.apifootball.library.common.util.ApiFootballException;
import es.com.kete1987.apifootball.library.odds.model.Bookmaker;
import es.com.kete1987.apifootball.library.odds.model.BookmakerOdds;
import es.com.kete1987.apifootball.library.odds.model.LiveBet;
import es.com.kete1987.apifootball.library.odds.model.LiveOddValue;
import es.com.kete1987.apifootball.library.odds.model.LiveOdds;
import es.com.kete1987.apifootball.library.odds.model.OddValue;
import es.com.kete1987.apifootball.library.odds.model.OddsMapping;
import es.com.kete1987.apifootball.library.odds.model.PrematchBet;
import es.com.kete1987.apifootball.library.odds.model.PrematchBetOdds;
import es.com.kete1987.apifootball.library.odds.model.PrematchOdds;
import es.com.kete1987.apifootball.library.odds.query.OddsQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OddsApiTest extends BaseApiTest {
    private OddsApi api;

    @BeforeEach
    void setUp() {
        api = new OddsApi(client, baseUrl, tracker);
    }

    private static void assertFixtureOdds(PrematchOdds odds) {
        assertEquals(140, odds.getLeague().getId());
        assertEquals("La Liga", odds.getLeague().getName());
        assertEquals("Spain", odds.getLeague().getCountry());
        assertEquals("https://media.api-sports.io/football/leagues/140.png", odds.getLeague().getLogo());
        assertEquals("https://media.api-sports.io/flags/es.svg", odds.getLeague().getFlag());
        assertEquals(2026, odds.getLeague().getSeason());
        assertEquals(1570461, odds.getFixture().getId());
        assertEquals("UTC", odds.getFixture().getTimezone());
        assertEquals("2026-11-22T16:00:00+00:00", odds.getFixture().getDate());
        assertEquals(1795363200L, odds.getFixture().getTimestamp());
        assertEquals("2026-11-08T16:51:14+00:00", odds.getUpdate());
        BookmakerOdds bm = odds.getBookmakers().get(0);
        assertEquals(7, bm.getId());
        assertEquals("William Hill", bm.getName());
        assertSame(bm, odds.findBookmaker(7));
        assertNull(odds.findBookmaker(999));
        PrematchBetOdds winner = bm.findBet(1);
        assertEquals("Match Winner", winner.getName());
        OddValue home = winner.getValues().get(0);
        assertEquals("Home", home.getValue());
        assertEquals("1.20", home.getOdd());
        assertEquals(1.2, home.getOddAsDouble());
        assertNull(home.getValueAsInteger());
        // type-varying value: JSON number for bet 38, string otherwise
        PrematchBetOdds exact = bm.findBet(38);
        assertEquals("0", exact.getValues().get(0).getValue());
        assertEquals(0, exact.getValues().get(0).getValueAsInteger());
        assertEquals(23.0, exact.getValues().get(0).getOddAsDouble());
        assertEquals("more 6", exact.getValues().get(2).getValue());
        assertNull(exact.getValues().get(2).getValueAsInteger());
        assertNull(bm.findBet(99999));
    }

    private static void assertSame(Object a, Object b) {
        org.junit.jupiter.api.Assertions.assertSame(a, b);
    }

    @Test
    void getOddsByFixture_parsesAndBuildsUrl() throws Exception {
        enqueue("odds_by_fixture.json");

        PrematchOdds odds = api.getOddsByFixture(1570461);

        assertFixtureOdds(odds);
        assertEquals("/odds?fixture=1570461&page=1", takeRequest().getPath());
    }

    @Test
    void getOddsByFixture_withBookmakerAndBet_buildsUrl() throws Exception {
        enqueue("odds_by_fixture.json");

        assertNotNull(api.getOddsByFixture(1570461, 7, 1));

        assertEquals("/odds?fixture=1570461&bookmaker=7&bet=1&page=1", takeRequest().getPath());
    }

    @Test
    void getOddsPage_exposesPaging() throws Exception {
        enqueue("odds_by_league_season.json");

        Page<PrematchOdds> page = api.getOddsPage(new OddsQuery().league(140).season(2026).timezone("Europe/Madrid"), 1);

        assertEquals(10, page.getResults());
        assertEquals(3, page.getItems().size());
        assertEquals(1, page.getPaging().getCurrent());
        assertEquals(2, page.getPaging().getTotal());
        assertTrue(page.hasMore());
        assertEquals(1570450, page.getItems().get(0).getFixture().getId());
        assertEquals("/odds?league=140&season=2026&timezone=Europe%2FMadrid&page=1", takeRequest().getPath());
    }

    @Test
    void getOddsPageByDate_lastPageAndNullFlag() throws Exception {
        enqueue("odds_by_date_page2.json");

        Page<PrematchOdds> page = api.getOddsPageByDate(LocalDate.of(2026, 11, 14), 2);

        assertEquals(7, page.getResults());
        assertEquals(2, page.getPaging().getCurrent());
        assertEquals(24, page.getPaging().getTotal());
        assertTrue(page.hasMore());
        assertNull(page.getItems().get(0).getLeague().getFlag());
        assertEquals("World", page.getItems().get(0).getLeague().getCountry());
        assertEquals("/odds?date=2026-11-14&page=2", takeRequest().getPath());
    }

    @Test
    void getAllOddsByLeagueSeason_walksPagesUpToMax() throws Exception {
        enqueue("odds_by_league_season.json");
        enqueue("odds_by_date_page2.json");

        List<PrematchOdds> all = api.getAllOddsByLeagueSeason(140, 2026, 2);

        assertEquals(6, all.size());
        assertEquals(2, server.getRequestCount());
        assertEquals("/odds?league=140&season=2026&page=1", takeRequest().getPath());
        assertEquals("/odds?league=140&season=2026&page=2", takeRequest().getPath());
    }

    @Test
    void getAllOdds_respectsMaxPages() throws Exception {
        enqueue("odds_by_league_season.json");

        List<PrematchOdds> all = api.getAllOdds(new OddsQuery().league(140).season(2026), 1);

        assertEquals(3, all.size());
        assertEquals(1, server.getRequestCount());
        assertEquals("/odds?league=140&season=2026&page=1", takeRequest().getPath());
    }

    @Test
    void getAllOddsByDate_buildsUrl() throws Exception {
        enqueue("odds_by_fixture.json");

        assertEquals(1, api.getAllOddsByDate(LocalDate.of(2026, 11, 22), 3).size());

        assertEquals("/odds?date=2026-11-22&page=1", takeRequest().getPath());
    }

    @Test
    void getOddsPageByLeagueSeason_buildsUrl() throws Exception {
        enqueue("odds_by_league_season.json");

        api.getOddsPageByLeagueSeason(140, 2026, 2);

        assertEquals("/odds?league=140&season=2026&page=2", takeRequest().getPath());
    }

    @Test
    void oddsQuery_coversEveryParameter() {
        OddsQuery q = new OddsQuery().fixture(1).league(2).season(2026).date("2026-11-14")
                .timezone("UTC").bookmaker(8).bet(5);
        assertEquals("{fixture=1, league=2, season=2026, date=2026-11-14, timezone=UTC, bookmaker=8, bet=5}",
                q.toParams().toString());
    }

    @Test
    void getOddsMappingPage_parses() throws Exception {
        enqueue("odds_mapping.json");

        Page<OddsMapping> page = api.getOddsMappingPage(1);

        assertEquals(100, page.getResults());
        assertEquals(31, page.getPaging().getTotal());
        OddsMapping m = page.getItems().get(0);
        assertEquals(253, m.getLeague().getId());
        assertEquals(2026, m.getLeague().getSeason());
        assertEquals(1490600, m.getFixture().getId());
        assertEquals("2026-11-07T21:00:00+00:00", m.getFixture().getDate());
        assertEquals(1794085200L, m.getFixture().getTimestamp());
        assertEquals("2026-11-07T20:01:18+00:00", m.getUpdate());
        assertEquals("/odds/mapping?page=1", takeRequest().getPath());
    }

    @Test
    void getAllOddsMapping_isCappedByMaxPages() throws Exception {
        enqueue("odds_mapping.json");
        enqueue("odds_mapping.json");

        assertEquals(6, api.getAllOddsMapping(2).size());

        assertEquals(2, server.getRequestCount());
        assertEquals("/odds/mapping?page=1", takeRequest().getPath());
        assertEquals("/odds/mapping?page=2", takeRequest().getPath());
    }

    @Test
    void getBookmakers_parsesNullName() throws Exception {
        enqueue("odds_bookmakers.json");

        List<Bookmaker> list = api.getBookmakers();

        assertEquals(3, list.size());
        assertEquals(1, list.get(0).getId());
        assertEquals("10Bet", list.get(0).getName());
        assertEquals(37, list.get(2).getId());
        assertNull(list.get(2).getName());
        assertEquals("/odds/bookmakers", takeRequest().getPath());
    }

    @Test
    void getBookmakerById_andSearch_buildUrls() throws Exception {
        enqueue("odds_bookmakers.json");
        enqueue("odds_bookmakers.json");

        assertEquals(1, api.getBookmakerById(1).getId());
        assertEquals(3, api.searchBookmakers("Bet").size());

        assertEquals("/odds/bookmakers?id=1", takeRequest().getPath());
        assertEquals("/odds/bookmakers?search=Bet", takeRequest().getPath());
    }

    @Test
    void getPrematchBets_parsesNullName() throws Exception {
        enqueue("odds_bets.json");

        List<PrematchBet> bets = api.getPrematchBets();

        assertEquals(3, bets.size());
        assertEquals(1, bets.get(0).getId());
        assertEquals("Match Winner", bets.get(0).getName());
        assertEquals(341, bets.get(1).getId());
        assertNull(bets.get(1).getName());
        assertEquals("/odds/bets", takeRequest().getPath());
    }

    @Test
    void getPrematchBetById_andSearch_buildUrls() throws Exception {
        enqueue("odds_bets.json");
        enqueue("odds_bets.json");

        assertEquals(1, api.getPrematchBetById(1).getId());
        api.searchPrematchBets("winner");

        assertEquals("/odds/bets?id=1", takeRequest().getPath());
        assertEquals("/odds/bets?search=winner", takeRequest().getPath());
    }

    @Test
    void getLiveBets_andLookups() throws Exception {
        enqueue("odds_live_bets.json");
        enqueue("odds_live_bets.json");
        enqueue("odds_live_bets.json");

        List<LiveBet> bets = api.getLiveBets();
        assertEquals(3, bets.size());
        assertEquals(1, bets.get(0).getId());
        assertEquals("Over/Under Extra Time", bets.get(0).getName());
        assertEquals(266, bets.get(2).getId());
        assertEquals(1, api.getLiveBetById(1).getId());
        assertEquals(3, api.searchLiveBets("corners").size());

        assertEquals("/odds/live/bets", takeRequest().getPath());
        assertEquals("/odds/live/bets?id=1", takeRequest().getPath());
        assertEquals("/odds/live/bets?search=corners", takeRequest().getPath());
    }

    @Test
    void getLiveOdds_parsesLiveShape() throws Exception {
        enqueue("odds_live.json");

        List<LiveOdds> list = api.getLiveOdds();

        assertEquals(1, list.size());
        LiveOdds o = list.get(0);
        assertEquals(1649008, o.getFixture().getId());
        assertEquals("First Half", o.getFixture().getStatus().getLong());
        assertEquals(14, o.getFixture().getStatus().getElapsed());
        assertEquals("14:19", o.getFixture().getStatus().getSeconds());
        assertEquals(10, o.getLeague().getId());
        assertEquals(2026, o.getLeague().getSeason());
        assertEquals(10960, o.getTeams().getHome().getId());
        assertEquals(0, o.getTeams().getHome().getGoals());
        assertEquals(1, o.getTeams().getAway().getGoals());
        assertFalse(o.getStatus().getStopped());
        assertFalse(o.getStatus().getBlocked());
        assertFalse(o.getStatus().getFinished());
        assertEquals("2026-11-12T08:14:29+00:00", o.getUpdate());
        assertEquals("Asian Handicap", o.findBet(33).getName());
        LiveOddValue v = o.findBet(33).getValues().get(0);
        assertEquals("Home", v.getValue());
        assertEquals("1.7", v.getOdd());
        assertEquals(1.7, v.getOddAsDouble());
        assertEquals("1.5", v.getHandicap());
        assertEquals(1.5, v.getHandicapAsDouble());
        assertEquals(Boolean.FALSE, v.getMain());
        assertEquals(Boolean.FALSE, v.getSuspended());
        LiveOddValue nullable = o.findBet(19).getValues().get(0);
        assertNull(nullable.getHandicap());
        assertNull(nullable.getHandicapAsDouble());
        assertNull(nullable.getMain());
        assertEquals(26.0, nullable.getOddAsDouble());
        assertNull(o.findBet(-1));
        assertEquals("/odds/live", takeRequest().getPath());
    }

    @Test
    void getLiveOdds_convenienceUrls() throws Exception {
        enqueue("odds_live.json");
        enqueue("odds_live.json");
        enqueue("odds_live.json");

        assertNotNull(api.getLiveOddsByFixture(1649008));
        assertNotNull(api.getLiveOddsByFixture(1649008, 33));
        assertEquals(1, api.getLiveOddsByLeague(10).size());

        assertEquals("/odds/live?fixture=1649008", takeRequest().getPath());
        assertEquals("/odds/live?fixture=1649008&bet=33", takeRequest().getPath());
        assertEquals("/odds/live?league=10", takeRequest().getPath());
    }

    @Test
    void errorFixture_throwsWithErrors() throws Exception {
        enqueue("error_unknown_param.json");

        ApiFootballException e = assertThrows(ApiFootballException.class,
                () -> api.getOddsPage(new OddsQuery().param("foo", "bar"), 1));

        assertEquals("The Foo field do not exist.", e.getErrors().get("foo"));
        assertEquals("/odds?foo=bar&page=1", takeRequest().getPath());
    }
}

package es.com.kete1987.apifootball.library;

import es.com.kete1987.apifootball.library.common.query.QueryValidation;
import es.com.kete1987.apifootball.library.fixtures.query.FixtureEventsQuery;
import es.com.kete1987.apifootball.library.fixtures.query.FixturesQuery;
import es.com.kete1987.apifootball.library.fixtures.query.InjuriesQuery;
import es.com.kete1987.apifootball.library.fixtures.query.RoundsQuery;
import es.com.kete1987.apifootball.library.leagues.query.LeaguesQuery;
import es.com.kete1987.apifootball.library.odds.query.OddsQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Client-side validation shared through {@link QueryValidation}, and the null rule of query setters. */
class QueryValidationTest extends BaseApiTest {

    private TeamsApi teams;
    private PlayersApi players;
    private OddsApi odds;

    @BeforeEach
    void setUp() {
        teams = new TeamsApi(client, baseUrl, tracker);
        players = new PlayersApi(client, baseUrl, tracker);
        odds = new OddsApi(client, baseUrl, tracker);
    }

    // ------------------------------------------------------------------ timezone

    @Test
    void timezone_acceptsUtcAndRegionIds() {
        for (String ok : new String[]{"UTC", "Europe/Madrid", "America/Argentina/Buenos_Aires", "Asia/Kolkata"}) {
            assertEquals(ok, QueryValidation.timezone(ok));
        }
    }

    @Test
    void timezone_rejectsOffsetsUnknownNamesAndNull() {
        for (String bad : new String[]{"+02:00", "-05:00", "Z", "GMT", "GMT+1", "UTC+1", "UT+01:00", "Etc/GMT+1",
                "utc", "CET", "EST", "Mars/Olympus", "europe/madrid", "Europe/Madrid ", "", " "}) {
            assertThrows(IllegalArgumentException.class, () -> QueryValidation.timezone(bad), bad);
        }
        assertThrows(IllegalArgumentException.class, () -> QueryValidation.timezone(null));
    }

    @Test
    void timezone_isAppliedByEveryQueryThatHasOne() {
        assertThrows(IllegalArgumentException.class, () -> new OddsQuery().timezone("Mars/Olympus"));
        assertThrows(IllegalArgumentException.class, () -> new OddsQuery().timezone("+02:00"));
        assertThrows(IllegalArgumentException.class, () -> new FixturesQuery().timezone("GMT+1"));
        assertThrows(IllegalArgumentException.class, () -> new InjuriesQuery().timezone("Z"));
        assertThrows(IllegalArgumentException.class, () -> new RoundsQuery().timezone("UTC+1"));
        assertEquals("Europe/Madrid", new OddsQuery().timezone("Europe/Madrid").toParams().get("timezone"));
    }

    // ------------------------------------------------------------------ dates

    @Test
    void oddsQueryDate_isValidated() {
        assertThrows(IllegalArgumentException.class, () -> new OddsQuery().date("2026-13-45"));
        assertThrows(IllegalArgumentException.class, () -> new OddsQuery().date("2026-2-1"));
        assertEquals("2026-02-01", new OddsQuery().date("2026-02-01").toParams().get("date"));
        assertEquals("2026-02-01", new OddsQuery().date(LocalDate.of(2026, 2, 1)).toParams().get("date"));
        assertThrows(IllegalArgumentException.class, () -> odds.getOddsByDatePage("2026-02-30", 1));
        assertThrows(IllegalArgumentException.class, () -> odds.getAllOddsByDate((LocalDate) null, 1));
        assertEquals(0, server.getRequestCount());
    }

    @Test
    void oddsByDatePage_sendsTheValidatedDate() throws Exception {
        enqueue("odds_by_date_page2.json");

        odds.getOddsByDatePage("2026-11-14", 2);

        assertEquals("/odds?date=2026-11-14&page=2", takeRequest().getPath());
    }

    @Test
    void teamStatisticsDate_isValidated() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> teams.getTeamStatistics(140, 2025, 541, "2026-13-45"));
        assertThrows(IllegalArgumentException.class, () -> teams.getTeamStatistics(140, 2025, 541, (String) null));
        assertThrows(IllegalArgumentException.class, () -> teams.getTeamStatistics(140, 2025, 541, (LocalDate) null));
        assertEquals(0, server.getRequestCount());

        enqueue("teams_statistics.json");
        teams.getTeamStatistics(140, 2025, 541, LocalDate.of(2026, 3, 1));
        assertEquals("/teams/statistics?league=140&season=2025&team=541&date=2026-03-01", takeRequest().getPath());
    }

    // ------------------------------------------------------------------ id lists

    @Test
    void playerAndCoachIdLists_rejectNullEmptyAndTooMany() {
        List<Integer> twentyOne = new ArrayList<>();
        for (int i = 1; i <= 21; i++) twentyOne.add(i);
        List<Integer> withNull = Arrays.asList(1, null);

        assertThrows(IllegalArgumentException.class, () -> players.getTrophiesByPlayers(null));
        assertThrows(IllegalArgumentException.class, () -> players.getTrophiesByPlayers(Collections.emptyList()));
        assertThrows(IllegalArgumentException.class, () -> players.getTrophiesByPlayers(twentyOne));
        assertThrows(IllegalArgumentException.class, () -> players.getTrophiesByPlayers(withNull));
        assertThrows(IllegalArgumentException.class, () -> players.getTrophiesByCoaches(withNull));
        assertThrows(IllegalArgumentException.class, () -> players.getSidelinedByPlayers(twentyOne));
        assertThrows(IllegalArgumentException.class, () -> players.getSidelinedByCoaches(Collections.emptySet()));
        assertEquals(0, server.getRequestCount());
    }

    @Test
    void idLists_acceptAnyCollection() throws Exception {
        enqueue("trophies_by_coachs.json");

        players.getTrophiesByCoaches(Set.of(2462));

        assertEquals("/trophies?coachs=2462", takeRequest().getPath());
    }

    // ------------------------------------------------------------------ setters

    @Test
    void setters_removeTheParameterOnNull() {
        FixturesQuery fixtures = new FixturesQuery().date("2026-01-01").timezone("UTC").round("R1").ids("1-2")
                .status("FT").from("2026-01-01").to("2026-01-02")
                .live("all");
        fixtures.date((String) null).timezone(null).round(null).ids((String) null).status((String) null)
                .from((LocalDate) null).to((String) null).live((String) null);
        assertTrue(fixtures.toParams().isEmpty(), fixtures.toParams().toString());

        assertTrue(new OddsQuery().date("2026-01-01").date((String) null).timezone(null).toParams().isEmpty());
        assertTrue(new InjuriesQuery().ids(List.of(1)).ids((List<Integer>) null).date((LocalDate) null)
                .toParams().isEmpty());
        assertTrue(new RoundsQuery().timezone(null).toParams().isEmpty());
        assertTrue(new FixtureEventsQuery().type(null).toParams().isEmpty());
        assertTrue(new FixturesQuery().status((String[]) null).live((List<Integer>) null).toParams().isEmpty());
    }

    @Test
    void eventType_isSentInLowerCase() {
        assertEquals("goal", new FixtureEventsQuery().type("Goal").toParams().get("type"));
        assertEquals("subst", new FixtureEventsQuery().type("SUBST").toParams().get("type"));
    }

    @Test
    void leaguesLast_acceptsOnlyTwoDigits() {
        assertThrows(IllegalArgumentException.class, () -> new LeaguesQuery().last(0));
        assertThrows(IllegalArgumentException.class, () -> new LeaguesQuery().last(100));
        assertEquals("99", new LeaguesQuery().last(99).toParams().get("last"));
        assertFalse(new LeaguesQuery().toParams().containsKey("last"));
    }
}

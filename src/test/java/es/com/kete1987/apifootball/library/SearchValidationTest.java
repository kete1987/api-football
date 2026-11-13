package es.com.kete1987.apifootball.library;

import es.com.kete1987.apifootball.library.common.query.QueryValidation;
import es.com.kete1987.apifootball.library.leagues.query.CountriesQuery;
import es.com.kete1987.apifootball.library.leagues.query.LeaguesQuery;
import es.com.kete1987.apifootball.library.odds.query.BookmakersQuery;
import es.com.kete1987.apifootball.library.odds.query.LiveBetsQuery;
import es.com.kete1987.apifootball.library.odds.query.PrematchBetsQuery;
import es.com.kete1987.apifootball.library.players.query.PlayerProfilesQuery;
import es.com.kete1987.apifootball.library.players.query.PlayersQuery;
import es.com.kete1987.apifootball.library.teams.query.CoachesQuery;
import es.com.kete1987.apifootball.library.teams.query.TeamsQuery;
import es.com.kete1987.apifootball.library.teams.query.VenuesQuery;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Truth table of the client-side {@code search} rule. Evidence: catalog "min 3" (players "min 4")
 * and the real API error for "Leganés" (migration sample af06): "The Search field may only
 * contain alpha-numeric characters and spaces."
 */
class SearchValidationTest extends BaseApiTest {

    @Test
    void generalRule_truthTable() {
        // valid
        assertEquals("celta", QueryValidation.search("celta"));
        assertEquals("rea", QueryValidation.search("rea"));          // exactly the minimum
        assertEquals("real madrid", QueryValidation.search("real madrid"));
        assertEquals("Leganes", QueryValidation.search("Leganes"));
        assertEquals("1860 Munich", QueryValidation.search("1860 Munich")); // digits are alpha-numeric
        assertEquals("123", QueryValidation.search("123"));
        // too short
        assertRejected("ma", 3);
        assertRejected("", 3);
        assertRejected("  a ", 3);       // spaces do not count
        assertRejected("   ", 3);
        // accent (the real failing case)
        assertRejected("Leganés", 3);
        assertRejected("Atlético de Madrid", 3);
        // symbols
        assertRejected("o'brien", 3);
        assertRejected("real-madrid", 3);
        assertRejected("a.c. milan", 3);
        assertRejected("real%20madrid", 3);
        assertRejected("münchen", 3);
        // null
        assertThrows(IllegalArgumentException.class, () -> QueryValidation.search(null));
    }

    @Test
    void playersRule_needsFourCharacters() {
        assertThrows(IllegalArgumentException.class,
                () -> QueryValidation.search("gom", QueryValidation.PLAYERS_SEARCH_MIN_LENGTH));
        assertEquals("gome", QueryValidation.search("gome", QueryValidation.PLAYERS_SEARCH_MIN_LENGTH));
        assertThrows(IllegalArgumentException.class, () -> new PlayersQuery().search("abc"));
        assertEquals("abcd", new PlayersQuery().search("abcd").toParams().get("search"));
        // profiles keep the general minimum of 3
        assertEquals("abc", new PlayerProfilesQuery().search("abc").toParams().get("search"));
    }

    @Test
    void messageIsClear() {
        IllegalArgumentException accent = assertThrows(IllegalArgumentException.class,
                () -> QueryValidation.search("Leganés"));
        assertTrue(accent.getMessage().contains("Leganés") && accent.getMessage().contains("accents"));
        IllegalArgumentException shortTerm = assertThrows(IllegalArgumentException.class,
                () -> QueryValidation.search("ma"));
        assertTrue(shortTerm.getMessage().contains("at least 3"));
    }

    @Test
    void everySearchSetter_validatesAndNullRemoves() {
        List<Function<String, Object>> setters = List.of(
                s -> new TeamsQuery().search(s),
                s -> new VenuesQuery().search(s),
                s -> new CoachesQuery().search(s),
                s -> new LeaguesQuery().search(s),
                s -> new CountriesQuery().search(s),
                s -> new PlayersQuery().search(s),
                s -> new PlayerProfilesQuery().search(s),
                s -> new BookmakersQuery().search(s),
                s -> new PrematchBetsQuery().search(s),
                s -> new LiveBetsQuery().search(s));
        for (Function<String, Object> setter : setters) {
            assertThrows(IllegalArgumentException.class, () -> setter.apply("Leganés"));
            assertThrows(IllegalArgumentException.class, () -> setter.apply("ma"));
            setter.apply("Leganes1");   // valid for all, including players (>= 4)
            setter.apply(null);         // null removes the parameter, never throws
        }
        assertFalse(new TeamsQuery().search("celta").search(null).toParams().containsKey("search"));
    }

    @Test
    void convenienceMethods_rejectBeforeAnyRequest() {
        TeamsApi teams = new TeamsApi(client, baseUrl, tracker);
        OddsApi odds = new OddsApi(client, baseUrl, tracker);
        PlayersApi players = new PlayersApi(client, baseUrl, tracker);
        LeaguesApi leagues = new LeaguesApi(client, baseUrl, tracker);
        assertThrows(IllegalArgumentException.class, () -> teams.searchTeams("Leganés"));
        assertThrows(IllegalArgumentException.class, () -> teams.searchVenues("ma"));
        assertThrows(IllegalArgumentException.class, () -> teams.searchCoaches("zi"));
        assertThrows(IllegalArgumentException.class, () -> odds.searchBookmakers("be"));
        assertThrows(IllegalArgumentException.class, () -> odds.searchPrematchBets("wï"));
        assertThrows(IllegalArgumentException.class, () -> odds.searchLiveBets("x"));
        assertThrows(IllegalArgumentException.class, () -> players.searchPlayersPage("gom", 140, 2026, 1));
        assertThrows(IllegalArgumentException.class, () -> players.searchPlayerProfilesPage("bü", 1));
        assertThrows(IllegalArgumentException.class, () -> leagues.searchLeagues("Primèra"));
        assertThrows(IllegalArgumentException.class, () -> leagues.searchCountries("España"));
        assertThrows(IllegalArgumentException.class, () -> leagues.searchCountries("sp"));
        assertEquals(0, server.getRequestCount());
    }

    @Test
    void toSearchTerm_makesTextAcceptable() {
        assertEquals("Leganes", QueryValidation.toSearchTerm("Leganés"));
        assertEquals("Atletico de Madrid", QueryValidation.toSearchTerm("Atlético de Madrid"));
        assertEquals("Deportivo A Coruna", QueryValidation.toSearchTerm("  Deportivo   A  Coruña "));
        assertEquals("Mgladbach", QueryValidation.toSearchTerm("M'gladbach"));
        assertEquals("AC Milan", QueryValidation.toSearchTerm("A.C. Milan"));
        assertEquals("Munchen", QueryValidation.toSearchTerm("München"));
        assertEquals("", QueryValidation.toSearchTerm("???"));
        assertEquals("", QueryValidation.toSearchTerm(null));
        // the result of the helper is accepted by the rule when long enough
        assertEquals("Leganes", QueryValidation.search(QueryValidation.toSearchTerm("Leganés")));
    }

    private static void assertRejected(String value, int min) {
        assertThrows(IllegalArgumentException.class, () -> QueryValidation.search(value, min), value);
    }
}

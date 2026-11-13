package es.com.kete1987.apifootball.library;

import com.google.gson.JsonParser;
import es.com.kete1987.apifootball.library.odds.model.Bookmaker;
import es.com.kete1987.apifootball.library.odds.model.BookmakerOdds;
import es.com.kete1987.apifootball.library.odds.model.OddValue;
import es.com.kete1987.apifootball.library.odds.model.PrematchBetOdds;
import es.com.kete1987.apifootball.library.odds.model.PrematchOdds;
import es.com.kete1987.apifootball.library.odds.util.BookmakerIds;
import es.com.kete1987.apifootball.library.odds.util.PrematchBetIds;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OddsHelpersTest extends BaseApiTest {
    private OddsApi api;

    @BeforeEach
    void setUp() {
        api = new OddsApi(client, baseUrl, tracker);
    }

    /** Hand-written wrapper around values copied from a real Bet365 "Exact Score" list (fixture 1570461). */
    private static final String EXACT_SCORE_JSON = "{\"id\":10,\"name\":\"Exact Score\",\"values\":["
            + "{\"value\":\"1:0\",\"odd\":\"10.00\"},{\"value\":\"2:1\",\"odd\":\"10.00\"},"
            + "{\"value\":\"0:0\",\"odd\":\"21.00\"},{\"value\":\"0:1\",\"odd\":\"34.00\"},"
            + "{\"value\":\"1:2\",\"odd\":\"34.00\"},{\"value\":\"9:1\",\"odd\":\"351.00\"}]}";

    private static PrematchBetOdds exactScoreBet() {
        return new com.google.gson.Gson().fromJson(EXACT_SCORE_JSON, PrematchBetOdds.class);
    }

    // ------------------------------------------------------------------ 1X2 and findValue

    @Test
    void matchWinner_fromRealFixture() throws Exception {
        enqueue("odds_by_fixture.json");

        PrematchOdds odds = api.getOddsByFixture(1570461);
        BookmakerOdds williamHill = odds.findBookmaker(BookmakerIds.WILLIAM_HILL);
        PrematchBetOdds mw = williamHill.findMatchWinner();

        assertEquals(PrematchBetIds.MATCH_WINNER, mw.getId());
        assertEquals(1.20, mw.getHomeOdd());
        assertEquals(6.5, mw.getDrawOdd());
        assertEquals(12.0, mw.getAwayOdd());
        assertNull(williamHill.findExactScore());
        assertNull(odds.findBookmaker(BookmakerIds.BET365));
        assertEquals("/odds?fixture=1570461&page=1", takeRequest().getPath());
    }

    @Test
    void findValue_isCaseInsensitiveTrimmedAndNullSafe() throws Exception {
        enqueue("odds_by_fixture.json");
        PrematchBetOdds mw = api.getOddsByFixture(1570461).findBookmaker(BookmakerIds.BETANO).findMatchWinner();

        assertEquals("1.22", mw.findValue("home").getOdd());
        assertEquals("6.80", mw.findValue("  DRAW ").getOdd());
        assertEquals("Away", mw.findValue("Away").getValue());
        assertNull(mw.findValue("Over"));
        assertNull(mw.findValue(null));
        assertNull(mw.findValue(""));
    }

    @Test
    void oneXTwoAccessors_nullWhenAbsent() {
        PrematchBetOdds empty = new com.google.gson.Gson().fromJson("{\"id\":1}", PrematchBetOdds.class);
        assertNull(empty.getHomeOdd());
        assertNull(empty.getDrawOdd());
        assertNull(empty.getAwayOdd());
        assertNull(empty.findValue("Home"));

        // hand-written: odd missing / not a number
        PrematchBetOdds odd = new com.google.gson.Gson().fromJson(
                "{\"id\":1,\"values\":[{\"value\":\"Home\"},{\"value\":\"Draw\",\"odd\":\"n/a\"}]}",
                PrematchBetOdds.class);
        assertNull(odd.getHomeOdd());
        assertNull(odd.getDrawOdd());
        assertNull(odd.getAwayOdd());
    }

    // ------------------------------------------------------------------ exact score

    @Test
    void exactScore_isHomeColonAway() {
        PrematchBetOdds bet = exactScoreBet();

        OddValue oneNil = bet.findValue("1:0");
        assertEquals(1, oneNil.getExactScoreHome());
        assertEquals(0, oneNil.getExactScoreAway());
        // real data: Real Madrid (home, 1.18 favourite) 1:0 costs 10.00 but 0:1 costs 34.00
        assertEquals(10.0, oneNil.getOddAsDouble());
        OddValue nilOne = bet.findValue("0:1");
        assertEquals(0, nilOne.getExactScoreHome());
        assertEquals(1, nilOne.getExactScoreAway());
        assertEquals(34.0, nilOne.getOddAsDouble());
        assertEquals(9, bet.findValue("9:1").getExactScoreHome());
        assertEquals(2, bet.findValue("1:2").getExactScoreAway());
    }

    @Test
    void exactScore_nullWhenNotAScore() {
        // hand-written values of other bets
        for (String raw : new String[]{"\"Home\"", "\"more 6\"", "5", "\"1-0\"", "\"1:\"", "\":1\"", "\"a:b\"", "\"1:0:0\"", "null"}) {
            OddValue v = new com.google.gson.Gson().fromJson("{\"value\":" + raw + ",\"odd\":\"2.00\"}", OddValue.class);
            assertNull(v.getExactScoreHome(), raw);
            assertNull(v.getExactScoreAway(), raw);
        }
        OddValue spaced = new com.google.gson.Gson().fromJson("{\"value\":\" 2 : 1 \"}", OddValue.class);
        assertEquals(2, spaced.getExactScoreHome());
        assertEquals(1, spaced.getExactScoreAway());
    }

    // ------------------------------------------------------------------ update

    @Test
    void updateAsInstant_parsesRealFixture() throws Exception {
        enqueue("odds_by_fixture.json");
        PrematchOdds odds = api.getOddsByFixture(1570461);

        assertEquals("2026-11-08T16:51:14+00:00", odds.getUpdate());
        assertEquals(Instant.parse("2026-11-08T16:51:14Z"), odds.getUpdateAsInstant());
    }

    @Test
    void updateAsInstant_nullSafe() {
        com.google.gson.Gson gson = new com.google.gson.Gson();
        assertNull(gson.fromJson("{}", PrematchOdds.class).getUpdateAsInstant());
        assertNull(gson.fromJson("{\"update\":\"yesterday\"}", PrematchOdds.class).getUpdateAsInstant());
        // hand-written: offset other than UTC
        assertEquals(Instant.parse("2026-11-08T15:51:14Z"),
                gson.fromJson("{\"update\":\"2026-11-08T16:51:14+01:00\"}", PrematchOdds.class).getUpdateAsInstant());
    }

    // ------------------------------------------------------------------ constants

    @Test
    void bet_ids() {
        assertEquals(1, PrematchBetIds.MATCH_WINNER);
        assertEquals(10, PrematchBetIds.EXACT_SCORE);
    }

    @Test
    void bookmakerIds_matchAppendixAndFixture() throws Exception {
        Map<Integer, String> appendix = appendixBookmakers();
        Map<Integer, String> expectedNames = new HashMap<>();
        expectedNames.put(BookmakerIds.BET365, "Bet365");
        expectedNames.put(BookmakerIds.WILLIAM_HILL, "William Hill");
        expectedNames.put(BookmakerIds.BETFAIR, "Betfair");
        expectedNames.put(BookmakerIds.SPORT_888, "888Sport");
        expectedNames.put(BookmakerIds.INTERWETTEN, "Interwetten");
        expectedNames.put(BookmakerIds.BET_10, "10Bet");
        expectedNames.put(BookmakerIds.MARATHONBET, "Marathonbet");
        expectedNames.put(BookmakerIds.PINNACLE, "Pinnacle");
        expectedNames.put(BookmakerIds.BWIN, "Bwin");
        expectedNames.put(BookmakerIds.ONE_X_BET, "1xBet");
        expectedNames.put(BookmakerIds.UNIBET, "Unibet");
        expectedNames.put(BookmakerIds.BETWAY, "Betway");
        expectedNames.put(BookmakerIds.BETSSON, "Betsson");
        expectedNames.put(BookmakerIds.BETANO, "Betano");
        expectedNames.put(BookmakerIds.BETVICTOR, "BetVictor");
        // every public constant is listed above (distinct ids, none forgotten)
        assertEquals(BookmakerIds.class.getFields().length, expectedNames.size());
        for (Map.Entry<Integer, String> e : expectedNames.entrySet()) {
            assertEquals(e.getValue(), appendix.get(e.getKey()), "Appendix A, id " + e.getKey());
        }

        // the real /odds/bookmakers fixture only holds some of them: check those too
        enqueue("odds_bookmakers.json");
        List<Bookmaker> fixture = api.getBookmakers();
        int checked = 0;
        for (Bookmaker b : fixture) {
            String expected = expectedNames.get(b.getId());
            if (expected != null) {
                assertEquals(expected, b.getName(), "odds_bookmakers.json, id " + b.getId());
                checked++;
            }
        }
        assertTrue(checked >= 1, "the fixture must contain at least one constant");
    }

    private static Map<Integer, String> appendixBookmakers() throws Exception {
        String catalog = new String(Files.readAllBytes(Path.of("docs", "API_CATALOG.md")), StandardCharsets.UTF_8);
        int from = catalog.indexOf("### Bookmakers (`/odds/bookmakers`");
        int to = catalog.indexOf("### Pre-match bets", from);
        assertTrue(from >= 0 && to > from, "Appendix A bookmakers section not found");
        Map<Integer, String> map = new HashMap<>();
        Matcher m = Pattern.compile("(\\d+) = ([^;\\r\\n]+?)(?:;|\\s*$)", Pattern.MULTILINE)
                .matcher(catalog.substring(from, to));
        while (m.find()) map.put(Integer.valueOf(m.group(1)), m.group(2).trim());
        assertNotNull(map.get(8));
        return map;
    }
}

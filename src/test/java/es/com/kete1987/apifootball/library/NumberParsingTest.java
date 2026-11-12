package es.com.kete1987.apifootball.library;

import es.com.kete1987.apifootball.library.common.util.NumberParsing;
import es.com.kete1987.apifootball.library.fixtures.model.FixtureLineup;
import es.com.kete1987.apifootball.library.fixtures.model.FixturePlayers;
import es.com.kete1987.apifootball.library.teams.model.Coach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** {@link NumberParsing} and the typed model getters built on it. */
class NumberParsingTest extends BaseApiTest {

    @Test
    void parseInteger_isStrict() {
        assertEquals(5, NumberParsing.parseInteger("5"));
        assertEquals(-3, NumberParsing.parseInteger(" -3 "));
        assertNull(NumberParsing.parseInteger("5.0"));
        assertNull(NumberParsing.parseInteger("37%"));
        assertNull(NumberParsing.parseInteger("3000000000"));
        assertNull(NumberParsing.parseInteger(""));
        assertNull(NumberParsing.parseInteger(null));
    }

    @Test
    void parseDouble_acceptsOnlyFiniteDecimals() {
        assertEquals(1.71, NumberParsing.parseDouble("1.71"));
        assertEquals(26.0, NumberParsing.parseDouble("26"));
        assertEquals(-1.0, NumberParsing.parseDouble("-1"));
        assertNull(NumberParsing.parseDouble("37%"));
        assertNull(NumberParsing.parseDouble("NaN"));
        assertNull(NumberParsing.parseDouble("Infinity"));
        assertNull(NumberParsing.parseDouble("0x10"));
        assertNull(NumberParsing.parseDouble(null));
    }

    @Test
    void parsePercent_stripsTheSign() {
        assertEquals(37.0, NumberParsing.parsePercent("37%"));
        assertEquals(8.97, NumberParsing.parsePercent("8.97%"));
        assertEquals(37.0, NumberParsing.parsePercent("37"));
        assertNull(NumberParsing.parsePercent("%"));
        assertNull(NumberParsing.parsePercent("abc%"));
        assertNull(NumberParsing.parsePercent(null));
    }

    @Test
    void parseLeadingInteger_readsTheNumberBeforeAUnit() {
        assertEquals(185, NumberParsing.parseLeadingInteger("185 cm"));
        assertEquals(78, NumberParsing.parseLeadingInteger("78kg"));
        assertEquals(68, NumberParsing.parseLeadingInteger("68%"));
        assertEquals(187, NumberParsing.parseLeadingInteger("187"));
        assertNull(NumberParsing.parseLeadingInteger("185.5 cm"));
        assertNull(NumberParsing.parseLeadingInteger("cm 185"));
        assertNull(NumberParsing.parseLeadingInteger(null));
    }

    @Test
    void coach_exposesHeightAndWeightAsNumbers() throws Exception {
        enqueue("coachs_by_team.json");

        List<Coach> coaches = new TeamsApi(client, baseUrl, tracker).getCoachesByTeam(541);

        assertEquals(185, coaches.get(0).getHeightCm());
        assertEquals(78, coaches.get(0).getWeightKg());
        assertNull(coaches.get(1).getHeightCm());
        assertNull(coaches.get(1).getWeightKg());
    }

    @Test
    void fixturePlayers_exposeAccuracyAndRatingAsNumbers() throws Exception {
        enqueue("fixtures_players.json");

        FixturePlayers.PlayerStatistics s = new FixturesApi(client, baseUrl, tracker).getFixturePlayers(1)
                .get(0).getPlayers().get(0).getFirstStatistics();

        assertEquals(9, s.getPasses().getAccuracyValue());
        assertEquals(5.92, s.getGames().getRatingValue());
    }

    @Test
    void lineupColors_areMappedWhenPresent() throws Exception {
        enqueueBody("{\"get\":\"fixtures/lineups\",\"parameters\":[],\"errors\":[],\"results\":1,"
                + "\"paging\":{\"current\":1,\"total\":1},\"response\":[{\"team\":{\"id\":1,\"name\":\"A\",\"logo\":\"l\","
                + "\"colors\":{\"player\":{\"primary\":\"5badff\",\"number\":\"ffffff\",\"border\":\"5badff\"},"
                + "\"goalkeeper\":{\"primary\":\"99ff99\",\"number\":\"000000\",\"border\":\"99ff99\"}}}},"
                + "{\"team\":{\"id\":2,\"name\":\"B\",\"logo\":\"l\",\"colors\":null}}]}", 200);

        List<FixtureLineup> lineups = new FixturesApi(client, baseUrl, tracker).getFixtureLineups(1);

        FixtureLineup.Colors colors = lineups.get(0).getTeam().getColors();
        assertEquals("5badff", colors.getPlayer().getPrimary());
        assertEquals("ffffff", colors.getPlayer().getNumber());
        assertEquals("99ff99", colors.getGoalkeeper().getBorder());
        assertNull(lineups.get(1).getTeam().getColors());
    }
}

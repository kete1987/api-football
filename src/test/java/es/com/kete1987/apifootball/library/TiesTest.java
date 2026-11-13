package es.com.kete1987.apifootball.library;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import es.com.kete1987.apifootball.library.competition.model.Tie;
import es.com.kete1987.apifootball.library.competition.util.TieStates;
import es.com.kete1987.apifootball.library.competition.util.Ties;
import es.com.kete1987.apifootball.library.fixtures.model.Fixture;
import es.com.kete1987.apifootball.library.fixtures.model.FixtureTeam;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link Ties} and {@link Tie} on real fixtures: every elimination tie of the Champions League
 * 2025-26 and of the World Cup 2026. Edge cases (unfinished, live, abandoned, awarded legs) are
 * real legs with their status and score edited in the test, marked as such.
 */
class TiesTest extends BaseApiTest {

    private static final int GALATASARAY = 645;
    private static final int JUVENTUS = 496;
    private static final int LUDOGORETS = 566;
    private static final int DINAMO_MINSK = 394;
    private static final int PSG = 85;
    private static final int ARSENAL = 42;

    /**
     * The 68 two-legged ties of the Champions League 2025-26, with the aggregate, winner and detail
     * of Sportmonks' {@code aggregates} (source: {@code samples/clasificaciones/sm_stages_25580_cl.json}
     * of the migration analysis, season 25580; Sportmonks data is not stored in this repository).
     * Each row: AF round, AF id of team A (home in the first leg), AF id of team B, aggregate A,
     * aggregate B, AF id of the winner, Sportmonks detail ({@code FT} = "After Full Time",
     * {@code AET} = "After Extra Time", {@code PEN} = "After Penalties"). Team ids differ between the
     * providers, so the Sportmonks winner was matched to the API-Football team by name and given the
     * higher figure of the Sportmonks result; the comment after each row quotes Sportmonks.
     */
    private static final Object[][] SPORTMONKS_AGGREGATES = {
            {"1st Qualifying Round", 580, 667, 2, 4, 667, "FT"}, // Vikingur Gota - Lincoln Red Imps FC; SM "2-4" Víkingur Gøta vs Lincoln Red Imps
            {"1st Qualifying Round", 3684, 579, 3, 2, 3684, "FT"}, // FC Noah - Buducnost Podgorica; SM "3-2" FC Noah vs FK Budućnost Podgorica
            {"1st Qualifying Round", 3854, 583, 2, 1, 3854, "FT"}, // Shelbourne - Linfield; SM "2-1" Shelbourne vs Linfield FC
            {"1st Qualifying Round", 3327, 276, 1, 5, 276, "FT"}, // Egnatia Rrogozhinë - Breidablik; SM "1-5" KF Egnatia vs Breidablik Kópavogur
            {"1st Qualifying Round", 559, 3342, 4, 3, 559, "FT"}, // FCSB - Inter Club d'Escaldes; SM "4-3" FCSB vs Inter Club d'Escaldes
            {"1st Qualifying Round", 566, 394, 3, 2, 566, "AET"}, // Ludogorets - Dinamo Minsk; SM "3-2" Ludogorets vs Dinamo Minsk
            {"1st Qualifying Round", 5308, 588, 1, 4, 588, "FT"}, // Virtus - Zrinjski; SM "1-4" SS Virtus vs HŠK Zrinjski Mostar
            {"1st Qualifying Round", 586, 4626, 2, 2, 4626, "PEN"}, // FK Zalgiris Vilnius - Hamrun Spartans; SM "2-2" FK Žalgiris vs Ħamrun Spartans FC
            {"1st Qualifying Round", 677, 664, 1, 3, 664, "FT"}, // Olimpija Ljubljana - Kairat Almaty; SM "1-3" NK Olimpija Ljubljana vs Kairat Almaty
            {"1st Qualifying Round", 3502, 375, 2, 6, 375, "FT"}, // FC Iberia 1999 - Malmo FF; SM "2-6" FC Iberia 1999 vs Malmö FF
            {"1st Qualifying Round", 1165, 691, 1, 0, 1165, "FT"}, // KuPS - Milsami Orhei; SM "1-0" KuPS vs FC Milsami Orhei
            {"1st Qualifying Round", 2273, 4160, 0, 2, 4160, "FT"}, // FC Levadia Tallinn - Rīgas FS; SM "0-2" FCI Levadia Tallinn vs RFS
            {"1st Qualifying Round", 14281, 684, 4, 2, 14281, "FT"}, // Drita - FC Differdange 03; SM "4-2" FC Drita vs Differdange FC 03
            {"1st Qualifying Round", 354, 609, 1, 2, 609, "AET"}, // The New Saints - Shkendija; SM "1-2" The New Saints vs KF Shkëndija
            {"2nd Qualifying Round", 667, 598, 1, 6, 598, "FT"}, // Lincoln Red Imps FC - FK Crvena Zvezda; SM "1-6" Crvena Zvezda vs Lincoln Red Imps
            {"2nd Qualifying Round", 3854, 556, 0, 4, 556, "FT"}, // Shelbourne - Qarabag; SM "0-4" Qarabağ vs Shelbourne
            {"2nd Qualifying Round", 561, 566, 1, 3, 566, "AET"}, // HNK Rijeka - Ludogorets; SM "1-3" Rijeka vs Ludogorets
            {"2nd Qualifying Round", 1165, 664, 2, 3, 664, "FT"}, // KuPS - Kairat Almaty; SM "2-3" KuPS vs Kairat
            {"2nd Qualifying Round", 656, 588, 6, 2, 656, "FT"}, // Slovan Bratislava - Zrinjski; SM "6-2" Slovan Bratislava vs Zrinjski
            {"2nd Qualifying Round", 4626, 572, 0, 6, 572, "FT"}, // Hamrun Spartans - Dynamo Kyiv; SM "0-6" Hamrun Spartans vs Dynamo Kyiv
            {"2nd Qualifying Round", 400, 14281, 3, 0, 400, "FT"}, // FC Copenhagen - Drita; SM "3-0" Drita vs FC København
            {"2nd Qualifying Round", 257, 617, 3, 1, 257, "FT"}, // Rangers - Panathinaikos; SM "3-1" Rangers vs Panathinaikos
            {"2nd Qualifying Round", 3684, 651, 4, 6, 651, "FT"}, // FC Noah - Ferencvarosi TC; SM "4-6" Noah vs Ferencvárosi
            {"2nd Qualifying Round", 3403, 604, 2, 1, 3403, "FT"}, // Pafos - Maccabi Tel Aviv; SM "2-1" Pafos FC vs Maccabi Tel Aviv
            {"2nd Qualifying Round", 4160, 375, 1, 5, 375, "FT"}, // Rīgas FS - Malmo FF; SM "1-5" Rīgas FS vs Malmö FF
            {"2nd Qualifying Round", 347, 276, 8, 1, 347, "FT"}, // Lech Poznan - Breidablik; SM "8-1" Breidablik vs Lech Poznań
            {"2nd Qualifying Round", 609, 559, 3, 1, 609, "FT"}, // Shkendija - FCSB; SM "3-1" FCSB vs Skendija 79
            {"2nd Qualifying Round", 319, 571, 2, 5, 571, "FT"}, // Brann - Red Bull Salzburg; SM "2-5" SK Brann vs Red Bull Salzburg
            {"2nd Qualifying Round", 567, 2184, 3, 2, 567, "FT"}, // Plzen - Servette FC; SM "3-2" Viktoria Plzeň vs Servette
            {"3rd Qualifying Round", 375, 400, 0, 5, 400, "FT"}, // Malmo FF - FC Copenhagen; SM "0-5" Malmö FF vs FC København
            {"3rd Qualifying Round", 609, 556, 1, 6, 556, "FT"}, // Shkendija - Qarabag; SM "1-6" Skendija 79 vs Qarabağ
            {"3rd Qualifying Round", 572, 3403, 0, 3, 3403, "FT"}, // Dynamo Kyiv - Pafos; SM "0-3" Dynamo Kyiv vs Paphos
            {"3rd Qualifying Round", 664, 656, 1, 1, 664, "PEN"}, // Kairat Almaty - Slovan Bratislava; SM "1-1" Kairat vs Slovan Bratislava
            {"3rd Qualifying Round", 257, 567, 4, 2, 257, "FT"}, // Rangers - Plzen; SM "4-2" Rangers vs Viktoria Plzeň
            {"3rd Qualifying Round", 347, 598, 2, 4, 598, "FT"}, // Lech Poznan - FK Crvena Zvezda; SM "2-4" Lech Poznań vs Crvena Zvezda
            {"3rd Qualifying Round", 571, 569, 2, 4, 569, "FT"}, // Red Bull Salzburg - Club Brugge KV; SM "2-4" Salzburg vs Club Brugge
            {"3rd Qualifying Round", 566, 651, 0, 3, 651, "FT"}, // Ludogorets - Ferencvarosi TC; SM "0-3" Ludogorets vs Ferencvárosi
            {"3rd Qualifying Round", 84, 211, 0, 4, 211, "FT"}, // Nice - Benfica; SM "0-4" Nice vs Benfica
            {"3rd Qualifying Round", 209, 611, 4, 6, 611, "FT"}, // Feyenoord - Fenerbahçe; SM "4-6" Feyenoord vs Fenerbahçe
            {"Quarter-finals", 529, 530, 2, 3, 530, "FT"}, // Barcelona - Atletico Madrid; SM "2-3" FC Barcelona vs Atlético Madrid
            {"Quarter-finals", 228, 42, 0, 1, 42, "FT"}, // Sporting CP - Arsenal; SM "0-1" Sporting CP vs Arsenal
            {"Quarter-finals", 541, 157, 4, 6, 157, "FT"}, // Real Madrid - Bayern München; SM "4-6" Real Madrid vs FC Bayern München
            {"Quarter-finals", 85, 40, 4, 0, 85, "FT"}, // Paris Saint Germain - Liverpool; SM "4-0" Paris Saint Germain vs Liverpool
            {"Semi-finals", 530, 42, 1, 2, 42, "FT"}, // Atletico Madrid - Arsenal; SM "1-2" Atlético Madrid vs Arsenal
            {"Semi-finals", 85, 157, 6, 5, 85, "FT"}, // Paris Saint Germain - Bayern München; SM "6-5" Paris Saint Germain vs FC Bayern München
            {"Round of 32", 327, 505, 5, 2, 327, "FT"}, // Bodo/Glimt - Inter; SM "5-2" Bodø / Glimt vs Inter
            {"Round of 32", 553, 168, 0, 2, 168, "FT"}, // Olympiakos Piraeus - Bayer Leverkusen; SM "0-2" Olympiacos F.C. vs Bayer 04 Leverkusen
            {"Round of 32", 211, 541, 1, 3, 541, "FT"}, // Benfica - Real Madrid; SM "1-3" Benfica vs Real Madrid
            {"Round of 32", 569, 530, 4, 7, 530, "FT"}, // Club Brugge KV - Atletico Madrid; SM "4-7" Club Brugge vs Atlético Madrid
            {"Round of 32", 556, 34, 3, 9, 34, "FT"}, // Qarabag - Newcastle; SM "3-9" Qarabağ vs Newcastle United
            {"Round of 32", 165, 499, 3, 4, 499, "FT"}, // Borussia Dortmund - Atalanta; SM "3-4" Borussia Dortmund vs Atalanta
            {"Round of 32", 645, 496, 7, 5, 645, "AET"}, // Galatasaray - Juventus; SM "7-5" Galatasaray vs Juventus
            {"Round of 32", 91, 85, 4, 5, 85, "FT"}, // Monaco - Paris Saint Germain; SM "4-5" Monaco vs Paris Saint Germain
            {"Round of 16", 530, 47, 7, 5, 530, "FT"}, // Atletico Madrid - Tottenham; SM "7-5" Atlético Madrid vs Tottenham Hotspur
            {"Round of 16", 645, 40, 1, 4, 40, "FT"}, // Galatasaray - Liverpool; SM "1-4" Galatasaray vs Liverpool
            {"Round of 16", 168, 42, 1, 3, 42, "FT"}, // Bayer Leverkusen - Arsenal; SM "1-3" Bayer 04 Leverkusen vs Arsenal
            {"Round of 16", 541, 50, 5, 1, 541, "FT"}, // Real Madrid - Manchester City; SM "5-1" Real Madrid vs Manchester City
            {"Round of 16", 34, 529, 3, 8, 529, "FT"}, // Newcastle - Barcelona; SM "3-8" Newcastle United vs FC Barcelona
            {"Round of 16", 327, 228, 3, 5, 228, "AET"}, // Bodo/Glimt - Sporting CP; SM "3-5" Bodø / Glimt vs Sporting CP
            {"Round of 16", 85, 49, 8, 2, 85, "FT"}, // Paris Saint Germain - Chelsea; SM "8-2" Paris Saint Germain vs Chelsea
            {"Round of 16", 499, 157, 2, 10, 157, "FT"}, // Atalanta - Bayern München; SM "2-10" Atalanta vs FC Bayern München
            {"Play-offs", 247, 664, 0, 0, 664, "PEN"}, // Celtic - Kairat Almaty; SM "0-0" Celtic vs Kairat
            {"Play-offs", 598, 3403, 2, 3, 3403, "FT"}, // FK Crvena Zvezda - Pafos; SM "2-3" Crvena Zvezda vs Paphos
            {"Play-offs", 551, 400, 1, 3, 400, "FT"}, // FC Basel 1893 - FC Copenhagen; SM "1-3" Basel vs FC København
            {"Play-offs", 327, 637, 6, 2, 327, "FT"}, // Bodo/Glimt - Sturm Graz; SM "6-2" Bodø / Glimt vs Sturm Graz
            {"Play-offs", 651, 556, 4, 5, 556, "FT"}, // Ferencvarosi TC - Qarabag; SM "4-5" Ferencvárosi vs Qarabağ
            {"Play-offs", 257, 569, 1, 9, 569, "FT"}, // Rangers - Club Brugge KV; SM "1-9" Rangers vs Club Brugge
            {"Play-offs", 611, 211, 0, 1, 211, "FT"}, // Fenerbahçe - Benfica; SM "0-1" Fenerbahçe vs Benfica
    };

    private FixturesApi api;

    @BeforeEach
    void setUp() {
        api = new FixturesApi(client, baseUrl, tracker);
    }

    private List<Fixture> championsLeague() throws Exception {
        enqueue("fixtures_by_league_ucl_2025.json");
        List<Fixture> fixtures = api.getFixturesByLeague(2, 2025);
        assertEquals("/fixtures?league=2&season=2025", takeRequest().getPath());
        return fixtures;
    }

    private List<Fixture> worldCup() throws Exception {
        enqueue("fixtures_by_league_world_cup_2026.json");
        List<Fixture> fixtures = api.getFixturesByLeague(1, 2026);
        assertEquals("/fixtures?league=1&season=2026", takeRequest().getPath());
        return fixtures;
    }

    private static Tie find(List<Tie> ties, String round, int teamA, int teamB) {
        for (Tie tie : ties) {
            if (round.equals(tie.getRound()) && tie.getTeamA().getId() == teamA && tie.getTeamB().getId() == teamB) {
                return tie;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ real data: Champions League

    @Test
    void championsLeague_all68TwoLeggedTiesMatchSportmonksAggregates() throws Exception {
        List<Tie> ties = Ties.ofEliminationRounds(championsLeague());

        assertEquals(69, ties.size());                 // 68 two-legged ties + the final
        assertEquals(68, SPORTMONKS_AGGREGATES.length);
        Set<Tie> covered = new HashSet<>();
        for (Object[] row : SPORTMONKS_AGGREGATES) {
            String round = (String) row[0];
            Tie tie = find(ties, round, (Integer) row[1], (Integer) row[2]);
            String label = Arrays.toString(row);
            assertNotNull(tie, label);
            covered.add(tie);
            assertTrue(tie.isTwoLegged(), label);
            assertEquals(TieStates.FINISHED, tie.getState(), label);
            assertEquals(row[3], tie.getAggregateA(), label);
            assertEquals(row[4], tie.getAggregateB(), label);
            assertEquals(row[5], tie.getWinnerId(), label);
            assertEquals(row[5].equals(row[1]) ? row[2] : row[1], tie.getLoser().getId(), label);
            String detail = (String) row[6];
            assertEquals("PEN".equals(detail), tie.isDecidedOnPenalties(), label);
            assertEquals("AET".equals(detail), tie.isDecidedInExtraTime(), label);
            assertTrue(tie.getFirstLeg().getFixture().getTimestamp() < tie.getSecondLeg().getFixture().getTimestamp());
        }
        assertEquals(68, covered.size());

        Tie last = ties.get(ties.size() - 1);
        assertEquals("Final", last.getRound());
        assertFalse(covered.contains(last));
    }

    @Test
    void championsLeague_galatasarayJuventus_secondLegWinnerFlagIsNotTheTieWinner() throws Exception {
        Tie tie = find(Ties.of(championsLeague()), "Round of 32", GALATASARAY, JUVENTUS);

        Fixture first = tie.getFirstLeg();
        Fixture second = tie.getSecondLeg();
        assertEquals(1515514, first.getFixture().getId());       // Galatasaray 5-2 Juventus
        assertEquals(1515527, second.getFixture().getId());      // Juventus 3-2 Galatasaray (AET)
        // per match, the API says Juventus won the second leg...
        FixtureTeam juventusAtHome = second.getTeams().getHome();
        assertEquals(JUVENTUS, juventusAtHome.getId());
        assertTrue(juventusAtHome.getWinner());
        // ...goals include extra time, fulltime is the 90-minute score
        assertEquals(3, second.getGoals().getHome());
        assertEquals(2, second.getGoals().getAway());
        assertEquals(0, second.getScore().getFulltime().getAway());
        // ...but Galatasaray go through 7-5 on aggregate
        assertEquals(7, tie.getAggregate(GALATASARAY));
        assertEquals(5, tie.getAggregate(JUVENTUS));
        assertEquals(GALATASARAY, tie.getWinnerId());
        assertEquals(JUVENTUS, tie.getLoser().getId());
        assertTrue(tie.isDecidedInExtraTime());
        assertFalse(tie.isDecidedOnPenalties());
        assertNull(tie.getPenaltyScore(GALATASARAY));
        assertEquals(JUVENTUS, tie.getOpponent(GALATASARAY).getId());
        assertNull(tie.getOpponent(1));
        assertNull(tie.getAggregate(1));
    }

    @Test
    void championsLeague_secondLegDrawnAfterExtraTimeStillHasATieWinner() throws Exception {
        Tie tie = find(Ties.of(championsLeague()), "1st Qualifying Round", LUDOGORETS, DINAMO_MINSK);

        // Dinamo Minsk 2-2 Ludogorets after extra time: neither side has winner = true
        assertNull(tie.getSecondLeg().getTeams().getHome().getWinner());
        assertNull(tie.getSecondLeg().getTeams().getAway().getWinner());
        assertEquals(3, tie.getAggregateA());
        assertEquals(2, tie.getAggregateB());
        assertEquals(LUDOGORETS, tie.getWinnerId());
        assertTrue(tie.isDecidedInExtraTime());
    }

    @Test
    void championsLeague_finalIsASingleLegTieDecidedOnPenalties() throws Exception {
        Map<String, List<Tie>> byRound = Ties.byRound(championsLeague());
        List<Tie> finals = byRound.get("Final");
        assertEquals(1, finals.size());
        Tie tie = finals.get(0);

        assertTrue(tie.isSingleLeg());
        assertFalse(tie.isTwoLegged());
        assertNull(tie.getSecondLeg());
        assertEquals(tie.getFirstLeg(), tie.getLastLeg());
        assertEquals(PSG, tie.getTeamA().getId());     // nominal home side at a neutral venue
        assertEquals(1, tie.getAggregate(PSG));
        assertEquals(1, tie.getAggregate(ARSENAL));
        assertEquals(4, tie.getPenaltyScore(PSG));
        assertEquals(3, tie.getPenaltyScore(ARSENAL));
        assertTrue(tie.isDecidedOnPenalties());
        assertFalse(tie.isDecidedInExtraTime());
        assertEquals(PSG, tie.getWinnerId());
    }

    @Test
    void championsLeague_roundOf32IsTwoLeggedWithEightTies() throws Exception {
        Map<String, List<Tie>> byRound = Ties.byRound(championsLeague());

        assertEquals(Arrays.asList("1st Qualifying Round", "2nd Qualifying Round", "3rd Qualifying Round",
                "Play-offs", "League Stage - 1", "League Stage - 8", "Round of 32", "Round of 16",
                "Quarter-finals", "Semi-finals", "Final"), new ArrayList<>(byRound.keySet()));
        int[] counts = {14, 15, 10, 7, 2, 2, 8, 8, 4, 2, 1};
        int i = 0;
        for (List<Tie> round : byRound.values()) assertEquals(counts[i++], round.size());
        assertEquals(2, Ties.legsPerTie(byRound.get("Round of 32")));
        assertEquals(1, Ties.legsPerTie(byRound.get("League Stage - 1")));
        assertEquals(LUDOGORETS, Ties.findByTeam(byRound.get("3rd Qualifying Round"), LUDOGORETS).getTeamA().getId());
        assertNull(Ties.findByTeam(byRound.get("Final"), LUDOGORETS));
    }

    // ------------------------------------------------------------------ real data: World Cup

    @Test
    void worldCup_all32KnockoutMatchesAreSingleLegTiesWhoseWinnerMatchesTheApi() throws Exception {
        List<Tie> ties = Ties.ofEliminationRounds(worldCup());

        assertEquals(32, ties.size());
        int penalties = 0;
        int extraTime = 0;
        for (Tie tie : ties) {
            assertTrue(tie.isSingleLeg());
            assertTrue(tie.isFinished());
            Fixture match = tie.getFirstLeg();
            FixtureTeam flagged = Boolean.TRUE.equals(match.getTeams().getHome().getWinner())
                    ? match.getTeams().getHome() : match.getTeams().getAway();
            assertTrue(flagged.getWinner(), tie.toString());
            assertEquals(flagged.getId(), tie.getWinnerId(), tie.toString());
            if (tie.isDecidedOnPenalties()) penalties++;
            if (tie.isDecidedInExtraTime()) extraTime++;
        }
        assertEquals(4, penalties);
        assertEquals(5, extraTime);
    }

    @Test
    void worldCup_shootOutWithoutExtraTimeScoreIsStillDecidedOnPenalties() throws Exception {
        List<Tie> r32 = Ties.byRound(worldCup()).get("Round of 32");
        Tie germanyParaguay = null;
        for (Tie tie : r32) {
            if (tie.getFirstLeg().getFixture().getId() == 1565176) germanyParaguay = tie;
        }
        Fixture match = germanyParaguay.getFirstLeg();
        assertEquals("PEN", match.getFixture().getStatus().getShort());
        assertNull(match.getScore().getExtratime().getHome());     // the known data gap
        assertEquals(1, germanyParaguay.getAggregateA());
        assertEquals(1, germanyParaguay.getAggregateB());
        assertTrue(germanyParaguay.isDecidedOnPenalties());
        assertEquals(match.getTeams().getAway().getId(), germanyParaguay.getWinnerId());  // Paraguay 4-3
    }

    @Test
    void roundOf32_meansTwoLegsInTheChampionsLeagueButOneInTheWorldCup() throws Exception {
        List<Tie> cl = Ties.byRound(championsLeague()).get("Round of 32");
        List<Tie> wc = Ties.byRound(worldCup()).get("Round of 32");

        assertEquals(8, cl.size());
        assertEquals(2, Ties.legsPerTie(cl));
        assertEquals(16, wc.size());
        assertEquals(1, Ties.legsPerTie(wc));
    }

    @Test
    void worldCup_groupRoundsAreLeftOutOfEliminationTies() throws Exception {
        List<Fixture> fixtures = worldCup();
        assertEquals(104, Ties.of(fixtures).size());          // every pairing, group matches included
        assertEquals(32, Ties.ofEliminationRounds(fixtures).size());
    }

    // ------------------------------------------------------------------ states and edge cases

    /** Galatasaray - Juventus legs as JSON, so a test can edit their status and score. */
    private List<JsonObject> galatasarayJuventusLegs() throws Exception {
        JsonArray response = JsonParser.parseString(load("fixtures_by_league_ucl_2025.json"))
                .getAsJsonObject().getAsJsonArray("response");
        List<JsonObject> legs = new ArrayList<>();
        for (JsonElement element : response) {
            int id = element.getAsJsonObject().getAsJsonObject("fixture").get("id").getAsInt();
            if (id == 1515514 || id == 1515527) legs.add(element.getAsJsonObject().deepCopy());
        }
        assertEquals(2, legs.size());
        return legs;
    }

    private static void setStatus(JsonObject leg, String shortCode) {
        leg.getAsJsonObject("fixture").getAsJsonObject("status").addProperty("short", shortCode);
    }

    private static void setGoals(JsonObject leg, Integer home, Integer away) {
        JsonObject goals = leg.getAsJsonObject("goals");
        goals.add("home", home == null ? JsonNull.INSTANCE : new com.google.gson.JsonPrimitive(home));
        goals.add("away", away == null ? JsonNull.INSTANCE : new com.google.gson.JsonPrimitive(away));
    }

    private static void setWinner(JsonObject leg, Boolean home, Boolean away) {
        JsonObject teams = leg.getAsJsonObject("teams");
        teams.getAsJsonObject("home").add("winner", home == null ? JsonNull.INSTANCE : new com.google.gson.JsonPrimitive(home));
        teams.getAsJsonObject("away").add("winner", away == null ? JsonNull.INSTANCE : new com.google.gson.JsonPrimitive(away));
    }

    private static void setPenalties(JsonObject leg, Integer home, Integer away) {
        JsonObject penalty = leg.getAsJsonObject("score").getAsJsonObject("penalty");
        penalty.add("home", home == null ? JsonNull.INSTANCE : new com.google.gson.JsonPrimitive(home));
        penalty.add("away", away == null ? JsonNull.INSTANCE : new com.google.gson.JsonPrimitive(away));
    }

    /** Parses edited legs through the library, as the API would return them. */
    private List<Fixture> parse(List<JsonObject> legs) throws Exception {
        JsonObject envelope = new JsonObject();
        envelope.addProperty("get", "fixtures");
        envelope.add("parameters", new JsonObject());
        envelope.add("errors", new JsonArray());
        envelope.addProperty("results", legs.size());
        JsonArray response = new JsonArray();
        for (JsonObject leg : legs) response.add(leg);
        envelope.add("response", response);
        enqueueBody(envelope.toString(), 200);
        List<Fixture> fixtures = api.getFixturesByLeague(2, 2025);
        takeRequest();
        return fixtures;
    }

    private Tie edited(Consumer<List<JsonObject>> edit) throws Exception {
        List<JsonObject> legs = galatasarayJuventusLegs();
        edit.accept(legs);
        List<Tie> ties = Ties.of(parse(legs));
        assertEquals(1, ties.size());
        return ties.get(0);
    }

    @Test
    void state_notStarted() throws Exception {
        // edited: both legs NS, no goals
        Tie tie = edited(legs -> legs.forEach(leg -> {
            setStatus(leg, "NS");
            setGoals(leg, null, null);
        }));
        assertEquals(TieStates.NOT_STARTED, tie.getState());
        assertNull(tie.getAggregateA());
        assertNull(tie.getWinner());
        assertNull(tie.getLoser());
        assertFalse(tie.isDecidedOnPenalties());
        assertFalse(tie.isDecidedInExtraTime());
    }

    @Test
    void state_firstLegPlayed() throws Exception {
        // edited: second leg NS, no goals
        Tie tie = edited(legs -> {
            setStatus(legs.get(1), "NS");
            setGoals(legs.get(1), null, null);
        });
        assertEquals(TieStates.FIRST_LEG_PLAYED, tie.getState());
        assertEquals(5, tie.getAggregate(GALATASARAY));
        assertEquals(2, tie.getAggregate(JUVENTUS));
        assertNull(tie.getWinner());
    }

    @Test
    void state_inPlayCountsTheLiveScoreInTheAggregate() throws Exception {
        // edited: second leg in the second half, Juventus leading 1-0
        Tie tie = edited(legs -> {
            setStatus(legs.get(1), "2H");
            setGoals(legs.get(1), 1, 0);
        });
        assertEquals(TieStates.IN_PLAY, tie.getState());
        assertEquals(5, tie.getAggregate(GALATASARAY));
        assertEquals(3, tie.getAggregate(JUVENTUS));
        assertNull(tie.getWinner());
    }

    @Test
    void abandonedOrPostponedSecondLeg_noWinner() throws Exception {
        for (String status : new String[] {"ABD", "PST", "CANC", "SUSP"}) {
            // edited: second leg with this status and a partial score
            Tie tie = edited(legs -> {
                setStatus(legs.get(1), status);
                setGoals(legs.get(1), 1, 0);
            });
            assertNull(tie.getWinner(), status);
            assertFalse(tie.isFinished(), status);
            String expected = "SUSP".equals(status) ? TieStates.IN_PLAY : TieStates.FIRST_LEG_PLAYED;
            assertEquals(expected, tie.getState(), status);
            // an abandoned/postponed/cancelled leg is not counted; a suspended one is live and is
            assertEquals("SUSP".equals(status) ? 3 : 2, tie.getAggregate(JUVENTUS), status);
        }
    }

    @Test
    void awardedLeg_countsWithTheAwardedScore() throws Exception {
        // edited: second leg awarded 3-0 to Juventus
        Tie tie = edited(legs -> {
            setStatus(legs.get(1), "AWD");
            setGoals(legs.get(1), 3, 0);
        });
        assertEquals(TieStates.FINISHED, tie.getState());
        assertEquals(5, tie.getAggregate(JUVENTUS));
        assertEquals(5, tie.getAggregate(GALATASARAY));
        assertNull(tie.getWinner());    // level, no shoot-out, two legs: no away-goals rule
    }

    @Test
    void levelTwoLeggedTieWithoutShootOut_noWinnerEvenIfTheSecondLegHasAWinnerFlag() throws Exception {
        // edited: second leg 3-0 FT, Juventus flagged winner of the match
        Tie tie = edited(legs -> {
            setStatus(legs.get(1), "FT");
            setGoals(legs.get(1), 3, 0);
            setWinner(legs.get(1), true, false);
        });
        assertEquals(5, tie.getAggregateA());
        assertEquals(5, tie.getAggregateB());
        assertNull(tie.getWinner());
    }

    @Test
    void levelTwoLeggedTie_shootOutInTheSecondLegDecides() throws Exception {
        // edited: second leg 3-0 after extra time, Galatasaray (away) win the shoot-out 5-4
        Tie tie = edited(legs -> {
            setStatus(legs.get(1), "PEN");
            setGoals(legs.get(1), 3, 0);
            setPenalties(legs.get(1), 4, 5);
            setWinner(legs.get(1), false, true);
        });
        assertEquals(GALATASARAY, tie.getWinnerId());
        assertTrue(tie.isDecidedOnPenalties());
        assertEquals(5, tie.getPenaltyScore(GALATASARAY));
        assertEquals(4, tie.getPenaltyScore(JUVENTUS));
    }

    @Test
    void singleAwardedMatch_usesTheWinnerFlagWhenLevel() throws Exception {
        // edited: only the first leg, awarded, level score, Juventus flagged winner
        List<JsonObject> legs = galatasarayJuventusLegs();
        JsonObject only = legs.get(0);
        setStatus(only, "AWD");
        setGoals(only, 0, 0);
        setWinner(only, false, true);
        Tie tie = Ties.of(parse(Collections.singletonList(only))).get(0);
        assertTrue(tie.isSingleLeg());
        assertEquals(JUVENTUS, tie.getWinnerId());
        assertFalse(tie.isDecidedOnPenalties());
    }

    @Test
    void moreThanTwoFixturesForOnePairing_noWinner() throws Exception {
        // edited: a third copy of the second leg (as a replay filed as a new fixture would look)
        List<JsonObject> legs = galatasarayJuventusLegs();
        JsonObject replay = legs.get(1).deepCopy();
        replay.getAsJsonObject("fixture").addProperty("id", 1);
        replay.getAsJsonObject("fixture").addProperty("timestamp",
                legs.get(1).getAsJsonObject("fixture").get("timestamp").getAsLong() + 86400);
        legs.add(replay);
        Tie tie = Ties.of(parse(legs)).get(0);
        assertEquals(3, tie.getLegCount());
        assertEquals(TieStates.FINISHED, tie.getState());
        assertNull(tie.getWinner());
        assertEquals(1, tie.getLastLeg().getFixture().getId());
    }

    @Test
    void legsInDifferentlyNamedRounds_pairOnlyWhenIgnoringTheRound() throws Exception {
        // edited: the second leg filed under another round text
        List<JsonObject> legs = galatasarayJuventusLegs();
        legs.get(1).getAsJsonObject("league").addProperty("round", "Knockout Round Play-offs - 2nd Leg");
        List<Fixture> fixtures = parse(legs);
        assertEquals(2, Ties.of(fixtures).size());
        List<Tie> ignoring = Ties.ignoringRound(fixtures);
        assertEquals(1, ignoring.size());
        assertEquals(GALATASARAY, ignoring.get(0).getWinnerId());
        assertEquals("Round of 32", ignoring.get(0).getRound());
    }

    @Test
    void legsAreOrderedByKickOffWhateverTheInputOrder() throws Exception {
        List<JsonObject> legs = galatasarayJuventusLegs();
        Collections.reverse(legs);
        Tie tie = Ties.of(parse(legs)).get(0);
        assertEquals(1515514, tie.getFirstLeg().getFixture().getId());
        assertEquals(GALATASARAY, tie.getTeamA().getId());
    }

    @Test
    void missingData_neverThrows() {
        assertTrue(Ties.of(null).isEmpty());
        assertTrue(Ties.ofEliminationRounds(null).isEmpty());
        assertTrue(Ties.ignoringRound(Collections.singletonList(null)).isEmpty());
        assertTrue(Ties.byRound(null).isEmpty());
        assertNull(Ties.legsPerTie(null));
        assertNull(Ties.legsPerTie(Collections.emptyList()));
        assertNull(Ties.findByTeam(null, 1));
        Tie empty = Tie.of(null);
        assertEquals(0, empty.getLegCount());
        assertEquals(TieStates.NOT_STARTED, empty.getState());
        assertNull(empty.getFirstLeg());
        assertNull(empty.getLastLeg());
        assertNull(empty.getRound());
        assertNull(empty.getTeamA());
        assertNull(empty.getWinner());
        assertNull(empty.getAggregateA());
        assertFalse(empty.involves(1));
    }
}

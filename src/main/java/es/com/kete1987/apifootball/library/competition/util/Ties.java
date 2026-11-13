package es.com.kete1987.apifootball.library.competition.util;

import es.com.kete1987.apifootball.library.competition.model.Tie;
import es.com.kete1987.apifootball.library.fixtures.model.Fixture;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pairs fixtures into {@link Tie}s. API-Football has no leg number, no aggregate and no tie
 * winner: two fixtures are legs of one tie when they are in the same round ({@code league.round})
 * and between the same two teams (by team id, home/away ignored); their order is the kick-off
 * timestamp. Pure computation, no request.
 *
 * <p>Pass <b>every</b> fixture of the round (typically {@code FixturesApi.getFixturesByLeague}, one
 * request for the whole competition): a two-legged tie whose second leg is missing from the input
 * looks like a finished single match. Validated on real data: the 68 two-legged ties of the
 * Champions League 2025-26 (qualifying rounds, play-offs, round of 32 to semi-finals) give the
 * same aggregate and the same winner as Sportmonks' {@code aggregates}, and the 32 single-match
 * World Cup 2026 knockout ties give the same winner as the API's own {@code winner} flag.
 *
 * <pre>{@code
 * List<Fixture> all = fixtures.getFixturesByLeague(2, 2025);     // 1 request
 * for (Tie tie : Ties.ofEliminationRounds(all)) {
 *     System.out.println(tie.getRound() + " " + tie.getTeamA().getName() + " " + tie.getAggregateA()
 *             + "-" + tie.getAggregateB() + " " + tie.getTeamB().getName()
 *             + (tie.getWinner() == null ? "" : " -> " + tie.getWinner().getName())
 *             + (tie.isDecidedOnPenalties() ? " on penalties" : ""));
 * }
 * }</pre>
 */
public final class Ties {

    private Ties() {
        // helpers only
    }

    /**
     * Pairs fixtures by round and pair of teams. Works on one round or on a whole competition (league
     * or group rounds then give one single-match "tie" per fixture; see
     * {@link #ofEliminationRounds(Collection)} to leave them out). Ties are ordered by the kick-off of
     * their first leg. A fixture with a missing team id forms a tie of its own. {@code null} input or
     * elements are skipped. Never throws.
     */
    public static List<Tie> of(Collection<Fixture> fixtures) {
        return pair(fixtures, true);
    }

    /**
     * {@link #of(Collection)} restricted to fixtures whose round is an elimination round by name
     * ({@link StageTypes#ofRound(String)} is {@link StageTypes#KNOCKOUT} or
     * {@link StageTypes#QUALIFYING}): league, group and unrecognised rounds are left out.
     */
    public static List<Tie> ofEliminationRounds(Collection<Fixture> fixtures) {
        List<Fixture> elimination = new ArrayList<>();
        if (fixtures != null) {
            for (Fixture fixture : fixtures) {
                if (fixture != null && StageTypes.isElimination(StageTypes.ofRound(roundOf(fixture)))) {
                    elimination.add(fixture);
                }
            }
        }
        return pair(elimination, true);
    }

    /**
     * Pairs fixtures by pair of teams only, ignoring the round text: for the rare competition whose
     * two legs are filed under differently named rounds. Pass only the fixtures of one knockout
     * stage: across a whole competition, two teams meeting in the league phase and again in a
     * knockout round would be merged into one "tie" of three or more fixtures (which never gets a
     * winner). Never throws.
     */
    public static List<Tie> ignoringRound(Collection<Fixture> fixtures) {
        return pair(fixtures, false);
    }

    /**
     * {@link #of(Collection)} grouped by round text; rounds are ordered by the kick-off of their
     * earliest tie (for the official order, iterate {@code CompetitionStructure.getRounds()} and look
     * the round up here). Never throws.
     */
    public static Map<String, List<Tie>> byRound(Collection<Fixture> fixtures) {
        Map<String, List<Tie>> grouped = new LinkedHashMap<>();
        for (Tie tie : of(fixtures)) {
            grouped.computeIfAbsent(tie.getRound(), k -> new ArrayList<>()).add(tie);
        }
        Map<String, List<Tie>> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<Tie>> entry : grouped.entrySet()) {
            result.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * The number of legs shared by every tie (1 or 2 in real competitions), or {@code null} when the
     * collection is empty or the ties differ. A fact to label a round with: the Champions League
     * 2025-26 {@code "Round of 32"} answers 2, the World Cup 2026 {@code "Round of 32"} answers 1.
     */
    public static Integer legsPerTie(Collection<Tie> ties) {
        Integer legs = null;
        if (ties == null) return null;
        for (Tie tie : ties) {
            if (tie == null) continue;
            if (legs == null) legs = tie.getLegCount();
            else if (legs != tie.getLegCount()) return null;
        }
        return legs;
    }

    /** The first tie involving {@code teamId}, or {@code null}. */
    public static Tie findByTeam(Collection<Tie> ties, int teamId) {
        if (ties == null) return null;
        for (Tie tie : ties) {
            if (tie != null && tie.involves(teamId)) return tie;
        }
        return null;
    }

    private static List<Tie> pair(Collection<Fixture> fixtures, boolean byRound) {
        Map<List<Object>, List<Fixture>> groups = new LinkedHashMap<>();
        if (fixtures != null) {
            for (Fixture fixture : fixtures) {
                if (fixture == null) continue;
                groups.computeIfAbsent(key(fixture, byRound), k -> new ArrayList<>()).add(fixture);
            }
        }
        List<Tie> ties = new ArrayList<>(groups.size());
        for (List<Fixture> legs : groups.values()) {
            ties.add(Tie.of(legs));
        }
        ties.sort(Comparator.comparing(Ties::firstKickOff, Comparator.nullsLast(Comparator.naturalOrder())));
        return Collections.unmodifiableList(ties);
    }

    private static List<Object> key(Fixture fixture, boolean byRound) {
        Integer home = null;
        Integer away = null;
        if (fixture.getTeams() != null) {
            if (fixture.getTeams().getHome() != null) home = fixture.getTeams().getHome().getId();
            if (fixture.getTeams().getAway() != null) away = fixture.getTeams().getAway().getId();
        }
        List<Object> key = new ArrayList<>(3);
        key.add(byRound ? roundOf(fixture) : null);
        if (home == null || away == null) {
            key.add("fixture");
            key.add(fixture); // identity: a fixture without both team ids is never paired
        } else {
            key.add(Math.min(home, away));
            key.add(Math.max(home, away));
        }
        return key;
    }

    private static Long firstKickOff(Tie tie) {
        Fixture first = tie.getFirstLeg();
        return first == null || first.getFixture() == null ? null : first.getFixture().getTimestamp();
    }

    private static String roundOf(Fixture fixture) {
        return fixture.getLeague() == null ? null : fixture.getLeague().getRound();
    }
}

package es.com.kete1987.apifootball.library.competition.model;

import es.com.kete1987.apifootball.library.common.model.TeamRef;
import es.com.kete1987.apifootball.library.competition.util.TieStates;
import es.com.kete1987.apifootball.library.fixtures.model.Fixture;
import es.com.kete1987.apifootball.library.fixtures.model.FixtureTeam;
import es.com.kete1987.apifootball.library.fixtures.model.Goals;
import es.com.kete1987.apifootball.library.fixtures.util.FixtureStatuses;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A knockout tie between two teams: one match (a final, a World Cup knockout match) or two legs
 * (most UEFA rounds), rebuilt from fixtures because API-Football has no leg, aggregate or tie
 * winner field. Build ties with {@code competition.util.Ties}, or with {@link #of(Collection)} when
 * you already know which fixtures form the tie. Pure computation, no request.
 *
 * <p><b>Teams.</b> Team A is the home side of the first leg, team B the away side. For a single
 * match at a neutral venue (finals, the whole World Cup) "home" is only the API's nominal side.
 *
 * <p><b>Aggregate.</b> The sum of {@code goals} over the legs that are played or live (see
 * {@link TieStates}): {@code goals} includes extra time and excludes the shoot-out. Never
 * {@code score.fulltime} (90 minutes only) and never {@code score.extratime}, which the API leaves
 * {@code null} in some matches that did go to extra time (3 of the 4 World Cup 2026 shoot-outs).
 *
 * <p><b>Winner.</b> Decided only when the state is {@link TieStates#FINISHED} and the tie has one or
 * two legs: the team with the higher aggregate; on a level aggregate, the winner of the last leg's
 * {@code score.penalty} shoot-out; for a single match still level with no shoot-out score, the
 * match's {@code winner} flag (the API sets it for awarded matches too). Otherwise {@code null}:
 * there is no away-goals rule (UEFA dropped it in 2021; a level two-legged tie without a shoot-out
 * is left undecided rather than guessed). Never use {@link FixtureTeam#getWinner()} of a second
 * leg as the tie winner: it is per match. In the Champions League 2025-26 play-off Juventus won the
 * second leg 3-2 after extra time ({@code winner: true}) but Galatasaray went through 7-5 on
 * aggregate.
 *
 * <p><b>Edge cases.</b> An abandoned, cancelled or postponed leg is neither played nor live, so the
 * tie is not finished and has no winner until the API updates that fixture (it normally keeps the
 * fixture id and changes its status and score when the match is replayed or awarded). An awarded
 * leg ({@code AWD}, {@code WO}) counts with the score the API gives it. If one pairing has more than
 * two fixtures (a replay filed as a new fixture next to the abandoned one, or the same teams meeting
 * twice in a league round), every fixture stays in {@link #getLegs()} but no winner is named.
 */
public final class Tie {

    private final List<Fixture> legs;
    private final TeamRef teamA;
    private final TeamRef teamB;
    private final String state;
    private final TeamRef winner;
    private final boolean decidedOnPenalties;

    private Tie(List<Fixture> legs) {
        this.legs = Collections.unmodifiableList(legs);
        TeamRef a = null;
        TeamRef b = null;
        for (Fixture leg : legs) {
            if (leg.getTeams() != null && leg.getTeams().getHome() != null && leg.getTeams().getAway() != null) {
                a = leg.getTeams().getHome();
                b = leg.getTeams().getAway();
                break;
            }
        }
        this.teamA = a;
        this.teamB = b;
        this.state = computeState();
        TeamRef w = null;
        boolean penalties = false;
        if (TieStates.FINISHED.equals(state) && !legs.isEmpty() && legs.size() <= 2
                && idOf(teamA) != null && idOf(teamB) != null) {
            Integer aggA = getAggregate(idOf(teamA));
            Integer aggB = getAggregate(idOf(teamB));
            if (aggA != null && aggB != null && !aggA.equals(aggB)) {
                w = aggA > aggB ? teamA : teamB;
            } else if (aggA != null && aggB != null) {
                Fixture last = getLastLeg();
                Integer penA = penaltiesOf(last, idOf(teamA));
                Integer penB = penaltiesOf(last, idOf(teamB));
                if (penA != null && penB != null && !penA.equals(penB)) {
                    w = penA > penB ? teamA : teamB;
                    penalties = true;
                } else if (legs.size() == 1) {
                    w = flaggedWinner(last);
                }
            }
        }
        this.winner = w;
        this.decidedOnPenalties = penalties;
    }

    /**
     * Builds one tie from the fixtures that form it (the legs of one pairing, in any order; they are
     * sorted by kick-off timestamp, fixtures without one last). {@code null} elements are skipped and a
     * {@code null} collection gives a tie without legs. The caller is responsible for passing fixtures
     * between the same two teams; use {@code Ties} to pair a whole round or competition.
     */
    public static Tie of(Collection<Fixture> fixtures) {
        List<Fixture> legs = new ArrayList<>();
        if (fixtures != null) {
            for (Fixture fixture : fixtures) {
                if (fixture != null) legs.add(fixture);
            }
        }
        legs.sort(CompetitionStructure.BY_KICK_OFF);
        return new Tie(legs);
    }

    /** Round text of the first leg ({@code league.round}), or {@code null}. */
    public String getRound() {
        Fixture first = getFirstLeg();
        return first == null || first.getLeague() == null ? null : first.getLeague().getRound();
    }

    /** The fixtures of the tie in kick-off order: one for a single match, two for a two-legged tie. */
    public List<Fixture> getLegs() {
        return legs;
    }

    /** Number of fixtures in the tie. */
    public int getLegCount() {
        return legs.size();
    }

    /** {@code true} for a single-match tie (finals, World Cup knockouts). */
    public boolean isSingleLeg() {
        return legs.size() == 1;
    }

    /** {@code true} for a tie of exactly two legs. */
    public boolean isTwoLegged() {
        return legs.size() == 2;
    }

    /** The first fixture by kick-off, or {@code null} when the tie has no legs. */
    public Fixture getFirstLeg() {
        return legs.isEmpty() ? null : legs.get(0);
    }

    /** The second fixture by kick-off, or {@code null} for a single match. */
    public Fixture getSecondLeg() {
        return legs.size() < 2 ? null : legs.get(1);
    }

    /** The last fixture by kick-off (the second leg, or the only match), or {@code null} when there are no legs. */
    public Fixture getLastLeg() {
        return legs.isEmpty() ? null : legs.get(legs.size() - 1);
    }

    /** Home side of the first leg, or {@code null} when no leg has teams. */
    public TeamRef getTeamA() {
        return teamA;
    }

    /** Away side of the first leg, or {@code null} when no leg has teams. */
    public TeamRef getTeamB() {
        return teamB;
    }

    /** {@code true} when {@code teamId} is team A or team B. */
    public boolean involves(int teamId) {
        return isTeam(teamA, teamId) || isTeam(teamB, teamId);
    }

    /** The other team of the tie, or {@code null} when {@code teamId} is not in it. */
    public TeamRef getOpponent(int teamId) {
        if (isTeam(teamA, teamId)) return teamB;
        if (isTeam(teamB, teamId)) return teamA;
        return null;
    }

    /**
     * Goals of {@code teamId} summed over the played and live legs ({@code goals}, extra time
     * included, shoot-out excluded), so it is the live aggregate while a leg is in play; {@code null}
     * when the team is not in the tie or no leg has started.
     */
    public Integer getAggregate(int teamId) {
        if (!involves(teamId)) return null;
        Integer total = null;
        for (Fixture leg : legs) {
            if (!isPlayed(leg) && !isLive(leg)) continue;
            Integer goals = goalsOf(leg, teamId);
            if (goals == null) continue;
            total = (total == null ? 0 : total) + goals;
        }
        return total;
    }

    /** {@link #getAggregate(int)} of team A, or {@code null}. */
    public Integer getAggregateA() {
        Integer id = idOf(teamA);
        return id == null ? null : getAggregate(id);
    }

    /** {@link #getAggregate(int)} of team B, or {@code null}. */
    public Integer getAggregateB() {
        Integer id = idOf(teamB);
        return id == null ? null : getAggregate(id);
    }

    /** One of the {@link TieStates} constants (see there for what counts as played or live). */
    public String getState() {
        return state;
    }

    /** {@code true} when the state is {@link TieStates#FINISHED}. */
    public boolean isFinished() {
        return TieStates.FINISHED.equals(state);
    }

    /** The team that goes through, or {@code null} while undecided (rules in the class Javadoc). */
    public TeamRef getWinner() {
        return winner;
    }

    /** Id of {@link #getWinner()}, or {@code null}. */
    public Integer getWinnerId() {
        return idOf(winner);
    }

    /** The team knocked out, or {@code null} while there is no winner. */
    public TeamRef getLoser() {
        if (winner == null) return null;
        return winner == teamA ? teamB : teamA;
    }

    /** {@code true} when the winner was decided by the last leg's penalty shoot-out. */
    public boolean isDecidedOnPenalties() {
        return decidedOnPenalties;
    }

    /**
     * {@code true} when the tie is finished, has a winner and its last leg ended {@code AET}: decided
     * in extra time without a shoot-out (Sportmonks' "After Extra Time"). Decided on the status code,
     * not on {@code score.extratime}.
     */
    public boolean isDecidedInExtraTime() {
        Fixture last = getLastLeg();
        return winner != null && !decidedOnPenalties && FixtureStatuses.AET.equals(statusOf(last));
    }

    /**
     * Shoot-out goals of {@code teamId} in the last leg ({@code score.penalty}), or {@code null} when
     * there was no shoot-out (yet) or the team is not in the tie.
     */
    public Integer getPenaltyScore(int teamId) {
        if (!involves(teamId)) return null;
        return penaltiesOf(getLastLeg(), teamId);
    }

    @Override
    public String toString() {
        return getRound() + ": " + nameOf(teamA) + " " + getAggregateA() + "-" + getAggregateB() + " "
                + nameOf(teamB) + " (" + state + ")";
    }

    // ------------------------------------------------------------------ internals

    private String computeState() {
        if (legs.isEmpty()) return TieStates.NOT_STARTED;
        int played = 0;
        for (Fixture leg : legs) {
            if (isLive(leg)) return TieStates.IN_PLAY;
            if (isPlayed(leg)) played++;
        }
        if (played == legs.size()) return TieStates.FINISHED;
        return played == 0 ? TieStates.NOT_STARTED : TieStates.FIRST_LEG_PLAYED;
    }

    private static boolean isPlayed(Fixture leg) {
        String status = statusOf(leg);
        Goals goals = leg.getGoals();
        return (FixtureStatuses.isFinished(status) || FixtureStatuses.isNotPlayed(status))
                && goals != null && goals.getHome() != null && goals.getAway() != null;
    }

    private static boolean isLive(Fixture leg) {
        return FixtureStatuses.isLive(statusOf(leg));
    }

    private static String statusOf(Fixture leg) {
        if (leg == null || leg.getFixture() == null || leg.getFixture().getStatus() == null) return null;
        return leg.getFixture().getStatus().getShort();
    }

    private static Integer goalsOf(Fixture leg, int teamId) {
        return sideValue(leg, leg.getGoals(), teamId);
    }

    private static Integer penaltiesOf(Fixture leg, int teamId) {
        if (leg == null || leg.getScore() == null) return null;
        return sideValue(leg, leg.getScore().getPenalty(), teamId);
    }

    private static Integer sideValue(Fixture leg, Goals goals, int teamId) {
        if (goals == null || leg.getTeams() == null) return null;
        if (isTeam(leg.getTeams().getHome(), teamId)) return goals.getHome();
        if (isTeam(leg.getTeams().getAway(), teamId)) return goals.getAway();
        return null;
    }

    private TeamRef flaggedWinner(Fixture leg) {
        if (leg == null || leg.getTeams() == null) return null;
        FixtureTeam home = leg.getTeams().getHome();
        FixtureTeam away = leg.getTeams().getAway();
        if (home != null && Boolean.TRUE.equals(home.getWinner())) return sameAs(home);
        if (away != null && Boolean.TRUE.equals(away.getWinner())) return sameAs(away);
        return null;
    }

    private TeamRef sameAs(TeamRef team) {
        Integer id = idOf(team);
        if (id == null) return null;
        if (id.equals(idOf(teamA))) return teamA;
        if (id.equals(idOf(teamB))) return teamB;
        return null;
    }

    private static boolean isTeam(TeamRef team, int teamId) {
        return team != null && team.getId() != null && team.getId() == teamId;
    }

    private static Integer idOf(TeamRef team) {
        return team == null ? null : team.getId();
    }

    private static String nameOf(TeamRef team) {
        return team == null ? null : team.getName();
    }
}

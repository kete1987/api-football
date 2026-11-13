package es.com.kete1987.apifootball.library.fixtures.model;

import es.com.kete1987.apifootball.library.common.util.ModelCollections;
import es.com.kete1987.apifootball.library.fixtures.util.FixtureEventAnalysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A fixture (match). One model for every endpoint returning fixtures:
 * <ul>
 *   <li>{@code /fixtures} by {@code id} or {@code ids}: {@link #getEvents()}, {@link #getLineups()},
 *       {@link #getStatistics()} and {@link #getPlayers()} are populated;</li>
 *   <li>{@code /fixtures?live=...}: only {@link #getEvents()} is populated;</li>
 *   <li>any other query, head to head and prediction history: all four are {@code null}.</li>
 * </ul>
 * The embedded members reuse the classes returned by the dedicated endpoints.
 */
public class Fixture {
    private FixtureInfo fixture;
    private FixtureLeague league;
    private Teams teams;
    private Goals goals;
    private Score score;
    private List<FixtureEvent> events;
    private List<FixtureLineup> lineups;
    private List<FixtureStatistics> statistics;
    private List<FixturePlayers> players;

    public FixtureInfo getFixture() {
        return fixture;
    }

    public FixtureLeague getLeague() {
        return league;
    }

    public Teams getTeams() {
        return teams;
    }

    /**
     * Current or final goals of the match, as counted by the API, including extra time but not a
     * penalty shoot-out (see {@link Score#getPenalty()}); members are {@code null} before the start.
     * This is the authoritative score: prefer it to counting events. On finished fixtures it equals
     * the number of {@link FixtureEvent#isGoal()} events per
     * {@link FixtureEvent#getTeamGoalCountsFor()} (verified on a {@code PEN} fixture, whose shoot-out
     * kicks are not events, and on a fixture with an own goal).
     */
    public Goals getGoals() {
        return goals;
    }

    public Score getScore() {
        return score;
    }

    /**
     * Events in the order the API sent them; present with {@code id}, {@code ids} and {@code live},
     * otherwise {@code null}. That order is <b>not chronological</b> (a card at {@code 90+6} has been
     * listed before a substitution at {@code 90}): use {@link #getEventsSorted()} for a timeline.
     */
    public List<FixtureEvent> getEvents() {
        return ModelCollections.unmodifiable(events);
    }

    /**
     * A new unmodifiable list with the same events sorted chronologically by
     * {@link FixtureEvent#BY_TIME}, or {@code null} when {@link #getEvents()} is {@code null} (same
     * convention as the other list getters). {@link #getEvents()} itself is not affected.
     *
     * <p>The API's order is not chronological, hence this helper. The sort is stable: events at the
     * same minute (and extra) keep the order the API sent them in, which matters because a second
     * yellow card arrives as a {@code "Yellow Card"} event followed by a {@code "Red Card"} event at
     * the same minute. Events without a minute go last.
     */
    public List<FixtureEvent> getEventsSorted() {
        if (events == null) {
            return null;
        }
        List<FixtureEvent> sorted = new ArrayList<>(events);
        sorted.sort(FixtureEvent.BY_TIME);
        return Collections.unmodifiableList(sorted);
    }

    /**
     * {@code true} when {@code event} is the red card that follows a second yellow: this fixture's
     * events hold a yellow card of the same player at the same minute. The API has no second-yellow
     * event, it sends "Yellow Card" then "Red Card" (recorded: Ruibal, 118', Navalcarnero-Betis). A
     * straight red, any other event, a {@code null} event and a fixture without events all give
     * {@code false}. Matching rules and limits: {@link FixtureEventAnalysis}.
     */
    public boolean isSecondYellowRed(FixtureEvent event) {
        return FixtureEventAnalysis.isSecondYellowRed(event, events);
    }

    /**
     * The cards of the fixture in chronological order ({@link #getEventsSorted()}) with each second
     * yellow collapsed into one entry: the yellow card that belongs to it is dropped and the red card
     * stays; tell it from a straight red with {@link #isSecondYellowRed(FixtureEvent)}. An earlier,
     * separate yellow of the same player stays. Empty when there are no events. See
     * {@link FixtureEventAnalysis#cardEventsMerged(List)}.
     */
    public List<FixtureEvent> getCardEventsMerged() {
        return FixtureEventAnalysis.cardEventsMerged(getEventsSorted());
    }

    /**
     * The score after each goal, in chronological order: one {@link ScoreStep} per goal event
     * ({@link FixtureEvent#isGoal()}) of {@link #getEventsSorted()}, so missed penalties (and every
     * non-goal event) are left out. A goal is added to the team in
     * {@link FixtureEvent#getTeamGoalCountsFor()}, which is the benefiting team also for an own goal
     * (the API lists an own goal under the team it counts for), so own goals need no special case. The
     * home team is the one whose id equals {@code teams.home.id}; a goal whose team is missing or is
     * neither side is skipped (it cannot be attributed). Shoot-out kicks are not events and not counted.
     *
     * <p>Events at the same minute keep the API's order, so two goals in the same minute are ordered as
     * sent. The API's own {@link #getGoals()} stays authoritative: on finished fixtures the last step
     * equals it (verified on 1-1, 0-1 own goal and 1-1 {@code PEN} fixtures), but during a live match
     * events can appear and vanish. Returns an empty list when there are no events or no goal;
     * {@code null} is never returned.
     */
    public List<ScoreStep> getRunningScore() {
        List<ScoreStep> steps = new ArrayList<>();
        List<FixtureEvent> sorted = getEventsSorted();
        Integer homeId = homeId();
        Integer awayId = awayId();
        int home = 0;
        int away = 0;
        if (sorted != null && homeId != null && awayId != null) {
            for (FixtureEvent e : sorted) {
                if (e.getTeamGoalCountsFor() == null || e.getTeamGoalCountsFor().getId() == null) continue;
                Integer id = e.getTeamGoalCountsFor().getId();
                if (id.equals(homeId)) {
                    home++;
                } else if (id.equals(awayId)) {
                    away++;
                } else {
                    continue;
                }
                steps.add(new ScoreStep(e, home, away));
            }
        }
        return Collections.unmodifiableList(steps);
    }

    /** The score after one goal: see {@link Fixture#getRunningScore()}. */
    public static class ScoreStep {
        private final FixtureEvent event;
        private final int home;
        private final int away;

        ScoreStep(FixtureEvent event, int home, int away) {
            this.event = event;
            this.home = home;
            this.away = away;
        }

        /** The goal event this step comes from. */
        public FixtureEvent getEvent() {
            return event;
        }

        /** Home goals after this goal. */
        public int getHome() {
            return home;
        }

        /** Away goals after this goal. */
        public int getAway() {
            return away;
        }

        /** The score as {@code "home-away"}, e.g. {@code "2-1"}. */
        public String getText() {
            return home + "-" + away;
        }
    }

    // ------------------------------------------------------------ lookups inside the fixture

    /**
     * Team statistics block of a team, or {@code null} when absent (no {@code id}/{@code ids} query,
     * statistics not yet available, {@code teamId} {@code null} or not in the fixture). Never throws.
     */
    public FixtureStatistics findStatistics(Integer teamId) {
        if (statistics == null || teamId == null) return null;
        for (FixtureStatistics s : statistics) {
            if (s != null && s.getTeam() != null && teamId.equals(s.getTeam().getId())) return s;
        }
        return null;
    }

    /** Statistics of the home team ({@code teams.home.id}), or {@code null}. See {@link #findStatistics(Integer)}. */
    public FixtureStatistics getHomeStatistics() {
        return findStatistics(homeId());
    }

    /** Statistics of the away team ({@code teams.away.id}), or {@code null}. See {@link #findStatistics(Integer)}. */
    public FixtureStatistics getAwayStatistics() {
        return findStatistics(awayId());
    }

    /**
     * Lineup of a team, or {@code null} when absent (lineups are published 20-40 minutes before
     * kick-off, and only with {@code id}/{@code ids}). Never throws.
     */
    public FixtureLineup findLineup(Integer teamId) {
        if (lineups == null || teamId == null) return null;
        for (FixtureLineup l : lineups) {
            if (l != null && l.getTeam() != null && teamId.equals(l.getTeam().getId())) return l;
        }
        return null;
    }

    /** Lineup of the home team, or {@code null}. See {@link #findLineup(Integer)}. */
    public FixtureLineup getHomeLineup() {
        return findLineup(homeId());
    }

    /** Lineup of the away team, or {@code null}. See {@link #findLineup(Integer)}. */
    public FixtureLineup getAwayLineup() {
        return findLineup(awayId());
    }

    /** Per-player statistics block of a team, or {@code null} when absent. Never throws. */
    public FixturePlayers findPlayers(Integer teamId) {
        if (players == null || teamId == null) return null;
        for (FixturePlayers p : players) {
            if (p != null && p.getTeam() != null && teamId.equals(p.getTeam().getId())) return p;
        }
        return null;
    }

    /** Per-player statistics of the home team, or {@code null}. See {@link #findPlayers(Integer)}. */
    public FixturePlayers getHomePlayers() {
        return findPlayers(homeId());
    }

    /** Per-player statistics of the away team, or {@code null}. See {@link #findPlayers(Integer)}. */
    public FixturePlayers getAwayPlayers() {
        return findPlayers(awayId());
    }

    /**
     * Name of a player by id, looked up in the lineups (starting XI, then substitutes) and then in the
     * per-player statistics; {@code null} when the id is {@code null}, unknown or its name is missing.
     * Events carry the player id and a name that the API abbreviates and spells differently from one
     * event to the next; the lineups give a stable one. Never throws.
     */
    public String findPlayerName(Integer playerId) {
        if (playerId == null) return null;
        if (lineups != null) {
            for (FixtureLineup l : lineups) {
                if (l == null) continue;
                String name = nameIn(l.getStartXI(), playerId);
                if (name == null) name = nameIn(l.getSubstitutes(), playerId);
                if (name != null) return name;
            }
        }
        if (players != null) {
            for (FixturePlayers fp : players) {
                if (fp == null) continue;
                for (FixturePlayers.PlayerEntry e : fp.getPlayers()) {
                    FixturePlayers.PlayerInfo p = e == null ? null : e.getPlayer();
                    if (p != null && playerId.equals(p.getId()) && p.getName() != null) return p.getName();
                }
            }
        }
        return null;
    }

    private static String nameIn(List<FixtureLineup.Entry> entries, Integer playerId) {
        if (entries == null) return null;
        for (FixtureLineup.Entry e : entries) {
            FixtureLineup.LineupPlayer p = e == null ? null : e.getPlayer();
            if (p != null && playerId.equals(p.getId()) && p.getName() != null) return p.getName();
        }
        return null;
    }

    private Integer homeId() {
        return teams == null || teams.home == null ? null : teams.home.getId();
    }

    private Integer awayId() {
        return teams == null || teams.away == null ? null : teams.away.getId();
    }

    /** Lineups; present with {@code id} and {@code ids}, otherwise {@code null}. */
    public List<FixtureLineup> getLineups() {
        return ModelCollections.unmodifiable(lineups);
    }

    /** Team statistics (no half split); present with {@code id} and {@code ids}, otherwise {@code null}. */
    public List<FixtureStatistics> getStatistics() {
        return ModelCollections.unmodifiable(statistics);
    }

    /** Player statistics; present with {@code id} and {@code ids}, otherwise {@code null}. */
    public List<FixturePlayers> getPlayers() {
        return ModelCollections.unmodifiable(players);
    }

    /** Home and away sides. */
    public static class Teams {
        private FixtureTeam home;
        private FixtureTeam away;

        public FixtureTeam getHome() {
            return home;
        }

        public FixtureTeam getAway() {
            return away;
        }
    }

    /**
     * Score by phase; each phase has {@code null} members when it was not played. In real responses
     * {@code extratime} holds only the goals scored in extra time (e.g. full time 2-2, extra time 0-1,
     * {@code goals} 2-3; {@code 0-0}, not {@code null}, when extra time was played without goals), and
     * {@code penalty} the shoot-out result, which {@code goals} excludes. The shoot-out kicks
     * themselves are not in the events, so {@code penalty} is the only place the shoot-out appears.
     */
    public static class Score {
        private Goals halftime;
        private Goals fulltime;
        private Goals extratime;
        private Goals penalty;

        public Goals getHalftime() {
            return halftime;
        }

        public Goals getFulltime() {
            return fulltime;
        }

        public Goals getExtratime() {
            return extratime;
        }

        public Goals getPenalty() {
            return penalty;
        }
    }
}

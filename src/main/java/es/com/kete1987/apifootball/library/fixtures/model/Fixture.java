package es.com.kete1987.apifootball.library.fixtures.model;

import es.com.kete1987.apifootball.library.common.util.ModelCollections;

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

package es.com.kete1987.apifootball.library.fixtures.model;

import es.com.kete1987.apifootball.library.common.util.ModelCollections;

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
     * This is the authoritative score: prefer it to counting events.
     */
    public Goals getGoals() {
        return goals;
    }

    public Score getScore() {
        return score;
    }

    /** Events; present with {@code id}, {@code ids} and {@code live}, otherwise {@code null}. */
    public List<FixtureEvent> getEvents() {
        return ModelCollections.unmodifiable(events);
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
     * {@code goals} 2-3), and {@code penalty} the shoot-out result, which {@code goals} excludes.
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

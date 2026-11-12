package es.com.kete1987.apifootball.library.odds.model;

import com.google.gson.annotations.SerializedName;
import es.com.kete1987.apifootball.library.common.util.ModelCollections;

import java.util.List;

/** In-play odds of one fixture ({@code /odds/live}). Has no history: it reflects the current moment. */
public class LiveOdds {
    private LiveFixture fixture;
    private LeagueRef league;
    private Teams teams;
    private Flags status;
    private String update;
    private List<LiveBetOdds> odds;

    public LiveFixture getFixture() {
        return fixture;
    }

    public LeagueRef getLeague() {
        return league;
    }

    public Teams getTeams() {
        return teams;
    }

    /** Market state flags of the fixture (stopped / blocked / finished). */
    public Flags getStatus() {
        return status;
    }

    /** ISO-8601 time of the last update (+00:00). */
    public String getUpdate() {
        return update;
    }

    public List<LiveBetOdds> getOdds() {
        return ModelCollections.unmodifiable(odds);
    }

    /** The in-play bet with the given <b>in-play</b> id, or {@code null}. */
    public LiveBetOdds findBet(int liveBetId) {
        if (odds == null) return null;
        for (LiveBetOdds bet : odds) {
            if (bet.getId() != null && bet.getId() == liveBetId) return bet;
        }
        return null;
    }

    /** Fixture id and match clock. */
    public static class LiveFixture {
        private Integer id;
        private Clock status;

        public Integer getId() {
            return id;
        }

        public Clock getStatus() {
            return status;
        }
    }

    /** Match clock; unlike {@code /fixtures} it has no {@code short} code. */
    public static class Clock {
        @SerializedName("long")
        private String longName;
        private Integer elapsed;
        private String seconds;

        /** Textual status, e.g. {@code "First Half"}. */
        public String getLong() {
            return longName;
        }

        public Integer getElapsed() {
            return elapsed;
        }

        /** Match clock as {@code "mm:ss"}, e.g. {@code "14:19"}. */
        public String getSeconds() {
            return seconds;
        }
    }

    /** League id and season. */
    public static class LeagueRef {
        private Integer id;
        private Integer season;

        public Integer getId() {
            return id;
        }

        public Integer getSeason() {
            return season;
        }
    }

    /** Home and away team with their current goals. */
    public static class Teams {
        private TeamGoals home;
        private TeamGoals away;

        public TeamGoals getHome() {
            return home;
        }

        public TeamGoals getAway() {
            return away;
        }
    }

    /** Team id and current goals. */
    public static class TeamGoals {
        private Integer id;
        private Integer goals;

        public Integer getId() {
            return id;
        }

        public Integer getGoals() {
            return goals;
        }
    }

    /** Market state flags. */
    public static class Flags {
        private Boolean stopped;
        private Boolean blocked;
        private Boolean finished;

        public Boolean getStopped() {
            return stopped;
        }

        public Boolean getBlocked() {
            return blocked;
        }

        public Boolean getFinished() {
            return finished;
        }
    }
}

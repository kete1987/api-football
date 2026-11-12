package es.com.kete1987.apifootball.library.fixtures.model;

import com.google.gson.annotations.SerializedName;
import es.com.kete1987.apifootball.library.common.util.ModelCollections;

import java.util.List;

/**
 * Prediction for a fixture (Poisson, team statistics, head to head; bookmaker odds are not used)
 * with comparison data and previous meetings. Percentages are text such as {@code "50%"}.
 */
public class Prediction {
    private Predictions predictions;
    private FixtureLeague league;
    private PredictionTeams teams;
    private Comparison comparison;
    private List<Fixture> h2h;

    public Predictions getPredictions() {
        return predictions;
    }

    public FixtureLeague getLeague() {
        return league;
    }

    public PredictionTeams getTeams() {
        return teams;
    }

    public Comparison getComparison() {
        return comparison;
    }

    /** Previous meetings between the two teams (plain fixtures, no embedded details). */
    public List<Fixture> getH2h() {
        return ModelCollections.unmodifiable(h2h);
    }

    /** The prediction itself. */
    public static class Predictions {
        private Winner winner;
        @SerializedName("win_or_draw")
        private Boolean winOrDraw;
        @SerializedName("under_over")
        private String underOver;
        private GoalsPrediction goals;
        private String advice;
        private Percent percent;

        public Winner getWinner() {
            return winner;
        }

        public Boolean getWinOrDraw() {
            return winOrDraw;
        }

        /** E.g. {@code "-3.5"}, or {@code null}. */
        public String getUnderOver() {
            return underOver;
        }

        public GoalsPrediction getGoals() {
            return goals;
        }

        /** E.g. {@code "Combo Winner : Real Madrid and -3.5 goals"}. */
        public String getAdvice() {
            return advice;
        }

        public Percent getPercent() {
            return percent;
        }
    }

    /** Predicted winner; every member may be {@code null}. */
    public static class Winner {
        private Integer id;
        private String name;
        private String comment;

        public Integer getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        /** E.g. {@code "Win or draw"}. */
        public String getComment() {
            return comment;
        }
    }

    /** Goal lines such as {@code "-3.5"}, {@code "+2.5"}. */
    public static class GoalsPrediction {
        private String home;
        private String away;

        public String getHome() {
            return home;
        }

        public String getAway() {
            return away;
        }
    }

    /** Win probabilities as text such as {@code "50%"}. */
    public static class Percent {
        private String home;
        private String draw;
        private String away;

        public String getHome() {
            return home;
        }

        public String getDraw() {
            return draw;
        }

        public String getAway() {
            return away;
        }
    }

    /** Both teams of the prediction. */
    public static class PredictionTeams {
        private PredictionTeam home;
        private PredictionTeam away;

        public PredictionTeam getHome() {
            return home;
        }

        public PredictionTeam getAway() {
            return away;
        }
    }

    /** A team with its recent form and league statistics. */
    public static class PredictionTeam extends TeamRef {
        @SerializedName("last_5")
        private Last5 last5;
        private PredictionTeamLeague league;

        public Last5 getLast5() {
            return last5;
        }

        public PredictionTeamLeague getLeague() {
            return league;
        }
    }

    /** Statistics of the last five matches. */
    public static class Last5 {
        private Integer played;
        private String form;
        private String att;
        private String def;
        private Last5Goals goals;

        public Integer getPlayed() {
            return played;
        }

        /** Form as a percentage text such as {@code "100%"}. */
        public String getForm() {
            return form;
        }

        public String getAtt() {
            return att;
        }

        public String getDef() {
            return def;
        }

        public Last5Goals getGoals() {
            return goals;
        }
    }

    public static class Last5Goals {
        @SerializedName("for")
        private TotalAverage scored;
        private TotalAverage against;

        /** Goals scored (the API key is {@code for}). */
        public TotalAverage getFor() {
            return scored;
        }

        public TotalAverage getAgainst() {
            return against;
        }
    }

    public static class TotalAverage {
        private Integer total;
        private String average;

        public Integer getTotal() {
            return total;
        }

        /** Average as text, e.g. {@code "2.6"}. */
        public String getAverage() {
            return average;
        }
    }

    /** Head-to-head style comparison, each member a home/away percentage pair. */
    public static class Comparison {
        private Pair form;
        private Pair att;
        private Pair def;
        @SerializedName("poisson_distribution")
        private Pair poissonDistribution;
        private Pair h2h;
        private Pair goals;
        private Pair total;

        public Pair getForm() {
            return form;
        }

        public Pair getAtt() {
            return att;
        }

        public Pair getDef() {
            return def;
        }

        public Pair getPoissonDistribution() {
            return poissonDistribution;
        }

        public Pair getH2h() {
            return h2h;
        }

        public Pair getGoals() {
            return goals;
        }

        public Pair getTotal() {
            return total;
        }
    }

    /** Home/away percentages as text, e.g. {@code "87%"}. */
    public static class Pair {
        private String home;
        private String away;

        public String getHome() {
            return home;
        }

        public String getAway() {
            return away;
        }
    }
}

package es.com.kete1987.apifootball.library.fixtures.model;

import com.google.gson.annotations.SerializedName;
import es.com.kete1987.apifootball.library.common.util.ModelCollections;

import java.util.List;
import java.util.Map;

/**
 * League-wide statistics of a team as embedded in a prediction ({@code teams.home.league}): the
 * {@code /teams/statistics} response without its {@code league} and {@code team} members.
 */
public class PredictionTeamLeague {
    private String form;
    private Fixtures fixtures;
    private GoalsStats goals;
    private Biggest biggest;
    @SerializedName("clean_sheet")
    private Triple cleanSheet;
    @SerializedName("failed_to_score")
    private Triple failedToScore;
    private PenaltyStats penalty;
    private List<LineupUsage> lineups;
    private CardStats cards;

    /** Form string, one letter (W/D/L) per match. */
    public String getForm() {
        return form;
    }

    public Fixtures getFixtures() {
        return fixtures;
    }

    public GoalsStats getGoals() {
        return goals;
    }

    public Biggest getBiggest() {
        return biggest;
    }

    public Triple getCleanSheet() {
        return cleanSheet;
    }

    public Triple getFailedToScore() {
        return failedToScore;
    }

    public PenaltyStats getPenalty() {
        return penalty;
    }

    public List<LineupUsage> getLineups() {
        return ModelCollections.unmodifiable(lineups);
    }

    public CardStats getCards() {
        return cards;
    }

    /** Home / away / total counts. */
    public static class Triple {
        private Integer home;
        private Integer away;
        private Integer total;

        public Integer getHome() {
            return home;
        }

        public Integer getAway() {
            return away;
        }

        public Integer getTotal() {
            return total;
        }
    }

    /** Home / away / total averages, as text ({@code "2.4"}). */
    public static class TripleText {
        private String home;
        private String away;
        private String total;

        public String getHome() {
            return home;
        }

        public String getAway() {
            return away;
        }

        public String getTotal() {
            return total;
        }
    }

    /** Matches played, won, drawn and lost. */
    public static class Fixtures {
        private Triple played;
        private Triple wins;
        private Triple draws;
        private Triple loses;

        public Triple getPlayed() {
            return played;
        }

        public Triple getWins() {
            return wins;
        }

        public Triple getDraws() {
            return draws;
        }

        public Triple getLoses() {
            return loses;
        }
    }

    /** Goals scored and conceded. */
    public static class GoalsStats {
        @SerializedName("for")
        private GoalsDetail scored;
        private GoalsDetail against;

        /** Goals scored (the API key is {@code for}). */
        public GoalsDetail getFor() {
            return scored;
        }

        public GoalsDetail getAgainst() {
            return against;
        }
    }

    /** Totals, averages and distributions of goals. */
    public static class GoalsDetail {
        private Triple total;
        private TripleText average;
        private Map<String, MinuteStat> minute;
        @SerializedName("under_over")
        private Map<String, UnderOver> underOver;

        public Triple getTotal() {
            return total;
        }

        public TripleText getAverage() {
            return average;
        }

        /** Buckets keyed {@code "0-15"} ... {@code "106-120"}. */
        public Map<String, MinuteStat> getMinute() {
            return ModelCollections.unmodifiable(minute);
        }

        /** Lines keyed {@code "0.5"} ... {@code "4.5"}. */
        public Map<String, UnderOver> getUnderOver() {
            return ModelCollections.unmodifiable(underOver);
        }
    }

    /** One minute bucket; both members are {@code null} when empty. */
    public static class MinuteStat {
        private Integer total;
        private String percentage;

        public Integer getTotal() {
            return total;
        }

        public String getPercentage() {
            return percentage;
        }
    }

    public static class UnderOver {
        private Integer over;
        private Integer under;

        public Integer getOver() {
            return over;
        }

        public Integer getUnder() {
            return under;
        }
    }

    /** Biggest streaks, wins, defeats and goal counts. */
    public static class Biggest {
        private Streak streak;
        private Score wins;
        private Score loses;
        private BiggestGoals goals;

        public Streak getStreak() {
            return streak;
        }

        public Score getWins() {
            return wins;
        }

        /** Biggest defeats; members are {@code null} when none. */
        public Score getLoses() {
            return loses;
        }

        public BiggestGoals getGoals() {
            return goals;
        }
    }

    public static class Streak {
        private Integer wins;
        private Integer draws;
        private Integer loses;

        public Integer getWins() {
            return wins;
        }

        public Integer getDraws() {
            return draws;
        }

        public Integer getLoses() {
            return loses;
        }
    }

    /** Home / away result text such as {@code "5-1"}, {@code null} when none. */
    public static class Score {
        private String home;
        private String away;

        public String getHome() {
            return home;
        }

        public String getAway() {
            return away;
        }
    }

    public static class BiggestGoals {
        @SerializedName("for")
        private Goals scored;
        private Goals against;

        /** Most goals scored in a match (the API key is {@code for}). */
        public Goals getFor() {
            return scored;
        }

        public Goals getAgainst() {
            return against;
        }
    }

    public static class PenaltyStats {
        private Outcome scored;
        private Outcome missed;
        private Integer total;

        public Outcome getScored() {
            return scored;
        }

        public Outcome getMissed() {
            return missed;
        }

        public Integer getTotal() {
            return total;
        }
    }

    public static class Outcome {
        private Integer total;
        private String percentage;

        public Integer getTotal() {
            return total;
        }

        public String getPercentage() {
            return percentage;
        }
    }

    public static class LineupUsage {
        private String formation;
        private Integer played;

        public String getFormation() {
            return formation;
        }

        public Integer getPlayed() {
            return played;
        }
    }

    /** Cards per minute bucket. The real API may add a stray {@code ""} key to the maps. */
    public static class CardStats {
        private Map<String, MinuteStat> yellow;
        private Map<String, MinuteStat> red;

        public Map<String, MinuteStat> getYellow() {
            return ModelCollections.unmodifiable(yellow);
        }

        public Map<String, MinuteStat> getRed() {
            return ModelCollections.unmodifiable(red);
        }
    }
}

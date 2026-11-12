package es.com.kete1987.apifootball.library.teams.model;

import com.google.gson.annotations.SerializedName;
import es.com.kete1987.apifootball.library.common.util.ModelCollections;

import java.util.List;
import java.util.Map;

/**
 * Statistics of one team in one league and season ({@code /teams/statistics}).
 *
 * <p>Maps are keyed by data-dependent strings: minute buckets ({@code "0-15"} ... {@code "106-120"},
 * plus a stray empty-string key {@code ""} in cards) and under/over lines ({@code "0.5"} ...
 * {@code "4.5"}).
 */
public class TeamStatistics {
    private LeagueRef league;
    private TeamRef team;
    private String form;
    private Fixtures fixtures;
    private Goals goals;
    private Biggest biggest;
    @SerializedName("clean_sheet")
    private HomeAwayTotal cleanSheet;
    @SerializedName("failed_to_score")
    private HomeAwayTotal failedToScore;
    private Penalty penalty;
    private List<Lineup> lineups;
    private Cards cards;

    public LeagueRef getLeague() {
        return league;
    }

    public TeamRef getTeam() {
        return team;
    }

    /** One W/D/L letter per match played, oldest first. */
    public String getForm() {
        return form;
    }

    public Fixtures getFixtures() {
        return fixtures;
    }

    public Goals getGoals() {
        return goals;
    }

    public Biggest getBiggest() {
        return biggest;
    }

    public HomeAwayTotal getCleanSheet() {
        return cleanSheet;
    }

    public HomeAwayTotal getFailedToScore() {
        return failedToScore;
    }

    public Penalty getPenalty() {
        return penalty;
    }

    public List<Lineup> getLineups() {
        return ModelCollections.unmodifiable(lineups);
    }

    public Cards getCards() {
        return cards;
    }

    /** League the statistics refer to. */
    public static class LeagueRef {
        private Integer id;
        private String name;
        private String country;
        private String logo;
        private String flag;
        private Integer season;

        public Integer getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getCountry() {
            return country;
        }

        public String getLogo() {
            return logo;
        }

        /** Country flag; {@code null} for "World". */
        public String getFlag() {
            return flag;
        }

        public Integer getSeason() {
            return season;
        }
    }

    /** Matches played, won, drawn and lost. */
    public static class Fixtures {
        private HomeAwayTotal played;
        private HomeAwayTotal wins;
        private HomeAwayTotal draws;
        private HomeAwayTotal loses;

        public HomeAwayTotal getPlayed() {
            return played;
        }

        public HomeAwayTotal getWins() {
            return wins;
        }

        public HomeAwayTotal getDraws() {
            return draws;
        }

        public HomeAwayTotal getLoses() {
            return loses;
        }
    }

    /** Goals scored and conceded. */
    public static class Goals {
        @SerializedName("for")
        private GoalDetail goalsFor;
        private GoalDetail against;

        public GoalDetail getFor() {
            return goalsFor;
        }

        public GoalDetail getAgainst() {
            return against;
        }
    }

    /** Goal totals, averages, minute distribution and under/over counts of one side. */
    public static class GoalDetail {
        private HomeAwayTotal total;
        private Average average;
        private Map<String, MinuteStat> minute;
        @SerializedName("under_over")
        private Map<String, UnderOver> underOver;

        public HomeAwayTotal getTotal() {
            return total;
        }

        public Average getAverage() {
            return average;
        }

        /** Keyed by minute bucket ({@code "0-15"}, ..., {@code "106-120"}). */
        public Map<String, MinuteStat> getMinute() {
            return ModelCollections.unmodifiable(minute);
        }

        /** Keyed by line ({@code "0.5"}, ..., {@code "4.5"}). */
        public Map<String, UnderOver> getUnderOver() {
            return ModelCollections.unmodifiable(underOver);
        }
    }

    /** Goals per match, as text (e.g. {@code "2.4"}). */
    public static class Average {
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

    /** Matches above and below a goals line. */
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

    /** Streaks and biggest results. */
    public static class Biggest {
        private Streak streak;
        private ResultSplit wins;
        private ResultSplit loses;
        private BiggestGoals goals;

        public Streak getStreak() {
            return streak;
        }

        public ResultSplit getWins() {
            return wins;
        }

        public ResultSplit getLoses() {
            return loses;
        }

        public BiggestGoals getGoals() {
            return goals;
        }
    }

    /** Longest streaks. */
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

    /** Biggest result at home and away as text (e.g. {@code "5-1"}); {@code null} when none. */
    public static class ResultSplit {
        private String home;
        private String away;

        public String getHome() {
            return home;
        }

        public String getAway() {
            return away;
        }
    }

    /** Most goals scored/conceded in a single match. */
    public static class BiggestGoals {
        @SerializedName("for")
        private HomeAway goalsFor;
        private HomeAway against;

        public HomeAway getFor() {
            return goalsFor;
        }

        public HomeAway getAgainst() {
            return against;
        }
    }

    /** A counter split by home and away. */
    public static class HomeAway {
        private Integer home;
        private Integer away;

        public Integer getHome() {
            return home;
        }

        public Integer getAway() {
            return away;
        }
    }

    /** Penalties scored and missed. */
    public static class Penalty {
        private Share scored;
        private Share missed;
        private Integer total;

        public Share getScored() {
            return scored;
        }

        public Share getMissed() {
            return missed;
        }

        public Integer getTotal() {
            return total;
        }
    }

    /** Count and percentage text (e.g. {@code "100.00%"}). */
    public static class Share {
        private Integer total;
        private String percentage;

        public Integer getTotal() {
            return total;
        }

        public String getPercentage() {
            return percentage;
        }
    }

    /** A formation and how many matches it was used. */
    public static class Lineup {
        private String formation;
        private Integer played;

        public String getFormation() {
            return formation;
        }

        public Integer getPlayed() {
            return played;
        }
    }

    /** Yellow and red cards by minute bucket; the maps may contain a stray {@code ""} key. */
    public static class Cards {
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

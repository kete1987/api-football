package es.com.kete1987.apifootball.library.leagues.model;

import com.google.gson.annotations.SerializedName;

/** One row of a standings table. */
public class StandingEntry {
    private Integer rank;
    private Team team;
    private Integer points;
    private Integer goalsDiff;
    private String group;
    private String form;
    private String status;
    private String description;
    private Record all;
    private Record home;
    private Record away;
    private String update;

    public Integer getRank() {
        return rank;
    }

    public Team getTeam() {
        return team;
    }

    public Integer getPoints() {
        return points;
    }

    public Integer getGoalsDiff() {
        return goalsDiff;
    }

    /** Table label, e.g. {@code "Primera División"}, {@code "Group A"}, {@code "Group Stage"}. */
    public String getGroup() {
        return group;
    }

    /** Last results as W/D/L letters, e.g. {@code "WWLWW"}; may be {@code null} early in a season. */
    public String getForm() {
        return form;
    }

    /** Position trend: {@code same}, {@code up} or {@code down} (see {@code StandingStatuses}). */
    public String getStatus() {
        return status;
    }

    /** Meaning of the position (e.g. a qualification zone); {@code null} for mid-table. */
    public String getDescription() {
        return description;
    }

    public Record getAll() {
        return all;
    }

    public Record getHome() {
        return home;
    }

    public Record getAway() {
        return away;
    }

    /** ISO-8601 date-time of the last update. */
    public String getUpdate() {
        return update;
    }

    /** Team of a standings row. */
    public static class Team {
        private Integer id;
        private String name;
        private String logo;

        public Integer getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getLogo() {
            return logo;
        }
    }

    /** Played matches and results of a row (overall, at home or away). */
    public static class Record {
        private Integer played;
        private Integer win;
        private Integer draw;
        private Integer lose;
        private Goals goals;

        public Integer getPlayed() {
            return played;
        }

        public Integer getWin() {
            return win;
        }

        public Integer getDraw() {
            return draw;
        }

        public Integer getLose() {
            return lose;
        }

        public Goals getGoals() {
            return goals;
        }
    }

    /** Goals scored and conceded. */
    public static class Goals {
        @SerializedName("for")
        private Integer goalsFor;
        private Integer against;

        public Integer getFor() {
            return goalsFor;
        }

        public Integer getAgainst() {
            return against;
        }
    }
}

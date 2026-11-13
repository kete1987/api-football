package es.com.kete1987.apifootball.library.leagues.model;

import com.google.gson.annotations.SerializedName;
import es.com.kete1987.apifootball.library.leagues.util.StandingZones;

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

    /**
     * Meaning of the position as free text, e.g. {@code "Promotion - Champions League (League phase)"},
     * {@code "Relegation - LaLiga2"}, {@code "Round of 32"}; {@code null} for mid-table. Every value
     * seen in real data is listed in {@link StandingZones}.
     */
    public String getDescription() {
        return description;
    }

    /**
     * The category of {@link #getDescription()}: the text before the first {@code " - "}, trimmed
     * ({@code "Promotion"}, {@code "Relegation"}; see {@link StandingZones}), or {@code null} when the
     * description is {@code null} or has no {@code " - "} ({@code "Round of 32"}). Never throws.
     */
    public String getZoneCategory() {
        if (description == null) return null;
        int dash = description.indexOf(" - ");
        if (dash <= 0) return null;
        String category = description.substring(0, dash).trim();
        return category.isEmpty() ? null : category;
    }

    /**
     * The destination of {@link #getDescription()}: the text after the first {@code " - "}, trimmed
     * ({@code "Champions League (League phase)"}, {@code "LaLiga2"}), or {@code null} when there is no
     * {@code " - "}. Never throws.
     */
    public String getZoneTarget() {
        if (description == null) return null;
        int dash = description.indexOf(" - ");
        if (dash <= 0) return null;
        String target = description.substring(dash + 3).trim();
        return target.isEmpty() ? null : target;
    }

    /**
     * {@code true} when {@link #getZoneCategory()} is exactly {@link StandingZones#PROMOTION}: the five
     * {@code "Promotion - ..."} descriptions seen in real data. {@code false} for {@code null}, for
     * {@code "Relegation - ..."} and for {@code "Round of 32"} (World Cup rows that reach the next
     * round carry no category, so read {@link #getDescription()} for them).
     */
    public boolean isPromotion() {
        return StandingZones.PROMOTION.equals(getZoneCategory());
    }

    /**
     * {@code true} when {@link #getZoneCategory()} is exactly {@link StandingZones#RELEGATION}
     * ({@code "Relegation - LaLiga2"} in real data); {@code false} otherwise, including {@code null}.
     */
    public boolean isRelegation() {
        return StandingZones.RELEGATION.equals(getZoneCategory());
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

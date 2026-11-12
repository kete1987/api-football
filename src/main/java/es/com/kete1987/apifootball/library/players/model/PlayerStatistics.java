package es.com.kete1987.apifootball.library.players.model;

import com.google.gson.annotations.SerializedName;
import es.com.kete1987.apifootball.library.players.util.StatValues;

/** Statistics of a player for one team in one league season. Most counters are nullable. */
public class PlayerStatistics {
    private TeamRef team;
    private LeagueRef league;
    private Games games;
    private Substitutes substitutes;
    private Shots shots;
    private Goals goals;
    private Passes passes;
    private Tackles tackles;
    private Duels duels;
    private Dribbles dribbles;
    private Fouls fouls;
    private Cards cards;
    private Penalty penalty;

    public TeamRef getTeam() {
        return team;
    }

    public LeagueRef getLeague() {
        return league;
    }

    public Games getGames() {
        return games;
    }

    public Substitutes getSubstitutes() {
        return substitutes;
    }

    public Shots getShots() {
        return shots;
    }

    public Goals getGoals() {
        return goals;
    }

    public Passes getPasses() {
        return passes;
    }

    public Tackles getTackles() {
        return tackles;
    }

    public Duels getDuels() {
        return duels;
    }

    public Dribbles getDribbles() {
        return dribbles;
    }

    public Fouls getFouls() {
        return fouls;
    }

    public Cards getCards() {
        return cards;
    }

    public Penalty getPenalty() {
        return penalty;
    }

    public static class Games {
        // The API misspells the JSON name ("appearences").
        @SerializedName("appearences")
        private Integer appearances;
        private Integer lineups;
        private Integer minutes;
        private Integer number;
        private String position;
        private String rating;
        private Boolean captain;

        public Integer getAppearances() {
            return appearances;
        }

        public Integer getLineups() {
            return lineups;
        }

        public Integer getMinutes() {
            return minutes;
        }

        public Integer getNumber() {
            return number;
        }

        public String getPosition() {
            return position;
        }

        /** Average rating as text ({@code "7.11"}) or null. */
        public String getRating() {
            return rating;
        }

        /** {@link #getRating()} as a number; null when absent or not numeric. Never throws. */
        public Double getRatingValue() {
            return StatValues.parseDouble(rating);
        }

        public Boolean getCaptain() {
            return captain;
        }
    }

    public static class Substitutes {
        @SerializedName("in")
        private Integer in;
        private Integer out;
        private Integer bench;

        /** Times substituted in (JSON {@code in}). */
        public Integer getIn() {
            return in;
        }

        public Integer getOut() {
            return out;
        }

        public Integer getBench() {
            return bench;
        }
    }

    public static class Shots {
        private Integer total;
        private Integer on;

        public Integer getTotal() {
            return total;
        }

        /** Shots on target. */
        public Integer getOn() {
            return on;
        }
    }

    public static class Goals {
        private Integer total;
        private Integer conceded;
        private Integer assists;
        private Integer saves;

        public Integer getTotal() {
            return total;
        }

        public Integer getConceded() {
            return conceded;
        }

        public Integer getAssists() {
            return assists;
        }

        public Integer getSaves() {
            return saves;
        }
    }

    public static class Passes {
        private Integer total;
        private Integer key;
        private String accuracy;

        public Integer getTotal() {
            return total;
        }

        public Integer getKey() {
            return key;
        }

        /**
         * Raw accuracy as text. The API sends a number here, but its type differs between
         * endpoints ({@code "68%"} / count string elsewhere), so it is kept as text.
         */
        public String getAccuracy() {
            return accuracy;
        }

        /** {@link #getAccuracy()} parsed as a number (a trailing {@code %} is ignored); null if absent or not numeric. */
        public Integer getAccuracyValue() {
            return StatValues.parseInteger(accuracy);
        }
    }

    public static class Tackles {
        private Integer total;
        private Integer blocks;
        private Integer interceptions;

        public Integer getTotal() {
            return total;
        }

        public Integer getBlocks() {
            return blocks;
        }

        public Integer getInterceptions() {
            return interceptions;
        }
    }

    public static class Duels {
        private Integer total;
        private Integer won;

        public Integer getTotal() {
            return total;
        }

        public Integer getWon() {
            return won;
        }
    }

    public static class Dribbles {
        private Integer attempts;
        private Integer success;
        private Integer past;

        public Integer getAttempts() {
            return attempts;
        }

        public Integer getSuccess() {
            return success;
        }

        public Integer getPast() {
            return past;
        }
    }

    public static class Fouls {
        private Integer drawn;
        private Integer committed;

        public Integer getDrawn() {
            return drawn;
        }

        public Integer getCommitted() {
            return committed;
        }
    }

    public static class Cards {
        private Integer yellow;
        private Integer yellowred;
        private Integer red;

        public Integer getYellow() {
            return yellow;
        }

        public Integer getYellowred() {
            return yellowred;
        }

        public Integer getRed() {
            return red;
        }
    }

    public static class Penalty {
        private Integer won;
        // The API misspells the JSON name ("commited").
        @SerializedName("commited")
        private Integer committed;
        private Integer scored;
        private Integer missed;
        private Integer saved;

        public Integer getWon() {
            return won;
        }

        public Integer getCommitted() {
            return committed;
        }

        public Integer getScored() {
            return scored;
        }

        public Integer getMissed() {
            return missed;
        }

        public Integer getSaved() {
            return saved;
        }
    }
}

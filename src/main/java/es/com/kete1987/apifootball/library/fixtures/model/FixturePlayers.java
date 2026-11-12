package es.com.kete1987.apifootball.library.fixtures.model;

import es.com.kete1987.apifootball.library.common.model.TeamRef;
import es.com.kete1987.apifootball.library.common.util.ModelCollections;

import es.com.kete1987.apifootball.library.common.util.NumberParsing;
import java.util.List;

/**
 * Per-player statistics of one team in a fixture (one element per team). Every statistic is
 * nullable: the API returns {@code null} freely, and numbers that look numeric may be strings
 * ({@code passes.accuracy}, {@code games.rating}).
 */
public class FixturePlayers {
    private PlayersTeam team;
    private List<PlayerEntry> players;

    public PlayersTeam getTeam() {
        return team;
    }

    public List<PlayerEntry> getPlayers() {
        return ModelCollections.unmodifiable(players);
    }

    /** Team with the date of the last data update. */
    public static class PlayersTeam extends TeamRef {
        private String update;

        /** Last update as returned; real responses carried the Unix epoch ({@code 1970-01-01T00:00:00+00:00}). */
        public String getUpdate() {
            return update;
        }
    }

    /** A player and their statistics. */
    public static class PlayerEntry {
        private PlayerInfo player;
        private List<PlayerStatistics> statistics;

        public PlayerInfo getPlayer() {
            return player;
        }

        /** Statistics; the API returns a single element. */
        public List<PlayerStatistics> getStatistics() {
            return ModelCollections.unmodifiable(statistics);
        }

        /** First statistics element, or {@code null} when none. */
        public PlayerStatistics getFirstStatistics() {
            return statistics == null || statistics.isEmpty() ? null : statistics.get(0);
        }
    }

    /** Identity of a player. */
    public static class PlayerInfo {
        private Integer id;
        private String name;
        private String photo;

        public Integer getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getPhoto() {
            return photo;
        }
    }

    /** The statistics block of a player. */
    public static class PlayerStatistics {
        private Games games;
        private Integer offsides;
        private Shots shots;
        private GoalStats goals;
        private Passes passes;
        private Tackles tackles;
        private Duels duels;
        private Dribbles dribbles;
        private Fouls fouls;
        private Cards cards;
        private Penalty penalty;

        public Games getGames() {
            return games;
        }

        public Integer getOffsides() {
            return offsides;
        }

        public Shots getShots() {
            return shots;
        }

        public GoalStats getGoals() {
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
    }

    /** Playing time and role. */
    public static class Games {
        private Integer minutes;
        private Integer number;
        private String position;
        private String rating;
        private Boolean captain;
        private Boolean substitute;

        public Integer getMinutes() {
            return minutes;
        }

        public Integer getNumber() {
            return number;
        }

        public String getPosition() {
            return position;
        }

        /** Rating as text ({@code "6.29"}; {@code "0"} for unused substitutes), or {@code null}. */
        public String getRating() {
            return rating;
        }

        /** Rating parsed as a number, or {@code null} when absent or not numeric. Never throws. */
        public Double getRatingValue() {
            return NumberParsing.parseDouble(rating);
        }

        public Boolean getCaptain() {
            return captain;
        }

        public Boolean getSubstitute() {
            return substitute;
        }
    }

    public static class Shots {
        private Integer total;
        private Integer on;

        public Integer getTotal() {
            return total;
        }

        public Integer getOn() {
            return on;
        }
    }

    public static class GoalStats {
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
         * Accuracy as text: a count ({@code "9"}) in real responses, a percentage ({@code "68%"}) in
         * the documentation example, or {@code null}.
         */
        public String getAccuracy() {
            return accuracy;
        }

        /**
         * {@link #getAccuracy()} as a whole number ({@code "9"} gives {@code 9}, {@code "68%"} gives
         * {@code 68}), or {@code null} when absent or not a whole number. Never throws.
         *
         * <p>In real responses this is the <b>number</b> of accurate passes (at most {@link #getTotal()}),
         * not a percentage, and it is not the same measure as the season accuracy of {@code /players}.
         * The parse does not tell a count from a percentage: check whether {@link #getAccuracy()}
         * ends with {@code %} if that matters.
         */
        public Integer getAccuracyValue() {
            return NumberParsing.parseLeadingInteger(accuracy);
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
        private Integer red;

        public Integer getYellow() {
            return yellow;
        }

        public Integer getRed() {
            return red;
        }
    }

    /** Penalty statistics (the API spells {@code commited} with one {@code t}). */
    public static class Penalty {
        private Integer won;
        private Integer commited;
        private Integer scored;
        private Integer missed;
        private Integer saved;

        public Integer getWon() {
            return won;
        }

        /** Penalties committed (sic: the API key is {@code commited}). */
        public Integer getCommitted() {
            return commited;
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

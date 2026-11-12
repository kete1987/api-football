package es.com.kete1987.apifootball.library.leagues.model;

import com.google.gson.annotations.SerializedName;

/**
 * Data available for a season of a competition. It reflects what exists <em>now</em>: every flag
 * is {@code false} before the competition starts.
 */
public class Coverage {
    private FixturesCoverage fixtures;
    private Boolean standings;
    private Boolean players;
    @SerializedName("top_scorers")
    private Boolean topScorers;
    @SerializedName("top_assists")
    private Boolean topAssists;
    @SerializedName("top_cards")
    private Boolean topCards;
    private Boolean injuries;
    private Boolean predictions;
    private Boolean odds;

    public FixturesCoverage getFixtures() {
        return fixtures;
    }

    public Boolean getStandings() {
        return standings;
    }

    public Boolean getPlayers() {
        return players;
    }

    public Boolean getTopScorers() {
        return topScorers;
    }

    public Boolean getTopAssists() {
        return topAssists;
    }

    public Boolean getTopCards() {
        return topCards;
    }

    public Boolean getInjuries() {
        return injuries;
    }

    public Boolean getPredictions() {
        return predictions;
    }

    public Boolean getOdds() {
        return odds;
    }

    /** Fixture-level coverage. */
    public static class FixturesCoverage {
        private Boolean events;
        private Boolean lineups;
        @SerializedName("statistics_fixtures")
        private Boolean statisticsFixtures;
        @SerializedName("statistics_players")
        private Boolean statisticsPlayers;

        public Boolean getEvents() {
            return events;
        }

        public Boolean getLineups() {
            return lineups;
        }

        public Boolean getStatisticsFixtures() {
            return statisticsFixtures;
        }

        public Boolean getStatisticsPlayers() {
            return statisticsPlayers;
        }
    }
}

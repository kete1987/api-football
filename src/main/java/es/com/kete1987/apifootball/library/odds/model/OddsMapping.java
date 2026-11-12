package es.com.kete1987.apifootball.library.odds.model;

/** A fixture for which pre-match odds exist ({@code /odds/mapping}). */
public class OddsMapping {
    private LeagueSeasonRef league;
    private MappingFixture fixture;
    private String update;

    public LeagueSeasonRef getLeague() {
        return league;
    }

    public MappingFixture getFixture() {
        return fixture;
    }

    /** ISO-8601 time of the last update (always {@code +00:00}). */
    public String getUpdate() {
        return update;
    }

    /** Fixture id and kick-off of a mapping entry. */
    public static class MappingFixture {
        private Integer id;
        private String date;
        private Long timestamp;

        public Integer getId() {
            return id;
        }

        public String getDate() {
            return date;
        }

        public Long getTimestamp() {
            return timestamp;
        }
    }
}

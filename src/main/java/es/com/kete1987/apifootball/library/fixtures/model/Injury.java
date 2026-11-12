package es.com.kete1987.apifootball.library.fixtures.model;

import es.com.kete1987.apifootball.library.common.model.TeamRef;

/** A player missing (or doubtful for) a fixture because of injury, suspension, etc. */
public class Injury {
    private InjuredPlayer player;
    private TeamRef team;
    private InjuryFixture fixture;
    private FixtureLeague league;

    public InjuredPlayer getPlayer() {
        return player;
    }

    public TeamRef getTeam() {
        return team;
    }

    public InjuryFixture getFixture() {
        return fixture;
    }

    /** League reference ({@code round} and {@code standings} are not provided here). */
    public FixtureLeague getLeague() {
        return league;
    }

    /** The affected player and the reason. */
    public static class InjuredPlayer {
        private Integer id;
        private String name;
        private String photo;
        private String type;
        private String reason;

        public Integer getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getPhoto() {
            return photo;
        }

        /** {@code "Missing Fixture"} or {@code "Questionable"}. */
        public String getType() {
            return type;
        }

        /** Free text such as {@code "Suspended"} or {@code "Knee Injury"}. */
        public String getReason() {
            return reason;
        }
    }

    /** The fixture the player is missing. */
    public static class InjuryFixture {
        private Integer id;
        private String timezone;
        private String date;
        private Long timestamp;

        public Integer getId() {
            return id;
        }

        public String getTimezone() {
            return timezone;
        }

        public String getDate() {
            return date;
        }

        public Long getTimestamp() {
            return timestamp;
        }
    }
}

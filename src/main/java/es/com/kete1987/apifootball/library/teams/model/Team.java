package es.com.kete1987.apifootball.library.teams.model;

/** A team together with its home venue ({@code /teams} response item). */
public class Team {
    private TeamInfo team;
    private Venue venue;

    public TeamInfo getTeam() {
        return team;
    }

    /** Home venue; its fields may be {@code null} when unknown. */
    public Venue getVenue() {
        return venue;
    }
}

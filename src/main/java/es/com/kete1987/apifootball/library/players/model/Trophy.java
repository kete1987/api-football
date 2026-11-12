package es.com.kete1987.apifootball.library.players.model;

/** A trophy or placing of a player or coach ({@code /trophies} with a single {@code player}/{@code coach}). */
public class Trophy {
    private String league;
    private String country;
    private String season;
    private String place;

    public String getLeague() {
        return league;
    }

    public String getCountry() {
        return country;
    }

    /** Free text: {@code "2024/2025"}, {@code "2025"}, {@code "1998 France"}; null for some records. */
    public String getSeason() {
        return season;
    }

    /** {@code "Winner"}, {@code "2nd Place"}... */
    public String getPlace() {
        return place;
    }
}

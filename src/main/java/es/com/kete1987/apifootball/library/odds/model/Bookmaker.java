package es.com.kete1987.apifootball.library.odds.model;

/** A bookmaker ({@code /odds/bookmakers}). The list can contain {@code null} names. */
public class Bookmaker {
    private Integer id;
    private String name;

    public Integer getId() {
        return id;
    }

    /** Bookmaker name; {@code null} for some ids (37 at the time of writing). */
    public String getName() {
        return name;
    }
}

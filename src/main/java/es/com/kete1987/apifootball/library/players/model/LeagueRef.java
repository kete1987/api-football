package es.com.kete1987.apifootball.library.players.model;

/** Reference to a league season as embedded in {@code /players} statistics. */
public class LeagueRef {
    private Integer id;
    private String name;
    private String country;
    private String logo;
    private String flag;
    private Integer season;

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCountry() {
        return country;
    }

    public String getLogo() {
        return logo;
    }

    /** Flag URL; {@code null} for international competitions. */
    public String getFlag() {
        return flag;
    }

    public Integer getSeason() {
        return season;
    }
}

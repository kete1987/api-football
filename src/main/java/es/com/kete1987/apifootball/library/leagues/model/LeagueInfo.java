package es.com.kete1987.apifootball.library.leagues.model;

/** The competition itself: id, name, type and logo. */
public class LeagueInfo {
    private Integer id;
    private String name;
    private String type;
    private String logo;

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /** {@code "League"} or {@code "Cup"} (see {@code LeagueTypes}); capitalised in responses. */
    public String getType() {
        return type;
    }

    public String getLogo() {
        return logo;
    }
}

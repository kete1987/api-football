package es.com.kete1987.apifootball.library.teams.model;

/** Minimal team reference (id, name, logo) used inside statistics and coaches. */
public class TeamRef {
    private Integer id;
    private String name;
    private String logo;

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLogo() {
        return logo;
    }
}

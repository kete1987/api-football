package es.com.kete1987.apifootball.library.players.model;

/** Reference to a team ({@code id}, {@code name}, {@code logo}) as embedded in the players endpoints. */
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

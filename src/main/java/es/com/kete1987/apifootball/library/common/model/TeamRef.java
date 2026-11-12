package es.com.kete1987.apifootball.library.common.model;

/**
 * Reference to a team ({@code id}, {@code name}, {@code logo}), shared by every endpoint that embeds
 * exactly that object (fixtures, events, statistics, players, squads, transfers, coaches...).
 * Richer team objects extend it.
 */
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

    /** Logo URL. */
    public String getLogo() {
        return logo;
    }
}

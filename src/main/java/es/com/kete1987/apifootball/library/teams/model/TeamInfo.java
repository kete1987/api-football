package es.com.kete1987.apifootball.library.teams.model;

/** Identity of a team as returned by {@code /teams}. */
public class TeamInfo {
    private Integer id;
    private String name;
    private String code;
    private String country;
    private Integer founded;
    private Boolean national;
    private String logo;

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /** Three-letter code (e.g. {@code "REA"}); may be {@code null} for small teams. */
    public String getCode() {
        return code;
    }

    public String getCountry() {
        return country;
    }

    /** Founding year; may be {@code null}. */
    public Integer getFounded() {
        return founded;
    }

    /** {@code true} for national teams. */
    public Boolean getNational() {
        return national;
    }

    public String getLogo() {
        return logo;
    }
}

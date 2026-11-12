package es.com.kete1987.apifootball.library.fixtures.model;

/**
 * League reference embedded in fixtures, injuries and predictions. {@code round} and
 * {@code standings} are only present in fixtures; {@code flag} may be {@code null}.
 */
public class FixtureLeague {
    private Integer id;
    private String name;
    private String country;
    private String logo;
    private String flag;
    private Integer season;
    private String round;
    private Boolean standings;

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

    /** Country flag URL, or {@code null}. */
    public String getFlag() {
        return flag;
    }

    /** Season as the 4-digit starting year. */
    public Integer getSeason() {
        return season;
    }

    /** Round label such as {@code "Regular Season - 13"}; only present in fixtures. */
    public String getRound() {
        return round;
    }

    /** Whether the competition covers standings; only present in fixtures. */
    public Boolean getStandings() {
        return standings;
    }
}

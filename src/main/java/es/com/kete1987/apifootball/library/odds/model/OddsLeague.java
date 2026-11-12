package es.com.kete1987.apifootball.library.odds.model;

/** League reference of a pre-match odds entry ({@code /odds}). */
public class OddsLeague {
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

    /** Country flag URL; {@code null} for international competitions ("World"). */
    public String getFlag() {
        return flag;
    }

    /** Season as its starting year (2025 = 2025-2026). */
    public Integer getSeason() {
        return season;
    }
}

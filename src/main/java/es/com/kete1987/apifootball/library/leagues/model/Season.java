package es.com.kete1987.apifootball.library.leagues.model;

/** A season of a competition. */
public class Season {
    private Integer year;
    private String start;
    private String end;
    private Boolean current;
    private Coverage coverage;

    /** Four-digit starting year ({@code 2025} is the 2025-2026 season). */
    public Integer getYear() {
        return year;
    }

    /** First day, {@code YYYY-MM-DD}. */
    public String getStart() {
        return start;
    }

    /** Last day, {@code YYYY-MM-DD}. */
    public String getEnd() {
        return end;
    }

    public Boolean getCurrent() {
        return current;
    }

    public Coverage getCoverage() {
        return coverage;
    }
}

package es.com.kete1987.apifootball.library.teams.model;

/** Count and share of goals/cards in a minute bucket; both are {@code null} when there were none. */
public class MinuteStat {
    private Integer total;
    private String percentage;

    public Integer getTotal() {
        return total;
    }

    /** Percentage as text, e.g. {@code "8.97%"}. */
    public String getPercentage() {
        return percentage;
    }
}

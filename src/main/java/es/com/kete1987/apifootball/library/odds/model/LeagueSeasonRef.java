package es.com.kete1987.apifootball.library.odds.model;

/** League id and season, as embedded in {@code /odds/live} and {@code /odds/mapping}. */
public class LeagueSeasonRef {
    private Integer id;
    private Integer season;

    public Integer getId() {
        return id;
    }

    /** Season as the 4-digit starting year. */
    public Integer getSeason() {
        return season;
    }
}

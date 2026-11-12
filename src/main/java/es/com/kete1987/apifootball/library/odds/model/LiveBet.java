package es.com.kete1987.apifootball.library.odds.model;

/**
 * A bet type of the <b>in-play</b> catalogue ({@code /odds/live/bets}). Its ids are NOT compatible
 * with the pre-match catalogue ({@link PrematchBet}). Names can be {@code null} or duplicated.
 */
public class LiveBet {
    private Integer id;
    private String name;

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}

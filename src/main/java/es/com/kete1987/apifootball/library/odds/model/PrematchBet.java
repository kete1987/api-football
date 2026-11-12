package es.com.kete1987.apifootball.library.odds.model;

/**
 * A bet type of the <b>pre-match</b> catalogue ({@code /odds/bets}). Its ids are NOT compatible
 * with the in-play catalogue ({@link LiveBet}). Names can be {@code null} or duplicated.
 */
public class PrematchBet {
    private Integer id;
    private String name;

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}

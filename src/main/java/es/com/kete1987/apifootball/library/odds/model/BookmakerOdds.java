package es.com.kete1987.apifootball.library.odds.model;

import es.com.kete1987.apifootball.library.common.util.ModelCollections;

import java.util.List;

/** The pre-match bets offered by one bookmaker for a fixture. */
public class BookmakerOdds {
    private Integer id;
    private String name;
    private List<PrematchBetOdds> bets;

    public Integer getId() {
        return id;
    }

    /** Bookmaker name; can be {@code null} (bookmaker 37 has none). */
    public String getName() {
        return name;
    }

    public List<PrematchBetOdds> getBets() {
        return ModelCollections.unmodifiable(bets);
    }

    /** The bet with the given pre-match id, or {@code null}. */
    public PrematchBetOdds findBet(int betId) {
        if (bets == null) return null;
        for (PrematchBetOdds bet : bets) {
            if (bet.getId() != null && bet.getId() == betId) return bet;
        }
        return null;
    }
}

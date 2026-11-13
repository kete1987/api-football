package es.com.kete1987.apifootball.library.odds.model;

import es.com.kete1987.apifootball.library.common.util.ModelCollections;

import java.util.List;

/** The pre-match bets offered by one bookmaker for a fixture. */
public class BookmakerOdds {
    private Integer id;
    private String name;
    private List<PrematchBetOdds> bets;

    /**
     * Bookmaker id (see {@code BookmakerIds}). Some bookmakers of the catalogue ({@code /odds/bookmakers})
     * rarely or never appear in the odds of a fixture (888Sport and Interwetten were absent from every
     * fixture checked), so always handle {@code PrematchOdds.findBookmaker} returning {@code null}.
     */
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
    public PrematchBetOdds findBet(int id) {
        if (bets == null) return null;
        for (PrematchBetOdds bet : bets) {
            if (bet.getId() != null && bet.getId() == id) return bet;
        }
        return null;
    }

    /** The 1X2 market ({@code PrematchBetIds.MATCH_WINNER}, id 1), or {@code null}. */
    public PrematchBetOdds findMatchWinner() {
        return findBet(es.com.kete1987.apifootball.library.odds.util.PrematchBetIds.MATCH_WINNER);
    }

    /** The "Exact Score" market ({@code PrematchBetIds.EXACT_SCORE}, id 10), or {@code null}. */
    public PrematchBetOdds findExactScore() {
        return findBet(es.com.kete1987.apifootball.library.odds.util.PrematchBetIds.EXACT_SCORE);
    }
}

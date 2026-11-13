package es.com.kete1987.apifootball.library.odds.util;

/**
 * Ids of well-known bookmakers of {@code /odds/bookmakers} (snapshot 2026-11-12, docs/API_CATALOG.md
 * Appendix A; the first-listed five are the ones the Quinidroid apps map). The catalogue changes
 * several times a week, so treat any other id as data: look it up with
 * {@code OddsApi.getBookmakers()}.
 *
 * <p>Being in the catalogue does not mean a bookmaker appears in the odds of a fixture: in the
 * Real Madrid - Celta example, {@link #BET365}, {@link #WILLIAM_HILL} and {@link #BETFAIR} were
 * present, while {@link #SPORT_888} and {@link #INTERWETTEN} were absent from every fixture
 * checked. Always handle {@code PrematchOdds.findBookmaker(int)} returning {@code null}.
 */
public final class BookmakerIds {
    private BookmakerIds() {
    }

    /** Bet365. */
    public static final int BET365 = 8;
    /** William Hill. */
    public static final int WILLIAM_HILL = 7;
    /** Betfair. */
    public static final int BETFAIR = 3;
    /** 888Sport (rarely, if ever, present in the odds of a fixture). */
    public static final int SPORT_888 = 21;
    /** Interwetten (rarely, if ever, present in the odds of a fixture). */
    public static final int INTERWETTEN = 15;

    /** 10Bet. */
    public static final int BET_10 = 1;
    /** Marathonbet. */
    public static final int MARATHONBET = 2;
    /** Pinnacle. */
    public static final int PINNACLE = 4;
    /** Bwin. */
    public static final int BWIN = 6;
    /** 1xBet. */
    public static final int ONE_X_BET = 11;
    /** Unibet. */
    public static final int UNIBET = 16;
    /** Betway. */
    public static final int BETWAY = 24;
    /** Betsson. */
    public static final int BETSSON = 26;
    /** Betano. */
    public static final int BETANO = 32;
    /** BetVictor. */
    public static final int BETVICTOR = 36;
}

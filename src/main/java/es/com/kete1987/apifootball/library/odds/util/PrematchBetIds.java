package es.com.kete1987.apifootball.library.odds.util;

/**
 * A few stable, well-known <b>pre-match</b> bet ids (catalogue of {@code /odds/bets}). These ids
 * are NOT valid for in-play odds, see {@link LiveBetIds}. Any other id is data: look it up with
 * {@code OddsApi.getPrematchBets()}.
 */
public final class PrematchBetIds {
    private PrematchBetIds() {
    }

    public static final int MATCH_WINNER = 1;
    public static final int ASIAN_HANDICAP = 4;
    public static final int GOALS_OVER_UNDER = 5;
    public static final int BOTH_TEAMS_SCORE = 8;
    public static final int EXACT_SCORE = 10;
    public static final int DOUBLE_CHANCE = 12;
}

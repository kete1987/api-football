package es.com.kete1987.apifootball.library.odds.util;

/**
 * A few well-known <b>in-play</b> bet ids (catalogue of {@code /odds/live/bets}). This is a
 * different id space from {@link PrematchBetIds}: e.g. pre-match 1 is "Match Winner" while in-play
 * 1 is "Over/Under Extra Time". Any other id is data: look it up with
 * {@code OddsApi.getLiveBets()}.
 */
public final class LiveBetIds {
    private LiveBetIds() {
    }

    /** "Fulltime Result" (1x2). */
    public static final int FULLTIME_RESULT = 59;
    public static final int ASIAN_HANDICAP = 33;
    public static final int MATCH_GOALS = 25;
    public static final int BOTH_TEAMS_TO_SCORE = 69;
    public static final int DOUBLE_CHANCE = 72;
}

package es.com.kete1987.apifootball.library.fixtures.util;

/**
 * Team statistic names of {@code /fixtures/statistics} ({@code statistics[].type}), as plain strings.
 *
 * <p>The set is open and its order varies between responses. Real 2026 responses carry 17 types: the
 * documented list without {@code "Passes %"}, plus {@code "Free Kicks"} and {@code "expected_goals"}
 * (snake_case). The same names are accepted by the {@code type} filter.
 */
public final class StatisticTypes {

    public static final String SHOTS_ON_GOAL = "Shots on Goal";
    public static final String SHOTS_OFF_GOAL = "Shots off Goal";
    public static final String SHOTS_INSIDE_BOX = "Shots insidebox";
    public static final String SHOTS_OUTSIDE_BOX = "Shots outsidebox";
    public static final String TOTAL_SHOTS = "Total Shots";
    public static final String BLOCKED_SHOTS = "Blocked Shots";
    public static final String FOULS = "Fouls";
    public static final String CORNER_KICKS = "Corner Kicks";
    public static final String OFFSIDES = "Offsides";
    public static final String BALL_POSSESSION = "Ball Possession";
    public static final String YELLOW_CARDS = "Yellow Cards";
    public static final String RED_CARDS = "Red Cards";
    public static final String GOALKEEPER_SAVES = "Goalkeeper Saves";
    public static final String TOTAL_PASSES = "Total passes";
    public static final String PASSES_ACCURATE = "Passes accurate";
    /** Documented but absent from real responses. */
    public static final String PASSES_PERCENT = "Passes %";
    /** Present in real responses, not in the documented list. */
    public static final String FREE_KICKS = "Free Kicks";
    /** Present in real responses, not in the documented list; value is text such as {@code "1.71"}. */
    public static final String EXPECTED_GOALS = "expected_goals";

    private StatisticTypes() {
        // constants only
    }
}

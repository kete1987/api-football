package es.com.kete1987.apifootball.library.fixtures.util;

/**
 * Event {@code type} and {@code detail} values of {@code /fixtures/events}, as plain strings (the API
 * adds values over time, e.g. {@code "Substitution 5"}).
 *
 * <p>Real responses mix casing ({@code "Goal"}, {@code "Card"}, {@code "subst"}, {@code "Var"}), so
 * compare case-insensitively. The {@code type} <em>filter</em> takes lower case ({@code goal},
 * {@code card}, {@code subst}, {@code var}); use the {@code FILTER_*} constants for it.
 */
public final class EventTypes {

    /** Event type: goal. */
    public static final String GOAL = "Goal";
    /** Event type: card. */
    public static final String CARD = "Card";
    /** Event type: substitution (lower case in real responses). */
    public static final String SUBSTITUTION = "subst";
    /** Event type: VAR decision. */
    public static final String VAR = "Var";

    /** Value of the {@code type} filter for goals. */
    public static final String FILTER_GOAL = "goal";
    /** Value of the {@code type} filter for cards. */
    public static final String FILTER_CARD = "card";
    /** Value of the {@code type} filter for substitutions. */
    public static final String FILTER_SUBSTITUTION = "subst";
    /** Value of the {@code type} filter for VAR events. */
    public static final String FILTER_VAR = "var";

    /** Goal detail. */
    public static final String NORMAL_GOAL = "Normal Goal";
    /** Goal detail. */
    public static final String OWN_GOAL = "Own Goal";
    /** Goal detail. */
    public static final String PENALTY = "Penalty";
    /** Goal detail. */
    public static final String MISSED_PENALTY = "Missed Penalty";

    /** Card detail. */
    public static final String YELLOW_CARD = "Yellow Card";
    /** Card detail (the documentation spells it "Red card"; compare case-insensitively). */
    public static final String RED_CARD = "Red Card";

    /** VAR detail. */
    public static final String GOAL_CANCELLED = "Goal cancelled";
    /** VAR detail. */
    public static final String PENALTY_CONFIRMED = "Penalty confirmed";

    /** Prefix of substitution details: {@code "Substitution 1"}, {@code "Substitution 2"}... */
    public static final String SUBSTITUTION_DETAIL_PREFIX = "Substitution";

    private EventTypes() {
        // constants only
    }
}

package es.com.kete1987.apifootball.library.competition.util;

/**
 * State of a tie, as answered by {@code Tie.getState()}. Decided on the status {@code short} code
 * of each leg (see {@code FixtureStatuses}):
 * <ul>
 *   <li>a leg is <i>played</i> when its status is finished ({@code FT}, {@code AET}, {@code PEN}) or
 *       not played but decided ({@code AWD}, {@code WO}) and both its goals are known;</li>
 *   <li>a leg is <i>live</i> when its status is in play ({@code 1H} ... {@code LIVE}, including
 *       {@code SUSP} and {@code INT});</li>
 *   <li>any other leg ({@code TBD}, {@code NS}, {@code PST}, {@code CANC}, {@code ABD}, unknown
 *       codes) is neither.</li>
 * </ul>
 */
public final class TieStates {

    /** No leg is played or live. */
    public static final String NOT_STARTED = "NOT_STARTED";
    /** At least one leg is live. */
    public static final String IN_PLAY = "IN_PLAY";
    /**
     * At least one leg is played, none is live and not every leg is played: in a two-legged tie, the
     * first leg is over and the second has not started (or was postponed, abandoned or cancelled).
     */
    public static final String FIRST_LEG_PLAYED = "FIRST_LEG_PLAYED";
    /** Every leg is played. The tie may still have no winner; see {@code Tie.getWinner()}. */
    public static final String FINISHED = "FINISHED";

    private TieStates() {
        // constants only
    }
}

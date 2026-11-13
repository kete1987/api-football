package es.com.kete1987.apifootball.library.fixtures.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Fixture status short codes ({@code fixture.status.short}) and helpers that classify them with the
 * groups of the API documentation (Scheduled, In Play, Finished, Postponed, Cancelled, Abandoned,
 * Not Played).
 *
 * <p>Always classify by the {@code short} code, never by {@code long}: the long wording differs
 * between the documentation and real responses. The API adds values over time, so these are plain
 * {@code String} constants rather than an enum; an unknown code is simply in no group.
 *
 * <p>The {@code status} filter of {@code /fixtures} accepts several codes joined with {@code -}
 * (e.g. {@code "FT-AET-PEN"}); see {@link #join(String...)} and the {@code *_GROUP} constants.
 */
public final class FixtureStatuses {

    /** Time to be defined: scheduled but date and time unknown. */
    public static final String TBD = "TBD";
    /** Not started. */
    public static final String NS = "NS";
    /** First half in play. */
    public static final String FIRST_HALF = "1H";
    /** Halftime. */
    public static final String HALFTIME = "HT";
    /** Second half in play. */
    public static final String SECOND_HALF = "2H";
    /** Extra time in play. */
    public static final String EXTRA_TIME = "ET";
    /** Break during extra time. */
    public static final String BREAK_TIME = "BT";
    /** Penalty shootout in progress. */
    public static final String PENALTY_IN_PROGRESS = "P";
    /** Suspended by the referee, may be rescheduled. */
    public static final String SUSPENDED = "SUSP";
    /** Interrupted by the referee, should resume shortly. */
    public static final String INTERRUPTED = "INT";
    /** In progress, very rare: half/elapsed data unavailable. */
    public static final String LIVE = "LIVE";
    /** Finished in regular time. */
    public static final String FT = "FT";
    /** Finished after extra time, without a shootout. */
    public static final String AET = "AET";
    /** Finished after the penalty shootout. */
    public static final String PEN = "PEN";
    /** Postponed to another day. */
    public static final String POSTPONED = "PST";
    /** Cancelled. */
    public static final String CANCELLED = "CANC";
    /** Abandoned (weather, safety...). */
    public static final String ABANDONED = "ABD";
    /** Technical loss. */
    public static final String TECHNICAL_LOSS = "AWD";
    /** Walkover: victory by forfeit or absence of the opponent. */
    public static final String WALKOVER = "WO";

    /** Scheduled group: {@code TBD-NS}. */
    public static final String SCHEDULED_GROUP = "TBD-NS";
    /** In-play group, usable as a {@code status} filter. */
    public static final String IN_PLAY_GROUP = "1H-HT-2H-ET-BT-P-SUSP-INT-LIVE";
    /** Finished group: {@code FT-AET-PEN}. */
    public static final String FINISHED_GROUP = "FT-AET-PEN";

    private static final Set<String> SCHEDULED = set(TBD, NS);
    private static final Set<String> IN_PLAY = set(FIRST_HALF, HALFTIME, SECOND_HALF, EXTRA_TIME,
            BREAK_TIME, PENALTY_IN_PROGRESS, SUSPENDED, INTERRUPTED, LIVE);
    private static final Set<String> INTERRUPTED_CODES = set(SUSPENDED, INTERRUPTED);
    private static final Set<String> FINISHED = set(FT, AET, PEN);
    private static final Set<String> NOT_PLAYED = set(TECHNICAL_LOSS, WALKOVER);

    private FixtureStatuses() {
        // constants and helpers only
    }

    private static Set<String> set(String... values) {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(values)));
    }

    private static boolean in(Set<String> group, String shortCode) {
        return shortCode != null && group.contains(shortCode.trim().toUpperCase());
    }

    /** {@code true} for {@code TBD} and {@code NS}. */
    public static boolean isScheduled(String shortCode) {
        return in(SCHEDULED, shortCode);
    }

    /**
     * {@code true} for every "In Play" code of the API documentation: {@code 1H, HT, 2H, ET, BT, P,
     * SUSP, INT, LIVE}.
     *
     * <p><b>Warning: this includes {@code SUSP} (suspended) and {@code INT} (interrupted).</b> The API
     * groups them as "in play", but the match is not being played: a screen that shows a running
     * minute whenever {@code isLive()} is {@code true} will keep showing a minute for a suspended match.
     * To handle them, check {@link #isInterrupted(String)} <em>first</em>, or use
     * {@link #isPlaying(String)}, which leaves them out. {@code HT} and {@code BT} are live too, and
     * {@code LIVE} (very rare) is live with no {@code elapsed}.
     *
     * <p>Truth table (verified by {@code FixtureHelpersStatusTest}): true for {@code 1H, HT, 2H, ET, BT,
     * P, SUSP, INT, LIVE}; false for every other code, for {@code null} and for unknown codes.
     */
    public static boolean isLive(String shortCode) {
        return in(IN_PLAY, shortCode);
    }

    /**
     * {@link #isLive(String)} minus {@code SUSP} and {@code INT}: the match is under way and expected to
     * continue now. Note that it still includes the breaks {@code HT} and {@code BT}, during which the
     * ball is not rolling. For {@code SUSP}/{@code INT} see {@link #isInterrupted(String)}.
     */
    public static boolean isPlaying(String shortCode) {
        return isLive(shortCode) && !isInterrupted(shortCode);
    }

    /**
     * {@code true} for {@code SUSP} (suspended by the referee, may be rescheduled) and {@code INT}
     * (interrupted, should resume shortly): "interrupted, pending resolution". Both are inside
     * {@link #isLive(String)} in the API's grouping, which is why this helper exists: check it before
     * treating a match as in play. {@code false} for everything else, including {@code PST},
     * {@code ABD} and {@code null}.
     */
    public static boolean isInterrupted(String shortCode) {
        return in(INTERRUPTED_CODES, shortCode);
    }

    /**
     * {@code true} when the match is not being played and its outcome is still to be decided or will
     * not happen as scheduled: {@code PST, SUSP, INT, CANC, ABD, AWD, WO} (the set an app typically
     * shows as "postponed"). Equals {@link #isInterrupted(String)} plus {@link #isPostponed(String)},
     * {@link #isCancelled(String)}, {@link #isAbandoned(String)} and {@link #isNotPlayed(String)}.
     */
    public static boolean isPendingResolution(String shortCode) {
        return isInterrupted(shortCode) || isPostponed(shortCode) || isCancelled(shortCode)
                || isAbandoned(shortCode) || isNotPlayed(shortCode);
    }

    /** {@code true} for {@code FT}, {@code AET} and {@code PEN}. */
    public static boolean isFinished(String shortCode) {
        return in(FINISHED, shortCode);
    }

    /** {@code true} for {@code PST}. */
    public static boolean isPostponed(String shortCode) {
        return POSTPONED.equalsIgnoreCase(shortCode == null ? null : shortCode.trim());
    }

    /** {@code true} for {@code CANC}. */
    public static boolean isCancelled(String shortCode) {
        return CANCELLED.equalsIgnoreCase(shortCode == null ? null : shortCode.trim());
    }

    /** {@code true} for {@code ABD}. */
    public static boolean isAbandoned(String shortCode) {
        return ABANDONED.equalsIgnoreCase(shortCode == null ? null : shortCode.trim());
    }

    /** {@code true} for {@code AWD} and {@code WO}. */
    public static boolean isNotPlayed(String shortCode) {
        return in(NOT_PLAYED, shortCode);
    }

    /** Joins several codes with {@code -} for the {@code status} filter, e.g. {@code "FT-AET-PEN"}. */
    public static String join(String... shortCodes) {
        if (shortCodes == null || shortCodes.length == 0) {
            throw new IllegalArgumentException("At least one status code is required");
        }
        return String.join("-", shortCodes);
    }
}

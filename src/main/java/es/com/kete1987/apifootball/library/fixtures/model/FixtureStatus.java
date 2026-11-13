package es.com.kete1987.apifootball.library.fixtures.model;

import com.google.gson.annotations.SerializedName;
import es.com.kete1987.apifootball.library.fixtures.util.FixtureMinutes;
import es.com.kete1987.apifootball.library.fixtures.util.FixtureStatuses;

/**
 * Status of a fixture. Always classify by {@link #getShort()} (see {@link FixtureStatuses}); the long
 * wording differs between the documentation and real responses.
 */
public class FixtureStatus {
    @SerializedName("long")
    private String longName;
    @SerializedName("short")
    private String shortCode;
    private Integer elapsed;
    private Integer extra;

    /** Descriptive text such as {@code "Match Finished"}. */
    public String getLong() {
        return longName;
    }

    /** Status code such as {@code "FT"} or {@code "NS"}. */
    public String getShort() {
        return shortCode;
    }

    /**
     * Minute in progress (1 when the match starts, <b>about +1 versus Sportmonks</b>), kept at 45/90
     * while added time runs in {@link #getExtra()}; {@code null} before the start and at {@code BT}.
     * Unreliable during extra time: see {@link FixtureMinutes}.
     */
    public Integer getElapsed() {
        return elapsed;
    }

    /**
     * Added time of the current or last half; {@code null} when none. At {@code HT}/{@code BT} it keeps
     * the value of the half just played. See {@link #getDisplayMinute()}.
     */
    public Integer getExtra() {
        return extra;
    }

    /** See {@link FixtureStatuses#isScheduled(String)}. */
    public boolean isScheduled() {
        return FixtureStatuses.isScheduled(shortCode);
    }

    /**
     * See {@link FixtureStatuses#isLive(String)}. <b>Includes {@code SUSP} and {@code INT}</b>
     * (suspended / interrupted matches): check {@link #isInterrupted()} first or use
     * {@link #isPlaying()}, otherwise an interrupted match keeps showing a running minute.
     */
    public boolean isLive() {
        return FixtureStatuses.isLive(shortCode);
    }

    /** See {@link FixtureStatuses#isPlaying(String)}: live without {@code SUSP}/{@code INT} (still true at {@code HT}/{@code BT}). */
    public boolean isPlaying() {
        return FixtureStatuses.isPlaying(shortCode);
    }

    /** See {@link FixtureStatuses#isInterrupted(String)}: {@code SUSP} or {@code INT}, which {@link #isLive()} also counts. */
    public boolean isInterrupted() {
        return FixtureStatuses.isInterrupted(shortCode);
    }

    /** See {@link FixtureStatuses#isPendingResolution(String)}: {@code PST, SUSP, INT, CANC, ABD, AWD, WO}. */
    public boolean isPendingResolution() {
        return FixtureStatuses.isPendingResolution(shortCode);
    }

    /**
     * Total minute to show, {@code elapsed + extra} ({@code 90} and {@code 4} give 94), with a
     * {@code null} {@code extra} counted as 0; {@code null} when {@code elapsed} is {@code null}.
     * Does not look at the status: at {@code HT}/{@code BT} the {@code extra} is the stale one of the
     * half just played, so check {@link #isPlaying()} and the status code first.
     *
     * <p>Observed live: the API's minute runs about +1 versus Sportmonks, added time comes in
     * {@code extra}, and the extra-time minute is unreliable (it can start too high and go
     * backwards); see {@link FixtureMinutes} for the figures.
     */
    public Integer getDisplayMinute() {
        return FixtureMinutes.total(elapsed, extra);
    }

    /**
     * The minute as written on a score-board, {@code "90+4"} with added time and {@code "45"}
     * without; {@code null} when {@code elapsed} is {@code null}. Same caveats as
     * {@link #getDisplayMinute()}.
     */
    public String getDisplayMinuteText() {
        return FixtureMinutes.format(elapsed, extra);
    }

    /** See {@link FixtureStatuses#isFinished(String)}. */
    public boolean isFinished() {
        return FixtureStatuses.isFinished(shortCode);
    }

    /** See {@link FixtureStatuses#isPostponed(String)}. */
    public boolean isPostponed() {
        return FixtureStatuses.isPostponed(shortCode);
    }

    /** See {@link FixtureStatuses#isCancelled(String)}. */
    public boolean isCancelled() {
        return FixtureStatuses.isCancelled(shortCode);
    }

    /** See {@link FixtureStatuses#isAbandoned(String)}. */
    public boolean isAbandoned() {
        return FixtureStatuses.isAbandoned(shortCode);
    }

    /** See {@link FixtureStatuses#isNotPlayed(String)}. */
    public boolean isNotPlayed() {
        return FixtureStatuses.isNotPlayed(shortCode);
    }
}

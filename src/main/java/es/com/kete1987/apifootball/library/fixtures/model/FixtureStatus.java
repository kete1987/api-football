package es.com.kete1987.apifootball.library.fixtures.model;

import com.google.gson.annotations.SerializedName;
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

    /** Minutes played; {@code null} before the start. */
    public Integer getElapsed() {
        return elapsed;
    }

    /** Added time of the current or last half; {@code null} when none. */
    public Integer getExtra() {
        return extra;
    }

    /** See {@link FixtureStatuses#isScheduled(String)}. */
    public boolean isScheduled() {
        return FixtureStatuses.isScheduled(shortCode);
    }

    /** See {@link FixtureStatuses#isLive(String)}. */
    public boolean isLive() {
        return FixtureStatuses.isLive(shortCode);
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

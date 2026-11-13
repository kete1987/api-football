package es.com.kete1987.apifootball.library.competition.util;

/**
 * Where a stage stands relative to a date, as answered by {@code Stage.getStatus(LocalDate)}. It is
 * decided on the round dates of {@code fixtures/rounds?dates=true} only (API-Football has no
 * "finished" or "current" flag per stage), so a postponed match moved to a later date moves the
 * stage's last date with it, but a match postponed without a new date is not visible here.
 */
public final class StageStatuses {

    /** Every round date of the stage is before the reference date. */
    public static final String FINISHED = "FINISHED";
    /** The reference date is between the first and the last round date of the stage, both included. */
    public static final String CURRENT = "CURRENT";
    /** Every round date of the stage is after the reference date. */
    public static final String UPCOMING = "UPCOMING";
    /** The stage has no dates (built from {@code fixtures/rounds} without {@code dates=true}) or no reference date was given. */
    public static final String UNKNOWN = "UNKNOWN";

    private StageStatuses() {
        // constants only
    }
}

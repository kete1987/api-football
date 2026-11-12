package es.com.kete1987.apifootball.library.odds.model;

/**
 * Fixture reference of a pre-match odds entry. The date can be stale with respect to
 * {@code /fixtures} (the odds feed is not refreshed when a fixture is rescheduled).
 */
public class OddsFixtureRef {
    private Integer id;
    private String timezone;
    private String date;
    private Long timestamp;

    public Integer getId() {
        return id;
    }

    /** Timezone the {@link #getDate()} is rendered in ({@code "UTC"} unless the request set one). */
    public String getTimezone() {
        return timezone;
    }

    /** ISO-8601 date-time, e.g. {@code 2026-11-22T16:00:00+00:00}. */
    public String getDate() {
        return date;
    }

    /** Absolute Unix time in seconds. */
    public Long getTimestamp() {
        return timestamp;
    }
}

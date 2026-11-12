package es.com.kete1987.apifootball.library.fixtures.model;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

/** The {@code fixture} member of a fixture: identity, date, venue and status. */
public class FixtureInfo {
    private Integer id;
    private String referee;
    private String timezone;
    private String date;
    private Long timestamp;
    private Periods periods;
    private Venue venue;
    private FixtureStatus status;

    public Integer getId() {
        return id;
    }

    /** Referee name; {@code null} for most fixtures. */
    public String getReferee() {
        return referee;
    }

    /** {@code "UTC"} or the requested timezone. */
    public String getTimezone() {
        return timezone;
    }

    /** ISO-8601 date with offset, as returned. */
    public String getDate() {
        return date;
    }

    /** {@link #getDate()} parsed, or {@code null} when absent or not parseable. Never throws. */
    public OffsetDateTime getDateTime() {
        if (date == null) return null;
        try {
            return OffsetDateTime.parse(date);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** Absolute Unix time in seconds. */
    public Long getTimestamp() {
        return timestamp;
    }

    public Periods getPeriods() {
        return periods;
    }

    public Venue getVenue() {
        return venue;
    }

    public FixtureStatus getStatus() {
        return status;
    }

    /** Kick-off timestamps (Unix seconds) of each half; {@code null} while not started. */
    public static class Periods {
        private Long first;
        private Long second;

        public Long getFirst() {
            return first;
        }

        public Long getSecond() {
            return second;
        }
    }
}

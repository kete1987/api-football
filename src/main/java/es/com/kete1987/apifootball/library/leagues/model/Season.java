package es.com.kete1987.apifootball.library.leagues.model;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * A season of a competition.
 *
 * <p>{@link #getCurrent()} does not mean "in progress": the API keeps the latest season flagged
 * {@code current} after it ends (the World Cup 2026, which ended on 2026-07-19, is still
 * {@code current: true} in November 2026). Decide on dates with {@link #hasStarted(LocalDate)},
 * {@link #isOver(LocalDate)} and {@link #isInProgress(LocalDate)}.
 */
public class Season {
    private Integer year;
    private String start;
    private String end;
    private Boolean current;
    private Coverage coverage;

    /** Four-digit starting year ({@code 2025} is the 2025-2026 season). */
    public Integer getYear() {
        return year;
    }

    /** First day, {@code YYYY-MM-DD}. */
    public String getStart() {
        return start;
    }

    /** Last day, {@code YYYY-MM-DD}. */
    public String getEnd() {
        return end;
    }

    /**
     * Whether the API flags this season as the competition's current one. It stays {@code true}
     * after the season ends, until the next season is flagged: see the class Javadoc.
     */
    public Boolean getCurrent() {
        return current;
    }

    /** {@link #getStart()} as a date, or {@code null} when it is missing or not a valid date. */
    public LocalDate getStartDate() {
        return parse(start);
    }

    /** {@link #getEnd()} as a date, or {@code null} when it is missing or not a valid date. */
    public LocalDate getEndDate() {
        return parse(end);
    }

    /**
     * {@code true} when {@code date} is on or after the start date; {@code false} when it is before,
     * or when the start date or {@code date} is missing. Never throws.
     */
    public boolean hasStarted(LocalDate date) {
        LocalDate first = getStartDate();
        return date != null && first != null && !date.isBefore(first);
    }

    /**
     * {@code true} when {@code date} is after the end date; {@code false} on the last day, before it,
     * or when the end date or {@code date} is missing. Never throws.
     *
     * <p>{@code end} is the date of the last fixture the API knows, not the planned end of the
     * competition: the Champions League 2026-27 season reads {@code 2026-07-07} to
     * {@code 2027-01-27} (the end of its league phase) while its knockout rounds are not scheduled
     * yet. A season can therefore look over and later get a later end date: check that no fixture
     * of the season is pending before treating it as finished for good.
     */
    public boolean isOver(LocalDate date) {
        LocalDate last = getEndDate();
        return date != null && last != null && date.isAfter(last);
    }

    /** {@code true} when {@link #hasStarted(LocalDate)} and not {@link #isOver(LocalDate)}. */
    public boolean isInProgress(LocalDate date) {
        return hasStarted(date) && !isOver(date);
    }

    private static LocalDate parse(String text) {
        if (text == null) return null;
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public Coverage getCoverage() {
        return coverage;
    }
}

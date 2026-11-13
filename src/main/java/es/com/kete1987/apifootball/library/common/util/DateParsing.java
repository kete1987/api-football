package es.com.kete1987.apifootball.library.common.util;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Safe parsing of the {@code YYYY-MM-DD} dates the API sends as text (season start and end, round
 * dates). Unlike {@code QueryValidation.date}, which rejects bad input before a request, this is for
 * responses: it never throws.
 */
public final class DateParsing {

    private DateParsing() {
        // utility class
    }

    /** The date of {@code text} (surrounding whitespace ignored), or {@code null} when it is {@code null} or not a real {@code YYYY-MM-DD} date. */
    public static LocalDate parseIsoDate(String text) {
        if (text == null) return null;
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}

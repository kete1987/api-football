package es.com.kete1987.apifootball.library.common.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Safe parsing of the numeric values the API sends as text ({@code "187"}, {@code "7.11"},
 * {@code "37%"}, {@code "185 cm"}). Surrounding whitespace is ignored. Every method returns
 * {@code null} instead of throwing when the text is {@code null} or does not have the expected
 * form; nothing is rounded or truncated.
 */
public final class NumberParsing {

    private static final Pattern INTEGER = Pattern.compile("[+-]?\\d+");
    private static final Pattern DECIMAL = Pattern.compile("[+-]?(\\d+(\\.\\d*)?|\\.\\d+)([eE][+-]?\\d+)?");
    /** An integer at the start of the text, not followed by a decimal part. */
    private static final Pattern LEADING_INTEGER = Pattern.compile("([+-]?\\d+)(?![\\d.,])");

    private NumberParsing() {
        // utility class
    }

    /**
     * Strict integer: {@code "5"} and {@code "-3"} parse; {@code "5.0"}, {@code "37%"}, values out
     * of the {@code int} range and anything else give {@code null}.
     */
    public static Integer parseInteger(String text) {
        if (text == null) return null;
        String trimmed = text.trim();
        if (!INTEGER.matcher(trimmed).matches()) return null;
        try {
            return Integer.valueOf(trimmed);
        } catch (NumberFormatException e) {
            return null; // out of range
        }
    }

    /**
     * Decimal number: {@code "1.71"}, {@code "26"}, {@code "-1"} parse; {@code "37%"},
     * {@code "NaN"}, {@code "Infinity"} and anything else give {@code null}.
     */
    public static Double parseDouble(String text) {
        if (text == null) return null;
        String trimmed = text.trim();
        if (!DECIMAL.matcher(trimmed).matches()) return null;
        double value = Double.parseDouble(trimmed);
        return Double.isInfinite(value) ? null : value;
    }

    /**
     * Percentage: {@code "37%"} gives {@code 37.0} and {@code "8.97%"} gives {@code 8.97}. A bare
     * number without the sign ({@code "37"}) is accepted too, because the API is not consistent
     * about it.
     */
    public static Double parsePercent(String text) {
        if (text == null) return null;
        String trimmed = text.trim();
        if (trimmed.endsWith("%")) trimmed = trimmed.substring(0, trimmed.length() - 1);
        return parseDouble(trimmed);
    }

    /**
     * Whole number at the start of the text followed by a unit or other text: {@code "185 cm"}
     * gives {@code 185}, {@code "68%"} gives {@code 68}, {@code "187"} gives {@code 187}. A leading
     * number with a decimal part ({@code "185.5 cm"}) or no leading number gives {@code null}.
     */
    public static Integer parseLeadingInteger(String text) {
        if (text == null) return null;
        Matcher m = LEADING_INTEGER.matcher(text.trim());
        if (!m.lookingAt()) return null;
        return parseInteger(m.group(1));
    }
}

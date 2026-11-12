package es.com.kete1987.apifootball.library.fixtures.model;

import es.com.kete1987.apifootball.library.common.util.NumberParsing;

/**
 * One statistic of a team in a fixture. The API's {@code value} is an integer ({@code 5}), a
 * percentage string ({@code "37%"}), a decimal string ({@code "1.71"}) or {@code null}, so it is kept
 * as text; the typed getters never throw and return {@code null} when the value is not numeric.
 */
public class Statistic {
    private String type;
    private String value;

    /** Statistic name; see {@code StatisticTypes}. */
    public String getType() {
        return type;
    }

    /** Raw value as text ({@code "5"}, {@code "37%"}, {@code "1.71"}), or {@code null}. */
    public String getValue() {
        return value;
    }

    /** {@code true} when the text value ends with {@code %}. */
    public boolean isPercentage() {
        return value != null && value.trim().endsWith("%");
    }

    /**
     * Numeric value ({@code "37%"} gives {@code 37.0}, {@code "1.71"} gives {@code 1.71}), or
     * {@code null} when absent or not a number. Never throws.
     */
    public Double getDoubleValue() {
        return isPercentage() ? NumberParsing.parsePercent(value) : NumberParsing.parseDouble(value);
    }

    /**
     * Integer value ({@code "5"} gives {@code 5}, {@code "37%"} gives {@code 37}), or {@code null}
     * when absent, not a number or not written as a whole number (e.g. {@code "1.71"},
     * {@code "8.5%"}). Never throws.
     */
    public Integer getIntValue() {
        return isPercentage() ? NumberParsing.parseLeadingInteger(value) : NumberParsing.parseInteger(value);
    }
}

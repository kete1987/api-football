package es.com.kete1987.apifootball.library.fixtures.model;

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

    /** Numeric value (without any {@code %}), or {@code null} when absent or not a number. */
    public Double getDoubleValue() {
        if (value == null) return null;
        String text = value.trim();
        if (text.endsWith("%")) text = text.substring(0, text.length() - 1).trim();
        try {
            return Double.valueOf(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Integer value (without any {@code %}), or {@code null} when absent, not a number or not a whole
     * number (e.g. {@code "1.71"}).
     */
    public Integer getIntValue() {
        Double d = getDoubleValue();
        if (d == null || d.isNaN() || d.isInfinite() || d != Math.rint(d)
                || d > Integer.MAX_VALUE || d < Integer.MIN_VALUE) {
            return null;
        }
        return d.intValue();
    }
}

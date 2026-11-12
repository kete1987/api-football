package es.com.kete1987.apifootball.library.odds.model;

import es.com.kete1987.apifootball.library.common.util.NumberParsing;

/**
 * One selection of a pre-match bet. Both members are kept as text because the API is loose with
 * types: {@code value} is a JSON number for some bets (e.g. "Exact Goals Number": {@code 0}, {@code 5})
 * and a string otherwise ({@code "Home"}, {@code "more 6"}); {@code odd} is a decimal string
 * ({@code "1.20"}).
 */
public class OddValue {
    private String value;
    private String odd;

    /** The selection label or number, always as text (e.g. {@code "Home"}, {@code "5"}, {@code "Over 2.5"}). */
    public String getValue() {
        return value;
    }

    /** The decimal odd as text, e.g. {@code "1.20"}. */
    public String getOdd() {
        return odd;
    }

    /** The odd as a number, or {@code null} when absent or not parseable. Never throws. */
    public Double getOddAsDouble() {
        return NumberParsing.parseDouble(odd);
    }

    /** The value as an integer, or {@code null} when it is not an integer label. Never throws. */
    public Integer getValueAsInteger() {
        return NumberParsing.parseInteger(value);
    }
}

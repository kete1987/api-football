package es.com.kete1987.apifootball.library.odds.model;

import es.com.kete1987.apifootball.library.common.util.NumberParsing;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One selection of a pre-match bet. Both members are kept as text because the API is loose with
 * types: {@code value} is a JSON number for some bets (e.g. "Exact Goals Number": {@code 0}, {@code 5})
 * and a string otherwise ({@code "Home"}, {@code "more 6"}); {@code odd} is a decimal string
 * ({@code "1.20"}).
 */
public class OddValue {
    private static final Pattern SCORE = Pattern.compile("\\s*(\\d{1,3})\\s*:\\s*(\\d{1,3})\\s*");

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

    /**
     * Home goals of an "Exact Score" selection such as {@code "1:0"}, or {@code null} when the value
     * is not a score. <b>The order is home:away</b>: verified on the real Real Madrid - Celta odds
     * (fixture 1570461, bet id 10), where the home favourite's selections are {@code 1:0}, {@code 2:0},
     * {@code 3:0}... and the Bet365 list has 40 selections. This differs from other providers that
     * give "winner-loser" (a 1-0 away win is {@code 1-0} there but {@code 0:1} here), so never
     * mix the two without converting. Never throws.
     */
    public Integer getExactScoreHome() {
        Matcher m = score();
        return m == null ? null : Integer.valueOf(m.group(1));
    }

    /**
     * Away goals of an "Exact Score" selection such as {@code "1:0"}, or {@code null} when the value
     * is not a score. The order is home:away, see {@link #getExactScoreHome()}. Never throws.
     */
    public Integer getExactScoreAway() {
        Matcher m = score();
        return m == null ? null : Integer.valueOf(m.group(2));
    }

    private Matcher score() {
        if (value == null) return null;
        Matcher m = SCORE.matcher(value);
        return m.matches() ? m : null;
    }
}

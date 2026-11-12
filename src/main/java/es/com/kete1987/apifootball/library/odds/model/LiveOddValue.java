package es.com.kete1987.apifootball.library.odds.model;

import es.com.kete1987.apifootball.library.common.util.NumberParsing;

/** One selection of an in-play bet ({@code /odds/live}). */
public class LiveOddValue {
    private String value;
    private String odd;
    private String handicap;
    private Boolean main;
    private Boolean suspended;

    /** Selection label ({@code "Home"}, {@code "Over"}...), kept as text. */
    public String getValue() {
        return value;
    }

    /** The odd as text, e.g. {@code "2.425"} or {@code "26"}. */
    public String getOdd() {
        return odd;
    }

    /** The odd as a number, or {@code null} when absent or not parseable. Never throws. */
    public Double getOddAsDouble() {
        return NumberParsing.parseDouble(odd);
    }

    /** Handicap/line as text ({@code "1.5"}, {@code "-1"}); {@code null} for bets without one. */
    public String getHandicap() {
        return handicap;
    }

    /** The handicap as a number, or {@code null} when absent or not parseable. Never throws. */
    public Double getHandicapAsDouble() {
        return NumberParsing.parseDouble(handicap);
    }

    /** {@code true} only when several identical values exist and this is the main line; otherwise {@code false} or {@code null}. */
    public Boolean getMain() {
        return main;
    }

    /** Whether this selection is currently suspended (not bettable). */
    public Boolean getSuspended() {
        return suspended;
    }
}

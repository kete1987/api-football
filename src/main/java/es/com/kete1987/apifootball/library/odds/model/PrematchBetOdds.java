package es.com.kete1987.apifootball.library.odds.model;

import es.com.kete1987.apifootball.library.common.util.ModelCollections;

import java.util.List;

/** A pre-match bet with its selections. The id belongs to the <b>pre-match</b> id space ({@code /odds/bets}). */
public class PrematchBetOdds {
    private Integer id;
    private String name;
    private List<OddValue> values;

    /** Pre-match bet id (see {@code PrematchBetIds}); not valid for in-play odds. */
    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<OddValue> getValues() {
        return ModelCollections.unmodifiable(values);
    }

    /**
     * The selection whose {@code value} equals {@code label}, ignoring case and surrounding
     * spaces (e.g. {@code "home"} finds {@code "Home"}), or {@code null} when there is none or
     * {@code label} is {@code null}. Never throws.
     */
    public OddValue findValue(String label) {
        if (label == null || values == null) return null;
        String wanted = label.trim();
        for (OddValue v : values) {
            if (v != null && v.getValue() != null && v.getValue().trim().equalsIgnoreCase(wanted)) return v;
        }
        return null;
    }

    /**
     * Odd of the {@code "Home"} selection as a number; meaningful on the 1X2 market
     * ({@code PrematchBetIds.MATCH_WINNER}). {@code null} when absent or unparseable.
     */
    public Double getHomeOdd() {
        return oddOf("Home");
    }

    /** Odd of the {@code "Draw"} selection of the 1X2 market, {@code null} when absent. */
    public Double getDrawOdd() {
        return oddOf("Draw");
    }

    /** Odd of the {@code "Away"} selection of the 1X2 market, {@code null} when absent. */
    public Double getAwayOdd() {
        return oddOf("Away");
    }

    private Double oddOf(String label) {
        OddValue v = findValue(label);
        return v == null ? null : v.getOddAsDouble();
    }
}

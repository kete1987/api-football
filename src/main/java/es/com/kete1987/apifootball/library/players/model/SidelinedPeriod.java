package es.com.kete1987.apifootball.library.players.model;

/** An injury/suspension period of a player or coach ({@code /sidelined} with a single {@code player}/{@code coach}). */
public class SidelinedPeriod {
    private String type;
    private String start;
    private String end;

    /** {@code "Ankle Injury"}, {@code "Suspended"}, ... */
    public String getType() {
        return type;
    }

    /** Start date, {@code YYYY-MM-DD}. */
    public String getStart() {
        return start;
    }

    /** End date, {@code YYYY-MM-DD}; null while ongoing. */
    public String getEnd() {
        return end;
    }
}

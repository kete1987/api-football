package es.com.kete1987.apifootball.library.leagues.util;

/** Values of the competition type. Exposed as strings because the API may add values. */
public final class LeagueTypes {

    /** League type as it appears in responses ({@code LeagueInfo.getType()}). */
    public static final String LEAGUE = "League";
    /** Cup type as it appears in responses. */
    public static final String CUP = "Cup";

    private LeagueTypes() {
        // constants
    }
}

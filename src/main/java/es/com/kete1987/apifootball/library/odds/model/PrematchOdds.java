package es.com.kete1987.apifootball.library.odds.model;

import es.com.kete1987.apifootball.library.common.util.ModelCollections;

import java.util.List;

/** Pre-match odds of one fixture ({@code /odds}), grouped by bookmaker. */
public class PrematchOdds {
    private OddsLeague league;
    private OddsFixtureRef fixture;
    private String update;
    private List<BookmakerOdds> bookmakers;

    public OddsLeague getLeague() {
        return league;
    }

    public OddsFixtureRef getFixture() {
        return fixture;
    }

    /** ISO-8601 time of the last update of these odds. */
    public String getUpdate() {
        return update;
    }

    public List<BookmakerOdds> getBookmakers() {
        return ModelCollections.unmodifiable(bookmakers);
    }

    /** The odds of the bookmaker with the given id, or {@code null}. */
    public BookmakerOdds findBookmaker(int id) {
        if (bookmakers == null) return null;
        for (BookmakerOdds b : bookmakers) {
            if (b.getId() != null && b.getId() == id) return b;
        }
        return null;
    }
}

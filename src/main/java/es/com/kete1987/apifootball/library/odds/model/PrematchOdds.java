package es.com.kete1987.apifootball.library.odds.model;

import es.com.kete1987.apifootball.library.common.util.ModelCollections;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Pre-match odds of one fixture ({@code /odds}), grouped by bookmaker.
 *
 * <p>Odds are available only "1-14 days before the fixture, 7-day history" (catalog 11.36): a
 * fixture more than 14 days away returns no entry. Prefer requesting by <b>fixture id</b> rather
 * than by date: {@code getFixture().getDate()} can be a provisional value (the 21 Nov 2026
 * Real Madrid - Celta fixture appeared as 22 Nov 16:00 and was not returned for its real date).
 */
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

    /**
     * ISO-8601 time of the last update of these odds, e.g. {@code 2026-11-08T16:51:14+00:00}.
     * It is <b>one timestamp per fixture, not per bookmaker or selection</b>, and can be days old
     * (the Real Madrid - Celta example was 5 days old when fetched).
     */
    public String getUpdate() {
        return update;
    }

    /** {@link #getUpdate()} as an {@link Instant}, or {@code null} when absent or not ISO-8601. Never throws. */
    public Instant getUpdateAsInstant() {
        if (update == null) return null;
        try {
            return OffsetDateTime.parse(update.trim()).toInstant();
        } catch (DateTimeParseException e) {
            return null;
        }
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

package es.com.kete1987.apifootball.library.odds.query;

import es.com.kete1987.apifootball.library.common.query.Query;
import es.com.kete1987.apifootball.library.common.query.QueryValidation;

/** Parameters of {@code GET /odds/bookmakers}. All optional. */
public class BookmakersQuery extends Query<BookmakersQuery> {

    /** Bookmaker id. */
    public BookmakersQuery id(int id) {
        return param("id", id);
    }

    /** Bookmaker name (min 3 characters, e.g. {@code Betfair}). Letters A-Z, digits and spaces only; throws {@link IllegalArgumentException} otherwise (see {@link QueryValidation#toSearchTerm}); {@code null} removes it. */
    public BookmakersQuery search(String search) {
        return param("search", search == null ? null : QueryValidation.search(search));
    }
}

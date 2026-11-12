package es.com.kete1987.apifootball.library.odds.query;

import es.com.kete1987.apifootball.library.common.query.Query;

/** Parameters of {@code GET /odds/bookmakers}. All optional. */
public class BookmakersQuery extends Query<BookmakersQuery> {

    /** Bookmaker id. */
    public BookmakersQuery id(int id) {
        return param("id", id);
    }

    /** Bookmaker name (min 3 characters, e.g. {@code Betfair}). */
    public BookmakersQuery search(String search) {
        return param("search", search);
    }
}

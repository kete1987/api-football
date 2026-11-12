package es.com.kete1987.apifootball.library.odds.query;

import es.com.kete1987.apifootball.library.common.query.Query;

/** Parameters of {@code GET /odds/live/bets} (in-play bet catalogue, a different id space from pre-match). All optional. */
public class LiveBetsQuery extends Query<LiveBetsQuery> {

    /** In-play bet id. */
    public LiveBetsQuery id(int id) {
        return param("id", id);
    }

    /** In-play bet id (the API types it as a string). */
    public LiveBetsQuery id(String id) {
        return param("id", id);
    }

    /** Bet name (min 3 characters, e.g. {@code winner}). */
    public LiveBetsQuery search(String search) {
        return param("search", search);
    }
}

package es.com.kete1987.apifootball.library.odds.query;

import es.com.kete1987.apifootball.library.common.query.Query;

/** Parameters of {@code GET /odds/bets} (pre-match bet catalogue). All optional. */
public class PrematchBetsQuery extends Query<PrematchBetsQuery> {

    /** Pre-match bet id. */
    public PrematchBetsQuery id(int id) {
        return param("id", id);
    }

    /** Pre-match bet id (the API types it as a string). */
    public PrematchBetsQuery id(String id) {
        return param("id", id);
    }

    /** Bet name (min 3 characters). */
    public PrematchBetsQuery search(String search) {
        return param("search", search);
    }
}

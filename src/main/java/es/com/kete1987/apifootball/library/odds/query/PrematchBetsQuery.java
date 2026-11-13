package es.com.kete1987.apifootball.library.odds.query;

import es.com.kete1987.apifootball.library.common.query.Query;
import es.com.kete1987.apifootball.library.common.query.QueryValidation;

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

    /** Bet name (min 3 characters). Letters A-Z, digits and spaces only; throws {@link IllegalArgumentException} otherwise (see {@link QueryValidation#toSearchTerm}); {@code null} removes it. */
    public PrematchBetsQuery search(String search) {
        return param("search", search == null ? null : QueryValidation.search(search));
    }
}

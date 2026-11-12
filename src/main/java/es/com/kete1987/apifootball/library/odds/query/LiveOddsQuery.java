package es.com.kete1987.apifootball.library.odds.query;

import es.com.kete1987.apifootball.library.common.query.Query;

/** Parameters of {@code GET /odds/live} (in-play odds). All optional. */
public class LiveOddsQuery extends Query<LiveOddsQuery> {

    /** Fixture id. */
    public LiveOddsQuery fixture(int fixture) {
        return param("fixture", fixture);
    }

    /** League id (no season needed). */
    public LiveOddsQuery league(int league) {
        return param("league", league);
    }

    /** <b>In-play</b> bet id (see {@code /odds/live/bets}); NOT a pre-match bet id. */
    public LiveOddsQuery bet(int bet) {
        return param("bet", bet);
    }
}

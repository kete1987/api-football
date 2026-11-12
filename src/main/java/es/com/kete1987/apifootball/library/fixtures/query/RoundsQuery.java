package es.com.kete1987.apifootball.library.fixtures.query;

import es.com.kete1987.apifootball.library.common.query.Query;
import es.com.kete1987.apifootball.library.common.query.QueryValidation;

/**
 * Optional filters of {@code GET /fixtures/rounds}. The {@code dates} flag is not here: it changes
 * the shape of the response, so it is selected by the API method
 * ({@code getRounds} vs {@code getRoundsWithDates}).
 */
public class RoundsQuery extends Query<RoundsQuery> {

    /** {@code current=true} returns only the current round; {@code false} all of them. */
    public RoundsQuery current(boolean current) {
        return param("current", current);
    }

    /**
     * Timezone such as {@code "Europe/Madrid"} (a name returned by {@code /timezone}); affects the
     * dates of {@code getRoundsWithDates}. Checked with {@link QueryValidation#timezone(String)};
     * {@code null} removes it.
     */
    public RoundsQuery timezone(String timezone) {
        return param("timezone", timezone == null ? null : QueryValidation.timezone(timezone));
    }
}

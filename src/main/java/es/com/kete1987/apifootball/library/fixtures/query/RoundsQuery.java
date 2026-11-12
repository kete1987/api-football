package es.com.kete1987.apifootball.library.fixtures.query;

import es.com.kete1987.apifootball.library.common.query.Query;

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

    /** IANA timezone such as {@code "Europe/Madrid"}; affects the dates of {@code getRoundsWithDates}. */
    public RoundsQuery timezone(String timezone) {
        return param("timezone", QueryValidation.timezone(timezone));
    }
}

package es.com.kete1987.apifootball.library.teams.query;

import es.com.kete1987.apifootball.library.common.query.Query;
import es.com.kete1987.apifootball.library.common.query.QueryValidation;

/** Parameters of {@code /coachs}. At least one should be set; all are combinable. */
public class CoachesQuery extends Query<CoachesQuery> {

    /** Coach id. */
    public CoachesQuery id(int id) {
        return param("id", id);
    }

    /** Team id: current and past coaches of that team. */
    public CoachesQuery team(int team) {
        return param("team", team);
    }

    /** Free text matched against the coach name; at least 3 characters. Letters A-Z, digits and spaces only; throws {@link IllegalArgumentException} otherwise (see {@link QueryValidation#toSearchTerm}); {@code null} removes it. */
    public CoachesQuery search(String search) {
        return param("search", search == null ? null : QueryValidation.search(search));
    }
}

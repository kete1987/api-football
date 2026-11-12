package es.com.kete1987.apifootball.library.teams.query;

import es.com.kete1987.apifootball.library.common.query.Query;

/** Parameters of {@code /teams}. At least one must be set; all are combinable. */
public class TeamsQuery extends Query<TeamsQuery> {

    /** Team id. */
    public TeamsQuery id(int id) {
        return param("id", id);
    }

    /** Team name. */
    public TeamsQuery name(String name) {
        return param("name", name);
    }

    /** League id (use together with {@link #season(int)}). */
    public TeamsQuery league(int league) {
        return param("league", league);
    }

    /** Season, four-digit year. */
    public TeamsQuery season(int season) {
        return param("season", season);
    }

    /** Country name. */
    public TeamsQuery country(String country) {
        return param("country", country);
    }

    /** Three-letter team code, e.g. {@code "FRA"}. */
    public TeamsQuery code(String code) {
        return param("code", code);
    }

    /** Venue id. */
    public TeamsQuery venue(int venue) {
        return param("venue", venue);
    }

    /** Free text matched against team name or country; at least 3 characters. */
    public TeamsQuery search(String search) {
        return param("search", search);
    }
}

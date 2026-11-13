package es.com.kete1987.apifootball.library.leagues.query;

import es.com.kete1987.apifootball.library.common.query.Query;
import es.com.kete1987.apifootball.library.common.query.QueryValidation;

import java.util.Locale;

/** Parameters of {@code /leagues}. All are optional and combinable. */
public class LeaguesQuery extends Query<LeaguesQuery> {

    /** League id. */
    public LeaguesQuery id(int id) {
        return param("id", id);
    }

    /** League name. */
    public LeaguesQuery name(String name) {
        return param("name", name);
    }

    /** Country name, e.g. {@code "Spain"}. */
    public LeaguesQuery country(String country) {
        return param("country", country);
    }

    /** Country alpha code (2 to 6 characters), e.g. {@code "FR"} or {@code "GB-ENG"}. */
    public LeaguesQuery code(String code) {
        return param("code", code);
    }

    /** Season, four-digit year (e.g. 2025). */
    public LeaguesQuery season(int season) {
        return param("season", season);
    }

    /** Team id: the leagues that team plays or played in. */
    public LeaguesQuery team(int team) {
        return param("team", team);
    }

    /** Competition type: {@code "league"} or {@code "cup"} (case-insensitive, sent in lower case). */
    public LeaguesQuery type(String type) {
        return param("type", type == null ? null : type.toLowerCase(Locale.ROOT));
    }

    /** {@code true}: only the active (or last) season of each competition; {@code false}: the rest. */
    public LeaguesQuery current(boolean current) {
        return param("current", current);
    }

    /** Free text matched against league name or country; at least 3 characters. Letters A-Z, digits and spaces only; throws {@link IllegalArgumentException} otherwise (see {@link QueryValidation#toSearchTerm}); {@code null} removes it. */
    public LeaguesQuery search(String search) {
        return param("search", search == null ? null : QueryValidation.search(search));
    }

    /**
     * The {@code last} leagues/cups added to the API; at most 2 digits (1 to 99).
     *
     * @throws IllegalArgumentException when outside 1-99
     */
    public LeaguesQuery last(int last) {
        return param("last", QueryValidation.range("last", last, 1, 99));
    }
}

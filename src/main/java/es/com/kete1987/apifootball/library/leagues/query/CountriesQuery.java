package es.com.kete1987.apifootball.library.leagues.query;

import es.com.kete1987.apifootball.library.common.query.Query;
import es.com.kete1987.apifootball.library.common.query.QueryValidation;

/** Parameters of {@code /countries}. All are optional and combinable. */
public class CountriesQuery extends Query<CountriesQuery> {

    /** Country name, e.g. {@code "england"} (case-insensitive). */
    public CountriesQuery name(String name) {
        return param("name", name);
    }

    /** Alpha code (2 to 6 characters), e.g. {@code "FR"}, {@code "GB-ENG"}. */
    public CountriesQuery code(String code) {
        return param("code", code);
    }

    /** Free text; at least 3 characters. Letters A-Z, digits and spaces only; throws {@link IllegalArgumentException} otherwise (see {@link QueryValidation#toSearchTerm}); {@code null} removes it. */
    public CountriesQuery search(String search) {
        return param("search", search == null ? null : QueryValidation.search(search));
    }
}

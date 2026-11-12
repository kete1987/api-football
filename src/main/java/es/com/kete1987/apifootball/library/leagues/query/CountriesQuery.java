package es.com.kete1987.apifootball.library.leagues.query;

import es.com.kete1987.apifootball.library.common.query.Query;

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

    /** Free text; at least 3 characters. */
    public CountriesQuery search(String search) {
        return param("search", search);
    }
}

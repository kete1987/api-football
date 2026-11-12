package es.com.kete1987.apifootball.library.teams.query;

import es.com.kete1987.apifootball.library.common.query.Query;

/** Parameters of {@code /venues}. At least one must be set; all are combinable. */
public class VenuesQuery extends Query<VenuesQuery> {

    /** Venue id. */
    public VenuesQuery id(int id) {
        return param("id", id);
    }

    /** Venue name. */
    public VenuesQuery name(String name) {
        return param("name", name);
    }

    /** City name. */
    public VenuesQuery city(String city) {
        return param("city", city);
    }

    /** Country name. */
    public VenuesQuery country(String country) {
        return param("country", country);
    }

    /** Free text matched against name, city or country; at least 3 characters. */
    public VenuesQuery search(String search) {
        return param("search", search);
    }
}

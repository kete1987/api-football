package es.com.kete1987.apifootball.library.fixtures.model;

/** Venue of a fixture. The {@code id} may be {@code null} <em>or {@code 0}</em> with a valid name. */
public class Venue {
    private Integer id;
    private String name;
    private String city;

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCity() {
        return city;
    }
}

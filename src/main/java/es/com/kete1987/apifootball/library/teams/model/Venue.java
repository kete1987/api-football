package es.com.kete1987.apifootball.library.teams.model;

/**
 * A venue (stadium). Returned by {@code /venues} and as the home venue of {@code /teams}; every
 * field may be {@code null} when the venue is unknown.
 */
public class Venue {
    private Integer id;
    private String name;
    private String address;
    private String city;
    private String country;
    private Integer capacity;
    private String surface;
    private String image;

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getCity() {
        return city;
    }

    /** Country name. Only present in {@code /venues} responses; {@code null} inside {@code /teams}. */
    public String getCountry() {
        return country;
    }

    public Integer getCapacity() {
        return capacity;
    }

    /** Free text, e.g. {@code "grass"}, {@code "artificial turf"}. */
    public String getSurface() {
        return surface;
    }

    public String getImage() {
        return image;
    }
}

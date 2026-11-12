package es.com.kete1987.apifootball.library.fixtures.model;

/** Reference to a player in an event ({@code player} / {@code assist}); both members may be {@code null}. */
public class PlayerRef {
    private Integer id;
    private String name;

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}

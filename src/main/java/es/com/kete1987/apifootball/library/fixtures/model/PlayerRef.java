package es.com.kete1987.apifootball.library.fixtures.model;

import es.com.kete1987.apifootball.library.common.util.MediaUrls;

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

    /**
     * Photo URL built from the id ({@code https://media.api-sports.io/football/players/{id}.png}):
     * events carry the player id but no photo. {@code null} without an id (the first polls after a live
     * event). See {@link MediaUrls}.
     */
    public String getPhotoUrl() {
        return MediaUrls.playerPhoto(id);
    }
}

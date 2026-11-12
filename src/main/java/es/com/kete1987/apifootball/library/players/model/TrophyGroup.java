package es.com.kete1987.apifootball.library.players.model;

import es.com.kete1987.apifootball.library.common.util.ModelCollections;

import java.util.List;

/** Element of {@code /trophies} when queried with {@code players} / {@code coachs}: id plus its trophies. */
public class TrophyGroup {
    private Integer id;
    private List<Trophy> trophies;

    /** Player or coach id. */
    public Integer getId() {
        return id;
    }

    public List<Trophy> getTrophies() {
        return ModelCollections.unmodifiable(trophies);
    }
}

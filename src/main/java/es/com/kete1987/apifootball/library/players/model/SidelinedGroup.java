package es.com.kete1987.apifootball.library.players.model;

import es.com.kete1987.apifootball.library.common.util.ModelCollections;

import java.util.List;

/** Element of {@code /sidelined} when queried with {@code players} / {@code coachs}: id plus its periods. */
public class SidelinedGroup {
    private Integer id;
    private List<SidelinedPeriod> sidelined;

    /** Player or coach id. */
    public Integer getId() {
        return id;
    }

    public List<SidelinedPeriod> getSidelined() {
        return ModelCollections.unmodifiable(sidelined);
    }
}

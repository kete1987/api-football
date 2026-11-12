package es.com.kete1987.apifootball.library.leagues.model;

import es.com.kete1987.apifootball.library.common.util.ModelCollections;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The ranking tables of a league in a season (the {@code league} object of a {@code /standings}
 * response item).
 *
 * <p>The API nests the rows as an array of arrays: one inner array per table. Most leagues have a
 * single table; tournaments with groups have one per group (and sometimes an extra overall table).
 */
public class Standings {
    private Integer id;
    private String name;
    private String country;
    private String logo;
    private String flag;
    private Integer season;
    private List<List<StandingEntry>> standings;

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /** Country name; {@code "World"} for international competitions. */
    public String getCountry() {
        return country;
    }

    public String getLogo() {
        return logo;
    }

    /** Country flag URL; {@code null} for "World". */
    public String getFlag() {
        return flag;
    }

    public Integer getSeason() {
        return season;
    }

    /**
     * Every table, exactly as the API nests them: one inner list per table or group; an empty list
     * when there are none.
     */
    public List<List<StandingEntry>> getTables() {
        if (standings == null) return Collections.emptyList();
        List<List<StandingEntry>> copy = new ArrayList<>(standings.size());
        for (List<StandingEntry> table : standings) {
            copy.add(ModelCollections.unmodifiable(table));
        }
        return Collections.unmodifiableList(copy);
    }

    /** The first table (the only one in most leagues); an empty list when there are none. */
    public List<StandingEntry> getTable() {
        if (standings == null || standings.isEmpty()) return Collections.emptyList();
        return ModelCollections.unmodifiable(standings.get(0));
    }

    /**
     * The table whose rows carry the given {@code group} label (e.g. {@code "Group A"}), or an
     * empty list when there is none.
     */
    public List<StandingEntry> getTable(String group) {
        if (standings != null && group != null) {
            for (List<StandingEntry> table : standings) {
                if (table != null && !table.isEmpty() && group.equals(table.get(0).getGroup())) {
                    return Collections.unmodifiableList(table);
                }
            }
        }
        return Collections.emptyList();
    }
}

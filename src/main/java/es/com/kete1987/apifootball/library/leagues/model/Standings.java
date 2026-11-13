package es.com.kete1987.apifootball.library.leagues.model;

import es.com.kete1987.apifootball.library.common.util.ModelCollections;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The ranking tables of a league in a season (the {@code league} object of a {@code /standings}
 * response item).
 *
 * <p>The API nests the rows as an array of arrays: one inner array per table. Most leagues have a
 * single table; tournaments with groups have one per group, and sometimes an extra table that ranks
 * teams across the groups: the World Cup 2026 response has 13 tables, {@code "Group A"} to
 * {@code "Group L"} and a 13th labelled {@code "Group Stage"} that ranks the 12 third-placed teams
 * (each of them also in its group table). {@link #getTables()} returns them all;
 * {@link #getGroupTables()} and {@link #getCrossGroupRankings()} separate them.
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

    /**
     * The real tables: every table except the cross-group rankings (see
     * {@link #getCrossGroupRankings()}), in API order. One table for a league (LaLiga) or a single
     * league phase (Champions League 2025-26, 36 rows); twelve for the World Cup 2026 groups. Empty
     * when there are none. Never throws.
     */
    public List<List<StandingEntry>> getGroupTables() {
        return select(false);
    }

    /**
     * Tables that rank teams already present in other tables, drawn from at least two of them: the
     * World Cup 2026 ranking of third-placed teams ({@code "Group Stage"}, 12 rows, one per group).
     * Rule: a table is a cross-group ranking when every row's team id also appears in another table
     * and those teams come from two or more different tables. A group table is never one (its teams
     * repeat in at most the ranking), and a single-table response has none. Rows without a team id
     * make a table a group table. Empty when there are none. Never throws.
     */
    public List<List<StandingEntry>> getCrossGroupRankings() {
        return select(true);
    }

    /** The first of {@link #getCrossGroupRankings()}, or an empty list when there is none. */
    public List<StandingEntry> getCrossGroupRanking() {
        List<List<StandingEntry>> rankings = getCrossGroupRankings();
        return rankings.isEmpty() ? Collections.<StandingEntry>emptyList() : rankings.get(0);
    }

    /**
     * The {@code group} label of the group table holding {@code teamId} (e.g. {@code "Group H"}),
     * ignoring cross-group rankings; {@code null} when no group table has the team. API-Football
     * fixtures do not say their group: two teams in the same group table played a group match.
     */
    public String getGroupOf(int teamId) {
        for (List<StandingEntry> table : getGroupTables()) {
            if (table == null) continue;
            for (StandingEntry row : table) {
                if (row != null && row.getTeam() != null && row.getTeam().getId() != null
                        && row.getTeam().getId() == teamId) {
                    return row.getGroup();
                }
            }
        }
        return null;
    }

    private List<List<StandingEntry>> select(boolean rankings) {
        List<List<StandingEntry>> tables = getTables();
        List<List<StandingEntry>> result = new ArrayList<>();
        for (int i = 0; i < tables.size(); i++) {
            if (isCrossGroupRanking(tables, i) == rankings) result.add(tables.get(i));
        }
        return Collections.unmodifiableList(result);
    }

    private static boolean isCrossGroupRanking(List<List<StandingEntry>> tables, int index) {
        List<StandingEntry> table = tables.get(index);
        if (tables.size() < 2 || table == null || table.isEmpty()) return false;
        Set<Integer> sources = new HashSet<>();
        for (StandingEntry row : table) {
            Integer id = row == null || row.getTeam() == null ? null : row.getTeam().getId();
            if (id == null) return false;
            int source = -1;
            for (int other = 0; other < tables.size() && source < 0; other++) {
                if (other != index && containsTeam(tables.get(other), id)) source = other;
            }
            if (source < 0) return false;
            sources.add(source);
        }
        return sources.size() >= 2;
    }

    private static boolean containsTeam(List<StandingEntry> table, int teamId) {
        if (table == null) return false;
        for (StandingEntry row : table) {
            if (row != null && row.getTeam() != null && row.getTeam().getId() != null
                    && row.getTeam().getId() == teamId) {
                return true;
            }
        }
        return false;
    }
}

package es.com.kete1987.apifootball.library.competition.util;

import es.com.kete1987.apifootball.library.fixtures.model.Fixture;
import es.com.kete1987.apifootball.library.leagues.model.StandingEntry;
import es.com.kete1987.apifootball.library.leagues.model.Standings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Rebuilds the group of a group-stage fixture. API-Football fixtures carry no group: the round is
 * {@code "Group Stage - 2"} (a matchday shared by every group) and the group only exists in
 * {@code /standings}, as the {@code group} label of each row. A fixture belongs to a group when both
 * of its teams are in that group's table ({@link Standings#getGroupTables()}, so the World Cup
 * ranking of third-placed teams is never taken for a group). Pure computation, no request; verified
 * on the 72 group matches of the World Cup 2026.
 */
public final class Groups {

    private Groups() {
        // helpers only
    }

    /**
     * The {@code group} label (e.g. {@code "Group A"}) of the group table holding both teams of
     * {@code fixture}, or {@code null} when the teams are in different tables or not in any (a
     * knockout match), or when an argument or a team id is missing. Never throws.
     */
    public static String groupOf(Standings standings, Fixture fixture) {
        if (standings == null || fixture == null || fixture.getTeams() == null
                || fixture.getTeams().getHome() == null || fixture.getTeams().getAway() == null) {
            return null;
        }
        Integer home = fixture.getTeams().getHome().getId();
        Integer away = fixture.getTeams().getAway().getId();
        if (home == null || away == null) return null;
        String homeGroup = standings.getGroupOf(home);
        return homeGroup != null && homeGroup.equals(standings.getGroupOf(away)) ? homeGroup : null;
    }

    /**
     * Fixtures grouped by {@link #groupOf(Standings, Fixture)}, keys in the order of the group
     * tables; each list keeps the input order. Fixtures without a group (knockout matches) are left
     * out. Never throws; {@code null} arguments give an empty map.
     */
    public static Map<String, List<Fixture>> byGroup(Standings standings, Collection<Fixture> fixtures) {
        Map<String, List<Fixture>> grouped = new LinkedHashMap<>();
        if (standings == null || fixtures == null) return Collections.emptyMap();
        for (List<StandingEntry> table : standings.getGroupTables()) {
            if (table != null && !table.isEmpty() && table.get(0) != null && table.get(0).getGroup() != null) {
                grouped.putIfAbsent(table.get(0).getGroup(), new ArrayList<>());
            }
        }
        for (Fixture fixture : fixtures) {
            String group = groupOf(standings, fixture);
            if (group != null) grouped.computeIfAbsent(group, k -> new ArrayList<>()).add(fixture);
        }
        Map<String, List<Fixture>> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<Fixture>> entry : grouped.entrySet()) {
            if (!entry.getValue().isEmpty()) result.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
        }
        return Collections.unmodifiableMap(result);
    }
}

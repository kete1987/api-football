package es.com.kete1987.apifootball.library.fixtures.model;

import com.google.gson.annotations.SerializedName;
import es.com.kete1987.apifootball.library.common.model.TeamRef;
import es.com.kete1987.apifootball.library.common.util.ModelCollections;

import java.util.List;

/** Statistics of one team in a fixture (one element per team). */
public class FixtureStatistics {
    private TeamRef team;
    private List<Statistic> statistics;
    @SerializedName("statistics_1h")
    private List<Statistic> statistics1h;
    @SerializedName("statistics_2h")
    private List<Statistic> statistics2h;

    public TeamRef getTeam() {
        return team;
    }

    /** Full-match statistics. */
    public List<Statistic> getStatistics() {
        return ModelCollections.unmodifiable(statistics);
    }

    /** First-half statistics; only present with {@code half=true}, otherwise {@code null}. */
    public List<Statistic> getFirstHalfStatistics() {
        return ModelCollections.unmodifiable(statistics1h);
    }

    /** Second-half statistics; only present with {@code half=true}, otherwise {@code null}. */
    public List<Statistic> getSecondHalfStatistics() {
        return ModelCollections.unmodifiable(statistics2h);
    }

    /** Finds a full-match statistic by its type name (case-insensitive), or {@code null}. */
    public Statistic find(String type) {
        return find(statistics, type);
    }

    /** Finds a first-half statistic by type name (case-insensitive), or {@code null}. */
    public Statistic findFirstHalf(String type) {
        return find(statistics1h, type);
    }

    /** Finds a second-half statistic by type name (case-insensitive), or {@code null}. */
    public Statistic findSecondHalf(String type) {
        return find(statistics2h, type);
    }

    /**
     * Integer value of a full-match statistic by type name (case-insensitive, see
     * {@code StatisticTypes}), or {@code defaultValue} when the statistic is absent, its value is
     * {@code null} or it is not written as a whole number. {@code "37%"} gives 37; the API sends
     * {@code null} for statistics it does not have (e.g. a team without a red card), which is often a
     * 0 in practice: pass {@code 0} as the default when that is what you want. Never throws.
     */
    public int getIntValue(String type, int defaultValue) {
        Statistic s = find(type);
        Integer v = s == null ? null : s.getIntValue();
        return v == null ? defaultValue : v;
    }

    /** Like {@link #getIntValue(String, int)} but returns {@code null} instead of a default. */
    public Integer getIntValue(String type) {
        Statistic s = find(type);
        return s == null ? null : s.getIntValue();
    }

    private static Statistic find(List<Statistic> list, String type) {
        if (list == null || type == null) return null;
        for (Statistic s : list) {
            if (type.equalsIgnoreCase(s.getType())) return s;
        }
        return null;
    }
}

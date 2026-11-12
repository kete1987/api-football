package es.com.kete1987.apifootball.library.fixtures.model;

import com.google.gson.annotations.SerializedName;
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

    private static Statistic find(List<Statistic> list, String type) {
        if (list == null || type == null) return null;
        for (Statistic s : list) {
            if (type.equalsIgnoreCase(s.getType())) return s;
        }
        return null;
    }
}

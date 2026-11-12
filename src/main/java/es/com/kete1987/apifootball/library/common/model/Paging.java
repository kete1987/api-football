package es.com.kete1987.apifootball.library.common.model;

/** The {@code paging} member of every API-Football response. */
public class Paging {
    private int current;
    private int total;

    public Paging() {
    }

    public Paging(int current, int total) {
        this.current = current;
        this.total = total;
    }

    /** Page this response corresponds to (1-based). */
    public int getCurrent() {
        return current;
    }

    /** Total number of pages available for the request. */
    public int getTotal() {
        return total;
    }

    /** True when at least one more page exists after this one. */
    public boolean hasMore() {
        return current < total;
    }
}

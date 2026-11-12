package es.com.kete1987.apifootball.library.common.model;

import es.com.kete1987.apifootball.library.common.util.ModelCollections;

import java.util.List;

/**
 * One page of a paginated endpoint: its items plus the paging information needed to decide
 * whether (and how many) further requests are worth spending.
 *
 * @param <T> element type
 */
public class Page<T> {
    private final List<T> items;
    private final Paging paging;
    private final int results;

    public Page(List<T> items, Paging paging, int results) {
        this.items = items;
        this.paging = paging;
        this.results = results;
    }

    /** Items of this page (never null). */
    public List<T> getItems() {
        return ModelCollections.unmodifiable(items);
    }

    public Paging getPaging() {
        return paging;
    }

    /** The {@code results} counter of the response (number of items in this page). */
    public int getResults() {
        return results;
    }

    /** True when at least one more page exists after this one. */
    public boolean hasMore() {
        return paging != null && paging.hasMore();
    }
}

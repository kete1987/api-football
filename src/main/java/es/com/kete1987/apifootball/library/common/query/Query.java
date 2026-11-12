package es.com.kete1987.apifootball.library.common.query;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Base class for the fluent parameter builders of endpoints with many optional filters
 * (e.g. {@code new FixturesQuery().league(140).season(2025).round("Regular Season - 1")}).
 *
 * <p>Only parameters that were explicitly set are sent. Subclasses add one typed setter per
 * parameter documented for their endpoint; {@link #param(String, Object)} is public as an escape
 * hatch for parameters the API may add before the library catches up.
 *
 * @param <Q> the concrete query type, so setters can be chained
 */
public abstract class Query<Q extends Query<Q>> {
    private final Map<String, String> params = new LinkedHashMap<>();

    /** Sets (or, with a {@code null} value, removes) a raw query parameter. */
    public Q param(String name, Object value) {
        if (value == null) {
            params.remove(name);
        } else {
            params.put(name, String.valueOf(value));
        }
        return self();
    }

    /** Parameters set so far, in insertion order. */
    public Map<String, String> toParams() {
        return Collections.unmodifiableMap(params);
    }

    @SuppressWarnings("unchecked")
    protected Q self() {
        return (Q) this;
    }
}

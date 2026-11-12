package es.com.kete1987.apifootball.library.common.util;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Helpers for safely exposing internal collection fields from model classes, so callers cannot
 * mutate the state Gson populated.
 */
public final class ModelCollections {

    private ModelCollections() {
        // utility class
    }

    /** Returns an unmodifiable view of the list, or {@code null} if the API field was absent. */
    public static <T> List<T> unmodifiable(List<T> list) {
        return list == null ? null : Collections.unmodifiableList(list);
    }

    /** Returns an unmodifiable view of the map, or {@code null} if the API field was absent. */
    public static <K, V> Map<K, V> unmodifiable(Map<K, V> map) {
        return map == null ? null : Collections.unmodifiableMap(map);
    }
}

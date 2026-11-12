package es.com.kete1987.apifootball.library.common.util;

import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

/**
 * Gson adapters for {@code Integer} and {@code Double} model fields that tolerate the way
 * API-Football mixes JSON types: the same member may arrive as a number, as a numeric string
 * ({@code "187"}) or as an empty string. Empty strings and {@code null} map to {@code null};
 * a non-numeric string is still a {@link JsonSyntaxException}, because silently dropping it would
 * hide data — such fields must be modelled as {@code String} instead.
 */
public final class LenientNumberAdapters {

    private LenientNumberAdapters() {
        // utility class
    }

    public static final TypeAdapter<Integer> INTEGER = new TypeAdapter<Integer>() {
        @Override
        public void write(JsonWriter out, Integer value) throws IOException {
            out.value(value);
        }

        @Override
        public Integer read(JsonReader in) throws IOException {
            String raw = nextScalar(in);
            if (raw == null) return null;
            try {
                return Integer.valueOf(raw);
            } catch (NumberFormatException e) {
                try {
                    double asDouble = Double.parseDouble(raw);
                    if (asDouble == Math.rint(asDouble)) return (int) asDouble;
                } catch (NumberFormatException ignored) {
                    // fall through to the syntax error below
                }
                throw new JsonSyntaxException("Expected an integer but was \"" + raw + "\" at " + in.getPath(), e);
            }
        }
    };

    public static final TypeAdapter<Double> DOUBLE = new TypeAdapter<Double>() {
        @Override
        public void write(JsonWriter out, Double value) throws IOException {
            out.value(value);
        }

        @Override
        public Double read(JsonReader in) throws IOException {
            String raw = nextScalar(in);
            if (raw == null) return null;
            try {
                return Double.valueOf(raw);
            } catch (NumberFormatException e) {
                throw new JsonSyntaxException("Expected a number but was \"" + raw + "\" at " + in.getPath(), e);
            }
        }
    };

    /** Reads a number or string token as trimmed text; {@code null} for JSON null or blank. */
    private static String nextScalar(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }
        String raw = in.nextString().trim();
        return raw.isEmpty() ? null : raw;
    }
}

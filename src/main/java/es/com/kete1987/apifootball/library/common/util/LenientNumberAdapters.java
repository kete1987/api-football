package es.com.kete1987.apifootball.library.common.util;

import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * Gson adapters for {@code Integer}, {@code Long} and {@code Double} model fields that tolerate the
 * way API-Football mixes JSON types: the same member may arrive as a number, as a numeric string
 * ({@code "187"}) or as an empty string. Empty strings and {@code null} map to {@code null}.
 *
 * <p>A non-numeric string, a fractional value for an integer field ({@code 1.5}) or a value out of
 * the range of the Java type is a {@link JsonSyntaxException}: silently dropping or saturating it
 * would hide data, so such fields must be modelled differently (e.g. as {@code String}).
 */
public final class LenientNumberAdapters {

    private LenientNumberAdapters() {
        // utility class
    }

    /** {@code Integer}: whole numbers within the {@code int} range ({@code 5}, {@code "5"}, {@code 5.0}). */
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
                return wholeNumber(raw).intValueExact();
            } catch (ArithmeticException e) {
                throw new JsonSyntaxException("Expected an int but was \"" + raw + "\" at " + in.getPath(), e);
            }
        }
    };

    /** {@code Long}: whole numbers within the {@code long} range (e.g. Unix timestamps). */
    public static final TypeAdapter<Long> LONG = new TypeAdapter<Long>() {
        @Override
        public void write(JsonWriter out, Long value) throws IOException {
            out.value(value);
        }

        @Override
        public Long read(JsonReader in) throws IOException {
            String raw = nextScalar(in);
            if (raw == null) return null;
            try {
                return wholeNumber(raw).longValueExact();
            } catch (ArithmeticException e) {
                throw new JsonSyntaxException("Expected a long but was \"" + raw + "\" at " + in.getPath(), e);
            }
        }
    };

    /** {@code Double}: any number or numeric string. */
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

    /** Parses a decimal text; the caller converts it exactly (fractions and overflow fail). */
    private static BigDecimal wholeNumber(String raw) {
        BigDecimal value;
        try {
            value = new BigDecimal(raw);
        } catch (NumberFormatException e) {
            throw new JsonSyntaxException("Expected an integer but was \"" + raw + "\"", e);
        }
        // Cheap guard before the exact conversion: a huge exponent (1e999999) is out of range anyway.
        if (value.precision() - value.scale() > 20) {
            throw new ArithmeticException("out of range");
        }
        return value;
    }
}

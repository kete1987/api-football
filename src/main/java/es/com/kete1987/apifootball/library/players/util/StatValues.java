package es.com.kete1987.apifootball.library.players.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Safe parsing of the text-typed numeric values of the API ("187", "185 cm", "7.11", "37%").
 * Every method returns {@code null} instead of throwing when the text carries no number.
 */
public final class StatValues {

    private static final Pattern NUMBER = Pattern.compile("^\\s*(-?\\d+(?:[.,]\\d+)?)");

    private StatValues() {
        // utility class
    }

    /** Leading number of {@code text} as a double ("7.11" -> 7.11, "37%" -> 37.0, "185 cm" -> 185.0), or null. */
    public static Double parseDouble(String text) {
        if (text == null) return null;
        Matcher m = NUMBER.matcher(text);
        if (!m.find()) return null;
        try {
            return Double.valueOf(m.group(1).replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Leading number of {@code text} truncated to an integer ("185 cm" -> 185), or null. */
    public static Integer parseInteger(String text) {
        Double value = parseDouble(text);
        return value == null ? null : Integer.valueOf((int) Math.floor(value));
    }
}

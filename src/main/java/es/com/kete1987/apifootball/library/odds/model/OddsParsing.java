package es.com.kete1987.apifootball.library.odds.model;

/** Null-safe text-to-number helpers shared by the odds models. */
final class OddsParsing {
    private OddsParsing() {
    }

    static Double toDouble(String text) {
        if (text == null) return null;
        try {
            double d = Double.parseDouble(text.trim());
            return Double.isNaN(d) || Double.isInfinite(d) ? null : d;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static Integer toInteger(String text) {
        if (text == null) return null;
        try {
            return Integer.valueOf(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

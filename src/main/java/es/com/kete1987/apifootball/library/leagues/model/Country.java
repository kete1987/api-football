package es.com.kete1987.apifootball.library.leagues.model;

/**
 * A country as returned by {@code /countries} and {@code /teams/countries}.
 *
 * <p>{@code code} and {@code flag} are {@code null} for "World" and for entries that are not
 * countries (e.g. teams-only regions). In {@code /teams/countries} names are hyphenated
 * (e.g. {@code American-Samoa}).
 */
public class Country {
    private String name;
    private String code;
    private String flag;

    public String getName() {
        return name;
    }

    /** Alpha code (2 to 6 characters, e.g. {@code ES}, {@code GB-ENG}); {@code null} when unknown. */
    public String getCode() {
        return code;
    }

    /** URL of the SVG flag; {@code null} when unknown. */
    public String getFlag() {
        return flag;
    }
}

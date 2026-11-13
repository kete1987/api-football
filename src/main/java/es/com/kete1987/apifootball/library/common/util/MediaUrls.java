package es.com.kete1987.apifootball.library.common.util;

/**
 * Builders for the image URLs of {@code media.api-sports.io}, following the patterns of
 * docs/API_CATALOG.md section 7 (and nothing else: no pattern here is invented).
 *
 * <table>
 *   <caption>URL patterns</caption>
 *   <tr><td>player photo</td><td>{@code https://media.api-sports.io/football/players/{id}.png}</td></tr>
 *   <tr><td>team logo</td><td>{@code https://media.api-sports.io/football/teams/{id}.png}</td></tr>
 *   <tr><td>league logo</td><td>{@code https://media.api-sports.io/football/leagues/{id}.png}</td></tr>
 *   <tr><td>venue image</td><td>{@code https://media.api-sports.io/football/venues/{id}.png}</td></tr>
 *   <tr><td>coach photo</td><td>{@code https://media.api-sports.io/football/coachs/{id}.png}</td></tr>
 *   <tr><td>country flag</td><td>{@code https://media.api-sports.io/flags/{code}.svg}, lower case code</td></tr>
 * </table>
 *
 * <p>Most responses already carry the URL ({@code logo}, {@code photo}, {@code flag}); use these when
 * only an id is at hand, typically a player in an event or a lineup. Images <b>do not count against the
 * daily quota</b> but are rate-limited per second/minute, and the API documentation recommends caching
 * them on your side. A builder makes no request. Rights of logos and photos belong to third parties.
 *
 * <p>Every method returns {@code null} instead of throwing when its argument is {@code null}, an id is
 * not positive (the API sends {@code 0} for an unknown venue) or a country code is not a plain code.
 */
public final class MediaUrls {

    /** Base of every image URL, with a trailing slash. */
    public static final String BASE_URL = "https://media.api-sports.io/";

    private MediaUrls() {
        // static builders only
    }

    private static String football(String folder, Integer id) {
        return id == null || id <= 0 ? null : BASE_URL + "football/" + folder + "/" + id + ".png";
    }

    /** Player photo URL, or {@code null} for a {@code null} or non-positive id. */
    public static String playerPhoto(Integer playerId) {
        return football("players", playerId);
    }

    /** Team logo URL, or {@code null} for a {@code null} or non-positive id. */
    public static String teamLogo(Integer teamId) {
        return football("teams", teamId);
    }

    /** League logo URL, or {@code null} for a {@code null} or non-positive id. */
    public static String leagueLogo(Integer leagueId) {
        return football("leagues", leagueId);
    }

    /** Venue image URL, or {@code null} for a {@code null} or non-positive id (the API sends 0 for unknown venues). */
    public static String venueImage(Integer venueId) {
        return football("venues", venueId);
    }

    /** Coach photo URL (the path segment is {@code coachs}), or {@code null} for a {@code null} or non-positive id. */
    public static String coachPhoto(Integer coachId) {
        return football("coachs", coachId);
    }

    /**
     * Country flag URL from the country code ({@code "ES"} or {@code "es"} give {@code .../flags/es.svg};
     * sub-national codes such as {@code "GB-ENG"} give {@code gb-eng.svg}). The code is trimmed and lower
     * cased. {@code null} for a {@code null}, blank or non plain code (only letters, digits and
     * {@code -} are accepted); the API itself has no flag for "World" and many teams-only countries, so
     * a {@code null} code is common.
     */
    public static String countryFlag(String countryCode) {
        if (countryCode == null) {
            return null;
        }
        String code = countryCode.trim().toLowerCase(java.util.Locale.ROOT);
        return code.matches("[a-z0-9]+(-[a-z0-9]+)*") ? BASE_URL + "flags/" + code + ".svg" : null;
    }
}

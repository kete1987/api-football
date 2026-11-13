package es.com.kete1987.apifootball.library.competition.util;

import es.com.kete1987.apifootball.library.competition.model.RoundName;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Types of a competition stage, rebuilt from the round text because API-Football has no stage
 * entity. Plain {@code String} constants, like the other constants classes: new wording appears
 * over time and must not break anything.
 *
 * <p>{@link #ofStageName(String, boolean)} decides from the name alone, with these rules, in order:
 * <ol>
 *   <li>{@code null} or blank: {@link #UNKNOWN};</li>
 *   <li>the name contains "qualif" or "preliminary" (any case): {@link #QUALIFYING};</li>
 *   <li>the name starts with "group": {@link #GROUP};</li>
 *   <li>the round carries a matchday number ({@code "<stage> - <n>"}): {@link #LEAGUE};</li>
 *   <li>the name contains "final" (Final, Semi-finals, Quarter-finals, 3rd Place Final, 8th Finals),
 *       "round of", "play-off", "playoff" or "knockout", or is an ordinal round such as
 *       "1st Round" / "Round 2": {@link #KNOCKOUT};</li>
 *   <li>anything else ({@code "Friendly International"}...): {@link #UNKNOWN}.</li>
 * </ol>
 *
 * <p>Truth table over every round text seen in real responses (tests and samples):
 * <table>
 *   <caption>Observed round texts</caption>
 *   <tr><th>Round text</th><th>Type</th></tr>
 *   <tr><td>1st / 2nd / 3rd Qualifying Round</td><td>QUALIFYING</td></tr>
 *   <tr><td>Group Stage - 1..3, Group Stage</td><td>GROUP</td></tr>
 *   <tr><td>Regular Season - 1..38, League Stage - 1..8, Apertura - 16, Apertura - 20,
 *       League A - 5, Clausura - Quadrangular - 4</td><td>LEAGUE</td></tr>
 *   <tr><td>Play-offs, Round of 32, Round of 16, Quarter-finals, Semi-finals, 3rd Place Final,
 *       Final</td><td>KNOCKOUT</td></tr>
 *   <tr><td>Friendly International</td><td>UNKNOWN</td></tr>
 * </table>
 * Never seen in a response, answered by the same rules: "Preliminary Round" (QUALIFYING),
 * "Group A" (GROUP), "1st Round", "Round 2", "8th Finals", "Promotion Play-offs",
 * "Knockout Round Play-offs" (KNOCKOUT), "Championship Round - 3", "Relegation Round - 2" (LEAGUE).
 *
 * <p>The name alone cannot tell that the Champions League {@code "Play-offs"} (August, before the
 * league phase) are a qualifying round: {@code CompetitionStructure} adds that by position (a
 * knockout stage before the first league or group stage is qualifying). Note also that
 * {@code "Clausura - Quadrangular - 4"} and {@code "League A - 5"} are really groups of a few
 * teams, but nothing in the text says so: they answer {@link #LEAGUE}, the type of any numbered
 * round.
 */
public final class StageTypes {

    /** A single-table phase played in numbered matchdays (Regular Season, League Stage, Apertura...). */
    public static final String LEAGUE = "LEAGUE";
    /** A group phase: several tables, numbered matchdays shared by every group (Group Stage - N). */
    public static final String GROUP = "GROUP";
    /** An elimination round of the main competition (Round of 16, Quarter-finals, Final...). */
    public static final String KNOCKOUT = "KNOCKOUT";
    /** A preliminary or qualifying round played before the main competition. */
    public static final String QUALIFYING = "QUALIFYING";
    /** A name none of the rules recognise (e.g. {@code "Friendly International"}). */
    public static final String UNKNOWN = "UNKNOWN";

    private static final Pattern ORDINAL_ROUND =
            Pattern.compile("^(\\d+(st|nd|rd|th) round|round \\d+)$");

    private StageTypes() {
        // constants and helpers only
    }

    /**
     * Type of a stage decided from its name only (rules in the class Javadoc). Never throws.
     *
     * @param stageName the stage name (a round text without its {@code " - <n>"}); may be {@code null}
     * @param numbered  whether the rounds of the stage carry a matchday number
     * @return one of the constants of this class
     */
    public static String ofStageName(String stageName, boolean numbered) {
        if (stageName == null || stageName.trim().isEmpty()) return UNKNOWN;
        String name = stageName.trim().toLowerCase(Locale.ROOT);
        if (name.contains("qualif") || name.contains("preliminary")) return QUALIFYING;
        if (name.startsWith("group")) return GROUP;
        if (numbered) return LEAGUE;
        if (name.contains("final") || name.contains("round of") || name.contains("play-off")
                || name.contains("playoff") || name.contains("knockout")
                || ORDINAL_ROUND.matcher(name).matches()) {
            return KNOCKOUT;
        }
        return UNKNOWN;
    }

    /**
     * Type of the stage of a round text such as {@code "League Stage - 5"}, from the name only (see
     * {@link #ofStageName(String, boolean)}); {@link #UNKNOWN} for {@code null}. Never throws.
     */
    public static String ofRound(String round) {
        RoundName name = RoundName.parse(round);
        return name == null ? UNKNOWN : name.getStageType();
    }

    /** {@code true} for {@link #LEAGUE} and {@link #GROUP}: the stage produces a standings table. */
    public static boolean isTablePhase(String type) {
        return LEAGUE.equals(type) || GROUP.equals(type);
    }

    /** {@code true} for {@link #KNOCKOUT} and {@link #QUALIFYING}: the stage is played in ties. */
    public static boolean isElimination(String type) {
        return KNOCKOUT.equals(type) || QUALIFYING.equals(type);
    }
}

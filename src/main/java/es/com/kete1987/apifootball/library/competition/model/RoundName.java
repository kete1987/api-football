package es.com.kete1987.apifootball.library.competition.model;

import es.com.kete1987.apifootball.library.competition.util.StageTypes;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A round label of API-Football ({@code league.round} of a fixture, or an item of
 * {@code fixtures/rounds}) split into its stage and its matchday number.
 *
 * <p>API-Football has no stage entity: the only structure is the round text, which is either
 * {@code "<stage>"} ({@code "Round of 16"}, {@code "Final"}, {@code "Play-offs"}) or
 * {@code "<stage> - <n>"} ({@code "League Stage - 5"}, {@code "Regular Season - 13"},
 * {@code "Group Stage - 2"}). Only a trailing {@code " - <digits>"} is taken as a matchday, so
 * {@code "Clausura - Quadrangular - 4"} is stage {@code "Clausura - Quadrangular"}, matchday 4.
 *
 * <p>The stage name is the API's text, untranslated. In particular the same name can mean
 * different things in different competitions ({@code "Round of 32"} is the two-legged league-phase
 * play-off of the Champions League 2025-26 but the single-match round of 32 of the World Cup 2026),
 * so this class never maps names to labels such as "1/16 final".
 */
public final class RoundName {

    private static final Pattern NUMBERED = Pattern.compile("^(.*\\S)\\s+-\\s+(\\d{1,9})$");

    private final String round;
    private final String stage;
    private final Integer matchday;

    private RoundName(String round, String stage, Integer matchday) {
        this.round = round;
        this.stage = stage;
        this.matchday = matchday;
    }

    /**
     * Parses a round label. Never throws.
     *
     * @param round the round text as the API sends it; may be {@code null}
     * @return the parsed name, or {@code null} when {@code round} is {@code null} or blank
     */
    public static RoundName parse(String round) {
        if (round == null || round.trim().isEmpty()) return null;
        String trimmed = round.trim();
        Matcher m = NUMBERED.matcher(trimmed);
        if (m.matches()) {
            return new RoundName(round, m.group(1), Integer.valueOf(m.group(2)));
        }
        return new RoundName(round, trimmed, null);
    }

    /**
     * The stage of a round label ({@code "League Stage"} for {@code "League Stage - 5"},
     * {@code "Final"} for {@code "Final"}), or {@code null} for a {@code null}/blank label.
     */
    public static String stageOf(String round) {
        RoundName name = parse(round);
        return name == null ? null : name.getStage();
    }

    /** The original round text, unchanged (usable as the {@code round} filter of {@code /fixtures}). */
    public String getRound() {
        return round;
    }

    /** The stage name: the round text without its trailing {@code " - <n>"}, trimmed. Never {@code null}. */
    public String getStage() {
        return stage;
    }

    /** The matchday number ({@code 5} for {@code "League Stage - 5"}), or {@code null} when the round has none. */
    public Integer getMatchday() {
        return matchday;
    }

    /** {@code true} when the round carries a matchday number. */
    public boolean hasMatchday() {
        return matchday != null;
    }

    /** The stage type decided from the name alone; see {@link StageTypes#ofStageName(String, boolean)}. */
    public String getStageType() {
        return StageTypes.ofStageName(stage, matchday != null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RoundName)) return false;
        RoundName other = (RoundName) o;
        return round.equals(other.round);
    }

    @Override
    public int hashCode() {
        return Objects.hash(round);
    }

    @Override
    public String toString() {
        return round;
    }
}

package es.com.kete1987.apifootball.library.competition.model;

import es.com.kete1987.apifootball.library.competition.util.StageStatuses;
import es.com.kete1987.apifootball.library.competition.util.StageTypes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A stage of a competition (league phase, group phase, a knockout round, a qualifying round),
 * rebuilt by {@link CompetitionStructure} from the round texts of {@code fixtures/rounds}: all the
 * rounds sharing a {@link RoundName#getStage() stage name} form one stage. API-Football has no stage
 * entity, no stage id and no finished/current flag; everything here is derived.
 *
 * <p>A stage does not translate its name: {@code "Round of 32"} is the name of two different things
 * in the Champions League 2025-26 (the two-legged league-phase play-off) and the World Cup 2026 (a
 * single-match round of 32). Count the ties and their legs with {@code Ties} and label from those
 * facts.
 */
public final class Stage {

    private final String name;
    private final int order;
    private final String type;
    private final List<String> rounds;
    private final LocalDate firstDate;
    private final LocalDate lastDate;

    Stage(String name, int order, String type, List<String> rounds, LocalDate firstDate, LocalDate lastDate) {
        this.name = name;
        this.order = order;
        this.type = type;
        this.rounds = Collections.unmodifiableList(new ArrayList<>(rounds));
        this.firstDate = firstDate;
        this.lastDate = lastDate;
    }

    /** Stage name as the API writes it, e.g. {@code "League Stage"}, {@code "Round of 16"}. */
    public String getName() {
        return name;
    }

    /** Zero-based position of the stage in the competition (order of first appearance in {@code fixtures/rounds}). */
    public int getOrder() {
        return order;
    }

    /**
     * Stage type, one of the {@link StageTypes} constants. It is the name-based type, except that a
     * {@link StageTypes#KNOCKOUT} stage placed before the first league or group stage of the
     * competition is {@link StageTypes#QUALIFYING} (the Champions League {@code "Play-offs"}).
     */
    public String getType() {
        return type;
    }

    /** {@code true} when the type is {@link StageTypes#LEAGUE} or {@link StageTypes#GROUP}. */
    public boolean isTablePhase() {
        return StageTypes.isTablePhase(type);
    }

    /** {@code true} when the type is {@link StageTypes#KNOCKOUT} or {@link StageTypes#QUALIFYING}. */
    public boolean isElimination() {
        return StageTypes.isElimination(type);
    }

    /**
     * Round texts of the stage, in the order of {@code fixtures/rounds} (which is chronological, not
     * numeric: LaLiga 2026-27 lists {@code "Regular Season - 6"} before {@code "Regular Season - 4"}).
     */
    public List<String> getRounds() {
        return rounds;
    }

    /**
     * Matchday numbers of the stage's rounds, in the order of {@link #getRounds()}; empty for a
     * stage whose rounds have no number (knockout rounds).
     */
    public List<Integer> getMatchdays() {
        List<Integer> matchdays = new ArrayList<>();
        for (String round : rounds) {
            RoundName parsed = RoundName.parse(round);
            if (parsed != null && parsed.getMatchday() != null) matchdays.add(parsed.getMatchday());
        }
        return Collections.unmodifiableList(matchdays);
    }

    /**
     * Earliest round date of the stage, or {@code null} when the structure was built without dates
     * (or none of its dates parsed). Dates are the {@code YYYY-MM-DD} values of
     * {@code fixtures/rounds?dates=true}.
     */
    public LocalDate getFirstDate() {
        return firstDate;
    }

    /** Latest round date of the stage, or {@code null} without dates; see {@link #getFirstDate()}. */
    public LocalDate getLastDate() {
        return lastDate;
    }

    /**
     * Where the stage stands on {@code date}: {@link StageStatuses#FINISHED},
     * {@link StageStatuses#CURRENT} (first date &lt;= date &lt;= last date, including the days
     * between two legs), {@link StageStatuses#UPCOMING}, or {@link StageStatuses#UNKNOWN} when the
     * stage has no dates or {@code date} is {@code null}. Never throws.
     *
     * <p>Dates only: a stage is {@code FINISHED} the day after its last scheduled date even if a
     * match is still pending without a new date. Use the fixtures' statuses for certainty.
     */
    public String getStatus(LocalDate date) {
        if (date == null || firstDate == null || lastDate == null) return StageStatuses.UNKNOWN;
        if (date.isAfter(lastDate)) return StageStatuses.FINISHED;
        if (date.isBefore(firstDate)) return StageStatuses.UPCOMING;
        return StageStatuses.CURRENT;
    }

    /** {@code true} when {@link #getStatus(LocalDate)} is {@link StageStatuses#FINISHED}. */
    public boolean isFinished(LocalDate date) {
        return StageStatuses.FINISHED.equals(getStatus(date));
    }

    /** {@code true} when {@link #getStatus(LocalDate)} is {@link StageStatuses#CURRENT}. */
    public boolean isCurrent(LocalDate date) {
        return StageStatuses.CURRENT.equals(getStatus(date));
    }

    /** {@code true} when {@link #getStatus(LocalDate)} is {@link StageStatuses#UPCOMING}. */
    public boolean isUpcoming(LocalDate date) {
        return StageStatuses.UPCOMING.equals(getStatus(date));
    }

    @Override
    public String toString() {
        return name + " (" + type + ", " + rounds.size() + " rounds)";
    }
}

package es.com.kete1987.apifootball.library.competition.model;

import es.com.kete1987.apifootball.library.common.util.DateParsing;
import es.com.kete1987.apifootball.library.competition.util.StageTypes;
import es.com.kete1987.apifootball.library.fixtures.model.Fixture;
import es.com.kete1987.apifootball.library.fixtures.model.RoundDates;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The stages of a competition season, rebuilt from the output of {@code fixtures/rounds}
 * ({@code FixturesApi.getRounds}) or {@code fixtures/rounds?dates=true}
 * ({@code FixturesApi.getRoundsWithDates}). Pure computation: building it and every method cost
 * no request.
 *
 * <p>Rules: rounds are kept in the API's order (chronological, not numeric) and duplicates are
 * dropped; every round whose {@link RoundName#getStage() stage name} is the same belongs to one
 * {@link Stage}, positioned where its first round appears; the stage type is
 * {@link StageTypes#ofStageName(String, boolean)}, except that a knockout stage before the first
 * league or group stage becomes {@link StageTypes#QUALIFYING} (the Champions League 2025-26
 * {@code "Play-offs"} in August). With dates, a stage spans from its earliest to its latest round
 * date.
 *
 * <pre>{@code
 * CompetitionStructure cl = CompetitionStructure.fromRoundDates(fixtures.getRoundsWithDates(2, 2025));
 * for (Stage stage : cl.getStages()) {
 *     System.out.println(stage.getName() + " " + stage.getType() + " " + stage.getStatus(LocalDate.now()));
 * }
 * Map<String, List<Fixture>> byRound = cl.groupByRound(fixtures.getFixturesByLeague(2, 2025));
 * }</pre>
 */
public final class CompetitionStructure {

    private final List<String> rounds;
    private final List<Stage> stages;

    private CompetitionStructure(List<String> rounds, List<Stage> stages) {
        this.rounds = Collections.unmodifiableList(rounds);
        this.stages = Collections.unmodifiableList(stages);
    }

    /**
     * Builds the structure from round texts in the order of {@code fixtures/rounds}. Stages have no
     * dates, so their status is {@code UNKNOWN}. {@code null} or blank rounds are skipped; a
     * {@code null} list gives an empty structure. Never throws.
     */
    public static CompetitionStructure fromRounds(List<String> rounds) {
        Map<String, List<String>> dates = new LinkedHashMap<>();
        if (rounds != null) {
            for (String round : rounds) {
                if (round != null && !round.trim().isEmpty()) dates.putIfAbsent(round, Collections.emptyList());
            }
        }
        return build(dates);
    }

    /**
     * Builds the structure from {@code fixtures/rounds?dates=true}, giving every stage its first and
     * last date. Items with a {@code null} round are skipped, unparsable dates are ignored; a
     * {@code null} list gives an empty structure. Never throws.
     */
    public static CompetitionStructure fromRoundDates(List<RoundDates> roundDates) {
        Map<String, List<String>> dates = new LinkedHashMap<>();
        if (roundDates != null) {
            for (RoundDates item : roundDates) {
                if (item == null || item.getRound() == null || item.getRound().trim().isEmpty()) continue;
                List<String> list = dates.computeIfAbsent(item.getRound(), k -> new ArrayList<>());
                if (item.getDates() != null) list.addAll(item.getDates());
            }
        }
        return build(dates);
    }

    private static CompetitionStructure build(Map<String, List<String>> roundDates) {
        Map<String, List<String>> stageRounds = new LinkedHashMap<>();
        Map<String, Boolean> stageNumbered = new LinkedHashMap<>();
        Map<String, LocalDate[]> stageSpan = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : roundDates.entrySet()) {
            RoundName name = RoundName.parse(entry.getKey());
            String stage = name.getStage();
            stageRounds.computeIfAbsent(stage, k -> new ArrayList<>()).add(entry.getKey());
            stageNumbered.merge(stage, name.hasMatchday(), Boolean::logicalOr);
            LocalDate[] span = stageSpan.computeIfAbsent(stage, k -> new LocalDate[2]);
            for (String text : entry.getValue()) {
                LocalDate date = DateParsing.parseIsoDate(text);
                if (date == null) continue;
                if (span[0] == null || date.isBefore(span[0])) span[0] = date;
                if (span[1] == null || date.isAfter(span[1])) span[1] = date;
            }
        }
        List<String> types = new ArrayList<>();
        int firstTable = -1;
        for (String stage : stageRounds.keySet()) {
            String type = StageTypes.ofStageName(stage, stageNumbered.get(stage));
            if (firstTable < 0 && StageTypes.isTablePhase(type)) firstTable = types.size();
            types.add(type);
        }
        List<Stage> stages = new ArrayList<>();
        int i = 0;
        for (Map.Entry<String, List<String>> entry : stageRounds.entrySet()) {
            String type = types.get(i);
            if (i < firstTable && StageTypes.KNOCKOUT.equals(type)) type = StageTypes.QUALIFYING;
            LocalDate[] span = stageSpan.get(entry.getKey());
            stages.add(new Stage(entry.getKey(), i, type, entry.getValue(), span[0], span[1]));
            i++;
        }
        return new CompetitionStructure(new ArrayList<>(roundDates.keySet()), stages);
    }

    /** Every round text, in the official order of {@code fixtures/rounds}, without duplicates. */
    public List<String> getRounds() {
        return rounds;
    }

    /** The stages in competition order; empty when there were no rounds. */
    public List<Stage> getStages() {
        return stages;
    }

    /** The stage with that exact name (e.g. {@code "Round of 16"}), or {@code null}. */
    public Stage getStage(String name) {
        if (name == null) return null;
        for (Stage stage : stages) {
            if (stage.getName().equals(name)) return stage;
        }
        return null;
    }

    /**
     * The stage a round text belongs to (e.g. {@code "League Stage"} for {@code "League Stage - 5"}),
     * or {@code null} when no stage has that name. The round itself does not have to be listed (a
     * stage name match is enough).
     */
    public Stage getStageOfRound(String round) {
        return getStage(RoundName.stageOf(round));
    }

    /** The stages of one {@link StageTypes} type, in competition order. */
    public List<Stage> getStages(String type) {
        List<Stage> result = new ArrayList<>();
        for (Stage stage : stages) {
            if (stage.getType().equals(type)) result.add(stage);
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * The stage being played on {@code date}: the first stage whose dates include it; otherwise, in
     * a break between stages, the next stage to start; {@code null} when every stage is over, when the
     * structure has no dates, or when {@code date} is {@code null}. Unlike the {@code current} flag of
     * a season, this turns {@code null} once the competition is over.
     */
    public Stage getActiveStage(LocalDate date) {
        if (date == null) return null;
        for (Stage stage : stages) {
            if (stage.isCurrent(date)) return stage;
        }
        Stage next = null;
        for (Stage stage : stages) {
            if (stage.isUpcoming(date) && (next == null || stage.getFirstDate().isBefore(next.getFirstDate()))) {
                next = stage;
            }
        }
        return next;
    }

    /**
     * Groups fixtures (typically {@code FixturesApi.getFixturesByLeague}, one request for the whole
     * season) by their {@code league.round}, keys in the official round order; within a round the
     * fixtures are sorted by kick-off timestamp (fixtures without one last, ties keep the input
     * order). Rounds without fixtures are left out. Fixtures whose round is not in the structure come
     * after the official rounds, in order of first appearance, and fixtures without a round are under
     * the {@code null} key at the very end: nothing is dropped. Never throws; {@code null} gives an
     * empty map.
     */
    public Map<String, List<Fixture>> groupByRound(Collection<Fixture> fixtures) {
        Map<String, List<Fixture>> unordered = new LinkedHashMap<>();
        if (fixtures != null) {
            for (Fixture fixture : fixtures) {
                if (fixture == null) continue;
                unordered.computeIfAbsent(roundOf(fixture), k -> new ArrayList<>()).add(fixture);
            }
        }
        Set<String> keys = new LinkedHashSet<>();
        for (String round : rounds) {
            if (unordered.containsKey(round)) keys.add(round);
        }
        for (String round : unordered.keySet()) {
            if (round != null) keys.add(round);
        }
        if (unordered.containsKey(null)) keys.add(null);
        Map<String, List<Fixture>> result = new LinkedHashMap<>();
        for (String key : keys) {
            List<Fixture> list = new ArrayList<>(unordered.get(key));
            list.sort(BY_KICK_OFF);
            result.put(key, Collections.unmodifiableList(list));
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * Groups fixtures by stage name, keys in stage order, each list sorted by kick-off; fixtures of
     * rounds whose stage is not in the structure follow, then fixtures without a round under the
     * {@code null} key. Never throws.
     */
    public Map<String, List<Fixture>> groupByStage(Collection<Fixture> fixtures) {
        Map<String, List<Fixture>> byRound = groupByRound(fixtures);
        Map<String, List<Fixture>> unordered = new LinkedHashMap<>();
        for (Map.Entry<String, List<Fixture>> entry : byRound.entrySet()) {
            String stage = entry.getKey() == null ? null : RoundName.stageOf(entry.getKey());
            unordered.computeIfAbsent(stage, k -> new ArrayList<>()).addAll(entry.getValue());
        }
        Set<String> keys = new LinkedHashSet<>();
        for (Stage stage : stages) {
            if (unordered.containsKey(stage.getName())) keys.add(stage.getName());
        }
        for (String stage : unordered.keySet()) {
            if (stage != null) keys.add(stage);
        }
        if (unordered.containsKey(null)) keys.add(null);
        Map<String, List<Fixture>> result = new LinkedHashMap<>();
        for (String key : keys) {
            List<Fixture> list = new ArrayList<>(unordered.get(key));
            list.sort(BY_KICK_OFF);
            result.put(key, Collections.unmodifiableList(list));
        }
        return Collections.unmodifiableMap(result);
    }

    private static String roundOf(Fixture fixture) {
        return fixture.getLeague() == null ? null : fixture.getLeague().getRound();
    }

    /** Kick-off order: {@code fixture.timestamp} ascending, fixtures without one last. Stable when used with {@code List.sort}. */
    static final Comparator<Fixture> BY_KICK_OFF = Comparator.comparing(
            CompetitionStructure::timestampOf, Comparator.nullsLast(Comparator.naturalOrder()));

    static Long timestampOf(Fixture fixture) {
        return fixture == null || fixture.getFixture() == null ? null : fixture.getFixture().getTimestamp();
    }
}

package es.com.kete1987.apifootball.library;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;
import es.com.kete1987.apifootball.library.account.model.Status;
import es.com.kete1987.apifootball.library.fixtures.model.Fixture;
import es.com.kete1987.apifootball.library.fixtures.model.FixtureEvent;
import es.com.kete1987.apifootball.library.fixtures.model.FixtureLineup;
import es.com.kete1987.apifootball.library.fixtures.model.FixturePlayers;
import es.com.kete1987.apifootball.library.fixtures.model.FixtureStatistics;
import es.com.kete1987.apifootball.library.fixtures.model.Injury;
import es.com.kete1987.apifootball.library.fixtures.model.Prediction;
import es.com.kete1987.apifootball.library.fixtures.model.RoundDates;
import es.com.kete1987.apifootball.library.leagues.model.Country;
import es.com.kete1987.apifootball.library.leagues.model.League;
import es.com.kete1987.apifootball.library.odds.model.Bookmaker;
import es.com.kete1987.apifootball.library.odds.model.LiveBet;
import es.com.kete1987.apifootball.library.odds.model.LiveOdds;
import es.com.kete1987.apifootball.library.odds.model.OddsMapping;
import es.com.kete1987.apifootball.library.odds.model.PrematchBet;
import es.com.kete1987.apifootball.library.odds.model.PrematchOdds;
import es.com.kete1987.apifootball.library.players.model.PlayerProfile;
import es.com.kete1987.apifootball.library.players.model.PlayerTeam;
import es.com.kete1987.apifootball.library.players.model.PlayerTransfers;
import es.com.kete1987.apifootball.library.players.model.PlayerWithStatistics;
import es.com.kete1987.apifootball.library.players.model.SidelinedGroup;
import es.com.kete1987.apifootball.library.players.model.SidelinedPeriod;
import es.com.kete1987.apifootball.library.players.model.Squad;
import es.com.kete1987.apifootball.library.players.model.Trophy;
import es.com.kete1987.apifootball.library.players.model.TrophyGroup;
import es.com.kete1987.apifootball.library.teams.model.Coach;
import es.com.kete1987.apifootball.library.teams.model.Team;
import es.com.kete1987.apifootball.library.teams.model.TeamStatistics;
import es.com.kete1987.apifootball.library.teams.model.Venue;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Checks that the models map every real response stored under {@code src/test/resources/fixtures/}
 * completely: each fixture is walked against the model type its API method parses into, and the
 * test fails on
 * <ul>
 *   <li>a JSON key with no model field (honouring {@code @SerializedName}, superclasses, and
 *       {@code List}/{@code Map}/{@code JsonElement} members), and</li>
 *   <li>a JSON scalar that the library's Gson cannot read into the field's Java type.</li>
 * </ul>
 * A new fixture must be added to {@link #MODEL_OF} or to {@link #NOT_MAPPED}; otherwise
 * {@link #everyFixtureIsMappedOrListed()} fails, so it cannot be forgotten.
 */
class ModelCoverageTest {

    /** Fixture file -> element type of its {@code response} (each array element, or the object itself). */
    private static final Map<String, Type> MODEL_OF = new LinkedHashMap<>();

    /** Fixtures deliberately not mapped: error envelopes, whose {@code response} is empty. */
    private static final Set<String> NOT_MAPPED = new TreeSet<>(Arrays.asList(
            "error_invalid_integer.json",
            "error_last_too_large.json",
            "error_missing_param.json",
            "error_no_params.json",
            "error_players_search_without_scope.json",
            "error_search_too_short.json",
            "error_standings_missing_season.json",
            "error_too_many_ids.json",
            "error_unknown_endpoint.json",
            "error_unknown_param.json"));

    static {
        // account / leagues
        map(Status.class, "status.json");
        map(String.class, "timezone.json");
        map(Country.class, "countries.json", "countries_by_name.json", "teams_countries.json");
        map(League.class, "leagues_by_id.json", "leagues_current_country.json", "leagues_by_id_world_cup.json",
                "leagues_by_id_ucl.json");
        map(Integer.class, "leagues_seasons.json", "teams_seasons.json", "players_seasons.json",
                "players_seasons_by_player.json");
        map(standingsItem(), "standings_league.json", "standings_groups.json", "standings_world_cup_2026.json",
                "standings_ucl_2025.json", "standings_laliga_2026.json");
        // teams
        map(Team.class, "teams_by_id.json", "teams_by_league_season.json");
        map(TeamStatistics.class, "teams_statistics.json");
        map(Venue.class, "venues_by_id.json", "venues_search.json");
        map(Coach.class, "coachs_by_team.json");
        // fixtures
        map(String.class, "fixtures_rounds.json", "fixtures_rounds_current.json", "fixtures_rounds_ucl_2025.json",
                "fixtures_rounds_world_cup_2026.json", "fixtures_rounds_laliga_2026.json");
        map(RoundDates.class, "fixtures_rounds_dates.json", "fixtures_rounds_dates_ucl_2025.json");
        map(Fixture.class, "fixtures_by_id.json", "fixtures_by_ids.json", "fixtures_live.json",
                "fixtures_by_date.json", "fixtures_by_date_sunday.json", "fixtures_last.json",
                "fixtures_next.json", "fixtures_status_multi.json", "fixtures_empty.json",
                "fixtures_invalid_date_overflow.json", "fixtures_invalid_timezone_fallback.json",
                "fixtures_headtohead.json", "fixtures_by_id_penalty_shootout.json", "fixtures_by_id_own_goal.json",
                "fixtures_by_league_ucl_2025.json", "fixtures_by_league_world_cup_2026.json");
        map(FixtureStatistics.class, "fixtures_statistics.json", "fixtures_statistics_half.json");
        map(FixtureEvent.class, "fixtures_events.json");
        map(FixtureLineup.class, "fixtures_lineups.json");
        map(FixturePlayers.class, "fixtures_players.json");
        map(Injury.class, "injuries_by_fixture.json", "injuries_by_league_season.json");
        map(Prediction.class, "predictions.json");
        // players
        map(PlayerWithStatistics.class, "players_by_id_season.json", "players_page1.json",
                "players_page2.json", "players_page_out_of_range.json", "players_topscorers.json",
                "players_topassists.json", "players_topyellowcards.json", "players_topredcards.json");
        map(PlayerProfile.class, "players_profiles_by_player.json", "players_profiles_page1.json");
        map(Squad.class, "players_squads.json", "players_squads_by_player.json");
        map(PlayerTeam.class, "players_teams.json");
        map(PlayerTransfers.class, "transfers_by_player.json");
        map(Trophy.class, "trophies_by_player.json");
        map(TrophyGroup.class, "trophies_by_coachs.json");
        map(SidelinedPeriod.class, "sidelined_by_player.json", "sidelined_by_coach.json");
        map(SidelinedGroup.class, "sidelined_by_players.json");
        // odds
        map(PrematchOdds.class, "odds_by_fixture.json", "odds_by_league_season.json", "odds_by_date_page2.json");
        map(OddsMapping.class, "odds_mapping.json");
        map(Bookmaker.class, "odds_bookmakers.json");
        map(PrematchBet.class, "odds_bets.json");
        map(LiveOdds.class, "odds_live.json");
        map(LiveBet.class, "odds_live_bets.json");
    }

    private static void map(Type type, String... fixtures) {
        for (String fixture : fixtures) MODEL_OF.put(fixture, type);
    }

    /** {@code LeaguesApi} parses {@code /standings} through a private wrapper. */
    private static Class<?> standingsItem() {
        try {
            return Class.forName(LeaguesApi.class.getName() + "$StandingsItem");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    // ------------------------------------------------------------------ tests

    @Test
    void everyFixtureIsMappedOrListed() throws IOException {
        Set<String> forgotten = fixtureFiles().stream()
                .filter(name -> !MODEL_OF.containsKey(name) && !NOT_MAPPED.contains(name))
                .collect(Collectors.toCollection(TreeSet::new));
        assertTrue(forgotten.isEmpty(), "Fixtures neither mapped to a model nor listed as not mapped: " + forgotten);

        Set<String> missing = new TreeSet<>(MODEL_OF.keySet());
        missing.addAll(NOT_MAPPED);
        missing.removeAll(fixtureFiles());
        assertTrue(missing.isEmpty(), "Listed fixtures that do not exist: " + missing);
    }

    @Test
    void notMappedFixturesAreErrorEnvelopes() throws IOException {
        for (String name : NOT_MAPPED) {
            JsonObject root = read(name);
            assertTrue(root.has("errors") && !ApiFootballBase.errorsOf(root.get("errors")).isEmpty(),
                    name + " is listed as an error fixture but carries no errors");
        }
    }

    @TestFactory
    Stream<DynamicTest> everyJsonKeyAndValueFitsTheModel() {
        return MODEL_OF.entrySet().stream().map(entry -> DynamicTest.dynamicTest(entry.getKey(), () -> {
            JsonElement response = read(entry.getKey()).get("response");
            List<String> problems = new ArrayList<>();
            if (response.isJsonArray()) {
                for (JsonElement element : response.getAsJsonArray()) {
                    walk(element, entry.getValue(), "response[]", problems);
                }
            } else {
                walk(response, entry.getValue(), "response", problems);
            }
            if (!problems.isEmpty()) {
                fail(entry.getKey() + " does not fit " + entry.getValue().getTypeName() + ":\n  "
                        + String.join("\n  ", new TreeSet<>(problems)));
            }
        }));
    }

    @Test
    void walkerDetectsAnUnmappedKeyAndAnUnreadableValue() {
        List<String> problems = new ArrayList<>();
        walk(JsonParser.parseString("{\"id\": \"abc\", \"unknown\": 1}"), Bookmaker.class, "$", problems);
        assertEquals(2, problems.size(), problems.toString());
    }

    // ------------------------------------------------------------------ the walk

    /** Compares {@code json} with {@code type}, recursing into objects, lists and maps. */
    private static void walk(JsonElement json, Type type, String path, List<String> problems) {
        if (json == null || json.isJsonNull()) return;
        Class<?> raw = rawClass(type);
        if (JsonElement.class.isAssignableFrom(raw)) return; // shape-shifting member, kept raw

        if (Collection.class.isAssignableFrom(raw)) {
            if (!json.isJsonArray()) {
                problems.add(path + ": expected an array for " + type.getTypeName() + " but was " + json);
                return;
            }
            Type element = typeArgument(type, 0);
            for (JsonElement item : json.getAsJsonArray()) walk(item, element, path + "[]", problems);
        } else if (Map.class.isAssignableFrom(raw)) {
            if (!json.isJsonObject()) {
                problems.add(path + ": expected an object for " + type.getTypeName() + " but was " + json);
                return;
            }
            Type value = typeArgument(type, 1);
            for (Map.Entry<String, JsonElement> e : json.getAsJsonObject().entrySet()) {
                walk(e.getValue(), value, path + "{" + e.getKey() + "}", problems);
            }
        } else if (isScalar(raw)) {
            if (!json.isJsonPrimitive()) {
                problems.add(path + ": " + raw.getSimpleName() + " field got " + json);
                return;
            }
            try {
                ApiFootballBase.gson().fromJson(json, raw);
            } catch (RuntimeException e) {
                problems.add(path + ": " + json + " cannot be read as " + raw.getSimpleName() + " (" + e.getMessage() + ")");
            }
        } else {
            if (!json.isJsonObject()) {
                problems.add(path + ": expected an object for " + raw.getSimpleName() + " but was " + json);
                return;
            }
            Map<String, Field> fields = jsonFields(raw);
            for (Map.Entry<String, JsonElement> e : json.getAsJsonObject().entrySet()) {
                Field field = fields.get(e.getKey());
                if (field == null) {
                    problems.add(path + "." + e.getKey() + ": no field in " + raw.getSimpleName());
                } else {
                    walk(e.getValue(), field.getGenericType(), path + "." + e.getKey(), problems);
                }
            }
        }
    }

    private static boolean isScalar(Class<?> raw) {
        return raw.isPrimitive() || raw == String.class || Number.class.isAssignableFrom(raw) || raw == Boolean.class;
    }

    /** Fields Gson binds, keyed by JSON name ({@code @SerializedName} value and alternates), superclasses included. */
    private static Map<String, Field> jsonFields(Class<?> type) {
        Map<String, Field> fields = new LinkedHashMap<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                int mods = field.getModifiers();
                if (Modifier.isStatic(mods) || Modifier.isTransient(mods) || field.isSynthetic()) continue;
                SerializedName name = field.getAnnotation(SerializedName.class);
                if (name == null) {
                    fields.putIfAbsent(field.getName(), field);
                } else {
                    fields.putIfAbsent(name.value(), field);
                    for (String alternate : name.alternate()) fields.putIfAbsent(alternate, field);
                }
            }
        }
        return fields;
    }

    private static Class<?> rawClass(Type type) {
        if (type instanceof Class) return (Class<?>) type;
        if (type instanceof ParameterizedType) return (Class<?>) ((ParameterizedType) type).getRawType();
        throw new IllegalArgumentException("Unsupported model type " + type);
    }

    private static Type typeArgument(Type type, int index) {
        if (!(type instanceof ParameterizedType)) {
            throw new IllegalArgumentException("Raw collection type " + type);
        }
        return ((ParameterizedType) type).getActualTypeArguments()[index];
    }

    // ------------------------------------------------------------------ fixture files

    private static Path fixtureDir() {
        URL url = ModelCoverageTest.class.getClassLoader().getResource("fixtures");
        if (url == null) throw new IllegalStateException("fixtures directory not on the test classpath");
        try {
            return Paths.get(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Set<String> fixtureFiles() throws IOException {
        try (Stream<Path> files = Files.list(fixtureDir())) {
            return files.map(p -> p.getFileName().toString())
                    .filter(name -> name.endsWith(".json"))
                    .collect(Collectors.toCollection(HashSet::new));
        }
    }

    private static JsonObject read(String name) throws IOException {
        String body = new String(Files.readAllBytes(fixtureDir().resolve(name)), StandardCharsets.UTF_8);
        return JsonParser.parseString(body).getAsJsonObject();
    }
}

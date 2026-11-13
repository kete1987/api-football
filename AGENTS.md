# API-Football Library - Agent Instructions

## Project overview

Java library (Java 11) that wraps the [API-Football](https://www.api-football.com/) v3 API (api-sports.io). Published on Maven Central as `es.com.kete1987:apifootball.library`. Licensed under Apache 2.0.

- Official documentation: https://www.api-football.com/documentation-v3
- Local endpoint reference: [docs/API_CATALOG.md](docs/API_CATALOG.md) — every endpoint, its parameters, its response tree and the quirks observed in real responses. **Read the relevant section before touching an endpoint.**

## Build

```
mvn test                 # unit tests (MockWebServer, no network)
mvn clean package        # compile + tests + jar
mvn verify "-Dgpg.skip"   # also builds the Javadoc and sources jars (must be free of errors)
mvn clean deploy         # publish to Maven Central (requires GPG key + credentials)
```

CI (`.github/workflows/ci.yml`) runs `mvn test` on pushes to master and on pull requests, publishes the JUnit report, and analyses the build with SonarCloud.

## Key structure

```
src/main/java/es/com/kete1987/apifootball/library/
├── ApiFootball.java           # facade: one accessor per sub-API, getStatus() shortcut, rate limits
├── ApiFootballBase.java       # shared HTTP: auth, Gson, envelope, errors, pagination, rate limits
├── AccountApi.java            # status
├── LeaguesApi.java            # timezone, countries, leagues, leagues/seasons, standings
├── TeamsApi.java              # teams, teams/statistics, teams/seasons, teams/countries, venues, coachs
├── FixturesApi.java           # fixtures, rounds, headtohead, statistics, events, lineups, players, injuries, predictions
├── PlayersApi.java            # players, seasons, profiles, squads, teams, top lists, transfers, trophies, sidelined
├── OddsApi.java               # odds, odds/mapping, odds/bookmakers, odds/bets, odds/live, odds/live/bets
├── RateLimitTracker.java      # rate-limit state shared by the sub-APIs of one facade
├── common/
│   ├── model/                 # Paging, Page<T>, RateLimit, TeamRef (the shared {id, name, logo})
│   ├── query/                 # Query (base of the parameter builders), QueryValidation
│   │                          # (dates, timezones, ranges, id lists, search terms)
│   └── util/                  # Constants, ApiFootballException, ModelCollections,
│                              # LenientNumberAdapters, NumberParsing, DateParsing, MediaUrls
├── competition/               # derived structure, no endpoint of its own (see below)
│   ├── model/                 # RoundName, Stage, CompetitionStructure, Tie (built, not parsed by Gson)
│   └── util/                  # Ties, Groups, StageTypes, StageStatuses, TieStates
└── <domain>/                  # account, leagues, teams, fixtures, players, odds
    ├── model/                 # Gson POJOs of that domain, with their null-safe helpers
    ├── query/                 # XxxQuery parameter builders of that domain
    └── util/                  # String constants and helpers that need no model: fixtures/util has
                               # FixtureStatuses, EventTypes, StatisticTypes, FixtureMinutes and
                               # FixtureEventAnalysis (helpers needing the whole event list);
                               # leagues/util StandingZones; odds/util BookmakerIds, PrematchBetIds, LiveBetIds
```

## API entry points

Plain constructors, no singletons. `ApiFootball` exposes one accessor per sub-API (`api.fixtures()`, `api.leagues()`, `api.teams()`, `api.players()`, `api.odds()`, `api.account()`), all sharing one HTTP client and one rate-limit tracker, plus a `getStatus()` shortcut and the rate-limit getters; it does not duplicate the other sub-API methods. Sub-APIs also work standalone (`new FixturesApi("KEY")`). Constructors taking an API key reject a `null` or blank key with `IllegalArgumentException` (the check lives in `buildHttpClient`).

Reference models are shared only when the JSON shape is identical: the `{id, name, logo}` team reference is `common.model.TeamRef` everywhere (richer team objects extend it), `{id, season}` in odds is `odds.model.LeagueSeasonRef`, and the statistics block of a prediction reuses `teams.model.SeasonStatistics` (the body of `/teams/statistics`). Where the API returns a different subset of fields (league references, player references...), each domain keeps its own small model.

**Naming.** Lookups by one key are `getXById` / `getXByTeam` / `getXByPlayer`...; league + season lookups are `getXByLeague(int league, int season)`; single-page methods end in `Page` (`getOddsByLeaguePage`); multi-page methods start with `getAll`. Java names say "Coaches" (`getTrophiesByCoaches`); `coachs` only appears in paths and query strings. Parameters use plain names (`team`, `player`, `league`, `id`), and id lists are `Collection<Integer>`.

## The response envelope

Every response is `{get, parameters, errors, results, paging, response}`. `ApiFootballBase` unwraps it; sub-APIs never declare `*Response` wrapper classes. Use the helper that matches the shape of `response`:

| Helper | Use when | Requests |
|---|---|---|
| `list(url, T.class)` | `response` is an array | 1 |
| `first(url, T.class)` | array, but the call targets one resource (e.g. by id); `null` when empty | 1 |
| `object(url, T.class)` | `response` is an object (e.g. `status`, `teams/statistics`); `null` when the API answers `[]` | 1 |
| `page(url, T.class, n)` | paginated endpoint, one page; returns `Page<T>` | 1 |
| `allPages(url, T.class, maxPages)` | paginated endpoint, up to `maxPages` pages (`maxPages` ≥ 1, there is no "unlimited") | 1 per page |

**Errors.** The API answers HTTP 200 with a non-empty `errors` member for most failures (wrong parameter, plan restriction, quota). `execute` turns both that and non-2xx responses into `ApiFootballException` (`getHttpCode()`, `getErrors()`); a non-envelope body such as `{"message": "..."}` (HTTP 499/500) is exposed under the key `message`, and bodies quoted in exception messages are cut to 500 characters. An empty body (HTTP 204) or a non-JSON body is an `ApiFootballException` too, and the typed helpers wrap Gson failures (`JsonParseException`, `IllegalStateException`, `NumberFormatException`) while mapping `response` into an `ApiFootballException` with the cause. So public methods throw only `IOException` (network), `ApiFootballException` (the API or its response) and `IllegalArgumentException` (invalid arguments, detected before any request).

**Rate limits.** Two independent limits, read from the headers of every response (including failed ones) and available right after any call:

```java
api.getRateLimit();                 // RateLimit snapshot of the last response
api.getDailyRequestsLimit();        // x-ratelimit-requests-limit
api.getDailyRequestsRemaining();    // x-ratelimit-requests-remaining
api.getMinuteRequestsLimit();       // X-RateLimit-Limit
api.getMinuteRequestsRemaining();   // X-RateLimit-Remaining
api.getRequestCount();              // responses received (timeouts and OkHttp retries not counted)
```

**Quota awareness is a design rule.** A library call must cost exactly one request unless its name and Javadoc say otherwise. For paginated endpoints always offer the single-page method (`...Page(query, page)`) next to the "all pages" one, and document the cost in the Javadoc. A method that sends more than one request is named for it: `getFixturesByIdsInBatches` (one request per 20 ids) states the cost in the first paragraph of its Javadoc, and validates the arguments of all batches before the first request.

## Conventions for sub-APIs

```java
public class FixturesApi extends ApiFootballBase {

    public FixturesApi(String apiKey) {
        this(buildHttpClient(apiKey), Constants.BASE_URL, new RateLimitTracker());
    }

    FixturesApi(OkHttpClient client, String baseUrl, RateLimitTracker tracker) {   // facade + tests
        super(client, baseUrl, tracker);
    }

    public List<Fixture> getFixtures(FixturesQuery query) throws IOException, ApiFootballException {
        return list(url("fixtures", query), Fixture.class);
    }

    public Fixture getFixtureById(int id) throws IOException, ApiFootballException {
        return first(param(url("fixtures"), "id", id), Fixture.class);
    }
}
```

- Both constructors above are mandatory, with exactly those signatures.
- **Validation.** Parameters the API silently misreads are checked client-side with `common.query.QueryValidation` and rejected with `IllegalArgumentException`: dates (real `YYYY-MM-DD`; every date lookup offers a `LocalDate` overload plus a validated `String` overload), timezones (exactly `"UTC"` or a region id with `/` that `ZoneId.of` knows, never an offset form such as `+02:00`, `GMT+1`, `Z`; the valid names are those of `/timezone`, and the API silently falls back to UTC on unknown ones), ranges (`last`/`next` 1-99) and id lists (1 to `MAX_IDS` = 20, no `null`). `page(...)` rejects page numbers below 1.
  - **Search terms.** Every `search` setter validates its term with `QueryValidation.search` (`QueryValidation.search(value, QueryValidation.PLAYERS_SEARCH_MIN_LENGTH)` for `/players`): only ASCII letters, digits and spaces, at least 3 characters (4 for `/players`; leading and trailing spaces do not count). Invalid terms throw `IllegalArgumentException` before any request, because the API answers errors with HTTP 200 and still counts them against the quota. `null` still removes the parameter. A convenience method that takes a search term (`searchCountries`, `searchTeams`...) must build its URL through the query's `search(...)` setter, never with a raw `param(...)`. `QueryValidation.toSearchTerm` is an explicit opt-in cleaner (diacritics via `java.text.Normalizer`, other characters dropped, spaces collapsed); setters never apply it silently. Tests that need the real API error for a bad search (`error_search_too_short.json`) build the query with the raw `param("search", ...)` escape hatch.
  - **Parameter combinations** the API answers with an error (which costs a request) are rejected in the API method, not in the query setters, because the order of setters must not matter: `FixturesApi.getFixtures` rejects `from`/`to` without `league` or `team`. Only reject what the catalog or a report shows to be an error. A combination missing from the catalog's list but verified live is recorded in the method's Javadoc and in catalog section 11.13 (`getFixturesByTeam(team, season)`, verified 2026-11-13).
- **Query setters.** Passing `null` to a setter removes the parameter (the contract of `Query.param`); no setter throws on `null`. Required arguments of API methods are validated in the API method instead. Enum-like filters the API expects in lower case are lower-cased (`LeaguesQuery.type`, `FixtureEventsQuery.type`).
- **Parameters.** Every endpoint with optional filters gets a `XxxQuery extends Query<XxxQuery>` in `<domain>/query/`, with one typed, Javadoc'd setter per documented parameter (`return param("league", league);`). The sub-API exposes a method taking the query, plus convenience overloads for the most common lookups (by id, by league + season...). Endpoints whose parameters are all required just take them as method arguments. Never send a parameter the caller did not set: the API rejects unknown or empty parameters.
- **Models.** Plain Gson POJOs under `<domain>/model/`, private fields, getters only, no setters. Use `@SerializedName` for snake_case or otherwise non-Java names. Use wrapper types (`Integer`, `Double`, `Boolean`) — the API returns `null` freely. List/Map getters return `ModelCollections.unmodifiable(...)`. Small objects used by a single parent may be static nested classes; objects shared across endpoints of the domain (team, league, player references...) get their own file and are reused, never duplicated.
- **Fields whose JSON type varies** (number / string / null, e.g. statistic values or `"50%"`) must be mapped to a type that cannot fail to parse — `String` for scalars, `JsonElement` for shape-shifting members — with typed convenience getters where useful. The catalog lists them.
- **Numeric strings.** `gson()` registers `LenientNumberAdapters`, so an `Integer`/`Long`/`Double` field accepts a number, a numeric string (`"187"`) or `""`/`null` (→ `null`). Integer fields reject fractional and out-of-range values (`JsonSyntaxException`, surfaced as `ApiFootballException`) instead of truncating or saturating. A value that can carry non-numeric text (`"37%"`, `"185 cm"`) must still be a `String`, with typed getters built on `common.util.NumberParsing` (`parseInteger`, `parseDouble`, `parsePercent`, `parseLeadingInteger`); those getters never throw and return `null` when the text does not parse.
- **Data-dependent keys** (minute buckets `"0-15"`, under/over `"0.5"`, the stray `""` key in `teams/statistics` cards) are `Map<String, X>`, never POJOs.
- **Parameter-dependent shapes** (e.g. `fixtures/rounds?dates=true`, `trophies?players=`) get a separate method with its own return type rather than one model trying to fit both.
- **Gson.** Never build a `Gson` instance; use `gson()` from `ApiFootballBase`. If a type adapter is needed, register it there.
- **Enumerated values** (fixture statuses, event types...) are exposed as `String` in models, with constants classes under `<domain>/util/` — the API adds values over time and an enum would break parsing.
- **Classification helpers** (`isGoal()`, `isLive()`, `getAccuracyValue()`...) must be decided on the field that carries the meaning, not on a coarse one: event `type` is coarse (a missed penalty is `type "Goal"`, `detail "Missed Penalty"`), status `long` is unreliable, a parsed number may be a count on one endpoint and a percentage on another. Before adding or changing one, list every documented and observed value of the underlying fields (catalog sections 8, 9 and the endpoint section) and state in the Javadoc what the helper answers for each, including the ones no fixture contains. `FixtureEventSemanticsTest` is the pattern: a truth table over every `type`/`detail` pair, plus a check that every `EventTypes` detail constant has a row.
- **Status.** `FixtureStatuses.isLive` follows the API's "In Play" group and includes `SUSP`/`INT`; keep that and its Javadoc warning. `isInterrupted`, `isPlaying` and `isPendingResolution` are the explicit variants, and `FixtureStatus` delegates to them. `FixtureHelpersStatusTest` is the truth table over all 19 documented codes (plus `null` and an unknown code). Other code that needs a status set (e.g. `Tie`) composes these methods instead of defining its own set.
- **Minute.** `FixtureMinutes` is the only place that combines `elapsed` and `extra`; `FixtureStatus` and `FixtureEvent.Time` delegate to it.
- **Helpers that need the surrounding list** to classify an event (second yellow) live in `fixtures/util/FixtureEventAnalysis` and are exposed through `Fixture` (`isSecondYellowRed`, `getCardEventsMerged`). A red card is a second yellow when the list holds a yellow of the **same player at the same minute** (`elapsed` and `extra`, null = 0); players are matched by id, by name when an id is null, and not at all when neither is available (then the red is reported as straight). Evidence: catalog 11.16.
- **Running score** (`Fixture.getRunningScore()`): goals of `getEventsSorted()` added to `getTeamGoalCountsFor()` (own goals need no special case), missed penalties excluded; the last step equals `goals` on the 1-1 `PEN` and the 0-1 own-goal fixtures. The fixture files are trimmed, so `fixtures_by_id.json` has fewer goal events than its `goals`: do not assert on it.
- **Odds helpers** never throw: `PrematchBetOdds.findValue(label)` (case-insensitive, trimmed), `getHomeOdd()/getDrawOdd()/getAwayOdd()` (1X2), `BookmakerOdds.findMatchWinner()/findExactScore()`, `OddValue.getExactScoreHome()/getExactScoreAway()` (the "Exact Score" bet, id 10, is `home:away`; other providers give winner-loser) and `PrematchOdds.getUpdateAsInstant()` return `null` when the data is missing or malformed. `odds.util.BookmakerIds` holds the well-known bookmaker ids; `OddsHelpersTest` checks the constants against the catalog's Appendix A and `odds_bookmakers.json`.
- **Parsing text from responses** (numbers, dates) goes through `common.util.NumberParsing` and `DateParsing`; a model or helper never carries its own private copy.
- Tests with data collected live use small inline JSON marked "recorded live" (no new fixture files).
- **Javadoc** every public method: what it returns, which parameters are required, and anything from the catalog a caller must know (update frequency, pagination cost, plan restrictions).

## Derived structure (`competition` package)

API-Football has no stage, leg, aggregate, tie winner or fixture group; `competition` rebuilds them from round texts, fixtures and standings. Rules for this package:

- Pure computation: no class here sends a request or takes an API object. Inputs are what the sub-APIs return (`List<String>` rounds, `List<RoundDates>`, `List<Fixture>`, `Standings`).
- Never throw on missing data: `null` inputs, `null` elements, missing teams, ids, dates or scores give `null`/empty/`UNKNOWN` answers. Unparsable dates are ignored.
- Never translate names. `"Round of 32"` means different things in different competitions; expose facts (stage type, number of ties, legs per tie) and let the caller label.
- Stage types come from the name (`StageTypes.ofStageName`, truth table in `CompetitionStructureTest` over every round text in the fixtures: a new round text in a fixture fails that test until it gets a row), plus one positional rule in `CompetitionStructure`: a knockout stage before the first league/group stage is qualifying.
- Ties: aggregate = sum of `goals` (extra time included); never `score.fulltime` (90 minutes) and never `score.extratime` (null in 3 of the 4 World Cup 2026 shoot-outs although extra time was played). A leg counts when its status is `FT`/`AET`/`PEN`/`AWD`/`WO` with both goals known; live legs count in the aggregate but the tie is not finished. Winner only for finished ties of one or two legs: higher aggregate, else the last leg's `score.penalty`, else (single match only) the match's `winner` flag. No away-goals rule. Abandoned/cancelled/postponed legs leave the tie unfinished; three or more fixtures for one pairing never get a winner.
- Logic that reads only one model lives on that model (`Season.isOver`, `Standings.getGroupOf`, `StandingEntry.getZoneCategory`); `competition` classes delegate to it (`Groups.groupOf` calls `Standings.getGroupOf`) instead of re-implementing it.

## Tests

JUnit 5 + OkHttp `MockWebServer`. Every test extends `BaseApiTest`, which starts the mock server and exposes `client`, `baseUrl` and `tracker`, so no test touches the network. Responses are JSON fixtures under `src/test/resources/fixtures/` — **real API responses** captured from the live API (arrays trimmed to a few elements, personal data anonymised). Do not hand-write or "fix" a fixture: if the model does not parse it, the model is wrong.

```java
class FixturesApiTest extends BaseApiTest {
    private FixturesApi api;

    @BeforeEach
    void setUp() {
        api = new FixturesApi(client, baseUrl, tracker);
    }

    @Test
    void getFixtureById_parsesFixture() throws Exception {
        enqueue("fixtures_by_id.json");

        Fixture fixture = api.getFixtureById(1208021);

        assertEquals("Match Finished", fixture.getFixture().getStatus().getLong());
        assertEquals("/fixtures?id=1208021", takeRequest().getPath());
    }
}
```

Each endpoint method needs at least one test that asserts (a) representative fields at every nesting level of the parsed fixture, including the type-varying ones, and (b) the request path and query string the library built.

`ModelCoverageTest` only proves that every JSON key has a field; it says nothing about what helpers answer. Shapes that no fixture contains (missed penalty, VAR, second yellow) are tested with small inline JSON in the test, marked as hand-written from the documented shape (or as recorded live, when it was). An own goal and a penalty during play are in real fixtures (`fixtures_by_id_own_goal.json`, `fixtures_by_id_penalty_shootout.json`), and `FixtureEventSemanticsTest` rebuilds the score from their events. Verified there: penalty shoot-out kicks are not events (only `score.penalty` carries the shoot-out), and an own goal's `team` is the team it counts for while its `player` is from the other team.

`CompetitionStructureTest` is the truth table for round texts (every round text in every fixture file must map to a row of `ROUND_TRUTH_TABLE` by stage name), and `TiesTest` validates the tie rules on the full Champions League 2025-26 and World Cup 2026 responses (`fixtures_by_league_ucl_2025.json` keeps every non-league-phase fixture and 4 league-phase ones) and holds the Sportmonks aggregates of the Champions League 2025-26 as hard-coded numbers (Sportmonks responses are never stored in this repository). Edge cases (live, abandoned, awarded legs) are real legs with their status and score edited in the test, marked as such.

`ModelCoverageTest` maps every fixture to the model type its API method parses into and walks the JSON: it fails on any key without a model field (honouring `@SerializedName`, superclasses, `List`/`Map`/`JsonElement` members), on any scalar Gson cannot read into the field's type, and on any fixture file that is neither mapped nor listed as an error fixture. A new fixture must be added there.

`ApiFootballIntegrationTest` is the exception: it hits the real API and is excluded from the default surefire run. It only runs under the `integration` profile and skips itself unless `API_FOOTBALL_KEY` is set:

```
API_FOOTBALL_KEY=... mvn test -Pintegration
```

It must stay cheap (currently 4 counted requests): every run spends real daily quota.

## Secrets

The API key never enters the repository: not in code, tests, fixtures, docs or commit messages. It travels in the `x-apisports-key` header, injected by an OkHttp interceptor in `buildHttpClient()`.

## Adding a new endpoint

1. Read its section in `docs/API_CATALOG.md` (and the official docs if it is new).
2. Capture a real response as a fixture (trimmed, anonymised) under `src/test/resources/fixtures/` and map it in `ModelCoverageTest`.
3. Add the model POJOs and, if it has optional filters, its `XxxQuery`.
4. Add the method to the sub-API that owns the domain (a new sub-API also needs its accessor in `ApiFootball`).
5. Add the test, update the catalog and the README endpoint table.

## Publishing a new version

1. Update `<version>` in `pom.xml`.
2. Run `mvn clean deploy` from IntelliJ (Maven panel → Lifecycle → deploy) or terminal.
3. Enter the GPG passphrase when prompted.
4. Wait for `BUILD SUCCESS` — the plugin polls Maven Central until the artifact is published.
5. Published at: https://central.sonatype.com/artifact/es.com.kete1987/apifootball.library

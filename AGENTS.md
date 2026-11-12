# API-Football Library - Agent Instructions

## Project overview

Java library (Java 11) that wraps the [API-Football](https://www.api-football.com/) v3 API (api-sports.io). Published on Maven Central as `es.com.kete1987:apifootball.library`. Licensed under Apache 2.0.

- Official documentation: https://www.api-football.com/documentation-v3
- Local endpoint reference: [docs/API_CATALOG.md](docs/API_CATALOG.md) — every endpoint, its parameters, its response tree and the quirks observed in real responses. **Read the relevant section before touching an endpoint.**

## Build

```
mvn test                 # unit tests (MockWebServer, no network)
mvn clean package        # compile + tests + jar
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
│   └── util/                  # Constants, ApiFootballException, ModelCollections,
│                              # LenientNumberAdapters, NumberParsing
└── <domain>/                  # account, leagues, teams, fixtures, players, odds
    ├── model/                 # Gson POJOs of that domain
    ├── query/                 # XxxQuery parameter builders of that domain
    └── util/                  # String constants (statuses, event types, bet ids...)
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

**Quota awareness is a design rule.** A library call must cost exactly one request unless its name and Javadoc say otherwise. For paginated endpoints always offer the single-page method (`...Page(query, page)`) next to the "all pages" one, and document the cost in the Javadoc.

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
- **Query setters.** Passing `null` to a setter removes the parameter (the contract of `Query.param`); no setter throws on `null`. Required arguments of API methods are validated in the API method instead. Enum-like filters the API expects in lower case are lower-cased (`LeaguesQuery.type`, `FixtureEventsQuery.type`).
- **Parameters.** Every endpoint with optional filters gets a `XxxQuery extends Query<XxxQuery>` in `<domain>/query/`, with one typed, Javadoc'd setter per documented parameter (`return param("league", league);`). The sub-API exposes a method taking the query, plus convenience overloads for the most common lookups (by id, by league + season...). Endpoints whose parameters are all required just take them as method arguments. Never send a parameter the caller did not set: the API rejects unknown or empty parameters.
- **Models.** Plain Gson POJOs under `<domain>/model/`, private fields, getters only, no setters. Use `@SerializedName` for snake_case or otherwise non-Java names. Use wrapper types (`Integer`, `Double`, `Boolean`) — the API returns `null` freely. List/Map getters return `ModelCollections.unmodifiable(...)`. Small objects used by a single parent may be static nested classes; objects shared across endpoints of the domain (team, league, player references...) get their own file and are reused, never duplicated.
- **Fields whose JSON type varies** (number / string / null, e.g. statistic values or `"50%"`) must be mapped to a type that cannot fail to parse — `String` for scalars, `JsonElement` for shape-shifting members — with typed convenience getters where useful. The catalog lists them.
- **Numeric strings.** `gson()` registers `LenientNumberAdapters`, so an `Integer`/`Long`/`Double` field accepts a number, a numeric string (`"187"`) or `""`/`null` (→ `null`). Integer fields reject fractional and out-of-range values (`JsonSyntaxException`, surfaced as `ApiFootballException`) instead of truncating or saturating. A value that can carry non-numeric text (`"37%"`, `"185 cm"`) must still be a `String`, with typed getters built on `common.util.NumberParsing` (`parseInteger`, `parseDouble`, `parsePercent`, `parseLeadingInteger`); those getters never throw and return `null` when the text does not parse.
- **Data-dependent keys** (minute buckets `"0-15"`, under/over `"0.5"`, the stray `""` key in `teams/statistics` cards) are `Map<String, X>`, never POJOs.
- **Parameter-dependent shapes** (e.g. `fixtures/rounds?dates=true`, `trophies?players=`) get a separate method with its own return type rather than one model trying to fit both.
- **Gson.** Never build a `Gson` instance; use `gson()` from `ApiFootballBase`. If a type adapter is needed, register it there.
- **Enumerated values** (fixture statuses, event types...) are exposed as `String` in models, with constants classes under `<domain>/util/` — the API adds values over time and an enum would break parsing.
- **Javadoc** every public method: what it returns, which parameters are required, and anything from the catalog a caller must know (update frequency, pagination cost, plan restrictions).

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

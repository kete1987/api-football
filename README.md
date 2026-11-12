# API-Football Library

Java library (Java 11+) for the [API-Football](https://www.api-football.com/) v3 API (api-sports.io). It covers every documented endpoint, parses responses into typed models, and exposes the rate-limit headers after every call so you always know how much quota you have left.

> This is an unofficial, community library. It is not affiliated with API-Football / API-Sports.

## Installation

```xml
<dependency>
    <groupId>es.com.kete1987</groupId>
    <artifactId>apifootball.library</artifactId>
    <version>1.0.0</version>
</dependency>
```

You need an API key from a direct [api-sports.io](https://dashboard.api-football.com/) subscription.

## Quick start

```java
ApiFootball api = new ApiFootball("YOUR_API_KEY");

// All fixtures in play right now, with their events (1 request)
List<Fixture> live = api.fixtures().getLiveFixtures();

// La Liga table for the 2025-2026 season
Standings standings = api.leagues().getStandings(140, 2025);
for (StandingEntry entry : standings.getTable()) {
    System.out.println(entry.getRank() + ". " + entry.getTeam().getName() + " " + entry.getPoints());
}

// A finished fixture with events, lineups, statistics and player ratings embedded (1 request)
Fixture fixture = api.fixtures().getFixtureById(1570443);

// Anything the API can filter by is available through a query builder
List<Fixture> fixtures = api.fixtures().getFixtures(new FixturesQuery()
        .league(140)
        .season(2025)
        .from(LocalDate.of(2026, 3, 1))
        .to(LocalDate.of(2026, 3, 31))
        .status(FixtureStatuses.FINISHED_GROUP)
        .timezone("Europe/Madrid"));
```

Create one `ApiFootball` per API key and reuse it. Each sub-API can also be used on its own, e.g. `new FixturesApi("YOUR_API_KEY")`.

## Rate limits

API-Football applies a daily quota and a per-minute limit, and reports both in the headers of every response. The library keeps the values of the last response:

```java
api.getDailyRequestsLimit();        // x-ratelimit-requests-limit      e.g. 7500
api.getDailyRequestsRemaining();    // x-ratelimit-requests-remaining  e.g. 7412
api.getMinuteRequestsLimit();       // X-RateLimit-Limit               e.g. 300
api.getMinuteRequestsRemaining();   // X-RateLimit-Remaining           e.g. 297
api.getRateLimit();                 // the four values as one immutable snapshot
api.getRequestCount();              // HTTP requests sent by this ApiFootball instance
api.getStatus();                    // account, plan and requests used today (free of quota)
```

Things worth knowing (observed against the live API):

- **Errors and empty results consume quota** exactly like successful calls. Only `status` is free.
- The daily "remaining" header is refreshed with some delay and can briefly go *up*; `getRequestCount()` is exact for what this instance has sent.
- **Every library method costs exactly one request**, except the `getAll...(…, maxPages)` methods of paginated endpoints, which cost one request per page and always require an explicit cap.

## Endpoints

| Sub-API | Accessor | API endpoints |
|---|---|---|
| `AccountApi` | `api.account()` | `status` |
| `LeaguesApi` | `api.leagues()` | `timezone`, `countries`, `leagues`, `leagues/seasons`, `standings` |
| `TeamsApi` | `api.teams()` | `teams`, `teams/statistics`, `teams/seasons`, `teams/countries`, `venues`, `coachs` |
| `FixturesApi` | `api.fixtures()` | `fixtures`, `fixtures/rounds`, `fixtures/headtohead`, `fixtures/statistics`, `fixtures/events`, `fixtures/lineups`, `fixtures/players`, `injuries`, `predictions` |
| `PlayersApi` | `api.players()` | `players`, `players/seasons`, `players/profiles`, `players/squads`, `players/teams`, `players/topscorers`, `players/topassists`, `players/topyellowcards`, `players/topredcards`, `transfers`, `trophies`, `sidelined` |
| `OddsApi` | `api.odds()` | `odds`, `odds/mapping`, `odds/bookmakers`, `odds/bets`, `odds/live`, `odds/live/bets` |

Every documented query parameter is available, either as a method argument or through the endpoint's `XxxQuery` builder. [docs/API_CATALOG.md](docs/API_CATALOG.md) describes each endpoint, its parameters, its response and the differences found between the official documentation and real responses.

## Pagination

Only four endpoints are paginated: `players` (20 per page), `players/profiles` (250), `odds` (10) and `odds/mapping` (100). Each has a single-page method and a capped multi-page method:

```java
// One page = one request; the Page tells you how many pages exist
Page<PlayerWithStatistics> page = api.players().getPlayersByLeaguePage(140, 2025, 1);
int totalPages = page.getPaging().getTotal();

// Up to 5 pages = up to 5 requests
List<PlayerWithStatistics> players =
        api.players().getAllPlayers(new PlayersQuery().league(140).season(2025), 5);
```

There is no "fetch everything" without a cap on purpose: `players/profiles` without filters is more than 2,700 pages.

## Error handling

All methods throw `IOException` (network) and `ApiFootballException` (the API rejected the request). API-Football reports most errors with HTTP 200 and an `errors` object in the body; the library turns those into exceptions too:

```java
try {
    api.teams().searchTeams("ma");
} catch (ApiFootballException e) {
    e.getHttpCode();   // 200
    e.getErrors();     // {search=The Search field must be at least 3 characters in length.}
}
```

Methods that look up a single resource return `null` when the API has no data for it.

Parameters the API would silently misinterpret are validated before the request is sent and throw `IllegalArgumentException`: an impossible date such as `2026-13-45` (the API would return another day's fixtures), an unknown timezone (the API would fall back to UTC), too many ids, and similar.

## Data notes

- Seasons are the starting year: `2025` is the 2025-2026 season.
- Values the API returns with mixed types (`"37%"`, `"1.71"`, `5`, `null`) are exposed as `String` with typed helpers that never throw, e.g. `statistic.getIntValue()`, `oddValue.getOddAsDouble()`.
- Fixture statuses, event types and similar enumerations are `String` constants (`FixtureStatuses`, `EventTypes`, `StatisticTypes`...), so a new value added by the API never breaks parsing.
- Pre-match bet ids and in-play bet ids are different id spaces (`PrematchBet` / `LiveBet`).

## Building and testing

```
mvn test                                         # unit tests, no network
API_FOOTBALL_KEY=... mvn test -Pintegration      # 4 real requests against the API
```

Unit tests run against real API responses stored under `src/test/resources/fixtures/`. See [AGENTS.md](AGENTS.md) for the architecture and the conventions for contributing.

## License

[Apache License 2.0](LICENSE)

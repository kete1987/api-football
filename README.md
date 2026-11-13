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
api.getRequestCount();              // responses received by this ApiFootball instance
api.getStatus();                    // account, plan and requests used today (free of quota)
```

Things worth knowing (observed against the live API):

- **Errors and empty results consume quota** exactly like successful calls. Only `status` is free.
- The daily "remaining" header is refreshed with some delay and can briefly go *up*; `getRequestCount()` counts the responses this instance has received (a timeout, or a retry OkHttp performs on its own, is not counted).
- **Every library method costs exactly one request**, except the `getAll...(…, maxPages)` methods of paginated endpoints, which cost one request per page and always require an explicit cap, and `getFixturesByIdsInBatches`, which costs `ceil(n / 20)` requests (20 ids = 1, 21 ids = 2; duplicates are sent once).
- Parameter combinations the API answers with an error cost a request too, so the library rejects them before sending: `getFixtures` throws `IllegalArgumentException` when `from`/`to` are set without `league` or `team`.

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

## Helpers

Beyond the endpoints, the models carry helpers that never throw (they return `null`, an empty list or `false` when the data is missing) and never send a request. Details and the evidence behind them are in the Javadoc and in [docs/API_CATALOG.md](docs/API_CATALOG.md).

### Fixtures

More lookups on `FixturesApi`, each one request unless stated: `getFixturesByLeague(league, season, from, to[, timezone])`, `getFixturesByTeam(team, season[, from, to])`, `getNextFixtures` / `getLastFixtures(team, count[, timezone])`, `getNextFixturesByLeague` / `getLastFixturesByLeague(league, count[, timezone])` (dates as `LocalDate` or validated `String`), and `getFixturesByIdsInBatches(ids)` for more than 20 ids (`ceil(n / 20)` requests).

**Status and minute.** Classify by the `short` code with `FixtureStatus` / `FixtureStatuses`. `isLive()` follows the API's "In Play" group and therefore **includes `SUSP` (suspended) and `INT` (interrupted)**: check `isInterrupted()` first, or use `isPlaying()` (live without them; still true at `HT`/`BT`). `isPendingResolution()` (`PST, SUSP, INT, CANC, ABD, AWD, WO`) suits "postponed-like" screens. `getDisplayMinute()` (`94`) and `getDisplayMinuteText()` (`"90+4"`) combine `elapsed` and `extra`, on the status and on `FixtureEvent.Time`; the API's minute runs about one ahead of Sportmonks and the extra-time minute is unreliable (catalog section 8).

```java
FixtureStatus status = fixture.getFixture().getStatus();
String shown = status.isInterrupted() ? "Interrupted"
        : status.isPlaying() ? status.getDisplayMinuteText() + "'" : status.getShort();
```

**Cards and score.** A second yellow arrives as a yellow and a red card at the same minute; `getCardEventsMerged()` returns one entry per dismissal and `isSecondYellowRed(card)` tells the two kinds of red apart. `getRunningScore()` gives the score after each goal.

```java
Fixture f = api.fixtures().getFixtureById(1644363);
for (FixtureEvent card : f.getCardEventsMerged()) {          // chronological
    boolean secondYellow = f.isSecondYellowRed(card);
}
for (Fixture.ScoreStep step : f.getRunningScore()) {
    System.out.println(step.getEvent().getTime().getDisplayMinuteText() + " " + step.getText());
}
```

**Lookups inside a fixture** (null-safe): `findStatistics(teamId)`, `getHomeStatistics()`, `getAwayStatistics()`, `findLineup(teamId)`, `getHomeLineup()`, `getAwayLineup()`, `findPlayers(teamId)`, `getHomePlayers()`, `getAwayPlayers()`, `findPlayerName(playerId)`; `FixtureStatistics.getIntValue("Corner Kicks", 0)`.

**Images.** `common.util.MediaUrls` builds the media URLs from ids (`playerPhoto`, `teamLogo`, `leagueLogo`, `venueImage`, `coachPhoto`, `countryFlag`), and `getPhotoUrl()` on `PlayerRef` and `LineupPlayer` does it for event and lineup players, which carry no photo. Images do not count against the daily quota.

**Lineup grid.** `grid` is `"row:column"`: row 1 is the goalkeeper, then the lines from back to front, and the column numbers the players of that row. There are no pitch coordinates; your app places the dots. **Compared with Sportmonks, API-Football's columns of the home team are mirrored** (checked on every starter of 5 matches: rows always agree; away columns agree; home columns agree only after mirroring). The fix, for the home team only, is `column' = playersInRow + 1 - column`; it needs the full XI. `Fixture.getGridColumnAsSportmonks` applies it for you (home mirrored, away unchanged):

```java
Fixture f = api.fixtures().getFixtureById(1528960);
FixtureLineup home = f.getHomeLineup();
for (FixtureLineup.Entry entry : home.getStartXI()) {
    FixtureLineup.LineupPlayer p = entry.getPlayer();
    Integer row = p.getGridRow();                                  // 2
    Integer apiColumn = p.getGridColumn();                         // 4 (Kayode, "2:4")
    Integer column = f.getGridColumnAsSportmonks(home, p);         // 1: mirrored in a row of 4
}
```

The formula, the evidence and a worked example with real values (Italy, 4-3-3) are in [catalog section 11.17](docs/API_CATALOG.md#1117-get-fixtureslineups).

### Competition structure

API-Football has no stage entity, no leg, aggregate or tie-winner fields and no group on a fixture: a competition is a list of round texts (`"League Stage - 5"`, `"Round of 16"`) and a list of fixtures. The `competition` package rebuilds the structure from them, with no extra requests.

**Stages.** `CompetitionStructure` groups the rounds of `getRounds` / `getRoundsWithDates` into ordered `Stage`s: name, type (`StageTypes.LEAGUE`, `GROUP`, `KNOCKOUT`, `QUALIFYING`, `UNKNOWN`), rounds, matchdays and, with dates, first/last date and a status relative to a date (`StageStatuses`). `groupByRound(fixtures)` and `groupByStage(fixtures)` order a whole season's fixtures by the official round order. Stage names are the API's text and are never translated: `"Round of 32"` is a two-legged play-off in the Champions League 2025-26 but a single-match round in the World Cup 2026, so label rounds from facts (`Ties.legsPerTie(...)`).

```java
CompetitionStructure cl = CompetitionStructure.fromRoundDates(api.fixtures().getRoundsWithDates(2, 2025));
for (Stage stage : cl.getStages()) {
    System.out.println(stage.getName() + " " + stage.getType() + " " + stage.getStatus(LocalDate.now()));
}
Stage active = cl.getActiveStage(LocalDate.now());   // in play, or the next one in a break; null once over
Map<String, List<Fixture>> byRound = cl.groupByRound(api.fixtures().getFixturesByLeague(2, 2025));
```

**Ties.** `Ties` pairs knockout fixtures into `Tie`s (same round, same two teams, legs ordered by kick-off): aggregate per team (sum of `goals`, which includes extra time), state, the team that goes through and whether it was decided on penalties. Single matches use the same model. Pass every fixture of the round: a two-legged tie with a missing leg looks like a finished single match. Do not use `FixtureTeam.getWinner()` of a second leg as the tie winner: it is per match.

```java
List<Fixture> all = api.fixtures().getFixturesByLeague(2, 2025);   // 1 request
for (Tie tie : Ties.ofEliminationRounds(all)) {
    System.out.println(tie.getRound() + ": " + tie.getTeamA().getName() + " " + tie.getAggregateA()
            + "-" + tie.getAggregateB() + " " + tie.getTeamB().getName()
            + (tie.getWinner() == null ? "" : " -> " + tie.getWinner().getName())
            + (tie.isDecidedOnPenalties() ? " (penalties)" : ""));
}
```

**Groups, standings and seasons.** `Standings.getGroupTables()` returns the real tables and `getCrossGroupRankings()` the tables that rank teams across groups (the World Cup 2026 response has 13 tables: Groups A-L plus a ranking of the 12 third-placed teams); `Standings.getGroupOf(teamId)` and `Groups.groupOf(standings, fixture)` give a group-stage fixture its group. `StandingEntry.getZoneCategory()`, `getZoneTarget()`, `isPromotion()` and `isRelegation()` read the `description` (`"Promotion - Champions League (League phase)"`). A season stays `current: true` after it ends, so use `Season.hasStarted(date)`, `isOver(date)`, `isInProgress(date)` and `League.getSeasonInProgress(date)`; `end` is the last fixture the API knows, not a promise that nothing is pending. `getCurrentRound` returns `null` once the season is over and the **next** round during a break.

### Odds

```java
PrematchOdds odds = api.odds().getOddsByFixture(1570461);          // by fixture id, not by date
BookmakerOdds bet365 = odds.findBookmaker(BookmakerIds.BET365);    // may be null
PrematchBetOdds winner = bet365.findMatchWinner();
Double home = winner.getHomeOdd();                                  // null when absent
OddValue score = bet365.findExactScore().findValue("1:0");
Integer homeGoals = score.getExactScoreHome();                      // order is home:away
Instant updated = odds.getUpdateAsInstant();                        // one time per fixture
```

Pre-match odds exist only 1 to 14 days before the match (with 7 days of history); outside that window `getOddsByFixture` returns `null`. Request odds by fixture id: the `date` that `/odds` reports can be provisional, and filtering by the real date can return nothing. `update` is one timestamp per fixture, not per bookmaker. Some bookmakers of the catalogue (888Sport, Interwetten) rarely or never appear in the odds of a fixture.

## Pagination

Only four endpoints are paginated: `players` (20 per page), `players/profiles` (250), `odds` (10) and `odds/mapping` (100). Each has a single-page method and a capped multi-page method:

```java
// One page = one request; the Page tells you how many pages exist (pages start at 1)
Page<PlayerWithStatistics> page = api.players().getPlayersByLeaguePage(140, 2025, 1);
Integer totalPages = page.getPaging().getTotal();

// Up to 5 pages = up to 5 requests
List<PlayerWithStatistics> players =
        api.players().getAllPlayers(new PlayersQuery().league(140).season(2025), 5);

// Single-page methods end in "Page"; league + season lookups are "...ByLeague"
Page<PrematchOdds> odds = api.odds().getOddsByLeaguePage(140, 2025, 1);
List<PrematchOdds> allOdds = api.odds().getAllOddsByLeague(140, 2025, 3);
```

There is no "fetch everything" without a cap on purpose: `players/profiles` without filters is more than 2,700 pages. If a later page of a multi-page walk fails, the exception is thrown and the pages already fetched are lost.

## Error handling

Methods throw exactly three exceptions:

- `IOException`: the network failed.
- `ApiFootballException`: the API rejected the request, or its response could not be used (an empty body such as HTTP 204, a body that is not JSON, or a `response` that does not fit the model; the parse error is kept as the cause).
- `IllegalArgumentException`: an argument is invalid; it is thrown before any request is sent.

API-Football reports most errors with HTTP 200 and an `errors` object in the body; the library turns those into exceptions too. Bodies that are not an envelope, such as `{"message": "..."}` with HTTP 499/500, expose their text under the key `message`:

```java
try {
    api.teams().searchTeams("ma");
} catch (ApiFootballException e) {
    e.getHttpCode();   // 200
    e.getErrors();     // {search=The Search field must be at least 3 characters in length.}
}
```

Methods that look up a single resource return `null` when the API has no data for it.

Parameters the API would silently misinterpret are validated before the request is sent (see `QueryValidation`):

- Dates must be real `YYYY-MM-DD` dates: `2026-13-45` is rejected (the API would return another day's fixtures). Date lookups take a `LocalDate` or a validated `String`.
- Timezones must be `"UTC"` or a region name such as `"Europe/Madrid"`, i.e. the names returned by `api.leagues().getTimezones()`; offsets such as `"+02:00"` or `"GMT+1"` are rejected (the API would silently fall back to UTC).
- Id lists (`Collection<Integer>`) take 1 to 20 ids and no `null`.
- Page numbers start at 1; `last`/`next` are 1 to 99.
- **Search terms** (teams, venues, coaches, leagues, countries, players, player profiles, bookmakers, bets) may contain only ASCII letters, digits and spaces and need at least 3 characters (4 for `/players`), because an invalid search still costs a request. To search text typed by a user, convert it first with the explicit helper `QueryValidation.toSearchTerm`, which strips accents, drops other symbols and collapses spaces: `api.teams().searchTeams(QueryValidation.toSearchTerm("Leganés"))` searches `"Leganes"`.

In the `XxxQuery` builders, passing `null` to a setter removes that parameter.

```java
List<Fixture> today = api.fixtures().getFixturesByDate(LocalDate.now(), "Europe/Madrid");
List<Fixture> season = api.fixtures().getFixturesByLeague(140, 2025);
List<Team> teams = api.teams().getTeamsByLeague(140, 2025);
Squad squad = api.players().getSquadByTeam(541);
Player profile = api.players().getPlayerProfileById(276);
List<TrophyGroup> trophies = api.players().getTrophiesByCoaches(Arrays.asList(2462, 1584));
TeamStatistics stats = api.teams().getTeamStatistics(140, 2025, 541, LocalDate.of(2026, 3, 1));
```

## Data notes

- Seasons are the starting year: `2025` is the 2025-2026 season.
- Values the API returns with mixed types (`"37%"`, `"1.71"`, `5`, `null`) are exposed as `String` with typed helpers that never throw, e.g. `statistic.getIntValue()`, `oddValue.getOddAsDouble()`, `player.getHeightCm()`. They are built on `NumberParsing`, which you can also use directly (`parseInteger`, `parseDouble`, `parsePercent`, `parseLeadingInteger`).
- `Integer`/`Long` fields accept numbers, numeric strings and `""` (read as `null`); a value that does not fit (e.g. out of the `int` range) is reported as `ApiFootballException`, never silently changed.
- The `{id, name, logo}` team reference is one class everywhere, `common.model.TeamRef`; the statistics block of a prediction is the same `SeasonStatistics` that `getTeamStatistics` returns.
- Fixture statuses, event types and similar enumerations are `String` constants (`FixtureStatuses`, `EventTypes`, `StatisticTypes`...), so a new value added by the API never breaks parsing.
- Match events have a coarse `type` and a `detail` that says what happened: a **missed penalty is `type "Goal"`** with `detail "Missed Penalty"`. Classify with the `FixtureEvent` helpers rather than `getType()`: `isGoal()` (excludes missed penalties), `isPenaltyGoal()`, `isOwnGoal()`, `isMissedPenalty()`, `isYellowCard()`, `isRedCard()`, `isVar()`. For substitutions the API's `player` is the player going off and `assist` the one coming on; `getPlayerOut()` / `getPlayerIn()` name them. An **own goal** is listed under the team it counts for, while its `player` plays for the other team: attribute goals with `getTeamGoalCountsFor()`. **Penalty shoot-out kicks are not events** (verified on a real `PEN` fixture); the shoot-out is only in `fixture.getScore().getPenalty()`, so counting `isGoal()` events per `getTeamGoalCountsFor()` gives `fixture.getGoals()` (extra time included, shoot-out excluded). For the score, still prefer `fixture.getGoals()`: during a live match events can briefly appear and vanish. The API does **not** return events in chronological order (a card at `90+6` can precede a substitution at `90`): `fixture.getEventsSorted()` returns a new, stably sorted list (same minute keeps the API's order, so a second yellow stays before its red), and `FixtureEvent.BY_TIME` is the comparator, e.g. to sort the result of `getFixtureEvents(id)`; `getEvents()` keeps the API's order.
- Pre-match bet ids and in-play bet ids are different id spaces (`PrematchBet` / `LiveBet`).
- `players/topscorers` (and the other three top lists) returns at most 20 players; there is no pagination to get more.

## Building and testing

```
mvn test                                         # unit tests, no network
API_FOOTBALL_KEY=... mvn test -Pintegration      # 4 real requests against the API
```

Unit tests run against real API responses stored under `src/test/resources/fixtures/`; `ModelCoverageTest` checks that every JSON key and value of those responses is mapped by the models. See [AGENTS.md](AGENTS.md) for the architecture and the conventions for contributing.

## License

[Apache License 2.0](LICENSE)

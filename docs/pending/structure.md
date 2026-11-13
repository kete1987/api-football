# Pending documentation: competition structure helpers

Branch `helpers-structure`. Text to merge into `README.md`, `AGENTS.md` and `docs/API_CATALOG.md`, plus two Javadoc changes for files this branch does not own. Each block below gives where it goes and the exact text to add.

---

## 1. README.md

### 1.1 New section, after "## Data notes" (before "## Building and testing")

````markdown
## Competition structure

API-Football has no stage entity, no leg/aggregate/tie-winner fields and no group on a fixture: a competition is a list of round texts (`"League Stage - 5"`, `"Round of 16"`) and a list of fixtures. The `competition` package rebuilds the structure from them, with no extra requests.

**Stages.** `CompetitionStructure` groups the rounds of `getRounds` / `getRoundsWithDates` into ordered `Stage`s: name, type (`StageTypes.LEAGUE`, `GROUP`, `KNOCKOUT`, `QUALIFYING`, `UNKNOWN`), rounds, matchdays and, with dates, first/last date and a status relative to a date (`StageStatuses.FINISHED`, `CURRENT`, `UPCOMING`, or `UNKNOWN` without dates). `groupByRound(fixtures)` and `groupByStage(fixtures)` order a whole season's fixtures (one `getFixturesByLeague` request) by the official round order.

```java
CompetitionStructure cl = CompetitionStructure.fromRoundDates(api.fixtures().getRoundsWithDates(2, 2025));
for (Stage stage : cl.getStages()) {
    System.out.println(stage.getName() + " " + stage.getType() + " " + stage.getStatus(LocalDate.now()));
}
Stage active = cl.getActiveStage(LocalDate.now());   // stage in play, or the next one in a break; null once over
Map<String, List<Fixture>> byRound = cl.groupByRound(api.fixtures().getFixturesByLeague(2, 2025));
```

Stage names are the API's text and are never translated: `"Round of 32"` is the two-legged league-phase play-off in the Champions League 2025-26 but a single-match round of 32 in the World Cup 2026. Label rounds from facts instead: the number of ties and `Ties.legsPerTie(...)`.

**Ties.** `Ties` pairs knockout fixtures into `Tie`s (same round, same two teams, legs ordered by kick-off): legs, aggregate per team (sum of `goals`, which includes extra time), state (`TieStates.NOT_STARTED`, `FIRST_LEG_PLAYED`, `IN_PLAY`, `FINISHED`), the team that goes through, and whether it was decided on penalties or in extra time. Single matches (finals, World Cup knockouts) use the same model. Pass every fixture of the round: a two-legged tie whose second leg is missing from the input looks like a finished single match.

```java
List<Fixture> all = api.fixtures().getFixturesByLeague(2, 2025);   // 1 request
for (Tie tie : Ties.ofEliminationRounds(all)) {
    System.out.println(tie.getRound() + ": " + tie.getTeamA().getName() + " " + tie.getAggregateA()
            + "-" + tie.getAggregateB() + " " + tie.getTeamB().getName()
            + (tie.getWinner() == null ? "" : " -> " + tie.getWinner().getName())
            + (tie.isDecidedOnPenalties() ? " (penalties)" : ""));
}
```

Do not use `FixtureTeam.getWinner()` of a second leg as the tie winner: it is per match (Juventus "won" the second leg of the Champions League 2025-26 play-off 3-2, but Galatasaray went through 7-5). Validated against Sportmonks' aggregates on all 68 two-legged ties of the Champions League 2025-26, and against the API's `winner` on the 32 World Cup 2026 knockout matches.

**Groups and standings.** `Standings.getGroupTables()` returns the real tables and `getCrossGroupRankings()` the tables that rank teams across groups (the World Cup 2026 response has 13 tables: Group A-L plus a "Group Stage" ranking of the 12 third-placed teams). `Standings.getGroupOf(teamId)` and `Groups.groupOf(standings, fixture)` / `Groups.byGroup(...)` give a group-stage fixture its group. `StandingEntry.getZoneCategory()`, `getZoneTarget()`, `isPromotion()` and `isRelegation()` read the `description` (`"Promotion - Champions League (League phase)"`, `"Relegation - LaLiga2"`); the raw text stays in `getDescription()`.

**Seasons.** A season stays `current: true` after it ends (the World Cup 2026 still is in November 2026). Use `Season.hasStarted(date)`, `isOver(date)`, `isInProgress(date)` and `League.getSeasonInProgress(date)`. `end` is the last fixture the API knows, so check pending fixtures before treating a season as finished for good.
````

### 1.2 "## Data notes": add one bullet at the end

```markdown
- `players/topscorers` (and the other top lists) returns at most 20 players; there is no pagination to get more.
```

---

## 2. AGENTS.md

### 2.1 "## Key structure": add to the tree, after the `common/` block

```
├── competition/               # derived structure, no endpoint of its own
│   ├── model/                 # RoundName, Stage, CompetitionStructure, Tie (built, not parsed by Gson)
│   └── util/                  # Ties, Groups, StageTypes, StageStatuses, TieStates
```

### 2.2 New section after "## Conventions for sub-APIs" (before "## Tests")

```markdown
## Derived structure (`competition` package)

API-Football has no stage, leg, aggregate, tie winner or fixture group; `competition` rebuilds them from round texts, fixtures and standings. Rules for this package:

- Pure computation: no class here sends a request or takes an API object. Inputs are what the sub-APIs return (`List<String>` rounds, `List<RoundDates>`, `List<Fixture>`, `Standings`).
- Never throw on missing data: `null` inputs, `null` elements, missing teams, ids, dates or scores give `null`/empty/`UNKNOWN` answers. Unparsable dates are ignored.
- Never translate names. `"Round of 32"` means different things in different competitions; expose facts (stage type, number of ties, legs per tie) and let the caller label.
- Stage types come from the name (`StageTypes.ofStageName`, truth table in `CompetitionStructureTest` over every round text in the fixtures: a new round text in a fixture fails that test until it gets a row), plus one positional rule in `CompetitionStructure`: a knockout stage before the first league/group stage is qualifying.
- Ties: aggregate = sum of `goals` (extra time included); never `score.fulltime` (90 minutes) and never `score.extratime` (null in 3 of the 4 World Cup 2026 shoot-outs although extra time was played). A leg counts when its status is `FT`/`AET`/`PEN`/`AWD`/`WO` with both goals known; live legs count in the aggregate but the tie is not finished. Winner only for finished ties of one or two legs: higher aggregate, else the last leg's `score.penalty`, else (single match only) the match's `winner` flag. No away-goals rule. Abandoned/cancelled/postponed legs leave the tie unfinished; three or more fixtures for one pairing never get a winner.
- `TiesTest` holds the Champions League 2025-26 aggregates of Sportmonks as hard-coded numbers (Sportmonks responses are never stored in this repository).
```

### 2.3 "## Tests": add after the paragraph about `FixtureEventSemanticsTest`

```markdown
`CompetitionStructureTest` is the truth table for round texts (every round text in every fixture file must map to a row of `ROUND_TRUTH_TABLE` by stage name), and `TiesTest` validates the tie rules on the full Champions League 2025-26 and World Cup 2026 responses (`fixtures_by_league_ucl_2025.json` keeps every non-league-phase fixture and 4 league-phase ones). Edge cases (live, abandoned, awarded legs) are real legs with their status and score edited in the test, marked as such.
```

---

## 3. docs/API_CATALOG.md

### 3.1 Section 11.4 `GET /leagues`: add after the response tree

```markdown
Real: `seasons[].current` stays `true` after the season ends: World Cup 2026 (`id=1`) is `current: true` with `end` `2026-07-19` on 2026-11-13. `end` is the date of the last fixture known, not the planned end: Champions League 2026 (`id=2`) reads `start 2026-07-07`, `end 2027-01-27` (end of the league phase) while its knockout rounds are not scheduled. Library: `Season.hasStarted/isOver/isInProgress(LocalDate)`, `League.getSeasonInProgress(LocalDate)`. Fixtures: `leagues_by_id_world_cup.json`, `leagues_by_id_ucl.json`.
```

### 3.2 Section 11.11 `GET /standings`: replace the "Real:" paragraph with

```markdown
Real: World Cup 2026 (`league=1&season=2026`) → 13 tables: Group A…L (4 rows each) + "Group Stage" (12 rows, the ranking of the third-placed teams, each of whom is also in its group table; `description` "Round of 32" for the first 8). Champions League 2025 → 1 table of 36 rows with `group` "UEFA Champions League". LaLiga 2026 → 1 table, `group` "Primera División". Library: `Standings.getGroupTables()` / `getCrossGroupRankings()` (rule: a table whose teams all appear in other tables, drawn from at least two of them, is a cross-group ranking), `getGroupOf(teamId)`.

`description` values observed (all responses above, plus Champions League 2026): `Promotion - Champions League (League phase)`, `Promotion - Europa League (League phase)`, `Promotion - Conference League (Qualification)`, `Promotion - Champions League (Play Offs: 1/8-finals)`, `Promotion - Champions League (Play Offs: 1/16-finals)` (the API's name for the league-phase play-off, which `/fixtures/rounds` calls "Round of 32"), `Relegation - LaLiga2`, `Round of 32`, `null`. Library: `StandingEntry.getZoneCategory()/getZoneTarget()/isPromotion()/isRelegation()`. The `form` of World Cup rows has 5 letters after 3 matches (it includes results before the tournament).

Fixtures: `standings_league.json`, `standings_groups.json` (trimmed to 5 tables × 3 rows), `standings_world_cup_2026.json`, `standings_ucl_2025.json`, `standings_laliga_2026.json`, `error_standings_missing_season.json`.
```

### 3.3 Section 11.12 `GET /fixtures/rounds`: add after the response tree (replacing the "Fixtures:" line)

```markdown
Real: the order is chronological, not numeric (LaLiga 2026 lists `Regular Season - 6` before `Regular Season - 4`). Round texts are `<stage>` or `<stage> - <n>`; the only structure the API gives. Observed (here or in `league.round` of fixtures): `Regular Season - N`, `League Stage - N`, `Group Stage - N`, `Group Stage`, `Apertura - N`, `Clausura - Quadrangular - N`, `League A - N`, `1st/2nd/3rd Qualifying Round`, `Play-offs` (Champions League: the August qualifying play-off), `Round of 32` (Champions League: the two-legged league-phase play-off; World Cup: single-match round of 32), `Round of 16`, `Quarter-finals`, `Semi-finals`, `3rd Place Final`, `Final`, `Friendly International`. `current=true` returns `[]` when the season is over (Champions League 2025 on 2026-11-13) and the next round during a break (LaLiga `Regular Season - 13` with 12 played). Library: `RoundName`, `StageTypes`, `CompetitionStructure` (stages, dates, status, `groupByRound`).

Fixtures: `fixtures_rounds.json`, `fixtures_rounds_dates.json`, `fixtures_rounds_current.json`, `fixtures_rounds_ucl_2025.json`, `fixtures_rounds_dates_ucl_2025.json`, `fixtures_rounds_world_cup_2026.json`, `fixtures_rounds_laliga_2026.json`.
```

### 3.4 Section 11.13 `GET /fixtures`: add a paragraph at the end

```markdown
Knockout ties (real): there is no leg, aggregate or tie winner. Two legs are the two fixtures of one round between the same teams, ordered by `timestamp`. `goals` includes extra time (Juventus 3-2 Galatasaray AET has `fulltime` 3-0, `extratime` 0-2, `goals` 3-2), so the aggregate is the sum of `goals`; the shoot-out is only in the last leg's `score.penalty`. `teams.*.winner` is per match: true for Juventus in that second leg although Galatasaray went through 7-5, and null for both sides in a second leg drawn after extra time (Dinamo Minsk 2-2 Ludogorets, Ludogorets through 3-2). In single matches `winner` includes the shoot-out. `score.extratime` is `{null, null}` in 3 of the 4 World Cup 2026 shoot-outs (Germany-Paraguay, Netherlands-Morocco, Australia-Egypt) although extra time was played: use the status (`AET`/`PEN`), not `extratime`. Group-stage fixtures carry no group. Library: `Ties`/`Tie`, `Groups`. Fixtures: `fixtures_by_league_ucl_2025.json` (`/fixtures?league=2&season=2025`, every fixture except 140 of the 144 league-phase ones), `fixtures_by_league_world_cup_2026.json` (`/fixtures?league=1&season=2026`, all 104).
```

### 3.5 Section 11.27 `GET /players/topscorers`: add

```markdown
Real: exactly 20 rows (LaLiga 2026); no pagination, no way to get the rest. A provider that pages the full list (Sportmonks) gives more.
```

### 3.6 Section 14 "Fixture file index": add rows

```markdown
| leagues_by_id_world_cup.json / leagues_by_id_ucl.json | `/leagues?id=1`, `/leagues?id=2` |
| standings_world_cup_2026.json | `/standings?league=1&season=2026` (full, 13 tables) |
| standings_ucl_2025.json | `/standings?league=2&season=2025` |
| standings_laliga_2026.json | `/standings?league=140&season=2026` (full, 20 rows) |
| fixtures_rounds_ucl_2025.json / fixtures_rounds_dates_ucl_2025.json | `/fixtures/rounds?league=2&season=2025` (+`dates=true`) |
| fixtures_rounds_world_cup_2026.json | `/fixtures/rounds?league=1&season=2026` |
| fixtures_rounds_laliga_2026.json | `/fixtures/rounds?league=140&season=2026` (all 38) |
| fixtures_by_league_ucl_2025.json | `/fixtures?league=2&season=2025` (league phase trimmed to 4 of 144) |
| fixtures_by_league_world_cup_2026.json | `/fixtures?league=1&season=2026` |
```

---

## 4. Javadoc for files this branch does not own

### 4.1 `PlayersApi.getTopScorers(int league, int season)` (and the other top lists)

```java
/**
 * The best scorers of a league season ({@code GET /players/topscorers}). One request. The API
 * returns at most 20 players and has no pagination for this endpoint: there is no way to get the
 * 21st. Updated several times a week; one call a day is enough.
 */
```

### 4.2 `FixturesApi.getCurrentRound(int league, int season)`

```java
/**
 * The current round label of a league season ({@code current=true}), or {@code null} when none. One
 * request. Returns {@code null} once the season is over (Champions League 2025 after the final) and
 * the next round during a break (observed: {@code "Regular Season - 13"} with round 12 finished).
 */
```

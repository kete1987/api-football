# Pending documentation: fixtures helpers (branch `helpers-fixtures`)

Text for another agent to merge into README.md, AGENTS.md and docs/API_CATALOG.md. Nothing here has been
applied to those files. Evidence comes from the live capture of 2026-11-12
(`football-api-comparison/data-download/20261112-225915`) and from the migration reports.

---

## 1. README.md

### 1.1 Fixtures helpers (new subsection, after the fixtures endpoint table)

**Status.** `FixtureStatus` / `FixtureStatuses` classify by the `short` code. `isLive()` follows the API's
"In Play" group and therefore **includes `SUSP` (suspended) and `INT` (interrupted)**. Use
`isInterrupted()` (`SUSP`, `INT`) to detect them, `isPlaying()` for live-without-interrupted (still true at
`HT`/`BT`) and `isPendingResolution()` (`PST, SUSP, INT, CANC, ABD, AWD, WO`) for "postponed-like" screens.

**Minute to show.** `status.getDisplayMinute()` (`elapsed + extra`, e.g. 94) and
`status.getDisplayMinuteText()` (`"90+4"`); the same two methods exist on `FixtureEvent.Time`. Null-safe.
Observed live: API-Football's minute runs about +1 versus Sportmonks (it gives the minute in progress, 1 at
kick-off), added time arrives in `extra`, and the extra-time minute is unreliable (it can start too high
and go backwards, e.g. 114 then 94).

**Cards and score.**

```java
Fixture f = api.fixtures().getFixtureById(1644363);
for (FixtureEvent card : f.getCardEventsMerged()) {          // chronological, one entry per dismissal
    boolean secondYellow = f.isSecondYellowRed(card);
}
for (Fixture.ScoreStep s : f.getRunningScore()) {            // score after each goal
    System.out.println(s.getEvent().getTime().getDisplayMinuteText() + " " + s.getText());
}
```

**Lookups inside a fixture** (all null-safe): `findStatistics(teamId)`, `getHomeStatistics()`,
`getAwayStatistics()`, `findLineup(teamId)`, `getHomeLineup()`, `getAwayLineup()`, `findPlayers(teamId)`,
`getHomePlayers()`, `getAwayPlayers()`, `findPlayerName(playerId)`;
`FixtureStatistics.getIntValue("Corner Kicks", 0)`.

**Media URLs.** `common.util.MediaUrls`: `playerPhoto`, `teamLogo`, `leagueLogo`, `venueImage`,
`coachPhoto`, `countryFlag(code)`; `PlayerRef.getPhotoUrl()` and `LineupPlayer.getPhotoUrl()` build the
photo from the id (events and lineups carry no photo). Images do not count against the daily quota.

**Lineup grid.** See section 3.3 below for the mirroring of the home team's columns
(`getGridRow()`, `getGridColumn()`, `FixtureLineup.getGridColumnMirrored(player)`,
`Fixture.getGridColumnAsSportmonks(lineup, player)`).

### 1.2 New `FixturesApi` methods (add rows to the endpoint/method table)

| Method | Requests | Notes |
|---|---|---|
| `getFixturesByLeague(league, season, from, to)` (+ `String` dates, + `timezone` overloads) | 1 | `league+season+from+to[+timezone]` |
| `getFixturesByTeam(team, season)` | 1 | all fixtures of a team in a season |
| `getFixturesByTeam(team, season, from, to)` (`LocalDate` or `String`) | 1 | `team+season+from+to` |
| `getNextFixtures(team, count, timezone)`, `getLastFixtures(team, count, timezone)` | 1 | |
| `getNextFixturesByLeague(league, count, timezone)`, `getLastFixturesByLeague(league, count, timezone)` | 1 | |
| `getFixturesByIdsInBatches(ids)` | **`ceil(n / 20)`** | any number of ids, batches of 20, duplicates sent once; 21 ids = 2 requests |

### 1.3 Quota note

`getFixtures(FixturesQuery)` throws `IllegalArgumentException` before any request when `from`/`to` are set
without `league` or `team`: the API answers with an error ("need another parameter") and an error costs a
request. Use `league` or `team` with `season`, or query one `date` at a time.

---

## 2. AGENTS.md

### 2.1 Key structure (replace the `util/` lines)

```
├── common/
│   └── util/                  # Constants, ApiFootballException, ModelCollections,
│                              # LenientNumberAdapters, NumberParsing, MediaUrls
└── fixtures/util/             # EventTypes, FixtureStatuses, StatisticTypes, FixtureMinutes,
                               # FixtureEventAnalysis (helpers needing the whole event list)
```

### 2.2 Quota awareness (append to the rule)

A method that sends more than one request is named for it: `getFixturesByIdsInBatches` (one request per 20
ids) has the cost in its Javadoc's first paragraph. Argument validation of a multi-request method happens for
all batches before the first request.

### 2.3 Validation (append to the Validation bullet)

Combinations the API answers with an error (which costs a request) are rejected in the API method, not in
the query setters, because the order of setters must not matter: `FixturesApi.getFixtures` rejects
`from`/`to` without `league` or `team`. Only reject what the catalog or a report shows to be an error.

### 2.4 Classification helpers (append)

- `FixtureStatuses.isLive` follows the API's group and includes `SUSP`/`INT`; keep that, and keep the
  Javadoc warning. `isInterrupted`, `isPlaying`, `isPendingResolution` are the explicit variants.
  `FixtureHelpersStatusTest` is the truth table over all 19 documented codes (plus `null` and an unknown code).
- A helper that needs the surrounding list to classify an event (second yellow) lives in
  `fixtures/util/FixtureEventAnalysis` and is exposed through `Fixture` (`isSecondYellowRed`,
  `getCardEventsMerged`). Rule: a red card is a second yellow when the list holds a yellow of the **same
  player at the same minute** (`elapsed` and `extra`, null = 0); players are matched by id, by name when an
  id is null, and not at all when neither is available (then the red is reported as straight). Verified
  with real data: A. Ruibal 118' (yellow then red, after an earlier yellow at 90+4) in
  Navalcarnero-Betis is a second yellow; L. Krejci 72' (no yellow) in Czech Republic-Spain is a straight red.
- Running score (`Fixture.getRunningScore()`): goals of `getEventsSorted()` added to
  `getTeamGoalCountsFor()` (own goals need no special case), missed penalties excluded; the last step equals
  `goals` on the 1-1 `PEN` and the 0-1 own-goal fixtures. The fixture files are trimmed, so
  `fixtures_by_id.json` has fewer goal events than its `goals`: do not assert on it.
- Tests with data collected live use small inline JSON marked "recorded live" (no new fixture files).

---

## 3. docs/API_CATALOG.md

### 3.1 Section 7 (media URLs) - add after the table

The patterns above are built by `common.util.MediaUrls` (`playerPhoto`, `teamLogo`, `leagueLogo`,
`venueImage`, `coachPhoto`, `countryFlag`). A `null`, non-positive id (the API sends venue id `0`) or a
non-plain country code gives `null`.

### 3.2 Section 8 (status codes) - add a note

`SUSP` and `INT` belong to the API's "In Play" group, so a "live" test on the group also matches a match
that is not being played. Apps that show a running minute must check `SUSP`/`INT` first
(`FixtureStatuses.isInterrupted`). At `HT` and `BT` `status.extra` keeps the added time of the half just
played (seen 1 and 4 at `HT`, 7 and 8 at `BT`) and at `BT` `elapsed` is `null`.

Minute behaviour observed live on 2026-11-12 (28 matches, against Sportmonks): the minute runs about +1
versus Sportmonks (+1 in 929 polls, 0 in 306, -1 in 26); `elapsed` stays at 45/90 while `extra` counts
(`90+1` to `90+7`, sometimes jumping `90+2` to `90+4`); the extra-time minute is unreliable: it starts too
high (97 when Sportmonks shows 91, 95 against 90) and goes backwards (100 to 94, 109 to 106, 114 to 94),
and there is no extra-time break status (`ET` keeps running).

### 3.3 Section 11.17 (`/fixtures/lineups`) - replace the one-line grid description

Grid: `"row:column"`. Row 1 is the goalkeeper and the following rows are the lines from the back to the
front (a 4-3-3 has rows 1 to 4); the column numbers the players of that row. Substitutes have `grid: null`.
**Neither API-Football nor Sportmonks gives metric coordinates**: a grid only says "third of four players in
the second line", so the app places the dots itself.

**Home-team columns are mirrored compared with Sportmonks.** Checked on 2026-11-12 by matching every
starter by shirt number against Sportmonks' `lineups[].formation_field` in the 5 matches that had a
lineup in both providers (Turkey-Belgium, Italy-France, Czech Republic-Spain, England-Croatia,
Navalcarnero-Betis):

| Side | Rows equal | Columns equal as sent | Columns equal after `playersInRow + 1 - column` |
|---|---|---|---|
| Home | 11 of 11 in all 5 matches | only 3 of 11 (the central players) | **11 of 11 in all 5 matches** |
| Away | 11 of 11 in all 5 matches | **11 of 11 in all 5 matches** | 3 of 11 |

Transformation, **home team only**, rows unchanged:
`column' = playersInRow + 1 - column`, with `playersInRow` the number of starters of that team that share the
row (the goalkeeper's row has 1, so the goalkeeper never moves). It is its own inverse, so it converts in
both directions. It needs the full starting XI: with fewer than 11 starters the row count is wrong.
Library: `FixtureLineup.getGridColumnMirrored(player)` (counts the row for you; apply it to the home lineup
only) or `Fixture.getGridColumnAsSportmonks(lineup, player)` (mirrors when the lineup is the home team's,
returns the column unchanged for the away team, `null` for a lineup of another fixture).

Worked example (Italy home, 4-3-3, API-Football fixture 1528960 / Sportmonks 19676662). Row 2 has 4 players
(`5 - column`), rows 3 and 4 have 3 (`4 - column`):

| Player | Shirt | API `grid` | Mirrored | Sportmonks `formation_field` |
|---|---|---|---|---|
| G. Donnarumma | 1 | 1:1 | 1:1 | 1:1 |
| M. Kayode | 2 | 2:4 | 2:1 | 2:1 |
| G. Scalvini | 3 | 2:3 | 2:2 | 2:2 |
| A. Bastoni | 6 | 2:2 | 2:3 | 2:3 |
| R. Calafiori | 5 | 2:1 | 2:4 | 2:4 |
| D. Frattesi | 16 | 3:3 | 3:1 | 3:1 |
| S. Tonali | 8 | 3:2 | 3:2 | 3:2 |
| N. Barella | 18 | 3:1 | 3:3 | 3:3 |
| S. Esposito | 7 | 4:3 | 4:1 | 4:1 |
| F. Esposito | 9 | 4:2 | 4:2 | 4:2 |
| N. Cambiaghi | 19 | 4:1 | 4:3 | 4:3 |

France (away, 4-2-3-1) in the same match: grid identical to Sportmonks for all 11 (e.g. M. Gusto `2:4` in
both). Without the correction, an XI drawn from the home grid appears left-right reversed next to the away
one. Sportmonks' `formation_position` (1-11) has no counterpart in API-Football.

### 3.4 Section 11.13 (`/fixtures`) - add under "Valid combinations"

`from`/`to` **without `league` or `team` is an error** ("need another parameter", verified live, query
`af08` of the migration reports) and the error costs a request; the library rejects it before sending.
`team+season` alone (all fixtures of a team in a season) is the documented way to list a team's season but
was not exercised live by this library.

### 3.5 Section 11.12 (`/fixtures/rounds`) - add under the parameter table

`current=true` returns `[]` when the season is over (2025 Champions League) and, **during a break between
rounds, the NEXT round**, not the last played (La Liga 2026: `"Regular Season - 13"` while round 12 was
finished and 13 had not started). `FixturesApi.getCurrentRound` returns `null` for `[]`.

### 3.6 Section 11.14 (`/fixtures/headtohead`) - add after the response description

Without `last` the result includes upcoming fixtures (`NS`). `last=N` returns only fixtures already played,
so it is not "the N latest dates". The history is incomplete for older cup ties (old Copa del Rey meetings
can be missing; data goes back to about 2012). Do not treat the list as exhaustive.

### 3.7 Section 11.16 (`/fixtures/events`) - add under the second-yellow description

Second yellow: the API sends `Yellow Card` and `Red Card` for the same player at the same minute (recorded:
A. Ruibal, `elapsed 118`, after an earlier yellow at `90+4`, Navalcarnero-Betis, fixture 1644363). A red
with no yellow of the same player at the same minute is a straight red (L. Krejci, 72', Czech Republic-Spain,
fixture 1528961). The `player.id` can be missing in the first polls after an event appears (seen on a
yellow card at 116').

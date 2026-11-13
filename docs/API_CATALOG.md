# API-Football v3 — API Catalog

Research date: 2026-11-12. Plan used for live calls: Pro (7 500 requests/day, 300/minute).

## 0. Sources and how to read this document

* **Documentation source.** `https://www.api-football.com/documentation-v3` is a Redoc page that loads
  `https://www.api-football.com/public/doc/openapi.yaml` (OpenAPI 3.0.0, `info.version` **3.9.3**).
  Both the page and the YAML are behind a Cloudflare interactive challenge (HTTP 403 "Just a moment…")
  for scripts and for the embedded browser, so the YAML was obtained from the Internet Archive
  (snapshot of 2025-11-02, 826 KB). A second copy published at
  `api-sports.io/public/documentations/football-v3.yaml` (snapshot 2025-12-04) has the same version and the
  same 38 paths; it only lacks the RapidAPI paragraphs. The live page could not be checked for changes made
  after December 2025; the live API responses below are from 2026-11-12 and are the stronger source of truth.
* **Real responses.** 85 live calls were made against `https://v3.football.api-sports.io` (78 counted + 7 calls to
  `/status`; `/status` `requests.current` went from 0 to 78). Every response was stored and analysed field by field across all elements (e.g. 1 248 fixtures,
  962 injuries, 2 588 odds values) to detect nullability and type variance.
* **Fixtures** live in `src/test/resources/fixtures/`. **They are trimmed**: every JSON array longer than 3
  elements was reduced to 2–5 elements chosen greedily to keep every variant shape seen (nulls, int vs string,
  different event types, statuses, …). `results` and `paging` are kept exactly as returned, so in trimmed files
  `results` is larger than `response.length`. A few enumeration-like arrays were kept complete
  (`statistics`, `statistics_1h`, `statistics_2h` in fixture statistics). The `/status` fixture is anonymised
  (firstname/lastname/email replaced). Files are UTF-8 without BOM, pretty-printed (the API itself returns compact
  JSON with escaped slashes `\/` and `\uXXXX` escapes; this is semantically identical).
* **Type notation used in field trees:** `int`, `number`, `string`, `bool`, `object`, `T[]` (array of T).
  A trailing `?` means the field was observed as `null` (or the docs show `null`). `int|string` means both
  JSON types were observed for the same field. `(docs)` marks information only from the documentation,
  `(real)` only from live responses. Dates are ISO-8601 strings; timestamps are Unix seconds.

---

## 1. Hosts and authentication

| Account type | Base URL | Required headers |
|---|---|---|
| API-SPORTS (dashboard.api-football.com) | `https://v3.football.api-sports.io` | `x-apisports-key: <key>` |
| RapidAPI | `https://api-football-v1.p.rapidapi.com/v3/` | `x-rapidapi-key: <key>`, `x-rapidapi-host: api-football-v1.p.rapidapi.com` |

* (docs) Accounts and keys on RapidAPI and on the API-SPORTS dashboard are **separate**; each has its own URL and
  key. A library should let the caller choose the "provider" (host + header names).
* (docs) The docs' code samples call the API-SPORTS host with `x-rapidapi-host: v3.football.api-sports.io` and
  `x-rapidapi-key`, i.e. the API-SPORTS host also accepts the RapidAPI header names (not verified here; all live
  calls used `x-apisports-key`).
* (docs) Only **GET** is allowed. Only these request headers are allowed: `x-rapidapi-host`, `x-rapidapi-key`,
  `x-apisports-key`. "If you make non-GET requests or add headers that are not in the list, you will receive an
  error from the API" (some JS frameworks add headers automatically). (real) CORS response headers confirm it:
  `access-control-allow-headers: x-rapidapi-key, x-apisports-key, x-rapidapi-host`,
  `access-control-allow-methods: GET, OPTIONS`, `access-control-allow-origin: *`.
  A Java client should not add custom headers (e.g. avoid extra `X-...` headers); `User-Agent`/`Accept` were sent by
  curl and caused no problem.
* (docs) The API key can be restricted to allowed domains/IPs in the dashboard (relevant for widgets).
* Query parameter values with spaces must be URL-encoded (e.g. `round=Regular Season - 1`,
  `type=Total Shots`).

## 2. Common response envelope

Every successful HTTP response (including logical errors) has this envelope:

```
{
  "get":        string            // endpoint name without leading slash, e.g. "fixtures", "teams/statistics", "foo" for an unknown endpoint
  "parameters": [] | object       // [] when no query parameters were sent; otherwise {name: string-value}
  "errors":     [] | object       // [] when there is no error; otherwise {key: message}
  "results":    int               // number of items in response (see notes)
  "paging":     {"current": int, "total": int}
  "response":   array | object    // array for almost every endpoint; object for /status and /teams/statistics
}
```

Important details for (Gson) deserialisation:

* **`parameters` is `[]` (empty JSON array) when empty and a JSON object when populated.** All values are
  **strings** even for integer parameters (`{"league":"140","season":"2026"}`). Unknown parameters are echoed
  (`{"foo":"bar"}`). The echo reflects server-side normalisation: an invalid timezone is echoed as
  `"timezone":"UTC"`. Use a custom `TypeAdapter`/`JsonDeserializer` that maps `[]` to an empty map.
* **`errors` is `[]` when empty and a JSON object when populated** (`{"team":"The Team field is required."}`).
  Multiple errors appear as multiple keys. Same adapter strategy as `parameters`. The docs' 204 example shows
  another populated form: `{"time": "...", "bug": "This is on our side, please report us this bug on ...", "report": "<endpoint>"}`.
* **`results`**: equals `response.length` for array responses. For `/teams/statistics` (object response) it is the
  number of top-level keys of the object (11 observed, docs example also 11). For `/status` it is **0** although
  `response` is a populated object (docs example says 1). For errors it is 0.
* **`paging`** is always present (the OpenAPI schemas omit it, the examples include it). For non-paginated endpoints
  it is `{"current":1,"total":1}`.
* **`response`** is `[]` for logical errors and for empty results — even on endpoints that normally return an object
  (e.g. `/teams/statistics` error → `"response": []`). Deserialise `response` per endpoint, and for the object
  endpoints handle `[]` as "no data".
* (docs, not observed) HTTP **204** "No Content" with the envelope and the `errors` object shown above;
  HTTP **499** "Time Out" and **500** "Internal Server Error" with a different body: `{"message": "Something went wrong while fetching details. Try again later."}`.
  The library must therefore parse a non-envelope body for 499/500 (and should handle 5xx/429/403 from Cloudflare
  defensively, whose bodies may be HTML).

## 3. Error handling (observed)

* **All logical/validation errors returned HTTP 200** with `errors` populated, `results: 0`, `response: []`,
  `paging {1,1}`. This includes calling a **non-existent endpoint** (`/foo` → `{"endpoint":"The Foo endpoint does not exist."}`).
* **Error requests consume daily quota** (the daily counter matched the total count of calls including the 13
  error/edge-case calls; see rate-limit notes) and the per-minute counter.
* Observed error messages (key → message):

| Request | `errors` |
|---|---|
| `/teams/statistics?league=140&season=2025` | `{"team": "The Team field is required."}` |
| `/teams` (no params) | `{"required": "At least one parameter is required."}` |
| `/fixtures?foo=bar` | `{"foo": "The Foo field do not exist."}` |
| `/teams?search=ma` | `{"search": "The Search field must be at least 3 characters in length."}` |
| `/teams?search=Leganés` | `{"search": "The Search field may only contain alpha-numeric characters and spaces."}` (HTTP 200, 0 results, consumes quota) |
| `/fixtures?league=abc` | `{"league": "The League field must contain an integer.", "season": "The Season field is required."}` |
| `/standings?league=140` | `{"season": "The Season field is required.", "team": "The Team field is required."}` |
| `/players?search=mbappe` | `{"team": "The League or Team field is required with the Search field.", "league": "The League or Team field is required with the Search field."}` |
| `/fixtures?team=541&last=100` | `{"last": "The Last field cannot be used with Last field."}` (sic — message is misleading; `last` is limited to 2 digits) |
| `/fixtures?ids=1-2-…-21` (21 ids) | `{"ids": "The Ids field does not match the regular expression: [id-id-id...] or integer. Maximum of 20 ids allowed."}` |
| `/foo` | `{"endpoint": "The Foo endpoint does not exist."}` (and `"get":"foo"`) |

* **Silent acceptance (no error!)** — a library should validate these itself:
  * `/fixtures?date=2026-13-45` → **no error**, 768 fixtures of **2027-02-14** (the date overflowed like PHP
    `mktime`). Fixture: `fixtures_invalid_date_overflow.json`.
  * `/fixtures?date=2026-11-12&timezone=Mars/Olympus` → **no error**, timezone silently replaced by UTC
    (`parameters.timezone` echoed as `"UTC"`). Fixture: `fixtures_invalid_timezone_fallback.json`.
  * `/players?league=140&season=2026&page=99` (beyond `paging.total`=31) → no error, `results:0`, `response:[]`,
    `paging {"current":99,"total":31}`. Fixture: `players_page_out_of_range.json`.
* Not observed (out of budget / unsafe): daily-quota-exceeded, per-minute-exceeded, missing/invalid key. Commonly
  reported shapes (UNVERIFIED here) are `errors: {"requests": "..."}`, `{"rateLimit": "..."}`, `{"token": "..."}`
  with HTTP 200. Treat any non-empty `errors` as failure regardless of the key.
* Recommended library behaviour: success iff HTTP 2xx **and** `errors` is empty; otherwise raise an exception carrying
  the `errors` map, the `get` value and the echoed `parameters`.

## 4. Rate-limit headers

(docs) API-SPORTS responses always include:

| Header | Meaning |
|---|---|
| `x-ratelimit-requests-limit` | requests allocated per day by the subscription |
| `x-ratelimit-requests-remaining` | requests remaining today |
| `X-RateLimit-Limit` | max calls per minute |
| `X-RateLimit-Remaining` | calls remaining in the current minute |

(docs) RapidAPI responses carry `x-ratelimit-requests-limit`, `x-ratelimit-requests-remaining`, `server`
(RapidAPI proxy version) and `X-RapidAPI-Proxy-Response` (true when the response was generated by the proxy, not by
API-Football). (docs) "If you exceed your allowed request rate per minute, either through continuous excessive usage
or by generating abnormal traffic spikes, your access may be temporarily or permanently blocked by our firewall
without prior notice."

(real) Header names arrive lower-case over HTTP/1.1 (`x-ratelimit-limit`, `x-ratelimit-remaining`,
`x-ratelimit-requests-limit`, `x-ratelimit-requests-remaining`) — read them case-insensitively. They were present on
every response, including errors and `/status`. An undocumented `x-ttl` header (seconds, apparently server cache TTL)
appeared on some `/fixtures` and `/fixtures/headtohead` responses: 25 (today's date), 300 (`ids`), 1800 (h2h),
7200 (past/future date, `status` filter). Other headers: `Cache-Control: no-store, no-cache, must-revalidate`,
`Server: cloudflare`, `cf-ray`, `Content-Type: application/json`, chunked transfer.

How the values behave (details and raw observations in `api-football-rate-limit-notes.md`):
* Per-minute counter: fixed window aligned to the **clock minute** (reset at `hh:mm:00`), decremented by every call,
  **including `/status`**.
* Daily remaining header: decremented by every non-`/status` call, but the value is **not monotonic** — at the first
  call of a new minute it sometimes jumps back up to a stale value (e.g. 7473 → 7499 → … → 7466). Treat it as
  approximate; it converges to `limit - used`.
* `/status` does not consume daily quota, but its `requests.current` (and its own `x-ratelimit-requests-remaining`)
  lags the real count by up to ~1–2 minutes.

## 5. Pagination

* Paginated endpoints (docs + real): `/players` (20 per page), `/players/profiles` (250 per page),
  `/odds` (10 per page), `/odds/mapping` (100 per page). Parameter `page` (int, default 1).
* `paging.current` = requested page, `paging.total` = number of pages. `results` = items on this page.
* Requesting a page beyond `total` is **not** an error: empty `response`, `paging.current` echoes the requested page.
* Observed sizes: `/players?league=140&season=2026` → 31 pages; `/players/profiles` (no params) → **2 771 pages**;
  `/odds?league=140&season=2026` → 2 pages; `/odds?date=2026-11-14` → 24 pages; `/odds/mapping` → 31 pages.
* Every other endpoint returns everything in one page regardless of size (e.g. `/fixtures?date=2026-11-08` returned
  1 235 fixtures / 1.2 MB in one response; `/injuries?league=140&season=2026` returned 962 items / 534 KB).
* Each page is a separate counted request — fetching all of `/players/profiles` costs ~2 771 requests.

## 6. Timezone handling

* All dates are returned in UTC by default (`fixture.timezone: "UTC"`, dates like `2026-11-08T20:00:00+00:00`).
* Endpoints accepting `timezone`: `/fixtures`, `/fixtures/headtohead`, `/fixtures/rounds` (affects `dates`),
  `/injuries`, `/odds`. The value must be one of `/timezone` (427 IANA names observed, e.g. `Europe/Madrid`).
* With `timezone=Europe/Madrid`, `fixture.timezone` becomes `"Europe/Madrid"`, `fixture.date` is rendered with offset
  (`2026-11-12T00:00:00+01:00`) and the `date` filter is interpreted as that local calendar day. `timestamp` is always
  absolute Unix seconds.
* Invalid timezone → silently replaced with UTC (no error). Validate client-side against `/timezone` or `java.time.ZoneId`.
* `odds/mapping`, `odds/live`, `injuries`, standings `update` etc. return `+00:00` timestamps.

## 7. Media / logo URLs

(docs) Images are served from `https://media.api-sports.io/` and **do not count against the daily quota** but are
rate-limited per second/minute; the docs recommend caching them on your side / your own CDN (BunnyCDN tutorial).
URL patterns (docs; confirmed in responses):

| Entity | URL |
|---|---|
| League logo | `https://media.api-sports.io/football/leagues/{league_id}.png` |
| Team logo | `https://media.api-sports.io/football/teams/{team_id}.png` |
| Venue image | `https://media.api-sports.io/football/venues/{venue_id}.png` |
| Player photo | `https://media.api-sports.io/football/players/{player_id}.png` |
| Coach photo | `https://media.api-sports.io/football/coachs/{coach_id}.png` |
| Country flag | `https://media.api-sports.io/flags/{country_code_lowercase}.svg` (e.g. `gb.svg`, `es.svg`; sub-national codes like `gb-eng.svg`) |

Flags/codes are `null` for "World" and for many teams-only countries.
The patterns above are built by `common.util.MediaUrls` (`playerPhoto`, `teamLogo`, `leagueLogo`, `venueImage`,
`coachPhoto`, `countryFlag`). A `null`, non-positive id (the API sends venue id `0`) or a non-plain country code gives
`null`. Events and lineups carry a player id but no photo: `PlayerRef.getPhotoUrl()` and `LineupPlayer.getPhotoUrl()`
build it from the id.
(docs) Logos and trademarks are delivered for identification only; rights belong to third parties.

## 8. Fixture status codes

(docs table; `TYPE` is a grouping useful as an enum attribute)

| short | long (docs) | type | description |
|---|---|---|---|
| TBD | Time To Be Defined | Scheduled | Scheduled but date and time are not known |
| NS | Not Started | Scheduled | |
| 1H | First Half, Kick Off | In Play | First half in play |
| HT | Halftime | In Play | Finished in the regular time (sic) |
| 2H | Second Half, 2nd Half Started | In Play | Second half in play |
| ET | Extra Time | In Play | Extra time in play |
| BT | Break Time | In Play | Break during extra time |
| P | Penalty In Progress | In Play | Penalty played after extra time |
| SUSP | Match Suspended | In Play | Suspended by referee's decision, may be rescheduled another day |
| INT | Match Interrupted | In Play | Interrupted by referee's decision, should resume in a few minutes |
| FT | Match Finished | Finished | Finished in the regular time |
| AET | Match Finished | Finished | Finished after extra time without going to the penalty shootout |
| PEN | Match Finished | Finished | Finished after the penalty shootout |
| PST | Match Postponed | Postponed | Postponed to another day; once the new date/time is known it goes back to NS |
| CANC | Match Cancelled | Cancelled | Cancelled, match will not be played |
| ABD | Match Abandoned | Abandoned | Abandoned (weather, safety, floodlights, staff…); may or may not be rescheduled |
| AWD | Technical Loss | Not Played | |
| WO | WalkOver | Not Played | Victory by forfeit or absence of competitor |
| LIVE | In Progress | In Play | Very rare; in progress but half/elapsed data unavailable |

Docs notes: `TBD`, `PST`, `CANC` fixtures are checked daily and the date may be wrong. Competitions without
livescore stay `NS` until the final result is set (up to 48 h later). Fixture ids never change. Data is updated every
15 s but may lag reality.

Real `long` values that differ from the docs table — **map by `short`, never by `long`**:
`1H` → `"First Half"` (docs "First Half, Kick Off"); `AWD` → `"Technical loss"` (lower-case l). Observed shorts:
FT, NS, PST, PEN, AET, 1H, AWD, ABD, CANC. `odds/live` uses its own `status.long` ("First Half") and no `short`.

`SUSP` and `INT` belong to the API's "In Play" group, so a "live" test on the group also matches a match that is not
being played. Apps that show a running minute must check `SUSP`/`INT` first (`FixtureStatuses.isInterrupted`;
`isLive` follows the group, `isPlaying` leaves them out, `isPendingResolution` = `PST, SUSP, INT, CANC, ABD, AWD, WO`).
At `HT` and `BT` `status.extra` keeps the added time of the half just played (seen 1 and 4 at `HT`, 7 and 8 at `BT`)
and at `BT` `elapsed` is `null`.

Minute behaviour observed live on 2026-11-12 (28 matches, against Sportmonks): API-Football gives the minute in
progress (1 at kick-off), so it runs about +1 versus Sportmonks (+1 in 929 polls, 0 in 306, -1 in 26); `elapsed` stays
at 45/90 while `extra` counts (`90+1` to `90+7`, sometimes jumping `90+2` to `90+4`); the extra-time minute is
unreliable: it starts too high (97 when Sportmonks shows 91, 95 against 90) and goes backwards (100 to 94, 109 to 106,
114 to 94), and there is no extra-time break status (`ET` keeps running). The minute to show is
`status.getDisplayMinute()` / `getDisplayMinuteText()` (`elapsed + extra`, `"90+4"`; also on `FixtureEvent.Time`).

The `status` filter (fixtures, headtohead) accepts one or several shorts joined by `-` (e.g. `PEN-AET`, `NS-PST-FT`);
the docs examples also use lower case (`status=ft`, `status=ns`).

## 9. Enumerations worth modelling

* **Fixture status short** — table above (+ type grouping: Scheduled, InPlay, Finished, Postponed, Cancelled,
  Abandoned, NotPlayed). Keep an `UNKNOWN` fallback.
* **League type** — response values `"League"`, `"Cup"`; the `type` *parameter* takes lower case `league` / `cup`.
* **Event `type` / `detail`** (docs): Goal → Normal Goal, Own Goal, Penalty, Missed Penalty; Card → Yellow Card,
  Red card (docs spelling; real `"Red Card"`); Subst → Substitution 1, 2, 3, …; Var → Goal cancelled,
  Penalty confirmed (VAR since 2020-21). **Real responses use `"subst"` in lower case** while `Goal`, `Card` are
  capitalised; parse case-insensitively. `detail` for substitutions is open-ended ("Substitution 5" observed).
  The `type` filter of `/fixtures/events` takes lower case (`type=card`, `type=goal` in docs).
  **`type` is coarse; `detail` says what happened.** A missed penalty is `type: "Goal"` with
  `detail: "Missed Penalty"`, so "is a goal" must be decided on `detail`, never on `type` alone (the docs list it
  under Goal without saying it is not a goal). See section 11.16 for the shapes seen live.
  There is **no shoot-out `detail`**: penalty shoot-out kicks are not events at all (verified, section 11.16).
  **Own goal**: listed under the team the goal **counts for**; `player` belongs to the other team.
* **Event `comments`** (free text, observed): `Foul`, `Time wasting`, `Persistent fouling`, `Argument`, `Dissent`;
  `null` for goals and substitutions.
* **Fixture statistics `type`** (docs list): Shots on Goal, Shots off Goal, Shots insidebox, Shots outsidebox,
  Total Shots, Blocked Shots, Fouls, Corner Kicks, Offsides, Ball Possession, Yellow Cards, Red Cards,
  Goalkeeper Saves, Total passes, Passes accurate, Passes %.
  **Real (2026, La Liga): 17 types**: the list above **without "Passes %"** plus **"Free Kicks"** and
  **"expected_goals"** (snake_case!). Order differs between responses. Model as an open set (enum with fallback or
  keep the raw string). **Real (2025 season, `fixtures?id=` of a Champions League final and a La Liga match):
  18 types**: the docs list **with "Passes %"**, plus **"expected_goals"** and **"goals_prevented"** (`"0.68"`), and
  no "Free Kicks" — the set varies by fixture/season.
* **Lineup position `pos`**: `G`, `D`, `M`, `F`. **Player `position`** (squads, profiles, players):
  `Goalkeeper`, `Defender`, `Midfielder`, `Attacker`.
* **Injury `player.type`**: `Missing Fixture`, `Questionable`. `reason` is free text ("Suspended", "Knee Injury", …).
* **Standings `status`**: observed `same`; known values `up`, `down` (not observed).
* **Venue `surface`**: observed `grass`, `artificial turf` (free text).
* **Trophy `place`**: observed `Winner`, `2nd Place` (free text).
* **Odds live `status` flags**: `stopped`, `blocked`, `finished` (booleans).
* **Bookmakers and bet ids**: lists in Appendix A (as of 2026-11-12). **Pre-match bet ids (`/odds/bets`) and in-play
  bet ids (`/odds/live/bets`) are different id spaces** (e.g. pre-match 1 = "Match Winner", live 1 =
  "Over/Under Extra Time"; docs: "not compatible"). The lists contain duplicates (e.g. live 84/85) and `null` names
  (bookmaker 37, bet 341). Recommend constants only for a few stable pre-match ids (1 Match Winner, 5 Goals Over/Under,
  8 Both Teams Score, 12 Double Chance, 4 Asian Handicap, 10 Exact Score) and otherwise treat as data.
* **Prediction strings**: `under_over`, `goals.home/away` use values like `"-3.5"`, `"+2.5"` (docs: -1.5…-4.5 /
  +1.5…+4.5); "-1.5" means at most 1 goal.

## 10. Conventions and quirks across endpoints

* **Seasons are 4-digit years** = the starting year (`2025` = 2025-2026).
* Ids (league, team, player, coach, venue, fixture) are unique and stable.
* Most "statistic" numbers that look numeric are **strings**: ratings (`"7.11"`), averages (`"2.4"`),
  percentages (`"37%"`, `"8.97%"`), odds (`"1.20"`), heights/weights. Some are inconsistent between endpoints (see per
  endpoint), e.g. `passes.accuracy` is `int` in `/players` but `string` in `/fixtures/players`.
* Typos are part of the contract: `appearences`, `commited`, `coachs`, `loses`.
* Several objects use **data-dependent keys** (map, not POJO): minute buckets `"0-15"`, `"16-30"`, … `"106-120"`
  and an **empty-string key `""`** in `teams/statistics.cards.yellow/red`; under/over keys `"0.5"`…`"4.5"`.
* A venue `id` may be `null` **or `0`** (9 fixtures in a World Cup sample had `venue.id: 0` with a valid name).
* Several fields are `null` in real data although the docs example shows a value (e.g. lineup `team.colors`).
* Response bodies are large for some calls: odds by league ≈1.6 MB/page, fixtures by date up to ≈1.2 MB,
  injuries by league ≈0.5 MB. Enable gzip (`Accept-Encoding: gzip`; server sends `vary: Accept-Encoding`).

---

## 11. Endpoints

38 documented paths + `/status` (documented in the introduction only) = **39 endpoints**.

Legend for parameter tables: R = required, O = optional, C = conditionally required.

### 11.1 `GET /status`
Account, subscription and today's usage. (docs) **Does not count against the daily quota.** No parameters.
Not paginated. Not in the OpenAPI `paths`; documented in the "API-SPORTS Account" section.
```
response: object
  account: object
    firstname: string
    lastname: string
    email: string
  subscription: object
    plan: string            // "Pro", "Free", ...
    end: string             // ISO datetime, e.g. "2027-11-12T07:46:56+00:00"
    active: bool
  requests: object
    current: int            // requests used today (lags ~1-2 min, see rate-limit notes)
    limit_day: int          // 7500
```
Real vs docs: `results` is **0** (docs example 1). Counts against the per-minute limit.
Fixture: `status.json` (anonymised).

### 11.2 `GET /timezone`
List of timezones accepted by the `timezone` parameter. No parameters. Not paginated.
Update: never ("contains all the existing timezone, it is not updated"). Recommended: 1 call when needed.
```
response: string[]      // 427 IANA names, e.g. "Africa/Abidjan" (docs example said 425)
```
Fixture: `timezone.json`.

### 11.3 `GET /countries`
Countries available in `/leagues`. `name` and `code` can be used as filters elsewhere. All parameters combinable.
Update: when a league from a new country is added. Recommended: 1 call/day.

| Param | Type | | Notes |
|---|---|---|---|
| name | string | O | country name, e.g. `england` (case-insensitive) |
| code | string | O | alpha code, 2–6 chars, e.g. `FR`, `GB-ENG`, `IT` |
| search | string | O | docs schema min/max length 3, but docs example uses `search=engl` (4 chars) — treat as "min 3" |
```
response: object[]
  name: string
  code: string?       // null for "World"
  flag: string?       // null for "World"
```
Real: 171 countries. Fixtures: `countries.json`, `countries_by_name.json`.

### 11.4 `GET /leagues`
Leagues and cups with their seasons and per-season `coverage`. Most parameters combinable.
Update: several times a day. Recommended: 1 call/hour.
Notes (docs): league ids are unique and stable across seasons; coverage reflects data available *now* (all false
before a competition starts) and can vary by season; "friendlies" are exceptions; new seasons are added
automatically; cup fixtures are added when both teams are known.

| Param | Type | | Notes |
|---|---|---|---|
| id | int | O | league id |
| name | string | O | league name |
| country | string | O | country name |
| code | string | O | country alpha code (2–6 chars: `FR`, `GB-ENG`) |
| season | int | O | YYYY |
| team | int | O | team id |
| type | string | O | `league` \| `cup` |
| current | string | O | `true` \| `false` — only the active (or last) season of each competition |
| search | string | O | min 3 chars; league name or country |
| last | int | O | max 2 digits; the X last leagues/cups added |
```
response: object[]
  league: object
    id: int
    name: string
    type: string            // "League" | "Cup" (capitalised)
    logo: string
  country: object
    name: string
    code: string?           // null for "World"
    flag: string?
  seasons: object[]          // only the current one when current=true
    year: int
    start: string           // "YYYY-MM-DD"
    end: string
    current: bool
    coverage: object
      fixtures: object
        events: bool
        lineups: bool
        statistics_fixtures: bool
        statistics_players: bool
      standings: bool
      players: bool
      top_scorers: bool
      top_assists: bool
      top_cards: bool
      injuries: bool
      predictions: bool
      odds: bool
```
Real vs docs: the OpenAPI schema omits `top_assists`, `top_cards`, `injuries` but they are present (and in the docs
example). Fixtures: `leagues_by_id.json` (La Liga, all seasons), `leagues_current_country.json`
(`country=Spain&current=true`, 39 results), `leagues_by_id_world_cup.json`, `leagues_by_id_ucl.json`.

Real: `seasons[].current` stays `true` after the season ends: World Cup 2026 (`id=1`) is `current: true` with `end`
`2026-07-19` on 2026-11-13. `end` is the date of the last fixture known, not the planned end: Champions League 2026
(`id=2`) reads `start 2026-07-07`, `end 2027-01-27` (end of the league phase) while its knockout rounds are not
scheduled. Library: `Season.hasStarted/isOver/isInProgress(LocalDate)`, `League.getSeasonInProgress(LocalDate)`.

### 11.5 `GET /leagues/seasons`
All available season years. No parameters. Update: when a league is added. Recommended: 1 call/day.
```
response: int[]      // e.g. [2008, 2009, ..., 2027]  (21 values)
```
Real vs docs: OpenAPI schema says `string[]`, real (and docs example) are **integers**. Fixture: `leagues_seasons.json`.

### 11.6 `GET /teams`
Teams with their home venue. **At least one parameter is required.** All parameters combinable.
Update: several times a week. Recommended: 1 call/day.

| Param | Type | | Notes |
|---|---|---|---|
| id | int | O | team id |
| name | string | O | team name |
| league | int | O | league id (use with season) |
| season | int | O | YYYY |
| country | string | O | country name |
| code | string | O | 3-letter team code (e.g. `FRA`) |
| venue | int | O | venue id |
| search | string | O | min 3 chars; team name or country |
```
response: object[]
  team: object
    id: int
    name: string
    code: string?          // 3 letters, e.g. "REA" (docs: nullable in practice for small teams; not observed null)
    country: string
    founded: int?          // (not observed null here)
    national: bool
    logo: string
  venue: object            // fields may be null when the venue is unknown (not observed in sample)
    id: int?
    name: string?
    address: string?
    city: string?
    capacity: int?
    surface: string?
    image: string?
```
Errors: no params → `{"required": "At least one parameter is required."}`; `search` < 3 chars → error.
Fixtures: `teams_by_id.json`, `teams_by_league_season.json`, `error_no_params.json`, `error_search_too_short.json`.

### 11.7 `GET /teams/statistics`
Statistics of one team in one league/season; optional `date` = statistics up to that date.
Update: twice a day. Recommended: 1 call/day for teams playing that day, otherwise 1/week.

| Param | Type | | Notes |
|---|---|---|---|
| league | int | R | |
| season | int | R | YYYY |
| team | int | R | |
| date | string | O | YYYY-MM-DD, limit date |

**`response` is an object** (not an array); on error it is `[]`. `results` = number of top-level keys (11).
```
response: object
  league: {id: int, name: string, country: string, logo: string, flag: string?, season: int}
  team: {id: int, name: string, logo: string}
  form: string                    // e.g. "WWWWWWLWWW..." (W/D/L per match)
  fixtures: object                // docs schema calls it "matches" — real key is "fixtures"
    played|wins|draws|loses: {home: int, away: int, total: int}
  goals: object
    for|against: object
      total: {home: int, away: int, total: int}
      average: {home: string, away: string, total: string}      // "2.4"
      minute: map<string, {total: int?, percentage: string?}>   // keys "0-15","16-30","31-45","46-60","61-75","76-90","91-105","106-120"; null when 0
      under_over: map<string, {over: int, under: int}>          // keys "0.5","1.5","2.5","3.5","4.5"
  biggest: object
    streak: {wins: int, draws: int, loses: int}
    wins: {home: string?, away: string?}     // "5-1"
    loses: {home: string?, away: string?}
    goals: {for: {home: int, away: int}, against: {home: int, away: int}}
  clean_sheet: {home: int, away: int, total: int}
  failed_to_score: {home: int, away: int, total: int}
  penalty: object
    scored: {total: int, percentage: string}   // "100.00%"
    missed: {total: int, percentage: string}   // "0%"
    total: int
  lineups: object[]
    formation: string     // "4-3-3"
    played: int
  cards: object
    yellow|red: map<string, {total: int?, percentage: string?}>   // same minute keys PLUS an empty-string key "" (real)
```
Real vs docs: the OpenAPI schema is incomplete (only league/team/"matches"/goals); the docs example matches reality
except that the real `cards.yellow` and `cards.red` maps contain an **extra `""` key** (e.g. `"": {"total": 1, "percentage": "1.43%"}`)
— a POJO with fixed fields is fine if unknown keys are ignored; a `Map` must accept the empty key.
Fixtures: `teams_statistics.json`, `error_missing_param.json` (missing `team`).

### 11.8 `GET /teams/seasons`
Seasons available for a team. Update: several times a week. Recommended: 1 call/day.

| Param | Type | | Notes |
|---|---|---|---|
| team | int | R | |
```
response: int[]      // e.g. [2010, ..., 2026]
```
Fixture: `teams_seasons.json`.

### 11.9 `GET /teams/countries`
Countries available for `/teams`. No parameters. Update: several times a week. Recommended: 1 call/day.
```
response: object[]
  name: string       // hyphenated, e.g. "American-Samoa"; includes non-country entries like "Catalonia", "Caracas"
  code: string?      // null for 64 of 234 entries
  flag: string?
```
Fixture: `teams_countries.json`.

### 11.10 `GET /venues`
Venues. **At least one parameter required.** All parameters combinable.
Update: several times a week. Recommended: 1 call/day.

| Param | Type | | Notes |
|---|---|---|---|
| id | int | O | |
| name | string | O | |
| city | string | O | |
| country | string | O | country name |
| search | string | O | min 3; name, city or country |
```
response: object[]
  id: int
  name: string
  address: string?
  city: string
  country: string
  capacity: int
  surface: string        // "grass", "artificial turf"
  image: string
```
Fixtures: `venues_by_id.json`, `venues_search.json`.

### 11.11 `GET /standings`
One or more ranking tables for a league/season or for a team. Some competitions have several tables (groups,
opening/closing…). Update: every hour. Recommended: 1/hour for leagues/teams with a fixture in progress, else 1/day.

| Param | Type | | Notes |
|---|---|---|---|
| league | int | C | league or team must be given |
| season | int | R | YYYY |
| team | int | C | |

Constraint (real): `season` is required and one of `league`/`team` is required (error message for `league` only:
`{"season": "...required", "team": "...required"}`).
```
response: object[]
  league: object
    id: int
    name: string
    country: string
    logo: string
    flag: string?            // null for "World"
    season: int
    standings: object[][]    // ARRAY OF ARRAYS: one inner array per table/group
      rank: int
      team: {id: int, name: string, logo: string}
      points: int
      goalsDiff: int
      group: string          // "Primera División", "Group A", "Group Stage", ...
      form: string?          // "WWLWW" (docs: may be null early in season)
      status: string         // "same" | "up" | "down"
      description: string?   // "Promotion - Champions League (League phase)", null for mid-table
      all|home|away: object
        played: int
        win: int
        draw: int
        lose: int
        goals: {for: int, against: int}
      update: string         // ISO datetime
```
Real: World Cup 2026 (`league=1&season=2026`) → 13 tables: Group A…L (4 rows each) + "Group Stage" (12 rows, the
ranking of the third-placed teams, each of whom is also in its group table; `description` "Round of 32" for the first 8).
Champions League 2025 → 1 table of 36 rows with `group` "UEFA Champions League". LaLiga 2026 → 1 table, `group`
"Primera División". Library: `Standings.getGroupTables()` / `getCrossGroupRankings()` (rule: a table whose teams all
appear in other tables, drawn from at least two of them, is a cross-group ranking), `getGroupOf(teamId)`.

`description` values observed (all responses above, plus Champions League 2026): `Promotion - Champions League (League phase)`,
`Promotion - Europa League (League phase)`, `Promotion - Conference League (Qualification)`,
`Promotion - Champions League (Play Offs: 1/8-finals)`, `Promotion - Champions League (Play Offs: 1/16-finals)` (the
API's name for the league-phase play-off, which `/fixtures/rounds` calls "Round of 32"), `Relegation - LaLiga2`,
`Round of 32`, `null`. Library: `StandingEntry.getZoneCategory()/getZoneTarget()/isPromotion()/isRelegation()`
(`leagues.util.StandingZones`). The `form` of World Cup rows has 5 letters after 3 matches (it includes results before
the tournament).

Fixtures: `standings_league.json`, `standings_groups.json` (trimmed to 5 tables × 3 rows), `standings_world_cup_2026.json`,
`standings_ucl_2025.json`, `standings_laliga_2026.json`, `error_standings_missing_season.json`.

### 11.12 `GET /fixtures/rounds`
Rounds of a league/season; the round string is usable as `round` filter in `/fixtures`.
Update: every day. Recommended: 1 call/day.

| Param | Type | | Notes |
|---|---|---|---|
| league | int | R | |
| season | int | R | YYYY |
| current | bool | O | `true` \| `false` — only the current round |
| dates | bool | O | default false; include the dates of each round (added in 3.9.3) |
| timezone | string | O | from `/timezone` |

**The element type changes with `dates`:**
```
// dates absent/false
response: string[]                  // "Regular Season - 1", ..., cup rounds like "Round of 16", "Final"
// dates=true
response: object[]
  round: string
  dates: string[]                   // "YYYY-MM-DD"
```
Real: the order is chronological, not numeric (LaLiga 2026 lists `Regular Season - 6` before `Regular Season - 4`). Round
texts are `<stage>` or `<stage> - <n>`; the only structure the API gives. Observed (here or in `league.round` of fixtures):
`Regular Season - N`, `League Stage - N`, `Group Stage - N`, `Group Stage`, `Apertura - N`, `Clausura - Quadrangular - N`,
`League A - N`, `1st/2nd/3rd Qualifying Round`, `Play-offs` (Champions League: the August qualifying play-off),
`Round of 32` (Champions League: the two-legged league-phase play-off; World Cup: single-match round of 32),
`Round of 16`, `Quarter-finals`, `Semi-finals`, `3rd Place Final`, `Final`, `Friendly International`.

`current=true` returns `[]` when the season is over (Champions League 2025 on 2026-11-13) and, **during a break between
rounds, the NEXT round**, not the last played (LaLiga 2026: `"Regular Season - 13"` while round 12 was finished and 13
had not started). `FixturesApi.getCurrentRound` returns `null` for `[]`. Library: `RoundName`, `StageTypes`,
`CompetitionStructure` (stages, dates, status, `groupByRound`).

Fixtures: `fixtures_rounds.json`, `fixtures_rounds_dates.json`, `fixtures_rounds_current.json`,
`fixtures_rounds_ucl_2025.json`, `fixtures_rounds_dates_ucl_2025.json`, `fixtures_rounds_world_cup_2026.json`,
`fixtures_rounds_laliga_2026.json`.

### 11.13 `GET /fixtures`
Fixtures (matches). Update: every 15 seconds. Recommended: 1 call/minute for leagues/teams/fixtures with a fixture
in progress, otherwise 1/day. Not paginated (one call can return >1 000 fixtures).

| Param | Type | | Notes |
|---|---|---|---|
| id | int | O | one fixture; response includes events, lineups, statistics, players |
| ids | string | O | `id-id-id`, max 20 ids; response includes events, lineups, statistics, players |
| live | string | O | `all` or `id-id` (league ids); response includes `events` |
| date | string | O | YYYY-MM-DD (invalid dates are NOT rejected, they overflow) |
| league | int | O | |
| season | int | O | YYYY |
| team | int | O | |
| last | int | O | max 2 digits (≤ 99) — X last fixtures |
| next | int | O | max 2 digits — X next fixtures |
| from | string | O | YYYY-MM-DD (use with `to`) |
| to | string | O | YYYY-MM-DD |
| round | string | O | value from `/fixtures/rounds` |
| status | string | O | one or more shorts `NS-PST-FT` |
| venue | int | O | venue id |
| timezone | string | O | from `/timezone` |

Valid combinations (docs "Use Cases"): `id`; `ids`; `live`; `league+season`; `date`; `next`; `last`;
`date+league+season`; `league+next`; `venue+next`; `league+last+status`; `team+last+timezone`;
`team+season+from+to`; `league+season+from+to+timezone`; `league+season+round`. Real: `league+last` (no season)
works; `league` alone with an invalid value also asked for `season`.

`team+season` (all fixtures of a team in a season) is not in the docs list but works: **verified 2026-11-13**,
`team=541&season=2026` returned 52 fixtures (2026-08-26 to 2027-01-27) with an empty `errors`
(`FixturesApi.getFixturesByTeam(team, season)`). `from`/`to` **without `league` or `team` is an error** ("need another
parameter", verified live, query `af08` of the migration reports) and the error costs a request; the library rejects it
before sending.
```
response: object[]
  fixture: object
    id: int
    referee: string?            // "J. Alberola"; null in ~75% of a global sample
    timezone: string            // "UTC" or requested tz
    date: string                // ISO with offset
    timestamp: int
    periods: {first: int?, second: int?}       // kick-off timestamps of each half
    venue: {id: int?, name: string?, city: string?}   // id null OR 0 observed
    status: object
      long: string
      short: string             // see section 8
      elapsed: int?             // minutes; null before start
      extra: int?               // added time of the current/last half (3.9.3); e.g. 4, 11
  league: object
    id: int
    name: string
    country: string
    logo: string
    flag: string?
    season: int
    round: string
    standings: bool             // 3.9.3: competition covers standings
  teams: object
    home|away: object
      id: int
      name: string
      logo: string
      winner: bool?             // null when draw or not finished
  goals: {home: int?, away: int?}
  score: object
    halftime|fulltime|extratime|penalty: {home: int?, away: int?}
  // only with id / ids:
  events: <element of /fixtures/events>[]
  lineups: <element of /fixtures/lineups>[]
  statistics: <element of /fixtures/statistics, without halves>[]
  players: <element of /fixtures/players>[]
  // only with live:
  events: <element of /fixtures/events>[]
```
Real vs docs: `extra` and `league.standings` present as per changelog. `status.long` wording differs (section 8).

Knockout ties (real): there is no leg, aggregate or tie winner. Two legs are the two fixtures of one round between the same
teams, ordered by `timestamp`. `goals` includes extra time (Juventus 3-2 Galatasaray AET has `fulltime` 3-0, `extratime`
0-2, `goals` 3-2), so the aggregate is the sum of `goals`; the shoot-out is only in the last leg's `score.penalty`.
`teams.*.winner` is per match: true for Juventus in that second leg although Galatasaray went through 7-5, and null for
both sides in a second leg drawn after extra time (Dinamo Minsk 2-2 Ludogorets, Ludogorets through 3-2). In single
matches `winner` includes the shoot-out. `score.extratime` is `{null, null}` in 3 of the 4 World Cup 2026 shoot-outs
(Germany-Paraguay, Netherlands-Morocco, Australia-Egypt) although extra time was played: use the status (`AET`/`PEN`),
not `extratime`. Group-stage fixtures carry no group. Library: `Ties`/`Tie`, `Groups`. Fixtures:
`fixtures_by_league_ucl_2025.json` (`/fixtures?league=2&season=2025`, every fixture except 140 of the 144 league-phase
ones), `fixtures_by_league_world_cup_2026.json` (`/fixtures?league=1&season=2026`, all 104).
Fixtures: `fixtures_by_id.json` (full detail), `fixtures_by_ids.json`, `fixtures_live.json` (with events),
`fixtures_by_date.json` (`timezone=Europe/Madrid`), `fixtures_by_date_sunday.json` (1 235 results, many statuses),
`fixtures_last.json`, `fixtures_next.json`, `fixtures_status_multi.json` (`status=PEN-AET`, extratime/penalty
scores), `fixtures_empty.json` (empty result), `fixtures_invalid_date_overflow.json`,
`fixtures_invalid_timezone_fallback.json`, `fixtures_by_id_penalty_shootout.json` (`PEN` fixture with all its events),
`fixtures_by_id_own_goal.json` (fixture with an own goal), `error_unknown_param.json`, `error_invalid_integer.json`,
`error_last_too_large.json`, `error_too_many_ids.json`.

### 11.14 `GET /fixtures/headtohead`
Head-to-head fixtures between two teams. Update: every 15 s. Recommended: 1/minute when a fixture is in progress,
otherwise 1/day.

| Param | Type | | Notes |
|---|---|---|---|
| h2h | string | R | `ID-ID` (two team ids) |
| date | string | O | YYYY-MM-DD |
| league | int | O | |
| season | int | O | YYYY |
| last | int | O | X last fixtures |
| next | int | O | X next fixtures |
| from | string | O | YYYY-MM-DD |
| to | string | O | YYYY-MM-DD |
| status | string | O | one or more shorts `NS-PST-FT` |
| venue | int | O | |
| timezone | string | O | |

Response: same element as `/fixtures` (without events/lineups/statistics/players).
Without `last` the result includes upcoming fixtures (`NS`). `last=N` returns only fixtures already played, so it is not
"the N latest dates". The history is incomplete for older cup ties (old Copa del Rey meetings can be missing; data goes
back to about 2012): do not treat the list as exhaustive.
Fixture: `fixtures_headtohead.json` (`h2h=541-529&last=3`).

### 11.15 `GET /fixtures/statistics`
Team statistics for one fixture. Update: every minute. Recommended: 1/minute while in progress, otherwise 1/day.

| Param | Type | | Notes |
|---|---|---|---|
| fixture | int | R | |
| team | int | O | |
| type | string | O | statistic name, e.g. `Total Shots` |
| half | bool | O | default false; add first/second half stats (data from 2024 season) |
```
response: object[]            // one per team
  team: {id: int, name: string, logo: string}
  statistics: object[]
    type: string              // see section 9
    value: int | string | null   // int counts; "37%" for Ball Possession; "1.71" for expected_goals; null when absent (docs)
  statistics_1h: object[]     // only with half=true (same element)
  statistics_2h: object[]     // only with half=true
```
Real vs docs: types differ from the docs list and between fixtures ("Free Kicks", "expected_goals",
"goals_prevented" added; "Passes %" absent in 2026 La Liga, present in 2025 fixtures; section 9). `value`
type varies **per element** — model as `JsonElement`/custom adapter (`Integer`, `String`, `null`).
Fixtures: `fixtures_statistics.json`, `fixtures_statistics_half.json` (not trimmed inside).

### 11.16 `GET /fixtures/events`
Events of one fixture. Update: every 15 s. Recommended: 1/minute while in progress, otherwise 1/day
(also available in `fixtures?live=all`).

| Param | Type | | Notes |
|---|---|---|---|
| fixture | int | R | |
| team | int | O | |
| player | int | O | |
| type | string | O | `goal`, `card`, `subst`, `var` |
```
response: object[]
  time: {elapsed: int, extra: int?}
  team: {id: int, name: string, logo: string}
  player: {id: int?, name: string?}      // null observed in a live fixture
  assist: {id: int?, name: string?}      // for subst: the player coming in (real); null for most cards
  type: string                          // "Goal", "Card", "subst", "Var"
  detail: string                        // "Normal Goal", "Yellow Card", "Substitution 1", ...
  comments: string?                     // "Foul", "Dissent", ...
```
Real vs docs: `type` for substitutions is lower case `"subst"`. Fixture: `fixtures_events.json` (only
"Normal Goal", "Yellow Card" and "Substitution N" events); an own goal and a penalty during play are in
`fixtures_by_id_own_goal.json` and `fixtures_by_id_penalty_shootout.json` (all events kept).

Shapes seen live in `fixtures?live=all` during a comparison run on 2026-11-12 (6 fixtures, polled every 30 s;
recorded after parsing, so not stored as fixtures):
* **Missed penalty**: `type "Goal"`, `detail "Missed Penalty"` (2 cases). `player` is the taker; in one case it was `{}`
  (null members) in the first poll and filled 60 s later, and in one case `assist` repeated the taker.
  The API's `goals`/`score` did not count it.
* **Scored penalty**: `type "Goal"`, `detail "Penalty"` (4 cases), also in extra time (`elapsed 120, extra 4`).
* **Own goal** (verified, fixture 1390944 Getafe 0-1 Atletico Madrid, `fixtures_by_id_own_goal.json`):

  | member | value |
  |---|---|
  | `type` / `detail` | `"Goal"` / `"Own Goal"` |
  | `time` | `{elapsed: 82, extra: null}` |
  | `team` | **Atletico Madrid (530), the team the goal counts for** (the away side, which won 1-0) |
  | `player` | `{id: 46795, name: "D. Duarte"}` — a **Getafe** player (in Getafe's `startXI` and `players`) |
  | `assist` / `comments` | `{id: null, name: null}` / `null` |
  | `goals` / `score.fulltime` | 0-1: the goal counts for the event's `team` |

  So `team` = beneficiary, `player` = a player of the **other** team. `FixtureEvent.getTeamGoalCountsFor()` names
  this. Sportmonks agrees: own goal = type id 15, `participant_id` = the benefiting team, `player_name`
  "Domingos Duarte", `result "0-1"`, `addition "1st Own Goal"`.
* **Second yellow**: the API sends `"Yellow Card"` then `"Red Card"` for the same player at the same minute (recorded:
  A. Ruibal, `elapsed 118`, after an earlier yellow at `90+4`, Navalcarnero-Betis, fixture 1644363). No `"Second Yellow
  card"` detail was seen. A red with no yellow of the same player at the same minute is a straight red (L. Krejci, 72',
  Czech Republic-Spain, fixture 1528961). The `player.id` can be missing in the first polls after an event appears (seen on
  a yellow card at 116'). Library: `Fixture.isSecondYellowRed(event)`, `getCardEventsMerged()`; matching rules in
  `FixtureEventAnalysis`.
* **VAR**: `"Penalty confirmed"`, `"Goal cancelled"` and the undocumented `"Card upgrade confirmed"`. Three VAR
  events first appeared **without `detail`** and got it 30 s later. A cancelled goal's `"Goal"` event was
  removed and replaced by the `"Var"`/`"Goal cancelled"` event (`goals` reverted one poll before the event list
  changed). A confirmed penalty and an upgraded card are followed by their own Goal/Card events.
* **Substitution**: `player` = player going **off**, `assist` = player coming **on** (49 of 49 events checked
  against the lineups of the same fixtures: `player` in `startXI`, `assist` in `substitutes`; confirmed against
  Sportmonks, which names them the other way round).
* **Penalty shoot-out: the kicks are not events** (verified, fixture 1544371, Champions League final 2026-05-30,
  PSG - Arsenal, `fixtures_by_id_penalty_shootout.json`):

  | field | value |
  |---|---|
  | `fixture.status` | `{long: "Match Finished", short: "PEN", elapsed: 120, extra: null}` |
  | `goals` | 1-1 (extra time included, shoot-out excluded) |
  | `score.halftime` / `fulltime` / `extratime` / `penalty` | 0-1 / 1-1 / **0-0** (not null: ET was played) / **4-3** |
  | `events` | 19: 2 goals (`Normal Goal` 6', `Penalty` 65' during play), 6 cards, 11 substitutions; **last event a card at 118'** |
  | shoot-out kicks in `events` | **none** — neither the 7 scored nor the 3 missed/saved kicks (Sportmonks lists all 10) |
  | `players[].penalty` | shoot-out kicks not counted (`scored` 1 only for the 65' taker; the saving goalkeeper `saved: 0`) |

  So `score.penalty` is the only place the shoot-out appears, and counting `isGoal()` events per team reproduces
  `goals` (1-1). A penalty during play in a match decided on penalties is an ordinary `"Penalty"` event with a
  match minute. Not seen yet: a match where a penalty is *missed* during play and then goes to a shoot-out
  (it would be an ordinary `"Missed Penalty"` event; nothing suggests otherwise).
  Sportmonks, for comparison, does list the kicks: type id 23 (shoot-out goal, `addition "Scored"`) and 22
  (shoot-out miss, `addition "Off target"` / `"Goalkeeper save"`), in the `penalties` period, `minute` = kick number
  1..10, `result` = running shoot-out score (`"4-3"` after the last kick); state `FT_PEN` ("After Penalties"); score
  type `PENALTY_SHOOTOUT` 4-3, `CURRENT` 1-1.
* **Event order and names**: events are **not strictly sorted** by time (a card at `90+6` was listed before a
  substitution at `90`, and `90+1` before `90`), and the same player id carries different spellings in different
  events of one fixture (`"Julián Alvarez"` on a card, `"J. Alvarez"` on a substitution). Sort by
  `elapsed`/`extra` and match players by id. The library does it: `Fixture.getEventsSorted()` (stable, same minute keeps the
  API's order, no-time events last) and the comparator `FixtureEvent.BY_TIME` (null `extra` = 0); `getEvents()` keeps the API's order.
* **Transient data**: events can appear for a single poll and vanish (a `"Normal Goal"` without player at
  119' made the API's own `goals` 2-4 for one poll in fixture 1644363 and was then removed).

### 11.17 `GET /fixtures/lineups`
Lineups (formation, coach, XI, substitutes). Available 20–40 minutes before kick-off when covered (`coverage.fixtures.lineups`);
otherwise sometimes only after the match. Update: every 15 minutes. Recommended: 1 every 15 min while in progress,
otherwise 1/day. Grid: see "Lineup grid" below (the home team's columns are mirrored compared with Sportmonks).

| Param | Type | | Notes |
|---|---|---|---|
| fixture | int | R | |
| team | int | O | |
| player | int | O | |
| type | string | O | e.g. `startXI` |
```
response: object[]          // one per team
  team: object
    id: int
    name: string
    logo: string
    colors: object?         // docs: {player: {primary, number, border}, goalkeeper: {primary, number, border}} hex strings without '#'
  formation: string         // "4-4-2"
  coach: {id: int?, name: string?, photo: string?}
  startXI: object[]
    player: {id: int, name: string, number: int, pos: string, grid: string?}   // pos G/D/M/F, grid "2:4"
  substitutes: object[]
    player: {id: int, name: string, number: int, pos: string?, grid: null}
```
Real vs docs: `team.colors` was **null** for both teams in the 2026 La Liga lineups (docs example has the object),
but **present** (`{player: {primary: "18345e", number, border}, goalkeeper: {...}}`) in the 2025 fixtures
`fixtures_by_id_penalty_shootout.json` and `fixtures_by_id_own_goal.json` → nullable.

**Lineup grid.** `grid` is `"row:column"`. Row 1 is the goalkeeper and the following rows are the lines from the back to
the front (a 4-3-3 has rows 1 to 4); the column numbers the players of that row. Substitutes have `grid: null`.
**Neither API-Football nor Sportmonks gives metric coordinates**: a grid only says "third of four players in the second
line", so the app places the dots itself. Library: `LineupPlayer.getGridRow()` / `getGridColumn()` parse it (`null` when
absent or malformed).

**Home-team columns are mirrored compared with Sportmonks.** Checked on 2026-11-12 by matching every starter by shirt
number against Sportmonks' `lineups[].formation_field` in the 5 matches that had a lineup in both providers
(Turkey-Belgium, Italy-France, Czech Republic-Spain, England-Croatia, Navalcarnero-Betis):

| Side | Rows equal | Columns equal as sent | Columns equal after `playersInRow + 1 - column` |
|---|---|---|---|
| Home | 11 of 11 in all 5 matches | only 3 of 11 (the central players) | **11 of 11 in all 5 matches** |
| Away | 11 of 11 in all 5 matches | **11 of 11 in all 5 matches** | 3 of 11 |

Transformation, **home team only**, rows unchanged: `column' = playersInRow + 1 - column`, with `playersInRow` the number
of starters of that team that share the row (the goalkeeper's row has 1, so the goalkeeper never moves). It is its own
inverse, so it converts in both directions. It needs the full starting XI: with fewer than 11 starters the row count is
wrong, so check `getStartXI().size() == 11` first. Library: `FixtureLineup.getGridColumnMirrored(player)` (counts the row
for you; apply it to the home lineup only) or `Fixture.getGridColumnAsSportmonks(lineup, player)` (mirrors when the
lineup is the home team's, returns the column unchanged for the away team, `null` for a lineup of another fixture).

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

France (away, 4-2-3-1) in the same match: grid identical to Sportmonks for all 11 (e.g. M. Gusto `2:4` in both). Without
the correction, an XI drawn from the home grid appears left-right reversed next to the away one. Sportmonks'
`formation_position` (1-11) has no counterpart in API-Football.

Fixture: `fixtures_lineups.json`.

### 11.18 `GET /fixtures/players`
Per-player statistics for one fixture. Update: every minute. Recommended: 1/minute while in progress, otherwise 1/day.

| Param | Type | | Notes |
|---|---|---|---|
| fixture | int | R | |
| team | int | O | |
```
response: object[]          // one per team
  team: {id: int, name: string, logo: string, update: string}   // real update "1970-01-01T00:00:00+00:00" (epoch!)
  players: object[]
    player: {id: int, name: string, photo: string}
    statistics: object[]    // one element
      games: {minutes: int?, number: int, position: string, rating: string?, captain: bool, substitute: bool}
      offsides: int?
      shots: {total: int?, on: int?}
      goals: {total: int?, conceded: int?, assists: int?, saves: int?}
      passes: {total: int?, key: int?, accuracy: string?}
      tackles: {total: int?, blocks: int?, interceptions: int?}
      duels: {total: int?, won: int?}
      dribbles: {attempts: int?, success: int?, past: int?}
      fouls: {drawn: int?, committed: int?}
      cards: {yellow: int, red: int}
      penalty: {won: int?, commited: int?, scored: int?, missed: int?, saved: int?}
```
Real vs docs: `passes.accuracy` is a **string count** in real data (`"9"`), a **percentage string** in the docs
example (`"68%"`), and an **int** in `/players`. `rating` is a string (`"6.29"`; `"0"` for unused subs). In the real
La Liga sample most numbers were `0` rather than `null`, but `goals.conceded`, `dribbles.past`, `penalty.won/commited/missed/saved`
were always `null`; the docs example has many nulls → treat every statistic as nullable. `team.update` was the Unix
epoch. Fixture: `fixtures_players.json`.
**Verified on real fixtures** (`fixtures_by_id_penalty_shootout.json`, `fixtures_by_id_own_goal.json`):
`penalty.scored/missed/saved` count **penalties during play only**, never shoot-out kicks (in the 4-3 shoot-out of
fixture 1544371 the goalkeeper who saved a kick has `saved: 0` and only the 65' penalty is `scored`); and an **own
goal is credited to nobody** in `goals.total` (fixture 1390944, won 1-0 by an own goal: no player has a goal; the
own-goal scorer has `total: null`). Summing `goals.total` therefore need not give the score.

### 11.19 `GET /injuries`
Players missing a fixture (injured, suspended…). Data from April 2021. **At least one parameter required.** All
parameters combinable. Update: every 4 hours. Recommended: 1 call/day.

| Param | Type | | Notes |
|---|---|---|---|
| league | int | O | needs season |
| season | int | C | YYYY; required with league, team and player |
| fixture | int | O | |
| team | int | O | needs season |
| player | int | O | needs season |
| date | string | O | YYYY-MM-DD |
| ids | string | O | `id-id-id` fixture ids, max 20 (3.9.3) |
| timezone | string | O | |
```
response: object[]
  player: object
    id: int
    name: string
    photo: string
    type: string        // "Missing Fixture" | "Questionable"
    reason: string      // free text
  team: {id: int, name: string, logo: string}
  fixture: {id: int, timezone: string, date: string, timestamp: int}
  league: {id: int, season: int, name: string, country: string, logo: string, flag: string?}
```
Fixtures: `injuries_by_league_season.json` (962 results, 534 KB untrimmed), `injuries_by_fixture.json` (empty result).

### 11.20 `GET /predictions`
Predictions for one fixture (Poisson, team stats, h2h…; bookmaker odds are not used) plus comparison data.
Update: every hour. Recommended: 1/hour while in progress, otherwise 1/day.

| Param | Type | | Notes |
|---|---|---|---|
| fixture | int | R | |
```
response: object[]   // one element
  predictions: object
    winner: {id: int?, name: string?, comment: string?}   // comment e.g. "Win or draw"; null observed
    win_or_draw: bool
    under_over: string?          // "-3.5"
    goals: {home: string, away: string}    // "-3.5", "-1.5"
    advice: string               // "Combo Winner : Real Madrid and -3.5 goals"
    percent: {home: string, draw: string, away: string}   // "50%"
  league: {id: int, name: string, country: string, logo: string, flag: string?, season: int}
  teams: object
    home|away: object
      id: int
      name: string
      logo: string
      last_5: object
        played: int
        form: string           // "100%"
        att: string            // "100%"
        def: string
        goals: {for: {total: int, average: string}, against: {total: int, average: string}}
      league: object           // same structure as /teams/statistics response WITHOUT league/team:
        form, fixtures{played,wins,draws,loses}, goals{for,against{total,average,minute,under_over}},
        biggest, clean_sheet, failed_to_score, penalty, lineups[], cards{yellow,red}
  comparison: object
    form|att|def|poisson_distribution|h2h|goals|total: {home: string, away: string}   // "87%"
  h2h: <fixture element as in /fixtures>[]   // previous meetings (10 observed)
```
Real: `biggest.loses.home` null when none. Fixture: `predictions.json` (h2h trimmed).

### 11.21 `GET /coachs`
Coaches with career. Update: daily. Recommended: 1/day.

| Param | Type | | Notes |
|---|---|---|---|
| id | int | O | coach id |
| team | int | O | team id (returns current and past coaches of that team) |
| search | string | O | min 3; coach name |
```
response: object[]
  id: int
  name: string
  firstname: string
  lastname: string
  age: int?
  birth: {date: string?, place: string?, country: string?}
  nationality: string?
  height: string?        // "185 cm"
  weight: string?        // "78 kg"
  photo: string
  team: {id: int, name: string, logo: string}     // current team
  career: object[]
    team: {id: int, name: string, logo: string}
    start: string        // YYYY-MM-DD
    end: string?         // null = current
```
Fixture: `coachs_by_team.json`.

### 11.22 `GET /players/seasons`
Seasons available for player statistics. Update: daily. Recommended: 1/day.

| Param | Type | | Notes |
|---|---|---|---|
| player | int | O | without it: all seasons (1962…2027 observed) |
```
response: int[]
```
Fixtures: `players_seasons.json`, `players_seasons_by_player.json`.

### 11.23 `GET /players/profiles`
All players (profile only). **Paginated, 250 per page** (2 771 pages without filters).
Update: several times a week. Recommended: 1 call/week.

| Param | Type | | Notes |
|---|---|---|---|
| player | int | O | |
| search | string | O | min 3; player lastname |
| page | int | O | default 1 |
```
response: object[]
  player: object
    id: int
    name: string
    firstname: string?
    lastname: string?
    age: int?
    birth: {date: string?, place: string?, country: string?}
    nationality: string?
    height: string?      // INCONSISTENT FORMAT: "187" or "185 cm"
    weight: string?      // "85" or "82 kg"
    number: int?
    position: string?    // Goalkeeper | Defender | Midfielder | Attacker
    photo: string
```
Fixtures: `players_profiles_by_player.json`, `players_profiles_page1.json`.

### 11.24 `GET /players`
Player profile + statistics per team/league/season. **Paginated, 20 per page.** A player may have several statistics
entries (transfers, several competitions). Rating is computed per position. Update: several times a week.
Recommended: 1/day.

| Param | Type | | Notes |
|---|---|---|---|
| id | int | O | player id |
| team | int | O | |
| league | int | O | |
| season | int | C | YYYY; requires id, league or team |
| search | string | C | min 4 chars (docs; the client rejects shorter terms before the request); requires league or team |
| page | int | O | default 1 |
```
response: object[]
  player: object
    id: int
    name: string
    firstname: string
    lastname: string
    age: int
    birth: {date: string, place: string?, country: string}
    nationality: string
    height: string?      // real "173" (no unit) vs docs "175 cm"
    weight: string?
    injured: bool
    photo: string
  statistics: object[]
    team: {id: int, name: string, logo: string}
    league: {id: int, name: string, country: string, logo: string, flag: string?, season: int}
    games: {appearences: int, lineups: int, minutes: int?, number: int?, position: string, rating: string?, captain: bool}
    substitutes: {in: int, out: int?, bench: int?}
    shots: {total: int?, on: int?}
    goals: {total: int, conceded: int, assists: int?, saves: int?}
    passes: {total: int?, key: int?, accuracy: int?}     // int here (string in /fixtures/players)
    tackles: {total: int?, blocks: int?, interceptions: int?}
    duels: {total: int?, won: int?}
    dribbles: {attempts: int?, success: int?, past: int?}
    fouls: {drawn: int?, committed: int?}
    cards: {yellow: int, yellowred: int?, red: int}
    penalty: {won: int?, commited: int?, scored: int, missed: int, saved: int?}
```
Real: with `league=140&season=2026` each player's `statistics` contained only league 140 (one entry per team;
21 entries for 20 players on page 1 → a player transferred mid-season has two). With `id=278&season=2026` (no
league) all competitions of that season are returned (La Liga, Champions League, World Cup, Nations League,
friendlies → 5 entries).
Fixtures: `players_by_id_season.json`, `players_page1.json`, `players_page2.json` (paging current=2),
`players_page_out_of_range.json`, `error_players_search_without_scope.json`.

### 11.25 `GET /players/squads`
Current squad of a team, or the teams of a player. **At least one parameter.** Same response format for both.
Update: several times a week. Recommended: 1/week.

| Param | Type | | Notes |
|---|---|---|---|
| team | int | O | |
| player | int | O | |
```
response: object[]
  team: {id: int, name: string, logo: string}
  players: object[]
    id: int
    name: string
    age: int?
    number: int?         // docs example shows null
    position: string
    photo: string
```
Fixtures: `players_squads.json`, `players_squads_by_player.json`.

### 11.26 `GET /players/teams`
Teams and seasons of a player's career (3.9.3). Update: several times a week. Recommended: 1/week.

| Param | Type | | Notes |
|---|---|---|---|
| player | int | R | |
```
response: object[]
  team: {id: int, name: string, logo: string}
  seasons: int[]       // descending
```
Fixture: `players_teams.json`.

### 11.27 `GET /players/topscorers`
Best scorers of a league/season: exactly 20 rows (LaLiga 2026), no pagination and no way to get the rest (a provider
that pages the full list, such as Sportmonks, gives more). Tie-breakers (docs): goals desc, penalties asc, assists desc, matches with goals
desc, minutes asc, team rank, red cards asc, yellow cards asc. Update: several times a week. Recommended: 1/day.

| Param | Type | | Notes |
|---|---|---|---|
| league | int | R | |
| season | int | R | YYYY |

Response: same element as `/players` (player + statistics[]). Fixture: `players_topscorers.json`.

### 11.28 `GET /players/topassists`
20 best assist providers. Tie-breakers (docs): assists, goals, fewer penalties, matches with assists, fewer minutes,
fewer red, fewer yellow. Same params/response as topscorers. Fixture: `players_topassists.json`.

### 11.29 `GET /players/topyellowcards`
20 players with most yellow cards (then red cards, matches, fewer minutes). Same params/response.
Fixture: `players_topyellowcards.json`.

### 11.30 `GET /players/topredcards`
20 players with most red cards (then yellow, matches, fewer minutes). Same params/response.
Fixture: `players_topredcards.json`.

### 11.31 `GET /transfers`
Transfers of a player or of a team. Update: several times a week. Recommended: 1/day.

| Param | Type | | Notes |
|---|---|---|---|
| player | int | C | player or team required |
| team | int | C | |
```
response: object[]        // one per player
  player: {id: int, name: string}
  update: string          // ISO datetime
  transfers: object[]
    date: string          // YYYY-MM-DD
    type: string?         // free text: "Free", "Loan", "N/A", "€ 180M", ...
    teams: object
      in: {id: int, name: string, logo: string}
      out: {id: int, name: string, logo: string}
```
`team=` responses contain one element per player and can be large (not captured to save budget).
Fixture: `transfers_by_player.json`.

### 11.32 `GET /trophies`
Trophies of a player or coach. Update: several times a week. Recommended: 1/day.

| Param | Type | | Notes |
|---|---|---|---|
| player | int | O | |
| players | string | O | `id-id-id`, max 20 (3.9.3) |
| coach | int | O | |
| coachs | string | O | `id-id-id`, max 20 (3.9.3) |

**The response shape changes with the plural parameters:**
```
// player / coach
response: object[]
  league: string
  country: string
  season: string?       // "2024/2025", "2025", "1998 France"; null observed
  place: string         // "Winner", "2nd Place"
// players / coachs
response: object[]
  id: int               // player or coach id
  trophies: <element above>[]
```
Fixtures: `trophies_by_player.json`, `trophies_by_coachs.json`.

### 11.33 `GET /sidelined`
Injury/suspension history of a player or coach. Update: several times a week. Recommended: 1/day.

| Param | Type | | Notes |
|---|---|---|---|
| player | int | O | |
| players | string | O | `id-id-id`, max 20 |
| coach | int | O | |
| coachs | string | O | `id-id-id`, max 20 |

Shape changes like trophies:
```
// player / coach
response: object[]
  type: string          // "Ankle Injury", "Suspended", ...
  start: string         // YYYY-MM-DD
  end: string?          // null observed (ongoing)
// players / coachs
response: object[]
  id: int
  sidelined: <element above>[]
```
Fixtures: `sidelined_by_player.json`, `sidelined_by_players.json`, `sidelined_by_coach.json` (empty result).

### 11.34 `GET /odds/live`
In-play odds for fixtures in progress. Fixtures appear 15–5 minutes before kick-off and disappear 5–20 minutes after
the end; **no history**. Update: every 5 s (5–60 s). No recommended frequency given.

| Param | Type | | Notes |
|---|---|---|---|
| fixture | int | O | |
| league | int | O | season not needed |
| bet | int | O | **live** bet id (`/odds/live/bets`) |
```
response: object[]
  fixture: object
    id: int
    status: {long: string, elapsed: int, seconds: string}     // "First Half", 14, "14:19" (no short!)
  league: {id: int, season: int}
  teams: object
    home|away: {id: int, goals: int}
  status: {stopped: bool, blocked: bool, finished: bool}
  update: string
  odds: object[]
    id: int             // live bet id
    name: string
    values: object[]
      value: string     // "Home", "Over", ...
      odd: string       // "2.425"
      handicap: string? // "1.5", "-1", null
      main: bool?       // true only when several identical values exist; otherwise false or null
      suspended: bool
```
Fixture: `odds_live.json` (1 live fixture at capture time).

### 11.35 `GET /odds/live/bets`
Bets available for in-play odds (ids NOT compatible with `/odds`). Update: every 60 s.

| Param | Type | | Notes |
|---|---|---|---|
| id | string | O | bet id |
| search | string | O | bet name (docs schema says length 3; example uses `winner`) |
```
response: object[]
  id: int
  name: string?
```
266 bets observed. Fixture: `odds_live_bets.json`. Full list in Appendix A.

### 11.36 `GET /odds`
Pre-match odds. **Paginated, 10 per page.** Odds available 1–14 days before the fixture, 7-day history.
Update: every 3 hours. Recommended: 1 call every 3 hours.

| Param | Type | | Notes |
|---|---|---|---|
| fixture | int | O | |
| league | int | O | use with season |
| season | int | O | YYYY |
| date | string | O | YYYY-MM-DD |
| timezone | string | O | |
| page | int | O | default 1 |
| bookmaker | int | O | |
| bet | int | O | **pre-match** bet id (`/odds/bets`) |
```
response: object[]
  league: {id: int, name: string, country: string, logo: string, flag: string?, season: int}
  fixture: {id: int, timezone: string, date: string, timestamp: int}
  update: string
  bookmakers: object[]
    id: int
    name: string
    bets: object[]
      id: int
      name: string
      values: object[]
        value: string | int     // int for "Exact Goals Number"-type bets (e.g. 0, 1, 2), string otherwise
        odd: string             // "1.20"
```
Real: `values[].value` is **a JSON number for some bets** (ids 38, 40, 41 observed) — use a lenient adapter (read as
string). A single page for a league can be ≈1.6 MB. Fixtures: `odds_by_fixture.json`,
`odds_by_league_season.json` (paging total 2), `odds_by_date_page2.json` (paging current 2 of 24).

Verified 2026-11-12/13 (Real Madrid - Celta, fixture 1570461): the fixture `date` of `/odds` is provisional
(`2026-11-22T16:00`, the real kick-off is `2026-11-21T20:00` in `/fixtures`), and `odds?league=140&season=2026&date=2026-11-21`
returned no result for that fixture. Request odds by fixture id. `update` is one timestamp per fixture (here
`2026-11-08T16:51:14+00:00`, 5 days old), not per bookmaker or selection. A fixture 15 days ahead returned 0 results
(window: 1-14 days before the fixture). Of the five bookmakers a real app maps, Bet365 (8), William Hill (7) and Betfair
(3) were present for that fixture; 888Sport (21) and Interwetten (15) are in `/odds/bookmakers` but did not appear in any
fixture checked. The "Exact Score" bet (id 10) lists 40 selections for Bet365 as `home:away` (`1:0`, `2:1`, `0:1`...);
for the home favourite (Match Winner home 1.18) `1:0` pays 10.00 and `0:1` pays 34.00, which fixes the order. Library:
`odds.util.BookmakerIds` (checked against Appendix A), `PrematchBetOdds.findValue/getHomeOdd/...`,
`OddValue.getExactScoreHome/Away`, `PrematchOdds.getUpdateAsInstant`.

### 11.37 `GET /odds/mapping`
Fixture ids for which pre-match odds exist. **Paginated, 100 per page.** Update: daily. Recommended: 1/day.

| Param | Type | | Notes |
|---|---|---|---|
| page | int | O | default 1 |
```
response: object[]
  league: {id: int, season: int}
  fixture: {id: int, date: string, timestamp: int}
  update: string
```
Fixture: `odds_mapping.json` (31 pages).

### 11.38 `GET /odds/bookmakers`
Bookmakers. Update: several times a week. Recommended: 1/day.

| Param | Type | | Notes |
|---|---|---|---|
| id | int | O | |
| search | string | O | name (docs schema length 3; example `Betfair`) |
```
response: object[]
  id: int
  name: string?        // bookmaker 37 has name null
```
Fixture: `odds_bookmakers.json`. Full list in Appendix A.

### 11.39 `GET /odds/bets`
Pre-match bets (ids NOT compatible with `/odds/live`). Update: several times a week. Recommended: 1/day.

| Param | Type | | Notes |
|---|---|---|---|
| id | string | O | |
| search | string | O | name |
```
response: object[]
  id: int
  name: string?        // bet 341 has name null
```
338 bets observed. Fixture: `odds_bets.json`. Full list in Appendix A.

---

## 12. Docs vs reality — summary of differences

| Area | Docs | Real (2026-11-12) |
|---|---|---|
| `/status` `results` | 1 | 0 |
| `/timezone` count | 425 | 427 |
| `/leagues/seasons` element type | schema: string | int |
| `/teams/statistics` key | schema: `matches` | `fixtures` (example also `fixtures`) |
| `/teams/statistics` cards maps | 8 minute keys | 8 minute keys + empty-string key `""` |
| fixture status long | "First Half, Kick Off", "Technical Loss" | "First Half", "Technical loss" |
| event type substitution | "Subst" | "subst" |
| VAR event details | Goal cancelled, Penalty confirmed | also "Card upgrade confirmed"; `detail` missing for ~30 s on new VAR events |
| fixture statistics types | 16 incl. "Passes %" | varies: 17 (2026 La Liga: no "Passes %", plus "Free Kicks", "expected_goals"); 18 (2025: with "Passes %", plus "expected_goals", "goals_prevented") |
| lineups `team.colors` | object | null (2026 La Liga) or object (2025 fixtures) |
| penalty shoot-out in events | (not stated) | shoot-out kicks are not events; only `score.penalty` has the shoot-out |
| own goal `team` | (not stated) | the team the goal counts for; `player` is from the other team |
| `/fixtures/players` `passes.accuracy` | "68%" | "9" (string count) |
| `/players` `height`/`weight` | "175 cm" | "173" (profiles: both formats) |
| `/odds` `values[].value` | string | string or int |
| `venue.id` | int | int, null or 0 |
| invalid `date` / `timezone` | (not stated) | silently accepted (date overflow, UTC fallback) |
| `search` | schema: exactly 3 for countries/bookmakers/bets | min 3 (4 for `/players`, docs); only ASCII letters, digits and spaces (accents and symbols are rejected with the message in section 3); longer works per docs examples. The library validates this client-side (`QueryValidation.search`). |
| errors | 204 with errors object; 499/500 `{"message"}` | HTTP 200 with errors object for all validation errors |
| `paging` | absent from schemas | always present |

## 13. Other documentation sections (summary)

* **Introduction**: language bindings examples (C, C#, cURL, Dart, Go, Java, JS, NodeJs, Objective-C, OCaml, PHP,
  PowerShell, Python, Ruby, Shell, Swift). "The update frequency indicated in the documentation is given as an
  indication and may vary for certain competitions."
* **Authentication / Account**: dashboard (consumption, subscription, server status, live tester); `/status`.
* **Architecture**: an image only.
* **Logos / Images**: see section 7.
* **Sample Scripts**: per-language snippets for `GET {endpoint}` (the Java sample uses Unirest with the
  `x-rapidapi-*` headers).
* **Changelog**: 3.9.3 (players/profiles, players/teams, fixtures `extra` + `league.standings`, rounds `dates`,
  statistics `half`, injuries `ids`, teams/statistics goals over/under, sidelined & trophies `players`/`coachs`),
  3.9.2 (odds/live, odds/live/bets, teams `code`/`venue` params, teams/countries, fixtures `ids`, multi-status,
  `venue` param on fixtures & headtohead), 3.8.1 (injuries, players/squads, top assists/yellow/red, lineup grid &
  colours, VAR events, team tri-code, teams/statistics minute/cards/formation/penalty stats, coach photos).
* **Widgets** (tag "Widgets"): HTML `<api-sports-widget>` components (`games`, `game`, `team`, `player`, `standings`,
  `league`, `leagues`, `h2h`, F1/MMA ones) using the API key in `data-key` (visible to visitors — restrict by
  domain/IP in the dashboard); `data-target-*`, `data-lang` (en, fr, es, it) + `data-custom-lang`, themes
  (white, grey, dark, blue, custom CSS variables), `data-show-errors`; caching advice (60 s cache cuts 115 200
  requests/day to 1 440). Widgets consume normal API requests. Not relevant to a Java client.
* **Integrations**: CDN (BunnyCDN tutorial for images) and "Databases Solutions" (Aiven, Firebase) — marketing
  content, nothing to implement.
* **Tutorials referenced**: "How to get all teams and players from a league id", "How to get standings for all
  current seasons".
* x-tagGroups lists a "Seasons" tag (leagues/seasons is tagged "Leagues") and "Odds (In-Play)"/"Odds (Pre-Match)".

## 14. Fixture file index

| File | Request |
|---|---|
| status.json | `/status` (anonymised) |
| timezone.json | `/timezone` |
| countries.json / countries_by_name.json | `/countries`, `/countries?name=england` |
| leagues_by_id.json | `/leagues?id=140` |
| leagues_by_id_world_cup.json / leagues_by_id_ucl.json | `/leagues?id=1`, `/leagues?id=2` |
| leagues_current_country.json | `/leagues?country=Spain&current=true` |
| leagues_seasons.json | `/leagues/seasons` |
| teams_by_id.json | `/teams?id=541` |
| teams_by_league_season.json | `/teams?league=140&season=2026` |
| teams_statistics.json | `/teams/statistics?league=140&season=2025&team=541` |
| teams_seasons.json | `/teams/seasons?team=541` |
| teams_countries.json | `/teams/countries` |
| venues_by_id.json / venues_search.json | `/venues?id=1456`, `/venues?search=madrid` |
| standings_league.json | `/standings?league=140&season=2026` |
| standings_groups.json | `/standings?league=1&season=2026` (trimmed to 5 tables × 3 rows) |
| standings_world_cup_2026.json | `/standings?league=1&season=2026` (full, 13 tables) |
| standings_ucl_2025.json | `/standings?league=2&season=2025` |
| standings_laliga_2026.json | `/standings?league=140&season=2026` (full, 20 rows) |
| fixtures_rounds.json / fixtures_rounds_dates.json / fixtures_rounds_current.json | `/fixtures/rounds?league=140&season=2026` (+`dates=true`, +`current=true`) |
| fixtures_rounds_ucl_2025.json / fixtures_rounds_dates_ucl_2025.json | `/fixtures/rounds?league=2&season=2025` (+`dates=true`) |
| fixtures_rounds_world_cup_2026.json | `/fixtures/rounds?league=1&season=2026` |
| fixtures_rounds_laliga_2026.json | `/fixtures/rounds?league=140&season=2026` (all 38) |
| fixtures_by_league_ucl_2025.json | `/fixtures?league=2&season=2025` (league phase trimmed to 4 of 144) |
| fixtures_by_league_world_cup_2026.json | `/fixtures?league=1&season=2026` |
| fixtures_by_id.json | `/fixtures?id=1570443` |
| fixtures_by_ids.json | `/fixtures?ids=1570443-1570451` |
| fixtures_by_id_penalty_shootout.json | `/fixtures?id=1544371` (Champions League final 2026-05-30, PSG - Arsenal, `PEN`; every event kept) |
| fixtures_by_id_own_goal.json | `/fixtures?id=1390944` (La Liga 2025-11-23, Getafe - Atletico Madrid, own goal; every event kept) |
| fixtures_live.json | `/fixtures?live=all` |
| fixtures_by_date.json | `/fixtures?date=2026-11-12&timezone=Europe/Madrid` |
| fixtures_by_date_sunday.json | `/fixtures?date=2026-11-08` |
| fixtures_last.json / fixtures_next.json | `/fixtures?league=140&last=10`, `/fixtures?league=140&next=5` |
| fixtures_status_multi.json | `/fixtures?league=1&season=2026&status=PEN-AET` |
| fixtures_empty.json | `/fixtures?league=140&season=2026&date=2026-11-12` |
| fixtures_invalid_date_overflow.json | `/fixtures?date=2026-13-45` |
| fixtures_invalid_timezone_fallback.json | `/fixtures?date=2026-11-12&timezone=Mars/Olympus` |
| fixtures_headtohead.json | `/fixtures/headtohead?h2h=541-529&last=3` |
| fixtures_statistics.json / fixtures_statistics_half.json | `/fixtures/statistics?fixture=1570443` (+`half=true`) |
| fixtures_events.json | `/fixtures/events?fixture=1570443` |
| fixtures_lineups.json | `/fixtures/lineups?fixture=1570443` |
| fixtures_players.json | `/fixtures/players?fixture=1570443` |
| injuries_by_fixture.json | `/injuries?fixture=1570443` (empty) |
| injuries_by_league_season.json | `/injuries?league=140&season=2026` |
| predictions.json | `/predictions?fixture=1570461` |
| coachs_by_team.json | `/coachs?team=541` |
| players_by_id_season.json | `/players?id=278&season=2026` |
| players_page1.json / players_page2.json | `/players?league=140&season=2026` (page 1, 2) |
| players_page_out_of_range.json | `/players?league=140&season=2026&page=99` |
| players_seasons.json / players_seasons_by_player.json | `/players/seasons`, `?player=278` |
| players_profiles_by_player.json / players_profiles_page1.json | `/players/profiles?player=278`, `/players/profiles` |
| players_squads.json / players_squads_by_player.json | `/players/squads?team=541`, `?player=278` |
| players_teams.json | `/players/teams?player=278` |
| players_topscorers.json, players_topassists.json, players_topyellowcards.json, players_topredcards.json | `?league=140&season=2025` |
| transfers_by_player.json | `/transfers?player=278` |
| trophies_by_player.json / trophies_by_coachs.json | `/trophies?player=278`, `/trophies?coachs=2462-1584` |
| sidelined_by_player.json / sidelined_by_players.json / sidelined_by_coach.json | `/sidelined?player=278`, `?players=278-730`, `?coach=2462` |
| odds_by_fixture.json | `/odds?fixture=1570461` |
| odds_by_league_season.json | `/odds?league=140&season=2026` |
| odds_by_date_page2.json | `/odds?date=2026-11-14&page=2` |
| odds_mapping.json | `/odds/mapping` |
| odds_bookmakers.json / odds_bets.json | `/odds/bookmakers`, `/odds/bets` |
| odds_live.json / odds_live_bets.json | `/odds/live`, `/odds/live/bets` |
| error_missing_param.json | `/teams/statistics?league=140&season=2025` |
| error_no_params.json | `/teams` |
| error_unknown_param.json | `/fixtures?foo=bar` |
| error_search_too_short.json | `/teams?search=ma` |
| error_invalid_integer.json | `/fixtures?league=abc` |
| error_standings_missing_season.json | `/standings?league=140` |
| error_players_search_without_scope.json | `/players?search=mbappe` |
| error_last_too_large.json | `/fixtures?team=541&last=100` |
| error_too_many_ids.json | `/fixtures?ids=` with 21 ids |
| error_unknown_endpoint.json | `/foo` |

## Appendix A. Bookmakers and bet ids (snapshot 2026-11-12)

These lists are data that the API says is updated several times a week; treat them as a snapshot, not as a closed enum.

### Bookmakers (`/odds/bookmakers`, 33)

1 = 10Bet; 2 = Marathonbet; 3 = Betfair; 4 = Pinnacle; 5 = SBO; 6 = Bwin; 7 = William Hill; 8 = Bet365; 9 = Dafabet; 10 = Ladbrokes; 11 = 1xBet; 12 = BetFred; 13 = 188Bet; 15 = Interwetten; 16 = Unibet; 17 = 5Dimes; 18 = Intertops; 19 = Bovada; 20 = Betcris; 21 = 888Sport; 22 = Tipico; 23 = Sportingbet; 24 = Betway; 25 = Expekt; 26 = Betsson; 27 = NordicBet; 28 = ComeOn; 30 = Netbet; 32 = Betano; 33 = Fonbet; 34 = Superbet; 36 = BetVictor; 37 = null

### Pre-match bets (`/odds/bets`, 338)

1 = Match Winner; 2 = Home/Away; 3 = Second Half Winner; 4 = Asian Handicap; 5 = Goals Over/Under; 6 = Goals Over/Under First Half; 7 = HT/FT Double; 8 = Both Teams Score; 9 = Handicap Result; 10 = Exact Score; 11 = Highest Scoring Half; 12 = Double Chance; 13 = First Half Winner; 14 = Team To Score First; 15 = Team To Score Last; 16 = Total - Home; 17 = Total - Away; 18 = Handicap Result - First Half; 19 = Asian Handicap First Half; 20 = Double Chance - First Half; 21 = Odd/Even; 22 = Odd/Even - First Half; 23 = Home Odd/Even; 24 = Results/Both Teams Score; 25 = Result/Total Goals; 26 = Goals Over/Under - Second Half; 27 = Clean Sheet - Home; 28 = Clean Sheet - Away; 29 = Win to Nil - Home; 30 = Win to Nil - Away; 31 = Correct Score - First Half; 32 = Win Both Halves; 33 = Double Chance - Second Half; 34 = Both Teams Score - First Half; 35 = Both Teams To Score - Second Half; 36 = Win To Nil; 37 = Home win both halves; 38 = Exact Goals Number; 39 = To Win Either Half; 40 = Home Team Exact Goals Number; 41 = Away Team Exact Goals Number; 42 = Second Half Exact Goals Number; 43 = Home Team Score a Goal; 44 = Away Team Score a Goal; 45 = Corners Over Under; 46 = Exact Goals Number - First Half; 47 = Winning Margin; 48 = To Score In Both Halves By Teams; 49 = Total Goals/Both Teams To Score; 50 = Goal Line; 51 = Halftime Result/Total Goals; 52 = Halftime Result/Both Teams Score; 53 = Away win both halves; 54 = First 10 min Winner; 55 = Corners 1x2; 56 = Corners Asian Handicap; 57 = Home Corners Over/Under; 58 = Away Corners Over/Under; 59 = Own Goal; 60 = Away Odd/Even; 61 = To Qualify; 62 = Correct Score - Second Half; 63 = Odd/Even - Second Half; 72 = Goal Line (1st Half); 73 = Both Teams to Score 1st Half - 2nd Half; 74 = 10 Over/Under; 75 = Last Corner; 76 = First Corner; 77 = Total Corners (1st Half); 78 = RTG_H1; 79 = Cards European Handicap; 80 = Cards Over/Under; 81 = Cards Asian Handicap; 82 = Home Team Total Cards; 83 = Away Team Total Cards; 84 = Total Corners (3 way) (1st Half); 85 = Total Corners (3 way); 86 = RCARD; 87 = Total ShotOnGoal; 88 = Home Total ShotOnGoal; 89 = Away Total ShotOnGoal; 91 = Total Goals (3 way); 92 = Anytime Goal Scorer; 93 = First Goal Scorer; 94 = Last Goal Scorer; 95 = To Score Two or More Goals; 96 = Last Goal Scorer; 97 = First Goal Method; 99 = To Score A Penalty; 100 = To Miss A Penalty; 102 = Player to be booked; 103 = Player to be Sent Off; 104 = Asian Handicap (2nd Half); 105 = Home Team Total Goals(1st Half); 106 = Away Team Total Goals(1st Half); 107 = Home Team Total Goals(2nd Half); 108 = Away Team Total Goals(2nd Half); 109 = Draw No Bet (1st Half); 110 = Scoring Draw; 111 = Home team will score in both halves; 112 = Away team will score in both halves; 113 = Both Teams To Score in Both Halves; 114 = Home Team Score a Goal (1st Half); 115 = Home Team Score a Goal (2nd Half); 116 = Away Team Score a Goal (1st Half); 117 = Away Team Score a Goal (2nd Half); 118 = Home Win/Over; 119 = Home Win/Under; 120 = Away Win/Over; 121 = Away Win/Under; 122 = Home team will win either half; 123 = Away team will win either half; 124 = Home Come From Behind and Win; 125 = Corners Asian Handicap (1st Half); 126 = Corners Asian Handicap (2nd Half); 127 = Total Corners (2nd Half); 128 = Total Corners (3 way) (2nd Half); 129 = Away Come From Behind and Win; 130 = Corners 1x2 (1st Half); 131 = Corners 1x2 (2nd Half); 132 = Home Total Corners (1st Half); 133 = Home Total Corners (2nd Half); 134 = Away Total Corners (1st Half); 135 = Away Total Corners (2nd Half); 136 = 1x2 - 15 minutes; 137 = 1x2 - 60 minutes; 138 = 1x2 - 75 minutes; 139 = 1x2 - 30 minutes; 140 = DC - 30 minutes; 141 = DC - 15 minutes; 142 = DC - 60 minutes; 143 = DC - 75 minutes; 144 = Goal in 1-15 minutes; 145 = Goal in 16-30 minutes; 146 = Goal in 31-45 minutes; 147 = Goal in 46-60 minutes; 148 = Goal in 61-75 minutes; 149 = Goal in 76-90 minutes; 150 = Home Team Yellow Cards; 151 = Away Team Yellow Cards; 152 = Yellow Asian Handicap; 153 = Yellow Over/Under; 154 = Yellow Double Chance; 155 = Yellow Over/Under (1st Half); 156 = Yellow Over/Under (2nd Half); 157 = Yellow Odd/Even; 158 = Yellow Cards 1x2; 159 = Yellow Asian Handicap (1st Half); 160 = Yellow Asian Handicap (2nd Half); 161 = Yellow Cards 1x2 (1st Half); 162 = Yellow Cards 1x2 (2nd Half); 163 = Penalty Awarded; 164 = Offsides Total; 165 = Offsides 1x2; 166 = Offsides Handicap; 167 = Offsides Home Total; 168 = Offsides Away Total; 169 = Offsides Double Chance; 170 = Fouls. Away Total; 171 = Fouls. Home Total; 172 = Fouls. Double Chance; 173 = Fouls. Total; 174 = Fouls. Handicap; 175 = Fouls. 1x2; 176 = ShotOnTarget 1x2; 177 = ShotOnTarget Handicap; 178 = ShotOnTarget Double Chance; 179 = First Team to Score; 180 = Last Team to Score; 181 = European Handicap (2nd Half); 182 = Draw No Bet (2nd Half); 183 = Double Chance/Total; 184 = To Score in Both Halves; 185 = First Team to Score (3 way) 1st Half; 186 = Total Goals Number By Ranges; 187 = Total Goals By Ranges (1st Half); 188 = Clean Sheet; 189 = To Advance Handicap; 190 = Home Exact Goals Number (1st Half); 191 = Away Exact Goals Number (1st Half); 192 = Home Highest Scoring Half; 193 = Away Highest Scoring Half; 194 = Result/Total Goals (2nd Half); 195 = Either Team Wins By 1 Goals; 196 = Either Team Wins By 2 Goals; 197 = Over/Under 15m-30m; 198 = Over/Under 30m-45m; 199 = Home Win To Nill (1st Half); 200 = Home Win To Nill (2nd Half); 201 = To Score In 1st Half; 202 = To Score In 2nd Half; 203 = Yellow Cards. Odd/Even (1st Half); 204 = Yellow Cards. Odd/Even (2nd Half); 205 = First Team to Score (3 way) 2nd Half; 206 = Home No Bet; 207 = Away No Bet; 208 = Corners. First Corner (3 way); 209 = Home Come From Behind and Draw; 210 = Away Come From Behind and Draw; 211 = Total Shots; 212 = Player Assists; 213 = Player Triples; 214 = Player Points; 215 = Player Singles; 216 = Multi Touchdown Scorer (2 or More); 217 = Multi Touchdown Scorer (3 or More); 218 = Away Anytime Goal Scorer; 219 = Away First Goal Scorer; 220 = Shots. Away Total; 221 = Shots. Home Total; 222 = To Win From Behind; 223 = Number of Goals In Match (Range); 224 = Game Decided After Penalties; 225 = Game Decided in Extra Time; 226 = Away Last Goal Scorer; 227 = Goal Method Header; 228 = Home Goal Method Header; 229 = Goal Method Outside the Box; 230 = Home Goal Method Outside the Box; 231 = Home Anytime Goal Scorer; 232 = Home First Goal Scorer; 233 = Home Last Goal Scorer; 234 = Home To Score Three or More Goals; 235 = Away To Score Three or More Goals; 236 = Away To Score Two or More Goals; 237 = Home To Score Two or More Goals; 238 = Home Team Goalscorers First; 239 = Corners. European Handicap; 240 = Home Player Shots; 241 = Away Player Shots; 242 = Player Shots On Target; 243 = Home Shots On Target; 244 = Away Shots On Target; 245 = Away Goal Method Header; 246 = Away Goal Method Outside the Box; 247 = Corners Race To; 248 = Time Of 1st Score; 249 = Multicorners; 250 = First Card Received (3 way); 251 = Player to be booked; 252 = Both Teams to Receive a Card; 253 = Time Of 1st Score; 254 = Team Performances (Range); 255 = Home Player Assists; 256 = Away Player Assists; 257 = Player to Score or Assist; 258 = Home Player to Score or Assist; 259 = Away Player to Score or Assist; 260 = Team Time Of 1st Score; 261 = Total Goal Minutes (Range); 262 = Late Goal (Range); 263 = Early Goal (Range); 264 = Player Shots On Target Total; 265 = Player Shots Total; 266 = Player Fouls Committed; 267 = Goalkeeper Saves; 268 = Home Goalkeeper Saves; 269 = Home Player Shots On Target Total; 270 = Home Player Shots Total; 271 = Home Player Fouls Committed; 272 = Home Player Tackles; 273 = Home Player Passes; 274 = Away Goalkeeper Saves; 275 = Away Player Shots On Target Total; 276 = Away Player Shots Total; 277 = Away Player Fouls Committed; 278 = Away Player Tackles; 279 = Away Player Passes; 280 = First Set Piece 5 Minutes; 281 = Total Tackles; 282 = Double Chance/Both Teams To Score; 283 = Away Win To Nill (1st Half); 284 = Away Win To Nill (2nd Half); 285 = Team To Score (Goals); 286 = Team Goalscorers First; 287 = Team Goalscorers Last; 288 = Home Team Goalscorers Last; 289 = Away Team Goalscorers First; 290 = Away Team Goalscorers Last; 291 = Time of First Goal Brackets (Range); 292 = Over/Under between 0 and 10m; 293 = Double Chance 0-15m; 294 = Double Chance 15-30m; 295 = Corners. Total (Range); 296 = Double Chance 30-45m; 297 = Corners. total between 0 and 10m; 298 = Method of Victory; 299 = Cards over/under between 0 and 10 m; 300 = Both Teams to Receive 2+ Cards; 301 = Tackles. Away Total; 302 = Tackles. Home Total; 303 = Over/Under between 0 and 10 m; 304 = Over/Under between  0 and 10 m; 305 = Race to the 2nd goal?; 306 = Race to the 3rd goal?; 307 = Corners. Double Chance; 308 = Race To; 309 = Yellow Cards. Home Total (1st Half); 310 = Yellow Cards Away Total (1st Half); 311 = Which team will score the 1st goal?; 312 = 1x2 - 20 minutes; 313 = Offsides Odd/Even; 314 = Fouls Odd/Even; 315 = Saves Total; 316 = Saves 1x2; 317 = Saves Asian H; 318 = Saves O/U Home; 319 = Saves O/U Away; 320 = Saves Double Chance; 321 = Penalty Awarded (1st Half); 322 = Penalty Awarded (2nd Half); 323 = Saves Odd/Even; 324 = Corner in 1-15 minutes; 325 = Corner in 16-30 minutes; 326 = Corner in 31-45 minutes; 327 = Corner in 45-60 minutes; 328 = Corner in 60-75 minutes; 329 = Corner in 75-90 minutes; 330 = Home Not lose/Over; 331 = Home Not lose/Under; 332 = Away Not lose/Over; 333 = Away Not lose/Under; 334 = 1x2 - 70 minutes; 335 = Red Cards Over/Under; 336 = Goal Line; 337 = Asian Handicap (Sets); 338 = Corners. Odd/Even; 339 = Corners. Double Chance; 340 = Shots.1x2; 341 = null; 342 = Red Card In The Match (1st Half); 343 = Fouls. Odd/Even; 344 = Odd/Even (1st Set); 345 = Set Betting; 346 = Extra Point (1st Set); 347 = Home Winning Margin (1st Set); 348 = Away Winning Margin (1st Set); 349 = Number Of Goals In Match

### In-play bets (`/odds/live/bets`, 266; different id space)

1 = Over/Under Extra Time; 2 = 1x2 Extra Time; 3 = Extra Time Asian Corners; 4 = Extra Time Total Corners (3 Ways) (1st Half); 5 = Extra Time Double Result; 6 = Which team will score the 1st goal in extra time?; 7 = Extra Time Asian Corners (1st Half); 8 = Method of Victory; 9 = Both Teams To Score (ET); 10 = To Qualify; 11 = Asian Handicap Extra Time; 12 = 1x2 Extra Time (1st Half); 13 = Extra Time Total Corners (3 Ways); 14 = Over/Under Extra Time (1st Half); 15 = Last Corner; 16 = How many goals will Away Team score?; 17 = Asian Handicap (1st Half); 18 = 1st Goal in Interval; 19 = 1x2 (1st Half); 20 = Match Corners; 21 = 3-Way Handicap; 22 = 1x2 - 30 minutes; 23 = Final Score; 24 = Over/Under Line (1st Half); 25 = Match Goals; 26 = European Handicap (1st Half); 27 = Home Team Score a Goal (2nd Half); 28 = Home Team  to Score in Both Halves; 29 = Result / Both Teams To Score; 30 = Both Teams To Score (1st Half); 31 = Total Corners (3way) (2nd Half); 32 = Asian Corners; 33 = Asian Handicap; 34 = 1x2 - 40 minutes; 35 = To Win 2nd Half; 36 = Over/Under Line; 37 = Total Corners; 38 = Away Team to Score in Both Halves; 39 = Away Team Goals; 40 = Total Corners (3 way) (1st Half); 41 = 1x2 - 50 minutes; 42 = Race to the 3rd corner?; 43 = Both Teams To Score (2nd Half); 44 = Race to the 9th corner?; 45 = Race to the 7th corner?; 46 = Goal Scorer; 47 = Away 1st Goal in Interval; 48 = Draw No Bet; 49 = Over/Under (1st Half); 50 = 1x2 - 60 minutes; 51 = Asian Corners (1st Half); 52 = 1x2 - 80 minutes; 53 = To Score 2 or More; 54 = Home 1st Goal in Interval; 55 = Correct Score (1st Half); 56 = 1x2 - 70 minutes; 57 = Away Team Clean Sheet; 58 = Home Team Goals; 59 = Fulltime Result; 60 = To Score 3 or More; 61 = Race to the 5th corner?; 62 = Last Team to Score (3 way); 63 = Anytime Goal Scorer; 64 = Half Time/Full Time; 65 = Next 10 Minutes Total; 66 = Home Team Clean Sheet; 67 = How many goals will Home Team score?; 68 = Goals Odd/Even; 69 = Both Teams to Score; 70 = Away Team Score a Goal (2nd Half); 71 = Which team will score the 4th corner? (2 Way); 72 = Double Chance; 73 = Which team will score the 1st goal?; 74 = Which team will score the 3rd corner? (2 Way); 75 = Which team will score the 2nd corner? (2 Way); 76 = Corners European Handicap; 77 = 1x2 - 10 minutes; 78 = Corners 1x2; 79 = 1x2 - 20 minutes; 80 = Method of 1st Goal; 81 = Method of Qualification; 82 = Game Won After Penalties Shootout; 83 = Game Won In Extra Time; 84 = Which team will score the 2nd goal?; 85 = Which team will score the 2nd goal?; 86 = Which team will score the 6th corner? (2 Way); 87 = Which team will score the 5th corner? (2 Way); 88 = Which team will score the 7th corner? (2 Way); 89 = Which team will score the 9th corner? (2 Way); 90 = 2nd Goal in Interval; 91 = Away 2nd Goal in Interval; 92 = Which team will score the 3rd goal?; 93 = Which team will score the 10th corner? (2 Way); 94 = 3rd Goal in Interval; 95 = Home 2nd Goal in Interval; 96 = Asian Handicap Converted Penalties; 97 = Sudden Death; 98 = Away Penalty Shootout; 99 = Home Penalty Shootout; 100 = Home Total Converted Penalties; 101 = Total Penalties in Shootout; 102 = Last Penalty Score/Miss; 103 = Correct Score in Shootouts; 104 = Total Converted Penalties; 105 = Total Converted Penalties - Goal Line; 106 = Away Total Converted Penalties; 107 = Penalties Shootout Winner; 108 = Which team will score the 11th corner? (2 Way); 109 = Which team will score the 4th goal?; 110 = Which team will score the 8th corner? (2 Way); 111 = Last Penalty Scorer in Shootout; 112 = Which team will score the 5th goal?; 113 = Method of 2nd Goal; 114 = Which team will score the 13th corner? (2 Way); 115 = Player to be Booked; 116 = Action In Next 1 Minutes; 117 = First Action In Next 5 Minutes; 118 = Player to be Sent Off; 119 = Total Cards; 120 = Which team will score the 12th corner? (2 Way); 121 = Which team will score the 14th corner? (2 Way); 122 = Which team will score the 15th corner? (2 Way); 123 = Which team will score the 16th corner? (2 Way); 124 = Which team will score the 17th corner? (2 Way); 125 = Home 3rd Goal in Interval; 126 = Which team will score the 18th corner? (2 Way); 127 = Which team will score the 6th goal?; 128 = Away 3rd Goal in Interval; 129 = Which team will score the 2nd goal in extra time?; 130 = Method of 3rd Goal; 131 = 4th Goal in Interval; 132 = Which team will score the 7th goal?; 133 = Which team will score the 19th corner? (2 Way); 134 = Home 4th Goal in Interval; 135 = Away 4th Goal in Interval; 136 = 5th Goal in Interval; 137 = Home 5th Goal in Interval; 138 = Method of 4th Goal; 139 = Which team will score the 8th goal?; 140 = Which team will score the 9th goal?; 141 = Which team will score the 10th goal?; 142 = Which team will score the 11th goal?; 143 = Which team will score the 12th goal?; 144 = 6th Goal in Interval; 145 = Method of 5th Goal; 146 = Method of 6th Goal; 147 = Which team will score the 20th corner? (2 Way); 148 = Player Shots; 149 = Total Shots; 150 = Total shots on goal; 151 = Away Total Shots; 152 = Home Total Shots; 153 = Player Shots on Targets; 154 = Home Total shots on goal; 155 = Player Assists; 156 = Away Total shots on goal; 157 = Method of 7th Goal; 158 = Method of 8th Goal; 159 = 7th Goal in Interval; 160 = To Win the Trophy; 161 = Away 5th Goal in Interval; 162 = Which team will score the 3rd goal in extra time?; 163 = Which team will score the 13th goal?; 164 = Which team will score the 4th goal in extra time?; 165 = Double Chance Extra Time; 166 = Double Chance Extra Time (1st Half); 167 = Asian Handicap Extra Time (1st Half); 168 = Corners Double Chance; 169 = Home Total Corners; 170 = Which team will score the 10th corner?; 171 = Away Not lose/Over; 172 = Away Not lose/Under; 173 = Home Not lose/Over; 174 = Corners Asian Handicap; 175 = Home Not lose/Under; 176 = Away Total Corners; 177 = Over/Under (2nd Half); 178 = Total shots on goal (1st Half); 179 = Home team will win either half; 180 = Double Chance (1st Half); 181 = Which team will score the 13th corner?; 182 = Yellow Asian Handicap; 183 = Draw/Over; 184 = Asian Handicap (2nd Half); 185 = Double Chance shots on goal; 186 = Away Win/Over; 187 = Yellow Double Chance (1st Half); 188 = Away Total Corners (1st Half); 189 = Total Corners (1st Half); 190 = Home Total Corners (1st Half); 191 = Home Team Total (1st Half); 192 = Race to the 3rd goal?; 193 = Corners 1x2 (1st Half); 194 = 1x2 shots on goal; 195 = Home Team Total (2nd Half); 196 = Away Team Total Goals (2nd Half); 197 = Yellow Asian Handicap (1st Half); 198 = Away Team Total Points (1st Half); 199 = Yellow Double Chance; 200 = Home Team Yellow cards; 201 = Home Win/Over; 202 = 1x2 shots on goal (1st Half); 203 = Yellow Cards 1x2; 204 = Corners Asian Handicap (1st Half); 205 = Asian Handicap shots on goal; 206 = Home Team Yellow Cards (1st Half); 207 = Yellow Cards 1x2 (1st Half); 208 = Double Chance shots on goal (1st Half); 209 = Yellow Over/Under (1st Half); 210 = Yellow Over/Under; 211 = Away Team Yellow Cards (1st Half); 212 = Asian Handicap shots on goal (1st Half); 213 = Away Team Yellow Cards; 214 = Away team will win either half; 215 = Both Teams To Score Under; 216 = Both Halves Over; 217 = Race to the 2nd goal?; 218 = Away Team Score a Goal; 219 = Both Teams To Score Over; 220 = Both Halves Under; 221 = Home Win/Under; 222 = Away Team Score a Goal (1st Half); 223 = Which team will score the 5th corner?; 224 = Which team will score the 4th corner?; 225 = Home Team Score a Goal (1st Half); 226 = Home Team Score a Goal; 227 = Draw/Under; 228 = Away Win/Under; 229 = Home Red Over/Under; 230 = Away Red Over/Under; 231 = Which team will score the 7th corner?; 232 = Red Over/Under; 233 = Yellow Odd/Even; 234 = Which team will score the 6th corner?; 235 = Which team will score the 8th corner?; 236 = Which team will score the 9th corner?; 237 = Which team will score the 11th corner?; 238 = Which team will score the 12th corner?; 239 = Which team will score the 14th corner?; 240 = Which team will score the 15th corner?; 241 = Which team will score the 1st corner?; 242 = Which team will score the 2nd corner?; 243 = Away Total shots on goal (1st Half); 244 = Home Total shots on goal (1st Half); 245 = Away Penalty Over/Under; 246 = Home Penalty Over/Under; 247 = Penalty Over/Under; 248 = Which team will score the 5th goal in extra time?; 249 = Which team will score the 6th goal in extra time?; 250 = Extra Time Over/Under Line; 251 = Result/Total Goals; 252 = Total Goals/Both Teams To Score; 253 = Over/Under between 70:01 m and 80:00 m; 254 = Over/Under between 60:01 m and 70:00 m; 255 = Over/Under between 50:01 m and 60:00 m; 256 = Which team will score the 3rd corner?; 257 = Over/Under between 40:01 m and 50:00 m; 258 = Over/Under between 30:01 m and 40:00 m; 259 = Home Win Both Halves; 260 = Away Win Both Halves; 261 = Over/Under between 00:00 m and 10:00 m; 262 = Over/Under between 20:01 m and 30:00 m; 263 = Over/Under between 10:01 m and 20:00 m; 264 = Corners over/under between 00:00 m and 10:00 m; 265 = Corners over/under between 20:01 m and 30:00 m; 266 = Corners over/under between 10:01 m and 20:00 m


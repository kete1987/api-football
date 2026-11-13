# Pending documentation: odds helpers and search validation

Text to be merged by another agent into README.md, AGENTS.md and docs/API_CATALOG.md. Each section lists the exact paragraphs to add and where they belong. Source: branch `helpers-odds`.

## README.md

Add to the section that describes query validation / usage notes (or create a "Search terms" subsection):

> **Search terms.** Every `search` parameter (teams, venues, coaches, leagues, countries, players, player profiles, bookmakers, bets) is validated before the request, because an invalid search still costs one request of your daily quota. A term may contain only ASCII letters, digits and spaces and needs at least 3 characters (4 for `/players`); anything else throws `IllegalArgumentException`. To search text typed by a user, convert it first with the explicit helper `QueryValidation.toSearchTerm`, which strips accents, drops other symbols and collapses spaces:
>
> ```java
> api.teams().searchTeams(QueryValidation.toSearchTerm("Leganés")); // searches "Leganes"
> ```
>
> Passing `null` to a `search` setter removes the parameter.

Add to the odds part of the README:

> **Odds helpers.**
>
> ```java
> PrematchOdds odds = api.odds().getOddsByFixture(1570461);          // by fixture id, not by date
> BookmakerOdds bet365 = odds.findBookmaker(BookmakerIds.BET365);    // may be null
> PrematchBetOdds winner = bet365.findMatchWinner();
> Double home = winner.getHomeOdd();                                  // null when absent
> OddValue score = bet365.findExactScore().findValue("1:0");
> Integer homeGoals = score.getExactScoreHome();                      // order is home:away
> Instant updated = odds.getUpdateAsInstant();                        // one time per fixture
> ```
>
> Pre-match odds exist only 1 to 14 days before the match (with 7 days of history); outside that window `getOddsByFixture` returns `null`. Request odds by fixture id: the `date` that `/odds` reports can be provisional and filtering by the real date can return nothing. `update` is one timestamp per fixture, not per bookmaker. Some bookmakers of the catalogue (for example 888Sport and Interwetten) rarely or never appear in the odds of a fixture.

## AGENTS.md

Replace the sentence about validation in "Conventions for sub-APIs" (the **Validation** bullet) by appending:

> Every `search` setter validates its term with `QueryValidation.search` (and `QueryValidation.search(value, QueryValidation.PLAYERS_SEARCH_MIN_LENGTH)` for `/players`): only ASCII letters, digits and spaces, at least 3 characters (4 for `/players`; leading and trailing spaces do not count). Invalid terms throw `IllegalArgumentException` before any request, because the API answers errors with HTTP 200 and still counts them against the quota. `null` still removes the parameter. `QueryValidation.toSearchTerm` is an explicit opt-in cleaner (diacritics via `java.text.Normalizer`, other characters dropped, spaces collapsed); setters never apply it silently. Tests that need the real API error for a bad search (`error_search_too_short.json`) build the query with the raw `param("search", ...)` escape hatch.

Add to "Fields whose JSON type varies" / models:

> **Odds helpers** never throw: `PrematchBetOdds.findValue(label)` (case-insensitive, trimmed), `getHomeOdd()/getDrawOdd()/getAwayOdd()` (1X2), `BookmakerOdds.findMatchWinner()/findExactScore()`, `OddValue.getExactScoreHome()/getExactScoreAway()` (the "Exact Score" bet, id 10, is `home:away`, verified against the real Real Madrid - Celta odds; other providers give winner-loser) and `PrematchOdds.getUpdateAsInstant()` all return `null` when the data is missing or malformed. `odds.util.BookmakerIds` holds the well-known bookmaker ids; `BookmakerIds` constants are checked in `OddsHelpersTest` against the catalog's Appendix A and `odds_bookmakers.json`.

## docs/API_CATALOG.md

In section 11.36 (`GET /odds`), after the paragraph starting "Real: `values[].value` is a JSON number...", add:

> Verified 2026-11-12/13 (Real Madrid - Celta, fixture 1570461): the fixture `date` of `/odds` is provisional (`2026-11-22T16:00`, the real kick-off is `2026-11-21T20:00` in `/fixtures`), and `odds?league=140&season=2026&date=2026-11-21` returned no result for that fixture. Request odds by fixture id. `update` is one timestamp per fixture (here `2026-11-08T16:51:14+00:00`, 5 days old), not per bookmaker or selection. A fixture 15 days ahead returned 0 results (window: 1-14 days before the fixture). Of the five bookmakers a real app maps, Bet365 (8), William Hill (7) and Betfair (3) were present for that fixture; 888Sport (21) and Interwetten (15) are in `/odds/bookmakers` but did not appear in any fixture checked. The "Exact Score" bet (id 10) lists 40 selections for Bet365 as `home:away` (`1:0`, `2:1`, `0:1`...); for the home favourite (Match Winner home 1.18) `1:0` pays 10.00 and `0:1` pays 34.00, which fixes the order.

In the error table of section 2 (observed error messages), add the row:

> | `/teams?search=Leganés` | `{"search": "The Search field may only contain alpha-numeric characters and spaces."}` (HTTP 200, 0 results, consumes quota) |

In section 12 ("Docs vs reality"), replace the `search` length row by:

> | `search` | schema: exactly 3 for countries/bookmakers/bets | min 3 (4 for `/players`, docs); only ASCII letters, digits and spaces (accents and symbols are rejected with the message above); longer works per docs examples. The library validates this client-side (`QueryValidation.search`). |

In the `/players` section 11.24, next to the `search` parameter, note: "min 4; the client rejects shorter terms before the request."

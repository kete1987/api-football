package es.com.kete1987.apifootball.library;

import es.com.kete1987.apifootball.library.common.query.Query;
import es.com.kete1987.apifootball.library.common.query.QueryValidation;
import es.com.kete1987.apifootball.library.common.util.ApiFootballException;
import es.com.kete1987.apifootball.library.common.util.Constants;
import es.com.kete1987.apifootball.library.fixtures.model.Fixture;
import es.com.kete1987.apifootball.library.fixtures.model.FixtureEvent;
import es.com.kete1987.apifootball.library.fixtures.model.FixtureLineup;
import es.com.kete1987.apifootball.library.fixtures.model.FixturePlayers;
import es.com.kete1987.apifootball.library.fixtures.model.FixtureStatistics;
import es.com.kete1987.apifootball.library.fixtures.model.Injury;
import es.com.kete1987.apifootball.library.fixtures.model.Prediction;
import es.com.kete1987.apifootball.library.fixtures.model.RoundDates;
import es.com.kete1987.apifootball.library.fixtures.query.FixtureEventsQuery;
import es.com.kete1987.apifootball.library.fixtures.query.FixtureLineupsQuery;
import es.com.kete1987.apifootball.library.fixtures.query.FixturePlayersQuery;
import es.com.kete1987.apifootball.library.fixtures.query.FixtureStatisticsQuery;
import es.com.kete1987.apifootball.library.fixtures.query.FixturesQuery;
import es.com.kete1987.apifootball.library.fixtures.query.HeadToHeadQuery;
import es.com.kete1987.apifootball.library.fixtures.query.InjuriesQuery;
import es.com.kete1987.apifootball.library.fixtures.query.RoundsQuery;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Fixtures sub-API: fixtures, rounds, head to head, statistics, events, lineups, player statistics,
 * injuries and predictions.
 *
 * <p>Can be used standalone ({@code new FixturesApi("apiKey")}) or via the {@link ApiFootball}
 * facade. Every public method costs exactly one request. None of these endpoints is paginated: a
 * single {@code /fixtures} call can return more than 1,000 fixtures.
 */
public class FixturesApi extends ApiFootballBase {

    public FixturesApi(String apiKey) {
        this(buildHttpClient(apiKey), Constants.BASE_URL, new RateLimitTracker());
    }

    FixturesApi(OkHttpClient client, String baseUrl, RateLimitTracker tracker) {
        super(client, baseUrl, tracker);
    }

    /** Endpoint URL with a required parameter first, followed by the optional ones of {@code query}. */
    private HttpUrl.Builder urlWith(String path, String name, Object value, Query<?> query) {
        HttpUrl.Builder builder = url(path);
        param(builder, name, value);
        if (query != null) {
            query.toParams().forEach(builder::addQueryParameter);
        }
        return builder;
    }

    // ------------------------------------------------------------------ /fixtures

    /**
     * Fixtures matching the query ({@code GET /fixtures}). One request; not paginated.
     *
     * <p>Embedded details depend on the query: with {@code id}/{@code ids} each fixture carries
     * events, lineups, statistics and players; with {@code live} only events; otherwise none. Data
     * updates every 15 seconds; recommended polling is once a minute while a fixture is in progress,
     * once a day otherwise. A {@code null} query sends no parameters, which the API rejects.
     *
     * <p><b>Rejected before any request</b> (the API answers with an error, and an error costs a
     * request): a query with {@code from} and/or {@code to} but neither {@code league} nor
     * {@code team}, e.g. a plain date range ({@code "need another parameter"}, verified live; catalog
     * section 11.13 lists only {@code team+season+from+to} and {@code league+season+from+to+timezone}
     * as valid range combinations). Nothing else is rejected client-side: other invalid combinations
     * are left to the API.
     *
     * @throws IllegalArgumentException for a date range without {@code league} or {@code team}
     */
    public List<Fixture> getFixtures(FixturesQuery query) throws IOException, ApiFootballException {
        requireLeagueOrTeamForDateRange(query);
        return list(url("fixtures", query), Fixture.class);
    }

    /**
     * Local check (the setters stay order-independent): {@code from}/{@code to} need {@code league}
     * or {@code team}. Applied to the query as built, so the order of the setters does not matter.
     */
    private static void requireLeagueOrTeamForDateRange(FixturesQuery query) {
        if (query == null) {
            return;
        }
        Map<String, String> params = query.toParams();
        boolean range = params.containsKey("from") || params.containsKey("to");
        if (range && !params.containsKey("league") && !params.containsKey("team")) {
            throw new IllegalArgumentException(
                    "from/to need league or team: the API rejects a date range alone (and the error costs a request)");
        }
    }

    /**
     * One fixture with its events, lineups, statistics and players, or {@code null} when the id is
     * unknown. One request.
     */
    public Fixture getFixtureById(int id) throws IOException, ApiFootballException {
        return first(url("fixtures", new FixturesQuery().id(id)), Fixture.class);
    }

    /**
     * Up to 20 fixtures by id, each with events, lineups, statistics and players. One request.
     *
     * @throws IllegalArgumentException when {@code null}, empty, with more than 20 ids or containing {@code null}
     */
    public List<Fixture> getFixturesByIds(Collection<Integer> ids) throws IOException, ApiFootballException {
        return getFixtures(new FixturesQuery().ids(QueryValidation.ids("ids", ids)));
    }

    /**
     * Fixtures by id for <b>any number of ids</b>, split into batches of 20 (the API limit of
     * {@code ids}). <b>Costs {@code ceil(n / 20)} requests</b>: 20 ids = 1, 21 ids = 2, 100 ids = 5,
     * and each request counts against the daily quota. Use {@link #getFixturesByIds(Collection)} when
     * there are at most 20 ids (always one request). Each fixture carries events, lineups, statistics and
     * players, so every response is large.
     *
     * <p>Duplicate ids are sent once; the batches are requested one after the other and the result is
     * the concatenation of their responses (the order inside a batch is the API's, and ids the API does
     * not know are simply absent). Argument validation happens for every batch <em>before</em> the first
     * request. If a request fails, the exception is thrown and the fixtures of the earlier batches are
     * lost, although their requests were already spent.
     *
     * @throws IllegalArgumentException when {@code null}, empty or containing {@code null}
     */
    public List<Fixture> getFixturesByIdsInBatches(Collection<Integer> ids) throws IOException, ApiFootballException {
        if (ids == null) throw new IllegalArgumentException("ids must not be null");
        Set<Integer> unique = new LinkedHashSet<>();
        for (Integer id : ids) {
            if (id == null) throw new IllegalArgumentException("ids must not contain null");
            unique.add(id);
        }
        if (unique.isEmpty()) throw new IllegalArgumentException("ids must contain at least one id");
        List<Integer> all = new ArrayList<>(unique);
        List<String> batches = new ArrayList<>();
        for (int from = 0; from < all.size(); from += QueryValidation.MAX_IDS) {
            String joined = QueryValidation.ids("ids", all.subList(from, Math.min(all.size(), from + QueryValidation.MAX_IDS)));
            batches.add(QueryValidation.ids("ids", joined));
        }
        List<Fixture> result = new ArrayList<>();
        for (String batch : batches) {
            result.addAll(getFixtures(new FixturesQuery().ids(batch)));
        }
        return result;
    }

    /** Every fixture in progress ({@code live=all}), each with its events. One request. */
    public List<Fixture> getLiveFixtures() throws IOException, ApiFootballException {
        return getFixtures(new FixturesQuery().liveAll());
    }

    /**
     * Fixtures in progress in the given leagues ({@code live=id-id}), each with its events. One request.
     *
     * @throws IllegalArgumentException when {@code null}, empty or containing {@code null}
     */
    public List<Fixture> getLiveFixtures(Collection<Integer> leagues) throws IOException, ApiFootballException {
        if (leagues == null) throw new IllegalArgumentException("leagues must not be null");
        return getFixtures(new FixturesQuery().live(leagues));
    }

    /**
     * Fixtures of a day, in UTC. One request; can return more than a thousand fixtures.
     *
     * @throws IllegalArgumentException when the date is {@code null}
     */
    public List<Fixture> getFixturesByDate(LocalDate date) throws IOException, ApiFootballException {
        return getFixtures(new FixturesQuery().date(QueryValidation.date("date", date)));
    }

    /**
     * Fixtures of a {@code YYYY-MM-DD} day, in UTC. One request.
     *
     * @throws IllegalArgumentException when the date is {@code null} or malformed (the API would
     *                                  silently overflow it into another day)
     */
    public List<Fixture> getFixturesByDate(String date) throws IOException, ApiFootballException {
        return getFixtures(new FixturesQuery().date(QueryValidation.date("date", date)));
    }

    /**
     * Fixtures of a day, with dates rendered in {@code timezone} and the day interpreted in it.
     * One request.
     *
     * @param timezone a name returned by {@code /timezone}, see {@link QueryValidation#timezone(String)}
     * @throws IllegalArgumentException for a null date or a null or invalid timezone
     */
    public List<Fixture> getFixturesByDate(LocalDate date, String timezone) throws IOException, ApiFootballException {
        return getFixtures(new FixturesQuery().date(QueryValidation.date("date", date))
                .timezone(QueryValidation.timezone(timezone)));
    }

    /**
     * Fixtures of a {@code YYYY-MM-DD} day, with dates rendered in {@code timezone} and the day
     * interpreted in it. One request.
     *
     * @param timezone a name returned by {@code /timezone}, see {@link QueryValidation#timezone(String)}
     * @throws IllegalArgumentException for a null or malformed date or a null or invalid timezone
     */
    public List<Fixture> getFixturesByDate(String date, String timezone) throws IOException, ApiFootballException {
        return getFixtures(new FixturesQuery().date(QueryValidation.date("date", date))
                .timezone(QueryValidation.timezone(timezone)));
    }

    /** All fixtures of a league season. One request; a full season is several hundred fixtures. */
    public List<Fixture> getFixturesByLeague(int league, int season) throws IOException, ApiFootballException {
        return getFixtures(new FixturesQuery().league(league).season(season));
    }

    /**
     * Fixtures of a league season between two dates, inclusive ({@code league+season+from+to}, a
     * combination the catalog lists). One request.
     *
     * @throws IllegalArgumentException when a date is {@code null}
     */
    public List<Fixture> getFixturesByLeague(int league, int season, LocalDate from, LocalDate to)
            throws IOException, ApiFootballException {
        return getFixtures(new FixturesQuery().league(league).season(season)
                .from(QueryValidation.date("from", from)).to(QueryValidation.date("to", to)));
    }

    /**
     * Same as {@link #getFixturesByLeague(int, int, LocalDate, LocalDate)} with {@code YYYY-MM-DD}
     * strings. One request.
     *
     * @throws IllegalArgumentException when a date is {@code null}, malformed or not a real calendar date
     */
    public List<Fixture> getFixturesByLeague(int league, int season, String from, String to)
            throws IOException, ApiFootballException {
        return getFixtures(new FixturesQuery().league(league).season(season)
                .from(QueryValidation.date("from", from)).to(QueryValidation.date("to", to)));
    }

    /**
     * Fixtures of a league season between two dates with dates rendered in {@code timezone}
     * ({@code league+season+from+to+timezone}). One request.
     *
     * @param timezone a name returned by {@code /timezone}, see {@link QueryValidation#timezone(String)}
     * @throws IllegalArgumentException for a {@code null} date or a {@code null} or invalid timezone
     */
    public List<Fixture> getFixturesByLeague(int league, int season, LocalDate from, LocalDate to, String timezone)
            throws IOException, ApiFootballException {
        return getFixtures(new FixturesQuery().league(league).season(season)
                .from(QueryValidation.date("from", from)).to(QueryValidation.date("to", to))
                .timezone(QueryValidation.timezone(timezone)));
    }

    /**
     * Same as {@link #getFixturesByLeague(int, int, LocalDate, LocalDate, String)} with
     * {@code YYYY-MM-DD} strings. One request.
     *
     * @throws IllegalArgumentException for a {@code null} or malformed date or a {@code null} or invalid timezone
     */
    public List<Fixture> getFixturesByLeague(int league, int season, String from, String to, String timezone)
            throws IOException, ApiFootballException {
        return getFixtures(new FixturesQuery().league(league).season(season)
                .from(QueryValidation.date("from", from)).to(QueryValidation.date("to", to))
                .timezone(QueryValidation.timezone(timezone)));
    }

    /**
     * All fixtures of a team in a season ({@code team+season}). One request; a team plays up to a few
     * dozen matches a season. This exact combination is not in the catalog's list of tested
     * combinations (section 11.13) but is the documented way to list a team's season; it has not been
     * exercised live by this library.
     */
    public List<Fixture> getFixturesByTeam(int team, int season) throws IOException, ApiFootballException {
        return getFixtures(new FixturesQuery().team(team).season(season));
    }

    /**
     * Fixtures of a team in a season between two dates, inclusive ({@code team+season+from+to}, a
     * combination the catalog lists). One request.
     *
     * @throws IllegalArgumentException when a date is {@code null}
     */
    public List<Fixture> getFixturesByTeam(int team, int season, LocalDate from, LocalDate to)
            throws IOException, ApiFootballException {
        return getFixtures(new FixturesQuery().team(team).season(season)
                .from(QueryValidation.date("from", from)).to(QueryValidation.date("to", to)));
    }

    /**
     * Same as {@link #getFixturesByTeam(int, int, LocalDate, LocalDate)} with {@code YYYY-MM-DD}
     * strings. One request.
     *
     * @throws IllegalArgumentException when a date is {@code null}, malformed or not a real calendar date
     */
    public List<Fixture> getFixturesByTeam(int team, int season, String from, String to)
            throws IOException, ApiFootballException {
        return getFixtures(new FixturesQuery().team(team).season(season)
                .from(QueryValidation.date("from", from)).to(QueryValidation.date("to", to)));
    }

    /**
     * Fixtures of one round of a league season. One request.
     *
     * @param round a label from {@link #getRounds(int, int)}, e.g. {@code "Regular Season - 13"}
     * @throws IllegalArgumentException when {@code round} is {@code null} or blank
     */
    public List<Fixture> getFixturesByRound(int league, int season, String round) throws IOException, ApiFootballException {
        if (round == null) throw new IllegalArgumentException("round must not be null");
        return getFixtures(new FixturesQuery().league(league).season(season).round(round));
    }

    /** The next {@code count} (1-99) fixtures of a team. One request. */
    public List<Fixture> getNextFixtures(int team, int count) throws IOException, ApiFootballException {
        return getFixtures(new FixturesQuery().team(team).next(count));
    }

    /** The last {@code count} (1-99) fixtures of a team. One request. */
    public List<Fixture> getLastFixtures(int team, int count) throws IOException, ApiFootballException {
        return getFixtures(new FixturesQuery().team(team).last(count));
    }

    /**
     * The next {@code count} (1-99) fixtures of a team with dates rendered in {@code timezone}. One request.
     *
     * @throws IllegalArgumentException for a {@code null} or invalid timezone or a count outside 1-99
     */
    public List<Fixture> getNextFixtures(int team, int count, String timezone) throws IOException, ApiFootballException {
        return getFixtures(new FixturesQuery().team(team).next(count).timezone(QueryValidation.timezone(timezone)));
    }

    /**
     * The last {@code count} (1-99) fixtures of a team with dates rendered in {@code timezone}
     * ({@code team+last+timezone}, a combination the catalog lists). One request.
     *
     * @throws IllegalArgumentException for a {@code null} or invalid timezone or a count outside 1-99
     */
    public List<Fixture> getLastFixtures(int team, int count, String timezone) throws IOException, ApiFootballException {
        return getFixtures(new FixturesQuery().team(team).last(count).timezone(QueryValidation.timezone(timezone)));
    }

    /**
     * The next {@code count} (1-99) fixtures of a league with dates rendered in {@code timezone}. One request.
     *
     * @throws IllegalArgumentException for a {@code null} or invalid timezone or a count outside 1-99
     */
    public List<Fixture> getNextFixturesByLeague(int league, int count, String timezone)
            throws IOException, ApiFootballException {
        return getFixtures(new FixturesQuery().league(league).next(count).timezone(QueryValidation.timezone(timezone)));
    }

    /**
     * The last {@code count} (1-99) fixtures of a league with dates rendered in {@code timezone}. One request.
     *
     * @throws IllegalArgumentException for a {@code null} or invalid timezone or a count outside 1-99
     */
    public List<Fixture> getLastFixturesByLeague(int league, int count, String timezone)
            throws IOException, ApiFootballException {
        return getFixtures(new FixturesQuery().league(league).last(count).timezone(QueryValidation.timezone(timezone)));
    }

    /** The next {@code count} (1-99) fixtures of a league. One request. */
    public List<Fixture> getNextFixturesByLeague(int league, int count) throws IOException, ApiFootballException {
        return getFixtures(new FixturesQuery().league(league).next(count));
    }

    /** The last {@code count} (1-99) fixtures of a league. One request. */
    public List<Fixture> getLastFixturesByLeague(int league, int count) throws IOException, ApiFootballException {
        return getFixtures(new FixturesQuery().league(league).last(count));
    }

    // ------------------------------------------------------------------ /fixtures/rounds

    /**
     * Round labels of a league season ({@code GET /fixtures/rounds}), plain strings such as
     * {@code "Regular Season - 1"} or {@code "Round of 16"}; usable as the {@code round} filter. One
     * request. Updated daily; one call per day is enough.
     */
    public List<String> getRounds(int league, int season) throws IOException, ApiFootballException {
        return getRounds(league, season, null);
    }

    /**
     * Round labels with optional {@code current} / {@code timezone} filters. One request. Does not
     * send {@code dates}; use {@link #getRoundsWithDates(int, int, RoundsQuery)} for the dates.
     */
    public List<String> getRounds(int league, int season, RoundsQuery query) throws IOException, ApiFootballException {
        return list(roundsUrl(league, season, query), String.class);
    }

    /**
     * The current round label of a league season ({@code current=true}), or {@code null} when the API
     * returns none. One request.
     *
     * <p>Observed live: it is {@code null} when the <b>season is over</b> (2025 Champions League
     * returned {@code []}), and during a <b>break between rounds it returns the NEXT round</b>, not the
     * last one played (La Liga 2026 answered {@code "Regular Season - 13"} while round 12 was finished
     * and 13 had not started). So {@code null} means "no current round", not an error, and the label
     * may be a round that has not begun.
     */
    public String getCurrentRound(int league, int season) throws IOException, ApiFootballException {
        List<String> rounds = getRounds(league, season, new RoundsQuery().current(true));
        return rounds.isEmpty() ? null : rounds.get(0);
    }

    /** Rounds with the dates of their matches ({@code dates=true}). One request. */
    public List<RoundDates> getRoundsWithDates(int league, int season) throws IOException, ApiFootballException {
        return getRoundsWithDates(league, season, null);
    }

    /**
     * Rounds with the dates of their matches ({@code dates=true}) and optional {@code current} /
     * {@code timezone} filters (the timezone shifts the dates). One request.
     */
    public List<RoundDates> getRoundsWithDates(int league, int season, RoundsQuery query)
            throws IOException, ApiFootballException {
        return list(roundsUrl(league, season, query).addQueryParameter("dates", "true"), RoundDates.class);
    }

    private HttpUrl.Builder roundsUrl(int league, int season, RoundsQuery query) {
        HttpUrl.Builder builder = url("fixtures/rounds");
        param(builder, "league", league);
        param(builder, "season", season);
        if (query != null) {
            query.toParams().forEach(builder::addQueryParameter);
        }
        return builder;
    }

    // ------------------------------------------------------------------ /fixtures/headtohead

    /**
     * Previous (and upcoming) fixtures between two teams ({@code GET /fixtures/headtohead},
     * {@code h2h=teamA-teamB}). One request. Same element as {@code /fixtures} without embedded
     * details. Updated every 15 seconds.
     *
     * <p>Observed live: <b>without {@code last} the result includes upcoming fixtures</b> ({@code NS}
     * and the like); <b>{@code last=N} returns only fixtures already played</b> (it excludes the
     * unplayed ones, so {@code last(5)} gives the five most recent played meetings, not the five most
     * recent dates); and the history is incomplete for older cup ties (old Copa del Rey matches can be
     * missing), with data going back to about 2012. Do not rely on the list being exhaustive.
     */
    public List<Fixture> getHeadToHead(int teamA, int teamB) throws IOException, ApiFootballException {
        return getHeadToHead(teamA, teamB, null);
    }

    /**
     * Head to head with optional filters ({@code date, league, season, last, next, from, to, status,
     * venue, timezone}). One request. {@code last} excludes unplayed fixtures, older cup ties may be
     * missing: see {@link #getHeadToHead(int, int)}.
     */
    public List<Fixture> getHeadToHead(int teamA, int teamB, HeadToHeadQuery query)
            throws IOException, ApiFootballException {
        return list(urlWith("fixtures/headtohead", "h2h", teamA + "-" + teamB, query), Fixture.class);
    }

    // ------------------------------------------------------------------ /fixtures/statistics

    /** Team statistics of a fixture, one element per team ({@code GET /fixtures/statistics}). One request. */
    public List<FixtureStatistics> getFixtureStatistics(int fixture) throws IOException, ApiFootballException {
        return getFixtureStatistics(fixture, null);
    }

    /**
     * Team statistics of a fixture with optional {@code team}, {@code type} and {@code half} filters.
     * One request. Updated every minute.
     */
    public List<FixtureStatistics> getFixtureStatistics(int fixture, FixtureStatisticsQuery query)
            throws IOException, ApiFootballException {
        return list(urlWith("fixtures/statistics", "fixture", fixture, query), FixtureStatistics.class);
    }

    /**
     * Team statistics of a fixture including first- and second-half splits ({@code half=true}; data
     * from the 2024 season). One request.
     */
    public List<FixtureStatistics> getFixtureStatisticsByHalf(int fixture) throws IOException, ApiFootballException {
        return getFixtureStatistics(fixture, new FixtureStatisticsQuery().half(true));
    }

    // ------------------------------------------------------------------ /fixtures/events

    /** Events (goals, cards, substitutions, VAR) of a fixture ({@code GET /fixtures/events}). One request. */
    public List<FixtureEvent> getFixtureEvents(int fixture) throws IOException, ApiFootballException {
        return getFixtureEvents(fixture, null);
    }

    /** Events of a fixture with optional {@code team}, {@code player} and {@code type} filters. One request. */
    public List<FixtureEvent> getFixtureEvents(int fixture, FixtureEventsQuery query)
            throws IOException, ApiFootballException {
        return list(urlWith("fixtures/events", "fixture", fixture, query), FixtureEvent.class);
    }

    // ------------------------------------------------------------------ /fixtures/lineups

    /**
     * Lineups of a fixture, one element per team ({@code GET /fixtures/lineups}). One request.
     * Available 20-40 minutes before kick-off when covered; the result is empty before that.
     */
    public List<FixtureLineup> getFixtureLineups(int fixture) throws IOException, ApiFootballException {
        return getFixtureLineups(fixture, null);
    }

    /** Lineups of a fixture with optional {@code team}, {@code player} and {@code type} filters. One request. */
    public List<FixtureLineup> getFixtureLineups(int fixture, FixtureLineupsQuery query)
            throws IOException, ApiFootballException {
        return list(urlWith("fixtures/lineups", "fixture", fixture, query), FixtureLineup.class);
    }

    // ------------------------------------------------------------------ /fixtures/players

    /** Per-player statistics of a fixture, one element per team ({@code GET /fixtures/players}). One request. */
    public List<FixturePlayers> getFixturePlayers(int fixture) throws IOException, ApiFootballException {
        return getFixturePlayers(fixture, null);
    }

    /** Per-player statistics of a fixture, optionally restricted to a team. One request. */
    public List<FixturePlayers> getFixturePlayers(int fixture, FixturePlayersQuery query)
            throws IOException, ApiFootballException {
        return list(urlWith("fixtures/players", "fixture", fixture, query), FixturePlayers.class);
    }

    // ------------------------------------------------------------------ /injuries

    /**
     * Players missing fixtures ({@code GET /injuries}, data from April 2021). One request. <b>At least
     * one parameter is required</b>; {@code league}, {@code team} and {@code player} need
     * {@code season}. Updated every 4 hours; one call per day is enough. A league season can return
     * about a thousand rows.
     */
    public List<Injury> getInjuries(InjuriesQuery query) throws IOException, ApiFootballException {
        return list(url("injuries", query), Injury.class);
    }

    /** Injuries of one fixture. One request. */
    public List<Injury> getInjuriesByFixture(int fixture) throws IOException, ApiFootballException {
        return getInjuries(new InjuriesQuery().fixture(fixture));
    }

    /**
     * Injuries of up to 20 fixtures ({@code ids}). One request.
     *
     * @throws IllegalArgumentException when {@code null}, empty, with more than 20 ids or containing {@code null}
     */
    public List<Injury> getInjuriesByFixtures(Collection<Integer> fixtures) throws IOException, ApiFootballException {
        return getInjuries(new InjuriesQuery().ids(QueryValidation.ids("ids", fixtures)));
    }

    /** Injuries of a league season. One request; large response. */
    public List<Injury> getInjuriesByLeague(int league, int season) throws IOException, ApiFootballException {
        return getInjuries(new InjuriesQuery().league(league).season(season));
    }

    /** Injuries of a team in a season. One request. */
    public List<Injury> getInjuriesByTeam(int team, int season) throws IOException, ApiFootballException {
        return getInjuries(new InjuriesQuery().team(team).season(season));
    }

    /** Injuries of a player in a season. One request. */
    public List<Injury> getInjuriesByPlayer(int player, int season) throws IOException, ApiFootballException {
        return getInjuries(new InjuriesQuery().player(player).season(season));
    }

    /**
     * Injuries of the fixtures of a day. One request.
     *
     * @throws IllegalArgumentException when the date is {@code null}
     */
    public List<Injury> getInjuriesByDate(LocalDate date) throws IOException, ApiFootballException {
        return getInjuries(new InjuriesQuery().date(QueryValidation.date("date", date)));
    }

    /**
     * Injuries of the fixtures of a {@code YYYY-MM-DD} day. One request.
     *
     * @throws IllegalArgumentException when the date is {@code null}, malformed or not a real calendar date
     */
    public List<Injury> getInjuriesByDate(String date) throws IOException, ApiFootballException {
        return getInjuries(new InjuriesQuery().date(QueryValidation.date("date", date)));
    }

    // ------------------------------------------------------------------ /predictions

    /**
     * Prediction for a fixture ({@code GET /predictions}): winner, goals, advice, percentages, team
     * form, comparison and previous meetings; bookmaker odds are not used. One request; {@code null}
     * when the API has no prediction. Updated hourly.
     */
    public Prediction getPrediction(int fixture) throws IOException, ApiFootballException {
        return first(param(url("predictions"), "fixture", fixture), Prediction.class);
    }
}

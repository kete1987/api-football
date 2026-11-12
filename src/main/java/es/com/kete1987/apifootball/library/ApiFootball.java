package es.com.kete1987.apifootball.library;

import es.com.kete1987.apifootball.library.account.model.Status;
import es.com.kete1987.apifootball.library.common.model.RateLimit;
import es.com.kete1987.apifootball.library.common.util.ApiFootballException;
import es.com.kete1987.apifootball.library.common.util.Constants;
import okhttp3.OkHttpClient;

import java.io.IOException;

/**
 * Main entry point for the API-Football v3 API.
 *
 * <p>Create one instance per API key and reuse it: the instance owns one OkHttp connection pool
 * and one rate-limit tracker shared by all its sub-APIs, so the rate-limit getters below always
 * reflect the most recent response whatever sub-API made the request.
 *
 * <pre>{@code
 * ApiFootball api = new ApiFootball("YOUR_API_KEY");
 * List<Fixture> live = api.fixtures().getLiveFixtures();
 * Standings table = api.leagues().getStandings(140, 2025);
 * Integer left = api.getDailyRequestsRemaining(); // null before the first response
 * }</pre>
 *
 * <p>Every API method throws {@code IOException} (network failure), {@link ApiFootballException}
 * (the API rejected the request, or its response could not be used) and
 * {@link IllegalArgumentException} (an invalid argument, detected before any request is sent).
 *
 * <p>The sub-APIs can also be created on their own ({@code new FixturesApi("YOUR_API_KEY")}) when
 * only one domain is needed.
 */
public class ApiFootball {

    private final RateLimitTracker tracker;
    private final AccountApi account;
    private final LeaguesApi leagues;
    private final TeamsApi teams;
    private final FixturesApi fixtures;
    private final PlayersApi players;
    private final OddsApi odds;

    /**
     * Creates a client for a direct api-sports.io subscription.
     *
     * @param apiKey your API-Football key (sent in the {@code x-apisports-key} header)
     * @throws IllegalArgumentException when the key is {@code null} or blank
     */
    public ApiFootball(String apiKey) {
        this(ApiFootballBase.buildHttpClient(apiKey), Constants.BASE_URL);
    }

    ApiFootball(OkHttpClient client, String baseUrl) {
        this.tracker = new RateLimitTracker();
        this.account = new AccountApi(client, baseUrl, tracker);
        this.leagues = new LeaguesApi(client, baseUrl, tracker);
        this.teams = new TeamsApi(client, baseUrl, tracker);
        this.fixtures = new FixturesApi(client, baseUrl, tracker);
        this.players = new PlayersApi(client, baseUrl, tracker);
        this.odds = new OddsApi(client, baseUrl, tracker);
    }

    // ------------------------------------------------------------------ sub-APIs

    /** Subscription status and daily usage. */
    public AccountApi account() {
        return account;
    }

    /** Timezones, countries, leagues, seasons and standings. */
    public LeaguesApi leagues() {
        return leagues;
    }

    /** Teams, team statistics, venues and coaches. */
    public TeamsApi teams() {
        return teams;
    }

    /** Fixtures, rounds, head to head, events, lineups, statistics, injuries and predictions. */
    public FixturesApi fixtures() {
        return fixtures;
    }

    /** Players, squads, top lists, transfers, trophies and sidelined. */
    public PlayersApi players() {
        return players;
    }

    /** Pre-match and in-play odds, bookmakers, bets and odds mapping. */
    public OddsApi odds() {
        return odds;
    }

    /**
     * Shortcut for {@code account().getStatus()}: account, subscription and requests consumed
     * today. Does not count against the daily quota (it does count against the per-minute limit).
     */
    public Status getStatus() throws IOException, ApiFootballException {
        return account.getStatus();
    }

    // ------------------------------------------------------------------ rate limits

    /**
     * Rate-limit headers of the most recent response made through any sub-API of this instance,
     * or {@code null} before the first request.
     */
    public RateLimit getRateLimit() {
        return tracker.last();
    }

    /** {@code x-ratelimit-requests-limit} of the last response: requests allocated per day. */
    public Integer getDailyRequestsLimit() {
        return account.getDailyRequestsLimit();
    }

    /**
     * {@code x-ratelimit-requests-remaining} of the last response: requests left today. The API
     * refreshes this value with some delay, so treat it as approximate.
     */
    public Integer getDailyRequestsRemaining() {
        return account.getDailyRequestsRemaining();
    }

    /** {@code X-RateLimit-Limit} of the last response: maximum calls per minute. */
    public Integer getMinuteRequestsLimit() {
        return account.getMinuteRequestsLimit();
    }

    /** {@code X-RateLimit-Remaining} of the last response: calls left in the current minute. */
    public Integer getMinuteRequestsRemaining() {
        return account.getMinuteRequestsRemaining();
    }

    /**
     * Number of HTTP responses received through this instance (all sub-APIs) since it was created,
     * error responses included. Requests that never got a response (timeouts, connection failures)
     * and retries OkHttp performs transparently are not counted.
     */
    public long getRequestCount() {
        return tracker.requestCount();
    }
}

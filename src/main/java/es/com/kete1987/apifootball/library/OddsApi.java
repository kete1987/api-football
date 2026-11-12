package es.com.kete1987.apifootball.library;

import es.com.kete1987.apifootball.library.common.model.Page;
import es.com.kete1987.apifootball.library.common.util.ApiFootballException;
import es.com.kete1987.apifootball.library.common.util.Constants;
import es.com.kete1987.apifootball.library.odds.model.Bookmaker;
import es.com.kete1987.apifootball.library.odds.model.LiveBet;
import es.com.kete1987.apifootball.library.odds.model.LiveOdds;
import es.com.kete1987.apifootball.library.odds.model.OddsMapping;
import es.com.kete1987.apifootball.library.odds.model.PrematchBet;
import es.com.kete1987.apifootball.library.odds.model.PrematchOdds;
import es.com.kete1987.apifootball.library.odds.query.BookmakersQuery;
import es.com.kete1987.apifootball.library.odds.query.LiveBetsQuery;
import es.com.kete1987.apifootball.library.odds.query.LiveOddsQuery;
import es.com.kete1987.apifootball.library.odds.query.OddsQuery;
import es.com.kete1987.apifootball.library.odds.query.PrematchBetsQuery;
import okhttp3.OkHttpClient;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * Odds sub-API: pre-match odds, in-play odds and their catalogues (bookmakers and bets).
 *
 * <p><b>Two separate bet id spaces.</b> Pre-match bet ids ({@code /odds/bets}, used by
 * {@code /odds}) and in-play bet ids ({@code /odds/live/bets}, used by {@code /odds/live}) are NOT
 * compatible: pre-match 1 is "Match Winner", in-play 1 is "Over/Under Extra Time". Methods and
 * models are named {@code Prematch...} / {@code Live...} accordingly.
 *
 * <p><b>Pagination and cost.</b> {@code /odds} (10 fixtures per page, up to ~1.6 MB per page for a
 * league) and {@code /odds/mapping} (100 per page) are paginated; every page is one request
 * against the daily quota. Single-page methods cost one request; the {@code getAll...} methods
 * cost one request per page walked and always take a {@code maxPages} cap (non-positive = no cap).
 * For example {@code /odds?date=...} may span 24 pages.
 */
public class OddsApi extends ApiFootballBase {

    public OddsApi(String apiKey) {
        this(buildHttpClient(apiKey), Constants.BASE_URL, new RateLimitTracker());
    }

    OddsApi(OkHttpClient client, String baseUrl, RateLimitTracker tracker) {
        super(client, baseUrl, tracker);
    }

    // ------------------------------------------------------------------ pre-match odds

    /**
     * Pre-match odds, one page (10 fixtures). Costs exactly 1 request. The API requires at least
     * one filter in {@code query}. Odds exist 1-14 days before kick-off with 7 days of history and
     * are refreshed every 3 hours.
     *
     * @param page 1-based page number; values below 1 are sent as 1
     */
    public Page<PrematchOdds> getOddsPage(OddsQuery query, int page) throws IOException, ApiFootballException {
        return page(url("odds", query), PrematchOdds.class, page);
    }

    /**
     * Pre-match odds, all pages. <b>Costs one request per page walked</b> (up to {@code maxPages}),
     * and a league page can weigh ~1.6 MB.
     *
     * @param maxPages maximum number of pages to fetch; a non-positive value means every page
     */
    public List<PrematchOdds> getAllOdds(OddsQuery query, int maxPages) throws IOException, ApiFootballException {
        return allPages(url("odds", query), PrematchOdds.class, maxPages);
    }

    /** Pre-match odds of one fixture (all bookmakers and bets). 1 request. {@code null} when no odds exist. */
    public PrematchOdds getOddsByFixture(int fixture) throws IOException, ApiFootballException {
        return firstOf(getOddsPage(new OddsQuery().fixture(fixture), 1));
    }

    /**
     * Pre-match odds of one fixture restricted to one bookmaker and one <b>pre-match</b> bet.
     * 1 request. {@code null} when no odds exist.
     */
    public PrematchOdds getOddsByFixture(int fixture, int bookmaker, int bet) throws IOException, ApiFootballException {
        return firstOf(getOddsPage(new OddsQuery().fixture(fixture).bookmaker(bookmaker).bet(bet), 1));
    }

    /** One page of pre-match odds of a league season. 1 request (~1.6 MB). */
    public Page<PrematchOdds> getOddsPageByLeagueSeason(int league, int season, int page)
            throws IOException, ApiFootballException {
        return getOddsPage(new OddsQuery().league(league).season(season), page);
    }

    /** All pages of pre-match odds of a league season. 1 request per page walked, capped by {@code maxPages}. */
    public List<PrematchOdds> getAllOddsByLeagueSeason(int league, int season, int maxPages)
            throws IOException, ApiFootballException {
        return getAllOdds(new OddsQuery().league(league).season(season), maxPages);
    }

    /** One page of pre-match odds of the fixtures of a date. 1 request. */
    public Page<PrematchOdds> getOddsPageByDate(LocalDate date, int page) throws IOException, ApiFootballException {
        return getOddsPage(new OddsQuery().date(date), page);
    }

    /** All pages of pre-match odds of a date (can be 20+ pages). 1 request per page walked, capped by {@code maxPages}. */
    public List<PrematchOdds> getAllOddsByDate(LocalDate date, int maxPages) throws IOException, ApiFootballException {
        return getAllOdds(new OddsQuery().date(date), maxPages);
    }

    // ------------------------------------------------------------------ odds mapping

    /**
     * Fixtures that have pre-match odds, one page (100 entries). 1 request. Updated daily;
     * about 1 call per day is recommended.
     */
    public Page<OddsMapping> getOddsMappingPage(int page) throws IOException, ApiFootballException {
        return page(url("odds/mapping"), OddsMapping.class, page);
    }

    /** All pages of the odds mapping (~31 pages observed). 1 request per page walked, capped by {@code maxPages}. */
    public List<OddsMapping> getAllOddsMapping(int maxPages) throws IOException, ApiFootballException {
        return allPages(url("odds/mapping"), OddsMapping.class, maxPages);
    }

    // ------------------------------------------------------------------ bookmakers

    /** All bookmakers. 1 request. Names can be {@code null}. Updated several times a week. */
    public List<Bookmaker> getBookmakers() throws IOException, ApiFootballException {
        return getBookmakers(null);
    }

    /** Bookmakers filtered by {@code query} ({@code null} = no filter). 1 request. */
    public List<Bookmaker> getBookmakers(BookmakersQuery query) throws IOException, ApiFootballException {
        return list(url("odds/bookmakers", query), Bookmaker.class);
    }

    /** The bookmaker with the given id, or {@code null}. 1 request. */
    public Bookmaker getBookmakerById(int id) throws IOException, ApiFootballException {
        return first(url("odds/bookmakers", new BookmakersQuery().id(id)), Bookmaker.class);
    }

    /** Bookmakers whose name matches {@code search} (min 3 characters). 1 request. */
    public List<Bookmaker> searchBookmakers(String search) throws IOException, ApiFootballException {
        return getBookmakers(new BookmakersQuery().search(search));
    }

    // ------------------------------------------------------------------ pre-match bets

    /** All pre-match bet types (~338). 1 request. Ids are NOT valid for in-play odds. */
    public List<PrematchBet> getPrematchBets() throws IOException, ApiFootballException {
        return getPrematchBets(null);
    }

    /** Pre-match bet types filtered by {@code query} ({@code null} = no filter). 1 request. */
    public List<PrematchBet> getPrematchBets(PrematchBetsQuery query) throws IOException, ApiFootballException {
        return list(url("odds/bets", query), PrematchBet.class);
    }

    /** The pre-match bet with the given id, or {@code null}. 1 request. */
    public PrematchBet getPrematchBetById(int id) throws IOException, ApiFootballException {
        return first(url("odds/bets", new PrematchBetsQuery().id(id)), PrematchBet.class);
    }

    /** Pre-match bets whose name matches {@code search} (min 3 characters). 1 request. */
    public List<PrematchBet> searchPrematchBets(String search) throws IOException, ApiFootballException {
        return getPrematchBets(new PrematchBetsQuery().search(search));
    }

    // ------------------------------------------------------------------ live odds

    /**
     * In-play odds. 1 request. With an empty query returns every fixture currently in play that
     * has odds. Fixtures appear 15-5 minutes before kick-off and vanish 5-20 minutes after the end;
     * there is no history. Bet ids in {@code query} are <b>in-play</b> ids.
     */
    public List<LiveOdds> getLiveOdds(LiveOddsQuery query) throws IOException, ApiFootballException {
        return list(url("odds/live", query), LiveOdds.class);
    }

    /** In-play odds of every fixture currently available. 1 request (can be large). */
    public List<LiveOdds> getLiveOdds() throws IOException, ApiFootballException {
        return getLiveOdds(null);
    }

    /** In-play odds of one fixture, or {@code null} when it has none right now. 1 request. */
    public LiveOdds getLiveOddsByFixture(int fixture) throws IOException, ApiFootballException {
        return first(url("odds/live", new LiveOddsQuery().fixture(fixture)), LiveOdds.class);
    }

    /** In-play odds of one fixture restricted to one <b>in-play</b> bet id, or {@code null}. 1 request. */
    public LiveOdds getLiveOddsByFixture(int fixture, int liveBet) throws IOException, ApiFootballException {
        return first(url("odds/live", new LiveOddsQuery().fixture(fixture).bet(liveBet)), LiveOdds.class);
    }

    /** In-play odds of every live fixture of a league (no season needed). 1 request. */
    public List<LiveOdds> getLiveOddsByLeague(int league) throws IOException, ApiFootballException {
        return getLiveOdds(new LiveOddsQuery().league(league));
    }

    // ------------------------------------------------------------------ live bets

    /** All in-play bet types (~266). 1 request. Ids are NOT valid for pre-match odds. */
    public List<LiveBet> getLiveBets() throws IOException, ApiFootballException {
        return getLiveBets(null);
    }

    /** In-play bet types filtered by {@code query} ({@code null} = no filter). 1 request. */
    public List<LiveBet> getLiveBets(LiveBetsQuery query) throws IOException, ApiFootballException {
        return list(url("odds/live/bets", query), LiveBet.class);
    }

    /** The in-play bet with the given id, or {@code null}. 1 request. */
    public LiveBet getLiveBetById(int id) throws IOException, ApiFootballException {
        return first(url("odds/live/bets", new LiveBetsQuery().id(id)), LiveBet.class);
    }

    /** In-play bets whose name matches {@code search} (min 3 characters). 1 request. */
    public List<LiveBet> searchLiveBets(String search) throws IOException, ApiFootballException {
        return getLiveBets(new LiveBetsQuery().search(search));
    }

    private static <T> T firstOf(Page<T> page) {
        List<T> items = page.getItems();
        return items.isEmpty() ? null : items.get(0);
    }
}

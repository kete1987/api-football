package es.com.kete1987.apifootball.library;

import es.com.kete1987.apifootball.library.common.util.ApiFootballException;
import es.com.kete1987.apifootball.library.common.util.Constants;
import es.com.kete1987.apifootball.library.leagues.model.Country;
import es.com.kete1987.apifootball.library.leagues.model.League;
import es.com.kete1987.apifootball.library.leagues.model.Standings;
import es.com.kete1987.apifootball.library.leagues.query.CountriesQuery;
import es.com.kete1987.apifootball.library.leagues.query.LeaguesQuery;
import es.com.kete1987.apifootball.library.leagues.query.StandingsQuery;
import okhttp3.OkHttpClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Timezones, countries, leagues, seasons and standings.
 *
 * <p>Can be used standalone ({@code new LeaguesApi("apiKey")}) or via the facade. Every method
 * costs exactly one request.
 */
public class LeaguesApi extends ApiFootballBase {

    public LeaguesApi(String apiKey) {
        this(buildHttpClient(apiKey), Constants.BASE_URL, new RateLimitTracker());
    }

    LeaguesApi(OkHttpClient client, String baseUrl, RateLimitTracker tracker) {
        super(client, baseUrl, tracker);
    }

    // ------------------------------------------------------------------ /timezone

    /**
     * Returns every timezone accepted by the {@code timezone} parameter of other endpoints
     * (about 427 IANA names). It never changes: one call when needed is enough.
     */
    public List<String> getTimezones() throws IOException, ApiFootballException {
        return list(url("timezone"), String.class);
    }

    // ------------------------------------------------------------------ /countries

    /** Returns every country available in {@code /leagues}. Updated when a new country appears; 1 call/day is enough. */
    public List<Country> getCountries() throws IOException, ApiFootballException {
        return list(url("countries"), Country.class);
    }

    /** Returns the countries matching the filters of the query (all combinable). */
    public List<Country> getCountries(CountriesQuery query) throws IOException, ApiFootballException {
        return list(url("countries", query), Country.class);
    }

    /** Returns the country with the given name (case-insensitive, e.g. {@code "england"}), or {@code null}. */
    public Country getCountryByName(String name) throws IOException, ApiFootballException {
        return first(param(url("countries"), "name", name), Country.class);
    }

    /** Returns the country with the given alpha code (e.g. {@code "ES"}, {@code "GB-ENG"}), or {@code null}. */
    public Country getCountryByCode(String code) throws IOException, ApiFootballException {
        return first(param(url("countries"), "code", code), Country.class);
    }

    /** Searches countries by name; {@code search} must have at least 3 characters. */
    public List<Country> searchCountries(String search) throws IOException, ApiFootballException {
        return list(param(url("countries"), "search", search), Country.class);
    }

    // ------------------------------------------------------------------ /leagues

    /**
     * Returns the leagues and cups matching the query, each with its seasons and coverage.
     * With an empty query every competition is returned (a large response). Updated several times
     * a day; 1 call/hour is enough.
     */
    public List<League> getLeagues(LeaguesQuery query) throws IOException, ApiFootballException {
        return list(url("leagues", query), League.class);
    }

    /** Returns the competition with the given id (all its seasons), or {@code null} when it does not exist. */
    public League getLeagueById(int id) throws IOException, ApiFootballException {
        return first(param(url("leagues"), "id", id), League.class);
    }

    /** Returns the competition with the given id restricted to one season, or {@code null}. */
    public League getLeagueById(int id, int season) throws IOException, ApiFootballException {
        return first(url("leagues", new LeaguesQuery().id(id).season(season)), League.class);
    }

    /** Returns the competitions of a country (name, e.g. {@code "Spain"}). */
    public List<League> getLeaguesByCountry(String country) throws IOException, ApiFootballException {
        return getLeagues(new LeaguesQuery().country(country));
    }

    /** Returns the competitions of a country that have the given season. */
    public List<League> getLeaguesByCountry(String country, int season) throws IOException, ApiFootballException {
        return getLeagues(new LeaguesQuery().country(country).season(season));
    }

    /** Returns the competitions of a country, only with their current (or last) season. */
    public List<League> getCurrentLeaguesByCountry(String country) throws IOException, ApiFootballException {
        return getLeagues(new LeaguesQuery().country(country).current(true));
    }

    /** Returns every competition that has the given season. */
    public List<League> getLeaguesBySeason(int season) throws IOException, ApiFootballException {
        return getLeagues(new LeaguesQuery().season(season));
    }

    /** Returns the competitions the given team plays or played in. */
    public List<League> getLeaguesByTeam(int team) throws IOException, ApiFootballException {
        return getLeagues(new LeaguesQuery().team(team));
    }

    /** Returns every competition, only with its current (or last) season. */
    public List<League> getCurrentLeagues() throws IOException, ApiFootballException {
        return getLeagues(new LeaguesQuery().current(true));
    }

    /** Searches competitions by league name or country; {@code search} must have at least 3 characters. */
    public List<League> searchLeagues(String search) throws IOException, ApiFootballException {
        return getLeagues(new LeaguesQuery().search(search));
    }

    // ------------------------------------------------------------------ /leagues/seasons

    /** Returns every season year available (e.g. 2008 ... 2027). Updated when a league is added. */
    public List<Integer> getLeagueSeasons() throws IOException, ApiFootballException {
        return list(url("leagues/seasons"), Integer.class);
    }

    // ------------------------------------------------------------------ /standings

    /**
     * Returns the standings of the competitions matching the query. {@code season} and one of
     * {@code league}/{@code team} are required. Updated hourly.
     *
     * @return one {@link Standings} per competition (several when queried by team)
     */
    public List<Standings> getStandings(StandingsQuery query) throws IOException, ApiFootballException {
        List<StandingsItem> items = list(url("standings", query), StandingsItem.class);
        List<Standings> result = new ArrayList<>(items.size());
        for (StandingsItem item : items) {
            if (item != null && item.league != null) result.add(item.league);
        }
        return result;
    }

    /**
     * Returns the standings of a league in a season (all tables/groups; see
     * {@link Standings#getTable()}), or {@code null} when the API has none.
     */
    public Standings getStandings(int league, int season) throws IOException, ApiFootballException {
        List<Standings> result = getStandings(new StandingsQuery().league(league).season(season));
        return result.isEmpty() ? null : result.get(0);
    }

    /** Returns the standings of every competition a team plays in during a season. */
    public List<Standings> getStandingsByTeam(int team, int season) throws IOException, ApiFootballException {
        return getStandings(new StandingsQuery().team(team).season(season));
    }

    /** Item of the standings response: {@code {"league": {...}}}. */
    private static class StandingsItem {
        private Standings league;
    }
}

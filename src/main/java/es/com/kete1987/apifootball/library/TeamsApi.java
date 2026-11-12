package es.com.kete1987.apifootball.library;

import es.com.kete1987.apifootball.library.common.query.QueryValidation;
import es.com.kete1987.apifootball.library.common.util.ApiFootballException;
import es.com.kete1987.apifootball.library.common.util.Constants;
import es.com.kete1987.apifootball.library.leagues.model.Country;
import es.com.kete1987.apifootball.library.teams.model.Coach;
import es.com.kete1987.apifootball.library.teams.model.Team;
import es.com.kete1987.apifootball.library.teams.model.TeamStatistics;
import es.com.kete1987.apifootball.library.teams.model.Venue;
import es.com.kete1987.apifootball.library.teams.query.CoachesQuery;
import es.com.kete1987.apifootball.library.teams.query.TeamsQuery;
import es.com.kete1987.apifootball.library.teams.query.VenuesQuery;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * Teams, team statistics, venues and coaches.
 *
 * <p>Can be used standalone ({@code new TeamsApi("apiKey")}) or via the facade. Every method
 * costs exactly one request.
 */
public class TeamsApi extends ApiFootballBase {

    public TeamsApi(String apiKey) {
        this(buildHttpClient(apiKey), Constants.BASE_URL, new RateLimitTracker());
    }

    TeamsApi(OkHttpClient client, String baseUrl, RateLimitTracker tracker) {
        super(client, baseUrl, tracker);
    }

    // ------------------------------------------------------------------ /teams

    /**
     * Returns the teams matching the query, each with its home venue. <b>At least one parameter
     * is required</b> (the API answers an error otherwise). Updated several times a week.
     */
    public List<Team> getTeams(TeamsQuery query) throws IOException, ApiFootballException {
        return list(url("teams", query), Team.class);
    }

    /** Returns the team with the given id, or {@code null} when it does not exist. */
    public Team getTeamById(int id) throws IOException, ApiFootballException {
        return first(param(url("teams"), "id", id), Team.class);
    }

    /** Returns the teams of a league in a season. */
    public List<Team> getTeamsByLeague(int league, int season) throws IOException, ApiFootballException {
        return getTeams(new TeamsQuery().league(league).season(season));
    }

    /** Returns the teams of a country (name, e.g. {@code "Spain"}). */
    public List<Team> getTeamsByCountry(String country) throws IOException, ApiFootballException {
        return getTeams(new TeamsQuery().country(country));
    }

    /** Returns the teams playing at a venue. */
    public List<Team> getTeamsByVenue(int venue) throws IOException, ApiFootballException {
        return getTeams(new TeamsQuery().venue(venue));
    }

    /** Searches teams by name or country; {@code search} must have at least 3 characters. */
    public List<Team> searchTeams(String search) throws IOException, ApiFootballException {
        return getTeams(new TeamsQuery().search(search));
    }

    // ------------------------------------------------------------------ /teams/statistics

    /**
     * Returns the statistics of a team in a league and season (all three are required), or
     * {@code null} when the API has no data. Updated twice a day.
     */
    public TeamStatistics getTeamStatistics(int league, int season, int team) throws IOException, ApiFootballException {
        return object(statisticsUrl(league, season, team), TeamStatistics.class);
    }

    /**
     * Same as {@link #getTeamStatistics(int, int, int)} but computed only with the matches played
     * up to {@code date} ({@code YYYY-MM-DD}). One request.
     *
     * @throws IllegalArgumentException when the date is {@code null}, malformed or not a real
     *                                  calendar date (the API would silently overflow it)
     */
    public TeamStatistics getTeamStatistics(int league, int season, int team, String date)
            throws IOException, ApiFootballException {
        return object(param(statisticsUrl(league, season, team), "date", QueryValidation.date("date", date)),
                TeamStatistics.class);
    }

    /**
     * Same as {@link #getTeamStatistics(int, int, int)} but computed only with the matches played
     * up to {@code date}. One request.
     *
     * @throws IllegalArgumentException when the date is {@code null}
     */
    public TeamStatistics getTeamStatistics(int league, int season, int team, LocalDate date)
            throws IOException, ApiFootballException {
        return object(param(statisticsUrl(league, season, team), "date", QueryValidation.date("date", date)),
                TeamStatistics.class);
    }

    private HttpUrl.Builder statisticsUrl(int league, int season, int team) {
        HttpUrl.Builder url = url("teams/statistics");
        param(url, "league", league);
        param(url, "season", season);
        param(url, "team", team);
        return url;
    }

    // ------------------------------------------------------------------ /teams/seasons

    /** Returns the seasons (years) for which a team has data. */
    public List<Integer> getTeamSeasons(int team) throws IOException, ApiFootballException {
        return list(param(url("teams/seasons"), "team", team), Integer.class);
    }

    // ------------------------------------------------------------------ /teams/countries

    /**
     * Returns the countries available for {@code /teams}. Names are hyphenated and some entries
     * are not countries; {@code code} and {@code flag} may be {@code null}.
     */
    public List<Country> getTeamCountries() throws IOException, ApiFootballException {
        return list(url("teams/countries"), Country.class);
    }

    // ------------------------------------------------------------------ /venues

    /** Returns the venues matching the query. <b>At least one parameter is required.</b> */
    public List<Venue> getVenues(VenuesQuery query) throws IOException, ApiFootballException {
        return list(url("venues", query), Venue.class);
    }

    /** Returns the venue with the given id, or {@code null} when it does not exist. */
    public Venue getVenueById(int id) throws IOException, ApiFootballException {
        return first(param(url("venues"), "id", id), Venue.class);
    }

    /** Returns the venues of a city. */
    public List<Venue> getVenuesByCity(String city) throws IOException, ApiFootballException {
        return getVenues(new VenuesQuery().city(city));
    }

    /** Returns the venues of a country (name). */
    public List<Venue> getVenuesByCountry(String country) throws IOException, ApiFootballException {
        return getVenues(new VenuesQuery().country(country));
    }

    /** Searches venues by name, city or country; {@code search} must have at least 3 characters. */
    public List<Venue> searchVenues(String search) throws IOException, ApiFootballException {
        return getVenues(new VenuesQuery().search(search));
    }

    // ------------------------------------------------------------------ /coachs

    /** Returns the coaches matching the query (endpoint {@code /coachs}). Updated daily. */
    public List<Coach> getCoaches(CoachesQuery query) throws IOException, ApiFootballException {
        return list(url("coachs", query), Coach.class);
    }

    /** Returns the coach with the given id, or {@code null} when none exists. */
    public Coach getCoachById(int id) throws IOException, ApiFootballException {
        return first(param(url("coachs"), "id", id), Coach.class);
    }

    /** Returns the current and past coaches of a team. */
    public List<Coach> getCoachesByTeam(int team) throws IOException, ApiFootballException {
        return getCoaches(new CoachesQuery().team(team));
    }

    /** Searches coaches by name; {@code search} must have at least 3 characters. */
    public List<Coach> searchCoaches(String search) throws IOException, ApiFootballException {
        return getCoaches(new CoachesQuery().search(search));
    }
}

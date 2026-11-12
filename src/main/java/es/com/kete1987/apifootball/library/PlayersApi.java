package es.com.kete1987.apifootball.library;

import es.com.kete1987.apifootball.library.common.model.Page;
import es.com.kete1987.apifootball.library.common.query.QueryValidation;
import es.com.kete1987.apifootball.library.common.util.ApiFootballException;
import es.com.kete1987.apifootball.library.common.util.Constants;
import es.com.kete1987.apifootball.library.players.model.Player;
import es.com.kete1987.apifootball.library.players.model.PlayerProfile;
import es.com.kete1987.apifootball.library.players.model.PlayerTeam;
import es.com.kete1987.apifootball.library.players.model.PlayerTransfers;
import es.com.kete1987.apifootball.library.players.model.PlayerWithStatistics;
import es.com.kete1987.apifootball.library.players.model.SidelinedGroup;
import es.com.kete1987.apifootball.library.players.model.SidelinedPeriod;
import es.com.kete1987.apifootball.library.players.model.Squad;
import es.com.kete1987.apifootball.library.players.model.Trophy;
import es.com.kete1987.apifootball.library.players.model.TrophyGroup;
import es.com.kete1987.apifootball.library.players.query.PlayerProfilesQuery;
import es.com.kete1987.apifootball.library.players.query.PlayersQuery;
import es.com.kete1987.apifootball.library.players.query.SquadsQuery;
import okhttp3.OkHttpClient;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * Players sub-API: player profiles and statistics, squads, career teams, top lists, transfers,
 * trophies and sidelined history.
 *
 * <p>Can be used standalone ({@code new PlayersApi("apiKey")}) or via the facade.
 *
 * <p>{@code /players} (20 items per page) and {@code /players/profiles} (250 per page, 2,771
 * pages without filters) are paginated and <b>every page is one request against the daily
 * quota</b>; each has a single-page method and an all-pages method capped by {@code maxPages}.
 */
public class PlayersApi extends ApiFootballBase {

    public PlayersApi(String apiKey) {
        this(buildHttpClient(apiKey), Constants.BASE_URL, new RateLimitTracker());
    }

    PlayersApi(OkHttpClient client, String baseUrl, RateLimitTracker tracker) {
        super(client, baseUrl, tracker);
    }

    // ------------------------------------------------------------------ /players

    /**
     * Returns one page (20 players) of {@code /players}. Cost: exactly 1 request.
     * Requires at least one of id, team or league in the query; season requires one of them.
     *
     * @param page 1-based page number; a page beyond {@code paging.total} yields an empty page, not an error
     * @throws IllegalArgumentException when {@code page} is lower than 1
     */
    public Page<PlayerWithStatistics> getPlayersPage(PlayersQuery query, int page)
            throws IOException, ApiFootballException {
        return page(url("players", query), PlayerWithStatistics.class, page);
    }

    /**
     * Returns the players of {@code /players} across pages, starting at page 1.
     * <b>Cost: one request per page fetched</b> (at most {@code maxPages}; a league-season is
     * about 31 pages).
     *
     * @param maxPages maximum number of pages to fetch, at least 1
     */
    public List<PlayerWithStatistics> getAllPlayers(PlayersQuery query, int maxPages)
            throws IOException, ApiFootballException {
        return allPages(url("players", query), PlayerWithStatistics.class, requireMaxPages(maxPages));
    }

    /**
     * Returns a player with his statistics of a season (all competitions). Cost: 1 request.
     *
     * @return the player, or {@code null} when not found
     */
    public PlayerWithStatistics getPlayerById(int playerId, int season) throws IOException, ApiFootballException {
        return first(url("players", new PlayersQuery().id(playerId).season(season)), PlayerWithStatistics.class);
    }

    /** One page (20 players) of the players of a team in a season. Cost: 1 request. */
    public Page<PlayerWithStatistics> getPlayersByTeamPage(int team, int season, int page)
            throws IOException, ApiFootballException {
        return getPlayersPage(new PlayersQuery().team(team).season(season), page);
    }

    /** One page (20 players) of the players of a league in a season. Cost: 1 request. */
    public Page<PlayerWithStatistics> getPlayersByLeaguePage(int league, int season, int page)
            throws IOException, ApiFootballException {
        return getPlayersPage(new PlayersQuery().league(league).season(season), page);
    }

    /**
     * Searches players by name inside a league season (search needs at least 4 characters and a
     * league or team). Cost: 1 request (one page of 20).
     */
    public Page<PlayerWithStatistics> searchPlayersPage(String search, int league, int season, int page)
            throws IOException, ApiFootballException {
        return getPlayersPage(new PlayersQuery().search(search).league(league).season(season), page);
    }

    // ------------------------------------------------------------------ /players/seasons

    /** Every season with player statistics (1962...). Cost: 1 request. */
    public List<Integer> getPlayerSeasons() throws IOException, ApiFootballException {
        return list(url("players/seasons"), Integer.class);
    }

    /** Seasons with statistics for one player. Cost: 1 request. */
    public List<Integer> getPlayerSeasons(int playerId) throws IOException, ApiFootballException {
        return list(param(url("players/seasons"), "player", playerId), Integer.class);
    }

    // ------------------------------------------------------------------ /players/profiles

    /**
     * Returns one page (250 profiles) of {@code /players/profiles}. Cost: exactly 1 request.
     * Without filters there are 2,771 pages.
     */
    public Page<PlayerProfile> getPlayerProfilesPage(PlayerProfilesQuery query, int page)
            throws IOException, ApiFootballException {
        return page(url("players/profiles", query), PlayerProfile.class, page);
    }

    /**
     * Returns profiles across pages, starting at page 1.
     * <b>Cost: one request per page fetched</b> (at most {@code maxPages}; 2,771 pages without filters).
     *
     * @param maxPages maximum number of pages to fetch, at least 1
     */
    public List<PlayerProfile> getAllPlayerProfiles(PlayerProfilesQuery query, int maxPages)
            throws IOException, ApiFootballException {
        return allPages(url("players/profiles", query), PlayerProfile.class, requireMaxPages(maxPages));
    }

    /** Profile of one player. Cost: 1 request. @return the player, or {@code null} when not found */
    public Player getPlayerProfile(int playerId) throws IOException, ApiFootballException {
        PlayerProfile profile = first(url("players/profiles", new PlayerProfilesQuery().player(playerId)),
                PlayerProfile.class);
        return profile != null ? profile.getPlayer() : null;
    }

    /** One page (250) of profiles whose last name matches {@code search} (min 3 characters). Cost: 1 request. */
    public Page<PlayerProfile> searchPlayerProfilesPage(String search, int page)
            throws IOException, ApiFootballException {
        return getPlayerProfilesPage(new PlayerProfilesQuery().search(search), page);
    }

    // ------------------------------------------------------------------ /players/squads

    /** Squads per {@code /players/squads}; the query needs a team or a player. Cost: 1 request. */
    public List<Squad> getSquads(SquadsQuery query) throws IOException, ApiFootballException {
        return list(url("players/squads", query), Squad.class);
    }

    /** Current squad of a team. Cost: 1 request. @return the squad, or {@code null} when none */
    public Squad getSquad(int teamId) throws IOException, ApiFootballException {
        return first(url("players/squads", new SquadsQuery().team(teamId)), Squad.class);
    }

    /** Teams (with their squads) a player currently belongs to. Cost: 1 request. */
    public List<Squad> getSquadsByPlayer(int playerId) throws IOException, ApiFootballException {
        return getSquads(new SquadsQuery().player(playerId));
    }

    // ------------------------------------------------------------------ /players/teams

    /** Teams and seasons of a player's career. Cost: 1 request. */
    public List<PlayerTeam> getPlayerTeams(int playerId) throws IOException, ApiFootballException {
        return list(param(url("players/teams"), "player", playerId), PlayerTeam.class);
    }

    // ------------------------------------------------------------------ top lists

    /** 20 best scorers of a league season. Cost: 1 request (not paginated). */
    public List<PlayerWithStatistics> getTopScorers(int league, int season) throws IOException, ApiFootballException {
        return top("players/topscorers", league, season);
    }

    /** 20 best assist providers of a league season. Cost: 1 request. */
    public List<PlayerWithStatistics> getTopAssists(int league, int season) throws IOException, ApiFootballException {
        return top("players/topassists", league, season);
    }

    /** 20 players with most yellow cards of a league season. Cost: 1 request. */
    public List<PlayerWithStatistics> getTopYellowCards(int league, int season)
            throws IOException, ApiFootballException {
        return top("players/topyellowcards", league, season);
    }

    /** 20 players with most red cards of a league season. Cost: 1 request. */
    public List<PlayerWithStatistics> getTopRedCards(int league, int season) throws IOException, ApiFootballException {
        return top("players/topredcards", league, season);
    }

    private List<PlayerWithStatistics> top(String path, int league, int season)
            throws IOException, ApiFootballException {
        return list(param(param(url(path), "league", league), "season", season), PlayerWithStatistics.class);
    }

    // ------------------------------------------------------------------ /transfers

    /** Transfer history of a player. Cost: 1 request. */
    public List<PlayerTransfers> getTransfersByPlayer(int playerId) throws IOException, ApiFootballException {
        return list(param(url("transfers"), "player", playerId), PlayerTransfers.class);
    }

    /** Transfers of a team (one element per player; may be a large response). Cost: 1 request. */
    public List<PlayerTransfers> getTransfersByTeam(int teamId) throws IOException, ApiFootballException {
        return list(param(url("transfers"), "team", teamId), PlayerTransfers.class);
    }

    // ------------------------------------------------------------------ /trophies

    /** Trophies of one player (flat list). Cost: 1 request. */
    public List<Trophy> getTrophiesByPlayer(int playerId) throws IOException, ApiFootballException {
        return list(param(url("trophies"), "player", playerId), Trophy.class);
    }

    /** Trophies of one coach (flat list). Cost: 1 request. */
    public List<Trophy> getTrophiesByCoach(int coachId) throws IOException, ApiFootballException {
        return list(param(url("trophies"), "coach", coachId), Trophy.class);
    }

    /**
     * Trophies of several players, grouped by player id.
     *
     * @param playerIds 1 to 20 ids, sent joined by "-"; Cost: 1 request
     * @throws IllegalArgumentException if {@code null}, empty, with more than 20 ids or containing {@code null}
     */
    public List<TrophyGroup> getTrophiesByPlayers(Collection<Integer> playerIds) throws IOException, ApiFootballException {
        return list(param(url("trophies"), "players", joinIds("players", playerIds)), TrophyGroup.class);
    }

    /**
     * Trophies of several coaches, grouped by coach id.
     *
     * @param coachIds 1 to 20 ids, sent joined by "-" (parameter {@code coachs}); Cost: 1 request
     * @throws IllegalArgumentException if {@code null}, empty, with more than 20 ids or containing {@code null}
     */
    public List<TrophyGroup> getTrophiesByCoachs(Collection<Integer> coachIds) throws IOException, ApiFootballException {
        return list(param(url("trophies"), "coachs", joinIds("coaches", coachIds)), TrophyGroup.class);
    }

    // ------------------------------------------------------------------ /sidelined

    /** Injury/suspension history of one player (flat list). Cost: 1 request. */
    public List<SidelinedPeriod> getSidelinedByPlayer(int playerId) throws IOException, ApiFootballException {
        return list(param(url("sidelined"), "player", playerId), SidelinedPeriod.class);
    }

    /** Sidelined history of one coach (flat list; may be empty). Cost: 1 request. */
    public List<SidelinedPeriod> getSidelinedByCoach(int coachId) throws IOException, ApiFootballException {
        return list(param(url("sidelined"), "coach", coachId), SidelinedPeriod.class);
    }

    /**
     * Sidelined history of several players, grouped by player id.
     *
     * @param playerIds 1 to 20 ids, sent joined by "-"; Cost: 1 request
     * @throws IllegalArgumentException if {@code null}, empty, with more than 20 ids or containing {@code null}
     */
    public List<SidelinedGroup> getSidelinedByPlayers(Collection<Integer> playerIds)
            throws IOException, ApiFootballException {
        return list(param(url("sidelined"), "players", joinIds("players", playerIds)), SidelinedGroup.class);
    }

    /**
     * Sidelined history of several coaches, grouped by coach id.
     *
     * @param coachIds 1 to 20 ids, sent joined by "-" (parameter {@code coachs}); Cost: 1 request
     * @throws IllegalArgumentException if {@code null}, empty, with more than 20 ids or containing {@code null}
     */
    public List<SidelinedGroup> getSidelinedByCoachs(Collection<Integer> coachIds) throws IOException, ApiFootballException {
        return list(param(url("sidelined"), "coachs", joinIds("coaches", coachIds)), SidelinedGroup.class);
    }

    // ------------------------------------------------------------------ helpers

    private static int requireMaxPages(int maxPages) {
        if (maxPages < 1) throw new IllegalArgumentException("maxPages must be >= 1");
        return maxPages;
    }

    private static String joinIds(String name, Collection<Integer> ids) {
        return QueryValidation.ids(name, ids);
    }
}

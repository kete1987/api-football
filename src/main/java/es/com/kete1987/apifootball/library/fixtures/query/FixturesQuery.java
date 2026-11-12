package es.com.kete1987.apifootball.library.fixtures.query;

import es.com.kete1987.apifootball.library.common.query.QueryValidation;

import java.util.Collection;

/**
 * Parameters of {@code GET /fixtures}. Only what is set is sent.
 *
 * <p>Combinations documented by the API: {@code id}; {@code ids}; {@code live}; {@code league+season};
 * {@code date}; {@code next}; {@code last}; {@code date+league+season}; {@code league+next};
 * {@code venue+next}; {@code league+last+status}; {@code team+last+timezone};
 * {@code team+season+from+to}; {@code league+season+from+to+timezone}; {@code league+season+round}.
 * Shared filters ({@code date, league, season, last, next, from, to, status, venue, timezone}) are
 * inherited from {@link FixtureFilters}.
 *
 * <p>With {@code id}/{@code ids} the response embeds events, lineups, statistics and players; with
 * {@code live} it embeds events only.
 */
public class FixturesQuery extends FixtureFilters<FixturesQuery> {

    /** A single fixture id; the response embeds events, lineups, statistics and players. */
    public FixturesQuery id(int id) {
        return param("id", id);
    }

    /**
     * Several fixture ids (1 to 20), sent as {@code id-id-id}; the response embeds events, lineups,
     * statistics and players.
     */
    public FixturesQuery ids(Collection<Integer> ids) {
        return param("ids", ids == null ? null : QueryValidation.ids("ids", ids));
    }

    /** Several fixture ids already joined as {@code "id-id-id"} (digits and dashes, at most 20). */
    public FixturesQuery ids(String ids) {
        return param("ids", ids == null ? null : QueryValidation.ids("ids", ids));
    }

    /** {@code live=all}: every fixture in progress; the response embeds events. */
    public FixturesQuery liveAll() {
        return param("live", "all");
    }

    /** Fixtures in progress of the given leagues, sent as {@code id-id}; the response embeds events. */
    public FixturesQuery live(Collection<Integer> leagues) {
        if (leagues == null) return param("live", null);
        if (leagues.isEmpty()) {
            throw new IllegalArgumentException("leagues must contain at least one id");
        }
        StringBuilder sb = new StringBuilder();
        for (Integer id : leagues) {
            if (id == null) throw new IllegalArgumentException("leagues must not contain null");
            if (sb.length() > 0) sb.append('-');
            sb.append(id);
        }
        return param("live", sb.toString());
    }

    /** Raw {@code live} value: {@code "all"} or league ids joined as {@code "id-id"}. */
    public FixturesQuery live(String live) {
        if (live == null) return param("live", null);
        if (!(live.equals("all") || live.matches("\\d+(-\\d+)*"))) {
            throw new IllegalArgumentException("live must be 'all' or league ids like 'id-id', was '" + live + "'");
        }
        return param("live", live);
    }

    /** Team id. Combine with {@code last}, {@code next} or {@code season}+{@code from}+{@code to}. */
    public FixturesQuery team(int team) {
        return param("team", team);
    }

    /** Round label as returned by {@code /fixtures/rounds}, e.g. {@code "Regular Season - 13"}. */
    public FixturesQuery round(String round) {
        if (round == null) return param("round", null);
        if (round.trim().isEmpty()) {
            throw new IllegalArgumentException("round must not be blank");
        }
        return param("round", round);
    }
}

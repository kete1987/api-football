package es.com.kete1987.apifootball.library.players.model;

import es.com.kete1987.apifootball.library.common.util.ModelCollections;

import java.util.List;

/**
 * One element of {@code /players} and of the four {@code /players/top*} lists: a player and one
 * statistics entry per team/competition (a player transferred mid-season, or playing several
 * competitions, has several).
 */
public class PlayerWithStatistics {
    private Player player;
    private List<PlayerStatistics> statistics;

    public Player getPlayer() {
        return player;
    }

    public List<PlayerStatistics> getStatistics() {
        return ModelCollections.unmodifiable(statistics);
    }
}

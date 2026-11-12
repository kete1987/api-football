package es.com.kete1987.apifootball.library.players.model;

import es.com.kete1987.apifootball.library.common.model.TeamRef;
import es.com.kete1987.apifootball.library.common.util.ModelCollections;

import java.util.List;

/** One element of {@code /players/teams}: a team of the player's career and the seasons played there. */
public class PlayerTeam {
    private TeamRef team;
    private List<Integer> seasons;

    public TeamRef getTeam() {
        return team;
    }

    /** Seasons (4-digit start years), descending; may be empty. */
    public List<Integer> getSeasons() {
        return ModelCollections.unmodifiable(seasons);
    }
}

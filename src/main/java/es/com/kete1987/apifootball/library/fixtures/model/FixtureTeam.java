package es.com.kete1987.apifootball.library.fixtures.model;

import es.com.kete1987.apifootball.library.common.model.TeamRef;

/** A side of a fixture: team reference plus {@code winner} ({@code null} for a draw or an unfinished match). */
public class FixtureTeam extends TeamRef {
    private Boolean winner;

    public Boolean getWinner() {
        return winner;
    }
}

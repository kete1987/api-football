package es.com.kete1987.apifootball.library.fixtures.model;

import es.com.kete1987.apifootball.library.fixtures.util.EventTypes;

/** An event of a fixture (goal, card, substitution, VAR). See {@link EventTypes}. */
public class FixtureEvent {
    private Time time;
    private TeamRef team;
    private PlayerRef player;
    private PlayerRef assist;
    private String type;
    private String detail;
    private String comments;

    public Time getTime() {
        return time;
    }

    public TeamRef getTeam() {
        return team;
    }

    /** Player involved; its members can be {@code null} (seen in live fixtures). */
    public PlayerRef getPlayer() {
        return player;
    }

    /** Assisting player; for substitutions the player coming in. Members are {@code null} for most cards. */
    public PlayerRef getAssist() {
        return assist;
    }

    /** {@code "Goal"}, {@code "Card"}, {@code "subst"}, {@code "Var"}... (casing varies). */
    public String getType() {
        return type;
    }

    /** E.g. {@code "Normal Goal"}, {@code "Yellow Card"}, {@code "Substitution 1"}. */
    public String getDetail() {
        return detail;
    }

    /** Free text such as {@code "Foul"}; usually {@code null}. */
    public String getComments() {
        return comments;
    }

    private boolean typeIs(String expected) {
        return type != null && type.equalsIgnoreCase(expected);
    }

    public boolean isGoal() {
        return typeIs(EventTypes.GOAL);
    }

    public boolean isCard() {
        return typeIs(EventTypes.CARD);
    }

    public boolean isSubstitution() {
        return typeIs(EventTypes.SUBSTITUTION);
    }

    public boolean isVar() {
        return typeIs(EventTypes.VAR);
    }

    /** Minute of an event; {@code extra} is the added time or {@code null}. */
    public static class Time {
        private Integer elapsed;
        private Integer extra;

        public Integer getElapsed() {
            return elapsed;
        }

        public Integer getExtra() {
            return extra;
        }
    }
}

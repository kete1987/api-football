package es.com.kete1987.apifootball.library.fixtures.model;

import es.com.kete1987.apifootball.library.common.model.TeamRef;
import es.com.kete1987.apifootball.library.fixtures.util.EventTypes;

import java.util.Comparator;

/**
 * An event of a fixture (goal, card, substitution, VAR). See {@link EventTypes}.
 *
 * <p><b>How the API classifies events.</b> Every event has a coarse {@code type} and a finer
 * {@code detail}, and the {@code type} alone does not say what happened:
 * <ul>
 *   <li>{@code type "Goal"} covers everything around goals and penalties: {@code detail}
 *       {@code "Normal Goal"}, {@code "Penalty"} (a scored penalty), {@code "Own Goal"} and
 *       {@code "Missed Penalty"} &mdash; a penalty that did <em>not</em> go in is still
 *       {@code type "Goal"}. Use {@link #isGoal()}, which excludes it, never {@code type} alone.</li>
 *   <li>{@code type "Card"}: {@code "Yellow Card"}, {@code "Red Card"} (the documentation spells it
 *       {@code "Red card"}). A second yellow arrives as two events at the same minute for the same
 *       player, {@code "Yellow Card"} and {@code "Red Card"}.</li>
 *   <li>{@code type "subst"} (lower case in real responses): {@code "Substitution 1"},
 *       {@code "Substitution 2"}... {@code player} is the player going <em>off</em> and {@code assist}
 *       the player coming <em>on</em>; see {@link #getPlayerOut()} / {@link #getPlayerIn()}.</li>
 *   <li>{@code type "Var"}: {@code "Goal cancelled"}, {@code "Penalty confirmed"} and, in real
 *       responses, {@code "Card upgrade confirmed"}; the {@code detail} can be missing for the first
 *       30 s or so after the event appears.</li>
 * </ul>
 * Every helper compares case-insensitively, ignores surrounding whitespace, accepts {@code null}
 * members and never throws.
 *
 * <p><b>Verified against real fixtures</b> ({@code /fixtures?id=}, see docs/API_CATALOG.md section 11.16):
 * <ul>
 *   <li><b>Penalty shoot-out kicks are not events.</b> In the 2026 Champions League final (fixture
 *       1544371, status {@code PEN}, {@code goals} 1-1, {@code score.penalty} 4-3, ten kicks taken) the
 *       last event is a card at 118'; no kick, scored or missed, appears. The shoot-out is only in
 *       {@code score.penalty}. Summing {@link #isGoal()} events per team therefore gives
 *       {@code goals} (extra time included, shoot-out excluded), as the API does.</li>
 *   <li><b>An own goal is listed under the team it counts for</b>, with {@link #getPlayer()} = the
 *       player of the <em>other</em> team who put it in (Getafe 0-1 Atletico Madrid, fixture 1390944:
 *       {@code team} Atletico Madrid, {@code player} D. Duarte of Getafe). Use
 *       {@link #getTeamGoalCountsFor()} to attribute goals.</li>
 *   <li>Events are not strictly sorted by minute ({@code 90+6} has been listed before {@code 90}),
 *       and the same player can carry different spellings in different events ({@code "Julian
 *       Alvarez"} / {@code "J. Alvarez"}): order by {@link #getTime()} and match players by id.</li>
 * </ul>
 */
public class FixtureEvent {
    /**
     * Chronological order of events. The API does not return a fixture's events in chronological
     * order (a card at {@code 90+6} has been listed before a substitution at {@code 90}, and
     * {@code 90+1} before {@code 90}), so a caller drawing a timeline has to sort them.
     *
     * <p>Orders by {@code time.elapsed} ascending, then by {@code time.extra} ascending, a
     * {@code null} extra counting as 0 (so {@code 90} comes before {@code 90+1}). Events with no
     * {@code time}, or no {@code elapsed} minute, sort last (a {@code null} event too), and never
     * make the comparator throw. Events that compare equal (same minute and extra) are <b>not</b>
     * reordered by it: use a stable sort ({@link java.util.List#sort}, {@code Collections.sort}) so
     * they keep the order the API sent them in. That matters because a second yellow card arrives
     * as a {@code "Yellow Card"} event followed by a {@code "Red Card"} event at the same minute.
     *
     * <p>Example: {@code List<FixtureEvent> events = new ArrayList<>(api.fixtures().getFixtureEvents(id));
     * events.sort(FixtureEvent.BY_TIME);} (the returned list is unmodifiable, hence the copy). For a
     * fixture, {@link Fixture#getEventsSorted()} does exactly this.
     */
    public static final Comparator<FixtureEvent> BY_TIME = (a, b) -> {
        Integer ea = elapsedOf(a);
        Integer eb = elapsedOf(b);
        if (ea == null || eb == null) {
            return ea == null ? (eb == null ? 0 : 1) : -1;
        }
        int c = Integer.compare(ea, eb);
        return c != 0 ? c : Integer.compare(extraOf(a), extraOf(b));
    };

    private Time time;
    private TeamRef team;
    private PlayerRef player;
    private PlayerRef assist;
    private String type;
    private String detail;
    private String comments;

    private static Integer elapsedOf(FixtureEvent e) {
        return e == null || e.time == null ? null : e.time.elapsed;
    }

    private static int extraOf(FixtureEvent e) {
        return e.time.extra == null ? 0 : e.time.extra;
    }

    /** Minute of the event; may be {@code null}. The API's order is not chronological: see {@link #BY_TIME}. */
    public Time getTime() {
        return time;
    }

    /**
     * Team the event belongs to. For a goal this is the team the goal <b>counts for</b>, also for an
     * <b>own goal</b> (verified: the own goal is listed under the benefiting team, while
     * {@link #getPlayer()} plays for the other team). For a substitution, card or missed penalty it is
     * the team of {@link #getPlayer()}. See {@link #getTeamGoalCountsFor()}.
     */
    public TeamRef getTeam() {
        return team;
    }

    /**
     * Player of the event: the scorer (for an <b>own goal, the player who put the ball into his own
     * net, who plays for the team that is <em>not</em> {@link #getTeam()}</b>), the penalty taker
     * (also for a missed penalty), the booked player, the player a VAR decision concerns &mdash; and, for a <b>substitution, the player going
     * off</b> (see {@link #getPlayerOut()}). Its members can be {@code null}, typically for a few
     * polls right after a live event appears.
     */
    public PlayerRef getPlayer() {
        return player;
    }

    /**
     * Second player of the event: the assisting player for a goal and, for a <b>substitution, the
     * player coming on</b> (see {@link #getPlayerIn()}). Its members are {@code null} for most cards.
     * Not meaningful for other events: a real missed penalty carried the taker here too.
     */
    public PlayerRef getAssist() {
        return assist;
    }

    /** {@code "Goal"}, {@code "Card"}, {@code "subst"}, {@code "Var"}... (casing varies). Coarse: see the class comment. */
    public String getType() {
        return type;
    }

    /**
     * What happened, e.g. {@code "Normal Goal"}, {@code "Missed Penalty"}, {@code "Yellow Card"},
     * {@code "Substitution 1"}, {@code "Goal cancelled"}; may be {@code null} for a moment on new
     * live events.
     */
    public String getDetail() {
        return detail;
    }

    /** Free text such as {@code "Foul"}; usually {@code null}. */
    public String getComments() {
        return comments;
    }

    private static boolean same(String actual, String expected) {
        return actual != null && actual.trim().equalsIgnoreCase(expected);
    }

    private boolean typeIs(String expected) {
        return same(type, expected);
    }

    private boolean detailIs(String expected) {
        return same(detail, expected);
    }

    /**
     * {@code true} for a goal that counts: {@code type "Goal"} with any {@code detail} except
     * {@code "Missed Penalty"} &mdash; so normal goals, scored penalties and own goals. A missed
     * penalty is {@code type "Goal"} in the API but is not a goal; see {@link #isMissedPenalty()}.
     *
     * <p>A {@code "Goal"} event whose {@code detail} is missing or not one of the documented values
     * counts as a goal, because the {@code type} says so and the API has added values before.
     *
     * <p><b>Penalty shoot-outs.</b> Shoot-out kicks are not in the events at all (verified on a real
     * {@code PEN} fixture, see the class comment), so this method never sees one and counting
     * {@code isGoal()} events per {@link #getTeamGoalCountsFor() team} gives the API's {@code goals}
     * (extra time included, shoot-out excluded). The shoot-out result is only in
     * {@code score.penalty}. The API's own {@code goals} / {@code score} remain the authoritative
     * score: events can briefly appear and vanish during a live match.
     *
     * <p>A goal later cancelled by VAR was, in the one real case observed, removed from the events and
     * replaced by a {@code "Var"} / {@code "Goal cancelled"} event, so it no longer counts here.
     */
    public boolean isGoal() {
        return typeIs(EventTypes.GOAL) && !detailIs(EventTypes.MISSED_PENALTY);
    }

    /** {@code true} for a scored penalty during play: {@code type "Goal"}, {@code detail "Penalty"}. A subset of {@link #isGoal()}. */
    public boolean isPenaltyGoal() {
        return typeIs(EventTypes.GOAL) && detailIs(EventTypes.PENALTY);
    }

    /**
     * {@code true} for an own goal: {@code type "Goal"}, {@code detail "Own Goal"}. A subset of
     * {@link #isGoal()}. Verified on a real own goal: {@link #getTeam()} is the team the goal
     * <b>counts for</b> (the beneficiary), {@link #getPlayer()} is the player of the <b>other</b>
     * team who scored it, {@link #getAssist()} has {@code null} members and {@link #getComments()} is
     * {@code null}. The player is not credited with a goal in the per-player statistics.
     */
    public boolean isOwnGoal() {
        return typeIs(EventTypes.GOAL) && detailIs(EventTypes.OWN_GOAL);
    }

    /**
     * {@code true} for a penalty that was not scored: {@code type "Goal"}, {@code detail
     * "Missed Penalty"}. {@link #isGoal()} is {@code false} for it. {@link #getPlayer()} is the
     * taker (its members can be {@code null} at first); {@link #getAssist()} carries no meaning.
     */
    public boolean isMissedPenalty() {
        return typeIs(EventTypes.GOAL) && detailIs(EventTypes.MISSED_PENALTY);
    }

    /**
     * For a goal ({@link #isGoal()}), the team the goal counts for &mdash; the API's {@code team},
     * which for an <b>own goal is the benefiting team, not the team of {@link #getPlayer()}</b>
     * (verified on real data). {@code null} for every other event, including a missed penalty.
     * Counting events by this team reproduces the API's {@code goals}.
     */
    public TeamRef getTeamGoalCountsFor() {
        return isGoal() ? team : null;
    }

    /** {@code true} for any card ({@code type "Card"}); see {@link #isYellowCard()} and {@link #isRedCard()}. */
    public boolean isCard() {
        return typeIs(EventTypes.CARD);
    }

    /**
     * {@code true} for {@code type "Card"}, {@code detail "Yellow Card"}. A second yellow is a yellow
     * card event too, followed by a separate red card event at the same minute.
     */
    public boolean isYellowCard() {
        return typeIs(EventTypes.CARD) && detailIs(EventTypes.YELLOW_CARD);
    }

    /**
     * {@code true} for {@code type "Card"}, {@code detail "Red Card"} (any casing). Covers both a
     * straight red and the red that follows a second yellow; to tell them apart, look for a
     * {@link #isYellowCard() yellow card} of the same player at the same minute.
     */
    public boolean isRedCard() {
        return typeIs(EventTypes.CARD) && detailIs(EventTypes.RED_CARD);
    }

    /** {@code true} for a substitution ({@code type "subst"}); see {@link #getPlayerOut()} and {@link #getPlayerIn()}. */
    public boolean isSubstitution() {
        return typeIs(EventTypes.SUBSTITUTION);
    }

    /**
     * For a substitution, the player going off (the API's {@code player}); {@code null} for any other
     * event. Verified against the lineups of real fixtures: in every substitution checked,
     * {@code player} was in the starting XI and {@code assist} among the substitutes.
     */
    public PlayerRef getPlayerOut() {
        return isSubstitution() ? player : null;
    }

    /** For a substitution, the player coming on (the API's {@code assist}); {@code null} for any other event. */
    public PlayerRef getPlayerIn() {
        return isSubstitution() ? assist : null;
    }

    /**
     * {@code true} for any VAR decision ({@code type "Var"}): {@code "Goal cancelled"},
     * {@code "Penalty confirmed"}, {@code "Card upgrade confirmed"}... A VAR event is never itself a
     * goal or a card: a confirmed penalty is followed by its own {@code "Penalty"} or
     * {@code "Missed Penalty"} event, an upgraded card by its own {@code "Red Card"} event, and a
     * cancelled goal's {@code "Goal"} event disappears from the list.
     */
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

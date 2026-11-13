package es.com.kete1987.apifootball.library.fixtures.util;

import es.com.kete1987.apifootball.library.fixtures.model.FixtureEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Helpers that need the whole list of events of a fixture to classify one of them. Most callers use
 * the equivalent methods of {@code Fixture} ({@code isSecondYellowRed}, {@code getCardEventsMerged});
 * this class works on any list, e.g. the result of {@code FixturesApi.getFixtureEvents}.
 *
 * <p><b>Second yellow card.</b> The API has no event for it: it sends a {@code "Yellow Card"} event
 * and a {@code "Red Card"} event for the same player at the same minute (recorded live in
 * Navalcarnero 2-3 Real Betis, fixture 1644363: A. Ruibal, id 47119, {@code Yellow Card} 118' then
 * {@code Red Card} 118', after an earlier yellow at {@code 90+4}). A red card is classified as a
 * <b>second yellow</b> when the list also holds a yellow card of the <b>same player at the same
 * minute</b> (same {@code elapsed}, same {@code extra}, a {@code null} extra counting as 0). A red with
 * no such yellow is a straight red (recorded live: Krejci, Czech Republic-Spain, 72', no yellow at all).
 * Notes:
 * <ul>
 *   <li>A yellow at an earlier minute does not make a later red a second yellow: Ruibal's yellow at
 *       {@code 90+4} is a plain yellow, the one at 118' is the second.</li>
 *   <li>Players are matched by id; when an id is {@code null} on either event (it happens in the first
 *       polls after an event appears) they are matched by name, ignoring case; when neither id nor name
 *       is available on both events the pair is <b>not</b> matched, so the red is reported as a
 *       straight red. The conservative answer is deliberate.</li>
 *   <li>The order of the two events is not required, only that both are in the list.</li>
 *   <li>A straight red at the very minute the same player was shown an unrelated yellow would be
 *       misread as a second yellow; the API gives no way to tell them apart and it has not been
 *       observed.</li>
 * </ul>
 */
public final class FixtureEventAnalysis {

    private FixtureEventAnalysis() {
        // helpers only
    }

    /**
     * {@code true} when {@code event} is a red card ({@link FixtureEvent#isRedCard()}) and
     * {@code events} contains a yellow card of the same player at the same minute: the red that follows
     * a second yellow. {@code false} for a straight red, for any non-red event, and when either argument
     * is {@code null}. Never throws. See the class comment for the matching rules.
     */
    public static boolean isSecondYellowRed(FixtureEvent event, List<FixtureEvent> events) {
        return event != null && events != null && event.isRedCard() && findYellowOf(event, events) != null;
    }

    /**
     * The red cards of {@code events} that are a second yellow, in the order of the list; an empty list
     * when {@code events} is {@code null}. See {@link #isSecondYellowRed(FixtureEvent, List)}.
     */
    public static List<FixtureEvent> secondYellowReds(List<FixtureEvent> events) {
        List<FixtureEvent> result = new ArrayList<>();
        if (events != null) {
            for (FixtureEvent e : events) {
                if (isSecondYellowRed(e, events)) {
                    result.add(e);
                }
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * The card events ({@link FixtureEvent#isCard()}) in the order of the list, with the yellow card
     * that belongs to a second yellow <b>removed</b>: the red stays and stands for the whole
     * "second yellow" (check it with {@link #isSecondYellowRed(FixtureEvent, List)}). That is what an
     * app that publishes one entry per dismissal wants: yellow, yellow-red and straight red are then
     * one entry each, instead of a yellow plus a red for the same moment. The earlier, separate yellow
     * of the same player stays. An empty list when {@code events} is {@code null}. Pass a list sorted
     * with {@link FixtureEvent#BY_TIME} (as {@code Fixture.getEventsSorted()} is) for a timeline.
     */
    public static List<FixtureEvent> cardEventsMerged(List<FixtureEvent> events) {
        List<FixtureEvent> result = new ArrayList<>();
        if (events != null) {
            List<FixtureEvent> absorbed = new ArrayList<>();
            for (FixtureEvent e : events) {
                if (e != null && e.isRedCard()) {
                    FixtureEvent yellow = findYellowOf(e, events);
                    if (yellow != null) {
                        absorbed.add(yellow);
                    }
                }
            }
            for (FixtureEvent e : events) {
                if (e != null && e.isCard() && !containsSame(absorbed, e)) {
                    result.add(e);
                }
            }
        }
        return Collections.unmodifiableList(result);
    }

    private static boolean containsSame(List<FixtureEvent> list, FixtureEvent e) {
        for (FixtureEvent x : list) {
            if (x == e) {
                return true;
            }
        }
        return false;
    }

    /** The yellow card of the same player at the same minute as {@code red}, or {@code null}. */
    private static FixtureEvent findYellowOf(FixtureEvent red, List<FixtureEvent> events) {
        for (FixtureEvent y : events) {
            if (y != null && y != red && y.isYellowCard() && sameMinute(y, red) && samePlayer(y, red)) {
                return y;
            }
        }
        return null;
    }

    private static boolean sameMinute(FixtureEvent a, FixtureEvent b) {
        FixtureEvent.Time ta = a.getTime();
        FixtureEvent.Time tb = b.getTime();
        if (ta == null || tb == null || ta.getElapsed() == null || tb.getElapsed() == null) {
            return false;
        }
        return ta.getElapsed().equals(tb.getElapsed()) && extra(ta) == extra(tb);
    }

    private static int extra(FixtureEvent.Time t) {
        return t.getExtra() == null ? 0 : t.getExtra();
    }

    private static boolean samePlayer(FixtureEvent a, FixtureEvent b) {
        if (a.getPlayer() == null || b.getPlayer() == null) {
            return false;
        }
        Integer ia = a.getPlayer().getId();
        Integer ib = b.getPlayer().getId();
        if (ia != null && ib != null) {
            return ia.equals(ib);
        }
        String na = a.getPlayer().getName();
        String nb = b.getPlayer().getName();
        return na != null && nb != null && !na.trim().isEmpty() && na.trim().equalsIgnoreCase(nb.trim());
    }
}

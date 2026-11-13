package es.com.kete1987.apifootball.library.fixtures.util;

/**
 * Combines the {@code elapsed} and {@code extra} members the API uses for a minute
 * ({@code fixture.status} and {@code events[].time}) into the minute to show.
 *
 * <p>The API keeps {@code elapsed} at the end of the half (45, 90, 105, 120) and counts the added
 * time in {@code extra}: a goal in the fourth minute of added time is {@code elapsed 90, extra 4},
 * which a score-board writes {@code 90+4}. Showing {@code elapsed} alone hides the added time.
 *
 * <p><b>What was observed live</b> (comparison with Sportmonks, 2026-11-12, 28 live matches):
 * <ul>
 *   <li>The API's minute runs about <b>+1 versus Sportmonks</b>: API-Football gives the minute in
 *       progress (1 when the match starts), Sportmonks the minutes elapsed (0 at the start; the
 *       second half starts at 45). With both providers in play the difference was +1 in 929 polls,
 *       0 in 306, -1 in 26. It is a convention, not a drift, and no helper here corrects it.</li>
 *   <li>Added time comes in {@code extra} (seen {@code 90+1} to {@code 90+7}), sometimes jumping
 *       ({@code 90+2} then {@code 90+4}). At half time the API keeps the last {@code extra} of the
 *       half (seen 1 and 4 at {@code HT}, 7 and 8 at {@code BT}), so a minute built from a
 *       {@code HT}/{@code BT} status shows stale added time: look at the status first.</li>
 *   <li>The <b>extra-time minute is unreliable</b>: it can start too high (97' when Sportmonks showed
 *       91', 95' against 90') and <b>go backwards</b> (100 to 94, 109 to 106, 114 to 94 in one match,
 *       and it stayed 4 minutes at 94-96 before jumping to 111). The API has no extra-time break
 *       status either ({@code ET} keeps running through the pause), and at {@code BT} {@code elapsed}
 *       is {@code null}. Do not build logic that needs a monotonic minute during {@code ET}; showing
 *       just "ET" or keeping the highest minute seen are the usual workarounds.</li>
 * </ul>
 * Every method accepts {@code null} and never throws.
 */
public final class FixtureMinutes {

    private FixtureMinutes() {
        // helpers only
    }

    /**
     * {@code elapsed + extra} ({@code extra} counted as 0 when {@code null}), or {@code null} when
     * {@code elapsed} is {@code null}: {@code (90, 4)} gives 94, {@code (45, null)} gives 45. A
     * negative {@code extra} is treated as 0.
     */
    public static Integer total(Integer elapsed, Integer extra) {
        if (elapsed == null) {
            return null;
        }
        return elapsed + (extra == null || extra < 0 ? 0 : extra);
    }

    /**
     * The minute as a score-board writes it: {@code "90+4"} when {@code extra} is positive,
     * {@code "45"} when it is {@code null} or 0; {@code null} when {@code elapsed} is {@code null}.
     */
    public static String format(Integer elapsed, Integer extra) {
        if (elapsed == null) {
            return null;
        }
        return extra == null || extra <= 0 ? String.valueOf(elapsed) : elapsed + "+" + extra;
    }
}

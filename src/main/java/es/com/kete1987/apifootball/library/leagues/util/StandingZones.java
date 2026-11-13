package es.com.kete1987.apifootball.library.leagues.util;

/**
 * Zone categories of a standings row: the part of {@code description} before {@code " - "} (see
 * {@code StandingEntry.getZoneCategory()}).
 *
 * <p>Every {@code description} seen in real responses (LaLiga 2026-27, Champions League 2025-26 and
 * 2026-27, World Cup 2026):
 * <ul>
 *   <li>{@code "Promotion - Champions League (League phase)"}</li>
 *   <li>{@code "Promotion - Europa League (League phase)"}</li>
 *   <li>{@code "Promotion - Conference League (Qualification)"}</li>
 *   <li>{@code "Promotion - Champions League (Play Offs: 1/8-finals)"}</li>
 *   <li>{@code "Promotion - Champions League (Play Offs: 1/16-finals)"}</li>
 *   <li>{@code "Relegation - LaLiga2"}</li>
 *   <li>{@code "Round of 32"} (World Cup groups and the third-placed ranking: no category)</li>
 *   <li>{@code null} (mid-table, and every row from 25th in the Champions League phase)</li>
 * </ul>
 * "Promotion" means "goes up to something" (a European competition, the next round), not only
 * promotion to a higher division. The description is free text and the API adds wording over time,
 * hence plain {@code String} constants.
 */
public final class StandingZones {

    /** {@code "Promotion"}: the row qualifies for something (European competition, next round). */
    public static final String PROMOTION = "Promotion";
    /** {@code "Relegation"}: the row goes down. */
    public static final String RELEGATION = "Relegation";

    private StandingZones() {
        // constants
    }
}

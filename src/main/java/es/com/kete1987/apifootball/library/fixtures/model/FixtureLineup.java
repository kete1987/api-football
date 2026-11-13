package es.com.kete1987.apifootball.library.fixtures.model;

import com.google.gson.annotations.SerializedName;
import es.com.kete1987.apifootball.library.common.model.TeamRef;
import es.com.kete1987.apifootball.library.common.util.ModelCollections;
import es.com.kete1987.apifootball.library.common.util.NumberParsing;
import es.com.kete1987.apifootball.library.common.util.MediaUrls;

import java.util.List;

/**
 * Lineup of one team in a fixture: formation, coach, starting XI and substitutes. Available 20-40
 * minutes before kick-off when covered.
 *
 * <h2>Lineup grid and the mirrored columns of the home team</h2>
 * Each starter has {@code grid = "row:column"}: row 1 is the goalkeeper, the following rows are the
 * lines from the back to the front (4-3-3: rows 1 to 4), and the column counts the players of that
 * row. Substitutes have no grid. <b>Neither provider gives metric coordinates</b>: a grid only says
 * "third of four players in the second line", so an app has to place the dots itself.
 *
 * <p><b>Compared with Sportmonks' {@code formation_field}, API-Football's columns of the HOME team are
 * mirrored.</b> Verified live on 2026-11-12 by matching every starter by shirt number in the 5 matches
 * that had a lineup in both providers (Turkey-Belgium, Italy-France, Czech Republic-Spain,
 * England-Croatia, Navalcarnero-Betis; ids such as 1528960 / 19676662 for Italy-France):
 * <ul>
 *   <li><b>Rows</b> are identical for both teams (55 of 55 players per side).</li>
 *   <li><b>Away team</b>: the grid is identical to {@code formation_field} (11 of 11 players in all 5
 *       matches).</li>
 *   <li><b>Home team</b>: the grid equals {@code formation_field} for only 3 of 11 players per match
 *       (the central ones), and the <b>columns are mirrored in 11 of 11 players in all 5 matches</b>.</li>
 * </ul>
 * <b>Transformation (home team only, rows untouched):</b>
 * {@code column' = playersInRow + 1 - column}, where {@code playersInRow} is the number of starters
 * of that team with the same row (the goalkeeper's row has 1, so it never moves). The same formula
 * converts in both directions (it is its own inverse). Use {@link #getGridColumnMirrored(LineupPlayer)},
 * which counts the players of the row for you, and apply it to the <b>home</b> team only (the lineup
 * does not know which side it is: compare {@code getTeam().getId()} with {@code fixture.teams.home.id},
 * or use {@code Fixture.getGridColumnAsSportmonks}). The helper counts only starters whose grid parses,
 * so an incomplete lineup (fewer than 11 starters) gives a smaller row and a wrong mirror: check
 * {@code getStartXI().size() == 11} first.
 *
 * <p>Worked example, Italy (home) in Italy-France, 4-3-3. Row 2 has four players, so
 * {@code column' = 5 - column}:
 * <pre>
 * player             API grid   mirrored   Sportmonks formation_field
 * M. Kayode          2:4        2:1        2:1
 * G. Scalvini        2:3        2:2        2:2
 * A. Bastoni         2:2        2:3        2:3
 * R. Calafiori       2:1        2:4        2:4
 * D. Frattesi        3:3        3:1        3:1     (row 3 has three players: 4 - column)
 * S. Tonali          3:2        3:2        3:2
 * N. Barella         3:1        3:3        3:3
 * S. Esposito        4:3        4:1        4:1
 * F. Esposito        4:2        4:2        4:2
 * N. Cambiaghi       4:1        4:3        4:3
 * G. Donnarumma      1:1        1:1        1:1
 * </pre>
 * France (away) in the same match needs no change. Without the correction a home XI drawn from
 * the API grid appears left-right reversed next to the away one.
 */
public class FixtureLineup {
    private LineupTeam team;
    private String formation;
    private Coach coach;
    @SerializedName("startXI")
    private List<Entry> startXi;
    private List<Entry> substitutes;

    public LineupTeam getTeam() {
        return team;
    }

    /** E.g. {@code "4-4-2"}. */
    public String getFormation() {
        return formation;
    }

    public Coach getCoach() {
        return coach;
    }

    public List<Entry> getStartXI() {
        return ModelCollections.unmodifiable(startXi);
    }

    public List<Entry> getSubstitutes() {
        return ModelCollections.unmodifiable(substitutes);
    }

    /**
     * Number of starters of this lineup whose grid row is {@code row} (the players of that line);
     * 0 for an unknown row or when there is no starting XI. Never throws.
     */
    public int getPlayersInGridRow(int row) {
        int count = 0;
        if (startXi != null) {
            for (Entry e : startXi) {
                if (e != null && e.getPlayer() != null && Integer.valueOf(row).equals(e.getPlayer().getGridRow())) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * The grid column of {@code player} mirrored inside its row: {@code playersInRow + 1 - column}
     * (see the class comment). This turns API-Football's <b>home</b> columns into Sportmonks'
     * {@code formation_field} columns, and back; apply it to the home team only. {@code playersInRow}
     * is {@link #getPlayersInGridRow(int)} of the player's row in this lineup. Returns {@code null} when
     * {@code player} is {@code null}, has no parseable grid, or the row is not found in this lineup
     * (e.g. the player belongs to another lineup) or the column is larger than the row. Never throws.
     *
     * <p>Example from a real lineup: row 2 of Italy has 4 players and Kayode has {@code 2:4}, so his
     * mirrored column is {@code 4 + 1 - 4 = 1}.
     */
    public Integer getGridColumnMirrored(LineupPlayer player) {
        if (player == null) {
            return null;
        }
        Integer row = player.getGridRow();
        Integer column = player.getGridColumn();
        if (row == null || column == null) {
            return null;
        }
        int inRow = getPlayersInGridRow(row);
        return inRow == 0 || column > inRow ? null : inRow + 1 - column;
    }

    /** Team of a lineup, with its kit colours. */
    public static class LineupTeam extends TeamRef {
        private Colors colors;

        /** Kit colours, or {@code null} when the API does not send them (the usual case in real responses). */
        public Colors getColors() {
            return colors;
        }
    }

    /** Kit colours of the outfield players and of the goalkeeper. */
    public static class Colors {
        private KitColors player;
        private KitColors goalkeeper;

        public KitColors getPlayer() {
            return player;
        }

        public KitColors getGoalkeeper() {
            return goalkeeper;
        }
    }

    /** Colours of one kit, as hex strings without {@code #} (e.g. {@code "ff0000"}). */
    public static class KitColors {
        private String primary;
        private String number;
        private String border;

        /** Shirt colour. */
        public String getPrimary() {
            return primary;
        }

        /** Colour of the shirt number. */
        public String getNumber() {
            return number;
        }

        /** Border colour. */
        public String getBorder() {
            return border;
        }
    }

    /** Coach; members may be {@code null}. */
    public static class Coach {
        private Integer id;
        private String name;
        private String photo;

        public Integer getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getPhoto() {
            return photo;
        }
    }

    /** Wrapper the API puts around each lineup player. */
    public static class Entry {
        private LineupPlayer player;

        public LineupPlayer getPlayer() {
            return player;
        }
    }

    /** Player of a lineup. */
    public static class LineupPlayer {
        private Integer id;
        private String name;
        private Integer number;
        private String pos;
        private String grid;

        public Integer getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public Integer getNumber() {
            return number;
        }

        /** {@code G}, {@code D}, {@code M} or {@code F}; may be {@code null} for substitutes. */
        public String getPos() {
            return pos;
        }

        /**
         * Pitch position {@code "row:column"} (row 1 = goalkeeper); {@code null} for substitutes. Prefer
         * {@link #getGridRow()} and {@link #getGridColumn()}.
         *
         * <p><b>The home team's columns are mirrored; read {@link FixtureLineup} before drawing a pitch.</b>
         */
        public String getGrid() {
            return grid;
        }

        /**
         * Row of {@link #getGrid()}: 1 = goalkeeper, then the lines from the back to the front (a 4-3-3 has
         * rows 1 to 4). {@code null} when the grid is {@code null}, blank or not {@code "int:int"}, or the
         * number is below 1. Never throws. Rows agree with Sportmonks' {@code formation_field} for both
         * teams (verified live).
         */
        public Integer getGridRow() {
            return parseGrid(grid, 0);
        }

        /**
         * Column of {@link #getGrid()} exactly as the API sends it, 1 = leftmost in the API's own
         * orientation; {@code null} for the same cases as {@link #getGridRow()}. <b>For the HOME team this
         * is mirrored compared with Sportmonks</b>; to compare or merge the two providers use
         * {@link FixtureLineup#getGridColumnMirrored(LineupPlayer)}. See the {@link FixtureLineup} comment.
         */
        public Integer getGridColumn() {
            return parseGrid(grid, 1);
        }

        private static Integer parseGrid(String grid, int index) {
            if (grid == null) {
                return null;
            }
            String[] parts = grid.trim().split(":", -1);
            if (parts.length != 2) {
                return null;
            }
            Integer row = NumberParsing.parseInteger(parts[0].trim());
            Integer column = NumberParsing.parseInteger(parts[1].trim());
            if (row == null || column == null || row < 1 || column < 1) {
                return null;
            }
            return index == 0 ? row : column;
        }

        /**
         * Photo URL built from the id ({@code https://media.api-sports.io/football/players/{id}.png},
         * catalog section 7); lineups carry the id but no photo. {@code null} without an id. Images do not
         * count against the quota; see {@link MediaUrls}.
         */
        public String getPhotoUrl() {
            return MediaUrls.playerPhoto(id);
        }
    }
}

package es.com.kete1987.apifootball.library.fixtures.model;

import com.google.gson.annotations.SerializedName;
import es.com.kete1987.apifootball.library.common.model.TeamRef;
import es.com.kete1987.apifootball.library.common.util.ModelCollections;

import java.util.List;

/**
 * Lineup of one team in a fixture: formation, coach, starting XI and substitutes. Available 20-40
 * minutes before kick-off when covered.
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

        /** Pitch position {@code "line:column"} (line 1 = goalkeeper); {@code null} for substitutes. */
        public String getGrid() {
            return grid;
        }
    }
}

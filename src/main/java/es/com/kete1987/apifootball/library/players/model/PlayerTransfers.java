package es.com.kete1987.apifootball.library.players.model;

import com.google.gson.annotations.SerializedName;
import es.com.kete1987.apifootball.library.common.model.TeamRef;
import es.com.kete1987.apifootball.library.common.util.ModelCollections;

import java.util.List;

/** One element of {@code /transfers}: a player and his transfer history. */
public class PlayerTransfers {
    private PlayerRef player;
    private String update;
    private List<Transfer> transfers;

    public PlayerRef getPlayer() {
        return player;
    }

    /** Last update, ISO datetime. */
    public String getUpdate() {
        return update;
    }

    public List<Transfer> getTransfers() {
        return ModelCollections.unmodifiable(transfers);
    }

    public static class PlayerRef {
        private Integer id;
        private String name;

        public Integer getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }

    public static class Transfer {
        private String date;
        private String type;
        private Teams teams;

        /** Transfer date, {@code YYYY-MM-DD}. */
        public String getDate() {
            return date;
        }

        /** Free text: {@code "Free"}, {@code "Loan"}, {@code "N/A"}, {@code "€ 180M"}...; may be null. */
        public String getType() {
            return type;
        }

        public Teams getTeams() {
            return teams;
        }
    }

    public static class Teams {
        @SerializedName("in")
        private TeamRef in;
        @SerializedName("out")
        private TeamRef out;

        /** Team the player joined. */
        public TeamRef getIn() {
            return in;
        }

        /** Team the player left. */
        public TeamRef getOut() {
            return out;
        }
    }
}

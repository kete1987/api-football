package es.com.kete1987.apifootball.library.players.model;

import es.com.kete1987.apifootball.library.common.util.ModelCollections;

import java.util.List;

/** One element of {@code /players/squads}: a team and its squad. */
public class Squad {
    private TeamRef team;
    private List<SquadPlayer> players;

    public TeamRef getTeam() {
        return team;
    }

    public List<SquadPlayer> getPlayers() {
        return ModelCollections.unmodifiable(players);
    }

    /** Squad member. */
    public static class SquadPlayer {
        private Integer id;
        private String name;
        private Integer age;
        private Integer number;
        private String position;
        private String photo;

        public Integer getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public Integer getAge() {
            return age;
        }

        /** Shirt number; may be null. */
        public Integer getNumber() {
            return number;
        }

        public String getPosition() {
            return position;
        }

        public String getPhoto() {
            return photo;
        }
    }
}

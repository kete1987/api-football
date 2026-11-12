package es.com.kete1987.apifootball.library.teams.model;

import es.com.kete1987.apifootball.library.common.model.TeamRef;

/**
 * Statistics of one team in one league and season ({@code /teams/statistics}): the
 * {@link SeasonStatistics} plus the league and team they refer to.
 */
public class TeamStatistics extends SeasonStatistics {
    private LeagueRef league;
    private TeamRef team;

    public LeagueRef getLeague() {
        return league;
    }

    public TeamRef getTeam() {
        return team;
    }

    /** League the statistics refer to. */
    public static class LeagueRef {
        private Integer id;
        private String name;
        private String country;
        private String logo;
        private String flag;
        private Integer season;

        public Integer getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getCountry() {
            return country;
        }

        public String getLogo() {
            return logo;
        }

        /** Country flag; {@code null} for "World". */
        public String getFlag() {
            return flag;
        }

        public Integer getSeason() {
            return season;
        }
    }
}

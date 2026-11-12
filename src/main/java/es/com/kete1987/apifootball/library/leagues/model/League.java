package es.com.kete1987.apifootball.library.leagues.model;

import es.com.kete1987.apifootball.library.common.util.ModelCollections;

import java.util.List;

/** A league or cup with its country and its seasons (each with its data coverage). */
public class League {
    private LeagueInfo league;
    private Country country;
    private List<Season> seasons;

    public LeagueInfo getLeague() {
        return league;
    }

    public Country getCountry() {
        return country;
    }

    /** Seasons of the competition; only the current one when queried with {@code current=true}. */
    public List<Season> getSeasons() {
        return ModelCollections.unmodifiable(seasons);
    }

    /** The season flagged as current, or {@code null} when none is. */
    public Season getCurrentSeason() {
        if (seasons == null) return null;
        for (Season season : seasons) {
            if (Boolean.TRUE.equals(season.getCurrent())) return season;
        }
        return null;
    }

    /** The season with the given year, or {@code null} when the competition has no such season. */
    public Season getSeason(int year) {
        if (seasons == null) return null;
        for (Season season : seasons) {
            if (season.getYear() != null && season.getYear() == year) return season;
        }
        return null;
    }
}

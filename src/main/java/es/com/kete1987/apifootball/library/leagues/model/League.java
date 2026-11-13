package es.com.kete1987.apifootball.library.leagues.model;

import es.com.kete1987.apifootball.library.common.util.ModelCollections;

import java.time.LocalDate;
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

    /**
     * The season flagged as current, or {@code null} when none is. The flag stays on a season after
     * it ends (the World Cup 2026 in November 2026): see {@link #getSeasonInProgress(LocalDate)}.
     */
    public Season getCurrentSeason() {
        if (seasons == null) return null;
        for (Season season : seasons) {
            if (Boolean.TRUE.equals(season.getCurrent())) return season;
        }
        return null;
    }

    /**
     * The season whose dates include {@code date} (see {@link Season#isInProgress(LocalDate)}), or
     * {@code null} when none does: between two seasons, after the last one, or when {@code date} is
     * {@code null}. Unlike {@link #getCurrentSeason()}, this is {@code null} for the World Cup once
     * its final is played. Never throws.
     */
    public Season getSeasonInProgress(LocalDate date) {
        if (seasons == null || date == null) return null;
        for (Season season : seasons) {
            if (season != null && season.isInProgress(date)) return season;
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

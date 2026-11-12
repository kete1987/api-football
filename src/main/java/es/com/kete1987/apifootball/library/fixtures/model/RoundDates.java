package es.com.kete1987.apifootball.library.fixtures.model;

import es.com.kete1987.apifootball.library.common.util.ModelCollections;

import java.util.List;

/** A round with its match dates, returned by {@code fixtures/rounds?dates=true}. */
public class RoundDates {
    private String round;
    private List<String> dates;

    /** Round label, usable as the {@code round} filter of {@code /fixtures}. */
    public String getRound() {
        return round;
    }

    /** Dates in {@code YYYY-MM-DD} form. */
    public List<String> getDates() {
        return ModelCollections.unmodifiable(dates);
    }
}

package es.com.kete1987.apifootball.library.odds.model;

import es.com.kete1987.apifootball.library.common.util.ModelCollections;

import java.util.List;

/** An in-play bet with its selections. The id belongs to the <b>in-play</b> id space ({@code /odds/live/bets}). */
public class LiveBetOdds {
    private Integer id;
    private String name;
    private List<LiveOddValue> values;

    /** In-play bet id (see {@code LiveBetIds}); not valid for pre-match odds. */
    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<LiveOddValue> getValues() {
        return ModelCollections.unmodifiable(values);
    }
}

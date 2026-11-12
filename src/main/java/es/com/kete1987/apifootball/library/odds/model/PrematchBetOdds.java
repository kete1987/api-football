package es.com.kete1987.apifootball.library.odds.model;

import es.com.kete1987.apifootball.library.common.util.ModelCollections;

import java.util.List;

/** A pre-match bet with its selections. The id belongs to the <b>pre-match</b> id space ({@code /odds/bets}). */
public class PrematchBetOdds {
    private Integer id;
    private String name;
    private List<OddValue> values;

    /** Pre-match bet id (see {@code PrematchBetIds}); not valid for in-play odds. */
    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<OddValue> getValues() {
        return ModelCollections.unmodifiable(values);
    }
}

package es.com.kete1987.apifootball.library.fixtures.model;

/** A home/away pair of goals; used for the fixture goals and every phase of the score. Members are {@code null} until known. */
public class Goals {
    private Integer home;
    private Integer away;

    public Integer getHome() {
        return home;
    }

    public Integer getAway() {
        return away;
    }
}

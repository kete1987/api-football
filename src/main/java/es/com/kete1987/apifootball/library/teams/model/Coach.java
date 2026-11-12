package es.com.kete1987.apifootball.library.teams.model;

import es.com.kete1987.apifootball.library.common.util.ModelCollections;

import java.util.List;

/** A coach with his current team and career ({@code /coachs} response item). */
public class Coach {
    private Integer id;
    private String name;
    private String firstname;
    private String lastname;
    private Integer age;
    private Birth birth;
    private String nationality;
    private String height;
    private String weight;
    private String photo;
    private TeamRef team;
    private List<Career> career;

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getFirstname() {
        return firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public Integer getAge() {
        return age;
    }

    public Birth getBirth() {
        return birth;
    }

    public String getNationality() {
        return nationality;
    }

    /** Text such as {@code "185 cm"}; may be {@code null}. */
    public String getHeight() {
        return height;
    }

    /** Text such as {@code "78 kg"}; may be {@code null}. */
    public String getWeight() {
        return weight;
    }

    public String getPhoto() {
        return photo;
    }

    /** Current team. */
    public TeamRef getTeam() {
        return team;
    }

    public List<Career> getCareer() {
        return ModelCollections.unmodifiable(career);
    }

    /** Birth data; every field may be {@code null}. */
    public static class Birth {
        private String date;
        private String place;
        private String country;

        /** {@code YYYY-MM-DD}. */
        public String getDate() {
            return date;
        }

        public String getPlace() {
            return place;
        }

        public String getCountry() {
            return country;
        }
    }

    /** One spell of the career. */
    public static class Career {
        private TeamRef team;
        private String start;
        private String end;

        public TeamRef getTeam() {
            return team;
        }

        /** {@code YYYY-MM-DD}. */
        public String getStart() {
            return start;
        }

        /** {@code YYYY-MM-DD}; {@code null} for the current spell. */
        public String getEnd() {
            return end;
        }
    }
}

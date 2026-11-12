package es.com.kete1987.apifootball.library.players.model;

import es.com.kete1987.apifootball.library.common.util.NumberParsing;

/**
 * Player profile, shared by {@code /players} (which fills {@code injured}) and
 * {@code /players/profiles} (which fills {@code number} and {@code position}). Fields an endpoint
 * does not send are null.
 */
public class Player {
    private Integer id;
    private String name;
    private String firstname;
    private String lastname;
    private Integer age;
    private Birth birth;
    private String nationality;
    private String height;
    private String weight;
    private Integer number;
    private String position;
    private Boolean injured;
    private String photo;

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

    /** Height as sent by the API: {@code "187"}, {@code "185 cm"} or null (format is inconsistent). */
    public String getHeight() {
        return height;
    }

    /**
     * Height in centimetres from {@link #getHeight()} ({@code "187"} or {@code "185 cm"}); null when
     * absent or not a whole number. Never throws.
     */
    public Integer getHeightCm() {
        return NumberParsing.parseLeadingInteger(height);
    }

    /** Weight as sent by the API: {@code "85"}, {@code "82 kg"} or null. */
    public String getWeight() {
        return weight;
    }

    /**
     * Weight in kilograms from {@link #getWeight()} ({@code "85"} or {@code "82 kg"}); null when
     * absent or not a whole number. Never throws.
     */
    public Integer getWeightKg() {
        return NumberParsing.parseLeadingInteger(weight);
    }

    /** Shirt number; only sent by {@code /players/profiles}, may be null. */
    public Integer getNumber() {
        return number;
    }

    /** Position (see {@code PlayerPositions}); only sent by {@code /players/profiles}. */
    public String getPosition() {
        return position;
    }

    /** Injury flag; only sent by {@code /players}. */
    public Boolean getInjured() {
        return injured;
    }

    public String getPhoto() {
        return photo;
    }

    /** Birth data; every member may be null. */
    public static class Birth {
        private String date;
        private String place;
        private String country;

        /** Birth date, {@code YYYY-MM-DD}. */
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
}

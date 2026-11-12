package es.com.kete1987.apifootball.library.account.model;

import com.google.gson.annotations.SerializedName;

/** Response of the {@code status} endpoint: account, subscription and daily usage. */
public class Status {
    private Account account;
    private Subscription subscription;
    private Requests requests;

    public Account getAccount() {
        return account;
    }

    public Subscription getSubscription() {
        return subscription;
    }

    public Requests getRequests() {
        return requests;
    }

    /** Owner of the API key. */
    public static class Account {
        private String firstname;
        private String lastname;
        private String email;

        public String getFirstname() {
            return firstname;
        }

        public String getLastname() {
            return lastname;
        }

        public String getEmail() {
            return email;
        }
    }

    /** Subscription the API key belongs to. */
    public static class Subscription {
        private String plan;
        private String end;
        private Boolean active;

        public String getPlan() {
            return plan;
        }

        /** End of the subscription as an ISO-8601 date-time, e.g. {@code 2027-11-12T07:46:56+00:00}. */
        public String getEnd() {
            return end;
        }

        public Boolean getActive() {
            return active;
        }
    }

    /** Requests consumed today against the daily quota. */
    public static class Requests {
        private Integer current;
        @SerializedName("limit_day")
        private Integer limitDay;

        public Integer getCurrent() {
            return current;
        }

        public Integer getLimitDay() {
            return limitDay;
        }
    }
}

package es.com.kete1987.apifootball.library.common.model;

/**
 * Immutable snapshot of the rate-limit headers of one response.
 *
 * <p>API-Football applies two independent limits: a daily quota that depends on the subscription
 * ({@code x-ratelimit-requests-*} headers) and a per-minute throttle ({@code X-RateLimit-*}
 * headers). Any value is {@code null} when the response did not carry the header.
 */
public final class RateLimit {
    private final Integer dailyLimit;
    private final Integer dailyRemaining;
    private final Integer minuteLimit;
    private final Integer minuteRemaining;

    public RateLimit(Integer dailyLimit, Integer dailyRemaining, Integer minuteLimit, Integer minuteRemaining) {
        this.dailyLimit = dailyLimit;
        this.dailyRemaining = dailyRemaining;
        this.minuteLimit = minuteLimit;
        this.minuteRemaining = minuteRemaining;
    }

    /** {@code x-ratelimit-requests-limit}: requests allocated per day by the subscription. */
    public Integer getDailyLimit() {
        return dailyLimit;
    }

    /** {@code x-ratelimit-requests-remaining}: requests left for the current day. */
    public Integer getDailyRemaining() {
        return dailyRemaining;
    }

    /** {@code X-RateLimit-Limit}: maximum number of calls per minute. */
    public Integer getMinuteLimit() {
        return minuteLimit;
    }

    /** {@code X-RateLimit-Remaining}: calls left in the current minute. */
    public Integer getMinuteRemaining() {
        return minuteRemaining;
    }

    @Override
    public String toString() {
        return "RateLimit{daily=" + dailyRemaining + "/" + dailyLimit
                + ", minute=" + minuteRemaining + "/" + minuteLimit + "}";
    }
}

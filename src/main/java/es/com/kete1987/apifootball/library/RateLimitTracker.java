package es.com.kete1987.apifootball.library;

import es.com.kete1987.apifootball.library.common.model.RateLimit;
import okhttp3.Response;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Shared, thread-safe holder for the rate-limit state observed across requests. One tracker is
 * shared by every sub-API of an {@link ApiFootball} instance, so the last response wins whatever
 * sub-API made it.
 */
class RateLimitTracker {
    static final String DAILY_LIMIT = "x-ratelimit-requests-limit";
    static final String DAILY_REMAINING = "x-ratelimit-requests-remaining";
    static final String MINUTE_LIMIT = "X-RateLimit-Limit";
    static final String MINUTE_REMAINING = "X-RateLimit-Remaining";

    private final AtomicReference<RateLimit> last = new AtomicReference<>();
    private final AtomicLong requestCount = new AtomicLong();

    void track(Response response) {
        requestCount.incrementAndGet();
        Integer dailyLimit = intHeader(response, DAILY_LIMIT);
        Integer dailyRemaining = intHeader(response, DAILY_REMAINING);
        Integer minuteLimit = intHeader(response, MINUTE_LIMIT);
        Integer minuteRemaining = intHeader(response, MINUTE_REMAINING);
        if (dailyLimit == null && dailyRemaining == null && minuteLimit == null && minuteRemaining == null) {
            return; // keep the previous snapshot rather than wiping it with an empty one
        }
        last.set(new RateLimit(dailyLimit, dailyRemaining, minuteLimit, minuteRemaining));
    }

    RateLimit last() {
        return last.get();
    }

    long requestCount() {
        return requestCount.get();
    }

    private static Integer intHeader(Response response, String name) {
        String value = response.header(name);
        if (value == null) return null;
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

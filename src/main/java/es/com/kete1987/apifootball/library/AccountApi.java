package es.com.kete1987.apifootball.library;

import es.com.kete1987.apifootball.library.account.model.Status;
import es.com.kete1987.apifootball.library.common.util.ApiFootballException;
import es.com.kete1987.apifootball.library.common.util.Constants;
import okhttp3.OkHttpClient;

import java.io.IOException;

/**
 * Account sub-API: subscription status and daily usage.
 *
 * <p>Can be used standalone ({@code new AccountApi("apiKey")}) or via the {@link ApiFootball}
 * facade ({@code api.account()}).
 */
public class AccountApi extends ApiFootballBase {

    public AccountApi(String apiKey) {
        this(buildHttpClient(apiKey), Constants.BASE_URL, new RateLimitTracker());
    }

    AccountApi(OkHttpClient client, String baseUrl, RateLimitTracker tracker) {
        super(client, baseUrl, tracker);
    }

    /**
     * Returns the account, subscription and requests consumed today.
     * This call does not count against the daily quota.
     */
    public Status getStatus() throws IOException, ApiFootballException {
        return object(url("status"), Status.class);
    }
}

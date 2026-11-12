package es.com.kete1987.apifootball.library;

import es.com.kete1987.apifootball.library.account.model.Status;
import es.com.kete1987.apifootball.library.common.model.Page;
import es.com.kete1987.apifootball.library.common.model.RateLimit;
import es.com.kete1987.apifootball.library.common.query.Query;
import es.com.kete1987.apifootball.library.common.util.ApiFootballException;
import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Exercises the shared plumbing in {@link ApiFootballBase} with inline bodies. */
class CoreBehaviourTest extends BaseApiTest {

    private AccountApi api;

    static class Item {
        int id;
    }

    static class ItemQuery extends Query<ItemQuery> {
        ItemQuery league(int league) {
            return param("league", league);
        }

        ItemQuery search(String search) {
            return param("search", search);
        }
    }

    private static String envelope(String errors, int current, int total, String response) {
        return "{\"get\":\"x\",\"parameters\":[],\"errors\":" + errors + ",\"results\":1,"
                + "\"paging\":{\"current\":" + current + ",\"total\":" + total + "},\"response\":" + response + "}";
    }

    @BeforeEach
    void setUp() {
        api = new AccountApi(client, baseUrl, tracker);
    }

    @Test
    void rateLimitHeaders_areExposedAfterARequest() throws Exception {
        assertNull(api.getRateLimit());
        enqueueBody(envelope("[]", 1, 1, "{\"requests\":{\"current\":12,\"limit_day\":7500}}"), 200);

        Status status = api.getStatus();

        assertEquals(12, status.getRequests().getCurrent());
        assertEquals(7500, status.getRequests().getLimitDay());
        RateLimit limit = api.getRateLimit();
        assertEquals(7500, limit.getDailyLimit());
        assertEquals(7499, limit.getDailyRemaining());
        assertEquals(300, limit.getMinuteLimit());
        assertEquals(299, limit.getMinuteRemaining());
        assertEquals(7499, api.getDailyRequestsRemaining());
        assertEquals(299, api.getMinuteRequestsRemaining());
        assertEquals(1, api.getRequestCount());
        assertEquals("/status", takeRequest().getPath());
    }

    @Test
    void responseWithoutHeaders_keepsPreviousSnapshot() throws Exception {
        enqueueBody(envelope("[]", 1, 1, "[]"), 200);
        server.enqueue(new MockResponse().setBody(envelope("[]", 1, 1, "[]")));

        api.getStatus();
        api.getStatus();

        assertEquals(7499, api.getDailyRequestsRemaining());
        assertEquals(2, api.getRequestCount());
    }

    @Test
    void errorsObjectWithHttp200_throwsWithParsedErrors() {
        enqueueBody(envelope("{\"season\":\"The Season field is required.\"}", 1, 1, "[]"), 200);

        ApiFootballException e = assertThrows(ApiFootballException.class, () -> api.getStatus());

        assertEquals(200, e.getHttpCode());
        assertEquals("The Season field is required.", e.getErrors().get("season"));
        assertTrue(e.getMessage().contains("season: The Season field is required."));
        // rate limits are tracked even when the request fails
        assertEquals(7499, api.getDailyRequestsRemaining());
    }

    @Test
    void errorsAsNonEmptyArray_throws() {
        enqueueBody(envelope("[\"Something went wrong\"]", 1, 1, "[]"), 200);

        ApiFootballException e = assertThrows(ApiFootballException.class, () -> api.getStatus());

        assertEquals("Something went wrong", e.getErrors().get("0"));
    }

    @Test
    void non2xx_throwsWithStatusCode() {
        enqueueBody("{\"message\":\"Too many requests\"}", 429);

        ApiFootballException e = assertThrows(ApiFootballException.class, () -> api.getStatus());

        assertEquals(429, e.getHttpCode());
        assertEquals("Too many requests", e.getErrors().get("message"));
        assertTrue(e.getMessage().startsWith("429"));
    }

    @Test
    void nonJsonBody_throws() {
        enqueueBody("<html>gateway</html>", 200);

        assertThrows(ApiFootballException.class, () -> api.getStatus());
    }

    @Test
    void object_returnsNullOnEmptyArray() throws Exception {
        enqueueBody(envelope("[]", 1, 1, "[]"), 200);

        assertNull(api.getStatus());
    }

    @Test
    void list_parsesArrayAndBuildsQueryUrl() throws Exception {
        enqueueBody(envelope("[]", 1, 1, "[{\"id\":1},{\"id\":2}]"), 200);

        List<Item> items = api.list(api.url("items", new ItemQuery().league(140).search("real madrid")), Item.class);

        assertEquals(2, items.size());
        assertEquals(2, items.get(1).id);
        assertEquals("/items?league=140&search=real%20madrid", takeRequest().getPath());
    }

    @Test
    void first_returnsNullWhenEmpty() throws Exception {
        enqueueBody(envelope("[]", 1, 1, "[]"), 200);

        assertNull(api.first(api.url("items"), Item.class));
    }

    @Test
    void page_requestsExactlyThatPage() throws Exception {
        enqueueBody(envelope("[]", 2, 3, "[{\"id\":21}]"), 200);

        Page<Item> page = api.page(api.url("items"), Item.class, 2);

        assertEquals(1, page.getItems().size());
        assertEquals(2, page.getPaging().getCurrent());
        assertEquals(3, page.getPaging().getTotal());
        assertTrue(page.hasMore());
        assertEquals("/items?page=2", takeRequest().getPath());
    }

    @Test
    void allPages_walksEveryPage() throws Exception {
        enqueueBody(envelope("[]", 1, 3, "[{\"id\":1}]"), 200);
        enqueueBody(envelope("[]", 2, 3, "[{\"id\":2}]"), 200);
        enqueueBody(envelope("[]", 3, 3, "[{\"id\":3}]"), 200);

        List<Item> items = api.allPages(api.url("items"), Item.class, 10);

        assertEquals(3, items.size());
        assertEquals(3, api.getRequestCount());
        assertEquals("/items?page=1", takeRequest().getPath());
        assertEquals("/items?page=2", takeRequest().getPath());
        assertEquals("/items?page=3", takeRequest().getPath());
    }

    @Test
    void allPages_stopsAtMaxPages() throws IOException, ApiFootballException {
        enqueueBody(envelope("[]", 1, 40, "[{\"id\":1}]"), 200);
        enqueueBody(envelope("[]", 2, 40, "[{\"id\":2}]"), 200);

        List<Item> items = api.allPages(api.url("items"), Item.class, 2);

        assertEquals(2, items.size());
        assertEquals(2, api.getRequestCount());
    }

    @Test
    void allPages_rejectsNonPositiveCap() {
        assertThrows(IllegalArgumentException.class, () -> api.allPages(api.url("items"), Item.class, 0));
        assertEquals(0, api.getRequestCount());
    }

    @Test
    void allPages_stopsOnEmptyPage() throws IOException, ApiFootballException {
        enqueueBody(envelope("[]", 1, 5, "[{\"id\":1}]"), 200);
        enqueueBody(envelope("[]", 2, 5, "[]"), 200);

        List<Item> items = api.allPages(api.url("items"), Item.class, 5);

        assertEquals(1, items.size());
        assertEquals(2, api.getRequestCount());
    }

    @Test
    void query_nullValueRemovesParameter() {
        ItemQuery query = new ItemQuery().league(1).search("x").param("search", null);

        assertFalse(query.toParams().containsKey("search"));
        assertEquals("1", query.toParams().get("league"));
    }
}

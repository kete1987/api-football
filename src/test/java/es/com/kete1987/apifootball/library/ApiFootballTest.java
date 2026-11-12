package es.com.kete1987.apifootball.library;

import es.com.kete1987.apifootball.library.account.model.Status;
import es.com.kete1987.apifootball.library.fixtures.model.Fixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/** The facade wires every sub-API to one shared client and one shared rate-limit tracker. */
class ApiFootballTest extends BaseApiTest {

    private ApiFootball api;

    @BeforeEach
    void setUp() {
        api = new ApiFootball(client, baseUrl);
    }

    @Test
    void exposesEverySubApi() {
        assertNotNull(api.account());
        assertNotNull(api.leagues());
        assertNotNull(api.teams());
        assertNotNull(api.fixtures());
        assertNotNull(api.players());
        assertNotNull(api.odds());
    }

    @Test
    void rateLimitIsNullBeforeAnyRequest() {
        assertNull(api.getRateLimit());
        assertNull(api.getDailyRequestsRemaining());
        assertEquals(0, api.getRequestCount());
    }

    @Test
    void getStatus_parsesFixture() throws Exception {
        enqueue("status.json");

        Status status = api.getStatus();

        assertEquals("Pro", status.getSubscription().getPlan());
        assertEquals(Boolean.TRUE, status.getSubscription().getActive());
        assertEquals(7500, status.getRequests().getLimitDay());
        assertEquals("John", status.getAccount().getFirstname());
        assertEquals("/status", takeRequest().getPath());
    }

    @Test
    void rateLimitAndRequestCount_areSharedAcrossSubApis() throws Exception {
        enqueue("fixtures_by_id.json");
        enqueue("status.json");

        Fixture fixture = api.fixtures().getFixtureById(1570443);
        api.account().getStatus();

        assertEquals(1570443, fixture.getFixture().getId());
        assertEquals(2, api.getRequestCount());
        assertEquals(2, api.fixtures().getRequestCount());
        assertEquals(7500, api.getDailyRequestsLimit());
        assertEquals(7499, api.getDailyRequestsRemaining());
        assertEquals(300, api.getMinuteRequestsLimit());
        assertEquals(299, api.getMinuteRequestsRemaining());
        assertEquals(api.getRateLimit(), api.odds().getRateLimit());
    }
}

package es.com.kete1987.apifootball.library;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Base of every unit test: starts a {@link MockWebServer} so no test touches the network.
 *
 * <p>Subclasses build the sub-API under test with the package-private constructor, e.g.
 * {@code new FixturesApi(client, baseUrl, tracker)}, enqueue a fixture from
 * {@code src/test/resources/fixtures/} and assert on both the parsed result and, through
 * {@link #takeRequest()}, the URL the library built.
 */
public abstract class BaseApiTest {

    protected MockWebServer server;
    protected OkHttpClient client;
    protected String baseUrl;
    protected RateLimitTracker tracker;

    @BeforeEach
    void setUpServer() throws IOException {
        server = new MockWebServer();
        server.start();
        baseUrl = server.url("/").toString();
        client = new OkHttpClient();
        tracker = new RateLimitTracker();
    }

    @AfterEach
    void tearDownServer() throws IOException {
        server.shutdown();
    }

    /** Enqueues a fixture file as a 200 response carrying the four rate-limit headers. */
    protected void enqueue(String fixture) throws IOException {
        enqueueBody(load(fixture), 200);
    }

    protected void enqueue(String fixture, int code) throws IOException {
        enqueueBody(load(fixture), code);
    }

    /** Enqueues a literal JSON body. */
    protected void enqueueBody(String body, int code) {
        server.enqueue(new MockResponse()
                .setResponseCode(code)
                .setBody(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("x-ratelimit-requests-limit", "7500")
                .addHeader("x-ratelimit-requests-remaining", "7499")
                .addHeader("X-RateLimit-Limit", "300")
                .addHeader("X-RateLimit-Remaining", "299"));
    }

    /** Returns the next request the library sent to the mock server. */
    protected RecordedRequest takeRequest() throws InterruptedException {
        return server.takeRequest();
    }

    protected String load(String name) throws IOException {
        String path = "fixtures/" + name;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null) throw new IOException("Test fixture not found: " + path);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int n;
            while ((n = is.read(buffer)) != -1) out.write(buffer, 0, n);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}

package com.cbdoctor.core.collector.rest;

import com.fasterxml.jackson.databind.JsonNode;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class MgmtRestClientTest {

    private MockWebServer server;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    private MgmtRestClient client() {
        String baseUrl = server.url("/").toString();
        return MgmtRestClient.fromConnectionString(
                baseUrl,
                "admin",
                "secret".toCharArray()
        );
    }

    @Test
    void getPoolsDefault_shouldReturnJson_when200() throws Exception {
        server.enqueue(new MockResponse.Builder()
                .code(200)
                .addHeader("Content-Type", "application/json")
                .body("""
                    {
                      "clusterName": "test-cluster",
                      "nodes": []
                    }
                """)
                .build());

        JsonNode json = client().getPoolsDefault();
        assertEquals("test-cluster", json.get("clusterName").asText());

        var request = server.takeRequest();
        assertEquals("/pools/default", request.getTarget()); // <-- OkHttp5 target :contentReference[oaicite:3]{index=3}

        String auth = request.getHeaders().get("Authorization"); // Headers object :contentReference[oaicite:4]{index=4}
        assertNotNull(auth);
        assertTrue(auth.startsWith("Basic "));
    }

    @Test
    void shouldThrowAuthException_on401() {
        server.enqueue(new MockResponse.Builder().code(401).build());
        assertThrows(MgmtAuthException.class, () -> client().getPoolsDefault());
    }

    @Test
    void shouldThrowAuthException_on403() {
        server.enqueue(new MockResponse.Builder().code(403).build());
        assertThrows(MgmtAuthException.class, () -> client().getBuckets());
    }

    @Test
    void shouldThrowRestException_on500() {
        server.enqueue(new MockResponse.Builder().code(500).body("boom").build());
        MgmtRestException ex = assertThrows(MgmtRestException.class, () -> client().getPoolsDefault());
        assertTrue(ex.getMessage().contains("HTTP 500"));
    }

    @Test
    void shouldResolveCouchbaseConnectionString() {
        var url = MgmtEndpointResolver.resolve("couchbase://localhost");
        assertEquals("http", url.scheme());
        assertEquals(8091, url.port());
    }

    @Test
    void shouldResolveSecureCouchbaseConnectionString() {
        var url = MgmtEndpointResolver.resolve("couchbases://localhost");
        assertEquals("https", url.scheme());
        assertEquals(18091, url.port());
    }
}

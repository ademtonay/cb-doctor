package com.cbdoctor.core.collector.rest;

import com.cbdoctor.core.collector.BucketInfo;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MgmtBucketsFetcher using MockWebServer.
 */
class MgmtBucketsFetcherTest {

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

    @Test
    void fetch_shouldParseNameAndReplicaNumber() {
        server.enqueue(new MockResponse.Builder()
                .code(200)
                .addHeader("Content-Type", "application/json")
                .body("""
                    [
                      { "name": "orders", "replicaNumber": 1, "healthStats": { "healthy": true } },
                      { "name": "events", "replicaNumber": 0, "healthStats": { "healthy": false } }
                    ]
                """)
                .build());

        MgmtRestClient client = MgmtRestClient.fromConnectionString(
                server.url("/").toString(),
                "admin",
                "secret".toCharArray()
        );

        MgmtBucketsFetcher fetcher = new MgmtBucketsFetcher(client);
        List<BucketInfo> buckets = fetcher.fetch();

        assertEquals(2, buckets.size());

        BucketInfo b0 = buckets.getFirst();
        assertEquals("orders", b0.name());
        assertEquals(1, b0.replicaNumber());
        assertTrue(b0.healthy());

        BucketInfo b1 = buckets.get(1);
        assertEquals("events", b1.name());
        assertEquals(0, b1.replicaNumber());
        assertFalse(b1.healthy());
    }

    @Test
    void fetch_shouldDefaultReplicaNumberToZero_whenMissing() {
        server.enqueue(new MockResponse.Builder()
                .code(200)
                .addHeader("Content-Type", "application/json")
                .body("""
                    [
                      { "name": "no-replica-field" }
                    ]
                """)
                .build());

        MgmtRestClient client = MgmtRestClient.fromConnectionString(
                server.url("/").toString(),
                "admin",
                "secret".toCharArray()
        );

        MgmtBucketsFetcher fetcher = new MgmtBucketsFetcher(client);
        List<BucketInfo> buckets = fetcher.fetch();

        assertEquals(1, buckets.size());
        assertEquals("no-replica-field", buckets.getFirst().name());
        assertEquals(0, buckets.getFirst().replicaNumber());
    }
}

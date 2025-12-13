package com.cbdoctor.core.collector.rest;

import com.cbdoctor.core.collector.ClusterInfo;
import com.cbdoctor.core.collector.NodeInfo;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MgmtClusterInfoFetcher using MockWebServer.
 */
class MgmtClusterInfoFetcherTest {

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
    void fetch_shouldParseClusterNameNodesAndRebalanceStatus() {
        server.enqueue(new MockResponse.Builder()
                .code(200)
                .addHeader("Content-Type", "application/json")
                .body("""
                    {
                      "clusterName": "local-cb",
                      "rebalanceStatus": "none",
                      "nodes": [
                        {
                          "hostname": "127.0.0.1:8091",
                          "status": "healthy",
                          "services": ["kv", "n1ql"],
                          "systemStats": { "diskUsedPercent": 12.5 }
                        },
                        {
                          "hostname": "127.0.0.2:8091",
                          "status": "unhealthy",
                          "services": ["index"]
                        }
                      ]
                    }
                """)
                .build());

        MgmtRestClient client = MgmtRestClient.fromConnectionString(
                server.url("/").toString(),
                "admin",
                "secret".toCharArray()
        );

        MgmtClusterInfoFetcher fetcher = new MgmtClusterInfoFetcher(client);
        ClusterInfo info = fetcher.fetch();

        assertEquals("local-cb", info.clusterName());
        assertEquals("none", info.rebalanceStatus());
        assertEquals(2, info.nodes().size());

        NodeInfo n0 = info.nodes().getFirst();
        assertEquals("127.0.0.1", n0.hostname());
        assertTrue(n0.healthy());
        assertTrue(n0.services().contains("kv"));
        assertEquals(12.5, n0.diskUsedPercent());

        NodeInfo n1 = info.nodes().get(1);
        assertFalse(n1.healthy());
        assertTrue(n1.services().contains("index"));
        assertNull(n1.diskUsedPercent());
    }
}

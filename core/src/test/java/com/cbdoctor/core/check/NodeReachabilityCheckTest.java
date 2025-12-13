package com.cbdoctor.core.check;

import com.cbdoctor.core.collector.ClusterSnapshot;
import com.cbdoctor.core.collector.NodeInfo;
import com.cbdoctor.core.model.Finding;
import com.cbdoctor.core.model.Severity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for NodeReachabilityCheck.
 */
class NodeReachabilityCheckTest {

    @Test
    void shouldReturnEmpty_whenNoNodes() {
        ClusterSnapshot snapshot = new ClusterSnapshot(
                "test",
                Instant.now(),
                List.of(),
                List.of(),
                Map.of()
        );

        NodeReachabilityCheck check = new NodeReachabilityCheck();
        assertTrue(check.run(snapshot).isEmpty());
    }

    @Test
    void shouldReturnEmpty_whenAllNodesHealthy() {
        ClusterSnapshot snapshot = new ClusterSnapshot(
                "test",
                Instant.now(),
                List.of(
                        new NodeInfo("n1", null, Set.of("kv"), true, null),
                        new NodeInfo("n2", null, Set.of("kv"), true, null)
                ),
                List.of(),
                Map.of()
        );

        NodeReachabilityCheck check = new NodeReachabilityCheck();
        assertTrue(check.run(snapshot).isEmpty());
    }

    @Test
    void shouldReturnHighFinding_whenAnyNodeUnhealthy() {
        ClusterSnapshot snapshot = new ClusterSnapshot(
                "test",
                Instant.now(),
                List.of(
                        new NodeInfo("n1", null, Set.of("kv"), true, null),
                        new NodeInfo("n2", null, Set.of("kv", "index"), false, null)
                ),
                List.of(),
                Map.of()
        );

        NodeReachabilityCheck check = new NodeReachabilityCheck();
        Finding finding = check.run(snapshot).orElseThrow();

        assertEquals("NODE_REACHABILITY", finding.id());
        assertEquals(Severity.HIGH, finding.severity());
        assertTrue(finding.message().contains("n2"));
        assertNotNull(finding.evidence());
        assertEquals(1, ((Number) finding.evidence().get("unhealthyNodeCount")).intValue());
    }
}

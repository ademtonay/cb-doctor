package com.cbdoctor.core.check;

import com.cbdoctor.core.collector.ClusterSnapshot;
import com.cbdoctor.core.collector.NodeInfo;
import com.cbdoctor.core.model.Finding;
import com.cbdoctor.core.model.Severity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for NodeReachabilityCheck.
 */
class NodeReachabilityCheckTest {

    @Test
    void shouldSkip_whenSnapshotIsNull() {
        CheckAssertions.assertSkipped(new NodeReachabilityCheck().run(null));
    }

    @Test
    void shouldSkip_whenNoNodes() {
        ClusterSnapshot snapshot = new ClusterSnapshot(
                "test",
                Instant.now(),
                null,
                List.of(),
                List.of(),
                null
        );

        CheckAssertions.assertSkipped(new NodeReachabilityCheck().run(snapshot));
    }

    @Test
    void shouldPass_whenAllNodesHealthy() {
        ClusterSnapshot snapshot = new ClusterSnapshot(
                "test",
                Instant.now(),
                null,
                List.of(
                        new NodeInfo("n1", null, Set.of("kv"), true, null),
                        new NodeInfo("n2", null, Set.of("kv"), true, null)
                ),
                List.of(),
                null
        );

        CheckAssertions.assertPass(new NodeReachabilityCheck().run(snapshot));
    }

    @Test
    void shouldPass_whenNodesContainNullEntriesButAllRealNodesHealthy() {
        List<NodeInfo> nodes = new java.util.ArrayList<>();
        nodes.add(null);
        nodes.add(new NodeInfo("n1", null, Set.of("kv"), true, null));

        ClusterSnapshot snapshot = new ClusterSnapshot(
                "test",
                Instant.now(),
                null,
                nodes,
                List.of(),
                null
        );

        CheckAssertions.assertPass(new NodeReachabilityCheck().run(snapshot));
    }


    @Test
    void shouldReturnHighFinding_whenAnyNodeUnhealthy() {
        ClusterSnapshot snapshot = new ClusterSnapshot(
                "test",
                Instant.now(),
                null,
                List.of(
                        new NodeInfo("n1", null, Set.of("kv"), true, null),
                        new NodeInfo("n2", null, Set.of("kv", "index"), false, null)
                ),
                List.of(),
                null
        );

        Finding finding = CheckAssertions.assertFinding(
                new NodeReachabilityCheck().run(snapshot),
                Severity.HIGH
        );

        assertEquals("NODE_REACHABILITY", finding.id());
        assertTrue(finding.message().contains("n2"));
        assertNotNull(finding.evidence());
        assertEquals(1, ((Number) finding.evidence().get("unhealthyNodeCount")).intValue());
    }
}

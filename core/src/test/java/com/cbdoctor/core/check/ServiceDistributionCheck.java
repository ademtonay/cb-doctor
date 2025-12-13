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
 * Tests for ServiceDistributionCheck.
 */
class ServiceDistributionCheckTest {

    @Test
    void shouldReturnEmpty_whenNoNodes() {
        ClusterSnapshot snapshot = new ClusterSnapshot(
                "test",
                Instant.now(),
                "none",
                List.of(),
                List.of(),
                null
        );

        assertTrue(new ServiceDistributionCheck().run(snapshot).isEmpty());
    }

    @Test
    void shouldReturnEmpty_whenNoServicesPresent() {
        ClusterSnapshot snapshot = new ClusterSnapshot(
                "test",
                Instant.now(),
                "none",
                List.of(
                        new NodeInfo("n1", null, Set.of(), true, null),
                        new NodeInfo("n2", null, Set.of(), true, null)
                ),
                List.of(),
                null
        );

        assertTrue(new ServiceDistributionCheck().run(snapshot).isEmpty());
    }

    @Test
    void shouldFlagIndexSingleNode_whenClusterHasMultipleNodes() {
        ClusterSnapshot snapshot = new ClusterSnapshot(
                "test",
                Instant.now(),
                "none",
                List.of(
                        new NodeInfo("n1", null, Set.of("kv", "index"), true, null),
                        new NodeInfo("n2", null, Set.of("kv"), true, null)
                ),
                List.of(),
                null
        );

        Finding finding = new ServiceDistributionCheck().run(snapshot).orElseThrow();
        assertEquals(Severity.MEDIUM, finding.severity());
        assertTrue(finding.message().toLowerCase().contains("index"));
    }

    @Test
    void shouldFlagKvMissingOnSomeNodes() {
        ClusterSnapshot snapshot = new ClusterSnapshot(
                "test",
                Instant.now(),
                "none",
                List.of(
                        new NodeInfo("n1", null, Set.of("kv"), true, null),
                        new NodeInfo("n2", null, Set.of("index"), true, null)
                ),
                List.of(),
                null
        );

        Finding finding = new ServiceDistributionCheck().run(snapshot).orElseThrow();
        assertEquals(Severity.MEDIUM, finding.severity());

        Map<String, Object> evidence = finding.evidence();
        assertNotNull(evidence.get("issues"));
    }
}

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
    void shouldSkip_whenSnapshotIsNull() {
        CheckAssertions.assertSkipped(new ServiceDistributionCheck().run(null));
    }

    @Test
    void shouldSkip_whenNoNodes() {
        ClusterSnapshot snapshot = new ClusterSnapshot(
                "test",
                Instant.now(),
                "none",
                List.of(),
                List.of(),
                null
        );

        CheckAssertions.assertSkipped(new ServiceDistributionCheck().run(snapshot));
    }

    @Test
    void shouldSkip_whenNoServicesPresent() {
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

        CheckAssertions.assertSkipped(new ServiceDistributionCheck().run(snapshot));
    }

    @Test
    void shouldPass_whenNoServicePlacementIssuesDetected() {
        ClusterSnapshot snapshot = new ClusterSnapshot(
                "test",
                Instant.now(),
                "none",
                List.of(
                        new NodeInfo("n1", null, Set.of("kv", "index"), true, null),
                        new NodeInfo("n2", null, Set.of("kv", "index"), true, null)
                ),
                List.of(),
                null
        );

        CheckAssertions.assertPass(new ServiceDistributionCheck().run(snapshot));
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

        Finding finding = CheckAssertions.assertFinding(
                new ServiceDistributionCheck().run(snapshot),
                Severity.MEDIUM
        );

        assertTrue(finding.message().toLowerCase().contains("index"));
        assertNotNull(finding.evidence());
        assertNotNull(finding.evidence().get("issues"));
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

        Finding finding = CheckAssertions.assertFinding(
                new ServiceDistributionCheck().run(snapshot),
                Severity.MEDIUM
        );

        Map<String, Object> evidence = finding.evidence();
        assertNotNull(evidence);
        assertNotNull(evidence.get("issues"));
    }
}

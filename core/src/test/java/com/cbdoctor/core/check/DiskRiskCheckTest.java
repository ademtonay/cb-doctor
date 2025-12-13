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
 * Tests for DiskRiskCheck.
 */
class DiskRiskCheckTest {

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

        assertTrue(new DiskRiskCheck().run(snapshot).isEmpty());
    }

    @Test
    void shouldReturnEmpty_whenNoDiskMetricsPresent() {
        ClusterSnapshot snapshot = new ClusterSnapshot(
                "test",
                Instant.now(),
                "none",
                List.of(
                        new NodeInfo("n1", null, Set.of("kv"), true, null),
                        new NodeInfo("n2", null, Set.of("kv"), true, null)
                ),
                List.of(),
                null
        );

        assertTrue(new DiskRiskCheck().run(snapshot).isEmpty());
    }

    @Test
    void shouldReturnEmpty_whenDiskBelowThreshold() {
        ClusterSnapshot snapshot = new ClusterSnapshot(
                "test",
                Instant.now(),
                "none",
                List.of(
                        new NodeInfo("n1", null, Set.of("kv"), true, 40.0),
                        new NodeInfo("n2", null, Set.of("kv"), true, 79.9)
                ),
                List.of(),
                null
        );

        assertTrue(new DiskRiskCheck().run(snapshot).isEmpty());
    }

    @Test
    void shouldReturnMedium_whenDiskAboveThreshold() {
        ClusterSnapshot snapshot = new ClusterSnapshot(
                "test",
                Instant.now(),
                "none",
                List.of(
                        new NodeInfo("n1", null, Set.of("kv"), true, 81.0),
                        new NodeInfo("n2", null, Set.of("kv"), true, 50.0)
                ),
                List.of(),
                null
        );

        Finding finding = new DiskRiskCheck().run(snapshot).orElseThrow();
        assertEquals(Severity.MEDIUM, finding.severity());
        assertTrue(finding.message().contains("n1"));
    }
}

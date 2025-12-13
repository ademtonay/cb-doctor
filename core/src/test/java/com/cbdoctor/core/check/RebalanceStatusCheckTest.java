package com.cbdoctor.core.check;

import com.cbdoctor.core.collector.ClusterSnapshot;
import com.cbdoctor.core.model.Severity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RebalanceStatusCheck.
 */
class RebalanceStatusCheckTest {

    @Test
    void shouldReturnEmpty_whenStatusNone() {
        ClusterSnapshot snapshot = new ClusterSnapshot(
                "test",
                Instant.now(),
                "none",
                List.of(),
                List.of(),
                null
        );

        assertTrue(new RebalanceStatusCheck().run(snapshot).isEmpty());
    }

    @Test
    void shouldReturnMedium_whenRunning() {
        ClusterSnapshot snapshot = new ClusterSnapshot(
                "test",
                Instant.now(),
                "running",
                List.of(),
                List.of(),
                null
        );

        var finding = new RebalanceStatusCheck().run(snapshot).orElseThrow();
        assertEquals(Severity.MEDIUM, finding.severity());
        assertTrue(finding.message().toLowerCase().contains("running"));
    }

    @Test
    void shouldReturnMedium_whenUnknownValue() {
        ClusterSnapshot snapshot = new ClusterSnapshot(
                "test",
                Instant.now(),
                "weird_state",
                List.of(),
                List.of(),
                null
        );

        var finding = new RebalanceStatusCheck().run(snapshot).orElseThrow();
        assertEquals(Severity.MEDIUM, finding.severity());
        assertTrue(finding.message().contains("weird_state"));
    }
}

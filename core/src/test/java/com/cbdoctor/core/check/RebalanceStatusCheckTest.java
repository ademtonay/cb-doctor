package com.cbdoctor.core.check;

import com.cbdoctor.core.collector.ClusterSnapshot;
import com.cbdoctor.core.model.Finding;
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
    void shouldSkip_whenSnapshotIsNull() {
        CheckAssertions.assertSkipped(new RebalanceStatusCheck().run(null));
    }

    @Test
    void shouldSkip_whenStatusIsNull() {
        ClusterSnapshot snapshot = new ClusterSnapshot(
                "test",
                Instant.now(),
                null,
                List.of(),
                List.of(),
                null
        );

        CheckAssertions.assertSkipped(new RebalanceStatusCheck().run(snapshot));
    }

    @Test
    void shouldSkip_whenStatusIsBlank() {
        ClusterSnapshot snapshot = new ClusterSnapshot(
                "test",
                Instant.now(),
                "   ",
                List.of(),
                List.of(),
                null
        );

        CheckAssertions.assertSkipped(new RebalanceStatusCheck().run(snapshot));
    }

    @Test
    void shouldPass_whenStatusIsNone() {
        ClusterSnapshot snapshot = new ClusterSnapshot(
                "test",
                Instant.now(),
                "none",
                List.of(),
                List.of(),
                null
        );

        CheckAssertions.assertPass(new RebalanceStatusCheck().run(snapshot));
    }

    @Test
    void shouldReturnMedium_whenStatusIsRunning() {
        ClusterSnapshot snapshot = new ClusterSnapshot(
                "test",
                Instant.now(),
                "running",
                List.of(),
                List.of(),
                null
        );

        Finding finding = CheckAssertions.assertFinding(
                new RebalanceStatusCheck().run(snapshot),
                Severity.MEDIUM
        );

        assertTrue(finding.message().toLowerCase().contains("running"));
    }

    @Test
    void shouldReturnMedium_whenStatusIsUnknown() {
        ClusterSnapshot snapshot = new ClusterSnapshot(
                "test",
                Instant.now(),
                "weird_state",
                List.of(),
                List.of(),
                null
        );

        Finding finding = CheckAssertions.assertFinding(
                new RebalanceStatusCheck().run(snapshot),
                Severity.MEDIUM
        );

        assertTrue(finding.message().contains("weird_state"));
    }
}

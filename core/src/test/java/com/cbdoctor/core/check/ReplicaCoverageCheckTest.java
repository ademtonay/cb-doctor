package com.cbdoctor.core.check;

import com.cbdoctor.core.collector.BucketInfo;
import com.cbdoctor.core.collector.ClusterSnapshot;
import com.cbdoctor.core.model.Finding;
import com.cbdoctor.core.model.Severity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ReplicaCoverageCheck.
 */
class ReplicaCoverageCheckTest {

    @Test
    void shouldSkip_whenSnapshotIsNull() {
        CheckAssertions.assertSkipped(new ReplicaCoverageCheck().run(null));
    }

    @Test
    void shouldSkip_whenNoBuckets() {
        ClusterSnapshot snapshot = new ClusterSnapshot(
                "test",
                Instant.now(),
                "none",
                List.of(),
                List.of(),
                null
        );

        CheckAssertions.assertSkipped(new ReplicaCoverageCheck().run(snapshot));
    }

    @Test
    void shouldPass_whenAllBucketsHaveReplicas() {
        ClusterSnapshot snapshot = new ClusterSnapshot(
                "test",
                Instant.now(),
                "none",
                List.of(),
                List.of(
                        new BucketInfo("orders", 1, true, null),
                        new BucketInfo("events", 2, true, null)
                ),
                null
        );

        CheckAssertions.assertPass(new ReplicaCoverageCheck().run(snapshot));
    }

    @Test
    void shouldReturnCritical_whenAnyBucketHasZeroReplicas() {
        ClusterSnapshot snapshot = new ClusterSnapshot(
                "test",
                Instant.now(),
                "none",
                List.of(),
                List.of(
                        new BucketInfo("orders", 1, true, null),
                        new BucketInfo("events", 0, true, null)
                ),
                null
        );

        Finding finding = CheckAssertions.assertFinding(
                new ReplicaCoverageCheck().run(snapshot),
                Severity.CRITICAL
        );

        assertTrue(finding.message().contains("events"));

        Object count = finding.evidence().get("bucketCountWithZeroReplicas");
        assertEquals(1, ((Number) count).intValue());
    }
}

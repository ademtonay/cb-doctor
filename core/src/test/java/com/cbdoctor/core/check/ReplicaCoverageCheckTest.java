package com.cbdoctor.core.check;

import com.cbdoctor.core.collector.BucketInfo;
import com.cbdoctor.core.collector.ClusterSnapshot;
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
    void shouldReturnEmpty_whenNoBuckets() {
        ClusterSnapshot snapshot = new ClusterSnapshot(
                "test",
                Instant.now(),
                "none",
                List.of(),
                List.of(),
                null
        );

        assertTrue(new ReplicaCoverageCheck().run(snapshot).isEmpty());
    }

    @Test
    void shouldReturnEmpty_whenAllBucketsHaveReplicas() {
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

        assertTrue(new ReplicaCoverageCheck().run(snapshot).isEmpty());
    }

    @Test
    void shouldReturnHigh_whenAnyBucketHasZeroReplicas() {
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

        var finding = new ReplicaCoverageCheck().run(snapshot).orElseThrow();
        assertEquals(Severity.CRITICAL, finding.severity());
        assertTrue(finding.message().contains("events"));

        Object count = finding.evidence().get("bucketCountWithZeroReplicas");
        assertEquals(1, ((Number) count).intValue());
    }
}

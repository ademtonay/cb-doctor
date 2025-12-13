package com.cbdoctor.core.collector;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ClusterSnapshot(
        String clusterName,
        Instant collectedAt,
        List<NodeInfo> nodes,
        List<BucketInfo> buckets,
        Map<String, Object> raw // fallback / debug / future use
) {

    public boolean hasNodes() {
        return nodes != null && !nodes.isEmpty();
    }

    public boolean hasBuckets() {
        return buckets != null && !buckets.isEmpty();
    }

    public static ClusterSnapshot empty(String clusterName) {
        return new ClusterSnapshot(
                clusterName,
                Instant.now(),
                List.of(),
                List.of(),
                Map.of()
        );
    }
}

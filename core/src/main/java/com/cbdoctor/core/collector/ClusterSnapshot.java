package com.cbdoctor.core.collector;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.List;

public record ClusterSnapshot(
        String clusterName,
        Instant collectedAt,
        String rebalanceStatus,
        List<NodeInfo> nodes,
        List<BucketInfo> buckets,
        JsonNode raw // fallback / debug / future use
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
                null,
                List.of(),
                List.of(),
                null
        );
    }
}

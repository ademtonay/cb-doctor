package com.cbdoctor.core.check;

import com.cbdoctor.core.collector.BucketInfo;
import com.cbdoctor.core.collector.ClusterSnapshot;
import com.cbdoctor.core.model.Finding;
import com.cbdoctor.core.model.Severity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Reports a HIGH finding when one or more buckets have replicaNumber == 0.
 * Replica count is a critical durability setting for production clusters.
 */
public final class ReplicaCoverageCheck implements Check {

    @Override
    public String id() {
        return "REPLICA_COVERAGE";
    }

    @Override
    public String name() {
        return "Replica coverage";
    }

    @Override
    public Optional<Finding> run(ClusterSnapshot snapshot) {
        if (snapshot == null || snapshot.buckets() == null || snapshot.buckets().isEmpty()) {
            // Not enough data to evaluate
            return Optional.empty();
        }

        List<BucketInfo> zeroReplicaBuckets = snapshot.buckets().stream()
                .filter(b -> b != null && b.replicaNumber() == 0)
                .toList();

        if (zeroReplicaBuckets.isEmpty()) {
            return Optional.empty();
        }

        String bucketNames = zeroReplicaBuckets.stream()
                .map(b -> b.name() != null ? b.name() : "unknown")
                .collect(Collectors.joining(", "));

        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("bucketCountWithZeroReplicas", zeroReplicaBuckets.size());
        evidence.put("buckets", zeroReplicaBuckets.stream()
                .map(b -> Map.of(
                        "name", safe(b.name()),
                        "replicaNumber", b.replicaNumber()
                ))
                .toList());

        return Optional.of(new Finding(
                id(),
                Severity.CRITICAL,
                name(),
                "One or more buckets have replicaNumber=0: " + bucketNames,
                evidence,
                "Set replicaNumber to at least 1 for production workloads (subject to node count and capacity). " +
                "After changing replica settings, monitor rebalance and disk usage."
        ));
    }

    private static String safe(String s) {
        return (s == null) ? "" : s;
    }
}
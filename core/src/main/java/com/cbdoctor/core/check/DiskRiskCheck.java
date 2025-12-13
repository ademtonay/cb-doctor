package com.cbdoctor.core.check;

import com.cbdoctor.core.collector.ClusterSnapshot;
import com.cbdoctor.core.collector.NodeInfo;
import com.cbdoctor.core.model.Finding;
import com.cbdoctor.core.model.Severity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Reports a MEDIUM finding when disk usage is above a threshold on any node.
 * <p>
 * MVP behavior:
 * - If at least one node reports diskUsedPercent and any is > 80 => MEDIUM finding
 * - If no node reports diskUsedPercent => Optional.empty() (engine may emit SKIPPED)
 */
public final class DiskRiskCheck implements Check {

    private static final double DEFAULT_THRESHOLD_PERCENT = 80.0;

    @Override
    public String id() {
        return "DISK_RISK";
    }

    @Override
    public String name() {
        return "Disk risk";
    }

    @Override
    public Optional<Finding> run(ClusterSnapshot snapshot) {
        if (snapshot == null || snapshot.nodes() == null || snapshot.nodes().isEmpty()) {
            return Optional.empty();
        }

        List<NodeInfo> nodes = snapshot.nodes().stream()
                .filter(Objects::nonNull)
                .toList();

        boolean anyMetricPresent = nodes.stream().anyMatch(n -> n.diskUsedPercent() != null);
        if (!anyMetricPresent) {
            // Not enough data to evaluate
            return Optional.empty();
        }

        List<NodeInfo> risky = nodes.stream()
                .filter(n -> n.diskUsedPercent() != null && n.diskUsedPercent() > DEFAULT_THRESHOLD_PERCENT)
                .toList();

        if (risky.isEmpty()) {
            return Optional.empty();
        }

        String affected = risky.stream()
                .map(n -> n.hostname() != null ? n.hostname() : "unknown")
                .collect(Collectors.joining(", "));

        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("thresholdPercent", DEFAULT_THRESHOLD_PERCENT);
        evidence.put("riskyNodeCount", risky.size());
        evidence.put("riskyNodes", risky.stream()
                .map(n -> Map.of(
                        "hostname", safe(n.hostname()),
                        "diskUsedPercent", n.diskUsedPercent()
                ))
                .toList());

        return Optional.of(new Finding(
                id(),
                Severity.MEDIUM,
                name(),
                "Disk usage is above " + DEFAULT_THRESHOLD_PERCENT + "% on: " + affected,
                evidence,
                "Investigate disk growth drivers (data, indexes, logs). " +
                "Consider adding capacity, reducing retention, or rebalancing before disk becomes critical."
        ));
    }

    private static String safe(String s) {
        return (s == null) ? "" : s;
    }
}

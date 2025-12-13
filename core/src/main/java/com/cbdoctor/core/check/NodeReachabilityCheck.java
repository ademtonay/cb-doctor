package com.cbdoctor.core.check;

import com.cbdoctor.core.collector.ClusterSnapshot;
import com.cbdoctor.core.collector.NodeInfo;
import com.cbdoctor.core.model.Finding;
import com.cbdoctor.core.model.Severity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Reports a HIGH finding when one or more cluster nodes are unhealthy/unreachable.
 */
public final class NodeReachabilityCheck implements Check {

    @Override
    public String id() {
        return "NODE_REACHABILITY";
    }

    @Override
    public String name() {
        return "Node reachability";
    }

    @Override
    public Optional<Finding> run(ClusterSnapshot snapshot) {
        if (snapshot == null || snapshot.nodes() == null || snapshot.nodes().isEmpty()) {
            // Not enough data to evaluate
            return Optional.empty();
        }

        List<NodeInfo> unhealthy = snapshot.nodes().stream()
                .filter(n -> n != null && !n.healthy())
                .toList();

        if (unhealthy.isEmpty()) {
            return Optional.empty();
        }

        String affected = unhealthy.stream()
                .map(n -> n.hostname() != null ? n.hostname() : "unknown")
                .collect(Collectors.joining(", "));

        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("unhealthyNodeCount", unhealthy.size());
        evidence.put("unhealthyNodes", unhealthy.stream()
                .map(n -> Map.of(
                        "hostname", safe(n.hostname()),
                        "services", n.services() == null ? List.of() : n.services().stream().toList()
                ))
                .toList());

        return Optional.of(new Finding(
                id(),
                Severity.HIGH,
                name(),
                "One or more nodes are unhealthy/unreachable: " + affected,
                evidence,
                "Check node availability, network connectivity, and Couchbase services. " +
                "If a node is down, investigate logs and consider failover/rebalance procedures."
        ));
    }

    private static String safe(String s) {
        return (s == null) ? "" : s;
    }
}

package com.cbdoctor.core.check;

import com.cbdoctor.core.collector.ClusterSnapshot;
import com.cbdoctor.core.collector.NodeInfo;
import com.cbdoctor.core.model.Finding;
import com.cbdoctor.core.model.Severity;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Flags service placement patterns that commonly increase operational risk.
 * <p>
 * MVP rules:
 * - Index service present on only one node => MEDIUM
 * - KV service missing on some nodes      => MEDIUM
 */
public final class ServiceDistributionCheck implements Check {

    @Override
    public String id() {
        return "SERVICE_DISTRIBUTION";
    }

    @Override
    public String name() {
        return "Service distribution";
    }

    @Override
    public Optional<Finding> run(ClusterSnapshot snapshot) {
        if (snapshot == null || snapshot.nodes() == null || snapshot.nodes().isEmpty()) {
            return Optional.empty();
        }

        List<NodeInfo> nodes = snapshot.nodes().stream()
                .filter(Objects::nonNull)
                .toList();

        if (nodes.isEmpty()) return Optional.empty();

        boolean anyServicesPresent = nodes.stream().anyMatch(n -> n.services() != null && !n.services().isEmpty());
        if (!anyServicesPresent) {
            return Optional.empty();
        }

        // Normalize service names and count per service.
        Map<String, List<NodeInfo>> serviceToNodes = new LinkedHashMap<>();
        for (NodeInfo n : nodes) {
            Set<String> normalized = normalizeServices(n.services());
            for (String s : normalized) {
                serviceToNodes.computeIfAbsent(s, k -> new ArrayList<>()).add(n);
            }
        }

        int totalNodes = nodes.size();
        List<Map<String, Object>> issues = new ArrayList<>();

        // Rule 1: Index service on only one node
        List<NodeInfo> indexNodes = serviceToNodes.getOrDefault("index", List.of());
        if (indexNodes.size() == 1 && totalNodes >= 2) {
            issues.add(Map.of(
                    "type", "INDEX_SINGLE_NODE",
                    "service", "index",
                    "nodeCountWithService", indexNodes.size(),
                    "nodes", hostnames(indexNodes)
            ));
        }

        // Rule 2: KV service missing on some nodes
        List<NodeInfo> kvNodes = serviceToNodes.getOrDefault("kv", List.of());
        if (!kvNodes.isEmpty() && kvNodes.size() < totalNodes) {
            Set<String> kvHosts = new LinkedHashSet<>(hostnames(kvNodes));
            List<String> missing = nodes.stream()
                    .map(NodeInfo::hostname)
                    .filter(Objects::nonNull)
                    .filter(h -> !kvHosts.contains(h))
                    .toList();

            issues.add(Map.of(
                    "type", "KV_NOT_ON_ALL_NODES",
                    "service", "kv",
                    "nodeCountWithService", kvNodes.size(),
                    "totalNodes", totalNodes,
                    "missingOnNodes", missing
            ));
        }

        if (issues.isEmpty()) {
            return Optional.empty();
        }

        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("issues", issues);

        String message = buildMessage(issues);

        return Optional.of(new Finding(
                id(),
                Severity.MEDIUM,
                name(),
                message,
                evidence,
                "Review service placement for resiliency and operational flexibility. " +
                "Consider distributing critical services across multiple nodes where appropriate."
        ));
    }

    private static Set<String> normalizeServices(Set<String> services) {
        if (services == null) return Set.of();

        Set<String> out = new LinkedHashSet<>();
        for (String s : services) {
            if (s == null) continue;
            String n = s.trim().toLowerCase();
            if (n.isEmpty()) continue;

            // Common aliases across Couchbase versions/outputs
            if (n.equals("n1ql") || n.equals("query")) n = "query";
            if (n.equals("data") || n.equals("kv")) n = "kv";
            if (n.equals("fts") || n.equals("search")) n = "search";

            out.add(n);
        }
        return Set.copyOf(out);
    }

    private static List<String> hostnames(List<NodeInfo> nodes) {
        return nodes.stream()
                .map(NodeInfo::hostname)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private static String buildMessage(List<Map<String, Object>> issues) {
        List<String> parts = new ArrayList<>();
        for (Map<String, Object> issue : issues) {
            String type = String.valueOf(issue.get("type"));
            if ("INDEX_SINGLE_NODE".equals(type)) {
                parts.add("Index service is running on a single node.");
            } else if ("KV_NOT_ON_ALL_NODES".equals(type)) {
                parts.add("KV service is not present on all nodes.");
            } else {
                parts.add("Service placement issue detected: " + type);
            }
        }
        return String.join(" ", parts);
    }
}

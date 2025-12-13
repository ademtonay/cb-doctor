package com.cbdoctor.core.collector.rest;

import com.cbdoctor.core.collector.ClusterInfo;
import com.cbdoctor.core.collector.NodeInfo;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;

/**
 * Fetches and parses cluster-level information from the Couchbase
 * Management REST endpoint (/pools/default).
 */
public final class MgmtClusterInfoFetcher {

    private final MgmtRestClient client;

    public MgmtClusterInfoFetcher(MgmtRestClient client) {
        this.client = Objects.requireNonNull(client, "client is required");
    }

    /**
     * Fetches cluster information and maps it into a ClusterInfo model.
     */
    public ClusterInfo fetch() {
        JsonNode root = client.getPoolsDefault();

        String clusterName = text(root, "clusterName", "unknown");
        String rebalanceStatus = text(root, "rebalanceStatus", "unknown");

        List<NodeInfo> nodes = parseNodes(root.path("nodes"));

        return new ClusterInfo(clusterName, rebalanceStatus, nodes, root);
    }

    /**
     * Parses node information from the "nodes" array.
     */
    private static List<NodeInfo> parseNodes(JsonNode nodesNode) {
        if (nodesNode == null || !nodesNode.isArray()) return List.of();

        List<NodeInfo> nodes = new ArrayList<>();
        for (JsonNode n : nodesNode) {
            String hostnameRaw = text(n, "hostname", "");
            String hostname = stripPort(hostnameRaw);

            // Node health is primarily determined by the "status" field
            String status = text(n, "status", "");
            boolean healthy = "healthy".equalsIgnoreCase(status);

            // Some Couchbase versions indicate failure via clusterMembership
            String membership = text(n, "clusterMembership", "");
            if ("inactiveFailed".equalsIgnoreCase(membership)
                || "inactiveAdded".equalsIgnoreCase(membership)) {
                healthy = false;
            }

            Set<String> services = parseServices(n);
            Double diskUsedPercent = parseDiskUsedPercent(n);

            nodes.add(new NodeInfo(
                    hostname,
                    null,              // IP address is optional for MVP
                    services,
                    healthy,
                    diskUsedPercent
            ));
        }

        return List.copyOf(nodes);
    }

    /**
     * Extracts service names from different possible representations.
     */
    private static Set<String> parseServices(JsonNode node) {
        Set<String> out = new LinkedHashSet<>();

        JsonNode services = node.get("services");
        if (services != null) {
            if (services.isArray()) {
                for (JsonNode s : services) {
                    if (s.isTextual()) out.add(s.asText());
                }
            } else if (services.isObject()) {
                services.fieldNames().forEachRemaining(out::add);
            }
        }

        JsonNode serviceNames = node.get("serviceNames");
        if (serviceNames != null && serviceNames.isArray()) {
            for (JsonNode s : serviceNames) {
                if (s.isTextual()) out.add(s.asText());
            }
        }

        return Set.copyOf(out);
    }

    /**
     * Attempts to extract disk usage percentage using best-effort parsing.
     */
    private static Double parseDiskUsedPercent(JsonNode node) {
        Double v = number(node, "diskUsedPercent");
        if (v != null) return v;

        JsonNode systemStats = node.get("systemStats");
        if (systemStats != null) {
            v = number(systemStats, "diskUsedPercent");
            if (v != null) return v;
        }

        JsonNode interestingStats = node.get("interestingStats");
        if (interestingStats != null) {
            v = number(interestingStats, "diskUsedPercent");
            return v;
        }

        return null;
    }

    /**
     * Reads a text field with a default fallback.
     */
    private static String text(JsonNode node, String field, String def) {
        if (node == null) return def;
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) return def;
        String s = v.asText();
        return (s == null || s.isBlank()) ? def : s;
    }

    /**
     * Reads a numeric field, allowing both numeric and string values.
     */
    private static Double number(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) return null;
        if (v.isNumber()) return v.doubleValue();
        if (v.isTextual()) {
            try {
                return Double.parseDouble(v.asText());
            } catch (NumberFormatException ignore) {
                return null;
            }
        }
        return null;
    }

    /**
     * Removes the port from a hostname if present.
     */
    private static String stripPort(String hostname) {
        if (hostname == null) return "";
        int idx = hostname.lastIndexOf(':');
        if (idx > 0 && idx < hostname.length() - 1) {
            String portPart = hostname.substring(idx + 1);
            if (portPart.chars().allMatch(Character::isDigit)) {
                return hostname.substring(0, idx);
            }
        }
        return hostname;
    }
}

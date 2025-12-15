package com.cbdoctor.core.collector.rest;

import com.cbdoctor.core.collector.ClusterInfo;
import com.cbdoctor.core.collector.NodeInfo;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;

/**
 * Fetches and parses cluster-level information from the Couchbase
 * Management REST endpoint (/pools/default).
 */
public final class MgmtClusterInfoFetcher extends AbstractMgmtFetcher<ClusterInfo> {
    public MgmtClusterInfoFetcher(MgmtRestClient client) {
        super(client);
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
    private List<NodeInfo> parseNodes(JsonNode nodesNode) {
        if (nodesNode == null || !nodesNode.isArray()) return List.of();

        List<NodeInfo> nodes = new ArrayList<>();
        for (JsonNode n : nodesNode) {
            String hostnameRaw = text(n, "hostname", "");
            String hostname = stripPort(hostnameRaw);

            String status = text(n, "status", "");
            boolean healthy = "healthy".equalsIgnoreCase(status);

            String membership = text(n, "clusterMembership", "");
            if ("inactiveFailed".equalsIgnoreCase(membership)
                || "inactiveAdded".equalsIgnoreCase(membership)) {
                healthy = false;
            }

            Set<String> services = parseServices(n);

            // Disk metrics are not reliably present in /pools/default nodes[].
            // Fetch per-node details using otpNode and extract disk usage from there.
            Double diskUsedPercent = null;
            String otpNode = text(n, "otpNode", "");

            if (!otpNode.isBlank()) {
                try {
                    JsonNode nodeDetails = client.getNodeByOtp(otpNode);
                    diskUsedPercent = extractDiskUsedPercent(nodeDetails);
                } catch (Exception ignored) {
                    // Best-effort: keep diskUsedPercent as null if the node details cannot be fetched.
                }
            }

            nodes.add(new NodeInfo(
                    hostname,
                    null,
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
}

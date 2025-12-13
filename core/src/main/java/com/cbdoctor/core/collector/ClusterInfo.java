package com.cbdoctor.core.collector;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/**
 * Represents high-level cluster information fetched from the Couchbase
 * Management REST API (/pools/default).
 * <p>
 * This model is intentionally lightweight and defensive to tolerate
 * schema differences across Couchbase versions.
 */
public record ClusterInfo(
        String clusterName,
        String rebalanceStatus,   // e.g. "none", "running", "unknown"
        List<NodeInfo> nodes,
        JsonNode raw              // Raw JSON for debugging or future use
) { }

package com.cbdoctor.core.collector.rest;

import com.cbdoctor.core.collector.BucketInfo;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Fetches and parses bucket information from Couchbase Management REST API:
 * GET /pools/default/buckets
 */
public final class MgmtBucketsFetcher extends AbstractMgmtFetcher<List<BucketInfo>> {
    public MgmtBucketsFetcher(MgmtRestClient client) {
        super(client);
    }

    /**
     * Fetches buckets and maps them into BucketInfo records.
     */
    public List<BucketInfo> fetch() {
        JsonNode root = client.getBuckets();

        if (root == null || !root.isArray()) {
            return List.of();
        }

        List<BucketInfo> buckets = new ArrayList<>();
        for (JsonNode b : root) {
            String name = text(b, "name", "unknown");
            int replicaNumber = intValue(b, "replicaNumber", 0);

            boolean healthy = inferHealth(b);

            buckets.add(new BucketInfo(name, replicaNumber, healthy, b));
        }

        return List.copyOf(buckets);
    }

    /**
     * Best-effort health inference. Couchbase versions may vary in fields.
     * For MVP, we try a few common hints and otherwise default to true.
     */
    private static boolean inferHealth(JsonNode bucket) {
        // Common patterns across versions/editions:
        // - "healthStats": { "healthy": true/false }
        // - "nodes": [ { "status": "healthy" | "unhealthy" }, ... ]
        // - "bucketCapabilities" etc. are not health indicators for MVP

        JsonNode healthStats = bucket.get("healthStats");
        if (healthStats != null) {
            JsonNode healthy = healthStats.get("healthy");
            if (healthy != null && healthy.isBoolean()) {
                return healthy.booleanValue();
            }
        }

        JsonNode nodes = bucket.get("nodes");
        if (nodes != null && nodes.isArray() && !nodes.isEmpty()) {
            for (JsonNode n : nodes) {
                String status = text(n, "status", "healthy");
                if (!"healthy".equalsIgnoreCase(status)) {
                    return false;
                }
            }
            return true;
        }

        return true;
    }

}

package com.cbdoctor.core.collector;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Minimal bucket representation for MVP checks (e.g. replica coverage).
 */
public record BucketInfo(
        String name,
        int replicaNumber,
        boolean healthy,
        JsonNode raw
) {
    public boolean hasReplicas() {
        return replicaNumber > 0;
    }
}

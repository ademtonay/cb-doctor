package com.cbdoctor.core.collector;

public record BucketInfo(
        String name,
        int replicaNumber,
        boolean healthy
) {
    public boolean hasReplicas() {
        return replicaNumber > 0;
    }
}

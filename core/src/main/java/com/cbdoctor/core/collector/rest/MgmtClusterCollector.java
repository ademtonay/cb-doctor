package com.cbdoctor.core.collector.rest;

import com.cbdoctor.core.collector.BucketInfo;
import com.cbdoctor.core.collector.ClusterInfo;
import com.cbdoctor.core.collector.ClusterSnapshot;

import java.time.Clock;
import java.util.List;
import java.util.Objects;

/**
 * Collects cluster state from Couchbase Management REST API and builds a ClusterSnapshot.
 */
public final class MgmtClusterCollector {

    private final MgmtClusterInfoFetcher clusterInfoFetcher;
    private final MgmtBucketsFetcher bucketsFetcher;
    private final Clock clock;

    public MgmtClusterCollector(MgmtRestClient client, Clock clock) {
        Objects.requireNonNull(client, "client is required");
        this.clusterInfoFetcher = new MgmtClusterInfoFetcher(client);
        this.bucketsFetcher = new MgmtBucketsFetcher(client);
        this.clock = (clock != null) ? clock : Clock.systemUTC();
    }

    public ClusterSnapshot collect() {
        ClusterInfo info = clusterInfoFetcher.fetch();
        List<BucketInfo> buckets = bucketsFetcher.fetch();

        return new ClusterSnapshot(
                info.clusterName(),
                clock.instant(),
                info.rebalanceStatus(),
                info.nodes(),
                buckets,
                info.raw()
        );
    }
}

package com.cbdoctor.core.check;

import com.cbdoctor.core.collector.ClusterSnapshot;
import com.cbdoctor.core.model.Finding;

import java.util.Optional;

public interface Check {

    /** Stable id (e.g. "REPLICA_ZERO", "NODE_DOWN") */
    String id();

    /** Human-readable name for rendering */
    String name();

    /**
     * Runs the check. If the check cannot be evaluated (missing data / endpoint),
     * return Optional.empty() and let engine decide whether to emit a SKIPPED finding.
     */
    Optional<Finding> run(ClusterSnapshot snapshot);
}

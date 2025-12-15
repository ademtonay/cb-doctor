package com.cbdoctor.core.check;

import com.cbdoctor.core.collector.ClusterSnapshot;
import com.cbdoctor.core.engine.CheckResult;


/**
 * A check evaluates the snapshot and returns PASS, SKIPPED, or a FINDING.
 */
public interface Check {

    /**
     * Stable id (e.g. "REPLICA_ZERO", "NODE_DOWN")
     */
    String id();

    /**
     * Human-readable name for rendering
     */
    String name();

    /**
     * Runs the check against the given cluster snapshot.
     * <p>
     * The check must explicitly report its outcome:
     * - PASS: the check was evaluated and no issues were found
     * - FINDING: the check detected a problem and returns a Finding
     * - SKIPPED: the check could not be evaluated due to missing data or endpoints
     * <p>
     * The engine does not infer PASS or SKIPPED implicitly; each check is
     * responsible for returning the correct CheckResult.
     */
    CheckResult run(ClusterSnapshot snapshot);
}

package com.cbdoctor.core.check;

import com.cbdoctor.core.collector.ClusterSnapshot;
import com.cbdoctor.core.engine.CheckResult;
import com.cbdoctor.core.model.Finding;
import com.cbdoctor.core.model.Severity;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Flags potentially risky rebalance states based on /pools/default rebalanceStatus.
 */
public final class RebalanceStatusCheck implements Check {

    @Override
    public String id() {
        return "REBALANCE_STATUS";
    }

    @Override
    public String name() {
        return "Rebalance status";
    }

    @Override
    public CheckResult run(ClusterSnapshot snapshot) {
        if (snapshot == null) {
            return CheckResult.skipped(id(), name(), "Snapshot is not available; rebalance status cannot be evaluated.");
        }

        String status = snapshot.rebalanceStatus();
        if (status == null || status.isBlank()) {
            return CheckResult.skipped(id(), name(), "rebalanceStatus is missing; rebalance status cannot be evaluated reliably.");
        }

        String normalized = status.trim().toLowerCase();

        // Rebalance not running -> PASS
        if ("none".equals(normalized)) {
            return CheckResult.pass();
        }

        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("rebalanceStatus", status);

        if ("running".equals(normalized)) {
            return CheckResult.finding(new Finding(
                    id(),
                    Severity.MEDIUM,
                    name(),
                    "Rebalance is currently running. This can be normal, but may increase risk if it runs for a long time.",
                    evidence,
                    "Confirm rebalance progress in Couchbase UI. If stuck, check logs and node health before retrying."
            ));
        }

        // Best-effort: Couchbase versions/editions may return different values.
        return CheckResult.finding(new Finding(
                id(),
                Severity.MEDIUM,
                name(),
                "Rebalance status is unusual or unknown: " + status,
                evidence,
                "Verify rebalance state in Couchbase UI and review recent cluster changes."
        ));
    }
}

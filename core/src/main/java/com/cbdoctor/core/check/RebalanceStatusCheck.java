package com.cbdoctor.core.check;

import com.cbdoctor.core.collector.ClusterSnapshot;
import com.cbdoctor.core.model.Finding;
import com.cbdoctor.core.model.Severity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

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
    public Optional<Finding> run(ClusterSnapshot snapshot) {
        if (snapshot == null) return Optional.empty();

        String status = snapshot.rebalanceStatus();
        if (status == null || status.isBlank()) {
            // Not enough data to evaluate reliably
            return Optional.empty();
        }

        String normalized = status.trim().toLowerCase();

        if ("none".equals(normalized)) {
            return Optional.empty();
        }

        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("rebalanceStatus", status);

        if ("running".equals(normalized)) {
            return Optional.of(new Finding(
                    id(),
                    Severity.MEDIUM,
                    name(),
                    "Rebalance is currently running. This can be normal, but may increase risk if it runs for a long time.",
                    evidence,
                    "Confirm rebalance progress in Couchbase UI. If stuck, check logs and node health before retrying."
            ));
        }

        // Best-effort: Couchbase versions/editions may return different values.
        return Optional.of(new Finding(
                id(),
                Severity.MEDIUM,
                name(),
                "Rebalance status is unusual or unknown: " + status,
                evidence,
                "Verify rebalance state in Couchbase UI and review recent cluster changes."
        ));
    }
}

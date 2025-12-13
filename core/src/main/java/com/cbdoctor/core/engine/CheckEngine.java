package com.cbdoctor.core.engine;

import com.cbdoctor.core.check.Check;
import com.cbdoctor.core.collector.ClusterSnapshot;
import com.cbdoctor.core.model.Finding;
import com.cbdoctor.core.model.Report;
import com.cbdoctor.core.model.Severity;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class CheckEngine {
    private final List<Check> checks;
    private final Clock clock;
    private final boolean emitSkipped;

    public CheckEngine(List<Check> checks, Clock clock, boolean emitSkipped) {
        this.checks = List.copyOf(checks);
        this.clock = clock;
        this.emitSkipped = emitSkipped;
    }

    public Report run(String clusterName, ClusterSnapshot snapshot) {
        List<Finding> findings = new ArrayList<>();

        for (Check check : checks) {
            try {
                check.run(snapshot).ifPresentOrElse(
                        findings::add,
                        () -> {
                            if (emitSkipped) {
                                findings.add(new Finding(
                                        check.id(),
                                        Severity.SKIPPED,
                                        check.name(),
                                        "Check skipped (insufficient data).",
                                        java.util.Map.of(),
                                        "Ensure required endpoints/metrics are accessible and retry."
                                ));
                            }
                        }
                );
            } catch (Exception e) {
                // Fail-safe: report as HIGH (tool should not crash silently)
                findings.add(new Finding(
                        check.id(),
                        Severity.HIGH,
                        check.name(),
                        "Check failed with exception: " + e.getClass().getSimpleName(),
                        java.util.Map.of("error", e.getMessage() == null ? "" : e.getMessage()),
                        "Inspect logs / connectivity and retry."
                ));
            }
        }

        Instant now = clock.instant();
        return new Report(clusterName, now, findings);
    }
}

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
import java.util.Map;

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
        List<CheckResult> findings = new ArrayList<>();

        for (Check check : checks) {
            try {
                CheckResult result = check.run(snapshot);

                if (result == null || result.status() == null) {
                    continue;
                }

                switch (result.status()) {
                    case PASS -> {
                        // Do nothing
                    }
                    case SKIPPED -> {
                        if (emitSkipped && result.finding() != null) {
                            findings.add(result);
                        }
                    }
                    case FINDING -> {
                        if (result.finding() != null) {
                            findings.add(result);
                        }
                    }
                }
            } catch (Exception e) {
                findings.add(CheckResult.finding(
                        new Finding(
                                check.id(),
                                Severity.HIGH,
                                check.name(),
                                "Check failed with exception: " + e.getClass().getSimpleName(),
                                Map.of("error", e.getMessage() == null ? "" : e.getMessage()),
                                "Inspect logs / connectivity and retry."
                        )
                ));
            }
        }

        Instant now = clock.instant();
        return new Report(clusterName, now, findings);
    }
}

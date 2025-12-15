package com.cbdoctor.core.model;

import com.cbdoctor.core.engine.CheckResult;

import java.time.Instant;
import java.util.List;

public record Report(
        String cluster,
        Instant timestamp,
        List<CheckResult> findings
) {
    public boolean hasHighSeverity() {
        return findings.stream().anyMatch(c -> c.finding().severity() == Severity.HIGH);
    }

    public boolean hasMediumSeverity() {
        return findings.stream().anyMatch(c -> c.finding().severity() == Severity.MEDIUM);
    }
}
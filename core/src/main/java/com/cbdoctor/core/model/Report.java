package com.cbdoctor.core.model;

import java.time.Instant;
import java.util.List;

public record Report(
        String cluster,
        Instant timestamp,
        List<Finding> findings
) {
    public boolean hasHighSeverity() {
        return findings.stream().anyMatch(f -> f.severity() == Severity.HIGH);
    }

    public boolean hasMediumSeverity() {
        return findings.stream().anyMatch(f -> f.severity() == Severity.MEDIUM);
    }
}
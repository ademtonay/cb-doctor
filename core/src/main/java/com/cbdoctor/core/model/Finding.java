package com.cbdoctor.core.model;

import java.util.Map;

/**
 * Represents a single check result that should be shown to the user.
 */
public record Finding(
        String id,
        Severity severity,
        String title,
        String message,
        Map<String, Object> evidence,
        String recommendation
) {

    public static Finding skipped(String id, String title, String reason) {
        return new Finding(
                id,
                Severity.SKIPPED,
                title,
                reason,
                Map.of(),
                "Provide required metrics/data sources to enable this check."
        );
    }
}

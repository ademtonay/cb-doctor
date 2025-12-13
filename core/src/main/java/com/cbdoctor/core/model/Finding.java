package com.cbdoctor.core.model;

import java.util.Map;

public record Finding(
        String id,
        Severity severity,
        String title,
        String message,
        Map<String, Object> evidence,
        String recommendation
) {}

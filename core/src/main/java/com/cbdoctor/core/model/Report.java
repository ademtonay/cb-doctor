package com.cbdoctor.core.model;

import java.time.Instant;
import java.util.List;

public record Report(
        String cluster,
        Instant timestamp,
        List<Finding> findings
) {}

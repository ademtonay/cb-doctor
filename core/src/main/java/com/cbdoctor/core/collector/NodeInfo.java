package com.cbdoctor.core.collector;

import java.util.Set;

public record NodeInfo(
        String hostname,
        String ip,
        Set<String> services,
        boolean healthy,
        Double diskUsedPercent // nullable
) {
    public boolean hasService(String service) {
        return services != null && services.contains(service);
    }
}

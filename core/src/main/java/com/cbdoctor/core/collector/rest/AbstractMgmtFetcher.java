package com.cbdoctor.core.collector.rest;

import com.fasterxml.jackson.databind.JsonNode;

public abstract class AbstractMgmtFetcher<T> {
    protected final MgmtRestClient client;

    public abstract T fetch();

    protected AbstractMgmtFetcher(MgmtRestClient client) {
        this.client = java.util.Objects.requireNonNull(client, "client is required");
    }

    protected static Double extractDiskUsedPercent(JsonNode node) {
        if (node == null) return null;

        JsonNode availableStorage = node.get("availableStorage");
        if (availableStorage != null) {
            JsonNode hdd = availableStorage.get("hdd");
            if (hdd != null && hdd.isArray() && !hdd.isEmpty()) {
                Double max = null;
                for (JsonNode entry : hdd) {
                    Double p = number(entry, "usagePercent");
                    if (p != null) {
                        max = (max == null) ? p : Math.max(max, p);
                    }
                }
                if (max != null) return max;
            }
        }

        JsonNode storageTotals = node.get("storageTotals");
        if (storageTotals != null) {
            JsonNode hddTotals = storageTotals.get("hdd");
            if (hddTotals != null) {
                Double used = number(hddTotals, "used");
                Double total = number(hddTotals, "total");
                if (used != null && total != null && total > 0) {
                    return (used / total) * 100.0;
                }
            }
        }

        return null;
    }

    /**
     * Reads a text field with a default fallback.
     */
    protected static String text(JsonNode node, String field, String def) {
        return getString(node, field, def);
    }

    /**
     * Reads a numeric field, allowing both numeric and string values.
     */
    protected static Double number(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) return null;
        if (v.isNumber()) return v.doubleValue();
        if (v.isTextual()) {
            try {
                return Double.parseDouble(v.asText());
            } catch (NumberFormatException ignore) {
                return null;
            }
        }
        return null;
    }

    /**
     * Removes the port from a hostname if present.
     */
    protected static String stripPort(String hostname) {
        if (hostname == null) return "";
        int idx = hostname.lastIndexOf(':');
        if (idx > 0 && idx < hostname.length() - 1) {
            String portPart = hostname.substring(idx + 1);
            if (portPart.chars().allMatch(Character::isDigit)) {
                return hostname.substring(0, idx);
            }
        }
        return hostname;
    }

    protected static String getString(JsonNode node, String field, String def) {
        if (node == null) return def;
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) return def;
        String s = v.asText();
        return (s == null || s.isBlank()) ? def : s;
    }

    /**
     * Reads an integer field with a default fallback.
     */
    protected static int intValue(JsonNode node, String field, int def) {
        if (node == null) return def;
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) return def;
        if (v.isInt()) return v.intValue();
        if (v.isNumber()) return v.numberValue().intValue();
        if (v.isTextual()) {
            try {
                return Integer.parseInt(v.asText());
            } catch (NumberFormatException ignore) {
                return def;
            }
        }
        return def;
    }
}

package com.cbdoctor.core.collector.rest;

import okhttp3.HttpUrl;

import java.util.Locale;

public final class MgmtEndpointResolver {
    private MgmtEndpointResolver() {}

    /**
     * Accepts:
     *  - <a href="http://host:8091">http://host:8091</a>
     *  - <a href="https://host:18091">https://host:18091</a>
     *  - couchbase://host
     *  - couchbases://host
     * and returns a base management URL like http(s)://host:port
     */
    public static HttpUrl resolve(String connectionString) {
        if (connectionString == null || connectionString.isBlank()) {
            throw new IllegalArgumentException("connectionString is required");
        }

        String s = connectionString.trim();
        String lower = s.toLowerCase(Locale.ROOT);

        // If already http/https, use as-is.
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            HttpUrl url = HttpUrl.parse(s);
            if (url == null) throw new IllegalArgumentException("Invalid URL: " + connectionString);

            // If no port explicitly set, default based on scheme.
            int port = url.port();
            if (port == -1) {
                port = url.isHttps() ? 18091 : 8091;
                url = url.newBuilder().port(port).build();
            }
            return url.newBuilder().encodedPath("/").build(); // base (no path)
        }

        // couchbase(s)://host[,host2]?params => take first host
        boolean tls = lower.startsWith("couchbases://");
        String hostPart = s.replaceFirst("(?i)^couchbases?://", "");

        // remove params/query part if any
        int q = hostPart.indexOf('?');
        if (q >= 0) hostPart = hostPart.substring(0, q);

        // if multiple hosts, take first
        String first = hostPart.split(",")[0].trim();

        // if host includes port, keep it; otherwise default mgmt port
        String host;
        int port;
        int colon = first.lastIndexOf(':');
        if (colon > 0 && colon < first.length() - 1 && isNumeric(first.substring(colon + 1))) {
            host = first.substring(0, colon);
            port = Integer.parseInt(first.substring(colon + 1));
        } else {
            host = first;
            port = tls ? 18091 : 8091;
        }

        String scheme = tls ? "https" : "http";
        return new HttpUrl.Builder()
                .scheme(scheme)
                .host(host)
                .port(port)
                .build();
    }

    private static boolean isNumeric(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) return false;
        }
        return !s.isEmpty();
    }
}

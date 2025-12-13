package com.cbdoctor.core.collector.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;

@Slf4j
public final class MgmtRestClient {
    private final OkHttpClient http;
    private final ObjectMapper mapper;
    private final HttpUrl baseUrl;
    private final String authHeaderValue;

    public MgmtRestClient(HttpUrl baseUrl, String username, char[] password, OkHttpClient http, ObjectMapper mapper) {
        if (baseUrl == null) throw new IllegalArgumentException("baseUrl is required");
        if (username == null || username.isBlank()) throw new IllegalArgumentException("username is required");
        if (password == null) throw new IllegalArgumentException("password is required");
        this.baseUrl = baseUrl;

        this.http = (http != null) ? http : defaultHttpClient();
        this.mapper = (mapper != null) ? mapper : new ObjectMapper();
        this.authHeaderValue = Credentials.basic(username, new String(password));

        // Best-effort clear password char[]
        Arrays.fill(password, '\0');
    }

    public static MgmtRestClient fromConnectionString(String connectionString, String username, char[] password) {
        HttpUrl base = MgmtEndpointResolver.resolve(connectionString);
        return new MgmtRestClient(base, username, password, null, null);
    }

    public HttpUrl baseUrl() {
        return baseUrl;
    }

    /** GET /pools/default */
    public JsonNode getPoolsDefault() {
        return getJson(path("/pools/default"));
    }

    /** GET /pools/default/buckets */
    public JsonNode getBuckets() {
        return getJson(path("/pools/default/buckets"));
    }

    public JsonNode getJson(HttpUrl url) {
        Request req = new Request.Builder()
                .url(url)
                .get()
                .header("Authorization", authHeaderValue)
                .header("Accept", "application/json")
                .build();

        try (Response res = http.newCall(req).execute()) {
            int code = res.code();

            if (code == 401 || code == 403) {
                throw new MgmtAuthException("Authentication/authorization failed (HTTP " + code + ") for " + redact(url));
            }
            if (code < 200 || code >= 300) {
                String body = safeBody(res);
                throw new MgmtRestException("Request failed (HTTP " + code + ") for " + redact(url) +
                                            (body.isBlank() ? "" : " - " + truncate(body, 500)));
            }

            String body = safeBody(res);
            if (body.isBlank()) {
                throw new MgmtRestException("Empty response body from " + redact(url));
            }

            return mapper.readTree(body);
        } catch (IOException e) {
            throw new MgmtRestException("I/O error calling " + redact(url) + ": " + e.getMessage(), e);
        }
    }

    private HttpUrl path(String path) {
        // baseUrl might already have /something; ensure we join cleanly
        return baseUrl.newBuilder()
                .encodedPath(path.startsWith("/") ? path : ("/" + path))
                .build();
    }

    private static OkHttpClient defaultHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(10))
                .writeTimeout(Duration.ofSeconds(10))
                .callTimeout(Duration.ofSeconds(15))
                .build();
    }

    private static String safeBody(Response res) throws IOException {
        return res.body().string();
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, max) + "…";
    }

    private static String redact(HttpUrl url) {
        // no credentials in URL anyway; keep it as base+path
        return url.scheme() + "://" + url.host() + ":" + url.port() + url.encodedPath();
    }
}

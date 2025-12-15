# Cb Doctor

Cb Doctor is a lightweight **Couchbase cluster diagnostic tool** that collects a cluster snapshot and runs a set of **health checks** to produce actionable findings.

It is designed to be:
- Deterministic (checks run only on snapshot data)
- Testable (pure logic, no Couchbase dependency in checks)
- Automation-friendly (CLI + API)
- Easy to extend (new checks can be added with minimal boilerplate)

---

## Features (MVP)

- Collects a **ClusterSnapshot** from Couchbase Management APIs
- Runs a set of diagnostic checks
- Produces consistent **Findings** with severity levels:
    - `CRITICAL`
    - `WARN`
    - `INFO`
    - `SKIPPED`
- Outputs results in **GitHub-friendly Markdown tables**
- CLI and API share the same core logic

---

## Architecture Overview
```text
cbdoctor
├── cbdoctor-core
│   ├── collector        # Snapshot collectors
│   ├── check            # Health checks
│   ├── model            # Finding, Severity, snapshot models
│   └── formatter        # Output formatters (Markdown, etc.)
│
├── cbdoctor-cli
│   └── Command-line interface
│
└── cbdoctor-api
    └── REST API
```


### Core Concepts

- **ClusterSnapshot**
    - Immutable representation of cluster state
    - Created once per run
- **Check**
    - Stateless
    - Input: `ClusterSnapshot`
    - Output: `List<Finding>`
- **Finding**
    - `severity`
    - `checkId`
    - `message`

---

## Requirements

- Java **21+**
- Maven or Gradle
- Access to Couchbase Management API (default `8091`)
- Couchbase credentials with read access

---

## Build

### Gradle

```bash
./gradlew clean build
```

## CLI Usage

The CLI collects a live snapshot from a Couchbase cluster, runs all checks, and prints a report.

### Basic Usage
```bash
java -jar cbdoctor-cli.jar \
  --host http://127.0.0.1:8091 \
  --username Administrator \
  --password password
```

### Common Options

| Option       | Description                           |
| ------------ |---------------------------------------|
| `--host`     | Couchbase base URL                    |
| `--username` | Couchbase username                    |
| `--password` | Couchbase password                    |
| `--format`   | Output format (`json`, default: `md`) |
| `--timeout`  | HTTP timeout (optional)               |

### Example CLI Output
| Severity | Check                | Message                                                                 |
|----------|----------------------|-------------------------------------------------------------------------|
| CRITICAL | REPLICA_COVERAGE     | One or more buckets have replicaNumber=0: beer-sample                  |
| PASS     | NODE_REACHABILITY    | All nodes are reachable.                                                |
| PASS     | REBALANCE_STATUS     | No rebalance operation is currently running.                           |
| PASS     | SERVICE_DISTRIBUTION | Services are evenly distributed across nodes.                          |
| SKIPPED  | DISK_RISK            | Check skipped (insufficient data).                                     |

### Checks Included (MVP)
| Check ID               | Description                               |
| ---------------------- | ----------------------------------------- |
| `REPLICA_COVERAGE`     | Buckets without replicas                  |
| `DISK_RISK`            | Disk usage risk (skipped if data missing) |
| `NODE_REACHABILITY`    | Node reachability                         |
| `REBALANCE_STATUS`     | Ongoing rebalance detection               |
| `SERVICE_DISTRIBUTION` | Couchbase service balance across nodes    |

>[!TIP] 
> Checks may return SKIPPED if required snapshot data is unavailable.
 
## Testing

### Unit Tests

Checks are tested in isolation using synthetic snapshots.

Example:

```java
@Test
void shouldReturnEmpty_whenNoNodes() {
    ClusterSnapshot snapshot = new ClusterSnapshot(
        "test",
        Instant.now(),
        "none",
        List.of(),
        List.of(),
        null
    );

    CheckAssertions.assertSkipped(
        new ServiceDistributionCheck().run(snapshot)
    );
}
```

### Run Tests
```bash
./gradlew test
```

## Extending the System

### Adding a New Check

1. Create a new class implementing `Check` 
2. Define: 
   - `checkId`
   - Required snapshot data
3. Return SKIPPED if data is missing
4. Add unit tests
5. Register the check

>[!NOTE]
> The system is intentionally open for extension, closed for modification for now.

## Design Principles
1. Fail safe (prefer SKIPPED over false positives)
2. Immutable snapshot
3. No network calls inside checks
4. CLI and API share the same core logic

## Roadmap (Post-MVP)
1. Node CPU & memory utilization
2. Historical trend analysis
3. JSON schema versioning
4. Pluggable output formats
5. CI integration examples
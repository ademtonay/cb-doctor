package com.cbdoctor.cli.commands;

import com.cbdoctor.cli.commands.utils.ObjectMappers;
import com.cbdoctor.core.check.*;
import com.cbdoctor.core.collector.ClusterSnapshot;
import com.cbdoctor.core.collector.rest.MgmtClusterCollector;
import com.cbdoctor.core.collector.rest.MgmtRestClient;
import com.cbdoctor.core.engine.CheckEngine;
import com.cbdoctor.core.engine.CheckResult;
import com.cbdoctor.core.model.Finding;
import com.cbdoctor.core.model.Report;
import com.cbdoctor.core.model.Severity;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.time.Clock;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * Scans a Couchbase cluster via the Management REST API and prints a report.
 */
@Command(
        name = "scan",
        description = "Scan a Couchbase cluster and report health and risks."
)
public final class ScanCommand implements Callable<Integer> {

    @Option(names = {"--conn"}, required = true,
            description = "Connection string (e.g. http://localhost:8091 or couchbase://host)")
    private String conn;

    @Option(names = {"--username"}, required = true, description = "Couchbase username")
    private String username;

    @Option(
            names = {"--password"},
            interactive = true,
            arity = "0..1",
            description = "Prompt for password (recommended). If omitted, CB_PASSWORD env will be used if present."
    )
    private char[] password;

    @Option(names = {"--format"}, defaultValue = "table", description = "Output format: table|json")
    private String format;

    @Option(names = "--verbose", description = "Show PASS results for checks with no findings.")
    private boolean verbose;

    @Override
    public Integer call() {
        try {
            char[] pass = resolvePassword();

            MgmtRestClient client = MgmtRestClient.fromConnectionString(conn, username, pass);
            MgmtClusterCollector collector = new MgmtClusterCollector(client, Clock.systemUTC());

            ClusterSnapshot snapshot = collector.collect();

            List<Check> checks = defaultChecks();
            Report report = runChecks(snapshot, checks);

            printReport(report, checks, format, verbose);

            return calculateExitCode(report);
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace(System.err);
            return 3;
        }
    }

    private static List<Check> defaultChecks() {
        return List.of(
                new NodeReachabilityCheck(),
                new RebalanceStatusCheck(),
                new ReplicaCoverageCheck(),
                new ServiceDistributionCheck(),
                new DiskRiskCheck()
        );
    }

    private static Report runChecks(ClusterSnapshot snapshot, List<Check> checks) {
        CheckEngine engine = new CheckEngine(
                checks,
                Clock.systemUTC(),
                true // emitSkipped
        );

        String clusterName = Optional.ofNullable(snapshot.clusterName()).orElse("cluster");
        return engine.run(clusterName, snapshot);
    }

    private char[] resolvePassword() {
        if (password != null && password.length > 0) {
            return password;
        }
        String env = System.getenv("CB_PASSWORD");
        if (env != null && !env.isBlank()) {
            return env.toCharArray();
        }
        throw new IllegalArgumentException("Password not provided. Use --password or set CB_PASSWORD.");
    }

    private static void printReport(Report report, List<Check> checks, String format, boolean verbose) throws Exception {
        String f = (format == null) ? "table" : format.trim().toLowerCase(Locale.ROOT);
        if ("json".equals(f)) {
            System.out.println(ObjectMappers.JSON.writerWithDefaultPrettyPrinter().writeValueAsString(report));
        } else {
            printTable(report, checks, verbose);
        }
    }

    private static void printTable(Report report, List<Check> checks, boolean verbose) {
        List<CheckResult> results = (report == null || report.findings() == null)
                ? List.of()
                : report.findings().stream()
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparing((CheckResult r) -> severityRank(r.finding().severity()))
                        .thenComparing(r -> r.finding().id())
                )
                .toList();

        boolean hasAnyOutput = !results.isEmpty();

        if (hasAnyOutput || verbose) {
            System.out.printf("%-8s | %-22s | %s%n", "SEVERITY", "CHECK", "MESSAGE");
            System.out.println("--------------------------------------------------------------------------------");
        }

        for (CheckResult r : results) {
            Finding finding = r.finding();
            System.out.printf(
                    "%-8s | %-22s | %s%n",
                    finding.severity(),
                    finding.id(),
                    finding.message()
            );
        }

        if (verbose) {
            printPassedChecks(report, checks);
        } else if (!hasAnyOutput) {
            System.out.println("No findings.");
        }
    }

    /**
     * Prints "OK" rows for checks that produced no findings.
     * A check is considered PASS if its id is not present in Report.findings().
     */
    private static void printPassedChecks(Report report, List<Check> checks) {
        Set<String> reportedIds = (report == null || report.findings() == null)
                ? Set.of()
                : report.findings().stream()
                .filter(Objects::nonNull)
                .map(r -> r.finding().id())
                .collect(Collectors.toSet());

        for (Check check : checks) {
            if (check == null) continue;

            if (!reportedIds.contains(check.id())) {
                System.out.printf(
                        "%-8s | %-22s | %s%n",
                        "OK",
                        check.id(),
                        "Check passed"
                );
            }
        }
    }

    private static int severityRank(Severity s) {
        return switch (s) {
            case CRITICAL -> 0;
            case HIGH -> 1;
            case MEDIUM -> 2;
            case LOW -> 3;
            case SKIPPED -> 4;
        };
    }

    private static int calculateExitCode(Report report) {
        if (report == null || report.findings() == null) {
            return 0;
        }

        if (report.hasHighSeverity()) return 2;

        if (report.hasMediumSeverity()) return 1;

        return 0;
    }
}

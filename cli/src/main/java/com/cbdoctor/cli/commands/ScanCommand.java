package com.cbdoctor.cli.commands;

import com.cbdoctor.cli.commands.utils.ObjectMappers;
import com.cbdoctor.core.check.*;
import com.cbdoctor.core.collector.ClusterSnapshot;
import com.cbdoctor.core.collector.rest.MgmtClusterCollector;
import com.cbdoctor.core.collector.rest.MgmtRestClient;
import com.cbdoctor.core.model.Finding;
import com.cbdoctor.core.model.Report;
import com.cbdoctor.core.model.Severity;
import com.cbdoctor.core.engine.CheckEngine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * Scans a Couchbase cluster via the Management REST API and prints a report.
 */
@Command(
        name = "scan",
        description = "Scan a Couchbase cluster and report health and risks."
)
public final class ScanCommand implements Callable<Integer> {

    @Option(names = {"--conn"}, required = true, description = "Connection string (e.g. http://localhost:8091 or couchbase://host)")
    private String conn;

    @Option(names = {"--username"}, required = true, description = "Couchbase username")
    private String username;

    @Option(names = {"--password"}, interactive = true, arity = "0..1",
            description = "Prompt for password (recommended). If omitted, CB_PASSWORD env will be used if present.")
    private char[] password;

    @Option(names = {"--format"}, defaultValue = "table", description = "Output format: table|json")
    private String format;

    @Override
    public Integer call() {
        try {
            char[] pass = resolvePassword();

            MgmtRestClient client = MgmtRestClient.fromConnectionString(conn, username, pass);
            MgmtClusterCollector collector = new MgmtClusterCollector(client, Clock.systemUTC());

            ClusterSnapshot snapshot = collector.collect();

            Report report = getReport(snapshot);

            printReport(report, format);

            return calculateExitCode(report);
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            return 3;
        }
    }

    private static Report getReport(ClusterSnapshot snapshot) {
        CheckEngine engine = new CheckEngine(
                List.of(
                        new NodeReachabilityCheck(),
                        new RebalanceStatusCheck(),
                        new ReplicaCoverageCheck(),
                        new ServiceDistributionCheck(),
                        new DiskRiskCheck()
                ),
                Clock.systemUTC(),
                true // emitSkipped
        );

        return engine.run(Optional.ofNullable(snapshot.clusterName()).orElse("cluster"), snapshot);
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

    private static void printReport(Report report, String format) throws Exception {
        String f = (format == null) ? "table" : format.trim().toLowerCase(Locale.ROOT);
        if ("json".equals(f)) {
            System.out.println(ObjectMappers.JSON.writerWithDefaultPrettyPrinter().writeValueAsString(report));
        } else {
            printTable(report);
        }
    }

    private static void printTable(Report report) {
        List<Finding> findings = report.findings().stream()
                .sorted(Comparator.comparing((Finding f) -> severityRank(f.severity()))
                        .thenComparing(Finding::id))
                .toList();

        if (findings.isEmpty()) {
            System.out.println("No findings.");
            return;
        }

        System.out.printf("%-8s | %-22s | %s%n", "SEVERITY", "CHECK", "MESSAGE");
        System.out.println("--------------------------------------------------------------------------------");
        for (Finding f : findings) {
            System.out.printf("%-8s | %-22s | %s%n",
                    f.severity(),
                    f.id(),
                    f.message());
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

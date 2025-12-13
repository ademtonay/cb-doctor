package com.cbdoctor.cli.commands;

import picocli.CommandLine.Command;

/**
 * Placeholder for the scan command.
 * Actual implementation will be added in the next step.
 */
@Command(
        name = "scan",
        description = "Scan a Couchbase cluster and report health and risks."
)
public final class ScanCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("cb-doctor scan: not implemented yet");
    }
}

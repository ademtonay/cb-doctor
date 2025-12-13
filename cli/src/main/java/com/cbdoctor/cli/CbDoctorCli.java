package com.cbdoctor.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * Entry point for the cb-doctor CLI.
 */
@Command(
        name = "cb-doctor",
        mixinStandardHelpOptions = true,
        version = "cb-doctor v0.1",
        description = "Health and risk diagnostics for Couchbase clusters.",
        subcommands = {
                com.cbdoctor.cli.commands.ScanCommand.class
        }
)
public final class CbDoctorCli implements Runnable {

    @Override
    public void run() {
        // If no subcommand is provided, print usage help
        CommandLine.usage(this, System.out);
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new CbDoctorCli()).execute(args);
        System.exit(exitCode);
    }
}

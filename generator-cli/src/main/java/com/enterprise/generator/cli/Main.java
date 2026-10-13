package com.enterprise.generator.cli;

import picocli.CommandLine;

/**
 * Entry point. Delegates to the picocli {@link GenerateCommand}. Kept trivial so the shaded jar
 * boots quickly and so that integration tests can invoke {@link #run(String...)} without
 * involving {@code System.exit}.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        System.exit(run(args));
    }

    /**
     * Run the CLI programmatically. Returns the exit code; does not call {@code System.exit}.
     */
    public static int run(String... args) {
        return new CommandLine(new GenerateCommand()).execute(args);
    }
}

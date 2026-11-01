package org.graalnix.launcher;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ScopeType;

@Command(
        name = "graalnix",
        description = "A Nix language implementation on GraalVM.",
        subcommands = {Format.class, Check.class, Fix.class, Eval.class, DrvPath.class})
public final class Main {
    private static final long STACK_SIZE = 1L << 30;

    @Option(
            names = {"-h", "--help"},
            usageHelp = true,
            scope = ScopeType.INHERIT,
            description = "Show this help and exit.")
    boolean help;

    public static CommandLine commandLine() {
        // A file name starting with @ is a file, not a list of arguments.
        return new CommandLine(new Main()).setExpandAtFiles(false);
    }

    public static void main(String[] argv) throws Exception {
        int[] status = {1};
        Thread t = new Thread(
                null,
                () -> status[0] = commandLine()
                        .setOut(new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true))
                        .setErr(new PrintWriter(new OutputStreamWriter(System.err, StandardCharsets.UTF_8), true))
                        .execute(argv),
                "graalnix",
                STACK_SIZE);
        t.start();
        t.join();
        System.exit(status[0]);
    }
}

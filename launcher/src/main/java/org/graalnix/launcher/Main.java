package org.graalnix.launcher;

import org.graalnix.cli.Check;
import org.graalnix.cli.Fix;
import org.graalnix.cli.Format;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ScopeType;

@Command(
        name = "graalnix",
        description = "A Nix language implementation on GraalVM.",
        subcommands = {Format.class, Check.class, Fix.class, Eval.class, DrvPath.class})
public final class Main {
    @Option(
            names = {"-h", "--help"},
            usageHelp = true,
            scope = ScopeType.INHERIT,
            description = "Show this help and exit.")
    boolean help;

    public static CommandLine commandLine() {
        return org.graalnix.cli.Main.configure(new CommandLine(new Main()));
    }

    public static void main(String[] argv) throws Exception {
        org.graalnix.cli.Main.run(commandLine(), argv);
    }
}

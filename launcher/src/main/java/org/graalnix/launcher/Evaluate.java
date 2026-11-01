package org.graalnix.launcher;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Unmatched;

abstract class Evaluate implements Callable<Integer> {
    @Spec
    CommandSpec spec;

    static final class Input {
        @Option(
                names = {"-E", "--expr"},
                paramLabel = "EXPR",
                description = "Evaluate EXPR.")
        String expr;

        @Parameters(paramLabel = "FILE", description = "Evaluate FILE; a directory stands for its default.nix.")
        Path file;
    }

    @ArgGroup(multiplicity = "1")
    Input input;

    @Option(
            names = {"-A", "--attr"},
            paramLabel = "ATTR",
            description = "Select an attribute path in the result.")
    String attr = "";

    @Option(
            names = "--arg",
            arity = "2",
            hideParamSyntax = true,
            paramLabel = "NAME EXPR",
            description = "Call a function result with NAME bound to EXPR.")
    List<String> args = new ArrayList<>();

    @Option(
            names = "--argstr",
            arity = "2",
            hideParamSyntax = true,
            paramLabel = "NAME VALUE",
            description = "Call a function result with NAME bound to the string VALUE.")
    List<String> argstrs = new ArrayList<>();

    @Option(names = "-I", paramLabel = "PATH", description = "Add to the search path, before NIX_PATH.")
    List<String> searchPath = new ArrayList<>();

    @Option(names = "--system", paramLabel = "SYS", description = "Set builtins.currentSystem.")
    String system;

    @Option(names = "--show-trace", description = "Print the Nix call stack when the evaluation fails.")
    boolean showTrace;

    @Unmatched
    List<String> engineOptions = new ArrayList<>();

    abstract String output();

    /** Nix strings are bytes; arguments arrive as UTF-16, so re-encode them one char per UTF-8 byte. */
    private static String bytes(String s) {
        return new String(s.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
    }

    private static void pairs(String kind, List<String> values, List<String> out) {
        for (int i = 0; i < values.size(); i += 2) {
            out.add(kind + "\u0002" + values.get(i) + "\u0002" + bytes(values.get(i + 1)));
        }
    }

    @Override
    public Integer call() {
        Context.Builder builder = Context.newBuilder("nix").allowAllAccess(true);
        // Measured on nixpkgs: runtime compilation made evaluations 4-10% slower at 1.5x the CPU and memory.
        builder.option("engine.Compilation", "false");
        if (system != null) builder.option("nix.System", system);
        for (String a : engineOptions) {
            if (!a.startsWith("--")) throw new ParameterException(spec.commandLine(), "Unexpected argument: " + a);
            int eq = a.indexOf('=');
            if (eq < 0) {
                builder.option(a.substring(2), "true");
            } else {
                builder.option(a.substring(2, eq), a.substring(eq + 1));
            }
        }
        List<String> path = new ArrayList<>(searchPath);
        String envPath = System.getenv("NIX_PATH");
        if (envPath != null && !envPath.isEmpty()) path.add(envPath);
        List<String> nixArgs = new ArrayList<>();
        pairs("expr", args, nixArgs);
        pairs("str", argstrs, nixArgs);
        builder.option("nix.NixPath", String.join(":", path));
        builder.option("nix.Output", output());
        builder.option("nix.Attr", attr);
        builder.option("nix.Args", String.join("\u0001", nixArgs));

        PrintWriter err = spec.commandLine().getErr();
        PrintStream out = new PrintStream(System.out, true, StandardCharsets.ISO_8859_1);
        Context context;
        try {
            context = builder.build();
        } catch (IllegalArgumentException e) {
            throw new ParameterException(spec.commandLine(), e.getMessage());
        }
        try (Context ctx = context) {
            Source source;
            if (input.expr != null) {
                source = Source.create("nix", bytes(input.expr));
            } else {
                Path p = input.file.toAbsolutePath().normalize();
                if (Files.isDirectory(p)) p = p.resolve("default.nix");
                String content = new String(Files.readAllBytes(p), StandardCharsets.ISO_8859_1);
                source = Source.newBuilder("nix", content, p.toString()).build();
            }
            out.println(ctx.eval(source).asString());
            return 0;
        } catch (PolyglotException e) {
            if (e.isInternalError()) {
                e.printStackTrace(err);
            } else if (e.getCause() instanceof StackOverflowError
                    || e.getMessage() != null && e.getMessage().contains("StackOverflowError")) {
                err.println("error: stack overflow");
            } else {
                err.println("error: " + e.getMessage());
                if (showTrace) {
                    for (PolyglotException.StackFrame f : e.getPolyglotStackTrace()) {
                        if (!f.isGuestFrame() || f.getSourceLocation() == null) continue;
                        var loc = f.getSourceLocation();
                        err.println("       at " + f.getRootName() + " "
                                + loc.getSource().getName() + ":" + loc.getStartLine() + ":" + loc.getStartColumn());
                    }
                }
            }
            return 1;
        } catch (IOException e) {
            err.println("error: " + e.getMessage());
            return 1;
        }
    }
}

package org.graalnix.launcher;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;

public final class Main {
    private static final long STACK_SIZE = 1L << 30;

    private Main() {}

    private static void usage() {
        System.err.println("""
                usage: graalnix eval [--strict] [--json] [--raw] (-E EXPR | FILE) [-A ATTR] [--arg NAME EXPR] [--argstr NAME VALUE]
                       graalnix drv-path (-E EXPR | FILE) [-A ATTR] [--arg NAME EXPR] [--argstr NAME VALUE]

                options: -I PATH       add to the search path (like NIX_PATH)
                         --system SYS  set builtins.currentSystem
                         any --nix.* or Truffle tool option (e.g. --inspect, --cpusampler)""");
        System.exit(2);
    }

    /** Nix strings are bytes; arguments arrive as UTF-16, so re-encode them one char per UTF-8 byte. */
    private static String bytes(String s) {
        return new String(s.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
    }

    public static void main(String[] argv) throws Exception {
        int[] status = {1};
        Thread t = new Thread(null, () -> status[0] = run(argv), "graalnix", STACK_SIZE);
        t.start();
        t.join();
        System.exit(status[0]);
    }

    private static int run(String[] argv) {
        if (argv.length == 0) usage();
        String command = argv[0];
        String output = switch (command) {
            case "eval" -> "eval";
            case "drv-path" -> "drv-path";
            default -> {
                usage();
                yield null;
            }
        };
        String expr = null;
        String file = null;
        String attr = "";
        List<String> args = new ArrayList<>();
        List<String> searchPath = new ArrayList<>();
        boolean showTrace = false;
        Context.Builder builder = Context.newBuilder("nix").allowAllAccess(true);
        for (int i = 1; i < argv.length; i++) {
            String a = argv[i];
            switch (a) {
                case "--strict" -> output = output.equals("eval") ? "strict" : output;
                case "--show-trace" -> showTrace = true;
                case "--json" -> output = "json";
                case "--raw" -> output = "raw";
                case "-E", "--expr" -> expr = argv[++i];
                case "-A", "--attr" -> attr = argv[++i];
                case "--arg" -> args.add("expr\u0002" + argv[++i] + "\u0002" + bytes(argv[++i]));
                case "--argstr" -> args.add("str\u0002" + argv[++i] + "\u0002" + bytes(argv[++i]));
                case "-I" -> searchPath.add(argv[++i]);
                case "--system" -> builder.option("nix.System", argv[++i]);
                default -> {
                    if (a.startsWith("--") && a.contains("=")) {
                        int eq = a.indexOf('=');
                        builder.option(a.substring(2, eq), a.substring(eq + 1));
                    } else if (a.startsWith("--")) {
                        builder.option(a.substring(2), "true");
                    } else if (file == null) {
                        file = a;
                    } else {
                        usage();
                    }
                }
            }
        }
        if ((expr == null) == (file == null)) usage();
        String envPath = System.getenv("NIX_PATH");
        if (envPath != null && !envPath.isEmpty()) searchPath.add(envPath);
        builder.option("nix.NixPath", String.join(":", searchPath));
        builder.option("nix.Output", output);
        builder.option("nix.Attr", attr);
        builder.option("nix.Args", String.join("\u0001", args));

        PrintStream out = new PrintStream(System.out, true, StandardCharsets.ISO_8859_1);
        try (Context ctx = builder.build()) {
            Source source;
            if (expr != null) {
                source = Source.create("nix", bytes(expr));
            } else {
                Path p = Path.of(file).toAbsolutePath().normalize();
                if (Files.isDirectory(p)) p = p.resolve("default.nix");
                String content = new String(Files.readAllBytes(p), StandardCharsets.ISO_8859_1);
                source = Source.newBuilder("nix", content, p.toString()).build();
            }
            out.println(ctx.eval(source).asString());
            return 0;
        } catch (PolyglotException e) {
            if (e.isInternalError()) {
                e.printStackTrace();
            } else if (e.getCause() instanceof StackOverflowError || e.getMessage() != null && e.getMessage().contains("StackOverflowError")) {
                System.err.println("error: stack overflow");
            } else {
                System.err.println("error: " + e.getMessage());
                if (showTrace) {
                    for (PolyglotException.StackFrame f : e.getPolyglotStackTrace()) {
                        if (!f.isGuestFrame() || f.getSourceLocation() == null) continue;
                        var loc = f.getSourceLocation();
                        System.err.println("       at " + f.getRootName() + " " + loc.getSource().getName() + ":" + loc.getStartLine() + ":" + loc.getStartColumn());
                    }
                }
            }
            return 1;
        } catch (IOException e) {
            System.err.println("error: " + e.getMessage());
            return 1;
        }
    }
}

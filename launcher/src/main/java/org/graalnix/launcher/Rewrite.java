package org.graalnix.launcher;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import org.graalnix.parser.ParseException;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * Rewrites files in place, as the treefmt formatter spec asks (https://treefmt.com/latest/reference/formatter-spec/):
 * a file is written only when its content changes, and a file that fails makes the exit status nonzero.
 */
abstract class Rewrite implements Callable<Integer> {
    @Spec
    CommandSpec spec;

    @Mixin
    Style style;

    @Parameters(
            paramLabel = "FILE",
            description = "Files to rewrite, or directories to search for .nix files; standard input to standard output"
                    + " without any.")
    List<Path> files = List.of();

    @Option(
            names = {"-q", "--quiet"},
            description = "Print only errors, not the files rewritten and the summary.")
    boolean quiet;

    abstract String rewrite(String src);

    abstract String verb();

    @Override
    public Integer call() throws IOException {
        PrintWriter out = spec.commandLine().getOut();
        PrintWriter err = spec.commandLine().getErr();
        if (files.isEmpty()) {
            String src;
            try {
                src = Sources.read(System.in);
            } catch (IOException e) {
                err.println("error: " + Sources.message(e));
                return 1;
            }
            String result = rewrite("<stdin>", src, err);
            if (result == null) return 1;
            out.print(result);
            out.flush();
            return 0;
        }
        long start = System.nanoTime();
        int changed = 0;
        int unchanged = 0;
        int failed = 0;
        for (Path p : Sources.expand(files)) {
            try {
                String src = Sources.read(p);
                String result = rewrite(p.toString(), src, err);
                if (result == null) {
                    failed++;
                } else if (result.equals(src)) {
                    unchanged++;
                } else {
                    Files.writeString(p, result, StandardCharsets.UTF_8);
                    if (!quiet) err.println(verb() + " " + p);
                    changed++;
                }
            } catch (IOException e) {
                err.println("error: " + Sources.message(e));
                failed++;
            }
        }
        if (!quiet) {
            err.println(files(changed) + " " + verb() + ", " + unchanged + " unchanged"
                    + (failed > 0 ? ", " + failed + " failed" : "")
                    + " in " + duration(System.nanoTime() - start));
        }
        return failed > 0 ? 1 : 0;
    }

    private static String duration(long nanos) {
        long millis = nanos / 1_000_000;
        return millis < 1000 ? millis + "ms" : String.format(Locale.ROOT, "%.1fs", millis / 1000.0);
    }

    private static String files(int n) {
        return n + (n == 1 ? " file" : " files");
    }

    private String rewrite(String name, String src, PrintWriter err) {
        try {
            return rewrite(src);
        } catch (ParseException e) {
            err.println(name + ":" + Sources.position(src, e.offset) + ": error: " + e.getMessage());
        } catch (IllegalStateException e) {
            err.println(name + ": error: " + e.getMessage());
        }
        return null;
    }
}

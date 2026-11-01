package org.graalnix.launcher;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import org.graalnix.lint.Diagnostic;
import org.graalnix.lint.Linter;
import org.graalnix.lint.Severity;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@Command(
        name = "check",
        description = "Report the linter's findings in Nix files without changing them;"
                + " exit with 1 if any is an error or a warning.")
final class Check implements Callable<Integer> {
    @Spec
    CommandSpec spec;

    @Parameters(
            paramLabel = "FILE",
            description = "Files to check, or directories to search for .nix files; standard input without any.")
    List<Path> files = List.of();

    @Override
    public Integer call() throws IOException {
        PrintWriter out = spec.commandLine().getOut();
        PrintWriter err = spec.commandLine().getErr();
        boolean failed = false;
        if (files.isEmpty()) {
            try {
                failed = check("<stdin>", Sources.read(System.in), out);
            } catch (IOException e) {
                err.println("error: " + Sources.message(e));
                failed = true;
            }
        }
        for (Path p : Sources.expand(files)) {
            try {
                failed |= check(p.toString(), Sources.read(p), out);
            } catch (IOException e) {
                err.println("error: " + Sources.message(e));
                failed = true;
            }
        }
        out.flush();
        return failed ? 1 : 0;
    }

    private static boolean check(String name, String src, PrintWriter out) {
        boolean failed = false;
        for (Diagnostic d : Linter.lint(src)) {
            out.println(d.render(src, name));
            failed |= d.severity() != Severity.HINT;
        }
        return failed;
    }
}

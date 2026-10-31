package org.graalnix.lint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.graalnix.parser.Syntax.*;
import org.graalnix.parser.TokenType;

/** Conventions of nixpkgs for derivations. */
final class Packaging {
    private Packaging() {}

    /** The phases of stdenv's setup.sh, with the suffix of their pre and post hooks. */
    private static final Map<String, String> PHASES = Map.of(
            "unpackPhase", "Unpack",
            "patchPhase", "Patch",
            "configurePhase", "Configure",
            "buildPhase", "Build",
            "checkPhase", "Check",
            "installPhase", "Install",
            "fixupPhase", "Fixup",
            "installCheckPhase", "InstallCheck",
            "distPhase", "Dist");

    static void check(String src, Scope scope, List<Diagnostic> out) {
        for (Node n : scope.nodes) {
            if (!(n instanceof AttrSet s)) continue;
            for (Binding b : s.binds()) {
                if (b instanceof Assign a
                        && a.path().size() == 1
                        && a.path().getFirst().attr() instanceof Ident name
                        && PHASES.get(Scope.name(name)) instanceof String hook
                        && a.value() instanceof Str str) phase(src, name, hook, str, out);
            }
        }
    }

    private static void phase(String src, Ident name, String hook, Str str, List<Diagnostic> out) {
        String body = src.substring(str.open().end(), str.close().start());
        String pre = "runHook pre" + hook;
        String post = "runHook post" + hook;
        boolean hasPre = runs(body, pre);
        boolean hasPost = runs(body, post);
        if (hasPre && hasPost) return;
        String missing = !hasPre && !hasPost ? "'" + pre + "' and '" + post + "'" : "'" + (hasPre ? post : pre) + "'";
        out.add(new Diagnostic(
                Rule.MISSING_PHASE_HOOKS,
                Nodes.start(name),
                Nodes.end(name),
                "'" + Scope.name(name) + "' does not run " + missing,
                fix(str, body, hasPre ? null : pre, hasPost ? null : post)));
    }

    private static boolean runs(String body, String call) {
        return Pattern.compile("\\b" + call + "\\b").matcher(body).find();
    }

    /**
     * Adds the calls on lines of their own, set apart by a blank line, at the indentation of the first line. Only an
     * indented string that starts and ends with a line break has room for them.
     */
    private static List<Edit> fix(Str str, String body, String pre, String post) {
        // The lexer keeps the line break after the opening '' in its token, as Nix's does.
        int last = body.lastIndexOf('\n');
        if (str.open().type() != TokenType.IND_STR_OPEN
                || !str.open().text().endsWith("\n")
                || last < 0
                || !body.substring(last + 1).isBlank()
                || body.isBlank()) return List.of();
        String line = body.lines().filter(l -> !l.isBlank()).findFirst().orElseThrow();
        String indent = line.substring(0, line.length() - line.stripLeading().length());
        int base = str.open().end();
        List<Edit> edits = new ArrayList<>();
        if (pre != null) edits.add(new Edit(base, base, indent + pre + "\n\n"));
        if (post != null) edits.add(new Edit(base + last + 1, base + last + 1, "\n" + indent + post + "\n"));
        return edits;
    }
}

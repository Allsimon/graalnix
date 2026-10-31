package org.graalnix.lint;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.graalnix.parser.Syntax.*;
import org.graalnix.parser.TokenType;
import org.graalnix.store.NixBase32;

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

    /** The attributes that these builders pass on unchanged in {@code finalAttrs}. */
    private static final Set<String> KEPT = Set.of("pname", "version", "src");

    /** The attributes that name a package, which {@code __structuredAttrs} goes after. */
    private static final Set<String> IDENTITY = Set.of("pname", "version", "name");

    /** The flags that the rules below add, which go together after the attributes that name the package. */
    private static final Set<String> FLAGS = Set.of("__structuredAttrs", "strictDeps");

    static void check(String src, Scope scope, List<Diagnostic> out) {
        for (Node n : scope.nodes) {
            if (n instanceof Call c && c.args().getLast() instanceof AttrSet s) finalAttrs(src, scope, c, s, out);
            if (n instanceof Str str) replace(src, str, out);
            if (n instanceof Call c) sriHash(src, scope, c, out);
            if (n instanceof Call c) {
                flag(src, c, Rule.MISSING_STRUCTURED_ATTRS, "__structuredAttrs", Builders.STRUCTURED_ATTRS, out);
                flag(src, c, Rule.MISSING_STRICT_DEPS, "strictDeps", Builders.STRICT_DEPS, out);
            }
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

    private static void finalAttrs(String src, Scope scope, Call call, AttrSet set, List<Diagnostic> out) {
        if (set.rec() == null || set.rec().type() != TokenType.REC) return;
        Boolean stdenv = builder(call.fn());
        if (stdenv == null) return;
        List<Scope.Def> defs = scope.owned.getOrDefault(set, List.of());
        if (defs.stream().allMatch(d -> d.uses.isEmpty())) return;
        int start = Nodes.start(set);
        int end = Nodes.end(set);
        List<Edit> edits = new ArrayList<>();
        edits.add(new Edit(set.rec().start(), set.rec().end(), "(finalAttrs:"));
        edits.add(new Edit(end, end, ")"));
        boolean fixable = true;
        for (Node n : scope.nodes) {
            if (!(n instanceof Ident i) || Nodes.start(i) < start || Nodes.end(i) > end) continue;
            Scope.Def d = scope.resolved.get(i);
            if (d != null && defs.contains(d)) {
                fixable &= stdenv || KEPT.contains(d.name);
                edits.add(new Edit(Nodes.start(i), Nodes.end(i), "finalAttrs." + d.name));
            } else if (Scope.name(i).equals("finalAttrs")) {
                // The new argument would capture it.
                fixable &= d != null && Nodes.start(d.owner) >= start && Nodes.end(d.owner) <= end;
            }
        }
        for (Node n : scope.nodes) {
            if (Nodes.start(n) < start || Nodes.end(n) > end) continue;
            if (n instanceof AttrSet inner) fixable &= inherits(src, scope, inner.binds(), defs, stdenv, edits);
            if (n instanceof Let l) fixable &= inherits(src, scope, l.binds(), defs, stdenv, edits);
        }
        // A `finalAttrs` bound inside the set, as by a nested `mkDerivation (finalAttrs: ...)`, would capture the
        // references rewritten below it.
        for (Scope.Def d : scope.defs) {
            if (!d.name.equals("finalAttrs") || Nodes.start(d.owner) < start || Nodes.end(d.owner) > end) continue;
            for (Edit e : edits.subList(2, edits.size())) {
                if (Nodes.start(d.owner) <= e.start() && e.end() <= Nodes.end(d.owner)) fixable = false;
            }
        }
        out.add(new Diagnostic(
                Rule.MISSING_FINAL_ATTRS,
                set.rec().start(),
                set.open().end(),
                "use 'finalAttrs: { ... }' instead of 'rec { ... }'",
                fixable ? edits : List.of()));
    }

    /** Reports a derivation that does not set {@code attr}, unless its builder sets it to true already. */
    private static void flag(
            String src, Call call, Rule rule, String attr, Set<String> defaults, List<Diagnostic> out) {
        if (builder(call.fn()) == null || defaults.contains(name(call.fn()))) return;
        AttrSet set = switch (call.args().getLast()) {
            case AttrSet s -> s;
            case Paren p when p.expr() instanceof Lambda l && l.body() instanceof AttrSet s -> s;
            default -> null;
        };
        if (set == null || set.rec() != null && set.rec().type() == TokenType.LET) return;
        for (Binding b : set.binds()) if (names(b).contains(attr)) return;
        out.add(new Diagnostic(
                rule,
                Nodes.start(call.fn()),
                Nodes.end(call.fn()),
                "set '" + attr + " = true;'",
                flagFix(src, set, attr + " = true;")));
    }

    /**
     * Inserts {@code line} after the leading bindings that name the package, set apart by a blank line, or right
     * under the flags that follow them; else at the top of the set. It takes the indentation of the bindings.
     */
    private static List<Edit> flagFix(String src, AttrSet set, String line) {
        if (set.binds().isEmpty()) return List.of();
        Binding first = set.binds().getFirst();
        int lineStart = src.lastIndexOf('\n', Nodes.start(first) - 1) + 1;
        String indent = src.substring(lineStart, Nodes.start(first));
        if (!indent.isBlank()) return List.of();
        Binding after = null;
        boolean flags = false;
        for (Binding b : set.binds()) {
            if (b instanceof Assign a && a.path().size() > 1) break;
            List<String> names = names(b);
            boolean flag = names.stream().allMatch(n -> n != null && FLAGS.contains(n));
            if (!flag && (flags || !names.stream().allMatch(n -> n != null && IDENTITY.contains(n)))) break;
            flags = flag;
            after = b;
        }
        if (after == null) {
            // Right after the brace, so that a comment on the first binding stays with it.
            int end = src.indexOf('\n', set.open().end());
            if (end < 0 || !src.substring(set.open().end(), end).isBlank()) return List.of();
            return List.of(new Edit(end + 1, end + 1, indent + line + "\n\n"));
        }
        int end = Nodes.end(after);
        if (end < src.length() && src.charAt(end) != '\n') return List.of();
        return List.of(new Edit(end, end, (flags ? "\n" : "\n\n") + indent + line));
    }

    /** The name a builder is called by, once {@link #builder} has recognized it. */
    private static String name(Node fn) {
        return fn instanceof Select s ? Nodes.staticName(s.path().getLast().attr()) : Scope.name((Ident) fn);
    }

    /** The attributes a binding sets: the first of its path, or the names it inherits. */
    private static List<String> names(Binding b) {
        return switch (b) {
            case Assign a ->
                java.util.Collections.singletonList(
                        Nodes.staticName(a.path().getFirst().attr()));
            case Inherit inh -> inh.names().stream().map(Nodes::staticName).toList();
        };
    }

    /**
     * Whether {@code fn} is a builder that takes {@code finalAttrs:}: true for {@code stdenv.mkDerivation} and its
     * variants, false for the other builders, null otherwise.
     */
    private static Boolean builder(Node fn) {
        if (fn instanceof Ident i) return Builders.FINAL_ATTRS.contains(Scope.name(i)) ? false : null;
        if (!(fn instanceof Select s) || s.fallback() != null) return null;
        String name = Nodes.staticName(s.path().getLast().attr());
        if (name == null) return null;
        if (Builders.FINAL_ATTRS.contains(name)) return false;
        if (!"mkDerivation".equals(name)) return null;
        String owner = s.path().size() > 1
                ? Nodes.staticName(s.path().get(s.path().size() - 2).attr())
                : s.target() instanceof Ident t ? Scope.name(t) : null;
        return owner != null && owner.toLowerCase(Locale.ROOT).contains("stdenv") ? true : null;
    }

    /** Turns the names of plain {@code inherit} that come from the set into {@code inherit (finalAttrs)}. */
    private static boolean inherits(
            String src, Scope scope, List<Binding> binds, List<Scope.Def> defs, boolean stdenv, List<Edit> edits) {
        boolean fixable = true;
        for (Binding b : binds) {
            if (!(b instanceof Inherit inh) || inh.from() != null) continue;
            List<String> kept = new ArrayList<>();
            List<String> moved = new ArrayList<>();
            for (AttrNode a : inh.names()) {
                Scope.Def d = scope.inherited.get(a);
                String text = src.substring(Nodes.attrStart(a), Nodes.attrEnd(a));
                if (d != null && defs.contains(d)) {
                    fixable &= stdenv || KEPT.contains(d.name);
                    moved.add(text);
                } else {
                    kept.add(text);
                }
            }
            if (moved.isEmpty()) continue;
            String text = "inherit (finalAttrs) " + String.join(" ", moved) + ";";
            if (!kept.isEmpty()) text = "inherit " + String.join(" ", kept) + "; " + text;
            edits.add(new Edit(inh.inherit().start(), inh.semi().end(), text));
        }
        return fixable;
    }

    /** Reports {@code sha256} given to a fetcher that takes {@code hash}, and converts its value to SRI. */
    private static void sriHash(String src, Scope scope, Call call, List<Diagnostic> out) {
        if (!(call.args().getLast() instanceof AttrSet set)) return;
        // A fetcher defined in the file, rather than taken from nixpkgs, may take only sha256.
        if (call.fn() instanceof Ident i
                && scope.resolved.get(i) instanceof Scope.Def d
                && (d.kind == Scope.Kind.LET || d.kind == Scope.Kind.REC)) return;
        String fetcher = switch (call.fn()) {
            case Ident i -> Scope.name(i);
            case Select s -> Nodes.staticName(s.path().getLast().attr());
            default -> null;
        };
        if (fetcher == null || !Builders.HASH_FETCHERS.contains(fetcher)) return;
        // builtins.fetchurl and the like take only sha256.
        if (call.fn() instanceof Select s
                && s.target() instanceof Ident t
                && Scope.name(t).equals("builtins")) return;
        AttrNode name = null;
        Node given = null;
        for (Binding b : set.binds()) {
            if (names(b).contains("hash")) return;
            switch (b) {
                case Assign a
                when a.path().size() == 1 && "sha256".equals(names(b).getFirst()) -> {
                    name = a.path().getFirst().attr();
                    given = a.value();
                }
                case Inherit i -> {
                    for (AttrNode a : i.names()) if ("sha256".equals(Nodes.staticName(a))) name = a;
                }
                default -> {}
            }
        }
        if (name == null) return;
        // In a rec set, a sibling may refer to it, as in url = "...${sha256}.deb", and lose it once renamed.
        boolean used = scope.owned.getOrDefault(set, List.of()).stream()
                .anyMatch(d -> d.name.equals("sha256") && !d.uses.isEmpty());
        String value = given == null || used ? null : sri(src, given);
        out.add(new Diagnostic(
                Rule.SRI_HASH,
                Nodes.attrStart(name),
                Nodes.attrEnd(name),
                "use 'hash' with an SRI hash instead of 'sha256'",
                value == null
                        ? List.of()
                        : List.of(
                                new Edit(Nodes.attrStart(name), Nodes.attrEnd(name), "hash"),
                                new Edit(Nodes.start(given), Nodes.end(given), value))));
    }

    /** The value of {@code hash} for {@code sha256 = v}, or null when it is not a literal hash. */
    private static String sri(String src, Node v) {
        if (v instanceof Select s
                && s.fallback() == null
                && "fakeSha256".equals(Nodes.staticName(s.path().getLast().attr()))) {
            String text = Nodes.text(src, s);
            return text.substring(0, text.length() - "fakeSha256".length()) + "fakeHash";
        }
        if (!(v instanceof Str s) || !s.parts().stream().allMatch(p -> p instanceof Lit)) return null;
        StringBuilder sb = new StringBuilder();
        for (Part p : s.parts()) sb.append(((Lit) p).text());
        String hash = sri(sb.toString().strip());
        return hash == null ? null : "\"" + hash + "\"";
    }

    /** Converts a SHA-256 in any of the forms Nix accepts, Nix base32, hex, base64 or SRI, to SRI. */
    static String sri(String hash) {
        if (hash.isEmpty() || hash.startsWith("sha256-")) return hash;
        if (hash.startsWith("sha256:")) hash = hash.substring("sha256:".length());
        byte[] bytes;
        try {
            bytes = switch (hash.length()) {
                case 52 -> NixBase32.decode(hash, 32);
                case 64 -> HexFormat.of().parseHex(hash);
                case 44 -> Base64.getDecoder().decode(hash);
                default -> null;
            };
        } catch (IllegalArgumentException e) {
            return null;
        }
        return bytes == null || bytes.length != 32
                ? null
                : "sha256-" + Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * Reports {@code --replace} in a call to a {@code substitute} function of stdenv's setup.sh. It scans the string
     * as shell code, with quotes and command separators, so that a call over several lines is one command;
     * interpolations count as words.
     */
    private static void replace(String src, Str str, List<Diagnostic> out) {
        boolean indented = str.open().type() == TokenType.IND_STR_OPEN;
        int command = str.open().end();
        char quote = 0;
        boolean comment = false;
        for (Part part : str.parts()) {
            if (!(part instanceof Lit lit)) continue;
            for (int i = lit.start(); i < lit.end(); i++) {
                char c = src.charAt(i);
                if (comment) {
                    if (c == '\n') {
                        comment = false;
                        command = i + 1;
                    }
                    continue;
                }
                if (indented && src.startsWith("''", i) && i + 2 < lit.end()) {
                    // The escapes ''$, ''' and ''\x of an indented string.
                    i += src.charAt(i + 2) == '\\' ? 3 : 2;
                    continue;
                }
                if (c == '\\' && (!indented || quote != '\'')) {
                    i++;
                    continue;
                }
                if (quote != 0) {
                    if (c == quote) quote = 0;
                    continue;
                }
                switch (c) {
                    case '\'', '"' -> quote = c;
                    case '\n', ';', '&', '|' -> command = i + 1;
                    // A comment, whose apostrophes are not quotes.
                    case '#' -> comment = i == command || Character.isWhitespace(src.charAt(i - 1));
                    case '-' -> {
                        if (isReplace(src, i, lit.end())
                                && src.substring(command, i).contains("substitute")) {
                            int end = i + "--replace".length();
                            out.add(new Diagnostic(
                                    Rule.DEPRECATED_REPLACE,
                                    i,
                                    end,
                                    "'--replace' is deprecated; use '--replace-fail'",
                                    List.of(new Edit(i, end, "--replace-fail"))));
                        }
                    }
                    default -> {}
                }
            }
        }
    }

    /** Whether {@code --replace} starts at {@code i} as a word of its own. */
    private static boolean isReplace(String src, int i, int end) {
        int after = i + "--replace".length();
        return src.startsWith("--replace", i)
                && Character.isWhitespace(src.charAt(i - 1))
                && (after == end || Character.isWhitespace(src.charAt(after)) || src.charAt(after) == '\\');
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

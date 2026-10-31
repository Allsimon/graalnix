package org.graalnix.lint;

import static org.graalnix.lint.Severity.*;

public enum Rule {
    SYNTAX_ERROR(
            ERROR,
            "The code does not parse, or breaks a rule Nix checks while parsing, such as defining an attribute twice."
                    + " No other rule runs then."),

    UNUSED_LET_BINDING(
            WARNING,
            "A `let` binding that the body never reaches. A binding used only by itself, or by other unused bindings,"
                    + " counts as unused too. The fix removes it."),
    UNUSED_LAMBDA_ARG(
            WARNING,
            "A function argument that the body never uses. The fix renames it to `_`, unless the body uses an outer"
                    + " `_`."),
    UNUSED_FORMAL(
            WARNING,
            "An attribute of a function's pattern that the body never uses. It is not reported when the `@` binding is"
                    + " used, as that may pass it on. The fix removes it only when the pattern has `+...+`, so that"
                    + " callers passing it still work."),
    UNUSED_AT_PATTERN(WARNING, "An `@` binding that the body never uses. The fix removes it."),

    BOOL_COMPARISON(
            WARNING,
            "A comparison with `true` or `false`. There is no fix: `v == true` is also a type test, where `v` alone"
                    + " fails on other types."),
    BOOL_SIMPLIFICATION(WARNING, "`!(a == b)`, which is `a != b`, and `!(a != b)`, which is `a == b`."),
    EMPTY_LET_IN(WARNING, "A `let` with no bindings."),
    COLLAPSIBLE_LET_IN(
            WARNING,
            "A `let` right in the body of another. The fix merges them unless the outer bindings use or bind a name of"
                    + " the inner ones."),
    LEGACY_LET_SYNTAX(
            WARNING,
            "The deprecated `+let { ...; body = ...; }+`. The fix turns it into `+rec { ...; body = ...; }.body+`."),
    EMPTY_INHERIT(WARNING, "An `inherit` with no names."),
    EMPTY_PATTERN(
            WARNING,
            "A function taking `{ }`. There is no fix: `_:` would also accept arguments that are not an empty"
                    + " attribute set."),
    REDUNDANT_PATTERN_BIND(WARNING, "`+{ ... }@x: body+`, which can be written `x: body`."),
    MANUAL_INHERIT(WARNING, "`a = a;` in an attribute set that is not recursive, which is `inherit a;`."),
    MANUAL_INHERIT_FROM(WARNING, "`a = e.a;`, which is `inherit (e) a;`."),
    ETA_REDUCTION(HINT, "`x: f x`, which is `f` when `f` is a name or an attribute path that does not use `x`."),
    USELESS_PARENS(
            WARNING,
            "Parentheses around a simple expression, or around a whole binding value, `let` body, interpolation or"
                    + " file."),
    UNQUOTED_URI(WARNING, "A URI literal, which is deprecated. The fix quotes it."),
    DEPRECATED_IS_NULL(WARNING, "`isNull e`, which is `e == null`."),
    DEPRECATED_TO_PATH(WARNING, "`builtins.toPath`, which is deprecated. There is no fix."),
    EMPTY_LIST_CONCAT(WARNING, "A concatenation with `[ ]`."),
    USELESS_HAS_ATTR(WARNING, "`if x ? a then x.a else d`, which is `x.a or d`."),
    FASTER_BUILTIN(
            HINT, "`lib.groupBy` and `lib.zipAttrsWith`, which the builtins of the same names implement natively."),
    REPEATED_KEYS(
            HINT,
            "Three or more bindings that start with the same attribute, such as `a.b`, `a.c` and `a.d`, which could"
                    + " form one nested attribute set. There is no fix."),

    UNDEFINED_VARIABLE(
            ERROR,
            "A variable that no binding defines, outside any `with`. Nix rejects the file before evaluating it."),
    UNKNOWN_BUILTIN(
            ERROR,
            "`builtins.x` where `x` is not a builtin, unless the file tests `builtins ? x` or gives an `or` fallback."),
    PREFIXED_BUILTIN(WARNING, "`__x` for the builtin `builtins.x`."),
    SHADOWED_BUILTIN(
            WARNING,
            "A binding that hides a global such as `map`, `true` or `builtins`. Attributes of a `rec` set and names"
                    + " inherited from an expression are not reported."),
    EXTRA_REC(WARNING, "A `rec` attribute set whose attributes never refer to each other."),
    EXTRA_WITH(WARNING, "A `with` that no variable can come from."),

    MISSING_PHASE_HOOKS(
            WARNING,
            "A phase such as `installPhase` set to a string that does not run its hooks: nixpkgs expects"
                    + " `runHook preInstall` first and `runHook postInstall` last, so that `preInstall` and"
                    + " `postInstall` still work. The fix adds the missing calls to strings of several lines."),
    MISSING_FINAL_ATTRS(
            WARNING,
            "A derivation built from a `rec` set whose attributes refer to each other, such as `+stdenv.mkDerivation"
                    + " rec { ... }+`. With `+finalAttrs: { ... }+`, an override of `version` through `overrideAttrs`"
                    + " also reaches the attributes that use it. The fix rewrites the references to"
                    + " `finalAttrs.version` and the like. For builders other than `stdenv.mkDerivation`, `finalAttrs`"
                    + " holds the attributes the builder made, so the fix is only offered when the references are to"
                    + " `pname`, `version` and `src`."),
    MISSING_STRUCTURED_ATTRS(
            HINT,
            "A derivation that does not set `__structuredAttrs`, which nixpkgs expects to be `true` for new packages."
                    + " The Python builders, which default to `true`, are not reported. The fix adds"
                    + " `__structuredAttrs = true;` after `pname` and `version`; it changes the build, since phases"
                    + " then see lists as Bash arrays, so check that the package still builds.");

    private final Severity severity;
    private final String description;

    Rule(Severity severity, String description) {
        this.severity = severity;
        this.description = description;
    }

    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT).replace('_', '-');
    }

    public Severity severity() {
        return severity;
    }

    /** What the rule finds and how it fixes it, in AsciiDoc inline syntax. */
    public String description() {
        return description;
    }
}

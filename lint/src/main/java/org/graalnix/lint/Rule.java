package org.graalnix.lint;

import static org.graalnix.lint.Severity.*;

public enum Rule {
    SYNTAX_ERROR(ERROR),

    UNUSED_LET_BINDING(WARNING),
    UNUSED_LAMBDA_ARG(WARNING),
    UNUSED_FORMAL(WARNING),
    UNUSED_AT_PATTERN(WARNING),

    BOOL_COMPARISON(WARNING),
    BOOL_SIMPLIFICATION(WARNING),
    EMPTY_LET_IN(WARNING),
    COLLAPSIBLE_LET_IN(WARNING),
    LEGACY_LET_SYNTAX(WARNING),
    EMPTY_INHERIT(WARNING),
    EMPTY_PATTERN(WARNING),
    REDUNDANT_PATTERN_BIND(WARNING),
    MANUAL_INHERIT(WARNING),
    MANUAL_INHERIT_FROM(WARNING),
    ETA_REDUCTION(HINT),
    USELESS_PARENS(WARNING),
    UNQUOTED_URI(WARNING),
    DEPRECATED_IS_NULL(WARNING),
    DEPRECATED_TO_PATH(WARNING),
    EMPTY_LIST_CONCAT(WARNING),
    USELESS_HAS_ATTR(WARNING),
    FASTER_BUILTIN(HINT),
    REPEATED_KEYS(HINT),

    UNDEFINED_VARIABLE(ERROR),
    UNKNOWN_BUILTIN(ERROR),
    PREFIXED_BUILTIN(WARNING),
    SHADOWED_BUILTIN(WARNING),
    EXTRA_REC(WARNING),
    EXTRA_WITH(WARNING);

    private final Severity severity;

    Rule(Severity severity) {
        this.severity = severity;
    }

    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT).replace('_', '-');
    }

    public Severity severity() {
        return severity;
    }
}

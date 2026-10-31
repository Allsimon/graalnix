package org.graalnix.runtime;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import java.util.function.Supplier;

public final class Thunk {
    /** Stands in for {@code env} while the thunk is being evaluated. */
    private static final Object EVALUATING = new Object();

    /** A {@link CallTarget} called with {@code env}, a {@link Supplier}, or a function applied to {@code env}. */
    private Object code;

    private Object env;
    private Object value;

    /** A suspended root: forcing calls {@code target} with {@code env} as its only argument. */
    public Thunk(CallTarget target, Object env) {
        this.code = target;
        this.env = env;
    }

    public Thunk(Supplier<Object> supplier) {
        this.code = supplier;
    }

    private Thunk(Object fn, Object arg, boolean application) {
        this.code = fn;
        this.env = arg;
    }

    public static Thunk apply(Object fn, Object arg) {
        return new Thunk(fn, arg, true);
    }

    public static Thunk apply(Object fn, Object a, Object b) {
        return new Thunk(() -> Apply.apply(Apply.apply(fn, a), b));
    }

    public boolean isEvaluated() {
        return value != null;
    }

    public Object force() {
        if (value != null) return value;
        return evaluate();
    }

    @TruffleBoundary
    private Object evaluate() {
        Object e = env;
        if (e == EVALUATING) throw new NixException("infinite recursion encountered" + describeSource());
        env = EVALUATING;
        try {
            Object v = switch (code) {
                case CallTarget target -> target.call(e);
                case Supplier<?> supplier -> supplier.get();
                default -> Apply.apply(code, e);
            };
            value = Thunk.force(v);
            code = null;
            env = null;
            return value;
        } finally {
            if (value == null) env = e;
        }
    }

    private String describeSource() {
        if (code instanceof com.oracle.truffle.api.RootCallTarget rt
                && rt.getRootNode().getSourceSection() != null) {
            var s = rt.getRootNode().getSourceSection();
            return "\n       while evaluating " + rt.getRootNode().getName() + " at "
                    + s.getSource().getName() + ":" + s.getStartLine() + ":" + s.getStartColumn();
        }
        return "";
    }

    public static Object force(Object o) {
        return o instanceof Thunk t ? t.force() : o;
    }
}

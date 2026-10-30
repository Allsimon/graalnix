package org.graalnix.runtime;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import java.util.function.Supplier;

public final class Thunk {
    private CallTarget target;
    private Object env;
    private Supplier<Object> supplier;
    private Object value;
    private boolean evaluating;

    /** A suspended root: forcing calls {@code target} with {@code env} as its only argument. */
    public Thunk(CallTarget target, Object env) {
        this.target = target;
        this.env = env;
    }

    public Thunk(Supplier<Object> supplier) {
        this.supplier = supplier;
    }

    public static Thunk apply(Object fn, Object arg) {
        return new Thunk(() -> Apply.apply(fn, arg));
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
        if (evaluating) throw new NixException("infinite recursion encountered" + describeSource());
        evaluating = true;
        try {
            Object v = target != null ? target.call(env) : supplier.get();
            value = Thunk.force(v);
            target = null;
            env = null;
            supplier = null;
            return value;
        } finally {
            evaluating = false;
        }
    }

    private String describeSource() {
        if (target instanceof com.oracle.truffle.api.RootCallTarget rt && rt.getRootNode().getSourceSection() != null) {
            var s = rt.getRootNode().getSourceSection();
            return "\n       while evaluating " + rt.getRootNode().getName() + " at " + s.getSource().getName() + ":" + s.getStartLine() + ":" + s.getStartColumn();
        }
        return "";
    }

    public static Object force(Object o) {
        return o instanceof Thunk t ? t.force() : o;
    }
}

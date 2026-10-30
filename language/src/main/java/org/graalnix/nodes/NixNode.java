package org.graalnix.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.SourceSection;

public abstract class NixNode extends Node {
    private SourceSection section;

    /** Evaluates to weak head normal form: never returns a {@link org.graalnix.runtime.Thunk}. */
    public abstract Object execute(VirtualFrame frame);

    /** Evaluates for storage in a lazy position; may return a thunk instead of a value. */
    public Object executeLazy(VirtualFrame frame) {
        return execute(frame);
    }

    public final NixNode at(SourceSection s) {
        this.section = s;
        return this;
    }

    @Override
    public SourceSection getSourceSection() {
        return section;
    }
}

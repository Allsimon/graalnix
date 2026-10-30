package org.graalnix.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.graalnix.runtime.NixAttrs;

/** Reads a variable of a {@code scopedImport} scope, passed as the root's second argument. */
public final class ScopeArgNode extends NixNode {
    private final String name;

    public ScopeArgNode(String name) {
        this.name = name;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return ((NixAttrs) frame.getArguments()[1]).getRaw(name);
    }
}

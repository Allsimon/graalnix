package org.graalnix.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.graalnix.runtime.NixException;
import org.graalnix.runtime.Types;

public final class AssertNode extends NixNode {
    @Child private NixNode cond;
    @Child private NixNode body;
    private final String text;

    public AssertNode(NixNode cond, NixNode body, String text) {
        this.cond = cond;
        this.body = body;
        this.text = text;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        if (!Types.bool(cond.execute(frame))) throw failed();
        return body.execute(frame);
    }

    @com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
    private NixException failed() {
        return new NixException("assertion '" + text + "' failed", this, true);
    }
}

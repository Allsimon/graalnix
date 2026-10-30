package org.graalnix.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.graalnix.runtime.Types;

public final class NotNode extends NixNode {
    @Child
    private NixNode operand;

    public NotNode(NixNode operand) {
        this.operand = operand;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return !Types.bool(operand.execute(frame));
    }
}

package org.graalnix.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.graalnix.runtime.Thunk;

public final class ReadVarNode extends NixNode {
    private final int depth;
    private final int slot;

    public ReadVarNode(int depth, int slot) {
        this.depth = depth;
        this.slot = slot;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return Thunk.force(executeLazy(frame));
    }

    @Override
    public Object executeLazy(VirtualFrame frame) {
        return Frames.up(frame, depth).getObject(slot);
    }
}

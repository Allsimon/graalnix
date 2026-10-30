package org.graalnix.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import org.graalnix.runtime.NixList;

public final class ListNode extends NixNode {
    @Children
    private final NixNode[] items;

    public ListNode(NixNode[] items) {
        this.items = items;
    }

    @Override
    @ExplodeLoop
    public Object execute(VirtualFrame frame) {
        Object[] out = new Object[items.length];
        for (int i = 0; i < items.length; i++) out[i] = items[i].executeLazy(frame);
        return new NixList(out);
    }
}

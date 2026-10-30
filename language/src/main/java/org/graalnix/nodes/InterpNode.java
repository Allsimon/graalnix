package org.graalnix.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import java.util.Set;
import org.graalnix.runtime.Coerce;
import org.graalnix.runtime.NixException;
import org.graalnix.runtime.NixPath;
import org.graalnix.runtime.NixString;

/** String interpolation, or path interpolation when the first part is a path. */
public final class InterpNode extends NixNode {
    @Children private final NixNode[] parts;
    private final boolean isPath;

    public InterpNode(NixNode[] parts, boolean isPath) {
        this.parts = parts;
        this.isPath = isPath;
    }

    @Override
    @ExplodeLoop
    public Object execute(VirtualFrame frame) {
        StringBuilder sb = new StringBuilder();
        Set<String> ctx = null;
        for (int i = 0; i < parts.length; i++) {
            Object v = parts[i].execute(frame);
            if (isPath && i == 0) {
                sb.append(((NixPath) v).path());
                continue;
            }
            NixString s = Coerce.toNixString(v, false, !isPath);
            sb.append(s.value);
            ctx = NixString.merge(ctx, s.context);
        }
        if (isPath) {
            if (ctx != null) throw new NixException("a string that refers to a store path cannot be appended to a path", this);
            return new NixPath(Coerce.canonicalize(sb.toString()));
        }
        return new NixString(sb.toString(), ctx);
    }
}

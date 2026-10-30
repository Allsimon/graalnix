package org.graalnix.builtins;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import org.graalnix.runtime.*;

final class AttrSets {
    private AttrSets() {}

    static void register(Builtins b) {
        b.add("attrNames", 1, a -> {
            NixAttrs s = Types.attrs(a[0]);
            Object[] out = new Object[s.size()];
            for (int i = 0; i < out.length; i++) out[i] = new NixString(s.keys[i]);
            return new NixList(out);
        });
        b.add("attrValues", 1, a -> new NixList(Types.attrs(a[0]).values.clone()));
        b.add("getAttr", 2, a -> {
            String name = Types.string(a[0]).value;
            Object v = Types.attrs(a[1]).get(name);
            if (v == null) throw new NixException("attribute '" + name + "' missing");
            return v;
        });
        b.add("hasAttr", 2, a -> Types.attrs(a[1]).has(Types.string(a[0]).value));
        b.add("removeAttrs", 2, a -> {
            NixAttrs s = Types.attrs(a[0]);
            Set<String> drop = new HashSet<>();
            for (Object x : Types.list(a[1]).items) drop.add(Types.string(x).value);
            List<Integer> keep = new ArrayList<>();
            for (int i = 0; i < s.size(); i++) if (!drop.contains(s.keys[i])) keep.add(i);
            if (keep.size() == s.size()) return s;
            return subset(s, keep);
        });
        b.add("listToAttrs", 1, a -> {
            TreeMap<String, Object> m = new TreeMap<>();
            for (Object x : Types.list(a[0]).items) {
                NixAttrs e = Types.attrs(x);
                Object name = e.get("name");
                if (name == null) throw new NixException("attribute 'name' missing");
                Object value = e.getRaw("value");
                if (value == null) throw new NixException("attribute 'value' missing");
                m.putIfAbsent(Types.string(name).value, value);
            }
            return NixAttrs.of(m);
        });
        b.add("intersectAttrs", 2, a -> {
            NixAttrs x = Types.attrs(a[0]);
            NixAttrs y = Types.attrs(a[1]);
            List<Integer> keep = new ArrayList<>();
            for (int i = 0; i < y.size(); i++) if (x.has(y.keys[i])) keep.add(i);
            return subset(y, keep);
        });
        b.add("catAttrs", 2, a -> {
            String name = Types.string(a[0]).value;
            List<Object> out = new ArrayList<>();
            for (Object x : Types.list(a[1]).items) {
                Object v = Types.attrs(x).getRaw(name);
                if (v != null) out.add(v);
            }
            return new NixList(out.toArray());
        });
        b.add("mapAttrs", 2, a -> {
            NixAttrs s = Types.attrs(a[1]);
            Object[] vals = new Object[s.size()];
            for (int i = 0; i < vals.length; i++) vals[i] = Thunk.apply(a[0], new NixString(s.keys[i]), s.values[i]);
            return new NixAttrs(s.keys, vals, s.positions);
        });
        b.add("zipAttrsWith", 2, a -> {
            TreeMap<String, List<Object>> m = new TreeMap<>();
            for (Object x : Types.list(a[1]).items) {
                NixAttrs s = Types.attrs(x);
                for (int i = 0; i < s.size(); i++)
                    m.computeIfAbsent(s.keys[i], k -> new ArrayList<>()).add(s.values[i]);
            }
            TreeMap<String, Object> out = new TreeMap<>();
            m.forEach((k, v) -> out.put(k, Thunk.apply(a[0], new NixString(k), new NixList(v.toArray()))));
            return NixAttrs.of(out);
        });
        b.add("functionArgs", 1, a -> {
            Object f = Types.function(a[0]);
            if (!(f instanceof NixLambda l) || l.info.formals() == null) return NixAttrs.EMPTY;
            TreeMap<String, Integer> order = new TreeMap<>();
            for (int i = 0; i < l.info.formals().length; i++) order.put(l.info.formals()[i], i);
            String[] k = order.keySet().toArray(new String[0]);
            Object[] v = new Object[k.length];
            var p = new com.oracle.truffle.api.source.SourceSection[k.length];
            for (int i = 0; i < k.length; i++) {
                int j = order.get(k[i]);
                v[i] = l.info.hasDefault()[j];
                p[i] = l.info.formalPositions()[j];
            }
            return new NixAttrs(k, v, p);
        });
    }

    static NixAttrs subset(NixAttrs s, List<Integer> keep) {
        String[] k = new String[keep.size()];
        Object[] v = new Object[keep.size()];
        var p = s.positions == null ? null : new com.oracle.truffle.api.source.SourceSection[keep.size()];
        for (int i = 0; i < k.length; i++) {
            k[i] = s.keys[keep.get(i)];
            v[i] = s.values[keep.get(i)];
            if (p != null) p[i] = s.positions[keep.get(i)];
        }
        return new NixAttrs(k, v, p);
    }
}

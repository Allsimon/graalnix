package org.graalnix.runtime;

import com.oracle.truffle.api.source.SourceSection;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

public final class NixAttrs {
    public static final NixAttrs EMPTY = new NixAttrs(new String[0], new Object[0], null);

    /** Sorted by byte order, which is what {@code attrNames} and iteration expose. */
    public final String[] keys;
    public final Object[] values;
    /** Definition sites for {@code unsafeGetAttrPos}; null when unknown. */
    public final SourceSection[] positions;

    public NixAttrs(String[] keys, Object[] values, SourceSection[] positions) {
        this.keys = keys;
        this.values = values;
        this.positions = positions;
    }

    public static NixAttrs of(Map<String, Object> map) {
        TreeMap<String, Object> sorted = map instanceof TreeMap<String, Object> t ? t : new TreeMap<>(map);
        return new NixAttrs(sorted.keySet().toArray(new String[0]), sorted.values().toArray(), null);
    }

    public int size() {
        return keys.length;
    }

    /** Same contract as {@link Arrays#binarySearch(Object[], Object)}, without the megamorphic {@code compareTo}. */
    public int indexOf(String key) {
        int lo = 0;
        int hi = keys.length - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            int c = keys[mid].compareTo(key);
            if (c < 0) {
                lo = mid + 1;
            } else if (c > 0) {
                hi = mid - 1;
            } else {
                return mid;
            }
        }
        return -(lo + 1);
    }

    /** Returns the raw (possibly unevaluated) value, or null if absent. */
    public Object getRaw(String key) {
        int i = indexOf(key);
        return i >= 0 ? values[i] : null;
    }

    public Object get(String key) {
        int i = indexOf(key);
        return i >= 0 ? Thunk.force(values[i]) : null;
    }

    public SourceSection position(String key) {
        int i = indexOf(key);
        return i >= 0 && positions != null ? positions[i] : null;
    }

    public boolean has(String key) {
        return indexOf(key) >= 0;
    }

    public NixAttrs update(NixAttrs right) {
        if (right.size() == 0) return this;
        if (size() == 0) return right;
        int n = keys.length + right.keys.length;
        String[] k = new String[n];
        Object[] v = new Object[n];
        SourceSection[] p = positions != null || right.positions != null ? new SourceSection[n] : null;
        int i = 0, j = 0, o = 0;
        while (i < keys.length || j < right.keys.length) {
            int c = i == keys.length ? 1 : j == right.keys.length ? -1 : keys[i].compareTo(right.keys[j]);
            if (c < 0) {
                k[o] = keys[i];
                v[o] = values[i];
                if (p != null && positions != null) p[o] = positions[i];
                i++;
            } else {
                k[o] = right.keys[j];
                v[o] = right.values[j];
                if (p != null && right.positions != null) p[o] = right.positions[j];
                j++;
                if (c == 0) i++;
            }
            o++;
        }
        return new NixAttrs(Arrays.copyOf(k, o), Arrays.copyOf(v, o), p == null ? null : Arrays.copyOf(p, o));
    }

    public boolean isDerivation() {
        Object t = get("type");
        return t instanceof NixString s && s.value.equals("derivation");
    }
}

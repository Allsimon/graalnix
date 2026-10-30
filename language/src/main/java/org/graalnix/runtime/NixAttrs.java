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
        return new NixAttrs(
                sorted.keySet().toArray(new String[0]), sorted.values().toArray(), null);
    }

    public int size() {
        return keys.length;
    }

    /** Same contract as {@link Arrays#binarySearch(Object[], Object)}, without the megamorphic {@code compareTo}. */
    public int indexOf(String key) {
        return indexOf(key, 0, keys.length - 1);
    }

    private int indexOf(String key, int lo, int hi) {
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
        int i = 0, o = 0;
        for (int j = 0; j < right.keys.length; j++) {
            int at = gallop(right.keys[j], i);
            int end = at >= 0 ? at : -(at + 1);
            o = copyRun(i, end, k, v, p, o);
            k[o] = right.keys[j];
            v[o] = right.values[j];
            if (p != null && right.positions != null) p[o] = right.positions[j];
            o++;
            i = at >= 0 ? end + 1 : end;
        }
        o = copyRun(i, keys.length, k, v, p, o);
        if (o == n) return new NixAttrs(k, v, p);
        return new NixAttrs(Arrays.copyOf(k, o), Arrays.copyOf(v, o), p == null ? null : Arrays.copyOf(p, o));
    }

    /** {@link #indexOf} restricted to keys from {@code from}, cheap when the key lies close to it. */
    private int gallop(String key, int from) {
        int bound = 1;
        while (from + bound <= keys.length && keys[from + bound - 1].compareTo(key) < 0) bound <<= 1;
        return indexOf(key, from + (bound >> 1), Math.min(from + bound, keys.length) - 1);
    }

    private int copyRun(int from, int to, String[] k, Object[] v, SourceSection[] p, int o) {
        int length = to - from;
        System.arraycopy(keys, from, k, o, length);
        System.arraycopy(values, from, v, o, length);
        if (p != null && positions != null) System.arraycopy(positions, from, p, o, length);
        return o + length;
    }

    public boolean isDerivation() {
        Object t = get("type");
        return t instanceof NixString s && s.value.equals("derivation");
    }
}

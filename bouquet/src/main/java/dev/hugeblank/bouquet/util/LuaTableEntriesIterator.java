package dev.hugeblank.bouquet.util;

import org.squiddev.cobalt.*;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

// LuaValue k = Constants.NIL;
// while (true) {
//     Varargs n = table.next(k);
//     if ((k = n.arg(1)).isNil())
//         break;
//     LuaValue v = n.arg(2);
//
//     builder = builder.add(convertTo(outOps, k), convertTo(outOps, v));
// }

public class LuaTableEntriesIterator implements Iterator<Map.Entry<LuaValue, LuaValue>> {
    private final LuaTable table;
    private LuaValue currentKey = Constants.NIL;

    public static Iterable<Map.Entry<LuaValue, LuaValue>> iterate(LuaTable table) {
        return () -> new LuaTableEntriesIterator(table);
    }

    public LuaTableEntriesIterator(LuaTable table) {
        this.table = table;
    }

    @Override
    public boolean hasNext() {
        try {
            return table.next(currentKey).arg(1).isNil();
        } catch (LuaError e) {
            throw new RuntimeException("we love LuaError", e);
        }
    }

    @Override
    public Map.Entry<LuaValue, LuaValue> next() {
        try {
            Varargs n = table.next(currentKey);

            if ((this.currentKey = n.arg(1)).isNil())
                throw new NoSuchElementException();

            LuaValue v = n.arg(2);

            return Map.entry(this.currentKey, v);
        } catch (LuaError e) {
            throw new RuntimeException("we love LuaError", e);
        }
    }
}

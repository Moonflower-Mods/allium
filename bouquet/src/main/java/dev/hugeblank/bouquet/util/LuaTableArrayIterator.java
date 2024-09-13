package dev.hugeblank.bouquet.util;

import org.squiddev.cobalt.LuaTable;
import org.squiddev.cobalt.LuaValue;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class LuaTableArrayIterator implements Iterator<LuaValue> {
    private final LuaTable table;
    private int currentIndex = 0;

    public static Iterable<LuaValue> iterate(LuaTable table) {
        return () -> new LuaTableArrayIterator(table);
    }

    public LuaTableArrayIterator(LuaTable table) {
        this.table = table;
    }

    @Override
    public boolean hasNext() {
        return table.length() > currentIndex;
    }

    @Override
    public LuaValue next() {
        currentIndex += 1;

        LuaValue value = table.rawget(currentIndex);
        if (value.isNil()) throw new NoSuchElementException();
        return value;
    }
}

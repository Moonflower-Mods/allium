package dev.hugeblank.bouquet.util;

import org.squiddev.cobalt.*;

public class TableHelpers {
    public static void forEach(LuaTable table, LuaBiConsumer<LuaValue, LuaValue> consumer) throws LuaError {
        LuaValue key = Constants.NIL;
        while (true) {
            Varargs entry = table.next(key);
            key = entry.arg(1);
            if (key == Constants.NIL) break;
            consumer.accept(key, entry.arg(2));
        }
    }

    public static boolean isArray(LuaTable table) throws LuaError {
        LuaValue key = Constants.NIL;
        while (true) {
            Varargs entry = table.next(key);
            key = entry.arg(1);
            if (key == Constants.NIL) break;
            if (!(key instanceof LuaInteger)) return false;
        }
        return true;
    }

    // Preserve ordering, stop at nil value
    public static void forEachI(LuaTable table, LuaBiConsumer<Integer, LuaValue> consumer) throws LuaError {
        if (!isArray(table)) return;
        int i = 1;
        LuaValue val = table.rawget(i);
        while (val != Constants.NIL) {
            consumer.accept(i, val);
            val = table.rawget(++i);
        }
    }

    public interface LuaBiConsumer<T, U> {
        void accept(T t, U u) throws LuaError;
    }
}

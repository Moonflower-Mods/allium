package dev.hugeblank.bouquet.api.lib.codec;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.*;
import dev.hugeblank.bouquet.util.LuaTableArrayIterator;
import dev.hugeblank.bouquet.util.LuaTableEntriesIterator;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import org.jetbrains.annotations.NotNull;
import org.squiddev.cobalt.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class LuaOps implements DynamicOps<LuaValue> {
    public static final LuaOps INSTANCE = new LuaOps();

    protected LuaOps() { }

    @Override
    public LuaValue empty() {
        return Constants.NIL;
    }

    @Override
    public <U> U convertTo(DynamicOps<U> outOps, LuaValue input) {
        switch (input) {
            case LuaNil ignored -> {
                return outOps.empty();
            }
            case LuaBoolean bool -> {
                return outOps.createBoolean(bool.value);
            }
            case LuaInteger integer -> {
                return outOps.createInt(integer.v);
            }
            case LuaDouble dbl -> {
                return outOps.createDouble(dbl.v);
            }
            case LuaString str -> {
                return outOps.createString(str.toString());
            }
            case LuaTable table -> {
                RecordBuilder<U> builder = outOps.mapBuilder();

                for (Map.Entry<LuaValue, LuaValue> entry : LuaTableEntriesIterator.iterate(table)) {
                    builder = builder.add(convertTo(outOps, entry.getKey()), convertTo(outOps, entry.getValue()));
                }

                return builder.build(outOps.empty()).getOrThrow();
            }
            case LuaUserdata userdata -> {
                // Try to automatically pick up NbtElement/JsonElement stuff.

                if (userdata.toUserdata() instanceof NbtElement nbt)
                    return NbtOps.INSTANCE.convertTo(outOps, nbt);
                else if (userdata.toUserdata() instanceof JsonElement json)
                    return JsonOps.INSTANCE.convertTo(outOps, json);
            }
            default -> throw new IllegalStateException("Don't know how to convert " + input);
        }

        throw new IllegalStateException("Don't know how to convert " + input);
    }

    @Override
    public DataResult<Number> getNumberValue(LuaValue input) {
        if (input instanceof LuaInteger integer) {
            return DataResult.success(integer.v);
        } else if (input instanceof LuaDouble dbl) {
            return DataResult.success(dbl.v);
        } else {
            return DataResult.error(() -> "Not a number: " + input);
        }
    }

    @Override
    public LuaValue createNumeric(Number i) {
        return switch (i) {
            case Integer integer -> LuaInteger.valueOf(integer);
            case Long l -> LuaInteger.valueOf(l);
            default -> LuaDouble.valueOf(i.doubleValue());
        };
    }

    @Override
    public DataResult<String> getStringValue(LuaValue input) {
        String str = input.toString();

        if (str == null) return DataResult.error(() -> "Not a string: " + input);

        return DataResult.success(str);
    }

    @Override
    public LuaValue createString(String value) {
        return LuaString.valueOf(value);
    }

    @Override
    public DataResult<LuaValue> mergeToList(LuaValue list, LuaValue value) {
        LuaTable copy;

        switch (list) {
            case LuaNil ignored -> copy = new LuaTable();
            case LuaTable old -> copy = copyTable(old);
            default -> {
                return DataResult.error(() -> "mergeToList called with not a LuaTable: " + list, list);
            }
        }

        copy.rawset(copy.length() + 1, value);
        return DataResult.success(copy);
    }

    @Override
    public DataResult<LuaValue> mergeToList(LuaValue list, List<LuaValue> values) {
        LuaTable copy;

        switch (list) {
            case LuaNil ignored -> copy = new LuaTable();
            case LuaTable old -> copy = copyTable(old);
            default -> {
                return DataResult.error(() -> "mergeToList called with not a LuaTable: " + list, list);
            }
        }

        for (LuaValue value : values) {
            copy.rawset(copy.length() + 1, value);
        }

        return DataResult.success(copy);
    }

    @Override
    public DataResult<LuaValue> mergeToMap(LuaValue map, LuaValue key, LuaValue value) {
        LuaTable copy;

        switch (map) {
            case LuaNil ignored -> copy = new LuaTable();
            case LuaTable old -> copy = copyTable(old);
            default -> {
                return DataResult.error(() -> "mergeToMap called with not a LuaTable: " + map, map);
            }
        }

        copy.rawset(key, value);
        return DataResult.success(copy);
    }

    @Override
    public DataResult<LuaValue> mergeToMap(LuaValue map, Map<LuaValue, LuaValue> values) {
        LuaTable copy;

        switch (map) {
            case LuaNil ignored -> copy = new LuaTable();
            case LuaTable old -> copy = copyTable(old);
            default -> {
                return DataResult.error(() -> "mergeToMap called with not a LuaTable: " + map, map);
            }
        }

        for (Map.Entry<LuaValue, LuaValue> entry : values.entrySet()) {
            copy.rawset(entry.getKey(), entry.getValue());
        }

        return DataResult.success(copy);
    }

    @Override
    public DataResult<LuaValue> mergeToMap(LuaValue map, MapLike<LuaValue> values) {
        LuaTable copy;

        switch (map) {
            case LuaNil ignored -> copy = new LuaTable();
            case LuaTable old -> copy = copyTable(old);
            default -> {
                return DataResult.error(() -> "mergeToMap called with not a LuaTable: " + map, map);
            }
        }

        values.entries().forEach(entry -> copy.rawset(entry.getFirst(), entry.getSecond()));

        return DataResult.success(copy);
    }

    @Override
    public DataResult<Stream<Pair<LuaValue, LuaValue>>> getMapValues(LuaValue input) {
        if (!(input instanceof LuaTable table)) return DataResult.error(() -> "Not a LuaTable: " + input);

        return DataResult.success(StreamSupport.stream(LuaTableEntriesIterator.iterate(table).spliterator(), false)
            .map(x -> new Pair<>(x.getKey(), x.getValue())));
    }

    @Override
    public DataResult<MapLike<LuaValue>> getMap(LuaValue input) {
        if (!(input instanceof LuaTable table)) return DataResult.error(() -> "Not a LuaTable: " + input);

        return DataResult.success(new MapLike<>() {
            @Override
            public LuaValue get(LuaValue key) {
                return table.rawget(key);
            }

            @Override
            public LuaValue get(String key) {
                return table.rawget(key);
            }

            @Override
            public Stream<Pair<LuaValue, LuaValue>> entries() {
                return StreamSupport.stream(LuaTableEntriesIterator.iterate(table).spliterator(), false)
                    .map(x -> new Pair<>(x.getKey(), x.getValue()));
            }
        });
    }

    @Override
    public LuaValue createMap(Stream<Pair<LuaValue, LuaValue>> map) {
        LuaTable table = new LuaTable();

        map.forEach(x -> table.rawset(x.getFirst(), x.getSecond()));

        return table;
    }

    @Override
    public DataResult<Stream<LuaValue>> getStream(LuaValue input) {
        if (!(input instanceof LuaTable table)) return DataResult.error(() -> "Not a LuaTable: " + input);

        List<LuaValue> values = new ArrayList<>(table.length());

        for (var value : LuaTableArrayIterator.iterate(table)) values.add(value);

        return DataResult.success(values.stream());
    }

    @Override
    public LuaValue createList(Stream<LuaValue> input) {
        Spliterator<LuaValue> spliterator = input.spliterator();

        LuaTable table;

        if (spliterator.estimateSize() == Long.MAX_VALUE) {
            table = new LuaTable();
        } else {
            table = new LuaTable((int) spliterator.estimateSize(), 0);
        }

        spliterator.forEachRemaining(x -> table.rawset(table.length() + 1, x));

        return table;
    }

    @Override
    public LuaValue remove(LuaValue input, String key) {
        if (!(input instanceof LuaTable table)) return input;

        LuaTable copy = copyTable(table);

        copy.rawset(key, Constants.NIL);

        return copy;
    }

    // TODO: add other methods that are apparently important for performance

    private static @NotNull LuaTable copyTable(LuaTable old) {
        LuaTable copied = new LuaTable();

        for (Map.Entry<LuaValue, LuaValue> entry : LuaTableEntriesIterator.iterate(old)) {
            copied.rawset(entry.getKey(), entry.getValue());
        }
        return copied;
    }

    @Override
    public String toString() {
        return "Lua";
    }
}

package dev.hugeblank.bouquet.api.lib.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import dev.hugeblank.allium.loader.type.InvalidArgumentException;
import dev.hugeblank.allium.loader.type.TypeCoercions;
import dev.hugeblank.allium.loader.type.annotation.LuaStateArg;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import net.minecraft.util.dynamic.Codecs;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaValue;
import org.squiddev.cobalt.function.LuaFunction;

import java.util.function.Function;

/**
 * Provides utilities for making DataFixerUpper {@link Codec}s.
 */
@LuaWrapped(name = "codec")
public class CodecLib {
    public static final DynamicOps<LuaValue> LUA_OPS = LuaOps.INSTANCE;

    public static final Codec<LuaValue> LUA_VALUE_CODEC = Codecs.fromOps(LUA_OPS);

    @LuaWrapped
    public static Codec<LuaValue> recordCodec(LuaFunction constructor, ForGetter... args) {
        throw new UnsupportedOperationException("TODO!");
    }

    @SuppressWarnings("unchecked")
    public static <T> MapCodec<LuaValue> toLua(MapCodec<T> codec, EClass<T> klass, @LuaStateArg LuaState state) {
        return codec.flatXmap(
            x -> DataResult.success(TypeCoercions.toLuaValue(x, klass)),
            x -> {
                try {
                    return DataResult.success((T) TypeCoercions.toJava(state, x, klass));
                } catch (LuaError | InvalidArgumentException e) {
                    return DataResult.error(() -> "Lua->Java coercion failed! " + e);
                }
            }
        );
    }

//    public static <T> Codec<T> toJava(Codec<LuaValue> codec, EClass<T> klass) {
//
//    }

    @LuaWrapped
    public static class ForGetter {
        private final MapCodec<LuaValue> value;
        private final Function<LuaValue, LuaValue> getter;

        private ForGetter(MapCodec<LuaValue> value, Function<LuaValue, LuaValue> getter) {
            this.value = value;
            this.getter = getter;
        }
    }
}

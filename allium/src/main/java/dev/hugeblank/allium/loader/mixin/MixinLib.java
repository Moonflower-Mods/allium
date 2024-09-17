package dev.hugeblank.allium.loader.mixin;

import dev.hugeblank.allium.api.WrappedLuaLibrary;
import dev.hugeblank.allium.api.event.MixinEventType;
import dev.hugeblank.allium.loader.Script;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import net.minecraft.util.Identifier;
import org.squiddev.cobalt.LuaError;

@LuaWrapped(name = "mixin")
public class MixinLib implements WrappedLuaLibrary {
    private final Script script;

    public MixinLib(Script script) {
        this.script = script;
    }

    @LuaWrapped
    public static MixinEventType get(Identifier eventId) {
        return MixinEventType.EVENT_MAP.get(eventId);
    }

    @LuaWrapped
    public MixinClassBuilder asClass(String targetClass) throws LuaError {
        return new MixinClassBuilder(targetClass, false, script);
    }

    @LuaWrapped
    public MixinClassBuilder asInterface(String targetClass) throws LuaError {
        return new MixinClassBuilder(targetClass, true, script);
    }
}

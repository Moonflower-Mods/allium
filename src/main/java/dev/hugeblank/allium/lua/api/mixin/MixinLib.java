package dev.hugeblank.allium.lua.api.mixin;

import dev.hugeblank.allium.loader.Script;
import dev.hugeblank.allium.lua.api.WrappedLuaLibrary;
import dev.hugeblank.allium.lua.event.MixinEventType;
import dev.hugeblank.allium.lua.type.annotation.LuaWrapped;
import net.minecraft.util.Identifier;
import org.squiddev.cobalt.LuaError;

import java.io.IOException;

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
    public MixinClassBuilder asClass(String targetClass) throws IOException, LuaError {
        return new MixinClassBuilder(targetClass, false, script);
    }

    @LuaWrapped
    public MixinClassBuilder asInterface(String targetClass) throws IOException, LuaError {
        return new MixinClassBuilder(targetClass, true, script);
    }
}

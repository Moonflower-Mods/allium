package dev.hugeblank.allium.lua.api.mixin;

import dev.hugeblank.allium.loader.Script;
import dev.hugeblank.allium.lua.api.WrappedLuaLibrary;
import dev.hugeblank.allium.lua.type.annotation.LuaStateArg;
import dev.hugeblank.allium.lua.type.annotation.LuaWrapped;
import org.squiddev.cobalt.LuaState;

import java.io.IOException;

@LuaWrapped(name = "mixin")
public class MixinLib implements WrappedLuaLibrary {
    private final Script script;

    public MixinLib(Script script) {
        this.script = script;
    }

    @LuaWrapped
    public MixinClassBuilder asClass(String targetClass, @LuaStateArg LuaState state) throws IOException {
        return new MixinClassBuilder(targetClass, script, false, state);
    }

    @LuaWrapped
    public MixinClassBuilder asInterface(String targetClass, @LuaStateArg LuaState state) throws IOException {
        return new MixinClassBuilder(targetClass, script, true, state);
    }
}

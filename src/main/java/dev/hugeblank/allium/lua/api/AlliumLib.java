package dev.hugeblank.allium.lua.api;

import dev.hugeblank.allium.loader.Script;
import dev.hugeblank.allium.lua.api.commands.CommandRegisterEntry;
import dev.hugeblank.allium.lua.type.annotation.CoerceToNative;
import dev.hugeblank.allium.lua.type.annotation.LuaWrapped;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@LuaWrapped(name = "allium")
public class AlliumLib implements WrappedLuaLibrary {
    public static final List<CommandRegisterEntry>
            COMMANDS = new ArrayList<>();


    public AlliumLib() {
    }

    @LuaWrapped
    public boolean isScriptLoaded(String id) {
        return Script.getFromID(id) != null;
    }

    @LuaWrapped
    public @CoerceToNative List<Script> getAllScripts() {
        return Script.getAllScripts().stream().toList();
    }

    @LuaWrapped
    public @Nullable Script getScript(String id) {
        return Script.getFromID(id);
    }
}

package dev.hugeblank.bouquet.api.lib;

import dev.hugeblank.allium.loader.Script;
import dev.hugeblank.allium.loader.type.WrappedLuaLibrary;
import dev.hugeblank.bouquet.api.lib.commands.CommandRegisterEntry;
import dev.hugeblank.allium.loader.type.annotation.CoerceToNative;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@LuaWrapped(name = "allium")
public class AlliumLib implements WrappedLuaLibrary {
    public static final List<CommandRegisterEntry>
            COMMANDS = new ArrayList<>();

    public AlliumLib(Script script) {

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

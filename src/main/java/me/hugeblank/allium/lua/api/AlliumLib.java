package me.hugeblank.allium.lua.api;

import me.hugeblank.allium.loader.Script;
import me.hugeblank.allium.lua.api.commands.CommandRegisterEntry;
import me.hugeblank.allium.lua.type.LuaWrapped;

import java.util.ArrayList;
import java.util.List;

@LuaWrapped
public class AlliumLib implements WrappedLuaLibrary {
    private final Script script;
    public static final List<CommandRegisterEntry>
            COMMANDS = new ArrayList<>();


    public AlliumLib(Script script) {
        this.script = script;
    }

    @Override
    public String getLibraryName() {
        return "allium";
    }
}

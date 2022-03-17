package me.hugeblank.allium.lua.api;

import me.hugeblank.allium.loader.Plugin;
import org.squiddev.cobalt.ValueFactory;
import org.squiddev.cobalt.lib.LuaLibrary;

public class ScriptLib {

    public static LuaLibrary create(Plugin plugin) {
        return LibBuilder.create("script")
                .set("getID", (state, args) -> ValueFactory.valueOf(plugin.getId()))
                .set("getVersion", (state, args) -> ValueFactory.valueOf(plugin.getVersion()))
                .set("getName", (state, args) -> ValueFactory.valueOf(plugin.getName()))
                .build();
    }
}

package me.hugeblank.allium.lua.api;

import me.hugeblank.allium.loader.Manifest;
import me.hugeblank.allium.loader.Script;
import org.squiddev.cobalt.ValueFactory;
import org.squiddev.cobalt.lib.LuaLibrary;

public class ScriptLib {

    public static LuaLibrary create(Script script) {
        Manifest manifest = script.getManifest();
        return LibBuilder.create("script")
                .set("getID", (state, args) -> ValueFactory.valueOf(manifest.id()))
                .set("getVersion", (state, args) -> ValueFactory.valueOf(manifest.version()))
                .set("getName", (state, args) -> ValueFactory.valueOf(manifest.name()))
                .build();
    }
}

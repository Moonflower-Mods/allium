package me.hugeblank.allium.lua.api;

import me.hugeblank.allium.loader.Script;
import me.hugeblank.allium.lua.type.LuaWrapped;

@LuaWrapped
public class ScriptLib implements WrappedLuaLibrary {
    private final Script script;

    public ScriptLib(Script script) {
        this.script = script;
    }

    @LuaWrapped
    public String getID() {
        return script.getManifest().id();
    }

    @LuaWrapped
    public String getVersion() {
        return script.getManifest().version();
    }

    @LuaWrapped
    public String getName() {
        return script.getManifest().name();
    }

    @Override
    public String getLibraryName() {
        return "script";
    }
}

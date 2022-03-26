package me.hugeblank.allium.loader;

import me.hugeblank.allium.lua.type.LuaWrapped;

public class Manifest {
    private final String id;
    private final String version;
    private final String name;
    private final Entrypoint entrypoints;

    public Manifest(String id, String version, String name, Entrypoint entrypoint) {
        this.id = id;
        this.version = version;
        this.name = name;
        this.entrypoints = entrypoint;
    }

    @LuaWrapped
    public String id() {
        return id;
    }

    @LuaWrapped
    public String version() {
        return version;
    }

    @LuaWrapped
    public String name() {
        return name;
    }

    public Entrypoint entrypoints() {
        return entrypoints;
    }

    public boolean isComplete() {
        return !(id == null || version == null || name == null || entrypoints == null);
    }
}

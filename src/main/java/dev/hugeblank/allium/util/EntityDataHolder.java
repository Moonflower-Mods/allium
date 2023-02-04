package dev.hugeblank.allium.util;

import net.minecraft.entity.Entity;
import org.squiddev.cobalt.LuaValue;

import java.util.Map;

public interface EntityDataHolder {
    LuaValue allium$getTemporaryData(String key);
    void allium$setTemporaryData(String key, LuaValue value);

    LuaValue allium$getData(String key);
    void allium$setData(String key, LuaValue value);

    void allium_private$copyFromData(Entity source);
}

package dev.hugeblank.bouquet.api.lib;

import dev.hugeblank.allium.api.WrappedLuaLibrary;
import dev.hugeblank.allium.loader.type.StaticBinder;
import dev.hugeblank.allium.loader.type.UserdataFactory;
import dev.hugeblank.allium.loader.type.annotation.LuaIndex;
import dev.hugeblank.bouquet.api.event.Events;
import dev.hugeblank.bouquet.api.event.SimpleEventType;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import net.minecraft.registry.Registries;
import org.squiddev.cobalt.LuaUserdata;

import java.util.HashMap;
import java.util.Map;

@LuaWrapped(name = "events")
public class DefaultEventsLib implements WrappedLuaLibrary {
    private static final Map<String, EClass<? extends Events>> MAP;

    @LuaIndex
    public LuaUserdata index(String key) {
        return StaticBinder.bindClass(MAP.get(key));
    }

    public static void registerCategory(String key, EClass<? extends Events> events) {
        MAP.put(key, events);
    }
    static {
        MAP = new HashMap<>();
    }

}

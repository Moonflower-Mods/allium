package me.hugeblank.allium.lua.event;

import me.hugeblank.allium.loader.Plugin;
import net.minecraft.util.Pair;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.LuaFunction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class Event {
    private static final Map<String, Event> EVENTS = new HashMap<>();

    private final String name;
    private final Function<Object[], Varargs> convert;
    private final List<Pair<Plugin, LuaFunction>> listeners = new ArrayList<>();
    public Event(String name, Function<Object[], Varargs> convert) {
        this.name = name;
        this.convert = convert;
        EVENTS.put(this.name, this);
    }

    public static Event get(String eventName) {
        if (EVENTS.containsKey(eventName)) {
            return EVENTS.get(eventName);
        }
        return null;
    }

    public List<Pair<Plugin, LuaFunction>> getListeners() {
        return listeners;
    }

    public static Map<String, Event> getEvents() {
        return EVENTS;
    }

    public void addListener(Plugin source, LuaFunction func) {
        listeners.add(new Pair<>(source, func));
    }

    public void queueEvent(Object ... values) {
        Varargs args = this.evaluate(values);
        for (Pair<Plugin, LuaFunction> pair : listeners) {
            try {
                pair.getRight().invoke(pair.getLeft().getExecutor().getState(), args.asImmutable());
            } catch (UnwindThrowable | LuaError e) {
                pair.getLeft().getLogger().error("Error handling event " + this.getName(), e);
            }
        }
    }

    public String getName() {
        return this.name;
    }

    private Varargs evaluate(Object ... values) {
        return ValueFactory.varargsOf(LuaString.valueOf(this.name), this.convert.apply(values.clone()));
    }
}

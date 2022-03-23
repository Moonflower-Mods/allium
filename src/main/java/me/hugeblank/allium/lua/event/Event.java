package me.hugeblank.allium.lua.event;

import me.hugeblank.allium.loader.Script;
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
    private final Map<Script, List<LuaFunction>> listeners = new HashMap<>();
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

    public static Map<String, Event> getEvents() {
        return EVENTS;
    }

    public boolean addListener(Script source, LuaFunction func) {
        if (!listeners.containsKey(source)) listeners.put(source, new ArrayList<>());
        return listeners.get(source).add(func);
    }

    public boolean removeListener(Script source, LuaFunction func) {
        if (!listeners.containsKey(source)) return false;
        return listeners.get(source).remove(func);
    }

    public boolean removeAllListeners(Script source) {
        return listeners.remove(source) != null;
    }

    public void queueEvent(Object ... values) {
        Varargs args = this.evaluate(values);
        listeners.forEach((script, handlers) -> {
            handlers.forEach((func) -> {
                try {
                    func.invoke(script.getExecutor().getState(), args.asImmutable());
                } catch (UnwindThrowable | LuaError e) {
                    script.getLogger().error("Error handling event " + this.getName(), e);
                }
            });
        });
    }

    public String getName() {
        return this.name;
    }

    private Varargs evaluate(Object ... values) {
        return ValueFactory.varargsOf(LuaString.valueOf(this.name), this.convert.apply(values.clone()));
    }
}

package me.hugeblank.allium.lua;

import org.squiddev.cobalt.Varargs;

import java.util.HashMap;
import java.util.Map;

public class Event {
    private static final Map<String, Event> EVENTS = new HashMap<>();
    private final String name;
    Event(String name, Varargs args){
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

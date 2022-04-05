package me.hugeblank.allium.lua.event;

import me.hugeblank.allium.loader.Script;
import me.hugeblank.allium.loader.ScriptResource;
import me.hugeblank.allium.lua.type.annotation.LuaWrapped;
import me.hugeblank.allium.lua.type.annotation.OptionalArg;

import java.util.*;

@LuaWrapped
public class EventType<T> {
    protected final List<EventHandler> handlers = new ArrayList<>();

    @LuaWrapped
    public ScriptResource register(Script source, T func, @OptionalArg Boolean destroyOnUnload) {
        if (destroyOnUnload == null) destroyOnUnload = true;

        var handler = new EventHandler(func, source, destroyOnUnload);
        handlers.add(handler);
        return handler;
    }

    protected class EventHandler implements ScriptResource {
        protected final T handler;
        protected final Script script;
        private final Script.ResourceRegistration registration;

        private EventHandler(T handler, Script script, boolean destroyOnUnload) {
            this.handler = handler;
            this.script = script;

            if (destroyOnUnload) {
                this.registration = script.registerResource(this);
            } else {
                this.registration = null;
            }
        }

        @Override
        public void close() {
            handlers.remove(this);

            if (this.registration != null) {
                registration.close();
            }
        }
    }
}

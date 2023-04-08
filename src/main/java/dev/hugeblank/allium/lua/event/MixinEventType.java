package dev.hugeblank.allium.lua.event;

import dev.hugeblank.allium.loader.Script;
import dev.hugeblank.allium.loader.ScriptResource;
import dev.hugeblank.allium.lua.type.TypeCoercions;
import dev.hugeblank.allium.lua.type.annotation.LuaWrapped;
import dev.hugeblank.allium.lua.type.annotation.OptionalArg;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import net.minecraft.util.Identifier;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.LuaFunction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@LuaWrapped
public class MixinEventType {
    public static final Map<Identifier, MixinEventType> EVENT_MAP = new HashMap<>();

    private final Identifier id;
    private final List<String> definitions;
    private final List<EClass<?>> arguments = new ArrayList<>();
    protected final List<EventHandler> handlers = new ArrayList<>();

    // The purity of EventType has been tainted by the existence of this class.
    public MixinEventType(Identifier id, List<String> definitions) {
        this.id = id;
        this.definitions = definitions;
        EVENT_MAP.put(id, this);
    }

    private static EClass<?> forName(Identifier id, String name) {
        try {
            // Surely this won't cause any issues in the future!
            return EClass.fromJava(Class.forName(name));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("in event "+id, e);
        }
    }

    @LuaWrapped
    public ScriptResource register(Script source, LuaFunction func, @OptionalArg Boolean destroyOnUnload) {
        if (destroyOnUnload == null) destroyOnUnload = true;

        var handler = new EventHandler(func, source, destroyOnUnload);
        handlers.add(handler);
        return handler;
    }

    public void invoke(Object... objects) throws UnwindThrowable, LuaError {
        if (arguments.isEmpty()) {
            definitions.forEach((def) -> arguments.add(forName(id, def)));
        }
        List<LuaValue> values = new ArrayList<>();
        int i = 0;
        for (EClass<?> argument : arguments) {
            values.add(TypeCoercions.toLuaValue(objects[i], argument));
            i++;
        }
        Varargs args = ValueFactory.varargsOf(values);
        for (EventHandler handler : handlers) {
            handler.handle(args);
        }
    }

    protected class EventHandler implements ScriptResource {
        protected final LuaFunction handler;
        protected final Script script;
        private final Script.ResourceRegistration registration;

        private EventHandler(LuaFunction handler, Script script, boolean destroyOnUnload) {
            this.handler = handler;
            this.script = script;

            if (destroyOnUnload) {
                this.registration = script.registerResource(this);
            } else {
                this.registration = null;
            }
        }
        
        public void handle(Varargs args) throws UnwindThrowable, LuaError {
            handler.invoke(script.getExecutor().getState(), args);
        }

        @Override
        public void close() {
            handlers.remove(this);

            if (this.registration != null) {
                registration.close();
            }
        }
    }

    @Override
    public String toString() {
        return "MixinEventType{" +
                "id=" + id +
                '}';
    }
}

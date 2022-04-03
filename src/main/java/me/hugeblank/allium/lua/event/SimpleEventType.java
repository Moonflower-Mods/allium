package me.hugeblank.allium.lua.event;

import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.hugeblank.allium.lua.type.annotation.LuaWrapped;
import net.minecraft.util.Identifier;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;

@LuaWrapped
public class SimpleEventType<T> extends EventType<T> {
    private final Identifier id;
    private final T invoker;

    @SuppressWarnings("unchecked")
    public SimpleEventType(Identifier id, T... typeGetter) {
        this(id, (EClass<T>) EClass.fromJava(typeGetter.getClass().componentType()));
    }

    @SuppressWarnings("unchecked")
    public SimpleEventType(Identifier id, EClass<T> eventType) {
        this.id = id;

        this.invoker = (T) Proxy.newProxyInstance(SimpleEventType.class.getClassLoader(), new Class[]{eventType.raw()}, (proxy, method, args) -> {
            switch (method.getName()) {
                case "equals" -> {
                    return proxy == args[0];
                }
                case "hashCode" -> {
                    return System.identityHashCode(proxy);
                }
                case "toString" -> {
                    return "<invoker for " + eventType + ">";
                }
            }
            
            if (method.isDefault())
                return InvocationHandler.invokeDefault(proxy, method, args);

            for (EventHandler handler : handlers) {
                try {
                    method.invoke(handler.handler, args);
                } catch (InvocationTargetException e) {
                    handler.script.getLogger().error("Error while handling event " + id, e.getTargetException());
                }
            }

            return null;
        });
    }

    public final T invoker() {
        return invoker;
    }

    @Override
    public String toString() {
        return "SimpleEventType{" +
            "id=" + id +
            '}';
    }
}

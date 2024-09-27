package dev.hugeblank.allium.loader.type;

import me.basiqueevangelist.enhancedreflection.api.EClass;
import org.squiddev.cobalt.LuaTable;
import org.squiddev.cobalt.LuaUserdata;

public class AlliumUserdata<T> extends LuaUserdata {
    private final EClass<T> clazz;

    AlliumUserdata(Object obj, LuaTable metatable, EClass<T> clazz) {
        super(obj, metatable);
        this.clazz = clazz;
    }

    AlliumUserdata(Object obj, LuaTable metatable, Class<T> clazz) {
        this(obj, metatable, EClass.fromJava(clazz));
    }

    public EClass<T> instanceClass() {
        return clazz;
    }


    public boolean instanceOf(Class<?> test) {
        return clazz.isAssignableFrom(test);
    }

    public boolean instanceOf(EClass<?> test) {
        return clazz.isAssignableFrom(test);
    }

    public <U> U toUserdata(EClass<U> test) {
        return toUserdata(test.raw());
    }

    public <U> U toUserdata(Class<U> test) {
        return test.cast(instance);
    }

    @Override
    public T toUserdata() {
        return clazz.cast(instance);
    }
}

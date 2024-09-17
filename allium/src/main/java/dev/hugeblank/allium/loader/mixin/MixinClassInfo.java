package dev.hugeblank.allium.loader.mixin;

import dev.hugeblank.allium.AlliumPreLaunch;
import dev.hugeblank.allium.loader.type.StaticBinder;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import org.squiddev.cobalt.LuaValue;

public class MixinClassInfo {

    private final String className;
    private final byte[] classBytes;
    private final boolean isInterface;

    public MixinClassInfo(String className, byte[] classBytes, boolean isInterface) {
        this.className = className.replace("/", ".");
        this.classBytes = classBytes;
        this.isInterface = isInterface;
    }

    public String getClassName() {
        return className;
    }

    public byte[] getBytes() {
        return classBytes;
    }

    public boolean isInterface() {
        return isInterface;
    }

    @LuaWrapped
    public LuaValue getInterface() throws ClassNotFoundException {
        if (!AlliumPreLaunch.isComplete()) throw new IllegalStateException("Mixin cannot be accessed in pre-launch phase.");
        if (!isInterface) throw new IllegalStateException("Cannot get interface of non-interface mixin.");
        EClass<?> clazz = EClass.fromJava(Class.forName(className));
        return StaticBinder.bindClass(clazz);
    }
}

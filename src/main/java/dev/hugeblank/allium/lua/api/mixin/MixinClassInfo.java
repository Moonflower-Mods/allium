package dev.hugeblank.allium.lua.api.mixin;

import dev.hugeblank.allium.AlliumPreLaunch;
import dev.hugeblank.allium.lua.api.JavaLib;
import dev.hugeblank.allium.lua.type.annotation.LuaWrapped;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import org.squiddev.cobalt.LuaValue;

public class MixinClassInfo {

    private final String className;
    private final byte[] classBytes;
    private final boolean isInterface;
    private EClass<?> clazz;

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
        clazz = EClass.fromJava(Class.forName(className));
        return JavaLib.importClass(clazz);
    }
}

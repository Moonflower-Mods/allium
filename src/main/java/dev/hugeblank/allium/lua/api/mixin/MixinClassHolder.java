package dev.hugeblank.allium.lua.api.mixin;

import dev.hugeblank.allium.AlliumPreLaunch;

public class MixinClassHolder {

    private final String className;

    public MixinClassHolder(String className) {
        this.className = className;
    }

    public Class<?> get() throws ClassNotFoundException {
        if (!AlliumPreLaunch.isComplete()) throw new IllegalStateException("Mixin cannot be accessed in pre-launch phase.");
        return Class.forName(className);
    }
}

package me.hugeblank.allium.loader;

import java.util.function.BiFunction;

public record ScriptCandidate<T>(Manifest manifest, T value, BiFunction<Manifest, T, Script> handler){

    public T load() {
        handler.apply(this.manifest, this.value);
        return value;
    }
}

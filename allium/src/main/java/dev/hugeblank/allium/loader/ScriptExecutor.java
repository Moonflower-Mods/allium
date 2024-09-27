package dev.hugeblank.allium.loader;

import org.jetbrains.annotations.Nullable;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.compiler.CompileException;
import org.squiddev.cobalt.compiler.LoadState;
import org.squiddev.cobalt.function.LuaFunction;

import java.io.IOException;
import java.io.InputStream;

public class ScriptExecutor extends EnvironmentManager {
    protected final Script script;

    public ScriptExecutor(Script script) {
        this.script = script;
        createEnvironment(script);
    }

    public LuaState getState() {
        return state;
    }

    public Varargs initialize(@Nullable InputStream sMain, @Nullable InputStream dMain) throws Throwable {
        Entrypoint entrypoints = script.getManifest().entrypoints();
        LuaFunction staticFunction;
        LuaFunction dynamicFunction;
        switch (entrypoints.getType()) {
            case STATIC -> {
                staticFunction = this.load(sMain, script.getId());
                return LuaThread.runMain(state, staticFunction);
            }
            case DYNAMIC -> {
                dynamicFunction = this.load(dMain, script.getId());
                return LuaThread.runMain(state, dynamicFunction);
            }
            case BOTH -> {
                staticFunction = this.load(sMain, script.getId() + ":static");
                dynamicFunction = this.load(dMain, script.getId() + ":dynamic");
                Varargs out = LuaThread.runMain(state, staticFunction);
                LuaThread.runMain(state, dynamicFunction);
                return out;
            }
        }
        // This should be caught sooner, but who knows maybe a dev (hugeblank) will come along and mess something up
        throw new Exception("Expected either static or dynamic entrypoint, got none");
    }

    public Varargs reload(InputStream dynamic) throws LuaError, CompileException, IOException {
        Entrypoint entrypoint = script.getManifest().entrypoints();
        if (entrypoint.hasType(Entrypoint.Type.DYNAMIC)) {
            LuaFunction dynamicFunction = this.load(dynamic, script.getId());
            return LuaThread.runMain(state, dynamicFunction);
        }
        return null;
    }

    public LuaFunction load(InputStream stream, String name) throws CompileException, IOException, LuaError {
        // TODO: Replacing using globals here with an empty table. Does it work?
        return LoadState.load(
                state,
                stream,
                name,
                state.globals()
        );
    }

}

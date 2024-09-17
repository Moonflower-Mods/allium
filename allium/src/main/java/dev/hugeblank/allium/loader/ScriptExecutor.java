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
    protected final LuaTable globals;

    public ScriptExecutor(Script script) {
        this.script = script;
        this.globals = createPreLaunchEnvironment(script);
    }

    public LuaState getState() {
        return state;
    }

    public Varargs initialize(@Nullable InputStream sMain, @Nullable InputStream dMain) throws Exception {
        bindExtendedLibraries(script, globals);
        Entrypoint entrypoints = script.getManifest().entrypoints();
        LuaFunction staticFunction;
        LuaFunction dynamicFunction;
        if (entrypoints.containsStatic() && entrypoints.containsDynamic()) {
            staticFunction = this.load(sMain, script.getId() + ":static");
            dynamicFunction = this.load(dMain, script.getId() + ":dynamic");
            Varargs out = LuaThread.runMain(state, staticFunction);
            LuaThread.runMain(state, dynamicFunction);
            return out;
        } else if (entrypoints.containsStatic()) {
            staticFunction = this.load(sMain, script.getId());
            return LuaThread.runMain(state, staticFunction);
        } else if (entrypoints.containsDynamic()) {
            dynamicFunction = this.load(dMain, script.getId());
            return LuaThread.runMain(state, dynamicFunction);
        }
        // This should be caught sooner, but who knows maybe a dev (hugeblank) will come along and mess something up
        throw new Exception("Expected either static or dynamic entrypoint, got none");
    }

    public void preInitialize(@Nullable InputStream pMain) throws CompileException, IOException, LuaError {
        Entrypoint entrypoints = script.getManifest().entrypoints();
        if (entrypoints.containsPreLaunch()) {
            LuaThread.runMain(state, this.load(pMain, script.getId() + ":preLaunch"));
        }
    }

    public Varargs reload(InputStream dynamic) throws LuaError, CompileException, IOException {
        Entrypoint entrypoint = script.getManifest().entrypoints();
        if (entrypoint.containsDynamic()) {
            LuaFunction dynamicFunction = this.load(dynamic, script.getId());
            return LuaThread.runMain(state, dynamicFunction);
        }
        return null;
    }

    public LuaFunction load(InputStream stream, String name) throws CompileException, IOException {
        return LoadState.load(
                state,
                stream,
                name,
                this.globals
        );
    }

}

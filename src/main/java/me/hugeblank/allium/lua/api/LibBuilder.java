package me.hugeblank.allium.lua.api;

import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.LibFunction;
import org.squiddev.cobalt.function.VarArgFunction;
import org.squiddev.cobalt.lib.LuaLibrary;

import java.util.HashMap;
import java.util.Map;

public final class LibBuilder {
    private final String name;
    private final Map<String, Function> functionMap = new HashMap<>();

    private LibBuilder(String name) {
        this.name = name;
    }

    public static LibBuilder create(String name) {
        return new LibBuilder(name);
    }

    public LibBuilder add(String functionName, Function function) {
        this.functionMap.put(functionName, function);

        return this;
    }

    public LuaLibrary build() {
        var func = new Function[this.functionMap.size()];
        var funcNames = new String[this.functionMap.size()];

        int i = 0;
        for (var entry : this.functionMap.entrySet()) {
            func[i] = entry.getValue();
            funcNames[i] = entry.getKey();
            i++;
        }

        return new LuaLibraryImpl(this.name, func, funcNames);
    }


    public interface Function {
        Varargs call(LuaState state, Varargs args) throws LuaError;
    }

    private record LuaLibraryImpl(String name, Function[] func,
                                  String[] funcNames) implements LuaLibrary {

        @Override
        public LuaValue add(LuaState state, LuaTable env) {
            LuaTable lib = new LuaTable();
            LibFunction.bind(lib, FunctionImpl::new, funcNames);
            env.rawset(name, lib);
            state.loadedPackages.rawset(name, lib);
            return lib;
        }

        private final class FunctionImpl extends VarArgFunction {
            @Override
            public Varargs invoke(LuaState state, Varargs args) throws LuaError {
                return func[opcode].call(state, args);
            }
        }
    }
}

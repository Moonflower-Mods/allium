package dev.hugeblank.allium.lua.api;

import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.LibFunction;
import org.squiddev.cobalt.function.VarArgFunction;
import org.squiddev.cobalt.lib.LuaLibrary;

import java.util.HashMap;
import java.util.Map;

public final class LibBuilder {
    private final String name;
    private final Map<String, Function> functionMap = new HashMap<>();
    private final Map<String, LuaValue> objectMap = new HashMap<>();
    private LuaTable metatable;

    private LibBuilder(String name) {
        this.name = name;
    }

    public static LibBuilder create(String name) {
        return new LibBuilder(name);
    }

    public LibBuilder set(String functionName, Function function) {
        this.functionMap.put(functionName, function);
        return this;
    }

    public LibBuilder set(String functionName, LuaValue value) {
        this.objectMap.put(functionName, value);
        return this;
    }

    public LibBuilder addMetatable(LuaTable table) {
        this.metatable = table;
        return this;
    }

    public LuaLibraryImpl build() {
        return new LuaLibraryImpl(this.name, this.buildTable());
    }

    public LuaTable buildTable() {
        var func = new Function[this.functionMap.size()];
        var funcNames = new String[this.functionMap.size()];

        int i = 0;
        for (var entry : this.functionMap.entrySet()) {
            func[i] = entry.getValue();
            funcNames[i] = entry.getKey();
            i++;
        }

        LuaTable lib = new LuaTable();
        for (var obj : this.objectMap.entrySet()) {
            lib.rawset(obj.getKey(), obj.getValue());
        }
        LibFunction.bind(lib, () -> new FunctionImpl(func), funcNames);
        if (metatable != null) {
            lib.setMetatable(this.metatable);
        }
        return lib;
    }


    public interface Function {
        Varargs call(LuaState state, Varargs args) throws LuaError;
    }

    private record LuaLibraryImpl(String name, LuaTable lib) implements LuaLibrary {

        @Override
        public LuaValue add(LuaState state, LuaTable env) {
            env.rawset(name, lib);
            state.loadedPackages.rawset(name, lib);
            return lib;
        }
    }

    private static final class FunctionImpl extends VarArgFunction {
        private final Function[] func;

        public FunctionImpl(Function[] functions) {
            this.func = functions;
        }
        @Override
        public Varargs invoke(LuaState state, Varargs args) throws LuaError {
            return func[opcode].call(state, args);
        }
    }
}

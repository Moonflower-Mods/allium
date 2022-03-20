package me.hugeblank.allium.loader;

import me.hugeblank.allium.lua.api.*;
import me.hugeblank.allium.lua.api.PackageLib;
import net.minecraft.resource.DirectoryResourcePack;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.compiler.CompileException;
import org.squiddev.cobalt.compiler.LoadState;
import org.squiddev.cobalt.function.LuaFunction;
import org.squiddev.cobalt.function.VarArgFunction;
import org.squiddev.cobalt.lib.*;

import java.io.*;

public class ScriptExecutor {
    protected final Script script;
    protected final LuaTable globals;
    protected final LuaState state;

    public ScriptExecutor(Script script) {
        this.script = script;

        // Derived from CobaltMachine.java
        // https://github.com/cc-tweaked/cc-restitched/blob/79366bf2f5389b45c0db1ad0d37fbddc6d1151b3/src/main/java/dan200/computercraft/core/lua/CobaltLuaMachine.java
        state = LuaState.builder().build();

        globals = new LuaTable();

        // Base globals
        globals.load( state, new BaseLib() );
        globals.load( state, new TableLib() );
        globals.load( state, new StringLib() );
        globals.load( state, new MathLib() );
        globals.load( state, new CoroutineLib() );
        globals.load( state, new Bit32Lib() );
        globals.load( state, new Utf8Lib() );
        globals.load( state, new DebugLib() );

        // Custom globals
        globals.load( state, AlliumLib.create(script) );
        globals.load( state, new GameLib() );
        globals.load( state, JavaLib.create() );
        globals.load( state, TextLib.create() );
        globals.load( state, NbtLib.create() );
        globals.load( state, CommandLib.create(script) );
        globals.load( state, ScriptLib.create(script) );

        // Package library, kinda quirky.
        PackageLib pkg = new PackageLib();
        globals.rawset( "package" , pkg.create(script) );
        globals.rawset( "require", new PackageLib.Require(pkg) );
        globals.rawset( "module", Constants.NIL ); // TODO: module call

        // Remove globals we don't want to expose
        globals.rawset( "collectgarbage", Constants.NIL );
        globals.rawset( "dofile", Constants.NIL );
        globals.rawset( "loadfile", Constants.NIL );


        globals.rawset( "print", new PrintMethod(script) );

        globals.rawset( "_VERSION", ValueFactory.valueOf( "Lua 5.1" ) );
        globals.rawset( "_HOST", ValueFactory.valueOf("Allium 0.1.0" ) );
    }

    public LuaState getState() {
        return state;
    }

    public LuaValue initialize(InputStream main) throws Throwable {
        LuaFunction loadValue = this.load(main, script.getManifest().id());
        state.setupThread(new LuaTable());
        return loadValue.call(state);
    }

    public LuaFunction load(InputStream stream, String name) throws CompileException, IOException {
        return LoadState.load(
                state,
                stream,
                name,
                this.globals
        );
    }

    private static final class PrintMethod extends VarArgFunction {
        private final Script script;

        PrintMethod(Script script) {
            this.script = script;
        }

        @Override
        public Varargs invoke(LuaState state, Varargs args) {
            StringBuilder out = new StringBuilder();
            for (int i = 1; i <= args.count(); i++) {
                out.append(args.arg(i).toString());
                if (i != args.count()) {
                    out.append(" ");
                }
            }
            script.getLogger().info(out.toString());
            return Constants.NIL;
        }
    }
}

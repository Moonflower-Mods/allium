package me.hugeblank.allium.lua.api;

import me.hugeblank.allium.Allium;
import me.hugeblank.allium.lua.type.UserdataFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.LibFunction;
import org.squiddev.cobalt.function.VarArgFunction;
import org.squiddev.cobalt.lib.LuaLibrary;

import java.util.List;
import java.util.Objects;

public class GameLib implements LuaLibrary {

    public GameLib() {}

    @Override
    public LuaValue add(LuaState state, LuaTable env) {
        LuaTable lib = new LuaTable();
        // Stuff should be here
        env.rawset("game", lib);
        LibFunction.bind(lib, GameLibFunctions::new, new String[]{
                "getPlayer", // Takes in a players username, returns a ServerPlayerEntity
                "getBlockPos", // Takes in 3 numbers, returns a BlockPos
                "getBlock", // Takes in a block identifier *string*, returns a Block
                "getItem", // Takes in an item identifier *string*, returns an Item
                "getWorld", // Takes in a world identifier *string*, returns a ServerWorld
                "listBlocks", // Lists all blocks provided by the game. Identifier key, Block value
                "listItems", // Lists all items provided by the game. Identifier key, Item value
                "listPlayers" // Lists all players currently online.
        });
        state.loadedPackages.rawset("game", lib);
        return lib;
    }

    private static final class GameLibFunctions extends VarArgFunction {
        private static LuaUserdata create(Object instance) {
            return new UserdataFactory<>(instance.getClass()).create(instance);
        }
        @Override
        public Varargs invoke(LuaState state, Varargs args) throws LuaError {
            switch (opcode) {
                case 0:
                    String username = args.arg(1).checkString();
                    ServerPlayerEntity player = Allium.SERVER.getPlayerManager().getPlayer(username);
                    if (player == null) throw new LuaError("Player '" + username + "' does not exist");
                    return create(player);
                case 1:
                    return create(new BlockPos(
                            args.arg(1).checkInteger(),
                            args.arg(2).checkInteger(),
                            args.arg(3).checkInteger()
                            )
                    );
                case 2:
                    return create(
                            Objects.requireNonNull(Allium.SERVER.getRegistryManager()
                                    .get(Registry.BLOCK_KEY)
                                    .get(new Identifier(args.arg(1).checkString())))
                    );
                case 3:
                    return create(
                            Objects.requireNonNull(Allium.SERVER.getRegistryManager()
                                    .get(Registry.ITEM_KEY)
                                    .get(new Identifier(args.arg(1).checkString())))
                    );
                case 4:
                    Identifier id = new Identifier(args.arg(1).checkString());
                    final ServerWorld[] world = new ServerWorld[1];
                    Allium.SERVER.getWorlds().forEach((serverWorld -> {
                        if (serverWorld.getRegistryKey().getValue().equals(id))
                            world[0] = serverWorld;
                    }));
                    if (world[0] == null) throw new LuaError("World " + id + " does not exist");
                    return create(world[0]);
                case 5:
                    LuaTable blocks = new LuaTable();
                    Allium.BLOCKS.forEach((identifier, block) -> blocks.rawset(create(identifier), create(block)));
                    return blocks;
                case 6:
                    LuaTable items = new LuaTable();
                    Allium.ITEMS.forEach((identifier, item) -> items.rawset(create(identifier), create(item)));
                    return items;
                case 7:
                    LuaTable players = new LuaTable();
                    List<ServerPlayerEntity> pl = Allium.SERVER.getPlayerManager().getPlayerList();
                    for (int i = 1; i <= pl.size(); i++) {
                        players.rawset(i, create(pl));
                    }
                    return players;
            }
            return Constants.NIL;
        }
    }
}

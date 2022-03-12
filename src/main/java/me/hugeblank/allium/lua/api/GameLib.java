package me.hugeblank.allium.lua.api;

import me.hugeblank.allium.Allium;
import me.hugeblank.allium.lua.type.UserdataTypes;
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

public class GameLib implements LuaLibrary {

    public GameLib() {}

    @Override
    public LuaValue add(LuaState state, LuaTable env) {
        LuaTable lib = new LuaTable();
        // Stuff should be here
        env.rawset("game", lib);
        LibFunction.bind(lib, GameLibFunctions::new, new String[]{
                "getPlayer",
                "getBlockPos",
                "getBlock",
                "getItem",
                "getWorld",
                "listBlocks",
                "listItems",
                "listPlayers"
        });
        state.loadedPackages.rawset("game", lib);
        return lib;
    }

    private static final class GameLibFunctions extends VarArgFunction {

        @Override
        public Varargs invoke(LuaState state, Varargs args) throws LuaError {
            switch (opcode) {
                case 0:
                    String username = args.arg(1).checkString();
                    ServerPlayerEntity player = Allium.SERVER.getPlayerManager().getPlayer(username);
                    if (player == null) throw new LuaError("Player '" + username + "' does not exist");
                    return UserdataTypes.PLAYER.create(player);
                case 1:
                    return UserdataTypes.BLOCK_POS.create(new BlockPos(
                            args.arg(1).checkInteger(),
                            args.arg(2).checkInteger(),
                            args.arg(3).checkInteger()
                            )
                    );
                case 2:
                    return UserdataTypes.BLOCK.create(
                        Allium.SERVER.getRegistryManager()
                                .get(Registry.BLOCK_KEY)
                                .get(new Identifier(args.arg(1).checkString()))
                    );
                case 3:
                    return UserdataTypes.ITEM.create(
                            Allium.SERVER.getRegistryManager()
                                    .get(Registry.ITEM_KEY)
                                    .get(new Identifier(args.arg(1).checkString()))
                    );
                case 4:
                    Identifier id = new Identifier(args.arg(1).checkString());
                    final ServerWorld[] world = new ServerWorld[1];
                    Allium.SERVER.getWorlds().forEach((serverWorld -> {
                        if (serverWorld.getRegistryKey().getValue().equals(id))
                            world[0] = serverWorld;
                    }));
                    if (world[0] == null) throw new LuaError("World " + id + " does not exist");
                    return UserdataTypes.WORLD.create(world[0]);
                case 5:
                    LuaTable blocks = new LuaTable();
                    Allium.BLOCKS.forEach((identifier, block) -> blocks.rawset(UserdataTypes.IDENTIFIER.create(identifier), UserdataTypes.BLOCK.create(identifier)));
                    return blocks;
                case 6:
                    LuaTable items = new LuaTable();
                    Allium.ITEMS.forEach((identifier, block) -> items.rawset(UserdataTypes.IDENTIFIER.create(identifier), UserdataTypes.ITEM.create(identifier)));
                    return items;
                case 7:
                    LuaTable players = new LuaTable();
                    List<ServerPlayerEntity> pl = Allium.SERVER.getPlayerManager().getPlayerList();
                    for (int i = 1; i <= pl.size(); i++) {
                        players.rawset(i, UserdataTypes.PLAYER.create(pl));
                    }
                    return players;
            }
            return Constants.NIL;
        }
    }
}

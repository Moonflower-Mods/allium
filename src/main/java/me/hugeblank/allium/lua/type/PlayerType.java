package me.hugeblank.allium.lua.type;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.LibFunction;
import org.squiddev.cobalt.function.VarArgFunction;

import java.util.HashMap;
import java.util.Map;

public class PlayerType {
    private final LuaTable tbl = new LuaTable();

    public PlayerType(ServerPlayerEntity player) {
        LibFunction.bind(
                tbl,
                () -> new PlayerFunctions(player),
                new String[] {
                        "setExperiencePoints",
                        "setExperienceLevel",
                        "addExperienceLevels",
                        "damage",
                        "shouldDamagePlayer",
                        "moveToWorld",
                        "canBeSpectated",
                        "sleep"
                }
        );
    }

    public LuaTable build() {
        return tbl;
    }

    public static ServerPlayerEntity checkPlayer(LuaState state, LuaValue value, MinecraftServer server) throws LuaError, UnwindThrowable {
        String name;
        if (value.isTable()) {
            LuaTable tbl = value.checkTable();
            name = tbl.rawget("getName").checkFunction().call(state).checkString();
        } else {
            name = value.checkString();
        }
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(name);
        if (player == null) throw new LuaError("Player '" + name + "' does not exist");
        return player;
    }

    private static final class PlayerFunctions extends VarArgFunction {
        private final ServerPlayerEntity player;

        PlayerFunctions(ServerPlayerEntity player) {
            this.player = player;
        }

        @Override
        public Varargs invoke(LuaState state, Varargs args) throws LuaError, UnwindThrowable {
            switch (opcode) {
                case 0: // setExperiencePoints(int)
                    player.setExperiencePoints(args.arg(1).checkInteger());
                    break;
                case 1: // setExperienceLevel(int)
                    player.setExperienceLevel(args.arg(1).checkInteger());
                    break;
                case 2: // addExperienceLevels(int)
                    player.addExperienceLevels(args.arg(1).checkInteger());
                    break;
                case 3: // damage(string double)
                    // TODO: DamageSource API
                    break;
                case 4: // shouldDamagePlayer(username) ex: hugeblank
                    return ValueFactory.valueOf(player.shouldDamagePlayer(checkPlayer(state, args.arg(1), player.getServer())));
                case 5: // moveToWorld(world) ex: minecraft:overworld
                    MinecraftServer server = player.getServer();
                    Map<String, RegistryKey<World>> worldKeys = new HashMap<>();
                    for (RegistryKey<World> key : player.getWorld().getServer().getWorldRegistryKeys()) {
                        worldKeys.put(key.getValue().toString(), key);
                    }
                    RegistryKey<World> wkey = worldKeys.get(args.arg(1).checkString());
                    if (wkey == null) throw new LuaError(args.arg(1).checkString() + " is not valid world identifier");
                    if (server == null) throw new LuaError("Player '" + player.getName().getString() + "' does not exist on server");
                    player.moveToWorld(server.getWorld(wkey));
                    break;
                case 6: // canBeSpectated(username)
                    return ValueFactory.valueOf(player.canBeSpectated(checkPlayer(state, args.arg(1), player.getServer())));
                case 7: // sleep(blockpos)
                    player.sleep(BlockPosType.checkBlockPos(state, args.arg(1)));

            }
            return Constants.NIL;
        }
    }
}

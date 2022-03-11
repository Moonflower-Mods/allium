package me.hugeblank.allium.lua.type;

import me.hugeblank.allium.Allium;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.TwoArgFunction;
import org.squiddev.cobalt.function.VarArgFunction;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerType {
    private static final String[] names = new String[] {
            "setExperiencePoints",
            "setExperienceLevel",
            "addExperienceLevels",
            "damage",
            "shouldDamagePlayer",
            "moveToWorld",
            "canBeSpectated",
            "sleep",
            "wakeUp",
            "startRiding",
            "stopRiding",
            "requestTeleportAndDismount"
    };

    public static LuaUserdata create(ServerPlayerEntity player) {
        LuaTable metatable = new LuaTable();
        metatable.rawset("__index", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaState state, LuaValue arg1, LuaValue arg2) throws LuaError {
                String name = arg2.checkString();
                int index = Arrays.asList(names).indexOf(name);
                if (index > -1) return new PlayerFunctions(index);
                return Constants.NIL;
            }
        });
        return new LuaUserdata(player, metatable);
    }

    public static LuaUserdata create(String usernameOrUUID) throws LuaError {
        ServerPlayerEntity player = Allium.SERVER.getPlayerManager().getPlayer(usernameOrUUID);
        if (player == null) throw new LuaError("Player '" + usernameOrUUID + "' does not exist");
        return create(player);
    }

    private static final class PlayerFunctions extends VarArgFunction {

        public PlayerFunctions(int opcode) {
            this.opcode = opcode;
        }

        @Override
        public Varargs invoke(LuaState state, Varargs args) throws LuaError, UnwindThrowable {
            ServerPlayerEntity player = args.arg(1).checkUserdata(ServerPlayerEntity.class);
            switch (opcode) {
                case 0: // setExperiencePoints(int)
                    player.setExperiencePoints(args.arg(2).checkInteger());
                    break;
                case 1: // setExperienceLevel(int)
                    player.setExperienceLevel(args.arg(2).checkInteger());
                    break;
                case 2: // addExperienceLevels(int)
                    player.addExperienceLevels(args.arg(2).checkInteger());
                    break;
                case 3: // damage(string double)
                    // TODO: damage
                    break;
                case 4: // shouldDamagePlayer(Player username)
                    return ValueFactory.valueOf(player.shouldDamagePlayer(args.arg(2).checkUserdata(ServerPlayerEntity.class)));
                case 5: // moveToWorld(String world) ex: minecraft:overworld
                    MinecraftServer server = player.getServer();
                    Map<String, RegistryKey<World>> worldKeys = new HashMap<>();
                    for (RegistryKey<World> key : player.getWorld().getServer().getWorldRegistryKeys()) {
                        worldKeys.put(key.getValue().toString(), key);
                    }
                    RegistryKey<World> wkey = worldKeys.get(args.arg(2).checkString());
                    if (wkey == null) throw new LuaError(args.arg(2).checkString() + " is not valid world identifier");
                    if (server == null) throw new LuaError("Player '" + player.getName().getString() + "' does not exist on server");
                    player.moveToWorld(server.getWorld(wkey));
                    break;
                case 6: // canBeSpectated(Player username)
                    return ValueFactory.valueOf(player.canBeSpectated(args.arg(2).checkUserdata(ServerPlayerEntity.class)));
                case 7: // sleep(BlockPos pos)
                    player.sleep(BlockPosType.checkBlockPos(state, args.arg(2)));
                    break;
                case 8: // wakeUp(boolean skipSleepTimer, boolean updateSleepingPlayers)
                    player.wakeUp(args.arg(2).checkBoolean(), args.arg(3).checkBoolean());
                    break;
                case 9: // startRiding(string UUID, boolean force)
                    Entity entity = player.getWorld().getEntity(UUID.fromString(args.arg(2).checkString()));
                    return ValueFactory.valueOf(player.startRiding(entity, args.arg(3).checkBoolean()));
                case 10: // stopRiding()
                    player.stopRiding();
                    break;
                case 11: // requestTeleportAndDismount(double x, double y, double z)
                    player.requestTeleportAndDismount(args.arg(2).checkDouble(), args.arg(3).checkDouble(), args.arg(4).checkDouble());
                    break;
                case 12: // isInvulnerableTo(DamageSource type)
                    // TODO: isInvulnerableTo
                    // ValueFactory.valueOf(player.isInvulnerableTo());
                    break;
                case 13: // handleFall(double heightDiff, boolean onGround)
                    player.handleFall(args.arg(2).checkDouble(), args.arg(3).checkBoolean());
                case 14: // openEditSignScreen(BlockEntity signBlockEntity)
                case 15: // openHandledScreen(NamedScreenHandlerFactory)
                case 16: // openHorseInventory
            }
            return Constants.NIL;
        }
    }
}

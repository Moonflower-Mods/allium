/* ____       .---.      .---.     .-./`)    ___    _  ,---.    ,---.
 .'  __ `.    | ,_|      | ,_|     \ .-.') .'   |  | | |    \  /    |
/   '  \  \ ,-./  )    ,-./  )     / `-' \ |   .'  | | |  ,  \/  ,  |
|___|  /  | \  '_ '`)  \  '_ '`)    `-'`"` .'  '_  | | |  |\_   /|  |
   _.-`   |  > (*)  )   > (*)  )    .---.  '   ( \.-.| |  _( )_/ |  |
.'   _    | (  .  .-'  (  .  .-'    |   |  ' (`. _` /| | (_ o _) |  |
|  _( )_  |  `-'`-'|___ `-'`-'|___  |   |  | (_ (_) _) |  (_,_)  |  |
\ (_ o _) /   |        \ |        \ |   |   \ /  . \ / |  |      |  |
 '.(_,_).'    `--------` `--------` '---'    ``-'`-''  '--'      '-*/
// (c) hugeblank 2022
// See LICENSE for more information
package dev.hugeblank.allium;

import dev.hugeblank.allium.loader.Script;
import dev.hugeblank.allium.util.Mappings;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class Allium implements ModInitializer {

    public static final String ID = "allium";
    public static final Logger LOGGER = LoggerFactory.getLogger(ID);
    public static final Map<Identifier, Block> BLOCKS = new HashMap<>();
    public static final Map<Identifier, Item> ITEMS = new HashMap<>();
    public static final boolean DEVELOPMENT = FabricLoader.getInstance().isDevelopmentEnvironment();
    public static MinecraftServer SERVER;
    public static Mappings MAPPINGS;
    public static Set<Script> CANDIDATES = new HashSet<>();
    public static final Path DUMP_DIRECTORY = FabricLoader.getInstance().getGameDir().resolve("allium-dump");
    public static final String VERSION = FabricLoader.getInstance().getModContainer("allium").orElseThrow().getMetadata().getVersion().getFriendlyString();

    @Override
    public void onInitialize() {
        CANDIDATES.forEach(Script::initialize);
        list(new StringBuilder("Initialized: "), Script::isInitialized);
    }

    static void list(StringBuilder sb, Function<Script, Boolean> func) {
        CANDIDATES.forEach((script) -> {
            if (func.apply(script)) sb.append(script.getId()).append(", ");
        });
        Allium.LOGGER.info(sb.substring(0, sb.length()-2));
    }
}

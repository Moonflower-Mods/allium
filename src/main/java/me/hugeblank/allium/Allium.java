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
package me.hugeblank.allium;

import me.hugeblank.allium.loader.Plugin;
import me.hugeblank.allium.lua.event.Events;
import me.hugeblank.allium.util.FileHelper;
import me.hugeblank.allium.util.YarnLoader;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Allium implements DedicatedServerModInitializer {
    public static final String ID = "allium";
    public static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve(ID);
    public static final Logger LOGGER = LoggerFactory.getLogger(ID);
    public static final Map<Identifier, Block> BLOCKS = new HashMap<>();
    public static final Map<Identifier, Item> ITEMS = new HashMap<>();
    public static final boolean DEVELOPMENT = FabricLoader.getInstance().isDevelopmentEnvironment();
    public static MinecraftServer SERVER;
    public static Map<String, String> MAPPINGS;

    @Override
    public void onInitializeServer() {

        LOGGER.info("Loading NathanFudge's Yarn Remapper");
        MAPPINGS = YarnLoader.init();
        LOGGER.info("Initializing events");
        Events.init();
        LOGGER.info("Loading Plugins");
        File[] files = Objects.requireNonNull(FileHelper.getPluginsDirectory().listFiles());
        for (File pluginDir : files) {
            Plugin plugin = Plugin.loadFromDir(pluginDir);
            if (plugin != null) {
                LOGGER.info(plugin + " loaded");
            }
        }

    }
}

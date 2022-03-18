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

import me.hugeblank.allium.loader.Script;
import me.hugeblank.allium.loader.resources.AlliumResourcePack;
import me.hugeblank.allium.lua.event.Events;
import me.hugeblank.allium.util.FileHelper;
import me.hugeblank.allium.util.Mappings;
import me.hugeblank.allium.util.YarnLoader;
import net.fabricmc.api.ModInitializer;
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

public class Allium implements ModInitializer {

    public static final String ID = "allium";
    public static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve(ID);
    public static final Logger LOGGER = LoggerFactory.getLogger(ID);
    public static final Map<Identifier, Block> BLOCKS = new HashMap<>();
    public static final Map<Identifier, Item> ITEMS = new HashMap<>();
    public static final boolean DEVELOPMENT = FabricLoader.getInstance().isDevelopmentEnvironment();
    public static MinecraftServer SERVER;
    public static Mappings MAPPINGS;
    public static final AlliumResourcePack PACK = new AlliumResourcePack();

    @Override
    public void onInitialize() {
        LOGGER.info("Loading NathanFudge's Yarn Remapper");
        MAPPINGS = YarnLoader.init();

        LOGGER.info("Initializing events");
        Events.init();

        Allium.LOGGER.info("Loading Scripts");
        File[] files = Objects.requireNonNull(FileHelper.getPluginsDirectory().listFiles());
        for (File pluginDir : files) {
            if (pluginDir.isDirectory()) {
                Script.fromDir(pluginDir.toPath());
            }
        }

        // Load scripts periphery to mods. Scripts loaded through this route get +10 style points.
        FabricLoader.getInstance().getAllMods().forEach((container) -> {
            if (container.getMetadata().getCustomValue("allium") != null) {
                Script.fromMod(container);
            }
        });
    }
}

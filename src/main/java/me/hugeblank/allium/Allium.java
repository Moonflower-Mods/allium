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
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Objects;

public class Allium implements ModInitializer {
    public static final String ID = "allium";
    public static final Logger LOGGER = LoggerFactory.getLogger(ID);

    @Override
    public void onInitialize() {
        Events.init();
        LOGGER.info("here!");
        File[] files = Objects.requireNonNull(FileHelper.getPluginsDirectory().listFiles());
        for (File pluginDir : files) {
            Plugin plugin = Plugin.loadFromDir(pluginDir);
            if (plugin != null) {
                LOGGER.info(plugin + " loaded");
            }
        }
    }
}

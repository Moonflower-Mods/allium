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
import dev.hugeblank.allium.api.AlliumExtension;
import dev.hugeblank.allium.util.FileHelper;
import dev.hugeblank.allium.util.Mappings;
import dev.hugeblank.allium.util.YarnLoader;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

public class Allium implements ModInitializer {

    public static final String ID = "allium";
    public static final Logger LOGGER = LoggerFactory.getLogger(ID);
    public static final boolean DEVELOPMENT = FabricLoader.getInstance().isDevelopmentEnvironment();
    public static Mappings MAPPINGS;
    public static final Path DUMP_DIRECTORY = FabricLoader.getInstance().getGameDir().resolve("allium-dump");
    public static final String VERSION = FabricLoader.getInstance().getModContainer(ID).orElseThrow().getMetadata().getVersion().getFriendlyString();

    @Override
    public void onInitialize() {
        clearDumpDirectory();

        try {
            if (!Files.exists(FileHelper.PERSISTENCE_DIR)) Files.createDirectory(FileHelper.PERSISTENCE_DIR);
            if (!Files.exists(FileHelper.CONFIG_DIR)) Files.createDirectory(FileHelper.CONFIG_DIR);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't create config directory", e);
        }

        LOGGER.info("Loading NathanFudge's Yarn Remapper");
        MAPPINGS = YarnLoader.init();

        Set<ModContainer> mods = new HashSet<>();
        FabricLoader.getInstance().getEntrypointContainers(ID, AlliumExtension.class)
                .forEach((initializer) -> {
                    initializer.getEntrypoint().onInitialize();
                    mods.add(initializer.getProvider());
                });
        list(mods, "Initialized Extensions: ", (builder, mod) -> builder.append(mod.getMetadata().getId()));

        Set<Script> scripts = new HashSet<>();
        scripts.addAll(FileHelper.getValidDirScripts(FileHelper.getScriptsDirectory()));
        scripts.addAll(FileHelper.getValidModScripts());
        list(scripts, "Found Scripts: ", (builder, script) -> builder.append(script.getId()));
        scripts.forEach(Script::initialize);
        list(scripts, "Initialized Scripts: ", (builder, script) -> {
            if (script.isInitialized()) builder.append(script.getId());
        });
    }

    private static <T> void list(Collection<T> collection, String initial, BiConsumer<StringBuilder, T> func) {
        StringBuilder builder = new StringBuilder(initial);
        collection.forEach((script) -> {
            func.accept(builder, script);
            builder.append(", ");
        });
        Allium.LOGGER.info(builder.substring(0, builder.length()-2));
    }

    private static void clearDumpDirectory() {
        if (DEVELOPMENT) {
            try {
                if (Files.isDirectory(DUMP_DIRECTORY))
                    Files.walkFileTree(DUMP_DIRECTORY, new FileVisitor<>() {
                        @Override
                        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            Files.delete(file);
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                            throw exc;
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                            Files.delete(dir);
                            return FileVisitResult.CONTINUE;
                        }
                    });
            } catch (IOException e) {
                throw new RuntimeException("Couldn't delete dump directory", e);
            }
        }
    }
}

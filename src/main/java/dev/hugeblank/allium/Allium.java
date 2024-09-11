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
import dev.hugeblank.allium.util.FileHelper;
import dev.hugeblank.allium.util.Mappings;
import dev.hugeblank.allium.util.YarnLoader;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

public class Allium implements ModInitializer {

    public static final String ID = "allium";
    public static final Logger LOGGER = LoggerFactory.getLogger(ID);
    public static final boolean DEVELOPMENT = FabricLoader.getInstance().isDevelopmentEnvironment();
    public static Mappings MAPPINGS;
    public static Set<Script> CANDIDATES = new HashSet<>();
    public static final Path DUMP_DIRECTORY = FabricLoader.getInstance().getGameDir().resolve("allium-dump");
    public static final String VERSION = FabricLoader.getInstance().getModContainer("allium").orElseThrow().getMetadata().getVersion().getFriendlyString();

    @Override
    public void onInitialize() {
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

        try {
            if (!Files.exists(FileHelper.PERSISTENCE_DIR)) Files.createDirectory(FileHelper.PERSISTENCE_DIR);
            if (!Files.exists(FileHelper.CONFIG_DIR)) Files.createDirectory(FileHelper.CONFIG_DIR);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't create config directory", e);
        }

        LOGGER.info("Loading NathanFudge's Yarn Remapper");
        MAPPINGS = YarnLoader.init();

        LOGGER.info("Loading Scripts");

//        if (DEVELOPMENT) CANDIDATES.addAll(FileHelper.getValidDirScripts(
//                // Load example scripts if in development environment
//                FabricLoader.getInstance().getGameDir().resolve("../examples")
//        ));
        CANDIDATES.addAll(FileHelper.getValidDirScripts(FileHelper.getScriptsDirectory()));
        CANDIDATES.addAll(FileHelper.getValidModScripts());
        list(new StringBuilder("Found: "), (script) -> true);
        CANDIDATES.forEach(Script::initialize);
        list(new StringBuilder("Initialized: "), Script::isInitialized);
    }

    private static void list(StringBuilder sb, Function<Script, Boolean> func) {
        CANDIDATES.forEach((script) -> {
            if (func.apply(script)) sb.append(script.getId()).append(", ");
        });
        Allium.LOGGER.info(sb.substring(0, sb.length()-2));
    }
}

package dev.hugeblank.allium;

import dev.hugeblank.allium.loader.Script;
import dev.hugeblank.allium.util.FileHelper;
import dev.hugeblank.allium.util.YarnLoader;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import static dev.hugeblank.allium.Allium.list;

public class AlliumPreLaunch implements PreLaunchEntrypoint {
    @Override
    public void onPreLaunch() {
        if (Allium.DEVELOPMENT) {
            try {
                if (Files.isDirectory(Allium.DUMP_DIRECTORY))
                    Files.walkFileTree(Allium.DUMP_DIRECTORY, new FileVisitor<>() {
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

        Allium.LOGGER.info("Loading NathanFudge's Yarn Remapper");
        Allium.MAPPINGS = YarnLoader.init();

        Allium.LOGGER.info("Loading Scripts");

        if (Allium.DEVELOPMENT) Allium.CANDIDATES.addAll(FileHelper.getValidDirScripts(
                // Load example scripts if in development environment
                FabricLoader.getInstance().getGameDir().resolve("../examples")
        ));
        Allium.CANDIDATES.addAll(FileHelper.getValidDirScripts(FileHelper.getScriptsDirectory()));
        Allium.CANDIDATES.addAll(FileHelper.getValidModScripts());
        list(new StringBuilder("Found: "), (script) -> true);

        Allium.CANDIDATES.forEach(Script::preInitialize);
        list(new StringBuilder("Pre-initialized: "), Script::isPreInitialized);
    }
}

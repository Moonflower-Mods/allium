package dev.hugeblank.allium;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.hugeblank.allium.api.AlliumExtension;
import dev.hugeblank.allium.loader.Script;
import dev.hugeblank.allium.loader.ScriptLoader;
import dev.hugeblank.allium.loader.mixin.MixinClassBuilder;
import dev.hugeblank.allium.util.EldritchURLStreamHandler;
import dev.hugeblank.allium.util.FileHelper;
import dev.hugeblank.allium.util.YarnLoader;
import dev.hugeblank.allium.util.asm.VisitedClass;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.spongepowered.asm.mixin.Mixins;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static dev.hugeblank.allium.Allium.*;

public class AlliumPreLaunch implements PreLaunchEntrypoint {
    public static final String MIXIN_CONFIG_NAME = "allium-generated.mixins.json";
    private static boolean complete = false;

    public static boolean isComplete() {
        return complete;
    }

    @Override
    public void onPreLaunch() {

        try {
            if (!Files.exists(FileHelper.PERSISTENCE_DIR)) Files.createDirectory(FileHelper.PERSISTENCE_DIR);
            if (!Files.exists(FileHelper.CONFIG_DIR)) Files.createDirectory(FileHelper.CONFIG_DIR);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't create config directory", e);
        }

        Allium.LOGGER.info("Loading NathanFudge's Yarn Remapper");
        Allium.MAPPINGS = YarnLoader.init();

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

        list(ScriptLoader.SCRIPTS, "Found Scripts: ", (builder, script) -> builder.append(script.getId()));
        ScriptLoader.SCRIPTS.forEach(Script::preLaunch);
        list(ScriptLoader.SCRIPTS, "Pre-initialized: ", (builder, script) -> {
            if (script.isPreInitialized()) builder.append(script.getId());
        });

        // Create a new mixin config
        JsonObject config = new JsonObject();
        config.addProperty("required", true);
        config.addProperty("minVersion", "0.8");
        config.addProperty("package", "allium.mixin");
        JsonObject injectors = new JsonObject();
        injectors.addProperty("defaultRequire", 1);
        config.add("injectors", injectors);
        JsonArray mixins = new JsonArray();
        MixinClassBuilder.MIXINS.forEach((key, value) -> {
            if (key.matches(".*mixin.*")) {
                mixins.add(key.substring(0, key.length()-6).replace("allium/mixin/", ""));
            }
        });
        config.add("mixins", mixins);
        String configJson = (new Gson()).toJson(config);
        Map<String, byte[]> mixinConfigMap = new HashMap<>();
        MixinClassBuilder.MIXINS.forEach((name, info) -> mixinConfigMap.put(name, info.getBytes()));
        mixinConfigMap.put(MIXIN_CONFIG_NAME, configJson.getBytes(StandardCharsets.UTF_8));
        URL mixinUrl = EldritchURLStreamHandler.create("allium-mixin", mixinConfigMap);

        // Stuff those files into class loader
        ClassLoader loader = AlliumPreLaunch.class.getClassLoader();
        Method addUrlMethod = null;
        for (Method method : loader.getClass().getMethods()) {
            if (method.getName().equals("addUrlFwd")) {
                addUrlMethod = method;
                break;
            }
        }
        if (addUrlMethod == null) throw new IllegalStateException("Could not find URL loader in ClassLoader " + loader);
        try {
            addUrlMethod.setAccessible(true);
            MethodHandle handle = MethodHandles.lookup().unreflect(addUrlMethod);
            handle.invoke(loader, mixinUrl);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Couldn't get handle for " + addUrlMethod, e);
        } catch (Throwable e) {
            throw new RuntimeException("Error invoking URL handler", e);
        }

        Mixins.addConfiguration(MIXIN_CONFIG_NAME);
        VisitedClass.clear();
        complete = true;
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

package dev.hugeblank.bouquet;

import dev.hugeblank.allium.loader.EnvironmentManager;
import dev.hugeblank.allium.api.AlliumExtension;
import dev.hugeblank.bouquet.api.event.CommonEvents;
import dev.hugeblank.bouquet.api.event.ServerEvents;
import dev.hugeblank.bouquet.api.lib.*;
import dev.hugeblank.bouquet.api.lib.commands.CommandLib;
import dev.hugeblank.bouquet.api.lib.commands.CommandsLib;
import dev.hugeblank.bouquet.api.lib.fs.FsLib;
import dev.hugeblank.bouquet.api.lib.http.HttpLib;
import me.basiqueevangelist.enhancedreflection.api.EClass;

public class BouquetAlliumExtension implements AlliumExtension {

    @Override
    public void onInitialize() {
        // Register common and server events
        DefaultEventsLib.registerCategory("common", EClass.fromJava(CommonEvents.class));
        DefaultEventsLib.registerCategory("server", EClass.fromJava(ServerEvents.class));

        EnvironmentManager.registerLibrary(AlliumLib::new);
        EnvironmentManager.registerLibrary(new DefaultEventsLib());
        EnvironmentManager.registerLibrary(new FabricLib());
        EnvironmentManager.registerLibrary(new GameLib());
        EnvironmentManager.registerLibrary(new JavaLib());
        EnvironmentManager.registerLibrary(new JsonLib());
        EnvironmentManager.registerLibrary(new NbtLib());
        EnvironmentManager.registerLibrary(new TextLib());
        EnvironmentManager.registerLibrary(new CommandsLib());
        EnvironmentManager.registerLibrary(CommandLib::new);

        EnvironmentManager.registerLibrary(FsLib::new);
        EnvironmentManager.registerLibrary(new HttpLib());
    }
}

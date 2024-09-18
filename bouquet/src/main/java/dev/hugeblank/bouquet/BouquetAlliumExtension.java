package dev.hugeblank.bouquet;

import dev.hugeblank.allium.loader.EnvironmentManager;
import dev.hugeblank.allium.api.AlliumExtension;
import dev.hugeblank.bouquet.api.lib.*;
import dev.hugeblank.bouquet.api.lib.commands.CommandLib;
import dev.hugeblank.bouquet.api.lib.fs.FsLib;
import dev.hugeblank.bouquet.api.lib.http.HttpLib;

public class BouquetAlliumExtension implements AlliumExtension {

    @Override
    public void onInitialize() {
        EnvironmentManager.registerLibrary(AlliumLib::new);
        EnvironmentManager.registerLibrary(new DefaultEventsLib());
        EnvironmentManager.registerLibrary(new FabricLib());
        EnvironmentManager.registerLibrary(new GameLib());
        EnvironmentManager.registerLibrary(new JavaLib());
        EnvironmentManager.registerLibrary(new JsonLib());
        EnvironmentManager.registerLibrary(new NbtLib());
        EnvironmentManager.registerLibrary(new TextLib());
        EnvironmentManager.registerLibrary(CommandLib::new);
        EnvironmentManager.registerLibrary(FsLib::new);
        EnvironmentManager.registerLibrary(new HttpLib());
    }
}

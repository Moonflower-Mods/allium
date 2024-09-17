package dev.hugeblank.bouquet;

import dev.hugeblank.allium.loader.EnvironmentManager;
import dev.hugeblank.allium.api.AlliumExtension;
import dev.hugeblank.bouquet.api.lib.*;

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
    }
}

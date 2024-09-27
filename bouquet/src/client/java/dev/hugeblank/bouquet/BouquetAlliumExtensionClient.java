package dev.hugeblank.bouquet;

import dev.hugeblank.allium.api.AlliumExtension;
import dev.hugeblank.bouquet.api.event.ClientEvents;
import dev.hugeblank.bouquet.api.lib.DefaultEventsLib;
import me.basiqueevangelist.enhancedreflection.api.EClass;

public class BouquetAlliumExtensionClient implements AlliumExtension {
    @Override
    public void onInitialize() {
        DefaultEventsLib.registerCategory("client", EClass.fromJava(ClientEvents.class)); // Register client events
    }
}

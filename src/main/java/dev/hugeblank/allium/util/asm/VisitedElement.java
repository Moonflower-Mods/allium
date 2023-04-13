package dev.hugeblank.allium.util.asm;

import dev.hugeblank.allium.Allium;
import dev.hugeblank.allium.util.Mappings;

public interface VisitedElement {
    VisitedClass owner();
    int access();
    String name();
    String descriptor();
    String signature();

    default String mappedName() {
        return Allium.DEVELOPMENT ? name() : Allium.MAPPINGS.getYarn(
                Mappings.asMethod(owner().name(), name())
        ).split("#")[1];
    }
}

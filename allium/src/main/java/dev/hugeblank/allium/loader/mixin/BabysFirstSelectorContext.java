package dev.hugeblank.allium.loader.mixin;

import org.spongepowered.asm.mixin.injection.selectors.ISelectorContext;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.util.asm.IAnnotationHandle;

public class BabysFirstSelectorContext implements ISelectorContext {
    public static final BabysFirstSelectorContext INSTANCE = new BabysFirstSelectorContext();

    @Override
    public ISelectorContext getParent() {
        return null;
    }

    @Override
    public IMixinContext getMixin() {
        return null;
    }

    @Override
    public Object getMethod() {
        return null;
    }

    @Override
    public IAnnotationHandle getAnnotation() {
        return null;
    }

    @Override
    public IAnnotationHandle getSelectorAnnotation() {
        return null;
    }

    @Override
    public String getSelectorCoordinate(boolean leaf) {
        return null;
    }

    @Override
    public String remap(String reference) {
        return reference;
    }

    @Override
    public String getElementDescription() {
        return "Baby's first selector context";
    }
}

package me.hugeblank.allium.lua.type.property;

import me.basiqueevangelist.enhancedreflection.api.EMethod;
import me.hugeblank.allium.lua.type.AnnotationUtils;
import org.squiddev.cobalt.LuaValue;

import java.util.Comparator;

public class MethodSorter implements Comparator<EMethod> {
    public static final MethodSorter INSTANCE = new MethodSorter();

    public static int getImplicitPriority(EMethod method) {
        int value = 0;

        for (var param : method.parameters()) {
            if (LuaValue.class.isAssignableFrom(param.rawParameterType().raw()))
                value--;
        }

        return value;
    }

    @Override
    public int compare(EMethod o1, EMethod o2) {
        int prio1 = AnnotationUtils.getPriority(o1);
        int prio2 = AnnotationUtils.getPriority(o2);

        if (prio1 != prio2)
            return Integer.compare(prio2, prio1);

        return Integer.compare(getImplicitPriority(o2), getImplicitPriority(o1));
    }
}

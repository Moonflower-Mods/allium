package dev.hugeblank.allium.lua.type.property;

import dev.hugeblank.allium.Allium;
import dev.hugeblank.allium.lua.type.AnnotationUtils;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.basiqueevangelist.enhancedreflection.api.EField;
import me.basiqueevangelist.enhancedreflection.api.EMethod;
import dev.hugeblank.allium.util.Mappings;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class PropertyResolver {
    private PropertyResolver() {

    }

    public static <T> PropertyData<? super T> resolveProperty(EClass<T> clazz, String name, boolean isStatic) {
        List<EMethod> foundMethods = new ArrayList<>();

        collectMethods(clazz, clazz.methods(), name, isStatic, foundMethods::add);

        if (foundMethods.size() > 0)
            return new MethodData<>(clazz, foundMethods, name, isStatic);

        EMethod getter = findMethod(clazz, clazz.methods(), "get" + StringUtils.capitalize(name),
            method -> AnnotationUtils.countLuaArguments(method) == 0 && (!isStatic || method.isStatic()));

        if (getter != null) {
            EMethod setter = findMethod(clazz, clazz.methods(), "set" + StringUtils.capitalize(name),
                method -> AnnotationUtils.countLuaArguments(method) == 1 && (!isStatic || method.isStatic()));

            return new PropertyMethodData<>(getter, setter);
        }

        EField field = findField(clazz, clazz.fields(), name, isStatic);

        if (field != null)
            return new FieldData<>(field);

        return EmptyData.INSTANCE;
    }

    public static void collectMethods(EClass<?> sourceClass, Collection<EMethod> methods, String name, boolean staticOnly, Consumer<EMethod> consumer) {
        methods.forEach((method -> {
            if (AnnotationUtils.isHiddenFromLua(method)) return;
            if (staticOnly && !method.isStatic()) return;

            String[] altNames = AnnotationUtils.findNames(method);
            if (altNames != null) {
                for (String altName : altNames) {
                    if (altName.equals(name)) {
                        consumer.accept(method);
                    }
                }

                return;
            }

            var methodName = method.name();

            if (methodName.equals(name) || methodName.equals("allium$" + name) || name.equals("m_" + methodName)) {
                consumer.accept(method);
            }

            if (methodName.startsWith("allium_private$")) {
                return;
            }

            if (!Allium.DEVELOPMENT) {
                var mappedName = Allium.MAPPINGS.getYarn(Mappings.asMethod(sourceClass, method)).split("#")[1];
                if (mappedName.equals(name) || mappedName.equals("m_" + methodName)) {
                    consumer.accept(method);
                }

                for (var clazz : sourceClass.allSuperclasses()) {
                    mappedName = Allium.MAPPINGS.getYarn(Mappings.asMethod(clazz, method)).split("#")[1];
                    if (mappedName.equals(name) || mappedName.equals("m_" + methodName)) {
                        consumer.accept(method);
                    }
                }

                for (var clazz : sourceClass.allInterfaces()) {
                    mappedName = Allium.MAPPINGS.getYarn(Mappings.asMethod(clazz, method)).split("#")[1];
                    if (mappedName.equals(name) || mappedName.equals("m_" + methodName)) {
                        consumer.accept(method);
                    }
                }
            }
        }));
    }

    public static EMethod findMethod(EClass<?> sourceClass, List<EMethod> methods, String name, Predicate<EMethod> filter) {
        for (EMethod method : methods) {
            if (AnnotationUtils.isHiddenFromLua(method)) continue;
            if (!filter.test(method)) continue;

            String[] altNames = AnnotationUtils.findNames(method);
            if (altNames != null) {
                for (String altName : altNames) {
                    if (altName.equals(name)) {
                        return method;
                    }
                }

                continue;
            }

            var methodName = method.name();

            if (methodName.equals(name) || methodName.equals("allium$" + name) || name.equals("m_" + methodName)) {
                return method;
            }

            if (methodName.startsWith("allium_private$")) {
                continue;
            }

            if (!Allium.DEVELOPMENT) {
                var mappedName = Allium.MAPPINGS.getYarn(Mappings.asMethod(sourceClass, method)).split("#")[1];
                if (mappedName.equals(name) || mappedName.equals("m_" + methodName)) {
                    return method;
                }

                for (var clazz : sourceClass.allSuperclasses()) {
                    mappedName = Allium.MAPPINGS.getYarn(Mappings.asMethod(clazz, method)).split("#")[1];
                    if (mappedName.equals(name) || mappedName.equals("m_" + methodName)) {
                        return method;
                    }
                }

                for (var clazz : sourceClass.allInterfaces()) {
                    mappedName = Allium.MAPPINGS.getYarn(Mappings.asMethod(clazz, method)).split("#")[1];
                    if (mappedName.equals(name) || mappedName.equals("m_" + methodName)) {
                        return method;
                    }
                }
            }
        }

        return null;
    }

    public static EField findField(EClass<?> sourceClass, Collection<EField> fields, String name, boolean staticOnly) {
        for (var field : fields) {
            if (AnnotationUtils.isHiddenFromLua(field)) continue;
            if (staticOnly && !field.isStatic()) continue;

            String[] altNames = AnnotationUtils.findNames(field);
            if (altNames != null) {
                for (String altName : altNames) {
                    if (altName.equals(name)) {
                        return field;
                    }
                }

                continue;
            }

            if (Allium.DEVELOPMENT) {
                if (field.name().equals(name)) {
                    return field;
                }
            } else {
                if (Allium.MAPPINGS.getYarn(Mappings.asMethod(sourceClass, field)).split("#")[1].equals(name)) {
                    return field;
                }

                for (var clazz : sourceClass.allSuperclasses()) {
                    if (Allium.MAPPINGS.getYarn(Mappings.asMethod(clazz, field)).split("#")[1].equals(name)) {
                        return field;
                    }
                }
            }
        }

        return null;
    }
}

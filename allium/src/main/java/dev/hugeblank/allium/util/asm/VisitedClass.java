package dev.hugeblank.allium.util.asm;

import dev.hugeblank.allium.Allium;
import dev.hugeblank.allium.util.JavaHelpers;
import dev.hugeblank.allium.util.Mappings;
import org.objectweb.asm.*;
import org.spongepowered.asm.service.MixinService;
import org.squiddev.cobalt.LuaError;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.objectweb.asm.Opcodes.ASM9;

public class VisitedClass {
    private static final Map<String, VisitedClass> VISITED = new HashMap<>();

    private final Map<String, VisitedField> visitedFields = new HashMap<>();
    private final Map<String, VisitedMethod> visitedMethods = new HashMap<>();

    private final String mappedClassName;
    private final int version;
    private final int access;
    private final String className;
    private final String signature;
    private final String superName;
    private final String[] interfaces;

    public VisitedClass(String mappedClassName, int version, int access, String className, String signature, String superName, String[] interfaces) {
        this.mappedClassName = mappedClassName;
        this.version = version;
        this.access = access;
        this.className = className;
        this.signature = signature;
        this.superName = superName;
        this.interfaces = interfaces;
        VISITED.put(mappedClassName, this);
    }

    public boolean containsMethod(String name) {
        return visitedMethods.containsKey(name);
    }

    public VisitedMethod getMethod(String name) {
        return visitedMethods.get(name);
    }

    public boolean containsField(String name) {
        return visitedFields.containsKey(name);
    }

    public VisitedField getField(String name) {
        return visitedFields.get(name);
    }

    public VisitedElement get(String name) {
        if (visitedMethods.containsKey(name)) {
            return visitedMethods.get(name);
        } else {
            return visitedFields.get(name);
        }
    }

    private void addVisitedField(int access, String name, String descriptor, String signature, Object value) {
        String[] mapped = Allium.MAPPINGS.getYarn(Mappings.asMethod(className, name)).split("#");
        VisitedField visitedField = new VisitedField(this, access, name, descriptor, signature, value);
        visitedFields.put(mapped[1], visitedField);
    }

    private void addVisitedMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        String[] mapped = Allium.MAPPINGS.getYarn(Mappings.asMethod(className, name)).split("#");
        VisitedMethod visitedMethod = new VisitedMethod(this, access, name, descriptor, signature, exceptions);
        String key = mapped[1];
        StringBuilder mappedDescriptor = new StringBuilder("(");
        for (Type arg : Type.getArgumentTypes(descriptor)) {
            mapTypeArg(mappedDescriptor, arg);
        }
        mappedDescriptor.append(")");
        mapTypeArg(mappedDescriptor, Type.getReturnType(descriptor));
        if (!name.equals("<init>") && !name.equals("<clinit>")) {
            key = key+mappedDescriptor;
        }
        visitedMethods.put(key, visitedMethod);
    }

    private void mapTypeArg(StringBuilder mappedDescriptor, Type arg) {
        if (arg.getSort() == Type.OBJECT) {
            mappedDescriptor
                    .append("L")
                    .append(Allium.MAPPINGS.getYarn(arg.getInternalName()))
                    .append(";");
        } else {
            mappedDescriptor.append(arg.getInternalName());
        }
    }

    public String mappedClassName() {
        return mappedClassName;
    }

    public int version() {
        return version;
    }

    public int access() {
        return access;
    }

    public String name() {
        return className;
    }

    public String signature() {
        return signature;
    }

    public String superName() {
        return superName;
    }

    public String[] interfaces() {
        return interfaces;
    }

    public static VisitedClass ofClass(String mappedClassName) throws LuaError {
        if (!VISITED.containsKey(mappedClassName)) {
            String unmappedName = JavaHelpers.getRawClassName(mappedClassName);
            try {
                new ClassReader(MixinService.getService().getResourceAsStream(unmappedName.replace(".", "/") + ".class")).accept(
                        new ClassVisitor(ASM9) {
                            VisitedClass instance;

                            @Override
                            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                                instance = new VisitedClass(mappedClassName, version, access, name, signature, superName, interfaces);
                                super.visit(version, access, name, signature, superName, interfaces);
                            }

                            @Override
                            public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                                instance.addVisitedField(access, name, descriptor, signature, value);
                                return super.visitField(access, name, descriptor, signature, value);
                            }

                            @Override
                            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                                instance.addVisitedMethod(access, name, descriptor, signature, exceptions);
                                return super.visitMethod(access, name, descriptor, signature, exceptions);
                            }
                        },
                        ClassReader.SKIP_FRAMES
                );
            } catch (IOException e) {
                throw new LuaError(new RuntimeException("Could not read target class: " + mappedClassName + " (unmapped:" + unmappedName + ")", e));
            }
        }
        return VISITED.get(mappedClassName);
    }

    public static void clear() {
        VISITED.clear();
    }
}

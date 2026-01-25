package io.github.hacihaciyev.types;

import io.github.hacihaciyev.util.Result;

import java.io.IOException;
import java.lang.classfile.ClassBuilder;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.ClassTransform;
import java.lang.classfile.CodeModel;
import java.lang.classfile.MethodModel;
import java.lang.classfile.attribute.RecordAttribute;
import java.lang.classfile.attribute.RecordComponentInfo;
import java.lang.constant.ClassDesc;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.constant.DynamicCallSiteDesc;
import java.lang.constant.MethodHandleDesc;
import java.lang.constant.MethodTypeDesc;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static java.lang.constant.ConstantDescs.CD_Class;
import static java.lang.constant.ConstantDescs.CD_Object;
import static java.lang.constant.ConstantDescs.CD_String;
import static java.lang.constant.ConstantDescs.CD_void;

public final class MetaGen {

    static final String INVALID_PACKAGE_DEF = "JetQuerious. Property: jetquerious.packages. Invalid package definition";

    static final Path META_REGISTRY_PATH = Path.of("target/classes/io/github/hacihaciyev/types/MetaRegistry.class");

    static final ClassDesc RECORD_DESC = ClassDesc.of("io.github.hacihaciyev.types.MetaRegistry$TypeMeta$Record");

    static final ClassDesc META_REGISTRY_DESC = ClassDesc.of("io.github.hacihaciyev.types.MetaRegistry");

    static final ClassDesc TYPE_META_DESC = ClassDesc.of("io.github.hacihaciyev.types.MetaRegistry$TypeMeta");

    static final ClassDesc FIELD_DESC = ClassDesc.of("io.github.hacihaciyev.types.MetaRegistry$Field");

    static final MethodTypeDesc FIELD_CONSTRUCTOR_DESC = MethodTypeDesc.of(CD_void, CD_String, CD_Class, ClassDesc.of("java.util.function.Function"));

    private MetaGen() {}

    static void main() {
        var packages = userSpec();
        for (var pkg : packages) {
            for (var type : readPackage(sanitized(pkg))) metaGen(type);
        }
    }

    private static void metaGen(byte[] type) {
        var classFile = ClassFile.of();
        var classModel = classFile.parse(type);

        var attribute = recordAttribute(classModel);
        if (attribute.isEmpty()) return;

        var classDesc = classModel.thisClass().asSymbol();
        var components = attribute.get().components();

        var method = buildMethod(classFile, classDesc, components);
        addToMetaRegistry(classFile, method, classDesc);
    }

    private static MethodModel buildMethod(ClassFile cf, ClassDesc cd, List<RecordComponentInfo> components) {
        var name = "_meta_" + cd.displayName().replace(".", "_");
        var returnType = MethodTypeDesc.of(TYPE_META_DESC);
        var modifiers  = ClassFile.ACC_PRIVATE | ClassFile.ACC_STATIC;

        var bytes = cf.build(CD_Object, clb -> clb.withMethod(name, returnType, modifiers, b -> b.withCode(cob -> {
            cob.loadConstant(components.size());
            cob.anewarray(FIELD_DESC);

            for (int i = 0; i < components.size(); i++) {
                var component = components.get(i);
                var fieldName = component.name().stringValue();
                var fieldDesc = component.descriptor();
                var fieldType = ClassDesc.ofDescriptor(fieldDesc.stringValue());

                cob.dup();
                cob.loadConstant(i);

                cob.new_(FIELD_DESC);
                cob.dup();

                cob.ldc(fieldName);

                var descriptor = fieldType.descriptorString();
                var firstChar = descriptor.charAt(0);
                var isPrimitive = firstChar != 'L' && firstChar != '[';

                if (isPrimitive) {
                    var wrapper = switch (firstChar) {
                        case 'I' -> "java.lang.Integer";
                        case 'J' -> "java.lang.Long";
                        case 'D' -> "java.lang.Double";
                        case 'F' -> "java.lang.Float";
                        case 'Z' -> "java.lang.Boolean";
                        case 'B' -> "java.lang.Byte";
                        case 'C' -> "java.lang.Character";
                        case 'S' -> "java.lang.Short";
                        default -> throw new IllegalStateException();
                    };
                    cob.getstatic(ClassDesc.of(wrapper), "TYPE", CD_Class);
                } else {
                    cob.ldc(fieldType);
                }

                var boxedType = isPrimitive ? ClassDesc.of(switch (firstChar) {
                    case 'I' -> "java.lang.Integer";
                    case 'J' -> "java.lang.Long";
                    case 'D' -> "java.lang.Double";
                    case 'F' -> "java.lang.Float";
                    case 'Z' -> "java.lang.Boolean";
                    case 'B' -> "java.lang.Byte";
                    case 'C' -> "java.lang.Character";
                    case 'S' -> "java.lang.Short";
                    default -> throw new IllegalStateException();
                }) : fieldType;

                cob.invokedynamic(DynamicCallSiteDesc.of(
                        MethodHandleDesc.ofMethod(
                                DirectMethodHandleDesc.Kind.STATIC,
                                ClassDesc.of("java.lang.invoke.LambdaMetafactory"), "metafactory",
                                MethodTypeDesc.of(
                                        ClassDesc.of("java.lang.invoke.CallSite"),
                                        ClassDesc.of("java.lang.invoke.MethodHandles$Lookup"),
                                        CD_String,
                                        ClassDesc.of("java.lang.invoke.MethodType"),
                                        ClassDesc.of("java.lang.invoke.MethodType"),
                                        ClassDesc.of("java.lang.invoke.MethodHandle"),
                                        ClassDesc.of("java.lang.invoke.MethodType")
                                )),
                        "apply",
                        MethodTypeDesc.of(ClassDesc.of("java.util.function.Function")),
                        MethodTypeDesc.of(CD_Object, CD_Object),
                        MethodHandleDesc.ofMethod(
                                DirectMethodHandleDesc.Kind.VIRTUAL,
                                cd,
                                fieldName,
                                MethodTypeDesc.of(fieldType)
                        ),
                        MethodTypeDesc.of(boxedType, cd)
                ));

                cob.invokespecial(FIELD_DESC, "<init>", FIELD_CONSTRUCTOR_DESC);
                cob.aastore();
            }

            cob.new_(RECORD_DESC);
            cob.dup_x1();
            cob.swap();

            cob.ldc(cd);
            cob.swap();

            cob.invokespecial(RECORD_DESC, "<init>", MethodTypeDesc.of(CD_void, CD_Class, CD_Object.arrayType()));
            cob.areturn();
        })));

        return cf.parse(bytes)
                .methods().stream()
                .filter(m -> m.methodName().stringValue().equals(name))
                .findFirst()
                .orElseThrow();
    }

    private static void addToMetaRegistry(ClassFile cf, MethodModel newMethod, ClassDesc recordClass)  {
        try {
            var registryBytes = Files.readAllBytes(META_REGISTRY_PATH);
            var metaMethodName = newMethod.methodName().stringValue();

            var updated = cf.transformClass(
                    cf.parse(registryBytes),
                    (clb, element) -> {
                        if (element instanceof MethodModel mm && mm.methodName().stringValue().equals("meta")) {
                            transformMetaMethod(recordClass, clb, mm, metaMethodName);
                            return;
                        }

                        clb.accept(element);
                    }
            );

            updated = cf.transformClass(cf.parse(updated), ClassTransform.endHandler(clb -> clb.accept(newMethod)));
            Files.write(META_REGISTRY_PATH, updated);
        } catch (IOException e) {
            throw new IllegalArgumentException(INVALID_PACKAGE_DEF, e);
        }
    }

    private static void transformMetaMethod(ClassDesc recordClass, ClassBuilder clb, MethodModel mm, String metaMethodName) {
        clb.transformMethod(mm, (mb, methodElement) -> {
            if (methodElement instanceof CodeModel cm) {
                mb.withCode(cob -> {
                    cob.aload(0);
                    cob.ldc(recordClass);

                    var labelNotEqual = cob.newLabel();
                    cob.if_acmpne(labelNotEqual);

                    cob.invokestatic(META_REGISTRY_DESC, metaMethodName, MethodTypeDesc.of(TYPE_META_DESC));
                    cob.areturn();

                    cob.labelBinding(labelNotEqual);

                    for (var el : cm) cob.with(el);
                });
                return;
            }

            mb.accept(methodElement);
        });
    }

    private static Optional<RecordAttribute> recordAttribute(ClassModel classModel) {
        for (var attribute : classModel.attributes()) {
            if (attribute instanceof RecordAttribute ra) return Optional.of(ra);
        }
        return Optional.empty();
    }

    private static String[] userSpec() {
        var pkgs = System.getProperty("jetquerious.packages");
        if (pkgs != null && !pkgs.isBlank()) return pkgs.split(";");
        return new String[0];
    }

    private static List<byte[]> readPackage(String resourcePath) {
        var result = new ArrayList<byte[]>();

        Thread.currentThread()
                .getContextClassLoader()
                .resources(resourcePath)
                .forEach(url -> {
                    switch (url.getProtocol()) {
                        case "file" -> fromDir(url, result);
                        case "jar"  -> fromJar(url, resourcePath, result);
                        default -> throw new IllegalArgumentException(INVALID_PACKAGE_DEF + ": unsupported protocol " + url);
                    }
                });

        return result;
    }

    private static void fromDir(URL url, List<byte[]> out) {
        try (var stream = Files.walk(pathOf(url))) {
            stream.filter(Files::isRegularFile)
                    .filter(f -> isClass(f))
                    .forEach(p -> fill(p, out));
        } catch (IOException e) {
            throw new IllegalArgumentException(INVALID_PACKAGE_DEF + ": " + url, e);
        }
    }

    private static void fromJar(URL url, String packagePath, List<byte[]> out) {
        try {
            var conn = (JarURLConnection) url.openConnection();
            try (var jar = conn.getJarFile()) {
                jar.stream()
                        .filter(e -> !e.isDirectory())
                        .filter(e -> e.getName().startsWith(packagePath + "/"))
                        .filter(f -> isClass(f))
                        .forEach(e -> fill(jar, e, out));
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(INVALID_PACKAGE_DEF + ": " + url, e);
        }
    }

    private static void fill(Path path, List<byte[]> out) {
        Result.of(() -> Files.readAllBytes(path)).fold(
                out::add,
                e -> {
                    throw new IllegalArgumentException(INVALID_PACKAGE_DEF, e);
                });
    }

    private static void fill(JarFile jar, JarEntry entry, List<byte[]> out) {
        try (var is = jar.getInputStream(entry)) {
            out.add(is.readAllBytes());
        } catch (IOException e) {
            throw new IllegalArgumentException(INVALID_PACKAGE_DEF, e);
        }
    }

    private static boolean isClass(Path path) {
        return path.getFileName().toString().endsWith(".class");
    }

    private static boolean isClass(JarEntry entry) {
        return entry.getName().endsWith(".class");
    }

    private static Path pathOf(URL url) {
        return Result.of(() -> Path.of(url.toURI())).fold(
                Function.identity(),
                e -> {
                    throw new IllegalArgumentException(INVALID_PACKAGE_DEF + ": " + url, e);
                });
    }

    private static String sanitized(String pkg) {
        return pkg.replace('.', '/');
    }
}

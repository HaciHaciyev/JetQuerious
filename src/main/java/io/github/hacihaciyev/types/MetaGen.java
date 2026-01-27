package io.github.hacihaciyev.types;

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
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.lang.constant.ConstantDescs.CD_Class;
import static java.lang.constant.ConstantDescs.CD_Object;
import static java.lang.constant.ConstantDescs.CD_String;
import static java.lang.constant.ConstantDescs.CD_void;

public final class MetaGen {

    static final String FAILED_RESET = "JetQuerious. Failed to reset MetaRegistry. You need to manually clean the bytecode";

    static final String INVALID_PACKAGE_DEF = "JetQuerious. Property: jetquerious.packages. Invalid package definition";

    static final Path META_REGISTRY_BACKUP = Path.of("target/classes/io/github/hacihaciyev/types/MetaRegistry.class.backup");

    static final Path META_REGISTRY_PATH = Path.of("target/classes/io/github/hacihaciyev/types/MetaRegistry.class");

    static final ClassDesc META_REGISTRY_DESC = ClassDesc.of("io.github.hacihaciyev.types.MetaRegistry");

    static final MethodTypeDesc TYPE_META_DESC = MethodTypeDesc.of(ClassDesc.of("io.github.hacihaciyev.types.MetaRegistry$TypeMeta"));

    static final ClassDesc RECORD_DESC = ClassDesc.of("io.github.hacihaciyev.types.MetaRegistry$TypeMeta$Record");

    static final ClassDesc FIELD_DESC = ClassDesc.of("io.github.hacihaciyev.types.MetaRegistry$Field");

    static final ClassDesc JAVA_FUNCTION_DESC = ClassDesc.of("java.util.function.Function");

    static final MethodTypeDesc FIELD_CONSTRUCTOR_DESC = MethodTypeDesc.of(CD_void, CD_String, CD_Class, JAVA_FUNCTION_DESC);

    static final ClassDesc FACTORY_DESC = ClassDesc.of("io.github.hacihaciyev.types.MetaRegistry$RecordFactory");

    static final MethodTypeDesc RECORD_CONSTRUCTOR_DESC = MethodTypeDesc.of(CD_void, CD_Class, FIELD_DESC.arrayType(), FACTORY_DESC);

    private MetaGen() {}

    static void main() {
        resetMetaRegistry();

        var packages = PkgScan.userSpec();
        for (var pkg : packages) {
            var classes = PkgScan.read(pkg);
            for (var type : classes) metaGen(type);
        }
    }

    private static void resetMetaRegistry() {
        try {
            if (!Files.exists(META_REGISTRY_BACKUP)) {
                Files.copy(META_REGISTRY_PATH, META_REGISTRY_BACKUP);
                return;
            }

            Files.copy(META_REGISTRY_BACKUP, META_REGISTRY_PATH, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new IllegalArgumentException(FAILED_RESET, e);
        }
    }

    private static void metaGen(byte[] type) {
        var classFile = ClassFile.of();
        var classModel = classFile.parse(type);

        var attribute = recordAttribute(classModel);
        if (attribute.isEmpty()) return;

        var classDesc = classModel.thisClass().asSymbol();
        var components = attribute.get().components();

        var method = genMetaMethod(classFile, classDesc, components);
        addToMetaRegistry(classFile, method, classDesc);
    }

    private static Optional<RecordAttribute> recordAttribute(ClassModel classModel) {
        for (var attribute : classModel.attributes()) {
            if (attribute instanceof RecordAttribute ra) return Optional.of(ra);
        }
        return Optional.empty();
    }

    private static MethodModel genMetaMethod(ClassFile cf, ClassDesc cd, List<RecordComponentInfo> components) {
        var name = defMethodName(cd);
        var bytes = cf.build(CD_Object, clb -> clb.withMethodBody(name, TYPE_META_DESC, defMethodModifiers(), cob -> {
            cob.loadConstant(components.size());
            cob.anewarray(FIELD_DESC);

            for (int i = 0; i < components.size(); i++) {
                var component = components.get(i);
                var fieldName = component.name().stringValue();
                var fieldEntry = component.descriptor();
                var fieldDesc = ClassDesc.ofDescriptor(fieldEntry.stringValue());

                cob.dup();
                cob.loadConstant(i);

                cob.new_(FIELD_DESC);
                cob.dup();

                cob.ldc(fieldName);

                if (fieldDesc.isPrimitive()) cob.getstatic(wrap(fieldDesc), "TYPE", CD_Class);
                else cob.ldc(fieldDesc);

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
                                MethodTypeDesc.of(fieldDesc)
                        ),
                        MethodTypeDesc.of(wrap(fieldDesc), cd)
                ));

                cob.invokespecial(FIELD_DESC, "<init>", FIELD_CONSTRUCTOR_DESC);
                cob.aastore();
            }

            cob.new_(RECORD_DESC);
            cob.dup_x1();
            cob.swap();

            cob.ldc(cd);
            cob.swap();

            cob.invokespecial(RECORD_DESC, "<init>", MethodTypeDesc.of(CD_void, CD_Class, FIELD_DESC.arrayType()));
            cob.areturn();
        }));

        return cf.parse(bytes)
                .methods().stream()
                .filter(m -> m.methodName().stringValue().equals(name))
                .findFirst()
                .orElseThrow();
    }

    private static String defMethodName(ClassDesc cd) {
        return "_meta_" + cd.descriptorString().replace("/", "_").replace(";", "");
    }

    private static int defMethodModifiers() {
        return ClassFile.ACC_PRIVATE | ClassFile.ACC_STATIC;
    }

    private static ClassDesc wrap(ClassDesc cd) {
        return switch (cd.displayName()) {
            case "int" -> ClassDesc.of("java.lang.Integer");
            case "long" -> ClassDesc.of("java.lang.Long");
            case "double" -> ClassDesc.of("java.lang.Double");
            case "float" -> ClassDesc.of("java.lang.Float");
            case "boolean" -> ClassDesc.of("java.lang.Boolean");
            case "byte" -> ClassDesc.of("java.lang.Byte");
            case "char" -> ClassDesc.of("java.lang.Character");
            case "short" -> ClassDesc.of("java.lang.Short");
            default -> cd;
        };
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

                    cob.invokestatic(META_REGISTRY_DESC, metaMethodName, TYPE_META_DESC);
                    cob.areturn();

                    cob.labelBinding(labelNotEqual);

                    for (var el : cm) cob.with(el);
                });
                return;
            }

            mb.accept(methodElement);
        });
    }

    private static class PkgScan {

        private PkgScan() {}

        static String[] userSpec() {
            var pkgs = System.getProperty("jetquerious.packages");
            if (pkgs != null && !pkgs.isBlank()) return pkgs.split(";");
            return new String[0];
        }

        static List<byte[]> read(String pkgPath) {
            pkgPath = asResPath(pkgPath);
            var result = new ArrayList<byte[]>();

            Thread.currentThread()
                    .getContextClassLoader()
                    .resources(pkgPath)
                    .forEach(url -> readRes(url, result));

            return result;
        }

        static void readRes(URL url, List<byte[]> out) {
            switch (url.getProtocol()) {
                case "file" -> fromDir(url, out);
                default -> throw invalid("unsupported protocol: " + url);
            }
        }

        static void fromDir(URL url, List<byte[]> out) {
            try (var stream = Files.walk(Path.of(url.toURI()))) {
                stream.filter(PkgScan::isClass).forEach(p -> fill(p, out));
            } catch (IOException | URISyntaxException e) {
                throw invalid(url.toString(), e);
            }
        }

        static boolean isClass(Path path) {
            return Files.isRegularFile(path) && path.getFileName().toString().endsWith(".class");
        }

        static void fill(Path path, List<byte[]> out) {
            try {
                out.add(Files.readAllBytes(path));
            } catch (IOException e) {
                throw invalid(path.toString(), e);
            }
        }

        static String asResPath(String pkg) {
            return pkg.replace('.', '/');
        }

        static IllegalArgumentException invalid(String msg) {
            return new IllegalArgumentException(INVALID_PACKAGE_DEF + ": " + msg);
        }

        static IllegalArgumentException invalid(String msg, Exception e) {
            return new IllegalArgumentException(INVALID_PACKAGE_DEF + ": " + msg, e);
        }
    }
}

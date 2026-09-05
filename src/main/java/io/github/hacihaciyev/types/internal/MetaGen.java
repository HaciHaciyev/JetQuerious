package io.github.hacihaciyev.types.internal;

import io.github.hacihaciyev.build_errors.MetaGenException;
import io.github.hacihaciyev.config.Conf;
import io.github.hacihaciyev.sql.internal.value_objects.ParamType;

import java.io.IOException;
import java.lang.classfile.ClassBuilder;
import java.lang.classfile.ClassElement;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.ClassTransform;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.CodeModel;
import java.lang.classfile.FieldModel;
import java.lang.classfile.MethodModel;
import java.lang.classfile.attribute.RecordAttribute;
import java.lang.classfile.attribute.RecordComponentInfo;
import java.lang.classfile.attribute.InnerClassesAttribute;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDesc;
import java.lang.constant.ConstantDescs;
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

    static final String FAILED_TO_INITIALIZE = "JetQuerious. Failed to initialize %s";
    
    static final String NO_VARARGS_ARRAY = "no varargs array found at call site";
    
    static final String PARAM_COUNT_MISMATCH = "expected %d parameters but call site provides %d";
    
    static final String PARAM_TYPE_MISMATCH = "parameter %d expected %s but got %s";
    
    static final String INCONSISTENT_STATIC_INIT = """
            JetQuerious. Class %s constructs %d tracked query object(s) during static initialization, \
            but only %d are assigned directly to a tracked static field. Every JQ/JQ.Read/JQ.Write built \
            during class initialization must be assigned directly to a tracked static field, otherwise \
            build-time parameter verification cannot reliably match query metadata to fields.
            """;

    static final String UNSAFE_STATIC_INIT = """
            JetQuerious. Class %s is not safe for MetaGen initialization. Static initialization must not alias \
            tracked query fields and must not build tracked queries outside direct tracked static field assignment.
            """;

    static final String IGNORED_FIELD_USAGE = "JetQuerious. Parameter verification failed in %s.%s() using field %s.%s: field comes from a MetaGen-ignored class and cannot be verified";

    static final String PARAM_VERIFICATION_FAILED = "JetQuerious. Parameter verification failed in %s.%s() using field %s.%s: %s";

    static final Path META_REGISTRY_BACKUP = Path.of(Conf.INSTANCE.outputDir() + "/io/github/hacihaciyev/types/internal/MetaRegistry.class.backup");

    static final Path META_REGISTRY_PATH = Path.of(Conf.INSTANCE.outputDir() + "/io/github/hacihaciyev/types/internal/MetaRegistry.class");

    static final ClassDesc META_REGISTRY_DESC = ClassDesc.of("io.github.hacihaciyev.types.internal.MetaRegistry");

    static final MethodTypeDesc TYPE_META_DESC = MethodTypeDesc.of(ClassDesc.of("io.github.hacihaciyev.types.internal.TypeMeta"));

    static final ClassDesc RECORD_DESC = ClassDesc.of("io.github.hacihaciyev.types.internal.TypeMeta$Record");

    static final ClassDesc FIELD_DESC = ClassDesc.of("io.github.hacihaciyev.types.internal.Field");

    static final ClassDesc JAVA_FUNCTION_DESC = ClassDesc.of("java.util.function.Function");

    static final MethodTypeDesc FIELD_CONSTRUCTOR_DESC = MethodTypeDesc.of(CD_void, CD_String, CD_Class, JAVA_FUNCTION_DESC);

    static final ClassDesc FACTORY_DESC = ClassDesc.of("io.github.hacihaciyev.types.internal.RecordFactory");

    static final MethodTypeDesc RECORD_CONSTRUCTOR_DESC = MethodTypeDesc.of(CD_void, CD_Class, FIELD_DESC.arrayType(), FACTORY_DESC);

    static final ClassDesc TYPE_INSTANTIATION_EXP_DESC = ClassDesc.of("io.github.hacihaciyev.types.TypeInstantiationException");

    static final MethodTypeDesc TYPE_INSTANTIATION_EXP_CONSTRUCTOR = MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_Class, ClassDesc.of("java.lang.Throwable"));

    static final DirectMethodHandleDesc LAMBDA_METAFACTORY_HANDLE = MethodHandleDesc.ofMethod(
            DirectMethodHandleDesc.Kind.STATIC,
            ClassDesc.of("java.lang.invoke.LambdaMetafactory"), "metafactory",
            MethodTypeDesc.of(
                    ClassDesc.of("java.lang.invoke.CallSite"),
                    ClassDesc.of("java.lang.invoke.MethodHandles$Lookup"),
                    ConstantDescs.CD_String,
                    ClassDesc.of("java.lang.invoke.MethodType"),
                    ClassDesc.of("java.lang.invoke.MethodType"),
                    ClassDesc.of("java.lang.invoke.MethodHandle"),
                    ClassDesc.of("java.lang.invoke.MethodType")
            )
    );

    static final ClassDesc JQ_DESC = ClassDesc.of("io.github.hacihaciyev.sql.JQ");

    static final ClassDesc JQ_READ_DESC = ClassDesc.of("io.github.hacihaciyev.sql.JQ$Read");

    static final ClassDesc JQ_WRITE_DESC = ClassDesc.of("io.github.hacihaciyev.sql.JQ$Write");

    static final ClassDesc RESULT_SET_EXTRACTOR_DESC = ClassDesc.of("io.github.hacihaciyev.jdbc.ResultSetExtractor");

    static final List<ClassDesc> TARGET_OWNERS = List.of(
            ClassDesc.of("io.github.hacihaciyev.jdbc.JetQuerious"), ClassDesc.of("io.github.hacihaciyev.jdbc.ReadOperations"),
            ClassDesc.of("io.github.hacihaciyev.jdbc.WriteOperations"), ClassDesc.of("io.github.hacihaciyev.jdbc.Transactions")
    );

    record TrackedField(ClassDesc owner, String name, ClassDesc type, List<ParamType> paramTypes) {}

    private MetaGen() {}

    private record MethodPair(MethodModel metaMethod, MethodModel factoryMethod) {}

    static void main() {
        MetaRegistryAlter.resetMetaRegistry();

        var packages = Conf.INSTANCE.packages();
        for (var pkg : packages) {
            var classes = PkgScan.read(pkg);
            for (var type : classes) metaGen(type);
        }

        ParamVerifier.verify(Conf.INSTANCE.repositories());
    }

    private static void metaGen(byte[] type) {
        var classFile = ClassFile.of();
        var classModel = classFile.parse(type);

        if (isInaccessible(classModel)) return;

        var attribute = recordAttribute(classModel);
        if (attribute.isEmpty()) return;

        var classDesc = classModel.thisClass().asSymbol();

        var methodPair = genMetaMethod(classFile, classDesc, attribute.get());
        MetaRegistryAlter.addMethodPair(classFile, methodPair, classDesc);
    }

    private static Optional<RecordAttribute> recordAttribute(ClassModel classModel) {
        for (var attribute : classModel.attributes()) {
            if (attribute instanceof RecordAttribute ra) return Optional.of(ra);
        }
        return Optional.empty();
    }

    private static MethodPair genMetaMethod(ClassFile cf, ClassDesc cd, RecordAttribute ra) {
        var components = ra.components();
        var name = defMethodName(cd);
        var factoryName = defFactoryMethodName(cd);
        var factoryMethod = genFactoryMethod(cf, cd, components, factoryName);

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

                cob.invokedynamic(lambdaForFieldAccessor(cd, fieldName, fieldDesc));
                cob.invokespecial(FIELD_DESC, "<init>", FIELD_CONSTRUCTOR_DESC);
                cob.aastore();
            }

            cob.new_(RECORD_DESC);
            cob.dup_x1();
            cob.swap();

            cob.ldc(cd);
            cob.swap();

            cob.invokedynamic(lambdaForRecordFactory(cd, factoryName));
            cob.invokespecial(RECORD_DESC, "<init>", RECORD_CONSTRUCTOR_DESC);
            cob.areturn();
        }));

        var metaMethod = cf.parse(bytes)
                .methods().stream()
                .filter(m -> m.methodName().stringValue().equals(name))
                .findFirst()
                .orElseThrow();

        return new MethodPair(metaMethod, factoryMethod);
    }

    private static String defMethodName(ClassDesc cd) {
        return "_meta_" + cd.descriptorString().replace("/", "_").replace(";", "");
    }

    private static String defFactoryMethodName(ClassDesc cd) {
        return "_factory_" + cd.descriptorString().replace("/", "_").replace(";", "");
    }

    private static MethodModel genFactoryMethod(ClassFile cf, ClassDesc cd, List<RecordComponentInfo> components, String factoryName) {
        var methodDescriptor = MethodTypeDesc.of(cd, CD_Object.arrayType());

        var bytes = cf.build(CD_Object, clb -> clb.withMethodBody(factoryName, methodDescriptor, defMethodModifiers(), cob -> {
            var tryStart = cob.newLabel();
            var tryEnd = cob.newLabel();
            var catchHandler = cob.newLabel();

            cob.exceptionCatch(tryStart, tryEnd, catchHandler, ClassDesc.of("java.lang.Exception"));
            cob.labelBinding(tryStart);

            cob.new_(cd);
            cob.dup();

            for (int i = 0; i < components.size(); i++) {
                var fieldDesc = ClassDesc.ofDescriptor(components.get(i).descriptor().stringValue());

                cob.aload(0);
                cob.loadConstant(i);
                cob.aaload();

                if (fieldDesc.isPrimitive()) {
                    cob.checkcast(wrap(fieldDesc));
                    cob.invokevirtual(wrap(fieldDesc), unboxMethodName(fieldDesc), MethodTypeDesc.of(fieldDesc));
                    continue;
                }

                cob.checkcast(fieldDesc);
            }

            var constructorDesc = MethodTypeDesc.of(CD_void,
                    components.stream().map(c -> ClassDesc.ofDescriptor(c.descriptor().stringValue())).toArray(ClassDesc[]::new));

            cob.invokespecial(cd, "<init>", constructorDesc);

            cob.labelBinding(tryEnd);
            cob.areturn();

            cob.labelBinding(catchHandler);

            cob.new_(TYPE_INSTANTIATION_EXP_DESC);
            cob.dup_x1();
            cob.swap();
            cob.ldc(cd);
            cob.swap();
            cob.invokespecial(TYPE_INSTANTIATION_EXP_DESC, "<init>", TYPE_INSTANTIATION_EXP_CONSTRUCTOR);
            cob.athrow();
        }));

        return cf.parse(bytes)
                .methods().stream()
                .filter(m -> m.methodName().stringValue().equals(factoryName))
                .findFirst()
                .orElseThrow();
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

    private static String unboxMethodName(ClassDesc cd) {
        return switch (cd.displayName()) {
            case "int"     -> "intValue";
            case "long"    -> "longValue";
            case "double"  -> "doubleValue";
            case "float"   -> "floatValue";
            case "boolean" -> "booleanValue";
            case "byte"    -> "byteValue";
            case "char"    -> "charValue";
            case "short"   -> "shortValue";
            default -> throw new MetaGenException("Not a primitive: " + cd);
        };
    }

    private static DynamicCallSiteDesc lambdaForFieldAccessor(ClassDesc cd, String fieldName, ClassDesc fieldDesc) {
        return DynamicCallSiteDesc.of(
                LAMBDA_METAFACTORY_HANDLE,
                "apply",
                MethodTypeDesc.of(JAVA_FUNCTION_DESC),
                accessorLambdaConstantDesc(cd, fieldName, fieldDesc)
        );
    }

    private static ConstantDesc[] accessorLambdaConstantDesc(ClassDesc cd, String fieldName, ClassDesc fieldDesc) {
        return new ConstantDesc[]{
                samSignature(),
                accessorMethodHandle(cd, fieldName, fieldDesc),
                accessorActualSignature(cd, fieldDesc)
        };
    }

    private static MethodTypeDesc samSignature() {
        return MethodTypeDesc.of(CD_Object, CD_Object);
    }

    private static DirectMethodHandleDesc accessorMethodHandle(ClassDesc cd, String fieldName, ClassDesc fieldDesc) {
        return MethodHandleDesc.ofMethod(
                DirectMethodHandleDesc.Kind.VIRTUAL,
                cd,
                fieldName,
                MethodTypeDesc.of(fieldDesc)
        );
    }

    private static MethodTypeDesc accessorActualSignature(ClassDesc cd, ClassDesc fieldDesc) {
        return MethodTypeDesc.of(wrap(fieldDesc), cd);
    }

    private static DynamicCallSiteDesc lambdaForRecordFactory(ClassDesc cd, String factoryName) {
        return DynamicCallSiteDesc.of(
                LAMBDA_METAFACTORY_HANDLE,
                "create",
                MethodTypeDesc.of(FACTORY_DESC),
                factoryLambdaConstantDesc(cd, factoryName)
        );
    }

    private static ConstantDesc[] factoryLambdaConstantDesc(ClassDesc cd, String factoryName) {
        return new ConstantDesc[]{
                factorySamSignature(),
                factoryMethodHandle(cd, factoryName),
                factoryActualSignature(cd)
        };
    }

    private static MethodTypeDesc factorySamSignature() {
        return MethodTypeDesc.of(CD_Object, CD_Object.arrayType());
    }

    private static DirectMethodHandleDesc factoryMethodHandle(ClassDesc cd, String factoryName) {
        return MethodHandleDesc.ofMethod(
                DirectMethodHandleDesc.Kind.STATIC,
                META_REGISTRY_DESC,
                factoryName,
                MethodTypeDesc.of(cd, CD_Object.arrayType())
        );
    }

    private static MethodTypeDesc factoryActualSignature(ClassDesc cd) {
        return MethodTypeDesc.of(cd, CD_Object.arrayType());
    }

    private static boolean isInaccessible(ClassModel classModel) {
        var thisClass = classModel.thisClass().asSymbol();
    
        for (var attr : classModel.attributes()) {
            if (!(attr instanceof InnerClassesAttribute ica)) continue;
    
            for (var info : ica.classes()) {
                if (!info.innerClass().asSymbol().equals(thisClass)) continue;
    
                int flags = info.flagsMask();
                return (flags & ClassFile.ACC_PUBLIC) == 0;
            }
        }
    
        return false;
    }

    private static class MetaRegistryAlter {

        private MetaRegistryAlter() {}

        static void resetMetaRegistry() {
            try {
                if (!Files.exists(META_REGISTRY_BACKUP)) {
                    Files.copy(META_REGISTRY_PATH, META_REGISTRY_BACKUP);
                    return;
                }

                Files.copy(META_REGISTRY_BACKUP, META_REGISTRY_PATH, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                throw new MetaGenException(FAILED_RESET, e);
            }
        }

        static void addMethodPair(ClassFile cf, MethodPair pair, ClassDesc recordClass) {
            try {
                var registryBytes = Files.readAllBytes(META_REGISTRY_PATH);

                var withFactory = appendMethod(cf, registryBytes, pair.factoryMethod());
                var withUpdatedMeta = updateMetaMethod(cf, withFactory, pair.metaMethod(), recordClass);
                var withBothMethods = appendMethod(cf, withUpdatedMeta, pair.metaMethod());

                Files.write(META_REGISTRY_PATH, withBothMethods);
            } catch (IOException e) {
                throw new MetaGenException(INVALID_PACKAGE_DEF, e);
            }
        }

        static byte[] updateMetaMethod(ClassFile cf, byte[] classBytes,
                                       MethodModel newMethod, ClassDesc recordClass) {
            var newMethodName = newMethod.methodName().stringValue();

            return cf.transformClass(
                    cf.parse(classBytes),
                    (clb, element) -> {
                        if (isMetaMethod(element)) {
                            injectIfStatement(clb, (MethodModel) element, recordClass, newMethodName);
                            return;
                        }

                        clb.accept(element);
                    }
            );
        }

        static boolean isMetaMethod(ClassElement element) {
            return element instanceof MethodModel mm && mm.methodName().stringValue().equals("meta");
        }

        static void injectIfStatement(ClassBuilder clb, MethodModel metaMethod,
                                              ClassDesc recordClass, String newMethodName) {
            clb.transformMethod(metaMethod, (mb, methodElement) -> {
                if (methodElement instanceof CodeModel cm) {
                    mb.withCode(cob -> generateIfStatement(cob, cm, recordClass, newMethodName));
                    return;
                }

                mb.accept(methodElement);
            });
        }

        static void generateIfStatement(CodeBuilder cob, CodeModel originalCode,
                                        ClassDesc recordClass, String metaMethodName) {
            cob.aload(0);
            cob.ldc(recordClass);

            var notEqual = cob.newLabel();
            cob.if_acmpne(notEqual);

            cob.invokestatic(META_REGISTRY_DESC, metaMethodName, TYPE_META_DESC);
            cob.areturn();

            cob.labelBinding(notEqual);
            for (var element : originalCode) cob.with(element);
        }

        static byte[] appendMethod(ClassFile cf, byte[] classBytes, MethodModel method) {
            return cf.transformClass(cf.parse(classBytes), ClassTransform.endHandler(clb -> clb.accept(method)));
        }
    }

    private static class PkgScan {

        private PkgScan() {}

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

        static MetaGenException invalid(String msg) {
            return new MetaGenException(INVALID_PACKAGE_DEF + ": " + msg);
        }

        static MetaGenException invalid(String msg, Exception e) {
            return new MetaGenException(INVALID_PACKAGE_DEF + ": " + msg, e);
        }
    }

    private static class ParamVerifier {
    
        private ParamVerifier() {}

        static void verify(String[] repositories) {
            var classModels = new ArrayList<ClassModel>();
            for (var pkg : repositories) {
                for (var bytes : PkgScan.read(pkg)) classModels.add(ClassFile.of().parse(bytes));
            }
    
            var tracked = collectTrackedFields(classModels);
            var ignoredOwners = ignoredTrackedOwners(classModels);
            for (var model : classModels) verifyUsagesInClass(model, tracked, ignoredOwners);
        }
    
        static List<TrackedField> collectTrackedFields(List<ClassModel> classModels) {
            var tracked = new ArrayList<TrackedField>();
    
            try (var registry = BuildTimeRegistry.open()) {
                traceModels(registry, classModels, tracked);
            }
    
            return tracked;
        }
        
        static void traceModels(BuildTimeRegistry registry, List<ClassModel> classModels, List<TrackedField> tracked) {
            var scanLoader = new ScanClassLoader(Thread.currentThread().getContextClassLoader());

            for (var model : classModels) {
                if (isInaccessible(model)) continue;
                
                var owner = model.thisClass().asSymbol();
        
                var shouldInit = model.fields().stream().anyMatch(field -> isStatic(field) && isTrackedType(ClassDesc.ofDescriptor(field.fieldType().stringValue())));
                if (!shouldInit) continue;
                if (isIgnored(owner)) continue;
                if (!isSafeForInitialization(model, owner)) throw new MetaGenException(UNSAFE_STATIC_INIT.formatted(owner.displayName()));
        
                var fieldWrites = trackedFieldWritesInClinit(model, owner);
                var sizeBefore = registry.size();
        
                initClass(scanLoader, owner, model);
        
                var constructed = registry.size() - sizeBefore;
                if (constructed != fieldWrites.size()) {
                    throw new MetaGenException(INCONSISTENT_STATIC_INIT.formatted(owner.displayName(), constructed, fieldWrites.size()));
                }
        
                for (var write : fieldWrites) tracked.add(new TrackedField(owner, write.fieldName(), write.fieldType(), registry.next().paramTypes()));
            }
        }

        private static final class ScanClassLoader extends ClassLoader {
            ScanClassLoader(ClassLoader parent) {
                super(parent);
            }

            void defineIfAbsent(String binaryName, byte[] bytes) {
                synchronized (getClassLoadingLock(binaryName)) {
                    if (findLoadedClass(binaryName) == null) {
                        defineClass(binaryName, bytes, 0, bytes.length);
                    }
                }
            }
        }
    
        static boolean isStatic(FieldModel field) {
            return (field.flags().flagsMask() & ClassFile.ACC_STATIC) != 0;
        }
    
        static boolean isTrackedType(ClassDesc fieldType) {
            return fieldType.equals(JQ_DESC)||fieldType.equals(JQ_READ_DESC)||fieldType.equals(JQ_WRITE_DESC);
        }

        static List<BytecodeTypeInterpreter.Event.FieldWrite> trackedFieldWritesInClinit(ClassModel model, ClassDesc owner) {
            var clinit = model.methods().stream()
                .filter(m -> m.methodName().stringValue().equals("<clinit>"))
                .findFirst();
            if (clinit.isEmpty()) return List.of();
        
            var code = clinit.get().code();
            if (code.isEmpty()) return List.of();
        
            var events = new BytecodeTypeInterpreter().run(code.get().elementList());
            var writes = new ArrayList<BytecodeTypeInterpreter.Event.FieldWrite>();
            
            for (var event : events) {
                if (event instanceof BytecodeTypeInterpreter.Event.FieldWrite fw && fw.owner().equals(owner) && isTrackedType(fw.fieldType())) writes.add(fw);
            }
            return writes;
        }

        static boolean isSafeForInitialization(ClassModel model, ClassDesc owner) {
            var clinit = model.methods().stream()
                .filter(m -> m.methodName().stringValue().equals("<clinit>"))
                .findFirst();
            if (clinit.isEmpty()) return true;

            var code = clinit.get().code();
            if (code.isEmpty()) return true;

            var events = new BytecodeTypeInterpreter().run(code.get().elementList());

            for (var event : events) {
                if (event instanceof BytecodeTypeInterpreter.Event.FieldRead fr
                    && fr.owner().equals(owner)
                    && isTrackedType(fr.fieldType())) {
                    return false;
                }
            }

            return true;
        }

        static boolean isIgnored(ClassDesc owner) {
            var binaryName = owner.descriptorString()
                .substring(1, owner.descriptorString().length() - 1)
                .replace('/', '.');
            var simpleName = owner.displayName();
            for (var entry : Conf.INSTANCE.metaGenIgnore()) {
                var candidate = entry.trim();
                if (!candidate.isEmpty() && (candidate.equals(binaryName) || candidate.equals(simpleName))) return true;
            }
            return false;
        }

        static java.util.Set<ClassDesc> ignoredTrackedOwners(List<ClassModel> classModels) {
            var ignored = new java.util.HashSet<ClassDesc>();
            for (var model : classModels) {
                var owner = model.thisClass().asSymbol();
                if (!isIgnored(owner)) continue;
                var hasTrackedFields = model.fields().stream().anyMatch(field -> isStatic(field) && isTrackedType(ClassDesc.ofDescriptor(field.fieldType().stringValue())));
                if (hasTrackedFields) ignored.add(owner);
            }
            return ignored;
        }

        static void initClass(ScanClassLoader scanLoader, ClassDesc owner, ClassModel model) {
            var binaryName = owner.descriptorString()
                .substring(1, owner.descriptorString().length() - 1)
                .replace('/', '.');
            try {
                var bytes = ClassFile.of().transformClass(model, ClassTransform.ACCEPT_ALL);
                scanLoader.defineIfAbsent(binaryName, bytes);
                Class.forName(binaryName, true, scanLoader);
            } catch (ClassNotFoundException | LinkageError e) {
                throw new MetaGenException(FAILED_TO_INITIALIZE.formatted(owner.displayName()), e);
            }
        }
    
        static void verifyUsagesInClass(ClassModel model, List<TrackedField> tracked) {
            verifyUsagesInClass(model, tracked, java.util.Set.of());
        }

        static void verifyUsagesInClass(ClassModel model, List<TrackedField> tracked, java.util.Set<ClassDesc> ignoredOwners) {
            for (var method : model.methods()) {
                var code = method.code();
    
                if (code.isEmpty()) continue;
    
                var interpreter = new BytecodeTypeInterpreter();
                var events = interpreter.run(code.get().elementList());
    
                for (var event : events) {
                    if (event instanceof BytecodeTypeInterpreter.Event.MethodCall call) verifyCall(call, tracked, ignoredOwners, model, method);
                }
            }
        }
        
        static void verifyCall(BytecodeTypeInterpreter.Event.MethodCall call, List<TrackedField> tracked, java.util.Set<ClassDesc> ignoredOwners, ClassModel callerClass, MethodModel callerMethod) {
            if (!isTargetOwner(call.owner())) return;
            if (call.arguments().isEmpty()) return;
        
            var sourceField = sourceFieldOf(call.arguments().get(0));
            if (sourceField != null && ignoredOwners.contains(sourceField.owner())) {
                throw new MetaGenException(IGNORED_FIELD_USAGE.formatted(
                    callerClass.thisClass().asSymbol().displayName(),
                    callerMethod.methodName().stringValue(),
                    sourceField.owner().displayName(),
                    sourceField.fieldName()
                ));
            }

            var field = trackedFieldOf(call.arguments().get(0), tracked);
            if (field == null) return;
            
            if (field.type().equals(RESULT_SET_EXTRACTOR_DESC)) return;
        
            var varargs = lastArrayArgument(call.arguments());
            if (varargs == null) {
                reportMismatch(callerClass, callerMethod, field, NO_VARARGS_ARRAY);
                return;
            }
        
            verifyArgsAgainstParamTypes(callerClass, callerMethod, field, varargs);
        }
    
        static boolean isTargetOwner(ClassDesc owner) {
            return TARGET_OWNERS.stream().anyMatch(owner::equals);
        }
    
        static TrackedField trackedFieldOf(SymbolicType arg, List<TrackedField> tracked) {
            if (!(arg instanceof SymbolicType.FromField(var owner, var name, var _))) return null;
    
            for (var field : tracked) {
                if (field.owner().equals(owner) && field.name().equals(name)) return field;
            }
    
            return null;
        }
    
        static BytecodeTypeInterpreter.Event.FieldRead sourceFieldOf(SymbolicType arg) {
            if (arg instanceof SymbolicType.FromField(var owner, var name, var type)) {
                return new BytecodeTypeInterpreter.Event.FieldRead(owner, name, type);
            }
            return null;
        }

        static SymbolicType.ArrayBuild lastArrayArgument(List<SymbolicType> arguments) {
            for (int i = arguments.size() - 1; i >= 0; i--) {
                if (arguments.get(i) instanceof SymbolicType.ArrayBuild build) return build;
            }
    
            return null;
        }
    
        static void verifyArgsAgainstParamTypes(ClassModel callerClass, MethodModel callerMethod, TrackedField field, SymbolicType.ArrayBuild varargs) {
            var expected = field.paramTypes();
            var actual = varargs.elements();
        
            if (actual.length != expected.size()) {
                reportMismatch(callerClass, callerMethod, field, PARAM_COUNT_MISMATCH.formatted(expected.size(), actual.length));
                return;
            }
        
            for (int i = 0; i < expected.size(); i++) verifySingleArg(callerClass, callerMethod, field, expected.get(i), actual[i], i);
        }
        
        static void verifySingleArg(ClassModel callerClass, MethodModel callerMethod, TrackedField field, ParamType expected, SymbolicType actual, int index) {
            if (actual instanceof SymbolicType.Unknown) return;
        
            var expectedDesc = ClassDesc.of(expected._type().getName());
            if (!actual.type().equals(expectedDesc)) {
                reportMismatch(callerClass, callerMethod, field, PARAM_TYPE_MISMATCH.formatted(index, expected._type().getName(), actual.type().displayName()));
            }
        }
        
        static void reportMismatch(ClassModel callerClass, MethodModel callerMethod, TrackedField field, String reason) {
            throw new MetaGenException(PARAM_VERIFICATION_FAILED.formatted(
                callerClass.thisClass().asSymbol().displayName(),
                callerMethod.methodName().stringValue(),
                field.owner().displayName(),
                field.name(),
                reason
            ));
        }
    }
}
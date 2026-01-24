import io.github.hacihaciyev.util.Result;

import static java.lang.constant.ConstantDescs.CD_Class;
import static java.lang.constant.ConstantDescs.CD_Object;
import static java.lang.constant.ConstantDescs.CD_String;
import static java.lang.constant.ConstantDescs.CD_void;

static final String INVALID_PACKAGE_DEF = "JetQuerious. Property: jetquerious.packages. Invalid package definition";
static final ClassDesc TYPE_META_DESC = ClassDesc.of("io.github.hacihaciyev.types.MetaRegistry$TypeMeta");
static final ClassDesc FIELD_DESC = ClassDesc.of("io.github.hacihaciyev.types.MetaRegistry$Field");
static final MethodTypeDesc FIELD_CONSTRUCTOR_DESC = MethodTypeDesc.of(CD_void, CD_String, CD_Class, ClassDesc.of("java.util.function.Function"));

void main() {
    var packages = userSpec();
    for (var pkg : packages) {
        for (var type : readPackage(sanitized(pkg))) metaGen(type);
    }
}

void metaGen(byte[] type) {
    var classFile = ClassFile.of();
    var classModel = classFile.parse(type);

    var attribute = recordAttribute(classModel);
    if (attribute.isEmpty()) return;

    var classDesc = classModel.thisClass().asSymbol();
    var components = attribute.get().components();

    var method = buildMethod(classFile, classDesc, components);
    addToMetaRegistry(classFile, method);
}

MethodModel buildMethod(ClassFile cf, ClassDesc cd, List<RecordComponentInfo> components) {
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

void addToMetaRegistry(ClassFile cf, MethodModel method) {

}

Optional<RecordAttribute> recordAttribute(ClassModel classModel) {
    for (var attribute : classModel.attributes()) {
        if (attribute instanceof RecordAttribute) return Optional.of((RecordAttribute) attribute);
    }
    return Optional.empty();
}

String[] userSpec() {
    var pkgs = System.getProperty("jetquerious.packages");
    if (pkgs != null && !pkgs.isBlank()) return pkgs.split(";");
    return new String[0];
}

List<byte[]> readPackage(String resourcePath) {
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

void fromDir(URL url, List<byte[]> out) {
    try (var stream = Files.walk(pathOf(url))) {
        stream.filter(Files::isRegularFile)
                .filter(this::isClass)
                .forEach(p -> fill(p, out));
    } catch (IOException e) {
        throw new IllegalArgumentException(INVALID_PACKAGE_DEF + ": " + url, e);
    }
}

void fromJar(URL url, String packagePath, List<byte[]> out) {
    try {
        var conn = (JarURLConnection) url.openConnection();
        try (var jar = conn.getJarFile()) {
            jar.stream()
                    .filter(e -> !e.isDirectory())
                    .filter(e -> e.getName().startsWith(packagePath + "/"))
                    .filter(this::isClass)
                    .forEach(e -> fill(jar, e, out));
        }
    } catch (IOException e) {
        throw new IllegalArgumentException(INVALID_PACKAGE_DEF + ": " + url, e);
    }
}

void fill(Path path, List<byte[]> out) {
    Result.of(() -> Files.readAllBytes(path)).fold(
            out::add,
            e -> {
                throw new IllegalArgumentException(INVALID_PACKAGE_DEF, e);
            });
}

void fill(JarFile jar, JarEntry entry, List<byte[]> out) {
    try (var is = jar.getInputStream(entry)) {
        out.add(is.readAllBytes());
    } catch (IOException e) {
        throw new IllegalArgumentException(INVALID_PACKAGE_DEF, e);
    }
}

boolean isClass(Path path) {
    return path.getFileName().toString().endsWith(".class");
}

boolean isClass(JarEntry entry) {
    return entry.getName().endsWith(".class");
}

Path pathOf(URL url) {
    return Result.of(() -> Path.of(url.toURI())).fold(
            Function.identity(),
            e -> {
                throw new IllegalArgumentException(INVALID_PACKAGE_DEF + ": " + url, e);
            });
}

String sanitized(String pkg) {
    return pkg.replace('.', '/');
}

static final ClassDesc RECORD_DESC = ClassDesc.of("io.github.hacihaciyev.types.MetaRegistry$TypeMeta$Record");
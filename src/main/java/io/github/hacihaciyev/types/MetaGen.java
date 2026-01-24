import io.github.hacihaciyev.util.Result;

static final String INVALID_PACKAGE_DEF = "JetQuerious. Property: jetquerious.packages. Invalid package definition";

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

MethodModel buildMethod(ClassFile classFile, ClassDesc classDesc, List<RecordComponentInfo> components) {
    var methodName = "_meta_" + classDesc.displayName().replace(".", "_");

    var classBytes = classFile.build(ConstantDescs.CD_Object, clb -> {
        clb.withMethod(
                methodName,
                MethodTypeDesc.of(ClassDesc.of("io.github.hacihaciyev.types.MetaRegistry$TypeMeta")),
                ClassFile.ACC_PRIVATE | ClassFile.ACC_STATIC,
                body -> body.withCode(cob -> {
                    cob.loadConstant(components.size());
                    cob.anewarray(ClassDesc.of("io.github.hacihaciyev.types.MetaRegistry$Field"));

                    for (int i = 0; i < components.size(); i++) {
                        var component = components.get(i);

                        cob.dup();
                        cob.loadConstant(i);

                        // TODO

                        cob.aastore();
                    }

                    cob.aconst_null();
                    cob.areturn();
                })
        );
    });

    var classModel = classFile.parse(classBytes);

    return classModel.methods().stream()
            .filter(m -> m.methodName().stringValue().equals(methodName))
            .findFirst()
            .orElseThrow();
}

void addToMetaRegistry(ClassFile classFile, Object method) {

}

Optional<RecordAttribute> recordAttribute(ClassModel classModel) {
    for (var attribute : classModel.attributes()) {
        if (attribute instanceof RecordAttribute) return Optional.of((RecordAttribute) attribute);
    }
    return Optional.empty();
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

String[] userSpec() {
    var pkgs = System.getProperty("jetquerious.packages");
    if (pkgs != null && !pkgs.isBlank()) return pkgs.split(";");
    return new String[0];
}

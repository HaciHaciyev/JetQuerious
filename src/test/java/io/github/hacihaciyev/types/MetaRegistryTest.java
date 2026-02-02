package io.github.hacihaciyev.types;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

@ExtendWith(MetaGenExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MetaRegistryTest {

    record Person(String name, int age) {}
    record Empty() {}
    record AllPrimitives(int i, long l, double d, float f, boolean z, byte b, char c, short s) {}
    record Mixed(String str, int num, List<String> list, int[] array) {}
    record Nested(Person person, String extra) {}

    @Test
    @Order(1)
    void testMethodsCreated() throws Exception {
        var cf = ClassFile.of();
        var registryPath = Path.of("target/classes/io/github/hacihaciyev/types/MetaRegistry.class");
        var registry = cf.parse(Files.readAllBytes(registryPath));
        
        assertTrue(hasMethod(registry, "_meta_Lio_github_hacihaciyev_types_MetaRegistryTest$Person"));
        assertTrue(hasMethod(registry, "_factory_Lio_github_hacihaciyev_types_MetaRegistryTest$Person"));
        assertTrue(hasMethod(registry, "_meta_Lio_github_hacihaciyev_types_MetaRegistryTest$Empty"));
        assertTrue(hasMethod(registry, "_factory_Lio_github_hacihaciyev_types_MetaRegistryTest$Empty"));
        assertTrue(hasMethod(registry, "_meta_Lio_github_hacihaciyev_types_MetaRegistryTest$AllPrimitives"));
        assertTrue(hasMethod(registry, "_factory_Lio_github_hacihaciyev_types_MetaRegistryTest$AllPrimitives"));
        assertTrue(hasMethod(registry, "_meta_Lio_github_hacihaciyev_types_MetaRegistryTest$Mixed"));
        assertTrue(hasMethod(registry, "_factory_Lio_github_hacihaciyev_types_MetaRegistryTest$Mixed"));
        assertTrue(hasMethod(registry, "_meta_Lio_github_hacihaciyev_types_MetaRegistryTest$Nested"));
        assertTrue(hasMethod(registry, "_factory_Lio_github_hacihaciyev_types_MetaRegistryTest$Nested"));
    }

    @Test
    @Order(2)
    void testPersonMeta() {
        var meta = (MetaRegistry.TypeMeta.Record<?>) MetaRegistry.meta(Person.class);
        
        assertEquals(Person.class, meta.type());
        assertEquals(2, meta.fields().length);
        
        assertEquals("name", meta.fields()[0].name());
        assertEquals(String.class, meta.fields()[0].type());
        
        assertEquals("age", meta.fields()[1].name());
        assertEquals(int.class, meta.fields()[1].type());
    }

    @Test
    @Order(3)
    void testAccessors() {
        var meta = (MetaRegistry.TypeMeta.Record<Person>) MetaRegistry.meta(Person.class);
        var person = new Person("Alice", 30);
        
        assertEquals("Alice", meta.fields()[0].accessor().apply(person));
        assertEquals(30, meta.fields()[1].accessor().apply(person));
    }

    @Test
    @Order(4)
    void testEmptyRecord() {
        var meta = (MetaRegistry.TypeMeta.Record<?>) MetaRegistry.meta(Empty.class);
        assertEquals(0, meta.fields().length);
    }

    @Test
    @Order(5)
    void testAllPrimitives() {
        var meta = (MetaRegistry.TypeMeta.Record<?>) MetaRegistry.meta(AllPrimitives.class);
        
        assertEquals(8, meta.fields().length);
        assertEquals(int.class, meta.fields()[0].type());
        assertEquals(long.class, meta.fields()[1].type());
        assertEquals(double.class, meta.fields()[2].type());
        assertEquals(float.class, meta.fields()[3].type());
        assertEquals(boolean.class, meta.fields()[4].type());
        assertEquals(byte.class, meta.fields()[5].type());
        assertEquals(char.class, meta.fields()[6].type());
        assertEquals(short.class, meta.fields()[7].type());
    }

    @Test
    @Order(6)
    void testMixedTypes() {
        var meta = (MetaRegistry.TypeMeta.Record<?>) MetaRegistry.meta(Mixed.class);
        
        assertEquals(4, meta.fields().length);
        assertEquals(String.class, meta.fields()[0].type());
        assertEquals(int.class, meta.fields()[1].type());
        assertEquals(List.class, meta.fields()[2].type());
        assertEquals(int[].class, meta.fields()[3].type());
    }

    @Test
    @Order(7)
    void testNestedRecord() {
        var meta = (MetaRegistry.TypeMeta.Record<Nested>) MetaRegistry.meta(Nested.class);
        var nested = new Nested(new Person("Bob", 25), "test");
        
        assertEquals(new Person("Bob", 25), meta.fields()[0].accessor().apply(nested));
        assertEquals("test", meta.fields()[1].accessor().apply(nested));
    }

    @Test
    @Order(8)
    void testNonRecordReturnsNone() {
        assertEquals(MetaRegistry.TypeMeta.NONE, MetaRegistry.meta(String.class));
        assertEquals(MetaRegistry.TypeMeta.NONE, MetaRegistry.meta(Object.class));
        assertEquals(MetaRegistry.TypeMeta.NONE, MetaRegistry.meta(Integer.class));
    }

    @ParameterizedTest
    @MethodSource("recordFactoryCases")
    @Order(9)
    <T> void testFactoryFromAccessorsGeneric(Class<T> type, Supplier<T> instanceSupplier) throws TypeInstantiationException {
        var meta = (MetaRegistry.TypeMeta.Record<T>) MetaRegistry.meta(type);

        var original = instanceSupplier.get();

        Object[] args = Arrays.stream(meta.fields())
                .map(f -> f.accessor().apply(original))
                .toArray();

        var copy = meta.factory().create(args);

        assertEquals(original, copy);
        assertNotSame(original, copy);
    }

    static Stream<Arguments> recordFactoryCases() {
        return Stream.of(
                Arguments.of(
                        Person.class,
                        (Supplier<Person>) () -> new Person("Alice", 30)
                ),
                Arguments.of(
                        Empty.class,
                        (Supplier<Empty>) Empty::new
                ),
                Arguments.of(
                        AllPrimitives.class,
                        (Supplier<AllPrimitives>) () ->
                                new AllPrimitives(1, 2L, 3.0, 4.0f, true, (byte) 5, 'c', (short) 6)
                ),
                Arguments.of(
                        Mixed.class,
                        (Supplier<Mixed>) () ->
                                new Mixed("str", 42, List.of("a", "b"), new int[]{1, 2, 3})
                ),
                Arguments.of(
                        Nested.class,
                        (Supplier<Nested>) () ->
                                new Nested(new Person("Bob", 25), "test")
                )
        );
    }

    static boolean hasMethod(ClassModel model, String methodName) {
        return model.methods().stream()
            .anyMatch(m -> m.methodName().stringValue().equals(methodName));
    }
}
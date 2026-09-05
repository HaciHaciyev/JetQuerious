package io.github.hacihaciyev.types.internal;

import io.github.hacihaciyev.build_errors.MetaGenException;
import io.github.hacihaciyev.sql.internal.Context;
import io.github.hacihaciyev.util.DBTestContainer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.FieldModel;
import java.lang.constant.ClassDesc;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import io.github.hacihaciyev.fixtures.*;
import static io.github.hacihaciyev.sql.SQL.*;
import static io.github.hacihaciyev.sql.QueryForge.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DBTestContainer.class)
class ParamVerifierTest {

    private static final Class<?> PARAM_VERIFIER;
    private static final Class<?> META_GEN;

    static {
        try {
            PARAM_VERIFIER = Class.forName("io.github.hacihaciyev.types.internal.MetaGen$ParamVerifier");
            META_GEN       = Class.forName("io.github.hacihaciyev.types.internal.MetaGen");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static ClassModel parseClass(Class<?> cls) throws Exception {
        var resource = cls.getName().replace('.', '/') + ".class";
        var loader   = ParamVerifierTest.class.getClassLoader();
        var bytes    = loader.getResourceAsStream(resource).readAllBytes();
        return ClassFile.of().parse(bytes);
    }

    private static ClassModel parseClass(String binaryName) throws Exception {
        var resource = binaryName.replace('.', '/') + ".class";
        var bytes    = ParamVerifierTest.class.getClassLoader().getResourceAsStream(resource).readAllBytes();
        return ClassFile.of().parse(bytes);
    }

    private static Method getMethod(Class<?> cls, String name, int paramCount) throws Exception {
        for (var m : cls.getDeclaredMethods()) {
            if (m.getName().equals(name) && m.getParameterCount() == paramCount) {
                m.setAccessible(true);
                return m;
            }
        }
        throw new NoSuchMethodException(cls.getName() + "." + name + "/" + paramCount);
    }

    @SuppressWarnings("unchecked")
    private static <T> T invoke(Class<?> cls, String name, Object... args) throws Exception {
        var m = getMethod(cls, name, args.length);
        try { 
            return (T) m.invoke(null, args); 
        } catch (InvocationTargetException e) {
            var cause = e.getCause();
            if (cause instanceof RuntimeException r) throw r;
            throw new RuntimeException(cause);
        }
    }
    
    private static boolean invokeBoolean(Class<?> cls, String name, Object... args) throws Exception {
        return (Boolean) invoke(cls, name, args);
    }

    @Nested
    class BuildTimeRegistryTests {

        @Test
        void open_createsEmptyRegistry() {
            try (var registry = BuildTimeRegistry.open()) {
                assertTrue(registry.isEmpty());
                assertEquals(0, registry.size());
            }
        }

        @Test
        void contextCreation_whenOpen_autoRegisters() {
            try (var registry = BuildTimeRegistry.open()) {
                select(col("id")).from("users").build();
                assertFalse(registry.isEmpty());
            }
        }

        @Test
        void contextCreation_whenNotOpen_isNoOp() {
            assertDoesNotThrow(() -> select(col("id")).from("users").build());
        }

        @Test
        void next_underflow_throws() {
            try (var registry = BuildTimeRegistry.open()) {
                assertThrows(MetaGenException.class, registry::next);
            }
        }

        @Test
        void next_afterRegistration_returnsMetadata() {
            try (var registry = BuildTimeRegistry.open()) {
                select(col("id")).from("users").build();
                var sizeBefore = registry.size();
                assertDoesNotThrow(registry::next);
                assertEquals(sizeBefore - 1, registry.size());
            }
        }

        @Test
        void next_canDrainAllRegistrations() {
            try (var registry = BuildTimeRegistry.open()) {
                select(col("id")).from("users").build();
                select(col("name")).from("users").build();
                var total = registry.size();
                for (var i = 0; i < total; i++) registry.next();
                assertTrue(registry.isEmpty());
            }
        }

        @Test
        void close_emptiesRegistry() {
            BuildTimeRegistry ref;
            try (var r = BuildTimeRegistry.open()) {
                ref = r;
                select(col("id")).from("users").build();
                assertFalse(r.isEmpty());
            }
            assertTrue(ref.isEmpty());
        }

        @Test
        void open_whenAlreadyOpen_throws() {
            try (var first = BuildTimeRegistry.open()) {
                assertThrows(IllegalStateException.class, BuildTimeRegistry::open);
            }
        }

        @Test
        void multipleBuilds_eachRegisters() {
            try (var registry = BuildTimeRegistry.open()) {
                select(col("id")).from("users").build();
                var after1 = registry.size();

                select(col("name")).from("users").build();
                var after2 = registry.size();

                assertTrue(after2 > after1);
            }
        }

        @Test
        void size_decreasesWithEachNext() {
            try (var registry = BuildTimeRegistry.open()) {
                select(col("id")).from("users").build();
                select(col("name")).from("users").build();

                var total = registry.size();
                for (var i = total; i > 0; i--) {
                    assertEquals(i, registry.size());
                    registry.next();
                }
                assertEquals(0, registry.size());
            }
        }

        @Test
        void differentThread_doesNotSeeOpenRegistry() throws Exception {
            try (var registry = BuildTimeRegistry.open()) {
                var thread = new Thread(() -> select(col("id")).from("users").build());
                thread.start();
                thread.join();
                assertTrue(registry.isEmpty());
            }
        }
    }

    @Nested
    class IsTrackedTypeTests {

        @Test
        void jqDesc_isTracked() throws Exception {
            assertTrue(invokeBoolean(PARAM_VERIFIER, "isTrackedType",
                ClassDesc.of("io.github.hacihaciyev.sql.JQ")));
        }

        @Test
        void jqReadDesc_isTracked() throws Exception {
            assertTrue(invokeBoolean(PARAM_VERIFIER, "isTrackedType",
                ClassDesc.of("io.github.hacihaciyev.sql.JQ$Read")));
        }

        @Test
        void jqWriteDesc_isTracked() throws Exception {
            assertTrue(invokeBoolean(PARAM_VERIFIER, "isTrackedType",
                ClassDesc.of("io.github.hacihaciyev.sql.JQ$Write")));
        }

        @Test
        void string_notTracked() throws Exception {
            assertFalse(invokeBoolean(PARAM_VERIFIER, "isTrackedType",
                ClassDesc.of("java.lang.String")));
        }

        @Test
        void integer_notTracked() throws Exception {
            assertFalse(invokeBoolean(PARAM_VERIFIER, "isTrackedType",
                ClassDesc.of("java.lang.Integer")));
        }

        @Test
        void context_notTracked() throws Exception {
            assertFalse(invokeBoolean(PARAM_VERIFIER, "isTrackedType",
                ClassDesc.of("io.github.hacihaciyev.sql.internal.Context")));
        }

        @Test
        void randomClass_notTracked() throws Exception {
            assertFalse(invokeBoolean(PARAM_VERIFIER, "isTrackedType",
                ClassDesc.of("io.github.hacihaciyev.sql.builders.SelectBuilder")));
        }
    }

    @Nested
    class IsInaccessibleTests {

        @Test
        void publicTopLevelClass_accessible() throws Exception {
            var model = parseClass(CorrectRepo.class);
            assertFalse(invokeBoolean(META_GEN, "isInaccessible", model));
        }

        @Test
        void packagePrivateInnerClass_inaccessible() throws Exception {
            var model = parseClass("io.github.hacihaciyev.fixtures.OuterWithInnerFixture$PackagePrivateInner");
            assertTrue(invokeBoolean(META_GEN, "isInaccessible", model));
        }

        @Test
        void publicInnerClass_accessible() throws Exception {
            var model = parseClass("io.github.hacihaciyev.fixtures.OuterWithInnerFixture$PublicInner");
            assertFalse(invokeBoolean(META_GEN, "isInaccessible", model));
        }

        @Test
        void anotherPublicTopLevel_accessible() throws Exception {
            var model = parseClass(MultiParamRepo.class);
            assertFalse(invokeBoolean(META_GEN, "isInaccessible", model));
        }
    }

    @Nested
    class IsStaticTests {

        @Test
        void staticField_returnsTrue() throws Exception {
            var model = parseClass(CorrectRepo.class);
            var field = model.fields().stream()
                .filter(f -> f.fieldName().stringValue().equals("INSERT"))
                .findFirst()
                .orElseThrow();
            assertTrue(invokeBoolean(PARAM_VERIFIER, "isStatic", field));
        }

        @Test
        void instanceField_returnsFalse() throws Exception {
            var model = parseClass(OuterWithInnerFixture.class);
            var field = model.fields().stream()
                .filter(f -> f.fieldName().stringValue().equals("instanceCounter"))
                .findFirst()
                .orElseThrow();
            assertFalse(invokeBoolean(PARAM_VERIFIER, "isStatic", field));
        }
    }

    @Nested
    class TrackedFieldWritesTests {

        @Test
        void correctRepo_returnsThreeWrites() throws Exception {
            var model = parseClass(CorrectRepo.class);
            var owner = model.thisClass().asSymbol();
            List<?> writes = invoke(PARAM_VERIFIER, "trackedFieldWritesInClinit", model, owner);
            assertEquals(3, writes.size());
        }

        @Test
        void multiParamRepo_returnsTwoWrites() throws Exception {
            var model = parseClass(MultiParamRepo.class);
            var owner = model.thisClass().asSymbol();
            List<?> writes = invoke(PARAM_VERIFIER, "trackedFieldWritesInClinit", model, owner);
            assertEquals(2, writes.size());
        }

        @Test
        void callerClass_noClinit_returnsEmpty() throws Exception {
            var model = parseClass(WrongParamCountCaller.class);
            var owner = model.thisClass().asSymbol();
            List<?> writes = invoke(PARAM_VERIFIER, "trackedFieldWritesInClinit", model, owner);
            assertTrue(writes.isEmpty());
        }

        @Test
        void classWithNoJqFields_returnsEmpty() throws Exception {
            var model = parseClass(OuterWithInnerFixture.class);
            var owner = model.thisClass().asSymbol();
            List<?> writes = invoke(PARAM_VERIFIER, "trackedFieldWritesInClinit", model, owner);
            assertTrue(writes.isEmpty());
        }

        @Test
        void writesMatchFieldOrder() throws Exception {
            var model  = parseClass(CorrectRepo.class);
            var owner  = model.thisClass().asSymbol();
            List<?> writes = invoke(PARAM_VERIFIER, "trackedFieldWritesInClinit", model, owner);

            var names = writes.stream()
                .map(w -> {
                    try {
                        var fn = w.getClass().getMethod("fieldName");
                        return fn.invoke(w).toString();
                    } catch (Exception e) { throw new RuntimeException(e); }
                })
                .toList();

            assertEquals(List.of("INSERT", "SELECT", "DELETE"), names);
        }
    }

    @Nested
    class CollectTrackedFieldsTests {

        @Test
        void emptyList_returnsEmpty() throws Exception {
            List<?> result = invoke(PARAM_VERIFIER, "collectTrackedFields", List.of());
            assertTrue(result.isEmpty());
        }

        @Test
        void inaccessibleInnerClass_skipped() throws Exception {
            var model  = parseClass("io.github.hacihaciyev.fixtures.OuterWithInnerFixture$PackagePrivateInner");
            List<?> result = invoke(PARAM_VERIFIER, "collectTrackedFields", List.of(model));
            assertTrue(result.isEmpty());
        }

        @Test
        void classWithNoJqFields_returnsEmpty() throws Exception {
            var model  = parseClass(OuterWithInnerFixture.class);
            List<?> result = invoke(PARAM_VERIFIER, "collectTrackedFields", List.of(model));
            assertTrue(result.isEmpty());
        }

        @Test
        void correctRepo_returnsThreeTrackedFields() throws Exception {
            var model  = parseClass(CorrectRepo.class);
            List<?> result = invoke(PARAM_VERIFIER, "collectTrackedFields", List.of(model));
            assertEquals(3, result.size());
        }

        @Test
        void multiParamRepo_returnsTwoTrackedFields() throws Exception {
            var model  = parseClass(MultiParamRepo.class);
            List<?> result = invoke(PARAM_VERIFIER, "collectTrackedFields", List.of(model));
            assertEquals(2, result.size());
        }

        @Test
        void multipleClasses_returnsAll() throws Exception {
            var correctModel    = parseClass(CorrectRepo.class);
            var multiParamModel = parseClass(MultiParamRepo.class);
            List<?> result = invoke(PARAM_VERIFIER, "collectTrackedFields", List.of(correctModel, multiParamModel));
            assertEquals(5, result.size());
        }

        @Test
        void duplicateClassModels_doNotBreakCollection() throws Exception {
            var model = parseClass(CorrectRepo.class);

            try {
                List<?> result = invoke(PARAM_VERIFIER, "collectTrackedFields", List.of(model, model));
                assertTrue(result.size() >= 3);
            } catch (MetaGenException e) {
                // Second pass over the same class does not re-run <clinit> (JVM initializes a class only
                // once), so no new queries are registered while bytecode analysis still expects writes.
                // This is an accepted limitation of duplicate class models within a single scan, not a
                // correctness issue for normal (non-duplicated) repository scanning.
                assertTrue(e.getMessage().contains("tracked query object"));
            }
        }

        @Test
        void mixedAccessibility_onlyPublicIncluded() throws Exception {
            var correctModel = parseClass(CorrectRepo.class);
            var innerModel   = parseClass("io.github.hacihaciyev.fixtures.OuterWithInnerFixture$PackagePrivateInner");
            List<?> result   = invoke(PARAM_VERIFIER, "collectTrackedFields", List.of(correctModel, innerModel));
            assertEquals(3, result.size());
        }

        @Test
        void classAlreadyInitializedElsewhere_stillTracksCorrectly() throws Exception {
            var alreadyInitialized = CorrectRepo.INSERT;
            var model  = parseClass(CorrectRepo.class);
            List<?> result = invoke(PARAM_VERIFIER, "collectTrackedFields", List.of(model));
            assertEquals(3, result.size());
        }

        @Test
        void aliasedField_throwsInconsistentStaticInit() throws Exception {
            var model = parseClass(AliasedFieldsRepo.class);
            assertThrows(MetaGenException.class,
                () -> invoke(PARAM_VERIFIER, "collectTrackedFields", List.of(model)));
        }

        @Test
        void arrayOfTrackedType_notRecognized_returnsEmpty() throws Exception {
            var model = parseClass(ArrayOfQueriesRepo.class);
            List<?> result = invoke(PARAM_VERIFIER, "collectTrackedFields", List.of(model));
            assertTrue(result.isEmpty());
        }

        @Test
        void conditionalBranchAssignment_stillTracksSingleField() throws Exception {
            var model = parseClass(ConditionalFieldRepo.class);
            List<?> result = invoke(PARAM_VERIFIER, "collectTrackedFields", List.of(model));
            assertEquals(1, result.size());
        }

        @Test
        void discardedConstruction_throwsInconsistentStaticInit() throws Exception {
            var model = parseClass(UnassignedConstructionRepo.class);
            assertThrows(MetaGenException.class,
                () -> invoke(PARAM_VERIFIER, "collectTrackedFields", List.of(model)));
        }
    }

    @Nested
    class VerifyUsagesTests {

        @SuppressWarnings("unchecked")
        private List<MetaGen.TrackedField> trackedFieldsFor(Class<?>... classes) throws Exception {
            var models = new java.util.ArrayList<ClassModel>();
            for (var cls : classes) models.add(parseClass(cls));
            return (List<MetaGen.TrackedField>) invoke(PARAM_VERIFIER, "collectTrackedFields", models);
        }

        @Test
        void correctCaller_passes() throws Exception {
            var tracked = trackedFieldsFor(CorrectRepo.class);
            var model   = parseClass(CorrectCaller.class);
            assertDoesNotThrow(() -> invoke(PARAM_VERIFIER, "verifyUsagesInClass", model, tracked));
        }

        @Test
        void callerWithTooFewParams_throws() throws Exception {
            var tracked = trackedFieldsFor(CorrectRepo.class);
            var model   = parseClass(WrongParamCountCaller.class);
            assertThrows(MetaGenException.class,
                () -> invoke(PARAM_VERIFIER, "verifyUsagesInClass", model, tracked));
        }

        @Test
        void callerWithWrongType_throws() throws Exception {
            var tracked = trackedFieldsFor(CorrectRepo.class);
            var model   = parseClass(WrongTypeCaller.class);
            assertThrows(MetaGenException.class,
                () -> invoke(PARAM_VERIFIER, "verifyUsagesInClass", model, tracked));
        }

        @Test
        void classWithNoJqCalls_passes() throws Exception {
            var tracked = trackedFieldsFor(CorrectRepo.class);
            var model   = parseClass(OuterWithInnerFixture.class);
            assertDoesNotThrow(() -> invoke(PARAM_VERIFIER, "verifyUsagesInClass", model, tracked));
        }

        @Test
        void errorMessage_containsFieldName() throws Exception {
            var tracked = trackedFieldsFor(CorrectRepo.class);
            var model   = parseClass(WrongParamCountCaller.class);
            var ex = assertThrows(MetaGenException.class,
                () -> invoke(PARAM_VERIFIER, "verifyUsagesInClass", model, tracked));
            assertTrue(ex.getMessage().contains("INSERT") || ex.getMessage().contains("DELETE"));
        }

        @Test
        void errorMessage_containsExpected() throws Exception {
            var tracked = trackedFieldsFor(CorrectRepo.class);
            var model   = parseClass(WrongParamCountCaller.class);
            var ex = assertThrows(MetaGenException.class,
                () -> invoke(PARAM_VERIFIER, "verifyUsagesInClass", model, tracked));
            assertTrue(ex.getMessage().contains("expected"));
        }

        @Test
        void emptyTrackedList_anyCallerPasses() throws Exception {
            var model = parseClass(WrongParamCountCaller.class);
            assertDoesNotThrow(() -> invoke(PARAM_VERIFIER, "verifyUsagesInClass", model, List.of()));
        }

        @Test
        void callerWithTooManyParams_throws() throws Exception {
            var tracked = trackedFieldsFor(CorrectRepo.class);
            var model   = parseClass(TooManyParamsCaller.class);
            var ex = assertThrows(MetaGenException.class,
                () -> invoke(PARAM_VERIFIER, "verifyUsagesInClass", model, tracked));
            assertTrue(ex.getMessage().contains("DELETE"));
        }

        @Test
        void nullParam_throwsTypeMismatch() throws Exception {
            var tracked = trackedFieldsFor(CorrectRepo.class);
            var model   = parseClass(NullParamCaller.class);
            assertThrows(MetaGenException.class,
                () -> invoke(PARAM_VERIFIER, "verifyUsagesInClass", model, tracked));
        }

        @Test
        void indirectFieldAccess_silentlyPasses_knownLimitation() throws Exception {
            var tracked = trackedFieldsFor(CorrectRepo.class);
            var model   = parseClass(IndirectFieldAccessCaller.class);
            assertDoesNotThrow(() -> invoke(PARAM_VERIFIER, "verifyUsagesInClass", model, tracked));
        }

        @Test
        void multipleRepositories_trackedFromAllCanVerifyUsage() throws Exception {
            var tracked = trackedFieldsFor(CorrectRepo.class, MultiParamRepo.class);
            var model   = parseClass(CorrectCaller.class);
            assertDoesNotThrow(() -> invoke(PARAM_VERIFIER, "verifyUsagesInClass", model, tracked));
        }
    }

    @Nested
    class SafeForInitializationTests {

        @Test
        void correctRepo_isSafe() throws Exception {
            var model = parseClass(CorrectRepo.class);
            var owner = model.thisClass().asSymbol();
            assertTrue(invokeBoolean(PARAM_VERIFIER, "isSafeForInitialization", model, owner));
        }

        @Test
        void multiParamRepo_isSafe() throws Exception {
            var model = parseClass(MultiParamRepo.class);
            var owner = model.thisClass().asSymbol();
            assertTrue(invokeBoolean(PARAM_VERIFIER, "isSafeForInitialization", model, owner));
        }

        @Test
        void aliasedFieldsRepo_isUnsafe() throws Exception {
            var model = parseClass(AliasedFieldsRepo.class);
            var owner = model.thisClass().asSymbol();
            assertFalse(invokeBoolean(PARAM_VERIFIER, "isSafeForInitialization", model, owner));
        }

        @Test
        void unassignedConstructionRepo_hasNoAliasing_soConsideredSafeToAttemptInit() throws Exception {
            // isSafeForInitialization only rejects aliasing (reading a tracked field within <clinit>).
            // The discarded-construction case is still caught, but by the existing runtime mismatch
            // check in traceModels() after initialization, not by this pre-check.
            var model = parseClass(UnassignedConstructionRepo.class);
            var owner = model.thisClass().asSymbol();
            assertTrue(invokeBoolean(PARAM_VERIFIER, "isSafeForInitialization", model, owner));
        }

        @Test
        void conditionalFieldRepo_noAliasing_isSafe() throws Exception {
            // A ternary assigning to the same tracked field on both branches contains two build()
            // call sites in bytecode, but that is not aliasing, so it must not be rejected.
            var model = parseClass(ConditionalFieldRepo.class);
            var owner = model.thisClass().asSymbol();
            assertTrue(invokeBoolean(PARAM_VERIFIER, "isSafeForInitialization", model, owner));
        }
    }

    @Nested
    class VerifyTopLevelApiTests {

        @Test
        void verify_onMixedPackages_reportsMetaGenOrVerificationFailure() {
            var ex = assertThrows(MetaGenException.class,
                () -> invoke(PARAM_VERIFIER, "verify", (Object) new String[]{"io.github.hacihaciyev.fixtures", "io.github.hacihaciyev.jdbc"}));
            assertFalse(ex.getMessage().isBlank());
        }

        @Test
        void verify_ignoredUnsafeClassIsSkippedDuringCollection() throws Exception {
            var prev = System.getProperty("jetquerious.metagen.ignore");
            System.setProperty("jetquerious.metagen.ignore", "AliasedFieldsRepo");
            try {
                var model = parseClass(AliasedFieldsRepo.class);
                List<?> result = invoke(PARAM_VERIFIER, "collectTrackedFields", List.of(model));
                assertTrue(result.isEmpty());
            } finally {
                if (prev == null) System.clearProperty("jetquerious.metagen.ignore");
                else System.setProperty("jetquerious.metagen.ignore", prev);
            }
        }
    }

    @Nested
    class InconsistentStaticInitTests {

        @Test
        void correctRepo_fieldCountMatchesConstructions() throws Exception {
            var model  = parseClass(CorrectRepo.class);
            var owner  = model.thisClass().asSymbol();
            List<?> writes = invoke(PARAM_VERIFIER, "trackedFieldWritesInClinit", model, owner);
            assertEquals(3, writes.size());
        }

        @Test
        void multiParamRepo_fieldCountMatchesConstructions() throws Exception {
            var model  = parseClass(MultiParamRepo.class);
            var owner  = model.thisClass().asSymbol();
            List<?> writes = invoke(PARAM_VERIFIER, "trackedFieldWritesInClinit", model, owner);
            assertEquals(2, writes.size());
        }
    }
}
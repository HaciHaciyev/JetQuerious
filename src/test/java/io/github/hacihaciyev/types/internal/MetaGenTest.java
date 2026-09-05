package io.github.hacihaciyev.types.internal;

import io.github.hacihaciyev.build_errors.MetaGenException;
import io.github.hacihaciyev.util.DBTestContainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DBTestContainer.class)
class MetaGenTest {

    @SuppressWarnings("unchecked")
    private static <T> T invoke(Class<?> cls, String name, Class<?>[] paramTypes, Object... args) throws Exception {
        Method m = cls.getDeclaredMethod(name, paramTypes);
        m.setAccessible(true);
        try {
            return (T) m.invoke(null, args);
        } catch (InvocationTargetException e) {
            var cause = e.getCause();
            if (cause instanceof Exception ex) throw ex;
            throw e;
        }
    }

    @Test
    void pkgScan_asResPath_convertsDotsToSlashes() throws Exception {
        var pkgScan = Class.forName("io.github.hacihaciyev.types.internal.MetaGen$PkgScan");
        var result = invoke(pkgScan, "asResPath", new Class[]{String.class}, "io.github.hacihaciyev.fixtures");
        assertEquals("io/github/hacihaciyev/fixtures", result);
    }

    @Test
    void pkgScan_read_findsFixtureClasses() throws Exception {
        var pkgScan = Class.forName("io.github.hacihaciyev.types.internal.MetaGen$PkgScan");
        List<byte[]> result = invoke(pkgScan, "read", new Class[]{String.class}, "io.github.hacihaciyev.fixtures");
        assertFalse(result.isEmpty());
    }

    @Test
    void pkgScan_read_onUnknownPackage_returnsEmpty() throws Exception {
        var pkgScan = Class.forName("io.github.hacihaciyev.types.internal.MetaGen$PkgScan");
        List<byte[]> result = invoke(pkgScan, "read", new Class[]{String.class}, "io.github.hacihaciyev.no.such.pkg");
        assertTrue(result.isEmpty());
    }

    @Test
    void metaGen_unboxMethodName_forPrimitiveWorks() throws Exception {
        var result = invoke(MetaGen.class, "unboxMethodName", new Class[]{java.lang.constant.ClassDesc.class}, java.lang.constant.ClassDesc.ofDescriptor("I"));
        assertEquals("intValue", result);
    }

    @Test
    void metaGen_unboxMethodName_forNonPrimitiveThrows() {
        var ex = assertThrows(MetaGenException.class, () ->
            invoke(MetaGen.class, "unboxMethodName", new Class[]{java.lang.constant.ClassDesc.class}, java.lang.constant.ClassDesc.of("java.lang.String"))
        );
        assertTrue(ex.getMessage().contains("Not a primitive"));
    }
}

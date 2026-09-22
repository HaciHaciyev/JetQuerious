package io.github.hacihaciyev.sql.internal;

import io.github.hacihaciyev.sql.QueryForge;
import io.github.hacihaciyev.sql.Criteria;

import io.github.hacihaciyev.build_errors.SchemaVerificationException;
import io.github.hacihaciyev.util.DBTestContainer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.github.hacihaciyev.sql.SQL.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DBTestContainer.class)
class CriteriaContextTest {

    private static Context.Criteria ctx(Criteria criteria) {
        return criteria.context();
    }

    @Nested
    class UnitDecomposition {

        @Test
        void whereIsFlattenedIntoOneUnitPerConjunct() {
            var c = ctx(QueryForge.criteria(col("id"))
                .from("users")
                .where(and(
                    eq(col("email"), opt(String.class)),
                    eq(col("age"), param(Integer.class)),
                    eq(col("active"), opt(Boolean.class))
                ))
                .build());

            assertEquals(3, c.unitsJava().size());
            assertTrue(c.unitsJava().get(0).optional());
            assertFalse(c.unitsJava().get(1).optional());
            assertTrue(c.unitsJava().get(2).optional());
        }

        @Test
        void groupByAndOrderByYieldOneUnitPerItem() {
            var c = ctx(QueryForge.criteria(col("department"), countAll())
                .from("employees")
                .groupBy(col("department"), col("active"))
                .orderBy(col("department"))
                .build());

            assertEquals(3, c.unitsJava().size());
            assertTrue(c.unitsJava().stream().noneMatch(CriteriaUnit::optional));
        }

        @Test
        void havingIsFlattenedLikeWhere() {
            var c = ctx(QueryForge.criteria(col("active"), countAll())
                .from("users")
                .groupBy(col("active"))
                .having(and(
                    gt(countAll(), param(Long.class)),
                    gt(countAll(), opt(Long.class))
                ))
                .build());

            var havingUnits = c.unitsOf(ClauseKind.HAVING);
            assertEquals(2, havingUnits.size());
            assertFalse(havingUnits.get(0).optional());
            assertTrue(havingUnits.get(1).optional());
        }

        @Test
        void noClauses_noUnits() {
            var c = ctx(QueryForge.criteria(col("id")).from("users").build());

            assertTrue(c.unitsJava().isEmpty());
            assertTrue(c.paramTypesJava().isEmpty());
        }
    }

    @Nested
    class DeclaredParamShape {

        @Test
        void paramTypesAreDeclarationOrderedAndPositioned() {
            var c = ctx(QueryForge.criteria(col("id"))
                .from("users")
                .where(and(
                    eq(col("active"), param(Boolean.class)),
                    eq(col("email"), opt(String.class))
                ))
                .build());

            assertEquals(2, c.paramTypesJava().size());
            assertEquals(Boolean.class, c.paramTypesJava().get(0)._type());
            assertEquals(1, c.paramTypesJava().get(0).position());
            assertEquals(String.class, c.paramTypesJava().get(1)._type());
            assertEquals(2, c.paramTypesJava().get(1).position());
        }

        @Test
        void clauseOrderIsWhereThenGroupByThenHavingThenOrderBy() {
            var c = ctx(QueryForge.criteria(col("active"), countAll())
                .from("users")
                .where(eq(col("name"), opt(String.class)))
                .groupBy(coalesce(col("active"), opt(Boolean.class)))
                .having(gt(countAll(), opt(Long.class)))
                .orderBy(coalesce(col("active"), opt(Boolean.class)))
                .build());

            assertEquals(4, c.paramTypesJava().size());
            assertEquals(String.class, c.paramTypesJava().get(0)._type());
            assertEquals(Boolean.class, c.paramTypesJava().get(1)._type());
            assertEquals(Long.class, c.paramTypesJava().get(2)._type());
            assertEquals(Boolean.class, c.paramTypesJava().get(3)._type());
        }

        @Test
        void fixedTypesCoverProjectionAndJoinPlaceholders() {
            var c = ctx(QueryForge.criteria(col("id"))
                .from("users")
                .where(eq(col("email"), opt(String.class)))
                .build());

            assertTrue(c.fixedTypes().isEmpty());
            assertEquals(1, c.paramTypesJava().size());
        }
    }

    @Nested
    class Pruning {

        @Test
        void nullOptionalArg_dropsUnitAndItsParameter() {
            var c = ctx(QueryForge.criteria(col("id"))
                .from("users")
                .where(eq(col("email"), opt(String.class)))
                .build());

            var prepared = c.prepare(new Object[]{null});

            assertFalse(prepared.sql().contains("WHERE"));
            assertEquals(0, prepared.paramTypes().size());
            assertEquals(0, prepared.args().length);
        }

        @Test
        void nonNullOptionalArg_keepsUnitAndBindsValue() {
            var c = ctx(QueryForge.criteria(col("id"))
                .from("users")
                .where(eq(col("email"), opt(String.class)))
                .build());

            var prepared = c.prepare(new Object[]{"a@example.com"});

            assertTrue(prepared.sql().contains("WHERE"));
            assertEquals(1, prepared.paramTypes().size());
            assertArrayEquals(new Object[]{"a@example.com"}, prepared.args());
        }

        @Test
        void survivingParametersAreRenumberedFromOne() {
            var c = ctx(QueryForge.criteria(col("id"))
                .from("users")
                .where(and(
                    eq(col("email"), opt(String.class)),
                    eq(col("age"), opt(Integer.class))
                ))
                .build());

            var prepared = c.prepare(new Object[]{null, 30});

            assertEquals(1, prepared.paramTypes().size());
            assertEquals(1, prepared.paramTypes().get(0).position());
            assertEquals(Integer.class, prepared.paramTypes().get(0)._type());
            assertArrayEquals(new Object[]{30}, prepared.args());
        }

        @Test
        void requiredUnitIsNeverDropped_evenWhenItsArgIsNull() {
            var c = ctx(QueryForge.criteria(col("id"))
                .from("users")
                .where(eq(col("email"), param(String.class)))
                .build());

            var prepared = c.prepare(new Object[]{null});

            // a required parameter stays; binding null is a legitimate runtime value
            assertTrue(prepared.sql().contains("WHERE"));
            assertEquals(1, prepared.paramTypes().size());
            assertArrayEquals(new Object[]{null}, prepared.args());
        }

        @Test
        void allOptionalUnitsNull_dropsEveryClause() {
            var c = ctx(QueryForge.criteria(col("active"), countAll())
                .from("users")
                .where(eq(col("name"), opt(String.class)))
                .groupBy(coalesce(col("active"), opt(Boolean.class)))
                .having(gt(countAll(), opt(Long.class)))
                .orderBy(coalesce(col("active"), opt(Boolean.class)))
                .build());

            var prepared = c.prepare(new Object[]{null, null, null, null});

            assertFalse(prepared.sql().contains("WHERE"));
            assertFalse(prepared.sql().contains("GROUP BY"));
            assertFalse(prepared.sql().contains("HAVING"));
            assertFalse(prepared.sql().contains("ORDER BY"));
            assertEquals(0, prepared.paramTypes().size());
        }

        @Test
        void clausesArePrunedIndependently() {
            var c = ctx(QueryForge.criteria(col("active"), countAll())
                .from("users")
                .where(eq(col("name"), opt(String.class)))
                .groupBy(coalesce(col("active"), opt(Boolean.class)))
                .orderBy(coalesce(col("active"), opt(Boolean.class)))
                .build());

            // where kept, groupBy dropped, orderBy kept
            var prepared = c.prepare(new Object[]{"x", null, true});

            assertTrue(prepared.sql().contains("WHERE"));
            assertFalse(prepared.sql().contains("GROUP BY"));
            assertTrue(prepared.sql().contains("ORDER BY"));
            assertEquals(2, prepared.paramTypes().size());
            assertArrayEquals(new Object[]{"x", true}, prepared.args());
        }

        @Test
        void plainItemWithNoPlaceholder_isNeverDropped() {
            var c = ctx(QueryForge.criteria(col("department"), countAll())
                .from("employees")
                .groupBy(col("department"))
                .build());

            var prepared = c.prepare(new Object[0]);

            assertTrue(prepared.sql().contains("GROUP BY department"));
        }

        @Test
        void mixedRequiredAndOptionalInSameClause_onlyOptionalDropped() {
            var c = ctx(QueryForge.criteria(col("id"))
                .from("users")
                .where(and(
                    eq(col("active"), param(Boolean.class)),
                    eq(col("email"), opt(String.class))
                ))
                .build());

            var prepared = c.prepare(new Object[]{true, null});

            assertTrue(prepared.sql().contains("WHERE"));
            assertEquals(1, prepared.paramTypes().size());
            assertArrayEquals(new Object[]{true}, prepared.args());
        }

        @Test
        void preparingTwiceWithDifferentArgs_isIndependent() {
            var c = ctx(QueryForge.criteria(col("id"))
                .from("users")
                .where(eq(col("email"), opt(String.class)))
                .build());

            var dropped = c.prepare(new Object[]{null});
            var kept    = c.prepare(new Object[]{"a@example.com"});

            assertFalse(dropped.sql().contains("WHERE"));
            assertTrue(kept.sql().contains("WHERE"));
            // the declared shape is unaffected by either preparation
            assertEquals(1, c.paramTypesJava().size());
        }
    }

    @Nested
    class ArgumentCountValidation {

        @Test
        void tooFewArgs_throws() {
            var c = ctx(QueryForge.criteria(col("id"))
                .from("users")
                .where(and(
                    eq(col("email"), opt(String.class)),
                    eq(col("age"), opt(Integer.class))
                ))
                .build());

            var ex = assertThrows(IllegalArgumentException.class, () -> c.prepare(new Object[]{null}));
            assertTrue(ex.getMessage().contains("expects 2 argument"));
        }

        @Test
        void tooManyArgs_throws() {
            var c = ctx(QueryForge.criteria(col("id"))
                .from("users")
                .where(eq(col("email"), opt(String.class)))
                .build());

            assertThrows(IllegalArgumentException.class, () -> c.prepare(new Object[]{"a", "b"}));
        }

        @Test
        void nullArgsArray_throws() {
            var c = ctx(QueryForge.criteria(col("id"))
                .from("users")
                .where(eq(col("email"), opt(String.class)))
                .build());

            assertThrows(IllegalArgumentException.class, () -> c.prepare(null));
        }

        @Test
        void noParams_emptyArgs_ok() {
            var c = ctx(QueryForge.criteria(col("id")).from("users").build());

            assertDoesNotThrow(() -> c.prepare(new Object[0]));
        }
    }

    @Nested
    class OptionalPlacement {

        @Test
        void optInProjection_throws() {
            var ex = assertThrows(SchemaVerificationException.class, () ->
                QueryForge.criteria(col("id"), opt(String.class)).from("users").build()
            );
            assertTrue(ex.getMessage().contains("opt"));
        }

        @Test
        void twoOptsInSameWhereConjunct_throws() {
            assertThrows(IllegalArgumentException.class, () ->
                QueryForge.criteria(col("id"))
                    .from("users")
                    .where(between(col("age"), opt(Integer.class), opt(Integer.class)))
                    .build()
            );
        }

        @Test
        void optMixedWithParamInSameConjunct_throws() {
            assertThrows(IllegalArgumentException.class, () ->
                QueryForge.criteria(col("id"))
                    .from("users")
                    .where(or(
                        eq(col("email"), opt(String.class)),
                        eq(col("name"), param(String.class))
                    ))
                    .build()
            );
        }

        @Test
        void twoOptsInSameGroupByItem_throws() {
            assertThrows(IllegalArgumentException.class, () ->
                QueryForge.criteria(countAll())
                    .from("employees")
                    .groupBy(coalesce(opt(String.class), opt(String.class)))
                    .build()
            );
        }

        @Test
        void optsInSeparateConjuncts_areFine() {
            assertDoesNotThrow(() ->
                QueryForge.criteria(col("id"))
                    .from("users")
                    .where(and(
                        eq(col("email"), opt(String.class)),
                        eq(col("age"), opt(Integer.class))
                    ))
                    .build()
            );
        }
    }

    @Nested
    class SchemaValidationStillApplies {

        @Test
        void nonExistentTable_throws() {
            assertThrows(SchemaVerificationException.class, () ->
                QueryForge.criteria(col("id")).from("ghost_table").build()
            );
        }

        @Test
        void nonExistentProjectionColumn_throws() {
            assertThrows(SchemaVerificationException.class, () ->
                QueryForge.criteria(col("ghost_column")).from("users").build()
            );
        }

        @Test
        void nonExistentColumnInOptionalWhereUnit_throwsAtBuildTime() {
            // validation sees the full unpruned shape, so an unreachable-at-runtime
            // unit is still schema-checked
            assertThrows(SchemaVerificationException.class, () ->
                QueryForge.criteria(col("id"))
                    .from("users")
                    .where(eq(col("ghost_column"), opt(String.class)))
                    .build()
            );
        }

        @Test
        void nonExistentColumnInOptionalOrderByUnit_throwsAtBuildTime() {
            assertThrows(SchemaVerificationException.class, () ->
                QueryForge.criteria(col("id"))
                    .from("users")
                    .orderBy(coalesce(col("ghost_column"), opt(String.class)))
                    .build()
            );
        }
    }

    @Nested
    class ContextIdentity {

        @Test
        void criteriaContextIsAContext() {
            var c = ctx(QueryForge.criteria(col("id")).from("users").build());
            assertInstanceOf(Context.class, c);
        }

        @Test
        void criteriaContextIsDQL() {
            var c = ctx(QueryForge.criteria(col("id")).from("users").build());
            assertInstanceOf(DQL.class, c);
        }

        @Test
        void criteriaIsNotAJQ() {
            var criteria = QueryForge.criteria(col("id")).from("users").build();
            assertFalse(io.github.hacihaciyev.sql.JQ.class.isAssignableFrom(criteria.getClass()));
        }

        @Test
        void projectionColumnsAreExposedLikeAnyContext() {
            var c = ctx(QueryForge.criteria(col("id"), col("name")).from("users").build());
            assertEquals(2, c.projectionColumns().size());
        }
    }
}
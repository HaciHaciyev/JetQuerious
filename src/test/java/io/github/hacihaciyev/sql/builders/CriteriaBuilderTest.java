package io.github.hacihaciyev.sql.builders;

import io.github.hacihaciyev.sql.QueryForge;
import io.github.hacihaciyev.sql.Criteria;

import io.github.hacihaciyev.build_errors.SchemaVerificationException;
import io.github.hacihaciyev.sql.expressions.Expr;
import io.github.hacihaciyev.sql.value_objects.TableRef;
import io.github.hacihaciyev.util.DBTestContainer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.github.hacihaciyev.sql.SQL.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DBTestContainer.class)
class CriteriaBuilderTest {

    @Nested
    class InputValidation {

        @Test
        void noProjections_throws() {
            assertThrows(IllegalArgumentException.class, () -> QueryForge.criteria(new Expr[0]));
        }

        @Test
        void nullProjection_throws() {
            assertThrows(NullPointerException.class, () -> QueryForge.criteria((Expr) null));
        }

        @Test
        void nullExprArray_throws() {
            assertThrows(NullPointerException.class, () -> QueryForge.criteria((Expr[]) null));
        }

        @Test
        void nullFromTableRef_throws() {
            assertThrows(NullPointerException.class, () ->
                QueryForge.criteria(col("id")).from((TableRef) null)
            );
        }

        @Test
        void nullWhere_throws() {
            assertThrows(NullPointerException.class, () ->
                QueryForge.criteria(col("id")).from("users").where(null)
            );
        }
    }

    @Nested
    class ProducesContextBackedCriteria {

        @Test
        void build_returnsCriteriaWithNonNullContext() {
            var criteria = QueryForge.criteria(col("id"), col("name")).from("users").build();
            assertNotNull(criteria.context());
        }

        @Test
        void selectAll_wildcardProjectionResolves() {
            var criteria = QueryForge.criteriaAll().from("users").build();
            assertFalse(criteria.context().projectionColumns().isEmpty());
        }

        @Test
        void selectDistinct_setsDistinctOnContext() {
            var criteria = QueryForge.criteriaDistinct(col("name")).from("users").build();
            assertTrue(criteria.context().distinct());
        }

        @Test
        void select_defaultsToNonDistinct() {
            var criteria = QueryForge.criteria(col("name")).from("users").build();
            assertFalse(criteria.context().distinct());
        }

        @Test
        void limitAndOffset_reachTheRenderedSql() {
            var criteria = QueryForge.criteria(col("id")).from("users").limit(10).offset(5).build();
            var sql = criteria.context().prepare(new Object[0]).sql();

            assertTrue(sql.contains("LIMIT 10"));
            assertTrue(sql.contains("OFFSET 5"));
        }

        @Test
        void joins_reachTheRenderedSql() {
            var criteria = QueryForge.criteria(col("u", "id"))
                .from(tAs("users", "u"))
                .join(tAs("orders", "o"), eq(col("u", "id"), col("o", "user_id")))
                .build();

            var sql = criteria.context().prepare(new Object[0]).sql();
            assertTrue(sql.contains("JOIN orders AS o ON"));
        }

        @Test
        void aliasedProjection_rendersAlias() {
            var criteria = QueryForge.criteria(colAs("id", "user_id")).from("users").build();
            var sql = criteria.context().prepare(new Object[0]).sql();

            assertTrue(sql.contains("id AS user_id"));
        }
    }

    @Nested
    class SchemaValidation {

        @Test
        void nonExistentTable_throws() {
            assertThrows(SchemaVerificationException.class, () ->
                QueryForge.criteria(col("id")).from("ghost_table").build()
            );
        }

        @Test
        void nonExistentColumn_throws() {
            assertThrows(SchemaVerificationException.class, () ->
                QueryForge.criteria(col("ghost_column")).from("users").build()
            );
        }

        @Test
        void validColumns_pass() {
            assertDoesNotThrow(() ->
                QueryForge.criteria(col("id"), col("name")).from("users").build()
            );
        }

        @Test
        void nonExistentColumnInJoinOn_throws() {
            assertThrows(SchemaVerificationException.class, () ->
                QueryForge.criteria(col("u", "id"))
                    .from(tAs("users", "u"))
                    .join(tAs("orders", "o"), eq(col("u", "ghost_column"), col("o", "user_id")))
                    .build()
            );
        }

        @Test
        void nonExistentColumnInGroupBy_throws() {
            assertThrows(SchemaVerificationException.class, () ->
                QueryForge.criteria(countAll()).from("users").groupBy(col("ghost_column")).build()
            );
        }

        @Test
        void nonExistentColumnInHaving_throws() {
            assertThrows(SchemaVerificationException.class, () ->
                QueryForge.criteria(col("active"), countAll())
                    .from("users")
                    .groupBy(col("active"))
                    .having(gt(col("ghost_column"), param(Integer.class)))
                    .build()
            );
        }
    }

    @Nested
    class OptionalityAcrossAllClauses {

        @Test
        void optInWhere_passes() {
            assertDoesNotThrow(() ->
                QueryForge.criteria(col("id"))
                    .from("users")
                    .where(eq(col("email"), opt(String.class)))
                    .build()
            );
        }

        @Test
        void optInHaving_passes() {
            assertDoesNotThrow(() ->
                QueryForge.criteria(col("active"), countAll())
                    .from("users")
                    .groupBy(col("active"))
                    .having(gt(countAll(), opt(Long.class)))
                    .build()
            );
        }

        @Test
        void optInGroupBy_passes() {
            assertDoesNotThrow(() ->
                QueryForge.criteria(countAll())
                    .from("employees")
                    .groupBy(coalesce(col("department"), opt(String.class)))
                    .build()
            );
        }

        @Test
        void optInOrderBy_passes() {
            assertDoesNotThrow(() ->
                QueryForge.criteria(col("id"))
                    .from("employees")
                    .orderBy(coalesce(col("department"), opt(String.class)))
                    .build()
            );
        }

        @Test
        void optInProjection_throws() {
            assertThrows(SchemaVerificationException.class, () ->
                QueryForge.criteria(col("id"), opt(String.class)).from("users").build()
            );
        }

        @Test
        void optInJoinOn_throws() {
            assertThrows(SchemaVerificationException.class, () ->
                QueryForge.criteria(col("u", "id"))
                    .from(tAs("users", "u"))
                    .join(tAs("orders", "o"), eq(col("o", "user_id"), opt(Long.class)))
                    .build()
            );
        }

        @Test
        void optInEveryPrunableClauseAtOnce_declaresFourParams() {
            var criteria = QueryForge.criteria(col("active"), countAll())
                .from("users")
                .where(eq(col("name"), opt(String.class)))
                .groupBy(coalesce(col("active"), opt(Boolean.class)))
                .having(gt(countAll(), opt(Long.class)))
                .orderBy(coalesce(col("active"), opt(Boolean.class)))
                .build();

            assertEquals(4, criteria.context().paramTypesJava().size());
        }
    }

    @Nested
    class Immutability {

        @Test
        void twoBuildsFromSameStage_produceIndependentCriteria() {
            var stage = QueryForge.criteria(col("id")).from("users");

            var a = stage.build();
            var b = stage.build();

            assertNotSame(a, b);
            assertEquals(a.context().paramTypesJava().size(), b.context().paramTypesJava().size());
        }
    }
}
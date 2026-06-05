package io.github.hacihaciyev.sql.builders;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

import io.github.hacihaciyev.sql.JQ;
import io.github.hacihaciyev.sql.Transaction;
import io.github.hacihaciyev.sql.Transaction.IsolationLevel;
import io.github.hacihaciyev.sql.Transaction.Savepoint;
import io.github.hacihaciyev.sql.expressions.*;
import io.github.hacihaciyev.util.DBTestContainer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.github.hacihaciyev.sql.SQL.*;
import static io.github.hacihaciyev.sql.QueryForge.*;

@ExtendWith(DBTestContainer.class)
class TransactionBuilderTest {
    
    private static JQ.Read selectBalance() {
        return select(col("total"))
            .from("orders")
            .where(eq(col("id"), param(Long.class)))
            .build();
    }
    
    private static JQ.Write debitBalance() {
        return update("orders")
            .set("total", subtract(col("total"), param(BigDecimal.class)))
            .where(eq(col("id"), param(Long.class)))
            .build();
    }
    
    private static JQ.Write creditBalance() {
        return update("orders")
            .set("total", add(col("total"), param(BigDecimal.class)))
            .where(eq(col("id"), param(Long.class)))
            .build();
    }
    
    private static JQ.Write insertUser() {
        return insertInto("users")
            .columns("name", String.class, "email", String.class)
            .build();
    }
    
    private static JQ.Write updateUserStatus() {
        return update("users")
            .set("active", Boolean.class)
            .where(eq(col("id"), param(Long.class)))
            .build();
    }
    
    private static JQ.Write deleteUser() {
        return deleteFrom("users")
            .where(eq(col("id"), param(Long.class)))
            .build();
    }
    
    private static JQ.Write insertBannedUser() {
        return insertInto("banned_users")
            .columns("user_id", Long.class)
            .build();
    }

    private static JQ.Write insertUserReturningId() {
        return insertInto("users")
            .columns("name", String.class, "email", String.class, "active", Boolean.class)
            .returning(col("id"))
            .build();
    }
    
    private static JQ.Write insertProfile() {
        return update("users")
            .set("age", Integer.class)
            .where(eq(col("id"), param(Long.class)))
            .build();
    }
    
    private static JQ.Write insertSettings() {
        return update("users")
            .set("active", Boolean.class)
            .where(eq(col("id"), param(Long.class)))
            .build();
    }
    
    private static JQ.Write insertWelcomeNotification() {
        return insertInto("orders")
            .columns("user_id", Long.class, "total", BigDecimal.class, "status", String.class)
            .build();
    }
    
    private static JQ.Write debitBalanceReturning() {
        return update("orders")
            .set("total", subtract(col("total"), param(BigDecimal.class)))
            .where(eq(col("id"), param(Long.class)))
            .returning(col("total"))
            .build();
    }
    
    private static JQ.Write insertAuditRecord() {
        return insertInto("order_items")
            .columns("order_id", Long.class, "product", String.class, "qty", Integer.class)
            .build();
    }
    
    private static JQ.Write insertOrderReturningId() {
        return insertInto("orders")
            .columns("user_id", Long.class, "total", BigDecimal.class, "amount", BigDecimal.class, "status", String.class)
            .returning(col("id"))
            .build();
    }
    
    private static JQ.Write insertShipment() {
        return insertInto("order_items")
            .columns("order_id", Long.class, "product", String.class, "qty", Integer.class)
            .build();
    }
    
    @Nested
    class Basic {

        @Test
        void singleWrite_builds() {
            var tx = new TransactionBuilder().add(insertUser()).build();

            assertNotNull(tx);
            assertEquals(1, tx.operations().length);
            assertEquals(0, tx.savepoints().length);
            assertEquals(IsolationLevel.DEFAULT, tx.isolationLevel());
        }

        @Test
        void singleRead_builds() {
            var tx = new TransactionBuilder().add(selectBalance()).build();

            assertNotNull(tx);
            assertEquals(1, tx.operations().length);
        }

        @Test
        void multipleOperations_builds() {
            var tx = new TransactionBuilder()
                .add(selectBalance())
                .add(debitBalance())
                .build();

            assertEquals(2, tx.operations().length);
        }

        @Test
        void operationsPreserveOrder() {
            var select = selectBalance();
            var update = debitBalance();

            var tx = new TransactionBuilder().add(select).add(update).build();

            assertSame(select, tx.operations()[0]);
            assertSame(update, tx.operations()[1]);
        }

        @Test
        void operationsArrayIsDefensive() {
            var tx = new TransactionBuilder().add(insertUser()).build();

            var ops = tx.operations();
            ops[0] = null;

            assertNotNull(tx.operations()[0]);
        }
    }

    @Nested
    class SelectThenUpdate {

        @Test
        void readThenDebit_builds() {
            var tx = new TransactionBuilder()
                .add(selectBalance())
                .add(debitBalance())
                .build();

            assertInstanceOf(JQ.Read.class, tx.operations()[0]);
            assertInstanceOf(JQ.Write.class, tx.operations()[1]);
            assertEquals(2, tx.operations().length);
        }

        @Test
        void transfer_debitThenCredit() {
            var tx = new TransactionBuilder()
                .add(debitBalance())
                .add(creditBalance())
                .build();

            assertEquals(2, tx.operations().length);
            assertInstanceOf(JQ.Write.class, tx.operations()[0]);
            assertInstanceOf(JQ.Write.class, tx.operations()[1]);
        }

        @Test
        void fullTransfer_selectDebitCredit() {
            var tx = new TransactionBuilder()
                .add(selectBalance())
                .add(selectBalance())
                .add(debitBalance())
                .add(creditBalance())
                .build();

            assertEquals(4, tx.operations().length);
        }
    }

    @Nested
    class IsolationLevels {

        @Test
        void default_isolationLevel() {
            var tx = new TransactionBuilder().add(insertUser()).build();

            assertEquals(IsolationLevel.DEFAULT, tx.isolationLevel());
        }

        @Test
        void readCommitted() {
            var tx = new TransactionBuilder()
                .add(selectBalance())
                .add(debitBalance())
                .isolationLevel(IsolationLevel.READ_COMMITTED)
                .build();

            assertEquals(IsolationLevel.READ_COMMITTED, tx.isolationLevel());
        }

        @Test
        void repeatableRead() {
            var tx = new TransactionBuilder()
                .add(selectBalance())
                .add(debitBalance())
                .isolationLevel(IsolationLevel.REPEATABLE_READ)
                .build();

            assertEquals(IsolationLevel.REPEATABLE_READ, tx.isolationLevel());
        }

        @Test
        void serializable() {
            var tx = new TransactionBuilder()
                .add(selectBalance())
                .add(debitBalance())
                .isolationLevel(IsolationLevel.SERIALIZABLE)
                .build();

            assertEquals(IsolationLevel.SERIALIZABLE, tx.isolationLevel());
        }

        @Test
        void readUncommitted() {
            var tx = new TransactionBuilder()
                .add(selectBalance())
                .isolationLevel(IsolationLevel.READ_UNCOMMITTED)
                .build();

            assertEquals(IsolationLevel.READ_UNCOMMITTED, tx.isolationLevel());
        }

        @Test
        void isolationLevel_canBeSetBeforeOrAfterOperations() {
            var tx1 = new TransactionBuilder()
                .isolationLevel(IsolationLevel.SERIALIZABLE)
                .add(insertUser())
                .build();

            var tx2 = new TransactionBuilder()
                .add(insertUser())
                .isolationLevel(IsolationLevel.SERIALIZABLE)
                .build();

            assertEquals(tx1.isolationLevel(), tx2.isolationLevel());
        }
    }

    @Nested
    class Savepoints {

        @Test
        void singleSavepoint_afterFirstOp() {
            var tx = new TransactionBuilder()
                .add(insertUser())
                .savepoint("after_insert")
                .add(updateUserStatus())
                .build();

            assertEquals(1, tx.savepoints().length);
            assertEquals("after_insert", tx.savepoints()[0].name());
            assertEquals(1, tx.savepoints()[0].position());
        }

        @Test
        void multipleSavepoints() {
            var tx = new TransactionBuilder()
                .add(insertUser())
                .savepoint("sp1")
                .add(updateUserStatus())
                .savepoint("sp2")
                .add(deleteUser())
                .build();

            assertEquals(2, tx.savepoints().length);
            assertEquals("sp1", tx.savepoints()[0].name());
            assertEquals(1, tx.savepoints()[0].position());
            assertEquals("sp2", tx.savepoints()[1].name());
            assertEquals(2, tx.savepoints()[1].position());
        }

        @Test
        void savepointAtStart_positionZero() {
            var tx = new TransactionBuilder()
                .savepoint("initial")
                .add(insertUser())
                .build();

            assertEquals(0, tx.savepoints()[0].position());
        }

        @Test
        void savepointAtEnd() {
            var tx = new TransactionBuilder()
                .add(insertUser())
                .add(updateUserStatus())
                .savepoint("end")
                .build();

            assertEquals(2, tx.savepoints()[0].position());
        }

        @Test
        void savepointsArrayIsDefensive() {
            var tx = new TransactionBuilder()
                .add(insertUser())
                .savepoint("sp1")
                .build();

            var sps = tx.savepoints();
            sps[0] = null;

            assertNotNull(tx.savepoints()[0]);
        }

        @Test
        void savepoint_nameIsTrimmed() {
            var sp = new Savepoint(0, "  my_savepoint  ");
            assertEquals("my_savepoint", sp.name());
        }
    }

    @Nested
    class ComplexScenarios {

        @Test
        void banUser_selectThenInsertBanned() {
            var tx = new TransactionBuilder()
                .add(selectBalance())
                .savepoint("before_ban")
                .add(updateUserStatus())
                .add(insertBannedUser())
                .isolationLevel(IsolationLevel.READ_COMMITTED)
                .build();

            assertEquals(3, tx.operations().length);
            assertEquals(1, tx.savepoints().length);
            assertEquals(IsolationLevel.READ_COMMITTED, tx.isolationLevel());
        }

        @Test
        void multiStepTransfer_withSavepoints() {
            var tx = new TransactionBuilder()
                .add(selectBalance())
                .savepoint("verified_balance")
                .add(debitBalance())
                .savepoint("debited")
                .add(creditBalance())
                .isolationLevel(IsolationLevel.SERIALIZABLE)
                .build();

            assertEquals(3, tx.operations().length);
            assertEquals(2, tx.savepoints().length);
            assertEquals("verified_balance", tx.savepoints()[0].name());
            assertEquals(1, tx.savepoints()[0].position());
            assertEquals("debited", tx.savepoints()[1].name());
            assertEquals(2, tx.savepoints()[1].position());
            assertEquals(IsolationLevel.SERIALIZABLE, tx.isolationLevel());
        }

        @Test
        void bulkInsert_manyOperations() {
            var builder = new TransactionBuilder();
            for (var i = 0; i < 10; i++) {
                builder.add(insertUser());
            }
            var tx = builder.build();

            assertEquals(10, tx.operations().length);
        }

        @Test
        void mixedReadWrite_preservesTypes() {
            var tx = new TransactionBuilder()
                .add(selectBalance())
                .add(debitBalance())
                .add(selectBalance())
                .add(creditBalance())
                .build();

            assertInstanceOf(JQ.Read.class, tx.operations()[0]);
            assertInstanceOf(JQ.Write.class, tx.operations()[1]);
            assertInstanceOf(JQ.Read.class, tx.operations()[2]);
            assertInstanceOf(JQ.Write.class, tx.operations()[3]);
        }
    }

    @Nested
    class Advanced {
    
        @Test
        void insertReturning_thenUpdateUsingReturnedId() {
            var tx = new TransactionBuilder()
                .add(insertUserReturningId())
                .add(updateUserStatus())
                .build();
    
            assertEquals(2, tx.operations().length);
        }
    
        @Test
        void insertUser_thenInsertProfileUsingReturnedId() {
            var tx = new TransactionBuilder()
                .add(insertUserReturningId())
                .add(insertProfile())
                .build();
    
            assertEquals(2, tx.operations().length);
        }
    
        @Test
        void updateReturning_thenInsertAuditRecord() {
            var tx = new TransactionBuilder()
                .add(debitBalanceReturning())
                .add(insertAuditRecord())
                .build();
    
            assertEquals(2, tx.operations().length);
        }
    
        @Test
        void chainedReturningPipeline() {
            var tx = new TransactionBuilder()
                .add(insertUserReturningId())
                .add(insertOrderReturningId())
                .add(insertShipment())
                .build();
    
            assertEquals(3, tx.operations().length);
        }
    
        @Test
        void oneReturnedValueUsedByMultipleOperations() {
            var tx = new TransactionBuilder()
                .add(insertUserReturningId())
                .add(insertProfile())
                .add(insertSettings())
                .build();
    
            assertEquals(3, tx.operations().length);
        }

        @Test
        void savepointAfterReturningOperation() {
            var tx = new TransactionBuilder()
                .add(insertUserReturningId())
                .savepoint("user_created")
                .add(insertProfile())
                .build();
    
            assertEquals(2, tx.operations().length);
            assertEquals(1, tx.savepoints().length);
        }
    
        @Test
        void userOnboardingWorkflow() {
            var tx = new TransactionBuilder()
                .add(insertUserReturningId())
                .savepoint("user_created")
                .add(insertProfile())
                .add(insertSettings())
                .savepoint("profile_initialized")
                .add(insertWelcomeNotification())
                .isolationLevel(IsolationLevel.READ_COMMITTED)
                .build();
    
            assertEquals(4, tx.operations().length);
            assertEquals(2, tx.savepoints().length);
            assertEquals(IsolationLevel.READ_COMMITTED, tx.isolationLevel());
        }
    }

    @Nested
    class ForUpdateTransaction {
        
        private static JQ.Read selectBalanceForUpdate() {
            return select(col("total"))
                .from("orders")
                .where(eq(col("id"), param(Long.class)))
                .forUpdate()
                .build();
        }
        
        private static JQ.Read selectBalanceForUpdateNoWait() {
            return select(col("total"))
                .from("orders")
                .where(eq(col("id"), param(Long.class)))
                .forUpdateNoWait()
                .build();
        }
        
        private static JQ.Read selectBalanceForUpdateSkipLocked() {
            return select(col("total"))
                .from("orders")
                .where(eq(col("id"), param(Long.class)))
                .forUpdateSkipLocked()
                .build();
        }
        
        private static JQ.Read selectOrderWithUserForUpdate() {
            return select(col("o", "total"), col("u", "active"))
                .from(tAs("orders", "o"))
                .join(tAs("users", "u"), eq(col("o", "user_id"), col("u", "id")))
                .where(eq(col("o", "id"), param(Long.class)))
                .forUpdateOf(tAs("orders", "o"), tAs("users", "u"))
                .build();
        }
        
        @Test
        void transferWithRowLock() {
            var tx = transaction()
                .add(selectBalanceForUpdate())
                .savepoint("locked")
                .add(debitBalance())
                .add(creditBalance())
                .build();
            
            assertEquals(3, tx.operations().length);
            assertEquals(1, tx.savepoints().length);
        }
        
        @Test
        void transferWithNoWait() {
            var tx = transaction()
                .add(selectBalanceForUpdateNoWait())
                .add(debitBalance())
                .add(creditBalance())
                .build();
            
            assertEquals(3, tx.operations().length);
        }
        
        @Test
        void transferWithJoinLock() {
            var tx = transaction()
                .add(selectOrderWithUserForUpdate())
                .add(debitBalance())
                .add(creditBalance())
                .build();
            
            assertEquals(3, tx.operations().length);
        }
        
        @Test
        void optimisticLockThenUpdate() {
            var tx = transaction()
                .add(selectBalanceForUpdate())
                .add(update("orders")
                    .set("status", lit("processed"))
                    .where(eq(col("id"), param(Long.class)))
                    .build())
                .build();
            
            assertEquals(2, tx.operations().length);
        }
        
        @Test
        void complexTransferWithSavepointsAndLock() {
            var tx = transaction()
                .isolationLevel(IsolationLevel.REPEATABLE_READ)
                .add(selectBalanceForUpdate())
                .savepoint("balance_locked")
                .add(debitBalance())
                .savepoint("debited")
                .add(selectBalanceForUpdate())
                .add(creditBalance())
                .build();
            
            assertEquals(4, tx.operations().length);
            assertEquals(2, tx.savepoints().length);
        }
    }

    @Nested
    class Validation {

        @Test
        void nullOperation_throws() {
            assertThrows(NullPointerException.class, () ->
                new TransactionBuilder().add(null)
            );
        }

        @Test
        void emptyOperations_throws() {
            assertThrows(IllegalArgumentException.class, () ->
                new TransactionBuilder().build()
            );
        }

        @Test
        void nullIsolationLevel_throws() {
            assertThrows(NullPointerException.class, () ->
                new TransactionBuilder().isolationLevel(null)
            );
        }

        @Test
        void nullSavepointName_throws() {
            assertThrows(NullPointerException.class, () ->
                new TransactionBuilder().add(insertUser()).savepoint(null)
            );
        }

        @Test
        void blankSavepointName_throws() {
            assertThrows(IllegalArgumentException.class, () ->
                new TransactionBuilder().add(insertUser()).savepoint("   ")
            );
        }

        @Test
        void negativeSavepointPosition_throws() {
            assertThrows(IllegalArgumentException.class, () ->
                new Savepoint(-1, "sp")
            );
        }

        @Test
        void transaction_nullOperationsArray_throws() {
            assertThrows(NullPointerException.class, () ->
                new Transaction(null, new Savepoint[0], IsolationLevel.DEFAULT)
            );
        }

        @Test
        void transaction_nullSavepointsArray_throws() {
            assertThrows(NullPointerException.class, () ->
                new Transaction(
                    new JQ[] { insertUser() },
                    null,
                    IsolationLevel.DEFAULT
                )
            );
        }

        @Test
        void transaction_nullIsolationLevel_throws() {
            assertThrows(NullPointerException.class, () ->
                new Transaction(
                    new JQ[] { insertUser() },
                    new Savepoint[0],
                    null
                )
            );
        }

        @Test
        void transaction_nullOperationInArray_throws() {
            assertThrows(NullPointerException.class, () ->
                new Transaction(
                    new JQ[] { insertUser(), null },
                    new Savepoint[0],
                    IsolationLevel.DEFAULT
                )
            );
        }

        @Test
        void transaction_nullSavepointInArray_throws() {
            assertThrows(NullPointerException.class, () ->
                new Transaction(
                    new JQ[] { insertUser() },
                    new Savepoint[] { null },
                    IsolationLevel.DEFAULT
                )
            );
        }
    }
}

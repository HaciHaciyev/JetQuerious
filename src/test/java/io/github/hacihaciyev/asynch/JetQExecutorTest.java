package io.github.hacihaciyev.asynch;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class JetQExecutorTest {

    private JetQExecutor executor;
    private final int DEFAULT_QUEUE_CAPACITY = 1 << 18;
    private final int DEFAULT_BATCH_SIZE = 64;

    @BeforeEach
    void setUp() {
        executor = new JetQExecutor();
    }

    @AfterEach
    void tearDown() throws ExecutionException, InterruptedException {
        if (executor != null) {
            executor.shutdownGracefully().get();
        }
    }

    @Test
    void testDefaultConstructor() {
        assertNotNull(executor);
    }

    @Test
    void testCustomConstructorValidArguments() {
        JetQExecutor customExecutor = new JetQExecutor(1024, 32, null);
        assertNotNull(customExecutor);
        customExecutor.shutdown();
    }

    @Test
    void testConstructorInvalidQueueCapacityTooSmall() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () ->
                new JetQExecutor(1, DEFAULT_BATCH_SIZE, null));
        assertEquals("Queue capacity must be 2 or power of 2", thrown.getMessage());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0})
    void testConstructorInvalidBatchSize(int invalidBatchSize) {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () ->
                new JetQExecutor(DEFAULT_QUEUE_CAPACITY, invalidBatchSize, null));
        assertEquals("Batch size must be positive.", thrown.getMessage());
    }

    @Test
    void testExecuteSimpleTask() throws ExecutionException, InterruptedException {
        CompletableFuture<String> future = executor.execute(() -> "Hello");
        assertEquals("Hello", future.get());
    }

    @Test
    void testExecuteTaskWithException() {
        CompletableFuture<String> future = executor.execute(() -> {
            throw new RuntimeException("Test Exception");
        });

        ExecutionException thrown = assertThrows(ExecutionException.class, future::get);
        assertInstanceOf(RuntimeException.class, thrown.getCause());
        assertEquals("Test Exception", thrown.getCause().getMessage());
    }

    @Test
    void testExecuteNullTaskThrowsException() {
        CompletableFuture<Object> future = executor.execute(null);
        ExecutionException thrown = assertThrows(ExecutionException.class, future::get);
        assertInstanceOf(IllegalStateException.class, thrown.getCause());
        assertEquals("Task cannot be null", thrown.getCause().getMessage());
    }


    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testHighThroughputUnderLoad() throws InterruptedException {
        int numTasks = 100_000;
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(numTasks);
        ExecutorService submitter = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        long startTime = System.nanoTime();

        for (int i = 0; i < numTasks; i++) {
            submitter.submit(() -> {
                executor.execute(() -> {
                    counter.incrementAndGet();
                    return null;
                }).join();
                latch.countDown();
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS), "All tasks should be processed");
        long endTime = System.nanoTime();
        double durationMs = (endTime - startTime) / 1_000_000.0;
        System.out.printf("Processed %d tasks in %.2f ms%n", numTasks, durationMs);

        assertEquals(numTasks, counter.get());
        submitter.shutdown();
        assertTrue(submitter.awaitTermination(1, TimeUnit.SECONDS));
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testMultipleTasksExecutionOrderAndResults() throws InterruptedException {
        int numTasks = 1000;
        List<CompletableFuture<Integer>> futures = new ArrayList<>();
        List<Integer> results = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(numTasks);

        for (int i = 0; i < numTasks; i++) {
            final int taskId = i;
            futures.add(executor.execute(() -> {
                try {
                    Thread.sleep(1);
                    results.add(taskId);
                    return taskId;
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } finally {
                    latch.countDown();
                }
            }));
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS), "All tasks should complete");

        for (CompletableFuture<Integer> future : futures) {
            assertTrue(future.isDone());
            assertFalse(future.isCompletedExceptionally());
        }

        assertEquals(numTasks, results.size());
        for (int i = 0; i < numTasks; i++) {
            assertTrue(results.contains(i), "Result list should contain " + i);
        }
    }

    @Test
    void testShutdownPreventsNewTasks() {
        executor.shutdown();
        CompletableFuture<String> future = executor.execute(() -> "Should not run");

        ExecutionException thrown = assertThrows(ExecutionException.class, future::get);
        assertInstanceOf(IllegalStateException.class, thrown.getCause());
        assertEquals("Executor is shutdown", thrown.getCause().getMessage());
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testShutdownGracefullyWaitsForEmptyQueue() throws InterruptedException, ExecutionException, TimeoutException {
        JetQExecutor customExecutor = new JetQExecutor(1024, 1, null);
        AtomicInteger completedTasks = new AtomicInteger(0);
        int numTasks = 100;
        CountDownLatch startedTasks = new CountDownLatch(numTasks);
        CountDownLatch allowCompletion = new CountDownLatch(1);

        for (int i = 0; i < numTasks; i++) {
            customExecutor.execute(() -> {
                startedTasks.countDown();
                try {
                    allowCompletion.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                completedTasks.incrementAndGet();
                return null;
            });
        }

        startedTasks.await(2, TimeUnit.SECONDS);

        CompletableFuture<Void> shutdownFuture = customExecutor.shutdownGracefully(100, TimeUnit.MILLISECONDS);
        assertFalse(shutdownFuture.isDone(), "Shutdown should not complete immediately while tasks are blocked");

        allowCompletion.countDown();
        shutdownFuture.get(5, TimeUnit.SECONDS);

        assertTrue(shutdownFuture.isDone());
        assertEquals(numTasks, completedTasks.get(), "All tasks should have completed before graceful shutdown finishes.");
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testShutdownGracefullyRespectsTimeout() throws Exception {
        JetQExecutor customExecutor = new JetQExecutor(1024, 1, null);
        AtomicInteger completedTasks = new AtomicInteger(0);
        int numTasks = 5;
        CountDownLatch allowCompletion = new CountDownLatch(1);

        for (int i = 0; i < numTasks; i++) {
            customExecutor.execute(() -> {
                try {
                    allowCompletion.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                completedTasks.incrementAndGet();
                return null;
            });
        }

        Thread.sleep(200);

        long timeoutMs = 100;
        CompletableFuture<Void> shutdownFuture = customExecutor.shutdownGracefully(timeoutMs, TimeUnit.MILLISECONDS);

        assertDoesNotThrow(() -> shutdownFuture.get(timeoutMs + 500, TimeUnit.MILLISECONDS));

        assertTrue(completedTasks.get() < numTasks,
                "Not all tasks should have completed yet");

        allowCompletion.countDown();

        shutdownFuture.get(500, TimeUnit.MILLISECONDS);
        assertEquals(numTasks, completedTasks.get(),
                "All tasks should complete after allowCompletion is released");
    }

    @ParameterizedTest
    @CsvSource({
            "1000000000, NANOSECONDS",
            "1, SECONDS"
    })
    void testShutdownGracefullyWithInfiniteTimeout(long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
        JetQExecutor customExecutor = new JetQExecutor(1024, 1, null);
        AtomicInteger completedTasks = new AtomicInteger(0);
        int numTasks = 10;
        CountDownLatch allowCompletion = new CountDownLatch(1);

        for (int i = 0; i < numTasks; i++) {
            customExecutor.execute(() -> {
                try {
                    allowCompletion.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                completedTasks.incrementAndGet();
                return null;
            });
        }
        Thread.sleep(100);

        CompletableFuture<Void> shutdownFuture = customExecutor.shutdownGracefully(timeout, unit);
        assertFalse(shutdownFuture.isDone());

        allowCompletion.countDown();

        shutdownFuture.get(5, TimeUnit.SECONDS);
        assertTrue(shutdownFuture.isDone());
        assertEquals(numTasks, completedTasks.get(), "All tasks should complete with infinite timeout.");
    }

    @Test
    void testShutdownGracefullyWithNegativeTimeout() {
        assertThrows(IllegalArgumentException.class, () ->
                executor.shutdownGracefully(-1, TimeUnit.SECONDS));
    }

    @Test
    void testShutdownGracefullyWithNullTimeUnit() {
        assertThrows(IllegalArgumentException.class, () ->
                executor.shutdownGracefully(1, null));
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testConcurrentSubmissionsAndShutdown() throws InterruptedException, ExecutionException, TimeoutException {
        JetQExecutor customExecutor = new JetQExecutor(1024, 10, null);
        int numThreads = 10;
        int tasksPerThread = 100;
        AtomicInteger completedTasks = new AtomicInteger(0);
        ExecutorService submitterPool = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads * tasksPerThread);

        for (int i = 0; i < numThreads; i++) {
            submitterPool.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < tasksPerThread; j++) {
                        customExecutor.execute(() -> {
                            completedTasks.incrementAndGet();
                            return null;
                        }).join();
                        doneLatch.countDown();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(5, TimeUnit.SECONDS);

        assertEquals(numThreads * tasksPerThread, completedTasks.get(), "All tasks should have been processed.");

        customExecutor.shutdownGracefully().get(2, TimeUnit.SECONDS);
        submitterPool.shutdown();
        assertTrue(submitterPool.awaitTermination(1, TimeUnit.SECONDS));

        ExecutionException thrown = assertThrows(ExecutionException.class, () ->
                customExecutor.execute(() -> "After shutdown").get());
        assertInstanceOf(IllegalStateException.class, thrown.getCause());
        assertEquals("Executor is shutdown", thrown.getCause().getMessage());
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void minimalBackpressureTest() throws Exception {
        int queueCapacity = 16;
        JetQExecutor customExecutor = new JetQExecutor(queueCapacity, 1, null);

        AtomicInteger executedCount = new AtomicInteger();
        CountDownLatch latch = new CountDownLatch(1);

        customExecutor.execute(() -> {
            try {
                latch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            executedCount.incrementAndGet();
            return null;
        });

        CompletableFuture<Void> f = customExecutor.execute(() -> {
            executedCount.incrementAndGet();
            return null;
        });

        latch.countDown();
        f.get(3, TimeUnit.SECONDS);

        customExecutor.shutdownGracefully().get(3, TimeUnit.SECONDS);
        assertEquals(2, executedCount.get());
    }


    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testExecutorWithLargeQueueCapacityWarning() {
        int largeCapacity = DEFAULT_QUEUE_CAPACITY * 2;
        JetQExecutor largeQueueExecutor = new JetQExecutor(largeCapacity, DEFAULT_BATCH_SIZE, null);
        assertNotNull(largeQueueExecutor);
        largeQueueExecutor.shutdown();
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testExecutorWithLargeBatchSizeWarning() {
        int largeBatchSize = DEFAULT_BATCH_SIZE * 2;
        JetQExecutor largeBatchExecutor = new JetQExecutor(DEFAULT_QUEUE_CAPACITY, largeBatchSize, null);
        assertNotNull(largeBatchExecutor);
        largeBatchExecutor.shutdown();
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testVirtualThreadUsageForTasks() throws ExecutionException, InterruptedException {
        long mainThreadId = Thread.currentThread().threadId();
        AtomicLong taskThreadId = new AtomicLong();

        CompletableFuture<Long> future = executor.execute(() -> {
            taskThreadId.set(Thread.currentThread().threadId());
            assertTrue(Thread.currentThread().isVirtual(), "Task should run on a virtual thread");
            return taskThreadId.get();
        });

        long resultThreadId = future.get();
        assertNotEquals(mainThreadId, resultThreadId, "Task should not run on the main thread.");
        assertTrue(future.isDone());
        assertFalse(future.isCompletedExceptionally());
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testVirtualThreadUsageForBatchExecution() throws ExecutionException, InterruptedException, TimeoutException {
        JetQExecutor customExecutor = new JetQExecutor(256, 10, null);
        long mainThreadId = Thread.currentThread().threadId();
        AtomicInteger tasksExecuted = new AtomicInteger(0);
        ConcurrentLinkedQueue<Long> threadIds = new ConcurrentLinkedQueue<>();
        int numTasks = 50;

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < numTasks; i++) {
            futures.add(customExecutor.execute(() -> {
                threadIds.add(Thread.currentThread().threadId());
                assertTrue(Thread.currentThread().isVirtual(), "Batch task should run on a virtual thread");
                tasksExecuted.incrementAndGet();
                return null;
            }));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(5, TimeUnit.SECONDS);

        assertEquals(numTasks, tasksExecuted.get());
        assertFalse(threadIds.isEmpty());
        for (Long id : threadIds) {
            assertNotEquals(mainThreadId, id, "Batch task should not run on the main thread.");
        }

        customExecutor.shutdownGracefully().get();
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testConsumerThreadLifecycle() throws InterruptedException, ExecutionException, TimeoutException {
        AtomicBoolean taskExecuted = new AtomicBoolean(false);
        CompletableFuture<Void> future = executor.execute(() -> {
            taskExecuted.set(true);
            return null;
        });

        future.get(1, TimeUnit.SECONDS);
        assertTrue(taskExecuted.get());

        executor.shutdownGracefully().get(1, TimeUnit.SECONDS);
        assertTrue(executor.shutdownGracefully().isDone(), "Graceful shutdown should complete.");

        ExecutionException thrown = assertThrows(ExecutionException.class, () ->
                executor.execute(() -> "Post-shutdown").get());
        assertInstanceOf(IllegalStateException.class, thrown.getCause());
    }
}
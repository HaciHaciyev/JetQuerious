package io.github.hacihaciyev.asynch;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.RepeatedTest;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JetQExecutorConcurrentTest {
    private static final int QUEUE_CAPACITY = 1 << 20;
    private JetQExecutor executor;

    @AfterEach
    void shutdownExecutor() throws Exception {
        if (executor != null)
            executor.shutdownGracefully(5, TimeUnit.SECONDS).get();
    }

    @RepeatedTest(10)
    void concurrentTest() throws Exception {
        final int PRODUCERS = 1 << 16;
        final int ITEMS_PER_PRODUCER = 10;
        final int TOTAL_ITEMS = PRODUCERS * ITEMS_PER_PRODUCER;

        executor = new JetQExecutor(QUEUE_CAPACITY, 64, errors -> {});

        ExecutorService producerExecutor = Executors.newVirtualThreadPerTaskExecutor();
        AtomicInteger produced = new AtomicInteger();
        AtomicInteger consumed = new AtomicInteger();

        CompletableFuture<?>[] allFutures = new CompletableFuture<?>[TOTAL_ITEMS];
        AtomicInteger futureIndex = new AtomicInteger();

        for (int i = 0; i < PRODUCERS; i++) {
            final int producerID = i;
            producerExecutes(producerExecutor, ITEMS_PER_PRODUCER, producerID, consumed, futureIndex, allFutures, produced);
        }

        producerExecutor.shutdown();
        boolean producersFinished = producerExecutor.awaitTermination(2, TimeUnit.MINUTES);
        assertTrue(producersFinished, "Producers should finish within timeout");

        CompletableFuture<Void> allOf = CompletableFuture.allOf(allFutures);
        try {
            allOf.get(1, TimeUnit.MINUTES);
        } catch (Exception ignored) {}

        assertEquals(TOTAL_ITEMS, produced.get(), "All tasks should be produced");

        int completedFutures = 0;
        for (CompletableFuture<?> future : allFutures) {
            if (future != null && future.isDone()) completedFutures++;
        }

        assertEquals(TOTAL_ITEMS, completedFutures, "All futures should be completed");
        assertEquals(TOTAL_ITEMS, consumed.get(), "All tasks should be consumed");
    }

    private void producerExecutes(ExecutorService producerExecutor, int ITEMS_PER_PRODUCER, int producerID,
                                  AtomicInteger consumed, AtomicInteger futureIndex,
                                  CompletableFuture<?>[] allFutures, AtomicInteger produced) {

        producerExecutor.execute(() -> {
            for (int j = 0; j < ITEMS_PER_PRODUCER; j++) {
                final int taskID = producerID * ITEMS_PER_PRODUCER + j;

                Supplier<Integer> task = () -> {
                    consumed.incrementAndGet();
                    return taskID;
                };

                CompletableFuture<Integer> future = executor.execute(task);
                int index = futureIndex.getAndIncrement();
                allFutures[index] = future;
                produced.incrementAndGet();
            }
        });
    }
}
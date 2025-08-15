package io.github.hacihaciyev.ds;

import org.junit.jupiter.api.RepeatedTest;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static org.junit.jupiter.api.Assertions.*;

class JetMPSConcurrentTest {

    @RepeatedTest(10)
    void shouldHandleConcurrentProducers() throws InterruptedException {
        final int PRODUCERS = 4;
        final int ITEMS_PER_PRODUCER = 10_000;
        final int TIMEOUT_SEC = 10;
        JetMPSC<Integer> queue = new JetMPSC<>(1024);

        ExecutorService executor = Executors.newFixedThreadPool(PRODUCERS);
        CountDownLatch latch = new CountDownLatch(PRODUCERS);
        AtomicInteger counter = new AtomicInteger();
        AtomicBoolean timeout = new AtomicBoolean(false);

        ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();
        timer.schedule(() -> timeout.set(true), TIMEOUT_SEC, TimeUnit.SECONDS);

        for (int i = 0; i < PRODUCERS; i++) {
            executor.execute(() -> {
                try {
                    for (int j = 0; j < ITEMS_PER_PRODUCER && !timeout.get(); j++) {
                        if (!queue.offer(counter.incrementAndGet())) {
                            Thread.yield();
                            j--;
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        AtomicInteger consumed = new AtomicInteger();
        Thread consumer = new Thread(() -> {
            try {
                while (consumed.get() < PRODUCERS * ITEMS_PER_PRODUCER && !timeout.get()) {
                    Integer item = queue.poll();
                    if (item != null) {
                        consumed.incrementAndGet();
                    } else {
                        Thread.yield();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        consumer.start();

        try {
            assertTrue(latch.await(TIMEOUT_SEC, TimeUnit.SECONDS),
                    "Producers didn't finish in time");
            consumer.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SEC));
        } finally {
            executor.shutdownNow();
            timer.shutdownNow();
            if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                System.err.println("Executor didn't terminate");
            }
        }

        if (timeout.get()) {
            fail("Test timed out. Produced: " + counter.get() + ", consumed: " + consumed.get());
        }

        assertEquals(PRODUCERS * ITEMS_PER_PRODUCER, consumed.get());
    }

    @RepeatedTest(10)
    void correctMPSCTest() throws Exception {
        final int PRODUCERS = 1024;
        final int ITEMS_PER_PRODUCER = 10_000;
        final int TOTAL_ITEMS = PRODUCERS * ITEMS_PER_PRODUCER;

        JetMPSC<Integer> queue = new JetMPSC<>(1 << 16);
        ExecutorService producerExecutor = Executors.newFixedThreadPool(PRODUCERS);
        AtomicInteger produced = new AtomicInteger();

        Thread consumer = new Thread(() -> {
            int consumed = 0;
            while (consumed < TOTAL_ITEMS) {
                Integer item = queue.poll();
                if (item != null) {
                    consumed++;
                }
            }
        });
        consumer.start();

        for (int i = 0; i < PRODUCERS; i++) {
            producerExecutor.execute(() -> {
                for (int j = 0; j < ITEMS_PER_PRODUCER; j++) {
                    while (!queue.offer(j)) {
                        Thread.onSpinWait();
                    }
                    produced.incrementAndGet();
                }
            });
        }

        producerExecutor.shutdown();
        producerExecutor.awaitTermination(1, TimeUnit.MINUTES);
        consumer.join();

        assertEquals(TOTAL_ITEMS, produced.get());
    }
}
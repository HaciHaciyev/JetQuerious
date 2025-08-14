package io.github.hacihaciyev.ds;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JetMPSCStressTest {

    @Test
    void highLoadStressTest() throws InterruptedException {
        final int DURATION_SEC = 5;
        final int PRODUCERS = 4;
        final int BUFFER_SIZE = 256;
        JetMPSC<Long> queue = new JetMPSC<>(BUFFER_SIZE);

        AtomicBoolean running = new AtomicBoolean(true);
        AtomicLong produced = new AtomicLong();
        AtomicLong consumed = new AtomicLong();

        Thread consumer = new Thread(() -> {
            while (running.get() || !queue.isEmpty()) {
                Long item = queue.poll();
                if (item != null) {
                    consumed.incrementAndGet();
                }
            }
        });
        consumer.start();

        ExecutorService producers = Executors.newFixedThreadPool(PRODUCERS);
        for (int i = 0; i < PRODUCERS; i++) {
            producers.execute(() -> {
                while (running.get()) {
                    if (queue.offer(System.nanoTime())) {
                        produced.incrementAndGet();
                    } else {
                        Thread.onSpinWait();
                    }
                }
            });
        }

        Thread.sleep(DURATION_SEC * 1000);
        running.set(false);
        producers.shutdown();
        producers.awaitTermination(1, TimeUnit.SECONDS);
        consumer.join();

        System.out.printf("[Stress] Produced: %,d items%n", produced.get());
        System.out.printf("[Stress] Consumed: %,d items%n", consumed.get());
        System.out.printf("[Stress] Queue size: %d%n", queue.size());

        assertEquals(produced.get(), consumed.get() + queue.size(),
                "Lost items detected. Produced: " + produced.get() +
                        ", Consumed: " + consumed.get() +
                        ", Remaining: " + queue.size());
    }
}

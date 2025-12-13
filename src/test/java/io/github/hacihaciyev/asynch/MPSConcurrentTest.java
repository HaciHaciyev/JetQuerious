package io.github.hacihaciyev.asynch;

import io.github.hacihaciyev.util.MPSC;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.RepeatedTest;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static org.junit.jupiter.api.Assertions.*;

@Disabled("For separate run")
class MPSConcurrentTest {

    private static final MPSC<Integer> QUEUE = new MPSC<>(1 << 20);

    @AfterEach
    void clearQueue() {
        while (QUEUE.poll() != null) {}
    }

    @RepeatedTest(10)
    void concurrentTest() throws Exception {
        final int PRODUCERS = 1 << 20;
        final int ITEMS_PER_PRODUCER = 10;
        final int TOTAL_ITEMS = PRODUCERS * ITEMS_PER_PRODUCER;

        ExecutorService producerExecutor = Executors.newVirtualThreadPerTaskExecutor();
        AtomicInteger produced = new AtomicInteger();

        Thread consumer = Thread.ofVirtual().start(() -> {
            int consumed = 0;
            while (consumed < TOTAL_ITEMS) {
                Integer item = QUEUE.poll();
                if (item != null) {
                    consumed++;
                }
            }
        });

        for (int i = 0; i < PRODUCERS; i++) {
            producerExecutor.execute(() -> {
                for (int j = 0; j < ITEMS_PER_PRODUCER; j++) {
                    while (!QUEUE.offer(j)) {
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
package io.github.hacihaciyev.ds;

import io.github.hacihaciyev.asynch.JetMPSC;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JetMPSCCorrectnessTest {

    @Test
    void shouldPreservePerProducerOrder() throws Exception {
        class Item {
            final int producerId;
            final int seqNum;
            Item(int producerId, int seqNum) {
                this.producerId = producerId;
                this.seqNum = seqNum;
            }
        }

        final int PRODUCERS = 4;
        final int ITEMS_PER_PRODUCER = 1_000;
        JetMPSC<Item> queue = new JetMPSC<>(1 << 8);

        Map<Integer, Integer> lastSeqPerProducer = new ConcurrentHashMap<>();
        Thread consumer = new Thread(() -> {
            for (int i = 0; i < PRODUCERS * ITEMS_PER_PRODUCER; ) {
                Item item = queue.poll();
                if (item != null) {
                    i++;
                    int lastSeq = lastSeqPerProducer.getOrDefault(item.producerId, -1);
                    assertEquals(lastSeq + 1, item.seqNum,
                            "Producer " + item.producerId + " violated order");
                    lastSeqPerProducer.put(item.producerId, item.seqNum);
                }
            }
        });
        consumer.start();

        ExecutorService producers = Executors.newFixedThreadPool(PRODUCERS);
        for (int producerId = 0; producerId < PRODUCERS; producerId++) {
            final int pid = producerId;
            producers.execute(() -> {
                for (int seq = 0; seq < ITEMS_PER_PRODUCER; seq++) {
                    while (!queue.offer(new Item(pid, seq))) {
                        Thread.onSpinWait();
                    }
                }
            });
        }

        producers.shutdown();
        producers.awaitTermination(1, TimeUnit.MINUTES);
        consumer.join();

        for (int producerId = 0; producerId < PRODUCERS; producerId++) {
            assertEquals(ITEMS_PER_PRODUCER - 1,
                    lastSeqPerProducer.get(producerId).intValue());
        }
    }
}

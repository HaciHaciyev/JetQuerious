package io.github.hacihaciyev.asynch;

import io.github.hacihaciyev.util.MPSC;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MPSCBasicTest {
    
    @Test
    void shouldOfferAndPollSingleElement() {
        MPSC<String> queue = new MPSC<>(16);
        assertTrue(queue.offer("test"));
        assertEquals("test", queue.poll());
        assertNull(queue.poll());
    }
    
    @Test
    void shouldHandleFullQueue() {
        MPSC<Integer> queue = new MPSC<>(2);
        assertTrue(queue.offer(1));
        assertTrue(queue.offer(2));
        assertFalse(queue.offer(3));
        assertEquals(1, queue.poll());
        assertTrue(queue.offer(3));
    }
    
    @Test
    void shouldPollBatch() {
        MPSC<Integer> queue = new MPSC<>(8);
        for (int i = 0; i < 5; i++) {
            queue.offer(i);
        }
        
        Integer[] batch = new Integer[3];
        int polled = queue.pollBatch(batch, 3);
        
        assertEquals(3, polled);
        assertArrayEquals(new Integer[]{0, 1, 2}, batch);
        assertEquals(2, queue.size());
    }
    
    @Test
    void shouldMaintainCorrectSize() {
        MPSC<String> queue = new MPSC<>(4);
        assertEquals(0, queue.size());
        
        queue.offer("a");
        queue.offer("b");
        assertEquals(2, queue.size());
        
        queue.poll();
        assertEquals(1, queue.size());
    }

    @Test
    void shouldDetectNullElementsInBatch() {
        MPSC<Integer> queue = new MPSC<>(8);
        for (int i = 0; i < 3; i++) {
            queue.offer(i);
        }

        Integer[] batch = new Integer[5];
        int polled = queue.pollBatch(batch, 5);

        assertEquals(3, polled);

        for (int i = 0; i < polled; i++) {
            assertNotNull(batch[i], "Element at index " + i + " should not be null");
            assertEquals(Integer.valueOf(i), batch[i]);
        }

        for (int i = polled; i < batch.length; i++) {
            assertNull(batch[i], "Element at index " + i + " should be null");
        }
    }

    @Test
    void shouldPollBatchFromEmptyQueue() {
        MPSC<Integer> queue = new MPSC<>(8);
        Integer[] batch = new Integer[3];
        int polled = queue.pollBatch(batch, 3);

        assertEquals(0, polled);
        assertArrayEquals(new Integer[]{null, null, null}, batch);
    }

    @Test
    void shouldPollBatchSmallerThanAvailable() {
        MPSC<Integer> queue = new MPSC<>(8);
        for (int i = 0; i < 5; i++) {
            queue.offer(i);
        }

        Integer[] batch = new Integer[3];
        int polled = queue.pollBatch(batch, 3);

        assertEquals(3, polled);
        assertArrayEquals(new Integer[]{0, 1, 2}, batch);

        assertEquals(3, queue.poll());
        assertEquals(4, queue.poll());
        assertNull(queue.poll());
    }

    @Test
    void shouldPollBatchLargerThanAvailable() {
        MPSC<Integer> queue = new MPSC<>(8);
        for (int i = 0; i < 2; i++) {
            queue.offer(i);
        }

        Integer[] batch = new Integer[5];
        int polled = queue.pollBatch(batch, 5);

        assertEquals(2, polled);
        assertArrayEquals(new Integer[]{0, 1, null, null, null}, batch);
    }

    @Test
    void shouldPollBatchMultipleTimes() {
        MPSC<Integer> queue = new MPSC<>(8);
        for (int i = 0; i < 6; i++) {
            queue.offer(i);
        }

        Integer[] batch1 = new Integer[3];
        int polled1 = queue.pollBatch(batch1, 3);
        assertEquals(3, polled1);
        assertArrayEquals(new Integer[]{0, 1, 2}, batch1);

        Integer[] batch2 = new Integer[3];
        int polled2 = queue.pollBatch(batch2, 3);
        assertEquals(3, polled2);
        assertArrayEquals(new Integer[]{3, 4, 5}, batch2);

        Integer[] batch3 = new Integer[3];
        int polled3 = queue.pollBatch(batch3, 3);
        assertEquals(0, polled3);
        assertArrayEquals(new Integer[]{null, null, null}, batch3);
    }

    @Test
    void shouldPollBatchWithExactSize() {
        MPSC<Integer> queue = new MPSC<>(8);
        for (int i = 0; i < 4; i++) {
            queue.offer(i);
        }

        Integer[] batch = new Integer[4];
        int polled = queue.pollBatch(batch, 4);

        assertEquals(4, polled);
        assertArrayEquals(new Integer[]{0, 1, 2, 3}, batch);
        assertTrue(queue.isEmpty());
    }

    @Test
    void shouldPollBatchAndVerifyArrayContents() {
        MPSC<String> queue = new MPSC<>(8);
        queue.offer("first");
        queue.offer("second");
        queue.offer("third");

        String[] batch = new String[5];
        int polled = queue.pollBatch(batch, 5);

        assertEquals(3, polled);
        assertEquals("first", batch[0]);
        assertEquals("second", batch[1]);
        assertEquals("third", batch[2]);
        assertNull(batch[3]);
        assertNull(batch[4]);

        for (int i = polled; i < batch.length; i++) {
            assertNull(batch[i], "Element at index " + i + " should be null");
        }
    }
}
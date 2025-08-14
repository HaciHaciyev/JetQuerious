package io.github.hacihaciyev.ds;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JetMPSCBasicTest {
    
    @Test
    void shouldOfferAndPollSingleElement() {
        JetMPSC<String> queue = new JetMPSC<>(16);
        assertTrue(queue.offer("test"));
        assertEquals("test", queue.poll());
        assertNull(queue.poll());
    }
    
    @Test
    void shouldHandleFullQueue() {
        JetMPSC<Integer> queue = new JetMPSC<>(2);
        assertTrue(queue.offer(1));
        assertTrue(queue.offer(2));
        assertFalse(queue.offer(3));
        assertEquals(1, queue.poll());
        assertTrue(queue.offer(3));
    }
    
    @Test
    void shouldPollBatch() {
        JetMPSC<Integer> queue = new JetMPSC<>(8);
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
        JetMPSC<String> queue = new JetMPSC<>(4);
        assertEquals(0, queue.size());
        
        queue.offer("a");
        queue.offer("b");
        assertEquals(2, queue.size());
        
        queue.poll();
        assertEquals(1, queue.size());
    }
}
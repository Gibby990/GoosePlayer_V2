package com.gooseplayer2.Packages;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class QueueTest {

    @Test
    //basic add and remove functionality for the Queue class
    void enqueueAndDequeue_basicBehavior() {
        Queue<String> q = new Queue<>();
        assertTrue(q.isEmpty(), "Queue should start empty");
        q.enqueue("one");
        q.enqueue("two");
        assertEquals(2, q.size(), "Queue should have two elements");
        String first = q.dequeue();
        assertEquals("one", first, "First dequeued element should be 'one'");
        String second = q.dequeue();
        assertEquals("two", second, "Second dequeued element should be 'two'");
        assertTrue(q.isEmpty(), "Queue should be empty after dequeuing all items");
    }

    @Test
    //verifies that the method rejects negative indices and indices greater than or equal to the size
    void get_indexOutOfBounds() {
        Queue<String> q = new Queue<>();
        q.enqueue("one");
        assertThrows(IndexOutOfBoundsException.class, () -> q.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> q.get(1));
    }
}

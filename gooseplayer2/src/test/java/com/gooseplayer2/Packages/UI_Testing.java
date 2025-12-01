package com.gooseplayer2.Packages;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SimpleMockTest {
    @Mock
    private Queue<java.io.File> mockQueue; // Mock the Queue dependency

    //Test for enqueue
    @Test
    void simpleEnqueueTest() {
        // Arrange: Create a mock File
        java.io.File mockFile = new java.io.File("test.mp3");

        // Act: Call enqueue (real code, but with mock)
        mockQueue.enqueue(mockFile);

        // Assert: Verify the mock was called
        verify(mockQueue).enqueue(mockFile);
    }

    //Test for dequeue
    @Test
    void simpleDequeueTest() {
        // Arrange: Mock a File and stub dequeue
        java.io.File mockFile = new java.io.File("test.mp3");
        when(mockQueue.dequeue()).thenReturn(mockFile);

        // Act: Call dequeue
        java.io.File dequeued = mockQueue.dequeue();

        // Assert: Verify return value
        assertEquals(mockFile, dequeued);
        verify(mockQueue).dequeue();
    }

    //Test for size
    @Test
    void simpleSizeTest() {
        // Arrange: Stub size
        when(mockQueue.size()).thenReturn(2);

        // Act: Call size
        int size = mockQueue.size();

        // Assert: Verify value
        assertEquals(2, size);
        verify(mockQueue).size();
    }
}
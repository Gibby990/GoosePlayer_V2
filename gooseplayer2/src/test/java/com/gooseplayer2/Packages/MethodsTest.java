package com.gooseplayer2.Packages;

import com.gooseplayer2.JPanels.MusicPlayer;
import net.beadsproject.beads.data.Sample;
import net.beadsproject.beads.data.SampleManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class MethodsTest {

    private MusicPlayer player;
    private Queue<QueuedFile> queue;
    private MockedStatic<SampleManager> mockedSampleManager;

    @BeforeEach
    void setUp() throws Exception {
        System.out.println("SETUP: Creating MusicPlayer with null panel...");

        // MOCK FILE SYSTEM
        File mockFile = mock(File.class);
        when(mockFile.exists()).thenReturn(true);
        when(mockFile.canRead()).thenReturn(true);
        when(mockFile.getAbsolutePath()).thenReturn("/mock/song.mp3");
        when(mockFile.getName()).thenReturn("song.mp3");

        // MOCK SampleManager
        mockedSampleManager = mockStatic(SampleManager.class);
        mockedSampleManager.when(() -> SampleManager.sample(anyString()))
                           .thenReturn(mock(Sample.class));

        player = new MusicPlayer(null, false, "Test Channel");

        // INJECT MOCK FILE INTO QUEUE via reflection
        queue = getField("Queue", Queue.class);
        // We'll override file behavior in tests
        System.out.println("SETUP: Ready!\n");
    }

    @AfterEach
    void tearDown() {
        if (mockedSampleManager != null) {
            mockedSampleManager.close();
        }
    }

    // === HELPERS ===
    private void addMockFiles(int count) throws Exception {
        for (int i = 0; i < count; i++) {
            File mockFile = mock(File.class);
            when(mockFile.exists()).thenReturn(true);
            when(mockFile.canRead()).thenReturn(true);
            when(mockFile.getName()).thenReturn("song" + (i + 1) + ".mp3");
            when(mockFile.getAbsolutePath()).thenReturn("/mock/song" + (i + 1) + ".mp3");
            player.addFilesToTree(List.of(mockFile));
        }
    }

    private <T> T getField(String name, Class<T> type) throws Exception {
        Field f = MusicPlayer.class.getDeclaredField(name);
        f.setAccessible(true);
        return type.cast(f.get(player));
    }

    // === TESTS ===

    @Test
    void shouldAddFilesToQueue() throws Exception {
        System.out.println("TEST: shouldAddFilesToQueue — START");
        addMockFiles(2);

        System.out.println("  → Queue size: " + queue.size());
        System.out.println("  → First song: " + queue.peek().getFile().getName());

        assertEquals(2, queue.size());
        assertEquals("song1.mp3", queue.peek().getFile().getName());
        System.out.println("TEST: PASSED\n");
    }

    @Test
    void shouldClearQueueAndHistory() throws Exception {
        System.out.println("TEST: shouldClearQueueAndHistory — START");
        addMockFiles(2);
        queue.dequeue();

        System.out.println("  → Calling clear()");
        player.clear();

        assertTrue(queue.isEmpty());
        assertTrue(queue.getHistory().isEmpty());
        System.out.println("TEST: PASSED\n");
    }

    @Test
    void shouldShuffleQueue() throws Exception {
        System.out.println("TEST: shouldShuffleQueue — START");
        addMockFiles(4);

        List<String> original = StreamSupport.stream(queue.spliterator(), false)
                .map(qf -> qf.getFile().getName())
                .toList();
        System.out.println("  → Original: " + original);

        player.shuffleQueue();

        List<String> shuffled = StreamSupport.stream(queue.spliterator(), false)
                .map(qf -> qf.getFile().getName())
                .toList();
        System.out.println("  → Shuffled: " + shuffled);

        assertEquals(4, queue.size());
        assertNotEquals(original, shuffled);
        System.out.println("TEST: PASSED\n");
    }

    @Test
    void shouldTrackPlaybackHistory() throws Exception {
        System.out.println("TEST: shouldTrackPlaybackHistory — START");
        addMockFiles(3);

        queue.dequeue();
        queue.dequeue();

        List<String> history = queue.getHistory().stream()
                .map(qf -> qf.getFile().getName())
                .toList();

        System.out.println("  → History: " + history);

        assertEquals(2, history.size());
        assertEquals("song1.mp3", history.get(0));
        assertEquals("song2.mp3", history.get(1));
        System.out.println("TEST: PASSED\n");
    }

    @Test
    void shouldNotAddNonAudioFiles() throws Exception {
        System.out.println("TEST: shouldNotAddNonAudioFiles — START");

        File jpg = mock(File.class);
        when(jpg.getName()).thenReturn("image.jpg");
        when(jpg.exists()).thenReturn(true);
        when(jpg.canRead()).thenReturn(true);

        File pdf = mock(File.class);
        when(pdf.getName()).thenReturn("doc.pdf");
        when(pdf.exists()).thenReturn(true);
        when(pdf.canRead()).thenReturn(true);

        File mp3 = mock(File.class);
        when(mp3.getName()).thenReturn("song.mp3");
        when(mp3.exists()).thenReturn(true);
        when(mp3.canRead()).thenReturn(true);

        player.addFilesToTree(List.of(jpg, pdf, mp3));

        assertEquals(1, queue.size());
        assertTrue(queue.peek().getFile().getName().endsWith(".mp3"));
        System.out.println("TEST: PASSED\n");
    }
}
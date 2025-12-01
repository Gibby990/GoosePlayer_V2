package com.gooseplayer2.Packages;

import com.gooseplayer2.MainFrame;
import com.gooseplayer2.JPanels.*;

import org.assertj.swing.core.matcher.JButtonMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.exception.ComponentLookupException;
import org.assertj.swing.fixture.*;
import org.assertj.swing.timing.Condition;
import org.assertj.swing.timing.Pause;
import org.assertj.swing.timing.Timeout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class MethodsTest {

    private FrameFixture window;
    private AudioPlayerSpy spy;  // SPY REFERENCE


    //SETUP
    @BeforeEach
    void setUp() throws Exception {
        MainFrame frame = GuiActionRunner.execute(() -> new MainFrame());

        spy = GuiActionRunner.execute(() -> {
            try {
                return new AudioPlayerSpy(false, "TestChannel");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        GuiActionRunner.execute(() -> {
            MusicPanel panel = frame.getMusicPanel();
            panel.setPlayerForTest(0, spy);
            panel.rebuildPlayerUI(0);  
        });

        window = new FrameFixture(frame);
        window.show();
        window.resizeTo(new Dimension(1200, 800));

        //setup with a song in the queue
        JTreeFixture queueTree = window.tree("queueTree");
        JTreeFixture libraryTree = window.tree("libraryTree");

        // Wait for at least one song in library
        Pause.pause(new Condition("Library has songs") {
            @Override
            public boolean test() {
                return libraryTree.target().getRowCount() > 1;
            }
        }, Timeout.timeout(10, TimeUnit.SECONDS));

        String songPath = "Library/" + libraryTree.target().getPathForRow(1).getLastPathComponent();
        System.out.println("Dragging: " + songPath);

        libraryTree.drag(songPath);
        queueTree.drop();
    }

    //TEARDOWN
    @AfterEach
    void tearDown() {
        if (window != null) {
            try {
                // 1. Stop audio on EDT
                GuiActionRunner.execute(() -> {
                    spy.stopAudio();  
                    spy.clear();      // clear queue
                });

                // 2. Wait for audio to stop (max 2 sec)
                Pause.pause(new Condition("Audio stopped") {
                    @Override
                    public boolean test() {
                        return !spy.isPlaying(); 
                    }
                }, Timeout.timeout(2, TimeUnit.SECONDS));

            } catch (Exception e) {
                System.err.println("Warning: Audio didn't stop cleanly: " + e.getMessage());
            } finally {
                // 3. Clean up UI
                window.cleanUp();
            }
        }
    }

    @Test
    void clickingPlayButtonCallsPlaySuccess() throws Exception {
        // Wait for UI to rebuild
        Thread.sleep(1000);

        // Find the Play button (from the spy)
        window.button(JButtonMatcher.withText("Play")).click();

        //Thread.sleep(1000);

        assertEquals(1, spy.getPlayCount(), "Play button should call play()");
        System.out.println("Play count: " + spy.getPlayCount());
    }

    @Test
    void secondClickFailsToFindPlayButton_WhenAlreadyPlaying() throws Exception{
        Thread.sleep(1000);
        // First click: "Play" → becomes "Pause"
        window.button(JButtonMatcher.withText("Play")).click();
        Thread.sleep(1000);

        System.out.println("Now button is 'Pause' — trying to click 'Play' again...");

        // EXPECT: ComponentLookupException because "Play" button no longer exists
        ComponentLookupException error = assertThrows(ComponentLookupException.class, () -> {
            window.button(JButtonMatcher.withText("Play")).click();
        });

        // THIS RUNS — TEST PASSES
        System.out.println("TEST PASSED: Expected error caught!");
        System.out.println("   Error: " + error.getMessage());
        System.out.println("   Actual play count: " + spy.getPlayCount());
    }

    // test for play-pause-play sequence and make sure the calls are correctly happening
    @Test
    void playPausePlay_Counts() throws Exception {
        Thread.sleep(1000);

        window.button(JButtonMatcher.withText("Play")).click();
        Thread.sleep(500);
        window.button(JButtonMatcher.withText("Pause")).click();
        Thread.sleep(500);
        window.button(JButtonMatcher.withText("Play")).click();
        //Thread.sleep(1000);

        assertEquals(2, spy.getPlayCount());
        System.out.println("Play count: " + spy.getPlayCount());
        assertEquals(1, spy.getPauseCount());
        System.out.println("Pause count: " + spy.getPauseCount());
    }

    @Test
    void AssertPlayButtonCallsNumber_Wrong() throws Exception {
        Thread.sleep(1000);
        // === ACT: Click Play → Pause → Play ===
        window.button(JButtonMatcher.withText("Play")).click();
        Thread.sleep(1000);
        window.button(JButtonMatcher.withText("Pause")).click();
        Thread.sleep(1000);
        window.button(JButtonMatcher.withText("Play")).click();
        //Thread.sleep(1000);

        // === ASSERT: Expect failure (count ≠ 3) ===
        AssertionError error = assertThrows(AssertionError.class, () -> {
            assertEquals(3, spy.getPlayCount(), "Play button should call play() 3 times");
        });

        // === THIS RUNS AFTER FAILURE ===
        System.out.println("TEST PASSED: Expected failure (count != 3) was detected!");
        System.out.println("   Actual count: " + spy.getPlayCount());  // ← NOW PRINTS
        System.out.println("   Error: " + error.getMessage());
    }

    //Pause test and ensure audio stops when stop button is clicked
    @Test
    void clickingStopButtonCallsPause() throws Exception {
        // Wait for UI to rebuild
        Thread.sleep(1000);

        // Find the Stop button (from the spy)
        window.button(JButtonMatcher.withText("Play")).click();
        Thread.sleep(2900);
        window.button(JButtonMatcher.withText("Pause")).click();

        assertEquals(1, spy.getPauseCount(), "Pause button should call pause()");
        System.out.println("Pause count: " + spy.getPauseCount());
        assertEquals(false, spy.isPlaying(), "Audio should be paused");
    }

    //Check that the Pause method is called precisely once when Pause is clicked
    @Test
    void clickingPauseButtonCallsPauseExactlyOnce() throws Exception {
        Thread.sleep(1000);
        window.button(JButtonMatcher.withText("Play")).click();
        Thread.sleep(800);
        window.button(JButtonMatcher.withText("Pause")).click();
        assertEquals(1, spy.getPauseCount(), "Pause button must call pause() once");
        System.out.println("Pause count: " + spy.getPauseCount());
    }

    // Check that the Pause button disappears after clicking
    @Test
    void pauseButtonDisappearsAfterClicking() throws Exception {
        Thread.sleep(1000);

        window.button(JButtonMatcher.withText("Play")).click();
        Thread.sleep(800);
        window.button(JButtonMatcher.withText("Pause")).click();

        // "Pause" button should be gone → "Play" button is back
        ComponentLookupException ex = assertThrows(ComponentLookupException.class, () -> {
            window.button(JButtonMatcher.withText("Pause")).requireVisible();
        });

        System.out.println("PASSED: Pause button correctly disappeared");
        System.out.println("   Error: " + ex.getMessage());
    }

    //Pause then Skip test to ensure Pause is only called once
    @Test
    void pauseThenSkip_StillOnlyOnePauseCall() throws Exception {
        Thread.sleep(1000);
        window.button(JButtonMatcher.withText("Play")).click();
        Thread.sleep(800);
        window.button(JButtonMatcher.withText("Pause")).click();
        Thread.sleep(500);
        window.button(JButtonMatcher.withText("Skip")).click();

        assertEquals(1, spy.getPauseCount(), "Skip should not add extra pause calls");
        assertEquals(1, spy.getSkipCount(), "Skip should be counted");
        System.out.println("Pause+Skip: Pause=" + spy.getPauseCount() + " Skip=" + spy.getSkipCount());
    }

    //Skip test to ensure Skip method is called when Skip button is clicked
    @Test
    void clickingSkipButtonCallsSkip() throws Exception {          
        Thread.sleep(1000);
        window.button(JButtonMatcher.withText("Skip")).click();

        assertEquals(1, spy.getSkipCount(), "Skip button should call skip()");
        System.out.println("Skip count: " + spy.getSkipCount());
    }

    //test skip button multiple times
    @Test
    void clickingSkipButtonMultipleTimes() throws Exception {       
        Thread.sleep(1000);
        window.button(JButtonMatcher.withText("Skip")).click();
        Thread.sleep(500);
        window.button(JButtonMatcher.withText("Skip")).click();
        Thread.sleep(500);
        window.button(JButtonMatcher.withText("Skip")).click();

        assertEquals(3, spy.getSkipCount(), "Skip button should call skip() three times");
        System.out.println("Skip count: " + spy.getSkipCount());
    }

    //test skip button functionality when queue is empty
    @Test
    void clickingSkipButtonWhenQueueEmpty() throws Exception {       
        Thread.sleep(1000);
        // First, clear the queue
        GuiActionRunner.execute(() -> {
            spy.clear();
        });

        // Now, click Skip
        window.button(JButtonMatcher.withText("Skip")).click();

        assertEquals(1, spy.getSkipCount(), "Skip button should call skip() even when queue is empty");
        System.out.println("Skip count (empty queue): " + spy.getSkipCount());
    }

    //test skip button functionality when audio is paused
    @Test
    void clickingSkipButtonWhenAudioPaused() throws Exception {       
        Thread.sleep(1000);
        window.button(JButtonMatcher.withText("Play")).click();
        Thread.sleep(800);
        window.button(JButtonMatcher.withText("Pause")).click();
        Thread.sleep(500);
        window.button(JButtonMatcher.withText("Skip")).click();

        assertEquals(1, spy.getSkipCount(), "Skip button should call skip() when audio is paused");
        System.out.println("Skip count (audio paused): " + spy.getSkipCount());
    }

    //test remove button functionality
    @Test
    void clickingRemoveButtonCallsRemove() throws Exception {       
        Thread.sleep(1000);
        window.button(JButtonMatcher.withText("Remove")).click();

        assertEquals(1, spy.getRemoveCount(), "Remove button should call remove()");
        System.out.println("Remove count: " + spy.getRemoveCount());
    }

    //test remove button multiple times
    @Test
    void clickingRemoveButtonMultipleTimes() throws Exception {      
        Thread.sleep(1000);
        window.button(JButtonMatcher.withText("Remove")).click();
        Thread.sleep(500);
        window.button(JButtonMatcher.withText("Remove")).click();

        assertEquals(2, spy.getRemoveCount(), "Remove button should call remove() two times");
        System.out.println("Remove count: " + spy.getRemoveCount());
    }

    //test remove button when queue is empty
    @Test
    void clickingRemoveButtonWhenQueueEmpty() throws Exception {       
        Thread.sleep(1000);
        // First, clear the queue
        GuiActionRunner.execute(() -> {
            spy.clear();
        });
        Thread.sleep(2000);
        // Now, click Remove
        window.button(JButtonMatcher.withText("Remove")).click();

        assertEquals(1, spy.getRemoveCount(), "Remove button should call remove() even when queue is empty");
        System.out.println("Remove count (empty queue): " + spy.getRemoveCount());
    }

    //test that remove cannot remove currently playing song
    @Test
    void removeCurrentlyPlayingSong_CallsMethodButSkipsLogic() throws Exception {
        Thread.sleep(1000);

        window.button(JButtonMatcher.withText("Play")).click();
        Thread.sleep(3000);

        JTreeFixture queueTree = window.tree("queueTree");
        queueTree.selectRow(1);

        int before = spy.getRemoveCount();
        window.button(JButtonMatcher.withText("Remove")).click();
        Thread.sleep(1000);

        assertEquals(before + 1, spy.getRemoveCount(), "remove() called, but should skip removal logic");
        assertEquals("Pause", spy.PlayPause.getText());
        assertEquals(2, queueTree.target().getRowCount(), "Song still there (not removed)");
        System.out.println("PASSED: Method called, but removal blocked");
    }
    
    //test that remove can remove a non-playing song
    @Test
    void removeNonPlayingSong_RemovesSuccessfully() throws Exception {

        // 1. Start playing the first song
        window.button(JButtonMatcher.withText("Play")).click();
        Thread.sleep(2500);

        // 2. Add a SECOND song to the queue (the non-playing one)
        JTreeFixture libraryTree = window.tree("libraryTree");
        JTreeFixture queueTree = window.tree("queueTree");

        // Drag the SECOND song from library to queue
        String secondSongPath = libraryTree.target().getPathForRow(2).getLastPathComponent().toString();
        libraryTree.drag("Library/" + secondSongPath);
        queueTree.drop();
        Thread.sleep(800);

        // 3. Now queue has 2 songs: [0] = playing, [1] = next
        // Select the SECOND song (index 1 in tree = row 2 because row 0 is root)
        queueTree.selectRow(2);  // row 2 = second song

        // 4. Click Remove
        int before = spy.getRemoveCount();
        window.button(JButtonMatcher.withText("Remove")).click();
        Thread.sleep(3000);

        // 5. Assert: remove() was called AND queue now has only 1 song
        assertEquals(before + 1, spy.getRemoveCount(), 
            "remove() should be called for non-playing song");

        // Root + 1 song = 2 rows total since row 0 is root
        assertEquals(2, queueTree.target().getRowCount(), 
            "Only the playing song should remain (root + 1 song)");

        System.out.println("PASSED: Non-playing song removed successfully");
        System.out.println("   Queue now has " + (queueTree.target().getRowCount() - 1) + " song(s)");
    }

    //test clear button functionality
    @Test
    void clickingClearButtonCallsClear() throws Exception {       
        Thread.sleep(1000);
        window.button(JButtonMatcher.withText("Clear")).click();
        Thread.sleep(2000);

        assertEquals(1, spy.getClearCount(), "Clear button should call clear()");
        System.out.println("Clear count: " + spy.getClearCount());
    }

    //test clear button multiple times
    @Test
    void clickingClearButtonMultipleTimes() throws Exception {      
        Thread.sleep(1000);
        window.button(JButtonMatcher.withText("Clear")).click();
        Thread.sleep(1500);
        window.button(JButtonMatcher.withText("Clear")).click();
        Thread.sleep(1500);

        assertEquals(2, spy.getClearCount(), "Clear button should call clear() two times");
        System.out.println("Clear count: " + spy.getClearCount());
    }

    //test clear button when queue is already empty
    @Test
    void clickingClearButtonWhenQueueEmpty() throws Exception {      
        Thread.sleep(1000);

        GuiActionRunner.execute(() -> spy.clear());  // setup
        Thread.sleep(1000);

        // Reset counter so we only count the button click
        GuiActionRunner.execute(() -> {
            try {
                java.lang.reflect.Field field = AudioPlayerSpy.class.getDeclaredField("clearCount");
                field.setAccessible(true);
                AtomicInteger counter = (AtomicInteger) field.get(spy);
                counter.set(0);
            } catch (Exception e) { throw new RuntimeException(e); }
        });

        window.button(JButtonMatcher.withText("Clear")).click();
        Thread.sleep(1000);

        assertEquals(1, spy.getClearCount());
    }

    //test clear button when audio is playing
    @Test
    void clickingClearButtonWhenAudioPlaying() throws Exception {      
        Thread.sleep(1000);
        window.button(JButtonMatcher.withText("Play")).click();
        Thread.sleep(800);
        window.button(JButtonMatcher.withText("Clear")).click();
        Thread.sleep(2000);

        assertEquals(1, spy.getClearCount(), "Clear button should call clear() when audio is playing");
        System.out.println("Clear count (audio playing): " + spy.getClearCount());
    }

    //test clear button when audio is paused
    @Test
    void clickingClearButtonWhenAudioPaused() throws Exception {      
        Thread.sleep(1000);
        window.button(JButtonMatcher.withText("Play")).click();
        Thread.sleep(800);
        window.button(JButtonMatcher.withText("Pause")).click();
        Thread.sleep(500);
        window.button(JButtonMatcher.withText("Clear")).click();
        Thread.sleep(2000);

        assertEquals(1, spy.getClearCount(), "Clear button should call clear() when audio is paused");
        System.out.println("Clear count (audio paused): " + spy.getClearCount());
    }

    //test shuffle button functionality
    @Test
    void clickingShuffleButtonCallsShuffle() throws Exception {      
        Thread.sleep(1000);
        window.button(JButtonMatcher.withText("Shuffle")).click();
        Thread.sleep(2000);

        assertEquals(1, spy.getShuffleCount(), "Shuffle button should call shuffle()");
        System.out.println("Shuffle count: " + spy.getShuffleCount());
    }

    //test shuffle button multiple times
    @Test
    void clickingShuffleButtonMultipleTimes() throws Exception {      
        Thread.sleep(1000);
        // Add more songs to the queue
        JTreeFixture libraryTree = window.tree("libraryTree");
        JTreeFixture queueTree = window.tree("queueTree");

        // Drag the SECOND and THIRD songs from library to queue
        String secondSongPath = libraryTree.target().getPathForRow(2).getLastPathComponent().toString();
        String thirdSongPath = libraryTree.target().getPathForRow(3).getLastPathComponent().toString();
        libraryTree.drag("Library/" + secondSongPath);
        queueTree.drop();

        libraryTree.drag("Library/" + thirdSongPath);
        queueTree.drop();

        Thread.sleep(800);
        window.button(JButtonMatcher.withText("Shuffle")).click();
        Thread.sleep(1500);
        window.button(JButtonMatcher.withText("Shuffle")).click();
        Thread.sleep(2500);

        assertEquals(2, spy.getShuffleCount(), "Shuffle button should call shuffle() two times");
        System.out.println("Shuffle count: " + spy.getShuffleCount());
    }

    //test shuffle button when queue has only one song
    @Test
    void clickingShuffleButtonWhenQueueHasOneSong() throws Exception {      
        Thread.sleep(1000);
        JTreeFixture libraryTree = window.tree("libraryTree");
        JTreeFixture queueTree = window.tree("queueTree");
        String secondSongPath = libraryTree.target().getPathForRow(2).getLastPathComponent().toString();
        // Cear the queue and add only one song
        spy.clear();  
        Thread.sleep(2000);
        libraryTree.drag("Library/" + secondSongPath);
        queueTree.drop();
        Thread.sleep(2000);
        // Now, click Shuffle
        window.button(JButtonMatcher.withText("Shuffle")).click();
        Thread.sleep(1000);

        assertEquals(1, spy.getShuffleCount(), "Shuffle button should call shuffle() even with one song");
        System.out.println("Shuffle count (one song): " + spy.getShuffleCount());
    }

    //test shuffle button when queue is empty
    @Test
    void clickingShuffleButtonWhenQueueEmpty() throws Exception {      
        Thread.sleep(1000);
        // First, clear the queue
        GuiActionRunner.execute(() -> {
            spy.clear();
        });
        Thread.sleep(2000);
        // Now, click Shuffle
        window.button(JButtonMatcher.withText("Shuffle")).click();
        Thread.sleep(1000);

        assertEquals(1, spy.getShuffleCount(), "Shuffle button should call shuffle() even when queue is empty");
        System.out.println("Shuffle count (empty queue): " + spy.getShuffleCount());
    }

    //test shuffle button when audio is playing
    @Test
    void clickingShuffleButtonWhenAudioPlaying() throws Exception {      
        Thread.sleep(1000);
        window.button(JButtonMatcher.withText("Play")).click();
        Thread.sleep(800);
        window.button(JButtonMatcher.withText("Shuffle")).click();
        Thread.sleep(2000);

        assertEquals(1, spy.getShuffleCount(), "Shuffle button should call shuffle() when audio is playing");
        System.out.println("Shuffle count (audio playing): " + spy.getShuffleCount());
    }

    //test shuffle button when audio is paused
    @Test
    void clickingShuffleButtonWhenAudioPaused() throws Exception {      
        Thread.sleep(1000);
        window.button(JButtonMatcher.withText("Play")).click();
        Thread.sleep(800);
        window.button(JButtonMatcher.withText("Pause")).click();
        Thread.sleep(500);
        window.button(JButtonMatcher.withText("Shuffle")).click();
        Thread.sleep(2000);

        assertEquals(1, spy.getShuffleCount(), "Shuffle button should call shuffle() when audio is paused");
        System.out.println("Shuffle count (audio paused): " + spy.getShuffleCount());
    }

}
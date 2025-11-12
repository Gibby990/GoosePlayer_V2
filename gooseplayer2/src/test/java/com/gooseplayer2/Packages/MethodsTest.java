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
//import org.assertj.swing.core.matcher.JButtonMatcher;
//import org.assertj.swing.timing.Condition;
//import org.assertj.swing.timing.Pause;
//import org.assertj.swing.timing.Timeout;
//import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
//import java.lang.reflect.Field;
//import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;  // ← MOCKITO

class MethodsTest {

    private FrameFixture window;
    private AudioPlayerSpy spy;  // ← SPY REFERENCE


    //SETUP
    @BeforeEach
    void setUp() throws Exception {
        MainFrame frame = GuiActionRunner.execute(() -> new MainFrame());

        spy = GuiActionRunner.execute(() -> {
            try {
                return new AudioPlayerSpy(mock(JComponent.class), false, "TestChannel");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        GuiActionRunner.execute(() -> {
            MusicPanel panel = frame.getMusicPanel();
            panel.setPlayerForTest(0, spy);
            panel.rebuildPlayerUI(0);  // ← THIS IS KEY
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
                    spy.stopAudio();  // ← CRITICAL
                    spy.clear();      // ← Optional: clear queue
                });

                // 2. Wait for audio to stop (max 2 sec)
                Pause.pause(new Condition("Audio stopped") {
                    @Override
                    public boolean test() {
                        return !spy.isPlaying();  // ← You need this method
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

}
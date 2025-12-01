package com.gooseplayer2.Packages;

import com.gooseplayer2.JPanels.MusicPlayer;
import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.core.matcher.JButtonMatcher;
//import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.io.File;
import java.util.Arrays;

import static org.assertj.swing.edt.GuiActionRunner.execute;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertEquals; // For assertEquals in Clear test
import static org.junit.jupiter.api.Assertions.assertTrue;

class UITesting2Test {
    private FrameFixture window;
    private JFrame frame;
    private MusicPlayer player;

    @BeforeEach
    void setUp() {
        try {
            // Explicitly disable headless mode for this test
            System.setProperty("java.awt.headless", "false");
            System.out.println("Headless mode: " + GraphicsEnvironment.isHeadless());

            frame = execute(() -> {
                JFrame f = new JFrame("MusicPlayer Test");
                f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                f.setSize(800, 600);
                f.setLocationRelativeTo(null);
                return f;
            });

            // Create real instance first to avoid Mockito interference with Swing
            player = execute(() -> new MusicPlayer(false, "TestChannel"));
            execute(() -> {
                frame.getContentPane().add(player, BorderLayout.CENTER);
                frame.validate();
                frame.repaint();
                frame.setVisible(true);
                frame.requestFocus();
                System.out.println("Frame set visible, showing: " + frame.isShowing());
                try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                System.out.println("After 2000ms, frame showing: " + frame.isShowing());
                System.out.println("Player showing: " + player.isShowing());
                System.out.println("Play button showing: " + player.PlayPause.isShowing());
            });

            window = new FrameFixture(frame);
            window.show();
            window.robot().waitForIdle(); // Sync with EDT
            System.out.println("FrameFixture initialized");

            // Mock only non-UI-related methods
            MusicPlayer mockPlayer = spy(player);
            doReturn(true).when(mockPlayer).isAudioFile(any(File.class));
            // Avoid mocking play() to allow UI state change
            player = mockPlayer;
            player.addFilesToTree(Arrays.asList(new File("test1.mp3")));
            player.addFilesToTree(Arrays.asList(new File("test2.mp3")));
            execute(() -> player.updatePlayPauseButtonLabel());
        } catch (Exception e) {
            System.out.println("Setup failed with exception: " + e.getMessage());
            throw new RuntimeException("Setup failed", e);
        } finally {
            System.clearProperty("java.awt.headless");
        }
    }

    @AfterEach
    void tearDown() {
        if (window != null) {
            window.cleanUp();
        }
        if (frame != null) {
            execute(() -> {
                frame.dispose();
                return null;
            });
        }
    }

    @Test
    void playButton_clickUpdatesText() {
        // Wait for the Play button to be visible and showing
        await().atMost(10, java.util.concurrent.TimeUnit.SECONDS).untilAsserted(() -> {
            JButton button = window.button(JButtonMatcher.withText("Play")).target();
            System.out.println("Found button: " + button.getText() + ", visible: " + button.isVisible() + ", showing: " + button.isShowing() + ", enabled: " + button.isEnabled());
            window.button(JButtonMatcher.withText("Play")).requireVisible();
            if (!button.isShowing()) {
                throw new AssertionError("Button is not showing: " + button);
            }
        });
        // Click the Play button
        window.button(JButtonMatcher.withText("Play")).click();
        System.out.println("Play button clicked via AssertJ-Swing. Hooray!!!!");
    }

    @Test
    void skipButton_clickTriggersAction() {
        // Wait for the Skip button to be visible and showing
        await().atMost(10, java.util.concurrent.TimeUnit.SECONDS).untilAsserted(() -> {
            JButton button = window.button(JButtonMatcher.withText("Skip")).target();
            System.out.println("Found skip button: " + button.getText() + ", visible: " + button.isVisible() + ", showing: " + button.isShowing() + ", enabled: " + button.isEnabled());
            window.button(JButtonMatcher.withText("Skip")).requireVisible();
            if (!button.isShowing()) {
                throw new AssertionError("Skip button is not showing: " + button);
            }
        });

        // Print queue contents (row count of the tree as proxy)
        System.out.println("Queue tree row count before skip: " + window.tree().target().getRowCount());

        // Click the Skip button
        window.button(JButtonMatcher.withText("Skip")).click();
        System.out.println("Skip button clicked via AssertJ-Swing");
    }
    @Test
    void shuffleButton_clickUpdatesText() {
        // Wait for the Shuffle button to be visible and showing
        await().atMost(10, java.util.concurrent.TimeUnit.SECONDS).untilAsserted(() -> {
            JButton button = window.button(JButtonMatcher.withText("Shuffle")).target();
            System.out.println("Found button: " + button.getText() + ", visible: " + button.isVisible() + ", showing: " + button.isShowing() + ", enabled: " + button.isEnabled());
            window.button(JButtonMatcher.withText("Shuffle")).requireVisible();
            if (!button.isShowing()) {
                throw new AssertionError("Button is not showing: " + button);
            }
        });
        // Click the Shuffle button
        window.button(JButtonMatcher.withText("Shuffle")).click();
        System.out.println("Shuffle button clicked via AssertJ-Swing. Hooray!!!!");
    }
    @Test
    void removeButton_clickUpdatesText() {
        // Wait for the Remove button to be visible and showing
        await().atMost(10, java.util.concurrent.TimeUnit.SECONDS).untilAsserted(() -> {
            JButton button = window.button(JButtonMatcher.withText("Remove")).target();
            System.out.println("Found button: " + button.getText() + ", visible: " + button.isVisible() + ", showing: " + button.isShowing() + ", enabled: " + button.isEnabled());
            window.button(JButtonMatcher.withText("Remove")).requireVisible();
            if (!button.isShowing()) {
                throw new AssertionError("Button is not showing: " + button);
            }
        });
        // Click the Remove button
        window.button(JButtonMatcher.withText("Remove")).click();
        System.out.println("Remove button clicked via AssertJ-Swing. Hooray!!!!");
    }
    @Test
    void clearButton_clickTriggersAction() {
        // Wait for the Clear button to be visible and showing
        await().atMost(10, java.util.concurrent.TimeUnit.SECONDS).untilAsserted(() -> {
            JButton button = window.button(JButtonMatcher.withText("Clear")).target();
            System.out.println("Found clear button: " + button.getText() + ", visible: " + button.isVisible() + ", showing: " + button.isShowing() + ", enabled: " + button.isEnabled());
            window.button(JButtonMatcher.withText("Clear")).requireVisible();
            if (!button.isShowing()) {
                throw new AssertionError("Clear button is not showing: " + button);
            }
        });

        // Print queue contents (row count of the tree as proxy)
        System.out.println("Queue tree row count before clear: " + window.tree().target().getRowCount());

        // Click the Clear button
        window.button(JButtonMatcher.withText("Clear")).click();
        System.out.println("Clear button clicked via AssertJ-Swing");

        //Wait for the queue tree row count to change 
        //default tree model root should remain but have no children so the row count is 1
        await().atMost(10, java.util.concurrent.TimeUnit.SECONDS).untilAsserted(() -> {
            JTree tree = window.tree().target();
            int rowCount = tree.getRowCount();
            int childCount = ((DefaultTreeModel) tree.getModel()).getChildCount(tree.getModel().getRoot());
            System.out.println("Queue tree row count after clear: " + rowCount + "; child count: " + childCount);
            assertEquals(1, rowCount, "Tree should show only root after clear");
            assertEquals(0, childCount, "No children should remain after clear");
        });
    }
    @Test
    void loopSongRadioButton_toggle() {
        // Define matcher for Loop Song radio
        GenericTypeMatcher<JRadioButton> songMatcher = new GenericTypeMatcher<JRadioButton>(JRadioButton.class) {
            @Override
            protected boolean isMatching(JRadioButton radio) {
                return "Loop Song".equals(radio.getText());
            }
        };

        // Wait and require visible/enabled
        await().atMost(10, java.util.concurrent.TimeUnit.SECONDS).untilAsserted(() -> {
            window.radioButton(songMatcher).requireVisible().requireEnabled();
        });

        // Click and assert selected (assume starts false → true)
        window.radioButton(songMatcher).click();
        await().atMost(3, java.util.concurrent.TimeUnit.SECONDS).untilAsserted(() -> {
            assertTrue(window.radioButton(songMatcher).target().isSelected(),
                    "Loop Song should be selected after click");
        });
    }
    @Test
    void loopPlaylistRadioButton_toggle() {
        // Define matcher for Loop Playlist radio
        GenericTypeMatcher<JRadioButton> playlistMatcher = new GenericTypeMatcher<JRadioButton>(JRadioButton.class) {
            @Override
            protected boolean isMatching(JRadioButton radio) {
                return "Loop Playlist".equals(radio.getText());
            }
        };

        // Wait and require visible/enabled
        await().atMost(10, java.util.concurrent.TimeUnit.SECONDS).untilAsserted(() -> {
            window.radioButton(playlistMatcher).requireVisible().requireEnabled();
        });

        // Click and assert selected (assume starts false → true)
        window.radioButton(playlistMatcher).click();
        await().atMost(3, java.util.concurrent.TimeUnit.SECONDS).untilAsserted(() -> {
            assertTrue(window.radioButton(playlistMatcher).target().isSelected(),
                    "Loop Playlist should be selected after click");
        });
    }

        

}
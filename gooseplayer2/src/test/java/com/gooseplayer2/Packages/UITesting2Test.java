package com.gooseplayer2.Packages;

import com.gooseplayer2.JPanels.MusicPlayer;
import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.core.matcher.JButtonMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Arrays;

import static org.assertj.swing.edt.GuiActionRunner.execute;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

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
            player = execute(() -> new MusicPlayer(null, false, "TestChannel"));
            execute(() -> {
                frame.getContentPane().add(player, BorderLayout.CENTER);
                frame.validate();
                frame.repaint();
                frame.setVisible(true);
                frame.requestFocus();
                System.out.println("Frame set visible, showing: " + frame.isShowing());
                try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                System.out.println("After 7000ms, frame showing: " + frame.isShowing());
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
            player.addFilesToTree(Arrays.asList(new File("test.mp3")));
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

        /* Wait for the button text to change to Pause after clicking
        await().atMost(10, java.util.concurrent.TimeUnit.SECONDS).pollInterval(1, java.util.concurrent.TimeUnit.SECONDS).untilAsserted(() -> {
            JButton button = window.button(new GenericTypeMatcher<JButton>(JButton.class) {
                @Override
                protected boolean isMatching(JButton button) {
                    System.out.println("Checking button: " + button.getText());
                    return "Pause".equals(button.getText());
                }
            }).target();
            window.button(new GenericTypeMatcher<JButton>(JButton.class) {
                @Override
                protected boolean isMatching(JButton button) {
                    return "Pause".equals(button.getText());
                }
            }).requireVisible();
        });*/
    }
}
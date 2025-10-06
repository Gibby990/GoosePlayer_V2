package com.gooseplayer2.Packages;

import com.gooseplayer2.JPanels.MusicPlayer;
import org.assertj.swing.core.matcher.JButtonMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;

class UITesting2Test {
    private FrameFixture window;
    private MusicPlayer player;
    private JFrame frame;

    @BeforeEach
    void setUp() {
        try {
            frame = GuiActionRunner.execute(() -> new JFrame("Test Frame"));
            player = GuiActionRunner.execute(() -> new MusicPlayer(null, false, "TestChannel"));
            GuiActionRunner.execute(() -> {
                frame.add(player);
                frame.setSize(800, 600);
                return null;
            });
            window = new FrameFixture(frame);
            window.show();
        } catch (Exception e) {
            throw new RuntimeException("Setup failed", e);
        }
    }

    @AfterEach
    void tearDown() {
        if (window != null) {
            window.cleanUp();
        }
        if (frame != null) {
            GuiActionRunner.execute(() -> {
                frame.dispose();
                return null;
            });
        }
    }

    @Test
    void playButton_clickUpdatesText() {
        window.button(JButtonMatcher.withText("Play")).click();
        window.button(JButtonMatcher.withText("Pause")).requireVisible();
    }
}
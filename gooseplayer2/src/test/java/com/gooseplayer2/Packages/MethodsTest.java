package com.gooseplayer2.Packages;

import com.gooseplayer2.MainFrame;
import com.gooseplayer2.JPanels.*;

import org.assertj.swing.core.matcher.JButtonMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.*;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;  // ← MOCKITO

class MethodsTest {

    private FrameFixture window;
    private AudioPlayerSpy spy;  // ← SPY REFERENCE


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
}
    @AfterEach
    void tearDown() {
        if (window != null) {
            window.cleanUp();
        }
    }
    @Test
    void clickingPlayButtonCallsPlaySuccess() throws Exception {
        // Wait for UI to rebuild
        Thread.sleep(1000);

        // Find the Play button (from the spy)
        window.button(JButtonMatcher.withText("Play")).click();

        Thread.sleep(1000);

        assertEquals(1, spy.getPlayCount(), "Play button should call play()");
        System.out.println("Play count: " + spy.getPlayCount());
    }

    @Test
    void clickingPlayButtonCallsPlayFail() throws Exception {
        // Wait for UI to rebuild
        Thread.sleep(1000);

        // Find the Play button (from the spy)
        window.button(JButtonMatcher.withText("Play")).click();
         Thread.sleep(1000);
        window.button(JButtonMatcher.withText("Play")).click();

        Thread.sleep(1000);

        assertEquals(3, spy.getPlayCount(), "Play button should call play()");
        System.out.println("Play count: " + spy.getPlayCount());
    }
}
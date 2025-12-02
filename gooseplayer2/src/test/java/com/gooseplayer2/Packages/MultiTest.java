package com.gooseplayer2.Packages;

import com.gooseplayer2.Config;
import com.gooseplayer2.MainFrame;
import com.gooseplayer2.JPanels.*;

import org.assertj.swing.core.matcher.JButtonMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.exception.ComponentLookupException;
import org.assertj.swing.fixture.*;
import org.assertj.swing.timing.Condition;
import org.assertj.swing.timing.Pause;
import org.assertj.swing.timing.Timeout;
import org.junit.jupiter.api.*;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class MultiTest {

    private FrameFixture window;
    private MainFrame frame;

    private final Map<String, AudioPlayerSpy> spies = new HashMap<>();
    private final Map<String, JPanelFixture> panels = new HashMap<>();
    private final Map<String, JTreeFixture> queues = new HashMap<>();

    private static final java.util.List<String> CHANNELS = 
        java.util.List.of("Channel 1", "Channel 2", "Channel 3");

    @BeforeEach
    void setUp() throws Exception {
        forceMultiModeAndChannelNames();

        frame = GuiActionRunner.execute(MainFrame::new);
        window = new FrameFixture(frame);
        window.show();
        window.resizeTo(new Dimension(1600, 1000));

        // Create 3 spies with REAL multi-channel layout (no Clear/Shuffle)
        for (String ch : CHANNELS) {
            AudioPlayerSpy spy = GuiActionRunner.execute(() -> {
                try {
                    return new AudioPlayerSpy(true, ch);  // true = slim multi layout
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            spies.put(ch, spy);
        }

        // Inject all spies
        GuiActionRunner.execute(() -> {
            MusicPanel panel = frame.getMusicPanel();
            for (int i = 0; i < panel.getPlayers().size(); i++) {
                MusicPlayer p = panel.getPlayers().get(i);
                AudioPlayerSpy spy = spies.get(p.getChannelName());
                if (spy != null) {
                    panel.setPlayerForTest(i, spy);
                    panel.rebuildPlayerUI(i);
                }
            }
        });

        // Drag one song into each queue
        JTreeFixture libraryTree = window.tree("libraryTree");
        Pause.pause(new Condition("Library loaded") {
            @Override public boolean test() { return libraryTree.target().getRowCount() > 1; }
        }, Timeout.timeout(10, TimeUnit.SECONDS));

        for (String ch : CHANNELS) {
            String panelName = "playerPanel_" + ch.replaceAll("\\s+", "_");

            Pause.pause(new Condition("Panel " + ch + " ready") {
                @Override public boolean test() {
                    try { window.panel(panelName); return true; }
                    catch (ComponentLookupException ignored) { return false; }
                }
            }, Timeout.timeout(5, TimeUnit.SECONDS));

            JPanelFixture panel = window.panel(panelName);
            panels.put(ch, panel);
            queues.put(ch, panel.tree("queueTree"));

            int songRow = CHANNELS.indexOf(ch) + 1;
            libraryTree.drag(songRow);
            queues.get(ch).drop();
        }

        // Wait until all queues have a song
        Pause.pause(new Condition("All queues have songs") {
            @Override public boolean test() {
                return queues.values().stream().allMatch(q -> q.target().getRowCount() > 1);
            }
        }, Timeout.timeout(10, TimeUnit.SECONDS));
    }

    @AfterEach
    void tearDown() {
        if (window != null) {
            GuiActionRunner.execute(() -> spies.values().forEach(s -> {
                s.stopAudio();
                s.clear();
            }));
            window.cleanUp();
        }
    }

    @Test
    void playOnChannel1_DoesNotAffectOthers() {
        clickAndAssertOnly("Channel 1", p -> p.button(JButtonMatcher.withText("Play")),
                AudioPlayerSpy::getPlayCount);
    }

    @Test
    void skipOnChannel2_DoesNotAffectOthers() {
        panels.get("Channel 2").button(JButtonMatcher.withText("Play")).click();
        Pause.pause(800);
        clickAndAssertOnly("Channel 2", p -> p.button(JButtonMatcher.withText("Skip")),
                AudioPlayerSpy::getSkipCount);
    }

    @Test
    void removeOnChannel3_DoesNotAffectOthers() {
        clickAndAssertOnly("Channel 3", p -> p.button(JButtonMatcher.withText("Remove")),
                AudioPlayerSpy::getRemoveCount);
    }

    private void clickAndAssertOnly(String activeChannel,
                                    java.util.function.Function<JPanelFixture, JButtonFixture> buttonGetter,
                                    java.util.function.ToIntFunction<AudioPlayerSpy> counterGetter) {
        buttonGetter.apply(panels.get(activeChannel)).click();
        Pause.pause(1000);

        for (String ch : CHANNELS) {
            int count = counterGetter.applyAsInt(spies.get(ch));
            assertEquals(ch.equals(activeChannel) ? 1 : 0, count,
                    ch + " should " + (ch.equals(activeChannel) ? "" : "NOT ") + "have reacted");
        }
        System.out.println("PASSED: Action on " + activeChannel + " isolated!");
    }

    private void forceMultiModeAndChannelNames() {
        GuiActionRunner.execute(() -> {
            try {
                File f = new File(Config.SETTINGS_FILE_PATH);
                Properties p = new Properties();
                if (f.exists()) {
                    try (var r = new FileReader(f)) { p.load(r); }
                }
                p.setProperty("style", "multi");
                p.setProperty("multichannel1name", "Channel 1");
                p.setProperty("multichannel2name", "Channel 2");
                p.setProperty("multichannel3name", "Channel 3");
                try (var w = new FileWriter(f)) { p.store(w, "Forced by tests"); }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
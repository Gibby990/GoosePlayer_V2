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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;                     
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

public class MultiplayerTest {
// === FIXTURES ===
private FrameFixture window;
private JPanelFixture playerPanel;
private JButtonFixture playButton, skipButton, removeButton, clearButton, shuffleButton;
private JTreeFixture queueTree;
private AudioPlayerSpy spy;

private MainFrame frame;  // Keep reference to frame so we can access it later

@BeforeEach
void setUp() throws Exception {
    // 1. Force multi-channel mode + known channel names
    forceMultiModeAndChannelNames();

    // 2. Create main frame (now guaranteed to be in multi mode)
    frame = GuiActionRunner.execute(MainFrame::new);

    // 3. Get the real name of the first player (e.g. "Channel 1")
    String targetChannel = GuiActionRunner.execute(() -> 
        frame.getMusicPanel().getPlayers().get(0).getChannelName()
    );

    // 4. Create spy with FULL mono layout (so it has Clear/Shuffle)
    spy = GuiActionRunner.execute(() -> {
        try {
            return new AudioPlayerSpy(false, targetChannel); 
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    });

    // 5. Replace player 0 with our full-featured spy
    GuiActionRunner.execute(() -> {
        MusicPanel panel = frame.getMusicPanel();
        panel.setPlayerForTest(0, spy);
        panel.rebuildPlayerUI(0);
    });

    // 6. Show window
    window = new FrameFixture(frame);
    window.show();
    window.resizeTo(new Dimension(1200, 800));

    // 6. Wait for our spied player panel to appear (using the real name)
    String panelName = "playerPanel_" + targetChannel.replaceAll("\\s+", "_");

    Pause.pause(new Condition("Spy player panel appears: " + panelName) {
        @Override
        public boolean test() {
            try {
                window.panel(panelName);
                return true;
            } catch (ComponentLookupException e) {
                return false;
            }
        }
    }, Timeout.timeout(10, TimeUnit.SECONDS));

    playerPanel = window.panel(panelName);

    // 7. Extract all fixtures from THIS player panel only
    playButton     = playerPanel.button(JButtonMatcher.withText("Play"));
    skipButton     = playerPanel.button(JButtonMatcher.withText("Skip"));
    removeButton   = playerPanel.button(JButtonMatcher.withText("Remove"));
    clearButton    = playerPanel.button(JButtonMatcher.withText("Clear"));
    shuffleButton  = playerPanel.button(JButtonMatcher.withText("Shuffle"));
    queueTree      = playerPanel.tree("queueTree");

}   
//TEARDOWN this wall Mr Gorbachev
@AfterEach
    void tearDown() {
        if (window == null) return;

        try {
            GuiActionRunner.execute(() -> {
                try {
                    MusicPanel musicPanel = ((MainFrame) window.target()).getMusicPanel();

                    // This works for BOTH mono and multi mode
                    for (MusicPlayer player : musicPanel.getPlayers()) {
                        if (player != null) {
                            player.stopAudio();   // stops Clip / SourceDataLine
                            player.clear();       // clears queue + resets state
                        }
                    }
                } catch (Exception e) {
                    // Never let cleanup crash the test run
                    System.err.println("Error during audio cleanup: " + e.getMessage());
                }
            });

            // Small pause so the audio threads can actually die
            Pause.pause(300);  // 300 ms is more than enough

        } finally {
            // ALWAYS clean up the Swing window (this also disposes native resources)
            window.cleanUp();
            window = null;
        }
    }
    
    @Test
    void playOnChannel1_DoesNotAffectChannel2() throws Exception {
        // === 1. Set up spy on Channel 1 ===
        String channel1Name = "Channel 1";
        AudioPlayerSpy spyChannel1 = GuiActionRunner.execute(() -> {
            try {
                return new AudioPlayerSpy(true, channel1Name);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        // === 2. Set up spy on Channel 2 ===
        String channel2Name = "Channel 2";
        AudioPlayerSpy spyChannel2 = GuiActionRunner.execute(() -> {
            try {
                return new AudioPlayerSpy(true, channel2Name);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        // === 3. Set up spy on Channel 3 ===
        String channel3Name = "Channel 3";
        AudioPlayerSpy spyChannel3 = GuiActionRunner.execute(() -> {
            try {
                return new AudioPlayerSpy(true, channel3Name);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        // === 3. Inject all spies ===

        GuiActionRunner.execute(() -> {
            MusicPanel panel = ((MainFrame) window.target()).getMusicPanel();

            for (int i = 0; i < panel.getPlayers().size(); i++) {
                MusicPlayer p = panel.getPlayers().get(i);
                if (channel1Name.equals(p.getChannelName())) {
                    panel.setPlayerForTest(i, spyChannel1);
                    panel.rebuildPlayerUI(i);
                } else if (channel2Name.equals(p.getChannelName())) {
                    panel.setPlayerForTest(i, spyChannel2);
                    panel.rebuildPlayerUI(i);
                }
            }
        });

        // === 4. Get fixtures for Channels  ===
        JPanelFixture channel1Panel = window.panel("playerPanel_" + channel1Name.replaceAll("\\s+", "_"));
        JButtonFixture playButtonCh1 = channel1Panel.button(JButtonMatcher.withText("Play"));

        // Add a song to Channel 1's queue
        JTreeFixture libraryTree = window.tree("libraryTree");
        JTreeFixture queueTreeCh1 = channel1Panel.tree("queueTree");

        // Add a song to Channel 2's queue
        JPanelFixture channel2Panel = window.panel("playerPanel_" + channel2Name.replaceAll("\\s+", "_"));
        JTreeFixture queueTreeCh2 = channel2Panel.tree("queueTree");

        Pause.pause(new Condition("Library loaded") {
            @Override public boolean test() { return libraryTree.target().getRowCount() > 1; }
        }, Timeout.timeout(10, TimeUnit.SECONDS));

        libraryTree.drag(1);
        queueTreeCh1.drop();

        libraryTree.drag(1);
        queueTreeCh2.drop();

        // === 5. ACT: Play on Channel 1 only ===
        playButtonCh1.click();

        // Give audio system a moment to react
        Pause.pause(1000);

        // === 6. ASSERT: Only Channel 1 played, Channel 2 untouched ===
        assertEquals(1, spyChannel1.getPlayCount(), "Channel 1 should have been played");
        assertEquals(0, spyChannel2.getPlayCount(), "Channel 2 should NOT have been played");

        System.out.println("PASSED: Playing Channel 1 did NOT trigger Channel 2!");
    }
    @Test
    void skipOnChannel1_DoesNotAffectChannel2() throws Exception {
        // === 1. Set up spy on Channel 1 ===
        String channel1Name = "Channel 1";
        AudioPlayerSpy spyChannel1 = GuiActionRunner.execute(() -> {
            try {
                return new AudioPlayerSpy(true, channel1Name);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });     
        // === 2. Set up spy on Channel 2 (to observe it does NOTHING) ===
        String channel2Name = "Channel 2";  

        AudioPlayerSpy spyChannel2 = GuiActionRunner.execute(() -> {
            try {
                return new AudioPlayerSpy(true, channel2Name);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        // === 3. Set up spy on Channel 3 (to observe it does NOTHING) ===
        String channel3Name = "Channel 3"; 
        AudioPlayerSpy spyChannel3 = GuiActionRunner.execute(() -> {
            try {
                return new AudioPlayerSpy(true, channel3Name);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        // === 3. Inject both spies ===
        GuiActionRunner.execute(() -> {
            MusicPanel panel = ((MainFrame) window.target()).getMusicPanel();

            for (int i = 0; i < panel.getPlayers().size(); i++) {
                MusicPlayer p = panel.getPlayers().get(i);
                if (channel1Name.equals(p.getChannelName())) {
                    panel.setPlayerForTest(i, spyChannel1);
                    panel.rebuildPlayerUI(i);
                } else if (channel2Name.equals(p.getChannelName())) {
                    panel.setPlayerForTest(i, spyChannel2);
                    panel.rebuildPlayerUI(i);
                }
            }
        });
        // === 4. Get fixtures for Channel 1 only ===
        JPanelFixture channel1Panel = window.panel("playerPanel_" + channel1Name.replaceAll
            ("\\s+", "_"));
        JButtonFixture skipButtonCh1 = channel1Panel.button(JButtonMatcher.withText("Skip"));
        JPanelFixture channel2Panel = window.panel("playerPanel_" + channel2Name.replaceAll
            ("\\s+", "_"));   
        JButtonFixture skipButtonCh2 = channel2Panel.button(JButtonMatcher.withText("Skip"));
        // Add a song to Channel 1's queue
        JTreeFixture libraryTree = window.tree("libraryTree");
        JTreeFixture queueTreeCh1 = channel1Panel.tree("queueTree");        
        Pause.pause(new Condition("Library loaded") {
            @Override public boolean test() { return libraryTree.target().getRowCount() > 1; }
        }, Timeout.timeout(10, TimeUnit.SECONDS));
        libraryTree.drag(1);
        queueTreeCh1.drop();
        // Start playback so Skip has something to skip
        JButtonFixture playButtonCh1 = channel1Panel.button(JButtonMatcher.withText("Play"));
        playButtonCh1.click();
        Pause.pause(1000); // Give audio system a moment to react
        // === 5. ACT: Skip on Channel 1 ===
        skipButtonCh1.click();
        Pause.pause(1000); // Give audio system a moment to react
        //Click skip for the second channel to ensure it does not affect the first channel
        skipButtonCh2.click();
        // === 6. ASSERT: Only Channel 1 skipped, Channel 2 untouched ===
        assertEquals(1, spyChannel1.getSkipCount(), "Channel 1 should have been skipped");
        assertEquals(1, spyChannel2.getSkipCount(), "Channel 2 should NOT have been skipped");
        assertEquals(0, spyChannel3.getPlayCount(), "Channel 3 should NOT have been played");
        System.out.println("PASSED: Skipping Channel 1 did NOT trigger Channel 2!");
    }


    private void forceMultiModeAndChannelNames() {
    GuiActionRunner.execute(() -> {
        try {
            File settingsFile = new File(Config.SETTINGS_FILE_PATH);
            Properties p = new Properties();
            if (settingsFile.exists()) {
                try (FileReader r = new FileReader(settingsFile)) {
                    p.load(r);
                }
            }

            // FORCE multi mode + predictable names
            p.setProperty("style", "multi");
            p.setProperty("multichannel1name", "Channel 1");
            p.setProperty("multichannel2name", "Channel 2");
            p.setProperty("multichannel3name", "Channel 3");

            try (FileWriter w = new FileWriter(settingsFile)) {
                p.store(w, "Forced by tests - multi mode");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to force multi mode", e);
        }
    });
}




}


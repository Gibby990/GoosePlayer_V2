package com.gooseplayer2.Packages;
import com.gooseplayer2.MainFrame;
import com.gooseplayer2.JPanels.MusicPanel;
import com.gooseplayer2.JPanels.MusicPlayer;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JButtonFixture;
import org.assertj.swing.fixture.JTreeFixture;
import org.assertj.swing.core.matcher.JButtonMatcher;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.assertj.swing.timing.Condition;
import org.assertj.swing.timing.Pause;
import org.assertj.swing.timing.Timeout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.awt.Dimension;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;
import com.gooseplayer2.Config;           // ← fixes "Config cannot be resolved"
import java.util.Properties;              // ← fixes "Properties cannot be resolved"

class MusicPlayerIntegrationTest {

    private FrameFixture window;
    
    // === Helper method for assertions with logging ===       
    private void assertAndLog(boolean condition, String message) {
    assertTrue(condition, message + " [FAILED]");
    System.out.println(message + " [SUCCESS]");
    }

    @BeforeEach
    void setUp() {
        // === 1. Force MONO mode by overwriting settings file ===
        GuiActionRunner.execute(() -> {
            try {
                File settingsFile = new File(Config.SETTINGS_FILE_PATH);
                Properties p = new Properties();
                if (settingsFile.exists()) {
                    try (FileReader r = new FileReader(settingsFile)) {
                        p.load(r);
                    }
                }

                // FORCE MONO MODE
                p.setProperty("style", "mono");
                p.setProperty("monochannelname", "Player 1");  // or "Channel 1" if you prefer

                try (FileWriter w = new FileWriter(settingsFile)) {
                    p.store(w, "Forced by tests - MONO MODE");
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to force mono mode", e);
            }
        });

        // === 2. Now create the frame — it will read the forced settings ===
        MainFrame frame = GuiActionRunner.execute(() -> {
            try {
                return new MainFrame();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        window = new FrameFixture(frame);
        window.show();
        window.show();
        window.resizeTo(new Dimension(1200, 800));

        // wait
        Pause.pause(new Condition("Main window is visible and sized") {
            @Override
            public boolean test() {
                return window.target().isShowing() &&
                    window.target().getWidth() >= 1180 &&   // a little tolerance
                    window.target().getHeight() >= 780;
            }
        }, Timeout.timeout(5, TimeUnit.SECONDS));

        assertAndLog(true, "Player is up: Window is visible and correctly sized");
        //window.resizeTo(new Dimension(1200, 800));  // bigger = easier to see
    }

    @Test
    void clearQueueThenDragSongFromLibraryToQueue() throws InterruptedException {
        // ASSERT PLAYER IS UP AND READY 
        assertAndLog(window != null, "Player is up: FrameFixture initialized");
        assertAndLog(window.target().isVisible(), "Player is up: Window is visible");
        assertAndLog("Music Player".equals(window.target().getTitle()), 
                    "Player is up: Title is 'Music Player'");
        // Player size check
        Dimension expected = new Dimension(1200, 800);
        assertAndLog(expected.equals(window.target().getSize()), 
                 "Player is up: Window size is 1200x800");
        window.requireVisible();
        window.requireTitle("Music Player");
        //time
        Thread.sleep(2_000);

        // === 1. CLICK "Clear" BUTTON ===
        JButtonFixture clearButton = window.button(JButtonMatcher.withText("Clear"));
        clearButton.requireVisible().requireEnabled();
        clearButton.click();
        System.out.println("Clear button clicked");

        // === ASSERT: Queue is empty ===
        JTreeFixture queueTree = window.tree("queueTree");

        Pause.pause(new Condition("Queue is cleared") {
            @Override
            public boolean test() {
                return queueTree.target().getRowCount() == 1;
            }
        }, Timeout.timeout(3, TimeUnit.SECONDS));

        assertNull(queueTree.target().getPathForRow(1), 
            "No songs should remain in queue");

        System.out.println("Queue cleared: only root node remains");

        // === 2. DRAG FIRST SONG FROM LIBRARY TO QUEUE ===
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

        // === 3. VERIFY SONG IN QUEUE ===
        Pause.pause(new Condition("Song in queue") {
            @Override
            public boolean test() {
                return queueTree.target().getRowCount() > 1;
            }
        }, Timeout.timeout(5, TimeUnit.SECONDS));

        String queued = queueTree.target().getPathForRow(1).getLastPathComponent().toString();
        assertFalse(queued.isEmpty(), "Song should be in queue");

        //time
        Thread.sleep(2_000);

        // === 4. CLICK "Play" BUTTON ===
        JButtonFixture playButton = window.button(JButtonMatcher.withText("Play"));
        playButton.requireVisible().requireEnabled();
        playButton.click();
        System.out.println("Play button clicked");

        //time
        Thread.sleep(3_000);

        // === 5. CLICK "Pause" BUTTON ===
        JButtonFixture pauseButton = window.button(JButtonMatcher.withText("Pause"));
        pauseButton.requireVisible().requireEnabled();
        pauseButton.click();
        System.out.println("Pause button clicked");

        //time
        Thread.sleep(2_000);

        // === 6. CLICK "Play" BUTTON AGAIN ===
        playButton.requireVisible().requireEnabled();
        playButton.click();
        System.out.println("Play button clicked");

        //time
        Thread.sleep(2_000);

        // === 7. CLICK "Skip" BUTTON THEN "Remove" BUTTON ===
        JButtonFixture skipButton = window.button(JButtonMatcher.withText("Skip"));
        skipButton.requireVisible().requireEnabled();
        skipButton.click();
        System.out.println("Skip button clicked");

        //time
        Thread.sleep(2_000);

        // === 9. REMOVE SONG FROM QUEUE ===
        Pause.pause(new Condition("Song appears in queue") {
            @Override
            public boolean test() {
                return queueTree.target().getRowCount() > 1;
            }
        }, Timeout.timeout(5, TimeUnit.SECONDS));

        String queuedSongPath = "Queue/" + queueTree.target().getPathForRow(1).getLastPathComponent();
        System.out.println("Selecting song in queue: " + queuedSongPath);
        queueTree.clickPath(queuedSongPath);  // ← SELECT IT

        Thread.sleep(1_000);  // Let UI update with the selection
        
        JButtonFixture removeButton = window.button(JButtonMatcher.withText("Remove"));
        removeButton.requireVisible().requireEnabled();
        removeButton.click();
        System.out.println("Remove button clicked");

        //time
        Thread.sleep(2_000);

        // === 8. KEEP OPEN 5 SECONDS ===
        System.out.println("SUCCESSFUL Test!. Window closes in 5s...");
        Thread.sleep(5_000);

        // === 9. STOP AUDIO BEFORE CLOSING ===
        GuiActionRunner.execute(() -> {
            try {
                JPanel container = window.panel("musicPanel").target();
                MusicPanel musicPanel = (MusicPanel) container;
                MusicPlayer player = musicPanel.getPlayers().get(0);
                player.stopAudio();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        // === 10. DISPOSE UI ON EDT ===
        GuiActionRunner.execute(() -> {
            JFrame frame = (JFrame) window.target();  // ← THIS IS CORRECT
            frame.dispose();
        });

        // === 11. GIVE AUDIO + EDT TIME TO DIE ===
        Thread.sleep(3000);  // 3 seconds
    }

    @AfterEach
    void tearDown() {
        if (window != null) {
            window.cleanUp();
        }
    }
}
package com.gooseplayer2.Packages;  // If you move to src/test/java/com/gooseplayer2/, change to: package com.gooseplayer2;

import com.gooseplayer2.MainFrame;  // Import for MainFrame (needed due to subpackage)
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.*;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MusicPlayerIntegrationTest {

    private JFrame mainFrame;

    @BeforeEach
    void setUp() throws UnsupportedAudioFileException, IOException, LineUnavailableException, InterruptedException {
        // Use a latch to wait for EDT completion
        CountDownLatch latch = new CountDownLatch(1);

        // Launch the full application in EDT
        SwingUtilities.invokeLater(() -> {
            try {
                mainFrame = new MainFrame();
                mainFrame.setVisible(true);
            } catch (Exception e) {
                throw new RuntimeException("Failed to launch MainFrame", e);
            } finally {
                latch.countDown();  // Signal completion even on error
            }
        });

        // Wait for EDT to process (up to 5s)
        if (!latch.await(5, TimeUnit.SECONDS)) {
            fail("EDT did not complete within 5 seconds");
        }

        // Additional wait for full rendering (components to load)
        Thread.sleep(2000);  // Give time for panels to initialize (FilePanel tree build, etc.)

        // Verify visibility (safe to call from test thread)
        assertNotNull(mainFrame, "MainFrame is still null after launch");
        assertTrue(mainFrame.isVisible(), "MainFrame is not visible after launch");
    }

    @Test
    void launchesFullPlayerAndClosesSuccessfully() {
        // Assert the frame is properly initialized and visible (already checked in setUp)
        assertNotNull(mainFrame);
        assertTrue(mainFrame.isVisible());
        assertEquals("Music Player", mainFrame.getTitle());

        // Optional: Interact minimally, e.g., find a component
        JToolBar toolBar = findToolBar(mainFrame);
        assertNotNull(toolBar);

        // Test closes without errors (dispose will trigger windowClosing event, saving queues)
        mainFrame.dispose();
        assertFalse(mainFrame.isDisplayable());
    }

    @AfterEach
    void tearDown() {
        if (mainFrame != null && mainFrame.isDisplayable()) {
            mainFrame.dispose();
        }
    }

    private JToolBar findToolBar(JFrame frame) {
        for (java.awt.Component c : frame.getContentPane().getComponents()) {
            if (c instanceof JToolBar) {
                return (JToolBar) c;
            }
        }
        return null;
    }
}
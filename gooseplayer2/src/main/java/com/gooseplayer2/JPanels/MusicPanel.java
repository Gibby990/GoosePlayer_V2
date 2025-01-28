package com.gooseplayer2.JPanels;

import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.border.*;
import javazoom.jl.decoder.JavaLayerException;
import java.awt.*;
import java.awt.event.*;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import com.gooseplayer2.Packages.Slugcat;
import com.gooseplayer2.Config;

public class MusicPanel extends JPanel {
    private GridBagLayout layout;
    private GridBagConstraints gbc;
    private Border outline;
    private String loadedStyle;
    private FilePanel filePanel;
    private Properties p;
    private FileReader reader;
    private MultiPlayer player1, player2, player3;
    private boolean[] playerMuted = new boolean[3]; // Track mute state for each player

    public MusicPanel() throws UnsupportedAudioFileException, IOException, LineUnavailableException, JavaLayerException {

        layout = new GridBagLayout();
        gbc = new GridBagConstraints();
        setLayout(layout);

        filePanel = new FilePanel();

        outline = BorderFactory.createLineBorder(Color.BLACK);
        Slugcat Monk = new Slugcat();

        reader = new FileReader(Config.SETTINGS_FILE_PATH);
        p = new Properties();
        p.load(reader);

        loadedStyle = p.getProperty("style");

        if (loadedStyle.equals("Multichannel")) {
            try {
                player1 = new MultiPlayer(1, filePanel);
                player2 = new MultiPlayer(2, filePanel);
                player3 = new MultiPlayer(3, filePanel);
            } catch (Exception e) {
                e.printStackTrace();
            }

            player1.setBorder(outline);
            player2.setBorder(outline);
            player3.setBorder(outline);

            gbc.fill = GridBagConstraints.BOTH;
            gbc.weightx = 1.0; 
            gbc.weighty = 1.0;

            Monk.addObjects(player1, this, layout, gbc, 0, 0, 1, 1);
            Monk.addObjects(player2, this, layout, gbc, 0, 1, 1, 1);
            Monk.addObjects(player3, this, layout, gbc, 0, 2, 1, 1);
        } else { // Go to Monochannel by default
            try {
                player1 = new MultiPlayer(1, filePanel);
            } catch (Exception e) {
                e.printStackTrace();
            }

            player1.setBorder(outline);

            gbc.fill = GridBagConstraints.BOTH;
            gbc.weightx = 1.0; 
            gbc.weighty = 1.0;

            Monk.addObjects(player1, this, layout, gbc, 0, 0, 1, 1);

        }

        // Add global key listener
        addGlobalKeyListener();
    }    

    private void addGlobalKeyListener() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent e) {
                if (e.getID() == KeyEvent.KEY_PRESSED) {
                    handleKeyPress(e);
                    return true; // Consume the event
                }
                return false; 
            }
        });
    }

    private void handleKeyPress(KeyEvent e) {
        int keyCode = e.getKeyCode();
        System.out.println(keyCode);
        boolean ctrlDown = e.isControlDown();
        boolean shiftDown = e.isShiftDown();

        if (ctrlDown && shiftDown) {
            switch (keyCode) {
                case KeyEvent.VK_P:
                    startAllPlayers();
                    break;
                case KeyEvent.VK_S:
                    pauseAllPlayers();
                    break;
                case KeyEvent.VK_1:
                case KeyEvent.VK_2:
                case KeyEvent.VK_3:
                    muteSelectedPlayer(keyCode - KeyEvent.VK_0); 
                    break;
            }
        }
    }

    private void muteSelectedPlayer(int playerNumber) {
        MultiPlayer player = getPlayerByNumber(playerNumber);
        if (player != null) {
            int index = playerNumber - 1; // Convert to 0-based index
            playerMuted[index] = !playerMuted[index]; // Toggle mute state
            if (playerMuted[index]) {
                player.mute();
                System.out.println("Player " + playerNumber + " muted");
            } else {
                player.unmute();
                System.out.println("Player " + playerNumber + " unmuted");
            }
        }
    }

    private MultiPlayer getPlayerByNumber(int number) {
        switch (number) {
            case 1: return player1;
            case 2: return player2;
            case 3: return player3;
            default: return null;
        }
    }

    private void startAllPlayers() {
        player1.play();
        player2.play();
        player3.play();
    }

    private void pauseAllPlayers() {
        player1.pause();
        player2.pause();
        player3.pause();
    }
}


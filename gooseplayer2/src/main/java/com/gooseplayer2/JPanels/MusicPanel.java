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

import com.gooseplayer2.Packages.Cat;
import com.gooseplayer2.Config;

public class MusicPanel extends JPanel {
    private GridBagLayout layout;
    private GridBagConstraints gbc;
    private Border outline;
    private String loadedStyle, monoChannelName, multiChannel1Name, multiChannel2Name, multiChannel3Name;
    private Properties p;
    private FileReader reader;
    private MusicPlayer player1, player2, player3;
    private boolean[] playerMuted = new boolean[3]; 
    private Mode mode;

    private enum Mode { MONO, MULTI, RADIO }

    public MusicPanel() throws UnsupportedAudioFileException, IOException, LineUnavailableException, JavaLayerException {

        layout = new GridBagLayout();
        gbc = new GridBagConstraints();
        setLayout(layout);

        this.setName("musicPanel");  // to identify in tests

        outline = BorderFactory.createLineBorder(Color.BLACK);
        Cat Monk = new Cat();

        // Importing Settings

        reader = new FileReader(Config.SETTINGS_FILE_PATH);
        p = new Properties();
        p.load(reader);

        loadedStyle = p.getProperty("style");

        monoChannelName = p.getProperty("monochannelname");
        multiChannel1Name = p.getProperty("multichannel1name");
        multiChannel2Name = p.getProperty("multichannel2name");
        multiChannel3Name = p.getProperty("multichannel3name");

        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0; 
        gbc.weighty = 1.0;

        mode = determineMode(loadedStyle);

        switch (mode) {
            case RADIO: {
                RadioPlayer radioPlayer = new RadioPlayer();
                radioPlayer.setBorder(outline);
                Monk.addObjects(radioPlayer, this, layout, gbc, 0, 0, 1, 1);
                break;
            }
            case MULTI: {
                try {
                    player1 = new MusicPlayer(true, multiChannel1Name);
                    player2 = new MusicPlayer(true, multiChannel2Name);
                    player3 = new MusicPlayer(true, multiChannel3Name);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                player1.setBorder(outline);
                player2.setBorder(outline);
                player3.setBorder(outline);

                Monk.addObjects(player1, this, layout, gbc, 0, 0, 1, 1);
                Monk.addObjects(player2, this, layout, gbc, 0, 1, 1, 1);
                Monk.addObjects(player3, this, layout, gbc, 0, 2, 1, 1);
                break;
            }
            case MONO:
            default: {
                try {
                    player1 = new MusicPlayer(false, monoChannelName);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                player1.setBorder(outline);
                Monk.addObjects(player1, this, layout, gbc, 0, 0, 1, 1);
                break;
            }
        }

        addGlobalKeyListener();
    }    

    private void addGlobalKeyListener() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent e) {
                if (e.getID() == KeyEvent.KEY_PRESSED) {
                    handleKeyPress(e);
                    return true; 
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
        MusicPlayer player = getPlayerByNumber(playerNumber);
        if (player != null) {
            int index = playerNumber - 1; 
            playerMuted[index] = !playerMuted[index];
            if (playerMuted[index]) {
                player.mute();
                System.out.println("Player " + playerNumber + " muted");
            } else {
                player.unmute();
                System.out.println("Player " + playerNumber + " unmuted");
            }
        }
    }

    private MusicPlayer getPlayerByNumber(int number) {
        switch (number) {
            case 1: return player1;
            case 2: return player2;
            case 3: return player3;
            default: return null;
        }
    }

    private void startAllPlayers() {
        for (MusicPlayer p : getPlayers()) {
            p.play();
        }
    }

    private void pauseAllPlayers() {
        for (MusicPlayer p : getPlayers()) {
            p.pause();
        }
    }

    public java.util.List<MusicPlayer> getPlayers() {
        java.util.List<MusicPlayer> players = new java.util.ArrayList<>();
        if (player1 != null) players.add(player1);
        if (player2 != null) players.add(player2);
        if (player3 != null) players.add(player3);
        return players;
    }

    private Mode determineMode(String rawStyle) {
        if (rawStyle == null) {
            return Mode.MONO;
        }
        String style = rawStyle.trim().toLowerCase();
        if (style.equals("radio")) {
            return Mode.RADIO;
        }
        if (style.equals("multi") || style.equals("multichannel") || style.equals("multi-channel") ||
            style.equals("multi_channel") || style.equals("channels") || style.equals("channel")) {
            return Mode.MULTI;
        }
        return Mode.MONO;
    }

    public boolean isRadioMode() {
        return mode == Mode.RADIO;
    }
        // --------------------------------------------------------------------
    // TEST-ONLY: Allows swapping a real MusicPlayer with a spy in tests
    // --------------------------------------------------------------------
    public void setPlayerForTest(int index, MusicPlayer player) {
        switch (index) {
            case 0 -> player1 = player;
            case 1 -> player2 = player;
            case 2 -> player3 = player;
        }
    }
    // Method to MusicPanel
public void rebuildPlayerUI(int index) {
    MusicPlayer player = switch (index) {
        case 0 -> player1;
        case 1 -> player2;
        case 2 -> player3;
        default -> null;
    };
    if (player == null) return;

    // Remove old player UI
    removeAll();

    // Re-add all players
    Cat Monk = new Cat();
    gbc.fill = GridBagConstraints.BOTH;
    gbc.weightx = 1.0;
    gbc.weighty = 1.0;

    if (mode == Mode.MULTI) {
        if (player1 != null) Monk.addObjects(player1, this, layout, gbc, 0, 0, 1, 1);
        if (player2 != null) Monk.addObjects(player2, this, layout, gbc, 0, 1, 1, 1);
        if (player3 != null) Monk.addObjects(player3, this, layout, gbc, 0, 2, 1, 1);
    } else {
        if (player1 != null) Monk.addObjects(player1, this, layout, gbc, 0, 0, 1, 1);
    }

    revalidate();
    repaint();
}
}


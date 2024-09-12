package com.gooseplayer2.JPanels;

import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.border.*;
import com.gooseplayer2.Packages.Slugcat;
import javazoom.jl.decoder.JavaLayerException;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;

public class MusicPanel extends JPanel {
    private GridBagLayout layout;
    private GridBagConstraints gbc;
    private Border outline;
    private FilePanel filePanel;
    private MusicPlayer player1, player2, player3;

    public MusicPanel() throws UnsupportedAudioFileException, IOException, LineUnavailableException, JavaLayerException {

        layout = new GridBagLayout();
        gbc = new GridBagConstraints();
        setLayout(layout);

        filePanel = new FilePanel();

        outline = BorderFactory.createLineBorder(Color.BLACK);
        Slugcat Monk = new Slugcat();

        try {
            player1 = new MusicPlayer(1, filePanel);
            player2 = new MusicPlayer(2, filePanel);
            player3 = new MusicPlayer(3, filePanel);
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
                return false; // Allow the event to be dispatched to other listeners
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
                    System.out.println("All Players Play");
                    startAllPlayers();
                    break;
                case KeyEvent.VK_S:
                    System.out.println("All Players Pause");
                    pauseAllPlayers();
                    break;
            }
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


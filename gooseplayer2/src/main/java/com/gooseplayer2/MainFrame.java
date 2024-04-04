package com.gooseplayer2;

import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.border.*;
import com.gooseplayer2.JPanels.*;
import com.gooseplayer2.Packages.Slugcat;

import javazoom.jl.decoder.JavaLayerException;

import java.awt.*;
import java.io.IOException;


public class MainFrame extends JFrame {
    GridBagLayout mainLayout;
    GridBagConstraints gbc;
    Border outline;

    public MainFrame() throws UnsupportedAudioFileException, IOException, LineUnavailableException, JavaLayerException {
        super("musicPlayer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 550);

        outline = BorderFactory.createLineBorder(Color.black);

        mainLayout = new GridBagLayout();
        gbc = new GridBagConstraints();
        getContentPane().setLayout(mainLayout);

        Slugcat Survivor = new Slugcat();

        FilePanel filePanel = new FilePanel();

        MusicPanel musicPanel = new MusicPanel();
        musicPanel.setBorder(outline);
        
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        gbc.weightx = 2.0;

        Survivor.addObjects(musicPanel, this, mainLayout, gbc, 0, 0, 2, 1);

        gbc.weightx = 1.0;
        Survivor.addObjects(filePanel, this, mainLayout, gbc, 2, 0, 1, 1);

        setVisible(true);
    }
}
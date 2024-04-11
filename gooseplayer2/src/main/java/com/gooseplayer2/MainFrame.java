package com.gooseplayer2;

import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.border.*;
import com.gooseplayer2.JPanels.*;
import com.gooseplayer2.Packages.Slugcat;

import javazoom.jl.decoder.JavaLayerException;

import java.awt.*;
import java.awt.image.BufferedImage;
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

        ImageIcon icon = new ImageIcon(getClass().getResource("/icons/Icon.png"));
        Image image = icon.getImage();
        Image scaledImage = getScaledImage(image, 512, 547);


        setIconImage(scaledImage);

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

    private Image getScaledImage(Image srcImg, int width, int height) {
        BufferedImage resizedImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = resizedImg.createGraphics();

        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(srcImg, 0, 0, width,height, null);
        g2.dispose();

        return resizedImg;
    }
}
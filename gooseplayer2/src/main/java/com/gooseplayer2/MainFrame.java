package com.gooseplayer2;

import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.border.*;
import com.gooseplayer2.JPanels.*;
import com.gooseplayer2.Packages.Slugcat;

import javazoom.jl.decoder.JavaLayerException;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


public class MainFrame extends JFrame {
    GridBagLayout mainLayout;
    GridBagConstraints gbc;
    Border outline;
    InputStream is;
    private JToolBar toolBar;

    public MainFrame() throws UnsupportedAudioFileException, IOException, LineUnavailableException, JavaLayerException {
        super("musicPlayer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(950, 600);

        outline = BorderFactory.createLineBorder(Color.black);

        ImageIcon icon = new ImageIcon(getClass().getResource("/icons/Icon.png"));
        Image image = icon.getImage();
        Image scaledImage = getScaledImage(image, 512, 547);


        setIconImage(scaledImage);

        //Toolbar
        toolBar = new JToolBar();
        JButton settingsButton = new JButton("Settings");
        settingsButton.setFocusPainted(false);
        toolBar.add(settingsButton);

        settingsButton.addActionListener(e -> {
            // TODO: Add settings

        });

        JButton helpButton = new JButton("Help");
        helpButton.setFocusPainted(false);
        toolBar.add(helpButton);

        helpButton.addActionListener(e -> {
            try {
                InputStream is = getClass().getResourceAsStream("/help.txt");
                if (is == null) {
                    JOptionPane.showMessageDialog(null, "Help file not found.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder helpContent = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    helpContent.append(line).append("\n");
                }
                reader.close();
                JOptionPane.showMessageDialog(null, helpContent.toString(), "Help", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "Failed to load help content.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });


        mainLayout = new GridBagLayout();
        gbc = new GridBagConstraints();
        getContentPane().setLayout(mainLayout);

        Slugcat Survivor = new Slugcat();

        FilePanel filePanel = new FilePanel();

        MusicPanel musicPanel = new MusicPanel();
        musicPanel.setBorder(outline);

        //Toolbar
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        Survivor.addObjects(toolBar, this, mainLayout, gbc, 0, 0, GridBagConstraints.REMAINDER, 1);

        //Everything else
        
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        gbc.weightx = 2.0;

        Survivor.addObjects(musicPanel, this, mainLayout, gbc, 0, 1, 2, 1);

        gbc.weightx = 1.0;
        Survivor.addObjects(filePanel, this, mainLayout, gbc, 2, 1, 1, 1);

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
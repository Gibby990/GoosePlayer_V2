package com.gooseplayer2;

import java.io.IOException;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.SwingUtilities;
import javazoom.jl.decoder.JavaLayerException;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> { //TODO: Make Project run off of the JRE
            try {
                new MainFrame();
            } catch (UnsupportedAudioFileException | IOException | LineUnavailableException | JavaLayerException e) {
                e.printStackTrace();
            }
        });
    }
}
package com.gooseplayer2;

import java.io.IOException;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.SwingUtilities;
import javazoom.jl.decoder.JavaLayerException;

// My pride and joy of college programming.

// TODO: Implement discord bot avaliabe in Hotbar
// TODO: Create Terminal from hotbar that displayes Bot updates
// TODO: Settings option to output audio from system or bot

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> { 
            try {
                new MainFrame(); 
            } catch (UnsupportedAudioFileException | IOException | LineUnavailableException | JavaLayerException e) {
                e.printStackTrace();
            }
        });
    }
}
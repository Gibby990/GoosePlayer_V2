package com.gooseplayer2;

import java.io.IOException;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.SwingUtilities;
import javazoom.jl.decoder.JavaLayerException;

<<<<<<< HEAD
/* Report for 9/8

 * Features:
 * Created "Clear" Function for MonoPlayer
 * Made player pause/unpause when moving slider.
 * 
 * Bug Fixes:
 * load song pauses the sampleplayer 
 * removed all previous connections before calling a new sampleplayer in play.
 * Fixed rubber banding when moving time on the slider
 * Made player display song time when song is queued, instead of when the song is played.
 * 
 * Code Cleanup:
 *  MusicPlayer.java
 *      Removed unused imports
 *      Trimmed debugging sys logs (Mostly time related ones)
 *      Removed the following beads components as the audio was going straight to ac.out, leaving these default eq settings unused
 *          Gain, BiquadFilter, highPass/lowPass Compressor, Limiter
 *          I dont think ill readd this feature, it would be if someone wanted to define thier own EQ in the program which I dont see happening
 *      Removed n variable that was used to identify a player for debug logs. I never used it and took up code space.
 *      Removed newValue variable.
 * 
 */
=======
>>>>>>> a62df01127ee385f49f8b9e50bc4c094ea75a8e8

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
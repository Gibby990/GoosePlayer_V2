// src/test/java/com/gooseplayer2/JPanels/AudioPlayerSpy.java
package com.gooseplayer2.JPanels;

import net.beadsproject.beads.ugens.SamplePlayer;

import java.io.IOException;
import javax.sound.sampled.*;

import java.util.concurrent.atomic.AtomicInteger;

public class AudioPlayerSpy extends MusicPlayer {

    private final AtomicInteger playCount   = new AtomicInteger();
    private final AtomicInteger pauseCount  = new AtomicInteger();
    private final AtomicInteger skipCount   = new AtomicInteger();
    private final AtomicInteger removeCount = new AtomicInteger();
    private final AtomicInteger clearCount  = new AtomicInteger();
    private final AtomicInteger shuffleCount = new AtomicInteger();
    private final AtomicInteger stopCount   = new AtomicInteger();
    private boolean playing = false;

    public AudioPlayerSpy(boolean isMultichannel, String channelName)
            throws UnsupportedAudioFileException, IOException, LineUnavailableException {
        super(isMultichannel, channelName);  // ← CALL SUPER
    }

    @Override public void play(){ 
        playCount.incrementAndGet();
        super.play(); 
    }
    @Override public void pause(){ 
        pauseCount.incrementAndGet();
        super.pause(); 
    }
    @Override public void skip(){ 
        skipCount.incrementAndGet();
        super.skip(); 
    }
    @Override public void remove(){ 
        removeCount.incrementAndGet();
        super.remove(); 
    }
    @Override public void clear(){ 
        clearCount.incrementAndGet();  
        super.clear(); 
    }
    @Override public void shuffleQueue(){
        shuffleCount.incrementAndGet();
        super.shuffleQueue(); 
    }
    @Override public void stopAudio(){
        stopCount.incrementAndGet();   
        super.stopAudio(); 
    }
    
    // Getters
    public boolean isPlaying() { 
        return playing; 
    }
    public int getPlayCount(){ 
        return playCount.get(); 
    }
    public int getPauseCount(){ 
        return pauseCount.get(); 
    }
    public int getSkipCount(){ 
        return skipCount.get(); 
    }
    public int getRemoveCount(){ 
        return removeCount.get(); 
    }
    public int getClearCount(){ 
        return clearCount.get(); 
    }
    public int getShuffleCount(){ 
        return shuffleCount.get(); 
    }
    public int getStopCount(){ 
        return stopCount.get(); 
    }
    public double getCurrentPosition() {
        SamplePlayer player = getSamplePlayer();
        return player != null ? player.getPosition() : 0.0;
    }
    
}
package com.gooseplayer2.JPanels;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Iterator;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.sound.sampled.*;

import com.gooseplayer2.Packages.DropFileHandler;
import com.gooseplayer2.Packages.Queue;
import com.gooseplayer2.Packages.Slugcat;
import com.gooseplayer2.Packages.QueuedFile;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.UGen;
import net.beadsproject.beads.core.io.JavaSoundAudioIO;
import net.beadsproject.beads.data.Sample;
import net.beadsproject.beads.data.SampleManager;
import net.beadsproject.beads.ugens.*;

//TODO: Implement timestamp function

public class MusicPlayer extends JPanel {

    // UI Components
    private JButton Pause, Play, Remove, Skip;
    private GridBagConstraints gbc;
    private GridBagLayout layout;
    private JLabel ChannelLabel, TimeLabel, VolumeLabel;
    private JRadioButton Loop;
    private JSlider ProgressBar, VolumeSlider;
    private JTree queueTree;
    private DefaultMutableTreeNode root;

    // Timer
    private Timer updateTimeTimer;

    // Audio Playback
    private AudioContext ac;
    private boolean isPaused = false, isPlaying = false, songLoaded = false;
    private double pausePosition = 0;
    private float sampleRate, volume, lastVolume = 1.0f; 
    private int minutes, newValue = 0, seconds, n;
    private BiquadFilter highPass, lowPass;
    private Compressor limiter;
    private Compressor compressor;
    private JavaSoundAudioIO audioIO;
    private UGen gain;
    private long sampleFrames;
    private Sample sample, nextSample;
    private SamplePlayer sp, nextSp;

    // File Management
    private File selectedFile;
    private Queue<QueuedFile> Queue = new Queue<>();
    private QueuedFile queuedFile; 

    public MusicPlayer(JComponent FilePanel, boolean isMultichannel, String channelName) throws UnsupportedAudioFileException, IOException, LineUnavailableException {

        //JTree Stuff 
        
        layout = new GridBagLayout();
        gbc = new GridBagConstraints();

        root = new DefaultMutableTreeNode("Queue");
        queueTree = new JTree(root);
        queueTree.setRootVisible(true);

        JScrollPane queueTreePane = new JScrollPane(queueTree);

        //Audio Initialization and Configuration

        audioIO = new JavaSoundAudioIO();
        ac = new AudioContext(audioIO);

        gain = new Gain(ac, 1, 1);
        ac.out.addInput(gain);

        // High-pass filter
        highPass = new BiquadFilter(ac, 1, BiquadFilter.HP);
        highPass.setFrequency(20); 

        // Low-pass filter
        lowPass = new BiquadFilter(ac, 1, BiquadFilter.LP);
        lowPass.setFrequency(20000);

        // Compressor
        compressor = new Compressor(ac, 1);
        compressor.setThreshold(0.7f);
        compressor.setRatio(2f);

        // Limiter
        limiter = new Compressor(ac, 1);
        limiter.setThreshold(0.95f);
        limiter.setRatio(20f);

        // Chain setup
        ac.out.addInput(limiter);
        limiter.addInput(compressor);
        compressor.addInput(lowPass);
        lowPass.addInput(highPass);

        //Timer

        updateTimeTimer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isPlaying) {
                    updateTime(); 
                }
            }
        });

        Slugcat Rivulet = new Slugcat();

        //JComponents

        Play = new JButton("Play");
        Play.addActionListener(new PlayListener());

        Pause = new JButton("Pause");
        Pause.addActionListener( new PauseListener());

        Skip = new JButton("Skip");
        Skip.addActionListener(new SkipListener());

        Remove = new JButton("Remove");
        Remove.addActionListener(new RemoveListener());

        Loop = new JRadioButton("Loop");
        Loop.addActionListener(new LoopListener());

        ProgressBar = new JSlider(0, 0, 100, 0); 
        ProgressBar.addChangeListener(e -> {
            if (ProgressBar.getValueIsAdjusting()) { 
                newValue = ProgressBar.getValue();
                System.out.println("Slider new value: " + newValue); 
                seek(newValue);
            }
        });

        ChannelLabel = new JLabel(channelName);
        TimeLabel = new JLabel("0:00 / 0:00");   
        VolumeLabel = new JLabel("Volume (100)");

        VolumeSlider = new JSlider(0, 100, 100);
        VolumeSlider.addChangeListener(e -> {
            if (!VolumeSlider.getValueIsAdjusting()) {
                volume = VolumeSlider.getValue() / 100.0f; 
                setVolume(volume);
                updateCurrentVolume(volume);
            }
        });

        // Display

        setLayout(layout);

        if (isMultichannel) {
            gbc.gridheight = 3;
            gbc.gridwidth = 6;
            gbc.weightx = 1.0;
            gbc.weighty = 1.0;

            // Title

            gbc.fill = GridBagConstraints.CENTER;

            Rivulet.addObjects(ChannelLabel, this, layout, gbc, 0, 0, 4, 1);

            // JButtons

            gbc.fill = GridBagConstraints.NONE;

            Rivulet.addObjects(Play, this, layout, gbc, 4, 0, 1, 1);
            Rivulet.addObjects(Pause, this, layout, gbc,4, 1, 1, 1);
            Rivulet.addObjects(Skip, this, layout, gbc, 4, 2, 1, 1);
            Rivulet.addObjects(Remove, this, layout, gbc, 4, 3, 1, 1);
            Rivulet.addObjects(Loop, this, layout, gbc,4, 5, 1, 1);

            // Bars

            gbc.fill = GridBagConstraints.HORIZONTAL;

            gbc.insets = new Insets(0, 25, 0, 0);

            Rivulet.addObjects(ProgressBar, this, layout, gbc, 0, 0, 4, 4);
            Rivulet.addObjects(TimeLabel, this, layout, gbc, 0, 2, 2, 1);
            Rivulet.addObjects(VolumeLabel, this, layout, gbc, 0, 4, 2, 1);
            Rivulet.addObjects(VolumeSlider, this, layout, gbc, 0, 5, 1, 1);
        
            gbc.insets = new Insets(0, 0, 0, 0);

            // Queue

            gbc.fill = GridBagConstraints.BOTH;

            Rivulet.addObjects(queueTreePane, this, layout, gbc, 5, 0, 1,6);
        } else {

            gbc.gridheight = 3;
            gbc.gridwidth = 6;
            gbc.weightx = 1.0;
            gbc.weighty = 1.0;

            // Title

            gbc.fill = GridBagConstraints.CENTER;

            Rivulet.addObjects(ChannelLabel, this, layout, gbc, 0, 0, 4, 1);

            // Bars

            gbc.fill = GridBagConstraints.HORIZONTAL;

            gbc.insets = new Insets(0, 20, 0, 0);

            Rivulet.addObjects(ProgressBar, this, layout, gbc, 0, 1, 3, 1);
            Rivulet.addObjects(TimeLabel, this, layout, gbc, 3, 1, 1, 1);
            Rivulet.addObjects(VolumeLabel, this, layout, gbc, 0, 2, 1, 1);
            Rivulet.addObjects(VolumeSlider, this, layout, gbc, 1, 2, 2, 1);

            gbc.insets = new Insets(0, 0, 0, 0);

            // JButtons

            gbc.fill = GridBagConstraints.NONE;

            Rivulet.addObjects(Play, this, layout, gbc, 0, 3, 1, 1);
            Rivulet.addObjects(Pause, this, layout, gbc,1, 3, 1, 1);
            Rivulet.addObjects(Skip, this, layout, gbc, 2, 3, 1, 1);
            Rivulet.addObjects(Remove, this, layout, gbc, 3, 3, 1, 1);
            Rivulet.addObjects(Loop, this, layout, gbc,4, 3, 1, 1);

            // Queue 

            gbc.fill = GridBagConstraints.BOTH;

            Rivulet.addObjects(queueTreePane, this, layout, gbc, 0, 4, 5, 1);

        }

        queueTree.setTransferHandler(new DropFileHandler(this, FilePanel));

    }

    // Action Events

    private class PlayListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            play();
        }
    }

    private class PauseListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (isPlaying) {
                pause();
            }
        }   
    }  
    
    private class SkipListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            skip();
        }
    }

    private class RemoveListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            remove();
        }
    }

    private class LoopListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (sp!= null) {
                if(Loop.isSelected()) {
                    sp.setLoopType(SamplePlayer.LoopType.LOOP_FORWARDS);
                } else {
                    sp.setLoopType(SamplePlayer.LoopType.NO_LOOP_FORWARDS);
                }
            }
        }
    }

    // Other methods

    private void loadSong() throws IOException, UnsupportedAudioFileException, LineUnavailableException {
        System.out.println("loadSong ran in player " + n);
        queuedFile = Queue.peek();
        if (queuedFile == null) return;
    
        selectedFile = queuedFile.getFile();
    
        if (!selectedFile.exists() || !selectedFile.canRead()) {
            throw new IOException("File not found or cannot be read: " + selectedFile.getAbsolutePath());
        }
    
        try {
            sample = SampleManager.sample(selectedFile.getAbsolutePath());
            if (sample == null) {
                throw new IOException("Failed to load sample from file: " + selectedFile.getAbsolutePath());
            }
    
            sampleFrames = sample.getNumFrames();
            sampleRate = sample.getSampleRate();
            if (sampleRate == 0) {
                throw new IllegalArgumentException("Sample rate is zero for file: " + selectedFile.getAbsolutePath());
            }
    
            float duration = sampleFrames / sampleRate;
            minutes = (int) (duration / 60);
            seconds = (int) (duration % 60);
    
            updateTime();
            SwingUtilities.invokeLater(() -> {
                int totalDuration = minutes * 60 + seconds;
                ProgressBar.setMaximum(totalDuration);
                ProgressBar.setValue(0);
            });
    
            songLoaded = true;
            System.out.println("Song loaded: " + selectedFile.getName());
    
            sp = new SamplePlayer(ac, sample);
            sp.setKillOnEnd(false);
            ac.out.addInput(sp);

        } catch (Exception e) {
            System.err.println("ERROR: Unable to load the selected file: " + selectedFile.getAbsolutePath());
            e.printStackTrace();
            songLoaded = false;
            throw e; 
        }
    }

    public void play() {
        if (!isPlaying && !Queue.isEmpty()) {
            try {
                if (!songLoaded) {
                    loadSong();
                }
                if (!ac.isRunning()) {
                    ac.start();
                }
                
                int retries = 3;
                while (retries > 0) {
                    try {
                        sp.start();
                        break;
                    } catch (NullPointerException e) {
                        System.err.println("Error starting playback. Retrying... (" + retries + " attempts left)");
                        retries--;
                        if (retries == 0) {
                            throw e;
                        }
                        Thread.sleep(1000); 
                    }
                }

                updateTimeTimer.start();
                isPlaying = true;
                isPaused = false;
                SwingUtilities.invokeLater(() -> System.out.println("Playback started"));
                preloadNextSong();
            } catch (Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> System.out.println("Playback failed"));
                resetCurrentSongData();
            }
        } else if (isPaused) {
            resume();
        }
    }

    public void pause() {
        if (sp != null && isPlaying) {
            System.out.println("Pause pressed at player " + n);
            pausePosition = sp.getPosition();  
            sp.pause(true);
            updateTimeTimer.stop(); 
            isPaused = true;
            isPlaying = false;
        }
    }

    private void resume() {
        if (sp != null && isPaused) {
            sp.start();  
            sp.setPosition(pausePosition); 
            updateTimeTimer.start();  
            isPaused = false;
            isPlaying = true;
        }
    }

    private void skip() {
        if(songLoaded == false) return;
        if (Loop.isSelected()) {
            seek(0);
            return;
        }

        stopCurrentPlayback();

        if (Queue.size() > 1) {
            Queue.dequeue();
            refreshQueueInJTree();
            try {
                loadSong();
                play();
            } catch (Exception e) {
                System.err.println("Error loading next song: " + e.getMessage());
                e.printStackTrace();
                handleSkipFailure();
            }
        } else {
            Queue.dequeue();
            refreshQueueInJTree();
            resetCurrentSongData();
            System.out.println("No more tracks in queue.");
        }
    }

    private void stopCurrentPlayback() {
        if (sp != null) {
            sp.pause(true);
            ac.out.removeAllConnections(sp);
        }
        isPlaying = false;
        updateTimeTimer.stop();
    }

    private void handleSkipFailure() {
        resetCurrentSongData();
        if (!Queue.isEmpty()) {
            Queue.dequeue();
        }
        refreshQueueInJTree();
        System.out.println("Failed to load next track. Skipping to the next one if available.");
        if (!Queue.isEmpty()) {
            skip();
        }
    }

    
    private void remove() {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) queueTree.getLastSelectedPathComponent();
        if (selectedNode != null && selectedNode != root) {
            QueuedFile topQueuedFile = Queue.peek();
            if (topQueuedFile != null && selectedNode.getUserObject().toString().equals(topQueuedFile.getFile().getName())) {
                System.out.println("Skipping removal of the first node in the queue.");
            } else {
                Iterator<QueuedFile> fivePebbles = Queue.iterator();
                while (fivePebbles.hasNext()) {
                    QueuedFile file = fivePebbles.next();
                    if (file.getFile().getName().equals(selectedNode.getUserObject().toString())) {
                        Queue.remove(file);
                        break;
                    }
                }
            }
        }
        refreshQueueInJTree();
    }

    // Other methods

    private void preloadNextSong() {
        if (Queue.size() > 1) {
            new Thread(() -> {
                try {
                    QueuedFile nextFile = Queue.get(1);
                    nextSample = SampleManager.sample(nextFile.getFile().getAbsolutePath());
                    nextSp = new SamplePlayer(ac, nextSample);
                    nextSp.setKillOnEnd(false);
                    if (Loop.isSelected()) {
                        nextSp.setLoopType(SamplePlayer.LoopType.LOOP_FORWARDS);
                    } else {
                        nextSp.setLoopType(SamplePlayer.LoopType.NO_LOOP_FORWARDS);
                    }
                    
                    if (!ac.isRunning()) {
                        ac.start();
                    }
                    
                    System.out.println("Next song preloaded: " + nextFile.getFile().getName());
                } catch (Exception e) {
                    System.err.println("Error preloading next song: " + e.getMessage());
                    e.printStackTrace();
                    nextSample = null;
                    nextSp = null;
                }
            }).start();
        } else {
            nextSample = null;
            nextSp = null;
        }
    }

    private void transitionToNextTrack() {
        if (Loop.isSelected()) {
            seek(0);
            return;
        }

        if (Queue.isEmpty()) {
            resetCurrentSongData();
            isPlaying = false;
            updateTimeTimer.stop();
            System.out.println("Playback finished. No more tracks in queue.");
            return;
        }

        try {
            if (sp != null) {
                ac.out.removeAllConnections(sp);
                sp.kill();
            }

            Queue.dequeue();

            if (nextSp != null) {
                sp = nextSp;
                sample = nextSample;
                songLoaded = true;
                ac.out.addInput(sp);
                updateSongInfo();
                sp.start();
                preloadNextSong();
            } else {
                resetCurrentSongData();
                songLoaded = false;
                loadSong();
                play();
            }

            refreshQueueInJTree();
            System.out.println("Transitioned to next track. Queue updated.");
        } catch (Exception e) {
            System.err.println("Error during track transition: " + e.getMessage());
            e.printStackTrace();
            resetCurrentSongData();
        }
    }

    private void updateSongInfo() {
        if (sample != null) {
            sampleFrames = sample.getNumFrames();
            sampleRate = sample.getSampleRate();
            float duration = sampleFrames / sampleRate;
            minutes = (int) (duration / 60);
            seconds = (int) (duration % 60);
            updateTime();
            SwingUtilities.invokeLater(() -> {
                int totalDuration = minutes * 60 + seconds;
                ProgressBar.setMaximum(totalDuration);
                ProgressBar.setValue(0);
            });
        }
    }

    private void seek(int seconds) {
        System.out.println("Seek method fired at Player " + n);
        if (sp != null) {
            sp.setPosition(seconds * 1000); 
            updateTime();
        }
    }

    private void updateTime() {
        if (sp != null && isPlaying) {
            double currentPositionInMilliseconds = sp.getPosition();
            int currentPositionInSeconds = (int) (currentPositionInMilliseconds / 1000.0);
            int currentMinutes = currentPositionInSeconds / 60;
            int currentSeconds = currentPositionInSeconds % 60;
    
            System.out.println("Instance " + n + " - updateTime() called. Current position: " + currentPositionInSeconds + " seconds.");
    
            SwingUtilities.invokeLater(() -> { 
                TimeLabel.setText(String.format("%d:%02d / %d:%02d", currentMinutes, currentSeconds, minutes, seconds));
                ProgressBar.setValue(currentPositionInSeconds);
            });
    
            if (currentPositionInSeconds >= ProgressBar.getMaximum()) {
                System.out.println("Instance " + n + " - End of track reached. Transitioning to next.");
                handleEndOfPlayback();
            }
        } else {
            System.out.println("Instance " + n + " - updateTime() called but player is null or not playing.");
        }
    }

    private void handleEndOfPlayback() {
        if (Queue.size() > 1) {
            System.out.println("Instance " + n + " - Transitioning to next track.");
            transitionToNextTrack();
        } else {
            System.out.println("Instance " + n + " - No more tracks in queue. Stopping playback.");
            resetCurrentSongData();
            isPlaying = false;
            updateTimeTimer.stop();
            SwingUtilities.invokeLater(() -> {
                TimeLabel.setText("0:00 / 0:00");
                ProgressBar.setValue(0);
            });
            if (!Queue.isEmpty()) {
                Queue.dequeue(); 
            }
            refreshQueueInJTree();
        }
    }

    
    private void updateCurrentVolume(float currentVolume) {
        if (currentVolume == 1.0) {
            SwingUtilities.invokeLater(() -> {
                VolumeLabel.setText("Volume (" + Math.round(currentVolume * 100) + ")");
            });
        } else if (currentVolume < 0.1) {
            VolumeLabel.setText("Volume (" + Math.round(currentVolume * 100) + ")    ");
        } else {
            SwingUtilities.invokeLater(() -> {
                VolumeLabel.setText("Volume (" + Math.round(currentVolume * 100) + ")  ");
            });
        }
    }

    public void resetCurrentSongData() {
        if (isPlaying || isPaused) { //For me to keep track of
            if (sp != null) {
                sp.pause(true);
                ac.out.removeAllConnections(sp);
            }
            updateTimeTimer.stop(); 
            isPlaying = false;
            isPaused = false;
        }
        songLoaded = false; 
        selectedFile = null; 
        pausePosition = 0;  
        sample = null;  
        sp = null;  
        sampleFrames = 0;  
        sampleRate = 0; 
        minutes = 0; 
        seconds = 0; 
    
        SwingUtilities.invokeLater(() -> {
            ProgressBar.setValue(0);
            ProgressBar.setMaximum(100);  
            TimeLabel.setText("0:00 / 0:00");
        });
    }

    private void setVolume(float volume) {
        if (sp != null) {
            ac.out.setGain(volume);
        }
    }

    public void addFilesToTree(java.util.List<File> files) {
        boolean wasEmpty = Queue.isEmpty();
        for (File file : files) {
            if (file.isDirectory()) {
                addFilesFromDirectory(file);
            } else {
                if (isAudioFile(file)) {
                    Queue.enqueue(new QueuedFile(file));
                }
            }
        }
        refreshQueueInJTree();
        
        if (wasEmpty && !Queue.isEmpty() && !isPlaying) {
            try {
                loadSong();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (isPlaying) {
            preloadNextSong();
        }
    }

    private void addFilesFromDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    addFilesFromDirectory(file); 
                } else {
                    if (isAudioFile(file)) {
                        Queue.enqueue(new QueuedFile(file));
                    }
                }
            }
        }
    }

    private boolean isAudioFile(File file) {
        String fileName = file.getName().toLowerCase();
        return fileName.endsWith(".mp3") || fileName.endsWith(".wav") || fileName.endsWith(".flac");
    }

    public void refreshQueueInJTree() {
        root.removeAllChildren(); 
        Iterator<QueuedFile> LTTM = Queue.iterator();
        boolean isFirst = true;
        while (LTTM.hasNext()) {
            QueuedFile file = LTTM.next();
            DefaultMutableTreeNode fileNode = new DefaultMutableTreeNode(file.getFile().getName());
            root.add(fileNode);
            
            if (isFirst) {
                queueTree.setCellRenderer(new DefaultTreeCellRenderer() {
                    @Override
                    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                        JLabel label = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
                        if (value.toString().equals(file.getFile().getName())) {
                            label.setFont(label.getFont().deriveFont(Font.BOLD));
                        } else {
                            label.setFont(label.getFont().deriveFont(Font.PLAIN));
                        }
                        return label;
                    }
                });
                isFirst = false;
            }
        }
        ((DefaultTreeModel) queueTree.getModel()).reload();
    }

    public void mute() {
        if (sp != null) {
            lastVolume = ac.out.getGain();
            ac.out.setGain(0);
            updateCurrentVolume(0);
            SwingUtilities.invokeLater(() -> VolumeSlider.setValue(0));

        }
    }

    public void unmute() {
        if (sp != null) {
            ac.out.setGain(lastVolume);
            updateCurrentVolume(lastVolume);
            SwingUtilities.invokeLater(() -> VolumeSlider.setValue((int)(lastVolume * 100)));
        }
    }

    /* 
     * EVERYTHING BELOW IS FOR SINGLE PLAYER ONLY
     */
}

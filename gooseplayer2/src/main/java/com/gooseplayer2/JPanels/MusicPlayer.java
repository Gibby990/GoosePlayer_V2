package com.gooseplayer2.JPanels;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Iterator;
import javax.swing.*;
//import javax.swing.event.ChangeEvent;
//import javax.swing.event.ChangeListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.sound.sampled.*;

import com.gooseplayer2.Packages.DropFileHandler;
import com.gooseplayer2.Packages.Queue;
import com.gooseplayer2.Packages.Slugcat;
import com.gooseplayer2.Packages.QueuedFile;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.io.JavaSoundAudioIO;
import net.beadsproject.beads.data.Sample;
import net.beadsproject.beads.data.SampleManager;
import net.beadsproject.beads.ugens.*;



public class MusicPlayer extends JPanel {

    // UI Components
    private JButton Empty, Pause, Play, Remove, Skip;
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
    private float sampleRate, volume;
    private int elapsedSeconds, minutes, newValue = 0, seconds, n;
    private JavaSoundAudioIO audioIO;
    private long sampleFrames;
    private Sample sample;
    private SamplePlayer sp;

    // File Management
    private File selectedFile;
    private Queue<QueuedFile> Queue = new Queue<>();
    private QueuedFile queuedFile;

    //TODO: Fix the issue of playing 2 players makes one skip songs.

    public MusicPlayer(int n, JComponent FilePanel) throws UnsupportedAudioFileException, IOException, LineUnavailableException {

        //JTree Stuff
        
        layout = new GridBagLayout();
        gbc = new GridBagConstraints();

        root = new DefaultMutableTreeNode("Queue");
        queueTree = new JTree(root);
        queueTree.setRootVisible(true);

        JScrollPane queueTreePane = new JScrollPane(queueTree);

        this.n = n;

        //Audio Initialization and Configuration

        audioIO = new JavaSoundAudioIO();
        ac = new AudioContext(audioIO);

        //Timer

        updateTimeTimer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isPlaying) {
                    elapsedSeconds++;  // Increment the elapsed time
                    System.out.println("Elapsed Seconds: " + elapsedSeconds);
                    updateTime();  // Update the UI and check for track end
                }
            }
        });
                
        //GUI

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

        Empty = new JButton("Empty");
        Empty.addActionListener(new EmptyListener());

        Loop = new JRadioButton("Loop");
        Loop.addActionListener(new LoopListener());

        
        ProgressBar = new JSlider(0, 0, 100, 0);  //TODO: find a better slider
        ProgressBar.addChangeListener(e -> {
            if (ProgressBar.getValueIsAdjusting()) { 
                newValue = ProgressBar.getValue();
                System.out.println("Slider new value: " + newValue); 
                elapsedSeconds = newValue;
                seek(newValue);
            }
        });

        ChannelLabel = new JLabel("Channel " + n);
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

        gbc.gridheight = 3;
        gbc.gridwidth = 6;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.NONE;

        // JButtons

        Rivulet.addObjects(Play, this, layout, gbc, 4, 0, 1, 1);
        Rivulet.addObjects(Pause, this, layout, gbc,4, 1, 1, 1);
        Rivulet.addObjects(Skip, this, layout, gbc, 4, 2, 1, 1);
        Rivulet.addObjects(Remove, this, layout, gbc, 4, 3, 1, 1);
        //Rivulet.addObjects(Empty, this, layout, gbc, 4, 4, 1, 1);
        //TODO: Implement Empty
        Rivulet.addObjects(Loop, this, layout, gbc,4, 5, 1, 1);

        // ProgressBar

        gbc.fill = GridBagConstraints.CENTER;

        Rivulet.addObjects(ChannelLabel, this, layout, gbc, 0, 0, 4, 1);

        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.insets = new Insets(0, 25, 0, 0);

        Rivulet.addObjects(ProgressBar, this, layout, gbc, 0, 0, 4, 4);
        Rivulet.addObjects(TimeLabel, this, layout, gbc, 0, 2, 2, 1);
        Rivulet.addObjects(VolumeLabel, this, layout, gbc, 0, 4, 2, 1);
        Rivulet.addObjects(VolumeSlider, this, layout, gbc, 0, 5, 1, 1);
        
        gbc.insets = new Insets(0, 0, 0, 0);

        // queueTree

        gbc.fill = GridBagConstraints.BOTH;

        Rivulet.addObjects(queueTreePane, this, layout, gbc, 5, 0, 1,6);

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

    private class EmptyListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
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
        System.out.println("loadSong ran in player" + n);
        queuedFile = Queue.peek();
        if (queuedFile == null) return;
    
        selectedFile = queuedFile.getFile();
    
        if (!selectedFile.exists()) {
            throw new FileNotFoundException("File not found: " + selectedFile.getAbsolutePath());
        }
        if (!selectedFile.canRead()) {
            throw new IOException("Cannot read file: " + selectedFile.getAbsolutePath());
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
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("ERROR: Unable to load the selected file: " + selectedFile.getAbsolutePath());
            e.printStackTrace();
            songLoaded = false;
        }
    }

    public void play() {
        if (!isPlaying && !Queue.isEmpty()) {
            try {
                if (!songLoaded) {
                    loadSong();  // Load the song if not already loaded
                }
                if (sp == null) {
                    sp = new SamplePlayer(ac, sample);  // Initialize the SamplePlayer with the loaded sample
                    ac.out.addInput(sp);  // Add the SamplePlayer to the audio context output
                }
                ac.start();  // Start the audio context
                updateTimeTimer.start();  // Start the timer to update UI and handle time-related logic
                isPlaying = true;
                isPaused = false;
                SwingUtilities.invokeLater(() -> System.out.println("Playback started"));
            } catch (Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> System.out.println("Playback failed"));
            }
        } else if (isPaused) {
            resume();  // If the player is paused, resume playback
        }
    }

    private void pause() {
        if (sp != null && isPlaying) {
            System.out.println("Pause pressed at player " + n);
            pausePosition = sp.getPosition();  // Save the current position to resume later
            ac.stop();  // Stop the audio context to pause playback
            updateTimeTimer.stop();  // Stop the timer since we're no longer playing
            isPaused = true;
            isPlaying = false;
        }
    }

    private void resume() {
        if (sp != null && isPaused) {
            ac.start();  // Start the audio context again
            sp.setPosition(pausePosition);  // Resume from the paused position
            updateTimeTimer.start();  // Ensure the timer is running
            isPaused = false;
            isPlaying = true;
        }
    }

    private void skip() {
        try {
            if (Loop.isSelected()) {
                seek(0);
            } else {
                if (!Queue.isEmpty()) {
                    if (sp != null) {
                        sp.pause(true);
                        ac.out.removeAllConnections(sp);
                        sp.kill();
                        sp = null;
                    }
                    ac.stop();
                    
                    resetCurrentSongData();
                    
                    Queue.dequeue();
                    songLoaded = false;

                    new Timer(500, new ActionListener() {
                        public void actionPerformed(ActionEvent evt) {
                            play();
                            ((Timer)evt.getSource()).stop(); 
                        }
                    }).start();

                    refreshQueueInJTree();
                    System.out.println("Track skipped. Queue updated.");
                    
                }
            }
        } catch (Exception e) {
            System.err.println("Error during skipping track: " + e.getMessage());
            resetCurrentSongData();
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

    private void seek(int seconds) {
        System.out.println("Seek method fired at Player " + n);
        if (sp != null) {
            sp.setPosition(seconds * 1000); 
            updateTime();
        }
    }
    

    public void adjustBufferSize(AudioContext ac, int newSize) {
        if (ac.getBufferSize() != newSize) {
            ac.stop();
            //ac.setBufferSize(newSize);
            ac.start();
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
                System.out.println("Instance " + n + " - End of track reached. Skipping to next.");
                skip(); 
            }
        } else {
            System.out.println("Instance " + n + " - updateTime() called but player is null or not playing.");
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
        if (isPlaying || isPaused) {
            ac.stop();  // Stop the audio context
            updateTimeTimer.stop();  // Stop the timer
            isPlaying = false;
            isPaused = false;
        }
        songLoaded = false;  // Mark the song as not loaded
        selectedFile = null;  // Clear the selected file
        pausePosition = 0;  // Reset the pause position
        sample = null;  // Clear the sample
        sp = null;  // Clear the SamplePlayer
        sampleFrames = 0;  // Reset sample frames
        sampleRate = 0;  // Reset sample rate
        minutes = 0;  // Reset minutes
        seconds = 0;  // Reset seconds
        elapsedSeconds = 0;  // Reset elapsed seconds
    
        SwingUtilities.invokeLater(() -> {
            ProgressBar.setValue(0);
            ProgressBar.setMaximum(100);  // Reset the progress bar
            TimeLabel.setText("0:00 / 0:00");  // Reset the time label
        });
    }

    public void addFilesToTree(java.util.List<File> files) {
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
    }

    private void setVolume(float volume) {
        if (sp != null) {
            ac.out.setGain(volume);
        }
    }

    public void setPosition(double position) {
        System.out.println("Setting position to: " + position + " in instance " + n);
        sp.setPosition(position);
    }
    
    public double getPosition() {
        double position = sp.getPosition();
        System.out.println("Retrieved position: " + position + " from instance " + n);
        return position;
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
        while (LTTM.hasNext()) {
            QueuedFile file = LTTM.next();
            DefaultMutableTreeNode fileNode = new DefaultMutableTreeNode(file.getFile().getName());
            root.add(fileNode);
        }
        ((DefaultTreeModel) queueTree.getModel()).reload();
    }
}


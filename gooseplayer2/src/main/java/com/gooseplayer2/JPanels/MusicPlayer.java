package com.gooseplayer2.JPanels;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import com.gooseplayer2.Packages.DropFileHandler;
import com.gooseplayer2.Packages.Slugcat;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.data.Sample;
import net.beadsproject.beads.data.SampleManager;
import net.beadsproject.beads.ugens.SamplePlayer;


import com.gooseplayer2.Packages.Queue;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Iterator;

import javax.sound.sampled.*;



public class MusicPlayer extends JPanel {

    private static final String LIBRARY_PATH = System.getProperty("user.dir") + File.separator + "Library";

    JTree fileTree;
    DefaultMutableTreeNode root;

    GridBagLayout layout;
    GridBagConstraints gbc;
    Border outline;

    int currentFrameIndex, minutes, seconds, elapsedSeconds;
    String folderDestination, currentTime, endTime, currentSong, playStatus, loopStatus;
    boolean isPlaying = false , isPaused = false, songLoaded = false;
    double pausePosition = 0;
    float sampleRate, Duration;
    long sampleFrames;


    JButton Play, Pause, Skip, Remove, Empty;
    JRadioButton Loop;
    JSlider Progressbar;
    JLabel CurrentlyPlayingLabel, StatusLabel, TimeLabel;
    private Timer Timer, updateTimeTimer;

    AudioInputStream audioInputStream;
    AudioInputStream decodedStream;
    AudioFormat decodedFormat;

    File selectedFile;

    AudioContext ac = new AudioContext(); // Init here because I dont trust any code below this line.
    SamplePlayer sp;
    Sample sample;

    FileInputStream fis;
    BufferedInputStream bis;

    Queue<File> Queue = new Queue<>();
    Iterator<File> LTTM = Queue.iterator();

    public MusicPlayer(int n, JComponent FilePanel) throws UnsupportedAudioFileException, IOException, LineUnavailableException {

        //JTree Stuff
        
        layout = new GridBagLayout();
        gbc = new GridBagConstraints();

        root = new DefaultMutableTreeNode("Queue");
        fileTree = new JTree(root);
        fileTree.setBorder(outline);
        fileTree.setRootVisible(true);

        //Initializing everything else

        Slugcat Rivulet = new Slugcat();
        
        outline = BorderFactory.createLineBorder(Color.black);

        Timer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateTime();
            }
        });

        // Initialize the Timer to update every second (1000 milliseconds)
        updateTimeTimer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateTime();
                elapsedSeconds++;
            }
        });

        //JComponents

        Play = new JButton("Play");
        Play.addActionListener(new PlayListener());

        Pause = new JButton("Pause");
        Pause.addActionListener( new PauseListener());

        Skip = new JButton("Skip");
        Skip.addActionListener(new SkipListener());

        Remove = new JButton("Remove");

        Empty = new JButton("Empty");
        Empty.addActionListener(new EmptyListener());

        Loop = new JRadioButton("Loop");

        Progressbar = new JSlider(0, 0, 100, 0);

        CurrentlyPlayingLabel = new JLabel("Currently playing : ");
        StatusLabel = new JLabel("Status: STOPPED");
        TimeLabel = new JLabel("Time: 0:00 / 0:00");

        // Display

        setLayout(layout);

        gbc.gridheight = 3;
        gbc.gridwidth = 6;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;

        Rivulet.addObjects(fileTree, this, layout, gbc, 5, 0, 1,6);

        gbc.fill = GridBagConstraints.NONE;

        Rivulet.addObjects(Play, this, layout, gbc, 4, 0, 1, 1);
        Rivulet.addObjects(Pause, this, layout, gbc,4, 1, 1, 1);
        //Rivulet.addObjects(Skip, this, layout, gbc, 4, 2, 1, 1);
        //Rivulet.addObjects(Remove, this, layout, gbc, 4, 3, 1, 1);
        //Rivulet.addObjects(Empty, this, layout, gbc, 4, 4, 1, 1);
        //Rivulet.addObjects(Loop, this, layout, gbc,4, 5, 1, 1);

        Rivulet.addObjects(Progressbar, this, layout, gbc, 2, 1, 2, 3);

        gbc.fill = GridBagConstraints.BOTH;

        Rivulet.addObjects(CurrentlyPlayingLabel, this, layout, gbc, 0, 0, 2, 1);
        Rivulet.addObjects(StatusLabel, this, layout, gbc, 0, 1, 2, 1);
        Rivulet.addObjects(TimeLabel, this, layout, gbc, 0, 2, 2, 1);


        fileTree.setTransferHandler(new DropFileHandler(this, FilePanel));

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
            //if (player != null) {
            //    player.stop();
            //}
            //Queue.dequeue();
            //playAction();
        }
    }

    private class EmptyListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
        }
    }

    // Other methods
    
    public void play() {
        if (!isPlaying && !Queue.isEmpty()) {
            try {
                if (!songLoaded) {
                    loadSong();
                }
                if (sp == null) {
                    sp = new SamplePlayer(ac, sample);
                    ac.out.addInput(sp);
                }

                new Thread(() -> {
                    try {
                        songLoaded = true;
                        isPlaying = true;
                        isPaused = false;
                        updateStatus("PLAYING");
                        updateCurrentlyPlaying(selectedFile.getName());
                        ac.start();
                        Timer.start();
                        startUpdateTimeTimer(); // Start the timer when playback starts
                        System.out.println("Playback successful");
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        updateStatus("ERROR: Unable to play the selected file.");
                        System.out.println("Playback failed in thread");
                    }
                }).start();
            } catch (Exception ex) {
                ex.printStackTrace();
                updateStatus("ERROR: Unable to play the selected file.");
                System.out.println("Playback failed");
            }
        } else if (isPaused) {
            resume();
        } else {
            updateStatus("Queue is already playing or empty.");
        }
    }

    private void pause() {
        if (sp != null) {
            pausePosition = sp.getPosition();
            ac.stop();
            Timer.stop();
            stopUpdateTimeTimer(); // Stop the timer when playback is paused
            isPaused = true;
            isPlaying = false;
            updateStatus("Paused");
        }
    }

    private void resume() {
        if (sp != null && isPaused) {
            ac.start();
            sp.setPosition(pausePosition);
            isPaused = false;
            isPlaying = true;
            updateStatus("Playing");
        }
    }
    
    private void loadSong() throws IOException {
        selectedFile = Queue.peek();

        File fileInLibrary = new File(LIBRARY_PATH, selectedFile.getName());

        System.out.println("Attempting to play file: " + fileInLibrary.getAbsolutePath());
        if (!fileInLibrary.canRead()) {
            throw new IOException("Cannot read file: " + fileInLibrary.getAbsolutePath());
        }
        if (!fileInLibrary.exists()) {
            throw new FileNotFoundException("File not found: " + selectedFile.getAbsolutePath());
        }

        try {
            sample = SampleManager.sample(fileInLibrary.getAbsolutePath());
            songLoaded = true;
            System.out.println("Song loaded: " + fileInLibrary.getName());


            sampleFrames = sample.getNumFrames();
            sampleRate = sample.getSampleRate();

            float duration = sampleFrames / sampleRate;
            minutes = (int) (duration / 60);
            seconds = (int) (duration % 60);

            updateTime();

            pausePosition = 0;

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("ERROR: Unable to load the selected file.");
        }

        songLoaded = true;
        System.out.println("Song loaded");
    }

    private void updateStatus(String message) {
        SwingUtilities.invokeLater(() -> {
            StatusLabel.setText("Status: " + message);
        });
    }

    private void updateTime() {
        if (sp != null && isPlaying) {
            int currentMinutes = elapsedSeconds / 60;
            int currentSeconds = elapsedSeconds % 60;
            SwingUtilities.invokeLater(() -> {
                TimeLabel.setText(String.format("Time: " + "%d:%02d / %d:%02d", currentMinutes, currentSeconds, minutes, seconds));
            });
        }
    }
    
    private void updateCurrentlyPlaying(String songName) {
        SwingUtilities.invokeLater(() -> {
            CurrentlyPlayingLabel.setText("Currently playing: " + songName);
        });
    }

    private void startUpdateTimeTimer() {
        updateTimeTimer.start();
    }

    private void stopUpdateTimeTimer() {
        updateTimeTimer.stop();
    }



    public void addFilesToTree(java.util.List<File> files) {
        for (File file : files) {
            Queue.enqueue(file); 
        }
        refreshQueueInJTree(); 
    }

    public void refreshQueueInJTree() {
        root.removeAllChildren(); 
        Iterator<File> LTTM = Queue.iterator();
        while (LTTM.hasNext()) {
            File file = LTTM.next();
            DefaultMutableTreeNode fileNode = new DefaultMutableTreeNode(file.getName());
            root.add(fileNode);
        }
        ((DefaultTreeModel) fileTree.getModel()).reload();
    }
}

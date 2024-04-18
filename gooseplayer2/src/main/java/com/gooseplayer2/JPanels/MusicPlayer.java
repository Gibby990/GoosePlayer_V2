package com.gooseplayer2.JPanels;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Iterator;
import javax.swing.*;
import javax.swing.border.*;
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
import net.beadsproject.beads.data.Sample;
import net.beadsproject.beads.data.SampleManager;
import net.beadsproject.beads.ugens.SamplePlayer;

public class MusicPlayer extends JPanel {

    // Constants
    private static final String LIBRARY_PATH = System.getProperty("user.dir") + File.separator + "Library";

    // UI Components
    private JTree fileTree;
    private DefaultMutableTreeNode root;
    private JButton Play, Pause, Skip, Empty;
    private JSlider ProgressBar;
    private JLabel CurrentlyPlayingLabel, StatusLabel, TimeLabel;
    private GridBagLayout layout;
    private GridBagConstraints gbc;
    private Border outline;

    // Timer
    private Timer Timer, updateTimeTimer;

    // Audio Playback
    private AudioContext ac = new AudioContext(); // Initialize here for consistiency.
    private SamplePlayer sp;
    private Sample sample;
    private boolean isPlaying = false, isPaused = false, songLoaded = false;
    private double pausePosition = 0;
    private float sampleRate;
    private long sampleFrames;
    private int minutes, seconds, elapsedSeconds, oldValue = 0;

    // File Management
    private File selectedFile;
    private Queue<QueuedFile> Queue = new Queue<>();


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


        updateTimeTimer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isPlaying) {
                    elapsedSeconds++; // Ensure this increments by 1 every second
                    System.out.println("Elapsed Seconds: " + elapsedSeconds);
                    updateTime();
                }
            }
        });

        //JComponents

        Play = new JButton("Play");
        Play.addActionListener(new PlayListener());

        Pause = new JButton("Pause");
        Pause.addActionListener( new PauseListener());

        Skip = new JButton("Skip");
        Skip.addActionListener(new SkipListener());

        //Remove = new JButton("Remove");

        Empty = new JButton("Empty");
        Empty.addActionListener(new EmptyListener());
        //Loop = new JRadioButton("Loop");

        ProgressBar = new JSlider(0, 0, 100, 0);  
        ProgressBar.addChangeListener(e -> {
            if (ProgressBar.getValueIsAdjusting()) {
                oldValue = ProgressBar.getValue();
                System.out.println("Slider new value: " + oldValue);
                elapsedSeconds = oldValue;
                seek(oldValue);
            }
        });

        CurrentlyPlayingLabel = new JLabel("Currently playing : ");
        StatusLabel = new JLabel("Status: STOPPED");
        TimeLabel = new JLabel("0:00                                         0:00");

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
        Rivulet.addObjects(Skip, this, layout, gbc, 4, 2, 1, 1);
        //Rivulet.addObjects(Remove, this, layout, gbc, 4, 3, 1, 1);
        //Rivulet.addObjects(Empty, this, layout, gbc, 4, 4, 1, 1);
        //Rivulet.addObjects(Loop, this, layout, gbc,4, 5, 1, 1);

        Rivulet.addObjects(ProgressBar, this, layout, gbc, 2, 1, 2, 3);
        Rivulet.addObjects(TimeLabel, this, layout, gbc, 2, 2, 2, 1);


        gbc.fill = GridBagConstraints.BOTH;

        Rivulet.addObjects(CurrentlyPlayingLabel, this, layout, gbc, 0, 0, 2, 1);
        Rivulet.addObjects(StatusLabel, this, layout, gbc, 0, 1, 2, 1);


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
            skip();
        }
    }

    private class EmptyListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
        }
    }
    // Other methods

    private void loadSong() throws IOException {
        QueuedFile queuedFile = Queue.peek();
        if(queuedFile == null) return;

        selectedFile = queuedFile.getFile();

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
            SwingUtilities.invokeLater(() -> {
                int totalDuration = minutes * 60 + seconds;
                ProgressBar.setMaximum(totalDuration);
                ProgressBar.setValue(0);
            });

            pausePosition = 0;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("ERROR: Unable to load the selected file.");
        }

        songLoaded = true;
        System.out.println("Song loaded");
    }

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
                        startUpdateTimer(); 
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
            stopUpdateTimer(); 
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

    private void skip() {
        if (!Queue.isEmpty()) {
            ac.stop();
            if (sp != null) {
                sp.pause(true);
                sp = null;
            }
            Timer.stop();
            stopUpdateTimer();
            isPlaying = false;
            isPaused = false;
            
            Queue.dequeue();
            songLoaded = false;
            
            if (!Queue.isEmpty()) {
                play(); 
            } else {
                resetCurrentSongData();
                updateStatus("Queue is empty.");
            }
        } else {
            updateStatus("Queue is empty.");
        }
        refreshQueueInJTree();
    }
    
    private void seek(int seconds) {
        if (sp != null) {
            sp.setPosition(seconds * 1000); 
            updateTime();
        }
    }
    

    private void updateStatus(String message) {
        SwingUtilities.invokeLater(() -> {
            StatusLabel.setText("Status: " + message);
        });
    }

    private void updateTime() {
        if (sp != null && isPlaying) {
            double currentPositionInMilliseconds = sp.getPosition();
            int currentPositionInSeconds = (int) (currentPositionInMilliseconds / 1000.0);
            int currentMinutes = currentPositionInSeconds / 60;
            int currentSeconds = currentPositionInSeconds % 60;
    
            SwingUtilities.invokeLater(() -> {
                TimeLabel.setText(String.format("%d:%02d                                         %d:%02d", currentMinutes, currentSeconds, minutes, seconds));
                ProgressBar.setValue(currentPositionInSeconds);
            });
        }
    }

    public void resetCurrentSongData() {
        if (isPlaying || isPaused) {
            ac.stop();
            Timer.stop();
            stopUpdateTimer();
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
        elapsedSeconds = 0;
        SwingUtilities.invokeLater(() -> {
            ProgressBar.setValue(0);
            ProgressBar.setMaximum(100); 
            CurrentlyPlayingLabel.setText("Currently playing: ");
            StatusLabel.setText("Status: STOPPED");
            TimeLabel.setText("0:00                                         0:00");
        });
        updateStatus("Queue is empty");
    }
    
    private void updateCurrentlyPlaying(String songName) {
        SwingUtilities.invokeLater(() -> {
            CurrentlyPlayingLabel.setText("Currently playing: " + songName);
        });
    }

    private void startUpdateTimer() {
        updateTimeTimer.start();
    }

    private void stopUpdateTimer() {
        updateTimeTimer.stop();
    }


    public void addFilesToTree(java.util.List<File> files) {
        for (File file : files) {
            Queue.enqueue(new QueuedFile(file)); 
        }
        refreshQueueInJTree(); 
    }

    public void refreshQueueInJTree() {
        root.removeAllChildren(); 
        Iterator<QueuedFile> LTTM = Queue.iterator();
        while (LTTM.hasNext()) {
            QueuedFile file = LTTM.next();
            DefaultMutableTreeNode fileNode = new DefaultMutableTreeNode(file.getFile().getName());
            root.add(fileNode);
        }
        ((DefaultTreeModel) fileTree.getModel()).reload();
    }
}


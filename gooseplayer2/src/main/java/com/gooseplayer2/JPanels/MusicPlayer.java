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
import net.beadsproject.beads.ugens.*;


public class MusicPlayer extends JPanel {

    // Constants
    private static final String LIBRARY_PATH = System.getProperty("user.dir") + File.separator + "Library";

    // UI Components
    private JTree queueTree;
    private DefaultMutableTreeNode root;
    private JButton Play, Pause, Skip, Empty;
    private JSlider ProgressBar;
    private JLabel TimeLabel, ChannelLabel;
    private JRadioButton Loop;
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
    private int minutes, seconds, elapsedSeconds, newValue = 0;

    // File Management
    private File selectedFile;
    private Queue<QueuedFile> Queue = new Queue<>();


    public MusicPlayer(int n, JComponent FilePanel) throws UnsupportedAudioFileException, IOException, LineUnavailableException {

        //JTree Stuff
        
        layout = new GridBagLayout();
        gbc = new GridBagConstraints();

        root = new DefaultMutableTreeNode("Queue");
        queueTree = new JTree(root);
        queueTree.setRootVisible(true);

        JScrollPane queueTreePane = new JScrollPane(queueTree);

        //Initializing everything else

        Slugcat Rivulet = new Slugcat();
        
        outline = BorderFactory.createLineBorder(Color.black);
        queueTree.setBorder(outline);

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
                    elapsedSeconds++;
                    System.out.println("Elapsed Seconds: " + elapsedSeconds);
                    updateTime();
                }
            }
        });

        //TODO: Implement instance AudioIO for each AudioContext isntead of using default
        // This might be causing all the issues of clipping when skipping songs or playing 2 players at once.

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

        Loop = new JRadioButton("Loop");
        Loop.addActionListener(new LoopListener());

        ProgressBar = new JSlider(0, 0, 100, 0);  //TODO: find a better slider
        ProgressBar.addChangeListener(e -> {
            if (ProgressBar.getValueIsAdjusting()) { //TODO: Reset Timer when song ends.
                newValue = ProgressBar.getValue();
                System.out.println("Slider new value: " + newValue); 
                elapsedSeconds = newValue;
                seek(newValue);
            }
        });

        TimeLabel = new JLabel("0:00 / 0:00");
        ChannelLabel = new JLabel("Channel " + n);

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
        //Rivulet.addObjects(Remove, this, layout, gbc, 4, 3, 1, 1);
        // TODO: Implement Remove
        //Rivulet.addObjects(Empty, this, layout, gbc, 4, 4, 1, 1);
        //TODO: Implement Empty
        Rivulet.addObjects(Loop, this, layout, gbc,4, 5, 1, 1);

        // ProgressBar

        gbc.fill = GridBagConstraints.CENTER;

        Rivulet.addObjects(ChannelLabel, this, layout, gbc, 0, 0, 4, 1);

        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.insets = new Insets(0, 25, 0, 0);
        Rivulet.addObjects(ProgressBar, this, layout, gbc, 0, 1, 4, 4);

        Rivulet.addObjects(TimeLabel, this, layout, gbc, 2, 2, 2, 1);
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
                        ac.start();
                        Timer.start();
                        startUpdateTimer(); 
                        System.out.println("Playback successful");
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        System.out.println("Playback failed in thread");
                    }
                }).start();
            } catch (Exception ex) {
                ex.printStackTrace();
                System.out.println("Playback failed");
            }
        } else if (isPaused) {
            resume();
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
        }
    }

    private void resume() {
        if (sp != null && isPaused) {
            ac.start();
            sp.setPosition(pausePosition);
            isPaused = false;
            isPlaying = true;
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
            }
        } else {
        }
        refreshQueueInJTree();
    }
    
    private void seek(int seconds) {
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
    
            SwingUtilities.invokeLater(() -> { 
                TimeLabel.setText(String.format("%d:%02d / %d:%02d", currentMinutes, currentSeconds, minutes, seconds));
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
        songLoaded = false;  //Im just going to pretend this mess does not exist and is isolated to this one method alone.
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
            TimeLabel.setText("0:00 / 0:00");
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
            //TODO: Check for if file is formatted for audio.
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
        ((DefaultTreeModel) queueTree.getModel()).reload();
    }
}


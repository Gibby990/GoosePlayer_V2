package com.gooseplayer2.JPanels;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import com.gooseplayer2.Packages.DropFileHandler;
import com.gooseplayer2.Packages.Slugcat;
import com.gooseplayer2.Packages.Queue;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Iterator;

import javax.sound.sampled.*;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.advanced.AdvancedPlayer;
import javazoom.jl.player.advanced.PlaybackEvent;
import javazoom.jl.player.advanced.PlaybackListener;

public class MusicPlayer extends JPanel {

    private static final String LIBRARY_PATH = System.getProperty("user.dir") + File.separator + "Library";

    JTree fileTree;
    DefaultMutableTreeNode root;

    GridBagLayout layout;
    GridBagConstraints gbc;
    Border outline;

    int currentFrameIndex;
    String folderDestination, currentTime, endTime, currentSong, playStatus, loopStatus;
    boolean isPlaying = false , isPaused = false, songLoaded = false;

    JButton Play, Pause, Skip, Remove, Empty;
    JRadioButton Loop;
    JSlider Progressbar;
    JLabel CurrentlyPlayingLabel, StatusLabel;

    AudioInputStream audioInputStream;
    AdvancedPlayer player;
    File selectedFile;

    FileInputStream fis;
    BufferedInputStream bis;

    Queue<File> Queue = new Queue<>();
    Iterator<File> LTTM = Queue.iterator();

    public MusicPlayer(int n, JComponent FilePanel) throws UnsupportedAudioFileException, IOException, LineUnavailableException, JavaLayerException  {

        //JTree Stuff
        
        layout = new GridBagLayout();
        gbc = new GridBagConstraints();

        root = new DefaultMutableTreeNode("Queue");
        fileTree = new JTree(root);
        fileTree.setBorder(outline);
        fileTree.setRootVisible(true);

        //Initializing everthing else

        Slugcat Rivulet = new Slugcat();

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

        outline = BorderFactory.createLineBorder(Color.black);

        Progressbar = new JSlider(0, 0, 100, 0);

        CurrentlyPlayingLabel = new JLabel("Currently playing : ");
        StatusLabel = new JLabel("Status: STOPPED");

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
        Rivulet.addObjects(Remove, this, layout, gbc, 4, 3, 1, 1);
        Rivulet.addObjects(Empty, this, layout, gbc, 4, 4, 1, 1);
        Rivulet.addObjects(Loop, this, layout, gbc,4, 5, 1, 1);

        Rivulet.addObjects(Progressbar, this, layout, gbc, 2, 1, 2, 3);

        gbc.fill = GridBagConstraints.BOTH;

        Rivulet.addObjects(CurrentlyPlayingLabel, this, layout, gbc, 0, 0, 2, 1);
        Rivulet.addObjects(StatusLabel, this, layout, gbc, 0, 1, 2, 1);


        fileTree.setTransferHandler(new DropFileHandler(this, FilePanel));

    }

    // Action Events


    private class PlayListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            playAction();
        }
    }

    private class PauseListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (player != null && isPlaying) {
                player.stop();
                isPaused = true;
                isPlaying = false;
                updateStatus("PAUSED");
            }
        }   
    }  
    
    private class SkipListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (player != null) {
                player.stop();
            }
            Queue.dequeue();
            playAction();
        }
    }

    private class EmptyListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            Queue.empty();
            updateStatus("Queue has been cleared");
            refreshQueueInJTree();
        }
    }

    // Other methods
    
    public void playAction() {
        if (!isPlaying && !Queue.isEmpty()) {
            try {
                if (!songLoaded) {
                    loadSong();
                }
                new Thread(() -> {
                    try {
                        songLoaded = true;
                        isPlaying = true;
                        isPaused = false;
                        updateStatus("PLAYING");
                        updateCurrentlyPlaying(selectedFile.getName());
                        player.play(currentFrameIndex, Integer.MAX_VALUE);
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
            System.out.println("Playback paused");
        } else {
            updateStatus("Queue is already playing or empty.");
        }
    }
    
    private void loadSong() throws JavaLayerException, IOException {
        selectedFile = Queue.peek();

        File fileInLibrary = new File(LIBRARY_PATH, selectedFile.getName());

        System.out.println("Attempting to play file: " + fileInLibrary.getAbsolutePath());
        if (!fileInLibrary.canRead()) {
            throw new IOException("Cannot read file: " + fileInLibrary.getAbsolutePath());
        }
        fis = new FileInputStream(fileInLibrary);
        bis = new BufferedInputStream(fis);

        if (!fileInLibrary.exists()) {
            throw new FileNotFoundException("File not found: " + selectedFile.getAbsolutePath());
        }
    
        player = new AdvancedPlayer(bis);
        player.setPlayBackListener(new PlaybackListener() {
            @Override
            public void playbackFinished(PlaybackEvent event) {
                isPlaying = false;
                songLoaded = false; 
                currentFrameIndex = 0; 
                updateStatus("STOPPED");
            }
        });
        songLoaded = true;
        System.out.println("Song loaded");
    }


    private void updateStatus(String message) {
        SwingUtilities.invokeLater(() -> {
            StatusLabel.setText("Status: " + message);
        });
    }
    
    private void updateCurrentlyPlaying(String songName) {
        SwingUtilities.invokeLater(() -> {
            CurrentlyPlayingLabel.setText("Currently playing: " + songName);
        });
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

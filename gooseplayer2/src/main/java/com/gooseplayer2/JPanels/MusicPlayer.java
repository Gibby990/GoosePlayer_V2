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
import java.nio.file.*;
import java.util.Iterator;

import javax.sound.sampled.*;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.advanced.AdvancedPlayer;
import javazoom.jl.player.advanced.PlaybackEvent;
import javazoom.jl.player.advanced.PlaybackListener;

public class MusicPlayer extends JPanel {
    JTree fileTree;
    DefaultMutableTreeNode root;
    private File tempDirectory;

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

    Clip clip;
    AudioInputStream audioInputStream;
    AdvancedPlayer player;
    File selectedFile;

    FileInputStream fis;
    BufferedInputStream bis;

    Queue<File> Queue = new Queue<>();
    Iterator<File> LTTM = Queue.iterator();

    public MusicPlayer(int n) throws UnsupportedAudioFileException, IOException, LineUnavailableException, JavaLayerException  {

        //JTree Stuff

        String MainDir = (
            "gooseplayer2" + File.separator + "src" + File.separator + "main" + File.separator + "java" +
            File.separator + "com" + File.separator + "gooseplayer2" + File.separator + "Library"
        );
        
        layout = new GridBagLayout();
        gbc = new GridBagConstraints();

        tempDirectory = Paths.get(MainDir).toFile();
        folderDestination = tempDirectory.getAbsolutePath();

        DefaultMutableTreeNode top = new DefaultMutableTreeNode(folderDestination);

        root = new DefaultMutableTreeNode("Queue");
        fileTree = new JTree(root);
        fileTree.setBorder(outline);
        fileTree.setRootVisible(true);

        createQueue(top, Paths.get(folderDestination));

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
        Loop = new JRadioButton("Loop");

        outline = BorderFactory.createLineBorder(Color.black);

        Progressbar = new JSlider(0, 0, 100, 0);

        CurrentlyPlayingLabel = new JLabel("Currently playing : PLACEHOLDER"); // Ill add methods for these later
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


        fileTree.setTransferHandler(new DropFileHandler(this));

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

    // Other methods

    //Play Method

    public void playAction() {
        if (!isPlaying && !Queue.isEmpty()) {
            try {
                if (!songLoaded) {
                    loadSong();
                }
                // Assuming you manage currentFrameIndex to resume from the last position
                new Thread(() -> {
                    try {
                        isPlaying = true;
                        isPaused = false;
                        updateStatus("PLAYING");
                        updateCurrentlyPlaying(selectedFile.getName());
                        // Start playing from the current frame index
                        player.play(currentFrameIndex, Integer.MAX_VALUE);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        updateStatus("ERROR: Unable to play the selected file.");
                    }
                }).start();
            } catch (Exception ex) {
                ex.printStackTrace();
                updateStatus("ERROR: Unable to play the selected file.");
            }
        } else if (isPaused) {
            // Resume playing if paused
            playAction();
        } else {
            updateStatus("Queue is already playing or empty.");
        }
    }
    
    private void loadSong() throws FileNotFoundException, JavaLayerException {
        selectedFile = Queue.peek();
        fis = new FileInputStream(selectedFile);
        bis = new BufferedInputStream(fis);
    
        player = new AdvancedPlayer(bis);
        player.setPlayBackListener(new PlaybackListener() {
            @Override
            public void playbackFinished(PlaybackEvent event) {
                isPlaying = false;
                songLoaded = false; // Ensure the song is reloaded next time
                currentFrameIndex = 0; // Reset frame index
                updateStatus("STOPPED");
            }
        });
        songLoaded = true;
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
            DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(file.getName());
            root.add(newNode);
            Queue.enqueue(file);
        }
        ((DefaultTreeModel)fileTree.getModel()).reload();
    }

    private void createQueue(DefaultMutableTreeNode node, Path folderPath) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folderPath)) {
            for (Path entry : stream) {
                if (Files.isRegularFile(entry)) {
                    DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(entry.getFileName().toString());
                    node.add(newNode);
                    Queue.enqueue(entry.toFile());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        ((DefaultTreeModel) fileTree.getModel()).reload(); // Refresh the tree
    }
}

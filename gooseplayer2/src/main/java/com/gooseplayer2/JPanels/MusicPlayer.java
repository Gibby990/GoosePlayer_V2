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

public class MusicPlayer extends JPanel {
    JTree fileTree;
    DefaultMutableTreeNode root;
    private File tempDirectory;

    GridBagLayout layout;
    GridBagConstraints gbc;
    Border outline;

    String folderDestination, currentTime, endTime, currentSong, playStatus, loopStatus;
    boolean isPlaying = false;

    JButton Play, Pause, Skip, Remove, Empty;
    JRadioButton Loop;
    JSlider Progressbar;
    JLabel CurrentlyPlayingLabel, StatusLabel;

    Clip clip;
    AudioInputStream audioInputStream;
    File selectedFile;

    Queue<File> Queue = new Queue<>();
    Iterator<File> LTTM = Queue.iterator();

    public MusicPlayer(int n) throws UnsupportedAudioFileException, IOException, LineUnavailableException  {

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
        Skip = new JButton("Skip");
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

        clip = AudioSystem.getClip();

    }

    // Action Events


    private class PlayListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (!isPlaying && !Queue.isEmpty()) {
                try {
                    selectedFile = Queue.dequeue();   
                    audioInputStream = AudioSystem.getAudioInputStream(selectedFile);
                    clip.open(audioInputStream);
                    clip.start();

                    isPlaying = true;
                    updateStatus("PLAYING: " + selectedFile.getName());
                } catch (IOException | UnsupportedAudioFileException | LineUnavailableException ex) {
                    ex.printStackTrace();
                    updateStatus("ERROR: Unable to play the selected file.");
                }
            } else {
                updateStatus("Queue is empty or already playing.");
            }
        }
    }


    // Other methods

    private void updateStatus(String message) {
        StatusLabel.setText("Status: " + message);
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

package com.gooseplayer2.JPanels;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Iterator;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
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

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;


public class MusicPlayer extends JPanel {

	// UI Components
	private JButton PlayPause, Remove, Skip, Clear, Shuffle;
    private GridBagConstraints gbc;
    private GridBagLayout layout;
    private JLabel ChannelLabel, TimeLabel, VolumeLabel;
	private JLabel AlbumArtLabel;
	private JRadioButton LoopSong, LoopPlaylist;
    private JSlider ProgressBar, VolumeSlider;
	private JTree queueTree;
	private DefaultMutableTreeNode root;

    // Timer
    private Timer updateTimeTimer;

    // Audio Playback
    private AudioContext ac;
    private boolean isPaused = false, isPlaying = false, songLoaded = false;
    private boolean isSeeking = false, wasPlayingBeforeSeek = false;
    private boolean shouldRestorePausePosition = false;
    private double pausePosition = 0;
    private float sampleRate, volume, lastVolume = 1.0f; 
    private int minutes, seconds;
    private JavaSoundAudioIO audioIO;
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

        PlayPause = new JButton("Play");
        PlayPause.addActionListener(new PlayPauseListener());

        Skip = new JButton("Skip");
        Skip.addActionListener(new SkipListener());

        Remove = new JButton("Remove");
        Remove.addActionListener(new RemoveListener());

		Clear = new JButton("Clear");
        Clear.addActionListener(new ClearListener());

		Shuffle = new JButton("Shuffle");
		Shuffle.addActionListener(new ShuffleListener());

		LoopSong = new JRadioButton("Loop Song");
		LoopSong.addActionListener(new LoopListener());
		LoopPlaylist = new JRadioButton("Loop Playlist");
		JPanel loopPanel = new JPanel(new GridLayout(2,1));
		loopPanel.add(LoopSong);
		loopPanel.add(LoopPlaylist);

        ProgressBar = new JSlider(0, 0, 100, 0); 
        ProgressBar.addChangeListener(e -> {
            boolean adjusting = ProgressBar.getValueIsAdjusting();
            int value = ProgressBar.getValue();
            if (adjusting) {
                if (!isSeeking) {
                    wasPlayingBeforeSeek = isPlaying;
                    if (isPlaying) {
                        pause();
                    }
                    isSeeking = true;
                }
                int previewMinutes = value / 60;
                int previewSeconds = value % 60;
                SwingUtilities.invokeLater(() -> {
                    TimeLabel.setText(String.format("%d:%02d / %d:%02d", previewMinutes, previewSeconds, minutes, seconds));
                });
            } else if (isSeeking) {
                seek(value);
                if (wasPlayingBeforeSeek) {
                    shouldRestorePausePosition = false;
                    resume();
                }
                isSeeking = false;
                wasPlayingBeforeSeek = false;
            }
        });

        ChannelLabel = new JLabel(channelName);
        TimeLabel = new JLabel("0:00 / 0:00");   
        VolumeLabel = new JLabel("Volume (100)");
        AlbumArtLabel = new JLabel();
        AlbumArtLabel.setHorizontalAlignment(SwingConstants.CENTER);
        AlbumArtLabel.setVerticalAlignment(SwingConstants.CENTER);
        AlbumArtLabel.setPreferredSize(new Dimension(128, 128));
        setDefaultAlbumArt();

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

            Rivulet.addObjects(PlayPause, this, layout, gbc, 4, 0, 1, 1);
            Rivulet.addObjects(Skip, this, layout, gbc, 4, 1, 1, 1);
			Rivulet.addObjects(Remove, this, layout, gbc, 4, 2, 1, 1);
			Rivulet.addObjects(loopPanel, this, layout, gbc,4, 4, 1, 1);

            // Bars

            gbc.fill = GridBagConstraints.HORIZONTAL;

            gbc.insets = new Insets(0, 25, 0, 0);

            Rivulet.addObjects(ProgressBar, this, layout, gbc, 0, 0, 4, 4);
            Rivulet.addObjects(TimeLabel, this, layout, gbc, 0, 1, 2, 1);
            Rivulet.addObjects(VolumeLabel, this, layout, gbc, 0, 3, 2, 1);
            Rivulet.addObjects(VolumeSlider, this, layout, gbc, 0, 4, 1, 1);
        
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

            // Progress and Time
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(0, 20, 0, 0); 
            Rivulet.addObjects(ProgressBar, this, layout, gbc, 0, 1, 4, 1);
            Rivulet.addObjects(TimeLabel, this, layout, gbc, 4, 1, 1, 1);

            // Volume Control
            Rivulet.addObjects(VolumeSlider, this, layout, gbc, 0, 2, 2, 1);

            // Album Art
            gbc.fill = GridBagConstraints.NONE;
            Rivulet.addObjects(AlbumArtLabel, this, layout, gbc, 5, 0, 1, 3);

            // Control Buttons
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.weightx = 1.0;
            gbc.insets = new Insets(0, 20, 0, 5);
            Rivulet.addObjects(PlayPause, this, layout, gbc, 0, 3, 1, 1);
            
            gbc.anchor = GridBagConstraints.EAST;
            Rivulet.addObjects(Skip, this, layout, gbc, 1, 3, 1, 1);

			gbc.fill = GridBagConstraints.NONE;
			gbc.anchor = GridBagConstraints.CENTER;
			Rivulet.addObjects(Shuffle, this, layout, gbc, 2, 3, 1, 1);
			Rivulet.addObjects(Remove, this, layout, gbc, 3, 3, 1, 1);
			Rivulet.addObjects(Clear, this, layout, gbc, 4, 3, 1, 1);
			Rivulet.addObjects(loopPanel, this, layout, gbc, 5, 3, 1, 1);

            // Queue
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets(0, 0, 0, 0);
            Rivulet.addObjects(queueTreePane, this, layout, gbc, 0, 4, 6, 1);
        }

        queueTree.setTransferHandler(new DropFileHandler(this, FilePanel));

    }

    // Action Events

    private class PlayPauseListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (isPlaying) {
                pause();
            } else {
                play();
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

    private class ClearListener implements ActionListener {
        @Override 
        public void actionPerformed(ActionEvent e) {
            clear();
        }
    }

	private class ShuffleListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			shuffleQueue();
		}
	}

    private class LoopListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (sp!= null) {
                if(LoopSong.isSelected()) {
                    sp.setLoopType(SamplePlayer.LoopType.LOOP_FORWARDS);
                } else {
                    sp.setLoopType(SamplePlayer.LoopType.NO_LOOP_FORWARDS);
                }
            }
        }
    }

    // Other methods

    private void loadSong() throws IOException, UnsupportedAudioFileException, LineUnavailableException {
        System.out.println("loadSong ran");
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
    
            SwingUtilities.invokeLater(() -> {
                int totalDuration = minutes * 60 + seconds;
                ProgressBar.setMaximum(totalDuration);
                ProgressBar.setValue(0);
                TimeLabel.setText(String.format("0:00 / %d:%02d", minutes, seconds));
            });
    
            songLoaded = true;
            System.out.println("Song loaded: " + selectedFile.getName());
    
            sp = new SamplePlayer(ac, sample);
            sp.setKillOnEnd(false);
            sp.pause(true); 

            updateAlbumArt(selectedFile);

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
                
                if (sp != null) {
                    ac.out.removeAllConnections(sp);
                    ac.out.addInput(sp);
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
                updatePlayPauseButtonLabel();
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
            System.out.println("Pause pressed");
            pausePosition = sp.getPosition();  
            sp.pause(true);
            updateTimeTimer.stop(); 
            isPaused = true;
            isPlaying = false;
            shouldRestorePausePosition = true;
            updatePlayPauseButtonLabel();
        }
    }

    private void resume() {
        if (sp != null && isPaused) {
            sp.start();  
            if (shouldRestorePausePosition) {
                sp.setPosition(pausePosition);
            }
            updateTimeTimer.start();  
            isPaused = false;
            isPlaying = true;
            shouldRestorePausePosition = false;
            updatePlayPauseButtonLabel();
        }
    }

    private void skip() {
        if(songLoaded == false) return;
        if (LoopSong.isSelected()) {
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
        } else if (Queue.size() == 1) {
            Queue.dequeue();
            if (LoopPlaylist.isSelected()) {
                java.util.List<QueuedFile> newUpcoming = new java.util.ArrayList<>(Queue.getHistory());
                Queue.empty();
                Queue.clearHistory();
                for (QueuedFile qf : newUpcoming) Queue.enqueue(qf);
                refreshQueueInJTree();
                try {
                    loadSong();
                    play();
                } catch (Exception e) {
                    System.err.println("Error restarting playlist after skip: " + e.getMessage());
                    e.printStackTrace();
                    handleSkipFailure();
                }
            } else {
                refreshQueueInJTree();
                resetCurrentSongData();
                System.out.println("No more tracks in queue.");
            }
        } else {
            // Already empty
            refreshQueueInJTree();
            resetCurrentSongData();
            System.out.println("No more tracks in queue.");
        }
    }

    private void clear() {
        stopCurrentPlayback();

        Queue.empty();
        Queue.clearHistory();

        resetCurrentSongData();
        refreshQueueInJTree();
        
        System.out.println("Queue cleared");
    }

	private void shuffleQueue() {
		if (Queue.size() <= 1) return;
		Queue.shuffle();
		refreshQueueInJTree();
		if (isPlaying) {
			preloadNextSong();
		}
		System.out.println("Queue shuffled");
	}

    private void stopCurrentPlayback() {
        if (sp != null) {
            sp.pause(true);
            ac.out.removeAllConnections(sp);
        }
        isPlaying = false;
        updateTimeTimer.stop();
        updatePlayPauseButtonLabel();
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
            String selectedName = selectedNode.getUserObject().toString();
            int renderedIndex = -1;
            for (int i = 0; i < root.getChildCount(); i++) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) root.getChildAt(i);
                if (selectedName.equals(node.getUserObject().toString())) {
                    renderedIndex = i;
                    break;
                }
            }
            if (renderedIndex >= 0) {
                int historyCount = Queue.getHistory().size();
                if (renderedIndex < historyCount) {
                    boolean ok = Queue.removeHistoryAt(renderedIndex);
                    if (!ok) System.out.println("Failed to remove history entry at index " + renderedIndex);
                } else {
                    int upcomingIndex = renderedIndex - historyCount;
                    if (upcomingIndex == 0) {
                        System.out.println("Skipping removal of the first node in the queue.");
                        refreshQueueInJTree();
                        return;
                    }
                    Iterator<QueuedFile> it = Queue.iterator();
                    int idx = 0;
                    while (it.hasNext()) {
                        QueuedFile qf = it.next();
                        if (idx == upcomingIndex) {
                            if (getDisplayName(qf.getFile()).equals(selectedName)) {
                                Queue.remove(qf);
                            }
                            break;
                        }
                        idx++;
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
                    if (LoopSong.isSelected()) {
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
        if (LoopSong.isSelected()) {
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
                if (!Queue.isEmpty()) {
                    QueuedFile currentFile = Queue.peek();
                    if (currentFile != null) {
                        selectedFile = currentFile.getFile();
                        updateAlbumArt(selectedFile);
                    }
                }
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
        System.out.println("Seek method fired");
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
    
            System.out.println("updateTime() called. Current position: " + currentPositionInSeconds + " seconds.");
    
            SwingUtilities.invokeLater(() -> { 
                TimeLabel.setText(String.format("%d:%02d / %d:%02d", currentMinutes, currentSeconds, minutes, seconds));
                ProgressBar.setValue(currentPositionInSeconds);
            });
    
            if (currentPositionInSeconds >= ProgressBar.getMaximum()) {
                System.out.println("End of track reached. Transitioning to next.");
                handleEndOfPlayback();
            }
        } else {
            System.out.println("updateTime() called but player is null or not playing.");
        }
    }

    private void handleEndOfPlayback() {
        if (Queue.size() > 1) {
            System.out.println("Transitioning to next track.");
            transitionToNextTrack();
        } else {
            if (LoopPlaylist.isSelected()) {
                stopCurrentPlayback();

                java.util.List<QueuedFile> newUpcoming = new java.util.ArrayList<>(Queue.getHistory());

                java.util.List<QueuedFile> remaining = new java.util.ArrayList<>();
                java.util.Iterator<QueuedFile> it = Queue.iterator();
                while (it.hasNext()) remaining.add(it.next());

                Queue.empty();
                Queue.clearHistory();

                for (QueuedFile qf : newUpcoming) Queue.enqueue(qf);
                for (QueuedFile qf : remaining) Queue.enqueue(qf);

                System.out.println("Loop Playlist: restarting from beginning.");
                try {
                    loadSong();
                    play();
                } catch (Exception e) {
                    e.printStackTrace();
                    handleSkipFailure();
                }
            } else {
                System.out.println("No more tracks in queue. Stopping playback.");
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
            VolumeLabel.setText("Volume (100)");
            if (AlbumArtLabel != null) {
                setDefaultAlbumArt();
            }
        });
        updatePlayPauseButtonLabel();
    }

    private void updateAlbumArt(File audioFile) {
        if (audioFile == null) {
            return;
        }
        new Thread(() -> {
            try {
                AudioFile af = AudioFileIO.read(audioFile);
                Tag tag = af.getTag();
                if (tag != null) {
                    Artwork artwork = tag.getFirstArtwork();
                    if (artwork != null) {
                        byte[] imageData = artwork.getBinaryData();
                        if (imageData != null && imageData.length > 0) {
                            ImageIcon icon = new ImageIcon(imageData);
                            Image scaled = icon.getImage().getScaledInstance(128, 128, Image.SCALE_SMOOTH);
                            SwingUtilities.invokeLater(() -> AlbumArtLabel.setIcon(new ImageIcon(scaled)));
                            return;
                        }
                    }
                }
            } catch (Exception ignored) {
            }
            SwingUtilities.invokeLater(this::setDefaultAlbumArt);
        }).start();
    }

    private void setDefaultAlbumArt() {
        try {
            java.net.URL url = getClass().getResource("/icons/albumMissing.png");
            if (url != null) {
                ImageIcon icon = new ImageIcon(url);
                Image scaled = icon.getImage().getScaledInstance(128, 128, Image.SCALE_SMOOTH);
                AlbumArtLabel.setIcon(new ImageIcon(scaled));
            } else {
                AlbumArtLabel.setIcon(null);
            }
        } catch (Exception e) {
            AlbumArtLabel.setIcon(null);
        }
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

	private String getDisplayName(File file) {
		String name = file.getName();
		int lastDot = name.lastIndexOf('.')
			;
		return (lastDot > 0) ? name.substring(0, lastDot) : name;
	}

    public void refreshQueueInJTree() {
        root.removeAllChildren();

        java.util.List<QueuedFile> history = Queue.getHistory();
        for (QueuedFile qf : history) {
            root.add(new DefaultMutableTreeNode(getDisplayName(qf.getFile())));
        }

        Iterator<QueuedFile> iterator = Queue.iterator();
        boolean isFirstUpcoming = true;
        String firstUpcomingName = null;
        while (iterator.hasNext()) {
            QueuedFile file = iterator.next();
            String display = getDisplayName(file.getFile());
            DefaultMutableTreeNode fileNode = new DefaultMutableTreeNode(display);
            root.add(fileNode);
            if (isFirstUpcoming) {
                firstUpcomingName = display;
                isFirstUpcoming = false;
            }
        }

        final String highlightName = firstUpcomingName;
        queueTree.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                JLabel label = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
                if (highlightName != null && value.toString().equals(highlightName)) {
                    label.setFont(label.getFont().deriveFont(Font.BOLD));
                } else {
                    label.setFont(label.getFont().deriveFont(Font.PLAIN));
                }
                return label;
            }
        });

        ((DefaultTreeModel) queueTree.getModel()).reload();
        queueTree.expandPath(new TreePath(root.getPath()));
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

    private void updatePlayPauseButtonLabel() {
        SwingUtilities.invokeLater(() -> {
            if (PlayPause != null) {
                PlayPause.setText(isPlaying ? "Pause" : "Play");
            }
        });
    }
}

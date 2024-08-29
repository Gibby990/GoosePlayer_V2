package com.gooseplayer2;

import com.gooseplayer2.JPanels.*;
import com.gooseplayer2.Packages.Slugcat;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;

import javazoom.jl.decoder.JavaLayerException;

import javax.sound.sampled.*;
import javax.swing.*;

public class MainFrame extends JFrame {
    private GridBagLayout mainLayout;
    private GridBagConstraints gbc;
    private JToolBar toolBar;
    private FilePanel filePanel;
    private ImageIcon icon;
    private Image image;
    private Image scaledImage;
    private JButton settingsButton;
    private Slugcat Survivor;
    private JButton helpButton;
    private JButton libraryButton;

    public MainFrame() throws UnsupportedAudioFileException, IOException, LineUnavailableException, JavaLayerException {
        super("musicPlayer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(950, 600);

        Config.applySettings();

        icon = new ImageIcon(getClass().getResource("/icons/Icon.png"));
        image = icon.getImage();
        scaledImage = getScaledImage(image, 512, 547);


        setIconImage(scaledImage);

        //Toolbar
        toolBar = new JToolBar();
        settingsButton = new JButton("Settings");
        settingsButton.setFocusPainted(false);
        toolBar.add(settingsButton);

        settingsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openSettingsDialog();
            }

        });

        helpButton = new JButton("Help");
        helpButton.setFocusPainted(false);
        toolBar.add(helpButton);
        
        helpButton.addActionListener(e -> {
            try (InputStream is = getClass().getResourceAsStream("/help.txt")) {
                if (is == null) {
                    JOptionPane.showMessageDialog(null, "Help file not found.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder helpContent = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    helpContent.append(line).append("\n");
                }
                JOptionPane.showMessageDialog(null, helpContent.toString(), "Help", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null, "Failed to load help content.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        libraryButton = new JButton("Library");
        libraryButton.setFocusPainted(false);
        toolBar.add(libraryButton);

        libraryButton.addActionListener(e -> {
            try {
                File library = new File("Library");
                if(!library.exists()) {
                    JOptionPane.showMessageDialog(null, "Library not found");
                    return;
                }
                Desktop desktop = Desktop.getDesktop();
                desktop.open(library);

                Thread watcherThread = new Thread(() -> watchDirectoryPath(library.toPath()));
        watcherThread.start();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        mainLayout = new GridBagLayout();
        gbc = new GridBagConstraints();
        getContentPane().setLayout(mainLayout);

        Survivor = new Slugcat();

        filePanel = new FilePanel();

        MusicPanel musicPanel = new MusicPanel();

        //Toolbar
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        Survivor.addObjects(toolBar, this, mainLayout, gbc, 0, 0, GridBagConstraints.REMAINDER, 1);

        //Everything else
        
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        gbc.weightx = 2.0;

        Survivor.addObjects(musicPanel, this, mainLayout, gbc, 0, 1, 2, 1);

        gbc.weightx = 1.0;
        Survivor.addObjects(filePanel, this, mainLayout, gbc, 2, 1, 1, 1);


        setVisible(true);
    }

    private Image getScaledImage(Image srcImg, int width, int height) {
        BufferedImage resizedImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = resizedImg.createGraphics();

        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(srcImg, 0, 0, width,height, null);
        g2.dispose();

        return resizedImg;
    }

    private void watchDirectoryPath(Path path) {
        try (WatchService service = FileSystems.getDefault().newWatchService()) {
            registerDirectoryAndSubdirectories(path, service);
            while (true) {
                WatchKey key = service.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    Path eventPath = (Path) event.context();
                    Path fullPath = ((Path) key.watchable()).resolve(eventPath);
                    if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                        if (Files.isDirectory(fullPath)) {
                            registerDirectoryAndSubdirectories(fullPath, service);
                        }
                    }
                    SwingUtilities.invokeLater(() -> filePanel.refreshLibrary());
                }
                if (!key.reset()) {
                    break;
                }
            }
        } catch (IOException | InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    private void registerDirectoryAndSubdirectories(Path start, WatchService service) throws IOException {
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                dir.register(service, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void openSettingsDialog() {
        try {
            Config configDialog = new Config(this);
            configDialog.setVisible(true);
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error opening settings: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
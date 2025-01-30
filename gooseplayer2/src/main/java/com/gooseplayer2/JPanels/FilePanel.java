package com.gooseplayer2.JPanels;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.gooseplayer2.Config;
import com.gooseplayer2.Packages.FileTransferHandler;
import com.gooseplayer2.Packages.Slugcat;

public class FilePanel extends JPanel {
    private JLabel label;
    private JTree fileTree;
    private JScrollPane pane;
    private GridBagLayout layout;
    private GridBagConstraints gbc;
    private Path libraryPath;

    public FilePanel() {

        libraryPath = Paths.get(Config.LIBRARY_PATH);
        DefaultMutableTreeNode top = new DefaultMutableTreeNode("Library");
        createNodes(top, libraryPath);

        fileTree = new JTree(top);
        fileTree.setToggleClickCount(1);

        pane = new JScrollPane(fileTree);

        label = new JLabel("Directory Tree");
        label.setHorizontalAlignment(JLabel.CENTER);

        layout = new GridBagLayout();
        gbc = new GridBagConstraints();
        Slugcat Hunter = new Slugcat();

        setLayout(layout);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        Hunter.addObjects(label, this, layout, gbc, 0, 0, 1, 1);
       
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0; 
        gbc.weighty = 1.0; 
        Hunter.addObjects(pane, this, layout, gbc, 0, 1, 1, 1);

        fileTree.setDragEnabled(true);
        fileTree.setTransferHandler(new FileTransferHandler());

        setVisible(true);
    }

    private void createNodes(DefaultMutableTreeNode node, Path folderPath) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folderPath)) {
            for (Path entry : stream) {
                if (!entry.getFileName().toString().equals(".gitkeep")) {
                    DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(entry.getFileName().toString());
                    node.add(newNode);
                    if (Files.isDirectory(entry)) {
                        createNodes(newNode, entry);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void refreshLibrary() {
        DefaultMutableTreeNode top = new DefaultMutableTreeNode("Library");
        createNodes(top, libraryPath);
        fileTree.setModel(new DefaultTreeModel(top));
    }
}

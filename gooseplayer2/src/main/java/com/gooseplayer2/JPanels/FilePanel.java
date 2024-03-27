package com.gooseplayer2.JPanels;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import javax.swing.*;
import javax.swing.tree.*;
import com.gooseplayer2.Packages.FileTransferHandler;
import com.gooseplayer2.Packages.Slugcat;
import java.awt.*;


public class FilePanel extends JPanel {
    private JTree fileTree;
    JLabel label;
    GridBagConstraints gbc;
    GridBagLayout layout;

    public FilePanel() {
        DefaultMutableTreeNode top = new DefaultMutableTreeNode("Library");
        createNodes(top, Paths.get(
            "gooseplayer2" + File.separator + "src" + File.separator + "main" + File.separator + "java" +
            File.separator + "com" + File.separator + "gooseplayer2" + File.separator + "Library"
        ));

        fileTree = new JTree(top);
        fileTree.setToggleClickCount(1);

        label = new JLabel("Directory Tree");
        label.setHorizontalAlignment(JLabel.CENTER);

        layout = new GridBagLayout();
        gbc = new GridBagConstraints();
        Slugcat Hunter = new Slugcat();

        setLayout(layout);

        Hunter.setFill("HORIZONTAL");
        Hunter.addObjects(label, this, layout, gbc, 0, 0, 1, 1);
       
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0; 
        gbc.weighty = 1.0; 
        Hunter.addObjects(fileTree, this, layout, gbc, 0, 1, 1, 1);

        fileTree.setDragEnabled(true);
        fileTree.setTransferHandler(new FileTransferHandler());

        setVisible(true);
    }

    private void createNodes(DefaultMutableTreeNode node, Path folderPath) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folderPath)) {
            for (Path entry : stream) {
                DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(entry.getFileName().toString());
                node.add(newNode);
                if (Files.isDirectory(entry)) {
                    createNodes(newNode, entry);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

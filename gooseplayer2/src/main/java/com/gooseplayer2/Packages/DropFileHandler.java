package com.gooseplayer2.Packages;

import javax.swing.*;
import java.awt.datatransfer.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import com.gooseplayer2.JPanels.*;

public class DropFileHandler extends TransferHandler {
    MusicPlayer player;
    // Assuming FilePanel is a type of JComponent that represents your library panel
    JComponent filePanel;

    public DropFileHandler(MusicPlayer player, JComponent filePanel) {
        this.player = player;
        this.filePanel = filePanel;
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport support) {
        if (!support.isDrop()) {
            return false;
        }

        support.setDropAction(COPY); 

        // Check if the drop is coming from the FilePanel
        if (support.getComponent() == filePanel) {
            // Here you can add additional checks or preparations
        }

        return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
    }

    @Override
    public boolean importData(TransferHandler.TransferSupport support) {
        if (!canImport(support)) {
            return false;
        }

        Transferable transferable = support.getTransferable();

        try {
            @SuppressWarnings("unchecked")
            List<File> files = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);

            // Add files to the specific player's queue
            player.addFilesToTree(files);

            return true;
        } catch (UnsupportedFlavorException | IOException e) {
            e.printStackTrace();
        }

        return false;
    }
}

package com.gooseplayer2.Packages;

import javax.swing.*;
import java.awt.datatransfer.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import com.gooseplayer2.JPanels.*;

public class DropFileHandler extends TransferHandler {
    MusicPlayer player;

    public DropFileHandler(MusicPlayer player) {
        this.player = player;
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport support) {
        if (!support.isDrop()) {
            return false;
        }

        support.setDropAction(COPY); 

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

            player.addFilesToTree(files);

            return true;
        } catch (UnsupportedFlavorException | IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    
}

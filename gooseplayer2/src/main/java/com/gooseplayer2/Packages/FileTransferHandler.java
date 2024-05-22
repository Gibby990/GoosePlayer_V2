package com.gooseplayer2.Packages;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.datatransfer.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import com.gooseplayer2.Config;

public class FileTransferHandler extends TransferHandler {
    @Override
    public int getSourceActions(JComponent c) {
        return COPY_OR_MOVE; 
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        JTree tree = (JTree) c;
        TreePath[] paths = tree.getSelectionPaths();
        if (paths == null || paths.length == 0) {
            return null;
        }

        List<File> fileList = new ArrayList<>();
        
        for (TreePath path : paths) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            Object userObject = node.getUserObject();
        if (userObject instanceof File) {
            fileList.add((File) userObject);
        } else {
            String fileName = userObject.toString();
            File file = new File(Config.LIBRARY_PATH, fileName);
            fileList.add(file);
        }
        }


        return new FileTransferable(fileList);
    }

    @Override
    protected void exportDone(JComponent source, Transferable data, int action) {
    }

    private static class FileTransferable implements Transferable {
        private final List<File> files;

        public FileTransferable(List<File> files) {
            this.files = files;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{DataFlavor.javaFileListFlavor};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return DataFlavor.javaFileListFlavor.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (!isDataFlavorSupported(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            return files;
        }
    }
}

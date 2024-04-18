package com.gooseplayer2.Packages;

import java.io.File;

public class QueuedFile {
    private static long nextID = 0;

    @SuppressWarnings("unused")
    private final long id;
    private final File file;

    public QueuedFile(File file) {
        this.id = nextID++;
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    @Override
    public String toString() {
        return file.getName();
    }
}

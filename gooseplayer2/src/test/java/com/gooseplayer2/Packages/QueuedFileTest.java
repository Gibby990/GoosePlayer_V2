package com.gooseplayer2.Packages;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.*;

public class QueuedFileTest {

    @Test
    void getFileAndToString() {
        File f = new File("song.mp3");
        QueuedFile qf = new QueuedFile(f);
        assertSame(f, qf.getFile());
        assertEquals("song.mp3", qf.toString());
    }

    @Test
    void idIsUnique() throws Exception {
        QueuedFile a = new QueuedFile(new File("a.mp3"));
        QueuedFile b = new QueuedFile(new File("b.mp3"));
        Field idField = QueuedFile.class.getDeclaredField("id");
        idField.setAccessible(true);
        long ida = idField.getLong(a);
        long idb = idField.getLong(b);
        assertNotEquals(ida, idb);
    }
}

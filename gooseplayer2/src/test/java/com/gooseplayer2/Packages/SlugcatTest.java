package com.gooseplayer2.Packages;

import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.*;
import static org.junit.jupiter.api.Assertions.*;

public class SlugcatTest {

    @Test
    void addObjects_addsComponent() {
        Slugcat s = new Slugcat();
        JPanel p = new JPanel();
        s.addObjects(new JButton("x"), p, new GridBagLayout(), new GridBagConstraints(), 0,0,1,1);
        assertEquals(1, p.getComponentCount());
    }
}

package com.gooseplayer2.Packages;

import java.awt.*;

public class Cat {
    GridBagLayout Layout;
    public GridBagConstraints gbc;

    public Cat() {
        Layout = new GridBagLayout();
        gbc = new GridBagConstraints();
    }

    public void addObjects (Component componentE, Container theContainer, GridBagLayout layout, GridBagConstraints constraints, int gridx, int gridy, int gridWidth, int gridHeight) {
        constraints.gridx = gridx;
        constraints.gridy = gridy;

        constraints.gridwidth = gridWidth;
        constraints.gridheight = gridHeight;

        //layout.setConstraints(componentE, gbc);
        theContainer.add(componentE, constraints);
    }
}
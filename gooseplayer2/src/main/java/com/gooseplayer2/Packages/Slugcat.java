package com.gooseplayer2.Packages;

import java.awt.*;

public class Slugcat {
    GridBagLayout Layout;
    public GridBagConstraints gbc;

    public Slugcat() {
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

    public void setFill(String fill) {
        switch (fill.toUpperCase()) {
            case "HORIZONTAL":
                gbc.fill = GridBagConstraints.HORIZONTAL;
                break;
            case "VERTICAL":
                gbc.fill = GridBagConstraints.VERTICAL;
                break;
            case "BOTH":
                gbc.fill = GridBagConstraints.BOTH;
                break;
            case "NONE":
                gbc.fill = GridBagConstraints.NONE;
                break;
            default:
                throw new IllegalArgumentException("Invalid fill argument");
        }
    }
}
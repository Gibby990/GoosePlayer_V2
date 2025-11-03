package com.gooseplayer2.JPanels;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import com.gooseplayer2.Packages.Slugcat;

public class RadioPlayer extends JPanel {

	private GridBagConstraints gbc;
	private GridBagLayout layout;
	private JLabel ChannelLabel, TimeLabel;
	private JLabel AlbumArtLabel;
	private JSlider ProgressBar, VolumeSlider;
	private JButton PlayPause, Skip, Remove, Clear, Shuffle, Enter;
	private JLabel UrlLabel;
	private JTextField UrlField;

	public RadioPlayer() {
		layout = new GridBagLayout();
		gbc = new GridBagConstraints();
		setLayout(layout);

		Slugcat Spearmaster = new Slugcat();

		PlayPause = new JButton("Play");
		PlayPause.setEnabled(false);

		Skip = new JButton("Skip");
		Skip.setEnabled(false);

		Remove = new JButton("Remove");
		Remove.setEnabled(false);

		Clear = new JButton("Clear");
		Clear.setEnabled(false);

		Shuffle = new JButton("Shuffle");
		Shuffle.setEnabled(false);

		Enter = new JButton("Enter"); // Checks url box for a valid AzuraCast URL eventually

		ProgressBar = new JSlider(0, 0, 100, 0);
		ProgressBar.setEnabled(false);

		TimeLabel = new JLabel("0:00 / 0:00");
		ChannelLabel = new JLabel("Radio");

		VolumeSlider = new JSlider(0, 100, 100);
		VolumeSlider.setEnabled(false);

		AlbumArtLabel = new JLabel();
		AlbumArtLabel.setHorizontalAlignment(SwingConstants.CENTER);
		AlbumArtLabel.setVerticalAlignment(SwingConstants.CENTER);
		AlbumArtLabel.setPreferredSize(new Dimension(128, 128));
		setDefaultAlbumArt();

		UrlLabel = new JLabel("Stream URL:");
		UrlLabel.setFont(UrlLabel.getFont().deriveFont(Font.PLAIN, 25f));
		UrlField = new JTextField();
		UrlField.setToolTipText("Enter AzuraCast stream URL");

		gbc.gridheight = 3;
		gbc.gridwidth = 6;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;

		gbc.fill = GridBagConstraints.CENTER;
		Spearmaster.addObjects(ChannelLabel, this, layout, gbc, 0, 0, 4, 1);

		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(0, 20, 0, 0);
		Spearmaster.addObjects(ProgressBar, this, layout, gbc, 0, 1, 4, 1);
		Spearmaster.addObjects(TimeLabel, this, layout, gbc, 4, 1, 1, 1);

		Spearmaster.addObjects(VolumeSlider, this, layout, gbc, 0, 2, 2, 1);

		gbc.fill = GridBagConstraints.NONE;
		Spearmaster.addObjects(AlbumArtLabel, this, layout, gbc, 5, 0, 1, 3);

		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.weightx = 1.0;
		gbc.insets = new Insets(0, 20, 0, 5);
		Spearmaster.addObjects(PlayPause, this, layout, gbc, 0, 3, 1, 1);

		gbc.anchor = GridBagConstraints.EAST;
		Spearmaster.addObjects(Skip, this, layout, gbc, 1, 3, 1, 1);

		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.CENTER;
		Spearmaster.addObjects(Shuffle, this, layout, gbc, 2, 3, 1, 1);
		Spearmaster.addObjects(Remove, this, layout, gbc, 3, 3, 1, 1);
		Spearmaster.addObjects(Clear, this, layout, gbc, 4, 3, 1, 1);

		JPanel urlPanel = new JPanel(new GridLayout(1, 3));
		urlPanel.add(UrlLabel);
		urlPanel.add(UrlField);
		urlPanel.add(Enter);

		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(0, 0, 0, 0);
		Spearmaster.addObjects(urlPanel, this, layout, gbc, 1, 4, 6, 1);
	}

	public String getStreamUrl() {
		return UrlField != null ? UrlField.getText() : "";
	}

	public void setStreamUrl(String url) {
		if (UrlField != null) {
			UrlField.setText(url != null ? url : "");
		}
	}

	private void setDefaultAlbumArt() {
		try {
			java.net.URL url = getClass().getResource("/icons/albumMissing.png");
			if (url != null) {
				ImageIcon icon = new ImageIcon(url);
				Image scaled = icon.getImage().getScaledInstance(128, 128, Image.SCALE_SMOOTH);
				AlbumArtLabel.setIcon(new ImageIcon(scaled));
			} else {
				AlbumArtLabel.setIcon(null);
			}
		} catch (Exception e) {
			AlbumArtLabel.setIcon(null);
		}
	}
}

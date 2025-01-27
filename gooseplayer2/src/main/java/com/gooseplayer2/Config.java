package com.gooseplayer2;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.awt.*;
import java.util.Properties;
import com.formdev.flatlaf.intellijthemes.*;

import javax.swing.*;

import com.gooseplayer2.Packages.Slugcat;

public class Config extends JDialog {
    public static final String LIBRARY_PATH = System.getProperty("user.dir") + File.separator + "gooseplayer2" + File.separator + "src" +  File.separator + "Library";
    public static final String SETTINGS_FILE_PATH = System.getProperty("user.dir") + File.separator + "gooseplayer2" + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "settings.txt";
    public static final String RESOURCES_FILE_PATH = System.getProperty("user.dir")+ File.separator + "gooseplayer2" + File.separator + "src" + File.separator + "main" + File.separator + "resources";

    private GridBagConstraints gbc;
    private GridBagLayout layout;
    private Properties p;
    private FileReader reader;
    private Slugcat Artificer;

    private String currentTheme;

    @SuppressWarnings("rawtypes")
    private JComboBox ThemeBox;
    private JSeparator Seperator;
    private JLabel ThemeLabel;
    
    public Config(JFrame parent) throws IOException {
        super(parent, "Settings", true);
        setSize(300, 200);
        setLocationRelativeTo(parent);

        layout = new GridBagLayout();
        gbc = new GridBagConstraints();

        Seperator = new JSeparator();
        Seperator.setOrientation(SwingConstants.HORIZONTAL); 

        Artificer = new Slugcat();

        reader = new FileReader(SETTINGS_FILE_PATH);
        p = new Properties();
        p.load(reader);

        currentTheme = p.getProperty("theme");

        //Theme List
        String[] themeList = {"Light Flat", "Solarized Light", "Arc", "Dark Flat", "Arc Dark", "Arc Dark Orange", "Hiberbee Dark", "Dark Purple", "Nord"};

        ThemeBox = new JComboBox<>(themeList);
        ThemeBox.setSelectedItem(currentTheme);
        ThemeBox.addActionListener(new ActionListener() {
            @SuppressWarnings("rawtypes")
            @Override
            public void actionPerformed(ActionEvent e) {
                try (FileWriter writer = new FileWriter(SETTINGS_FILE_PATH)) {
                    JComboBox cb = (JComboBox)e.getSource();
                    String selectedTheme = (String)cb.getSelectedItem();
                    p.setProperty("theme", selectedTheme);
                    p.store(writer, null);
                    currentTheme = selectedTheme;
                    applySettings();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });
        ThemeLabel = new JLabel("Theme: ");


        //Layout

        setLayout(layout);

        gbc.gridheight = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        Artificer.addObjects(ThemeBox, this, layout, gbc, 1, 0, 1, 1);
        Artificer.addObjects(ThemeLabel, this, layout, gbc, 0, 0, 1, 1);

        Artificer.addObjects(Seperator, this, layout, gbc, 0, 1, 2, 1);

        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> {
            saveSettings();
            dispose();
        });
        Artificer.addObjects(saveButton, this, layout, gbc, 0, 2, 2, 1);
    }

    private void saveSettings() {
        try (FileWriter writer = new FileWriter(SETTINGS_FILE_PATH)) {
            p.store(writer, null);
            applySettings();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void applySettings() {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                Properties p = new Properties();
                p.load(new FileReader(SETTINGS_FILE_PATH));

                applyTheme(p.getProperty("theme", ""));
                
            } catch (Exception e) {
                e.printStackTrace();
            }
            SwingUtilities.updateComponentTreeUI(JFrame.getFrames()[0]);
            for (Window window : Window.getWindows()) {
                SwingUtilities.updateComponentTreeUI(window);
            }
        });
    }

    public static void applyTheme(String theme) {
        switch (theme) {
            case "Light Flat":
                FlatLightFlatIJTheme.setup();
                break;
            case "Solarized Light":
                FlatSolarizedLightIJTheme.setup();
                break;
            case "Arc" :
                FlatArcIJTheme.setup();
                break;
            case "Dark Flat":
                FlatDarkFlatIJTheme.setup();
                break;
            case "Arc Dark": 
                FlatArcDarkIJTheme.setup();
                break;
            case "Arc Dark Orange":
                FlatArcDarkOrangeIJTheme.setup();
                break;
            case "Hiberbee Dark":
                FlatHiberbeeDarkIJTheme.setup();
                break;
            case "Dark Purple":
                FlatDarkPurpleIJTheme.setup();
                break;
            case "Nord":
                FlatNordIJTheme.setup();
                break;
            default:
                FlatLightFlatIJTheme.setup();
                break;
        }
    }
}
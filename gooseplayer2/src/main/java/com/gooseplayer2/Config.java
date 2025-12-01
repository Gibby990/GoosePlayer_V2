package com.gooseplayer2;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.awt.*;
import java.util.Properties;
import java.util.Objects;
import com.formdev.flatlaf.intellijthemes.*;

import javax.swing.*;

import com.gooseplayer2.Packages.Cat;

public class Config extends JDialog {
    public static final String SETTINGS_FILE_PATH = initSettingsPath();
    public static final String LIBRARY_PATH = initLibraryPath();
    public static final String RESOURCES_FILE_PATH = System.getProperty("user.dir")+ File.separator + "gooseplayer2" + File.separator + "src" + File.separator + "main" + File.separator + "resources";

    private GridBagConstraints gbc;
    private GridBagLayout layout;
    private Properties p;
    private FileReader reader;
    private Cat Artificer; 

    private String currentTheme, currentStyle, currentMonoChannelName, currentMultiChannel1Name, currentMultiChannel2Name, currentMultiChannel3Name;

    private JButton saveButton;
    @SuppressWarnings("rawtypes")
    private JComboBox ThemeBox, StyleBox;
    private JSeparator Seperator;
    private JTextField MonoChannelField, MultiChannel1Field, MultiChannel2Field, MultiChannel3Field;
    private JTextField LibraryField;
    private JButton libraryBrowseButton, libraryDefaultButton;
    private JLabel ThemeLabel, StyleLabel, RestartWarning, MonoChannelLabel, MultiChannel1Label, MultiChannel2Label, MultiChannel3Label, LibraryLabel;
    
    public Config(JFrame parent) throws IOException {
        super(parent, "Settings", true);
        setSize(500, 300);
        setLocationRelativeTo(parent);

        layout = new GridBagLayout();
        gbc = new GridBagConstraints();

        Seperator = new JSeparator();
        Seperator.setOrientation(SwingConstants.HORIZONTAL); 

        Artificer = new Cat();

        reader = new FileReader(SETTINGS_FILE_PATH);
        p = new Properties();
        p.load(reader);

        currentTheme = p.getProperty("theme");
        currentStyle = p.getProperty("style");
        currentMonoChannelName = p.getProperty("monochannelname");
        currentMultiChannel1Name = p.getProperty("multichannel1name");
        currentMultiChannel2Name = p.getProperty("multichannel2name");
        currentMultiChannel3Name = p.getProperty("multichannel3name");
        String configuredLibrary = p.getProperty("librarypath");
        String currentLibraryPath;
        if (configuredLibrary == null || configuredLibrary.isEmpty()) {
            currentLibraryPath = LIBRARY_PATH;
        } else {
            currentLibraryPath = configuredLibrary;
        }

        //Theme List
        String[] themeList = {"Light Flat", "Solarized Light", "Arc", "Dark Flat", "Arc Dark", "Arc Dark Orange", "Hiberbee Dark", "Dark Purple", "Nord"};
        String[] styleList = {"Monochannel", "Multichannel", "Radio"};

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

        RestartWarning = new JLabel("<html>" + "<B>" + "WARNING: Changes below will restart program" + "</B>" + "</html>");

        StyleBox = new JComboBox<>(styleList);
        StyleBox.setSelectedItem(currentStyle);
        StyleBox.addActionListener(new ActionListener() {
            @SuppressWarnings("rawtypes")
            @Override
            public void actionPerformed(ActionEvent e) {
                try (FileWriter writer = new FileWriter(SETTINGS_FILE_PATH)) {
                    JComboBox cb = (JComboBox)e.getSource();
                    String selectedStyle = (String)cb.getSelectedItem();
                    p.setProperty("style", selectedStyle);
                    p.store(writer, null);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });

        StyleLabel = new JLabel("Style: ");

        MonoChannelLabel = new JLabel("MonoChannel Name:");
        MonoChannelField = new JTextField();
        MonoChannelField.setText(currentMonoChannelName);

        MultiChannel1Label = new JLabel("MultiChannel 1 Name");
        MultiChannel1Field = new JTextField();
        MultiChannel1Field.setText(currentMultiChannel1Name);

        MultiChannel2Label = new JLabel("MultiChannel 2 Name");
        MultiChannel2Field = new JTextField();
        MultiChannel2Field.setText(currentMultiChannel2Name);

        MultiChannel3Label = new JLabel("MultiChannel 3 Name");
        MultiChannel3Field = new JTextField();
        MultiChannel3Field.setText(currentMultiChannel3Name);

        LibraryLabel = new JLabel("Library Folder");
        LibraryField = new JTextField();
        LibraryField.setText(currentLibraryPath);

        libraryBrowseButton = new JButton("Browse...");
        libraryBrowseButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            try {
                File startDir = new File(LibraryField.getText());
                if (startDir.exists()) chooser.setCurrentDirectory(startDir);
            } catch (Exception ignored) {}
            int result = chooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selected = chooser.getSelectedFile();
                if (selected != null) {
                    LibraryField.setText(selected.getAbsolutePath());
                }
            }
        });

        libraryDefaultButton = new JButton("Use Built-in");
        libraryDefaultButton.addActionListener(e -> {
            String builtIn = computeDefaultLibraryPath();
            LibraryField.setText(builtIn);
        });

        JPanel libraryPathPanel = new JPanel();
        libraryPathPanel.setLayout(new BorderLayout(5, 0));
        libraryPathPanel.add(LibraryField, BorderLayout.CENTER);
        JPanel libraryButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        libraryButtons.add(libraryDefaultButton);
        libraryButtons.add(libraryBrowseButton);
        libraryPathPanel.add(libraryButtons, BorderLayout.EAST);

        saveButton = new JButton("Save"); //Only works for Themes
        saveButton.addActionListener(e -> {
            boolean restartNeeded = saveSettings();
            dispose();
            if (restartNeeded) {
                restartApplication();
            }
        });
        
        //Layout

        setLayout(layout);

        gbc.gridheight = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.NONE;

        Artificer.addObjects(ThemeLabel, this, layout, gbc, 0, 0, 1, 1);
        Artificer.addObjects(ThemeBox, this, layout, gbc, 1, 0, 1, 1);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        Artificer.addObjects(Seperator, this, layout, gbc, 0, 1, 2, 1);
        
        gbc.insets = new Insets(0, 10, 0, 0);
        Artificer.addObjects(RestartWarning, this, layout, gbc, 0, 2, 2, 1);
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.fill = GridBagConstraints.NONE;

        Artificer.addObjects(StyleLabel, this, layout, gbc, 0, 3, 1, 1);
        Artificer.addObjects(StyleBox, this , layout, gbc, 1, 3, 1, 1);

        Artificer.addObjects(MonoChannelLabel, this, layout, gbc, 0, 5, 1, 1);
        Artificer.addObjects(MonoChannelField, this, layout, gbc,1, 5, 1, 1);

        Artificer.addObjects(MultiChannel1Label, this, layout, gbc, 0, 6, 1, 1);
        Artificer.addObjects(MultiChannel1Field, this, layout, gbc, 1, 6, 1, 1);

        Artificer.addObjects(MultiChannel2Label, this, layout, gbc, 0, 7, 1, 1);
        Artificer.addObjects(MultiChannel2Field, this, layout, gbc, 1, 7, 1, 1);

        Artificer.addObjects(MultiChannel3Label, this, layout, gbc, 0, 8, 1, 1);
        Artificer.addObjects(MultiChannel3Field, this, layout, gbc, 1, 8, 1, 1);

        Artificer.addObjects(LibraryLabel, this, layout, gbc, 0, 9, 1, 1);
        Artificer.addObjects(libraryPathPanel, this, layout, gbc, 1, 9, 1, 1);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        Artificer.addObjects(saveButton, this, layout, gbc, 0, 10, 2, 1);
    }

    private static String initLibraryPath() {
        try {
            Properties props = new Properties();
            props.load(new FileReader(SETTINGS_FILE_PATH));
            String configured = props.getProperty("librarypath");
            if (configured != null && !configured.isEmpty()) {
                File cfgDir = new File(configured);
                if (cfgDir.exists() && cfgDir.isDirectory()) {
                    return cfgDir.getAbsolutePath();
                }
            }
        } catch (Exception ignored) {}
        return computeDefaultLibraryPath();
    }

    private static String computeDefaultLibraryPath() {
        String cwdLibrary = System.getProperty("user.dir") + File.separator + "Library";
        File cwdDir = new File(cwdLibrary);
        if (cwdDir.exists() && cwdDir.isDirectory()) {
            return cwdDir.getAbsolutePath();
        }

        String devPath = System.getProperty("user.dir") + File.separator + "gooseplayer2" + File.separator + "src" + File.separator + "Library";
        File devDir = new File(devPath);
        if (devDir.exists() && devDir.isDirectory()) {
            return devDir.getAbsolutePath();
        }

        try {
            File codeSource = new File(Config.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            File jarDir = codeSource.getParentFile();
            if (jarDir != null) {
                File libDir = new File(jarDir, "Library");
                if (libDir.exists() && libDir.isDirectory()) {
                    return libDir.getAbsolutePath();
                }
            }
        } catch (Exception ignored) {}

        if (!cwdDir.exists()) {
            cwdDir.mkdirs();
        }
        return cwdDir.getAbsolutePath();
    }

    private boolean saveSettings() {
        boolean restartRequired = false;
        try (FileWriter writer = new FileWriter(SETTINGS_FILE_PATH)) {

            // Detect changes that require restart (non-theme settings)
            boolean styleChanged = !Objects.equals(currentStyle, p.getProperty("style"));
            boolean monoChanged = !Objects.equals(currentMonoChannelName, MonoChannelField.getText());
            boolean multi1Changed = !Objects.equals(currentMultiChannel1Name, MultiChannel1Field.getText());
            boolean multi2Changed = !Objects.equals(currentMultiChannel2Name, MultiChannel2Field.getText());
            boolean multi3Changed = !Objects.equals(currentMultiChannel3Name, MultiChannel3Field.getText());
            boolean libraryChanged = !Objects.equals(p.getProperty("librarypath", LIBRARY_PATH), LibraryField.getText());

            // Persist updates
            p.setProperty("monochannelname", MonoChannelField.getText());
            p.setProperty("multichannel1name", MultiChannel1Field.getText());
            p.setProperty("multichannel2name", MultiChannel2Field.getText());
            p.setProperty("multichannel3name", MultiChannel3Field.getText());
            p.setProperty("librarypath", LibraryField.getText());

            p.store(writer, null);

            // Apply theme immediately; other changes may need restart
            applySettings();

            restartRequired = styleChanged || monoChanged || multi1Changed || multi2Changed || multi3Changed || libraryChanged;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return restartRequired;
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

    private static String initSettingsPath() {
        String appData = System.getenv("APPDATA");
        String targetPath;
        if (appData != null && !appData.isEmpty()) {
            targetPath = appData + File.separator + "GoosePlayer2" + File.separator + "settings.txt";
        } else {
            targetPath = System.getProperty("user.dir") + File.separator + "settings.txt";
        }

        File settingsFile = new File(targetPath);
        File parentDir = settingsFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        if (!settingsFile.exists()) {
            try (InputStream in = Config.class.getResourceAsStream("/settings.txt")) {
                if (in != null) {
                    try (FileOutputStream out = new FileOutputStream(settingsFile)) {
                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = in.read(buffer)) != -1) {
                            out.write(buffer, 0, read);
                        }
                    }
                } else {
                    settingsFile.createNewFile();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return settingsFile.getAbsolutePath();
    }

    private static void restartApplication() {
        try {
            String javaBinBase = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
            String javaBin = javaBinBase;
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                File javaExe = new File(javaBinBase + ".exe");
                if (javaExe.exists()) {
                    javaBin = javaExe.getAbsolutePath();
                }
            }

            String classPath = System.getProperty("java.class.path");
            String mainClass = "com.gooseplayer2.Main";

            ProcessBuilder builder = new ProcessBuilder(javaBin, "-cp", classPath, mainClass);
            builder.directory(new File(System.getProperty("user.dir")));
            builder.start();
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }
        System.exit(0);
    }
}
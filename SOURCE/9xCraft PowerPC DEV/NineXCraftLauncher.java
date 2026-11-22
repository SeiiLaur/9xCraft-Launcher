import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Vector;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.net.URL;
import java.net.URLClassLoader;
import java.lang.reflect.*;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import javax.sound.midi.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Enumeration;
import java.util.IdentityHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.File;

public class NineXCraftLauncher extends JFrame {

public static boolean keepLogWindowGlobal = false;

private JLabel folderWarningLabel;
private Timer folderWarningTimer;

    private JComboBox versionBox;
private JTextField userField = new StoneField("Player");
private JTextField widthField = new StoneField("854");
private JTextField heightField = new StoneField("480");
private JTextField minMemField = new StoneField("128");
private JTextField maxMemField = new StoneField("256");

    private JCheckBox keepLevelDat = new JCheckBox("Keep level.dat file (RubyDung)");
    private JCheckBox keepLogOpen = new JCheckBox("Keep log window open");
    private JCheckBox softwareGL = new JCheckBox("Use Software Rendering");
    private JCheckBox noDDraw = new JCheckBox("Disable DirectDraw");
    private JCheckBox fullscreenBox = new JCheckBox("Fullscreen");
    private JCheckBox standaloneMode = new JCheckBox("Use Standalone Mode");

    private File optionsFile = new File("options.txt");
    private File javaConfigFile = new File("java.txt");

    private String customJavaPath = null;
    public static String customJvmArgs = "";
private String selectedSkin = "DEFAULT";
private File skinsDirectory = getLauncherSkinsDirectory();
private boolean darkMode = false;
private JMenuItem darkModeItem;
private String zoomMode = "MEDIUM";
private static String[] launcherStartupArguments = new String[0];

private java.util.List<String> recentJars = new ArrayList<String>();
private static final int MAX_RECENT = 5;

static {
    ZoomSupport.configure(readOptionValue("zoom", "MEDIUM"));
    DarkMode.apply(Boolean.valueOf(readOptionValue("darkMode", "false")).booleanValue());
}

    public NineXCraftLauncher() {
        super("9xCraft Launcher");

        darkMode = Boolean.valueOf(readOptionValue("darkMode", "false")).booleanValue();
        zoomMode = ZoomSupport.normalizeMode(readOptionValue("zoom", "MEDIUM"));
        ZoomSupport.configure(zoomMode);

        setSize(ZoomSupport.scaleDimension(new Dimension(460, 420)));
setResizable(false);
setLocationRelativeTo(null);
setExtendedState(JFrame.NORMAL);
setContentPane(new JPanel() {

    Image lightDirt = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/1.png"));
    java.net.URL darkDirtURL = getClass().getResource("/3.png");
    Image darkDirt = darkDirtURL == null ? null : Toolkit.getDefaultToolkit().getImage(darkDirtURL);

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Image dirt = darkMode && darkDirt != null ? darkDirt : lightDirt;
        int tileSize = ZoomSupport.scale(64);

        if (dirt != null) {
            for (int x = 0; x < getWidth(); x += tileSize) {
                for (int y = 0; y < getHeight(); y += tileSize) {
                    g.drawImage(dirt, x, y, tileSize, tileSize, this);
                }
            }
        }
    }
});

getContentPane().setLayout(new BorderLayout());

createMenuBar();

JPanel center = new JPanel() {

    Image stone = Toolkit.getDefaultToolkit().getImage(
        getClass().getResource("/0.png")
    );

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (stone != null) {
            int tileSize = ZoomSupport.scale(64);
            for (int x = 0; x < getWidth(); x += tileSize) {
                for (int y = 0; y < getHeight(); y += tileSize) {
                    g.drawImage(stone, x, y, tileSize, tileSize, this);
                }
            }
        }
    }
};

center.setLayout(new GridLayout(8,2));
center.setOpaque(false);

java.net.URL headerURL = getClass().getResource("/header.png");
if (headerURL != null) {

    JPanel headerPanel = new JPanel() {

        private Image shadow = new ImageIcon(
                getClass().getResource("/17.png")
        ).getImage();

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            int width = getWidth();
            int height = getHeight();
            int shadowHeight = ZoomSupport.scale(shadow.getHeight(null));

            g.drawImage(shadow,
                    0,
                    height - shadowHeight,
                    width,
                    shadowHeight,
                    null);
        }
    };

    headerPanel.setLayout(new BorderLayout());
    headerPanel.setOpaque(false);

    JLabel header = new JLabel(ZoomSupport.scaleImageIcon(new ImageIcon(headerURL)));
    header.setHorizontalAlignment(SwingConstants.CENTER);

    headerPanel.add(header, BorderLayout.CENTER);

    add(headerPanel, BorderLayout.NORTH);
}

versionBox = new JComboBox(loadVersions());
versionBox.setMaximumRowCount(4);

versionBox.setBorder(BorderFactory.createEmptyBorder());
versionBox.setUI(createVersionBoxUI());
versionBox.setFocusable(false);

versionBox.setRenderer(new DefaultListCellRenderer() {

    private Image stone = new ImageIcon(
            getClass().getResource("/stone_box.jpg")
    ).getImage();

    public Component getListCellRendererComponent(
            JList list, Object value, int index,
            boolean isSelected, boolean cellHasFocus) {

        JLabel label;

        if (index == -1) {
            label = new JLabel(value.toString()) {
                protected void paintComponent(Graphics g) {
                    if (stone != null) {
                        g.drawImage(stone, 0, 0, getWidth(), getHeight(), this);
                    }
                    super.paintComponent(g);
                }
            };

            label.setOpaque(false);
            label.setForeground(Color.BLACK);
            label.setFont(list.getFont());
            label.setBorder(BorderFactory.createEmptyBorder(0, ZoomSupport.scale(6), 0, 0));

        } else {
            label = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);

            label.setForeground(darkMode ? Color.WHITE : Color.BLACK);
        }

        return label;
    }
});

center.add(new OutlineLabel("Minecraft Version:"));
center.add(versionBox);

center.add(new OutlineLabel("Username:"));
center.add(userField);

center.add(new OutlineLabel("Width:"));
center.add(widthField);

center.add(new OutlineLabel("Height:"));
center.add(heightField);

center.add(new OutlineLabel("Min Memory (MB):"));
center.add(minMemField);

center.add(new OutlineLabel("Max Memory (MB):"));
center.add(maxMemField);

center.add(softwareGL);
center.add(noDDraw);

fullscreenBox.setForeground(Color.WHITE);
fullscreenBox.setOpaque(false);
center.add(fullscreenBox);

softwareGL.setForeground(Color.WHITE);
noDDraw.setForeground(Color.WHITE);
keepLogOpen.setForeground(Color.WHITE);
keepLogOpen.setOpaque(false);
softwareGL.setOpaque(false);
noDDraw.setOpaque(false);
center.add(keepLogOpen);

add(center, BorderLayout.CENTER);

final ImageIcon normalIcon = ZoomSupport.scaleImageIcon(new ImageIcon(
        getClass().getResource("/launch_button.jpg")
));

final ImageIcon hoverIcon = ZoomSupport.scaleImageIcon(new ImageIcon(
        getClass().getResource("/launch_button_hover.jpg")
));

final ImageIcon pressIcon = ZoomSupport.scaleImageIcon(new ImageIcon(
        getClass().getResource("/launch_button_pressed.jpg")
));

final JButton launchButton = new JButton(normalIcon);

launchButton.setPreferredSize(new Dimension(
        normalIcon.getIconWidth(),
        normalIcon.getIconHeight()
));

launchButton.setContentAreaFilled(false);
launchButton.setBorderPainted(false);
launchButton.setFocusPainted(false);
launchButton.setOpaque(false);

launchButton.addMouseListener(new MouseAdapter() {

    public void mouseEntered(MouseEvent e) {
        launchButton.setIcon(hoverIcon);
    }

    public void mouseExited(MouseEvent e) {
        launchButton.setIcon(normalIcon);
    }

    public void mousePressed(MouseEvent e) {
        launchButton.setIcon(pressIcon);
    }

    public void mouseReleased(MouseEvent e) {
        launchButton.setIcon(normalIcon);
    }
});

folderWarningLabel = new JLabel("", SwingConstants.CENTER);
folderWarningLabel.setForeground(Color.RED);
folderWarningLabel.setVisible(true);
folderWarningLabel.setText(" ");

JPanel launchPanel = new JPanel();
launchPanel.setOpaque(false);

launchPanel.setLayout(new BoxLayout(launchPanel, BoxLayout.Y_AXIS));
folderWarningLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
launchButton.setAlignmentX(Component.CENTER_ALIGNMENT);

launchPanel.add(folderWarningLabel);
launchPanel.add(launchButton);
launchPanel.add(Box.createRigidArea(new Dimension(0, ZoomSupport.scale(5))));

        JLabel footer = new OutlineLabel(
            "VER_111R111926; Distributed by Yumiko; November 2026",
            Color.WHITE,
            Color.BLACK
        );
        footer.setFont(ZoomSupport.scaleFont(new Font("Dialog", Font.PLAIN, 10)));
        footer.setHorizontalAlignment(SwingConstants.LEFT);
        JPanel bottomPanel = new JPanel() {

    private Image shadow = new ImageIcon(
            getClass().getResource("/17.png")
    ).getImage();

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int width = getWidth();
        int shadowHeight = ZoomSupport.scale(shadow.getHeight(null));

        g.drawImage(shadow,
                0, 0, width, shadowHeight,
                0, shadow.getHeight(null), 1, 0,
                null);
    }
};

bottomPanel.setLayout(new BorderLayout());
bottomPanel.setOpaque(false);
bottomPanel.setBorder(BorderFactory.createEmptyBorder(ZoomSupport.scale(4), 0, 0, 0));
bottomPanel.add(launchPanel, BorderLayout.CENTER);
bottomPanel.add(footer, BorderLayout.SOUTH);

add(bottomPanel, BorderLayout.SOUTH);

        launchButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                saveOptions();
                launchGame();
            }
        });

        loadOptions();
        loadJavaConfig();
        refreshVersionBoxUI();
        updateDarkModeMenuText();

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                saveOptions();
                dispose();
                System.exit(0);
            }
        });

        setVisible(true);

SwingUtilities.invokeLater(new Runnable() {
    public void run() {
        getRootPane().requestFocusInWindow();
    }
});
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");

JMenuItem newItem = new JMenuItem("New");

JMenuItem openJarItem = new JMenuItem("Open .jar");
openJarItem.addActionListener(new ActionListener() {
    public void actionPerformed(ActionEvent e) {
        openJarDirectly();
    }
});

JMenuItem openRecentItem = new JMenuItem("Open Recent");
openRecentItem.addActionListener(new ActionListener() {
    public void actionPerformed(ActionEvent e) {
        openRecentWindow();
    }
});

JMenuItem viewMinecraftItem = new JMenuItem("View .minecraft");
viewMinecraftItem.addActionListener(new ActionListener() {
    public void actionPerformed(ActionEvent e) {
        openSelectedMinecraftFolder();
    }
});

JMenuItem exitItem = new JMenuItem("Exit");

newItem.addActionListener(new ActionListener() {
    public void actionPerformed(ActionEvent e) {
        new NineXCraftLauncher();
    }
});

exitItem.addActionListener(new ActionListener() {
    public void actionPerformed(ActionEvent e) {
        saveOptions();
        dispose();
        System.exit(0);
    }
});

fileMenu.add(newItem);
fileMenu.add(openJarItem);
fileMenu.add(openRecentItem);
fileMenu.add(viewMinecraftItem);
fileMenu.addSeparator();
fileMenu.add(exitItem);

        JMenu editMenu = new JMenu("Edit");
        JMenuItem javaItem = new JMenuItem("Java...");
        javaItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                openJavaSettings();
            }
        });
        editMenu.add(javaItem);

JMenuItem jvmArgsItem = new JMenuItem("JVM Arguments...");

jvmArgsItem.addActionListener(new ActionListener() {
    public void actionPerformed(ActionEvent e) {
        new JvmArgsWindow();
    }
});

editMenu.add(jvmArgsItem);

JMenuItem infPaintItem = new JMenuItem("InfPaint");

infPaintItem.addActionListener(new ActionListener() {
    public void actionPerformed(ActionEvent e) {
        openInfPaint();
    }
});

editMenu.add(infPaintItem);

JMenu optionsMenu = new JMenu("Options");

JMenuItem extendedItem = new JMenuItem("Extended Configuration");

extendedItem.addActionListener(new ActionListener() {
    public void actionPerformed(ActionEvent e) {
        openExtendedConfig();
    }
});

optionsMenu.add(extendedItem);

JMenuItem skinsItem = new JMenuItem("Skins...");
skinsItem.addActionListener(new ActionListener() {
    public void actionPerformed(ActionEvent e) {
        openSkinsWindow();
    }
});
optionsMenu.add(skinsItem);

darkModeItem = new JMenuItem("Enable dark mode [EXP]");
darkModeItem.addActionListener(new ActionListener() {
    public void actionPerformed(ActionEvent e) {
        boolean previousDarkMode = darkMode;
        darkMode = !darkMode;
        saveOptions();
        if (!restartLauncherProcess()) {
            darkMode = previousDarkMode;
            saveOptions();
            updateDarkModeMenuText();
        }
    }
});
optionsMenu.add(darkModeItem);

JMenu zoomMenu = new JMenu("Zoom");
JMenuItem smallZoomItem = new JMenuItem("Small");
smallZoomItem.addActionListener(new ActionListener() {
    public void actionPerformed(ActionEvent e) {
        changeZoomMode("SMALL");
    }
});
JMenuItem mediumZoomItem = new JMenuItem("Medium [Default]");
mediumZoomItem.addActionListener(new ActionListener() {
    public void actionPerformed(ActionEvent e) {
        changeZoomMode("MEDIUM");
    }
});
JMenuItem largeZoomItem = new JMenuItem("Large");
largeZoomItem.addActionListener(new ActionListener() {
    public void actionPerformed(ActionEvent e) {
        changeZoomMode("LARGE");
    }
});
zoomMenu.add(smallZoomItem);
zoomMenu.add(mediumZoomItem);
zoomMenu.add(largeZoomItem);
optionsMenu.add(zoomMenu);

        JMenu helpMenu = new JMenu("Help");

        JMenuItem tetoItem = new JMenuItem("???");

tetoItem.addActionListener(new ActionListener() {
    public void actionPerformed(ActionEvent e) {
        openTetoWindow();
    }
});

        helpMenu.add(tetoItem);

        JMenuItem readmeItem = new JMenuItem("Open README");
        readmeItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                openReadme();
            }
        });
        helpMenu.add(readmeItem);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(optionsMenu);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

private javax.swing.plaf.ComboBoxUI createVersionBoxUI() {
    return new javax.swing.plaf.basic.BasicComboBoxUI() {
        protected JButton createArrowButton() {
            if (darkMode) {
                javax.swing.plaf.basic.BasicArrowButton arrow =
                    new javax.swing.plaf.basic.BasicArrowButton(
                        SwingConstants.SOUTH,
                        new Color(72, 72, 72),
                        new Color(215, 215, 215),
                        new Color(240, 240, 240),
                        new Color(105, 105, 105)
                    );
                arrow.setBackground(new Color(72, 72, 72));
                arrow.setForeground(Color.WHITE);
                arrow.setBorder(BorderFactory.createLineBorder(new Color(105, 105, 105), ZoomSupport.scale(1)));
                arrow.setPreferredSize(ZoomSupport.scaleDimension(new Dimension(18, 18)));
                arrow.setFocusable(false);
                return arrow;
            }
            return super.createArrowButton();
        }
    };
}

private void refreshVersionBoxUI() {
    if (versionBox != null) {
        versionBox.setUI(createVersionBoxUI());
        versionBox.revalidate();
        versionBox.repaint();
    }
}

private void updateDarkModeMenuText() {
    if (darkModeItem != null) {
        darkModeItem.setText(darkMode ? "Disable dark mode [EXP]" : "Enable dark mode [EXP]");
    }
}

private void changeZoomMode(String requestedMode) {
    String previousZoomMode = zoomMode;
    String nextZoomMode = ZoomSupport.normalizeMode(requestedMode);

    if (previousZoomMode.equals(nextZoomMode)) {
        return;
    }

    zoomMode = nextZoomMode;
    saveOptions();

    if (!restartLauncherProcess()) {
        zoomMode = previousZoomMode;
        saveOptions();
    }
}

private boolean restartLauncherProcess() {
    try {
        File javaExecutable = new File(System.getProperty("java.home"), "bin/java");
        String executablePath = javaExecutable.isFile() ? javaExecutable.getAbsolutePath() : "java";

        String classPath = System.getProperty("java.class.path");
        if (classPath == null || classPath.trim().length() == 0) {
            throw new IOException("The current Java classpath could not be determined.");
        }

        ArrayList<String> command = new ArrayList<String>();
        command.add(executablePath);

        File launcherLocation = null;
        try {
            launcherLocation = new File(
                NineXCraftLauncher.class.getProtectionDomain().getCodeSource().getLocation().toURI()
            );
        } catch (Exception ignored) {}

        if (launcherLocation != null && launcherLocation.isFile() &&
            launcherLocation.getName().toLowerCase().endsWith(".jar")) {
            command.add("-jar");
            command.add(launcherLocation.getAbsolutePath());
        } else {
            command.add("-cp");
            command.add(classPath);
            command.add(NineXCraftLauncher.class.getName());
        }

        for (int i = 0; i < launcherStartupArguments.length; i++) {
            command.add(launcherStartupArguments[i]);
        }

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(new File(System.getProperty("user.dir")));
        builder.start();

        dispose();
        System.exit(0);
        return true;
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this,
            "Could not restart the launcher. The previous setting has been restored.\n\n" +
            e.getClass().getName() + ": " + e.getMessage(),
            "Launcher Restart Failed",
            JOptionPane.ERROR_MESSAGE);
        return false;
    }
}

private static String readOptionValue(String optionName, String defaultValue) {
    File file = new File("options.txt");
    if (!file.isFile()) {
        return defaultValue;
    }

    BufferedReader reader = null;
    try {
        reader = new BufferedReader(new FileReader(file));
        String prefix = optionName + "=";
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith(prefix)) {
                return line.substring(prefix.length()).trim();
            }
        }
    } catch (Exception ignored) {
    } finally {
        if (reader != null) {
            try { reader.close(); } catch (Exception ignored) {}
        }
    }
    return defaultValue;
}

private void showFolderWarning(String versionName) {

    folderWarningLabel.setText(
        "Folder for " + versionName + " does not exist yet."
    );

    if (folderWarningTimer != null && folderWarningTimer.isRunning()) {
        folderWarningTimer.stop();
    }

    folderWarningTimer = new Timer(5000, new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            folderWarningLabel.setText(" ");
        }
    });

    folderWarningTimer.setRepeats(false);
    folderWarningTimer.start();
}

String getBestJavaExecutable() {

    if (customJavaPath != null && customJavaPath.length() > 0) {
        return customJavaPath;
    }

    DefaultListModel model = new DefaultListModel();
    detectJavaInstallations(model);

    if (model.size() > 0) {
        return model.getElementAt(0).toString();
    }
        return "java";
}

private void openInfPaint() {
    try {
        File jar = new File("InfPaint.jar");

        if (!jar.exists()) {
            JOptionPane.showMessageDialog(this,
                "InfPaint.jar not found in launcher folder.");
            return;
        }

String javaExecut = getBestJavaExecutable();

        ProcessBuilder pb = new ProcessBuilder(
                javaExecut,
                "-jar",
                jar.getAbsolutePath()
        );

        pb.start();

    } catch (Exception e) {
        e.printStackTrace();
    }
}

private void openTetoWindow() {

    final JFrame frame = new JFrame("???");
    frame.setSize(ZoomSupport.scaleDimension(new Dimension(400, 300)));
    frame.setResizable(false);
    frame.setLocationRelativeTo(this);

    final TetoPanel panel = new TetoPanel();
    panel.setBackground(Color.BLACK);

    frame.add(panel);

    final Sequencer[] sequencerHolder = new Sequencer[1];

    Thread midiThread = new Thread(new Runnable() {
        public void run() {
            try {
                Sequencer sequencer = MidiSystem.getSequencer();
                sequencer.open();
                sequencerHolder[0] = sequencer;

                InputStream midiStream = getClass().getResourceAsStream("/s.mid");
                if (midiStream != null) {
                    Sequence sequence = MidiSystem.getSequence(midiStream);
                    sequencer.setSequence(sequence);
                    sequencer.start();
                } else {
                    System.err.println("Could not find s.mid in JAR!");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    });
    midiThread.start();

    frame.addWindowListener(new java.awt.event.WindowAdapter() {
        public void windowClosing(java.awt.event.WindowEvent e) {
            try {
                Sequencer sequencer = sequencerHolder[0];
                if (sequencer != null) {
                    if (sequencer.isRunning()) sequencer.stop();
                    sequencer.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                frame.dispose();
            }
        }
    });

    frame.setVisible(true);


    new Thread(new Runnable() {
        public void run() {
            try {
                String imageURL = fetchRandomTetoImage();
                if (imageURL != null) {
                    ImageIcon icon = new ImageIcon(new URL(imageURL));
                    panel.setImage(icon.getImage());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }).start();
}

class TetoPanel extends JPanel {

    private Image image;

    public void setImage(Image img) {
        this.image = img;
        repaint();
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (image == null)
            return;

        int panelW = getWidth();
        int panelH = getHeight();

        int imgW = image.getWidth(null);
        int imgH = image.getHeight(null);

        if (imgW <= 0 || imgH <= 0)
            return;

        double scale = Math.min(
                (double)panelW / imgW,
                (double)panelH / imgH
        );

        int newW = (int)(imgW * scale);
        int newH = (int)(imgH * scale);

        int x = (panelW - newW) / 2;
        int y = (panelH - newH) / 2;

        g.drawImage(image, x, y, newW, newH, null);
    }
}

private String fetchRandomTetoImage() {

    try {
        String api =
        "https://safebooru.org/index.php?page=dapi&s=post&q=index&tags=kasane_teto&limit=100";

        URL url = new URL(api);
        java.net.HttpURLConnection conn =
        (java.net.HttpURLConnection) url.openConnection();
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");

        BufferedReader br =
        new BufferedReader(new InputStreamReader(conn.getInputStream()));

        java.util.ArrayList links = new java.util.ArrayList();
        String line;

        while ((line = br.readLine()) != null) {
            int index = line.indexOf("file_url=\"");
            while (index != -1) {
                int start = index + 10;
                int end = line.indexOf("\"", start);
                if (end > start) {
                    String link = line.substring(start, end);
                    if (!link.startsWith("http")) {
                        link = "https:" + link;
                    }
                    links.add(link);
                }
                index = line.indexOf("file_url=\"", end);
            }
        }

        br.close();

        if (links.size() == 0)
            return null;

        int r = (int)(Math.random() * links.size());
        String selected = (String)links.get(r);
        System.out.println("Teto URL: " + selected);
        return selected;

    } catch (Exception e) {
        e.printStackTrace();
    }

    return null;
}

private void openSelectedMinecraftFolder() {
    try {
        String version = (String) versionBox.getSelectedItem();
        if (version == null || version.equals("No Versions Found"))
            return;

        if (version.toLowerCase().endsWith(".jar")) {
            version = version.substring(0, version.length() - 4);
        }

        File base = new File(System.getProperty("user.home"), ".9xcraft");
        File versionDir = new File(base, version);

        if (!versionDir.exists()) {
            showFolderWarning(version);
            return;
        }

        File dotMinecraft = new File(versionDir, ".minecraft");
        if (dotMinecraft.exists() && dotMinecraft.isDirectory()) {
            versionDir = dotMinecraft;
        }

        Runtime.getRuntime().exec(new String[] { "open", versionDir.getAbsolutePath() });
    } catch (Exception ignored) {}
}

private void openExtendedConfig() {

    final JDialog dialog = new JDialog(this, "Extended Configuration", true);
    dialog.setSize(ZoomSupport.scaleDimension(new Dimension(350, 120)));
    dialog.setResizable(false);

    JPanel panel = new JPanel() {
        Image stone = Toolkit.getDefaultToolkit().getImage(
            getClass().getResource("/0.png")
        );

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (stone != null) {
                int tileSize = ZoomSupport.scale(64);
                for (int x = 0; x < getWidth(); x += tileSize) {
                    for (int y = 0; y < getHeight(); y += tileSize) {
                        g.drawImage(stone, x, y, tileSize, tileSize, this);
                    }
                }
            }
        }
    };

    panel.setLayout(new GridLayout(2,1));
    panel.setOpaque(false);

    keepLevelDat.setForeground(Color.WHITE);
    keepLevelDat.setOpaque(false);
    standaloneMode.setForeground(Color.WHITE);
    standaloneMode.setOpaque(false);

    panel.add(keepLevelDat);
    panel.add(standaloneMode);

    dialog.setContentPane(panel);
    dialog.setLocationRelativeTo(this);
    dialog.setVisible(true);
}

private void openJarDirectly() {

    JFileChooser chooser = new JFileChooser();
    chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
        public boolean accept(File f) {
            if (f.isDirectory()) return true;
            return f.getName().toLowerCase().endsWith(".jar");
        }
        public String getDescription() {
            return "JAR Files (*.jar)";
        }
    });

    if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
        File jarFile = chooser.getSelectedFile();
        String path = jarFile.getAbsolutePath();
        addRecentJar(path);
        launchJarDirect(path);
    }
}

private void addRecentJar(String path) {
    recentJars.remove(path);
    recentJars.add(0, path);
    while (recentJars.size() > MAX_RECENT) {
        recentJars.remove(recentJars.size() - 1);
    }
    saveOptions();
}

private void openRecentWindow() {

    final JDialog dialog = new JDialog(this, "Recent JARs", true);
    dialog.setSize(ZoomSupport.scaleDimension(new Dimension(350, 150)));
    dialog.setResizable(false);
    dialog.setLocationRelativeTo(this);

    JPanel panel = new JPanel() {
        Image lightDirt = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/1.png"));
        java.net.URL darkDirtURL = getClass().getResource("/3.png");
    Image darkDirt = darkDirtURL == null ? null : Toolkit.getDefaultToolkit().getImage(darkDirtURL);

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Image dirt = darkMode && darkDirt != null ? darkDirt : lightDirt;
            int tileSize = ZoomSupport.scale(64);
            if (dirt != null) {
                for (int x = 0; x < getWidth(); x += tileSize) {
                    for (int y = 0; y < getHeight(); y += tileSize) {
                        g.drawImage(dirt, x, y, tileSize, tileSize, this);
                    }
                }
            }
        }
    };

    panel.setOpaque(false);

    DefaultListModel model = new DefaultListModel();
    for (String s : recentJars)
        model.addElement(s);

    final JList list = new JList(model) {
        Image stone = Toolkit.getDefaultToolkit().getImage(
            getClass().getResource("/0.png")
        );

        protected void paintComponent(Graphics g) {
            if (stone != null) {
                int tileSize = ZoomSupport.scale(64);
                for (int x = 0; x < getWidth(); x += tileSize) {
                    for (int y = 0; y < getHeight(); y += tileSize) {
                        g.drawImage(stone, x, y, tileSize, tileSize, this);
                    }
                }
            }
            super.paintComponent(g);
        }
    };
    list.setOpaque(false);

    list.setCellRenderer(new ListCellRenderer() {
        public Component getListCellRendererComponent(
                JList list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus) {

            OutlineLabel label = new OutlineLabel(
                    value.toString(),
                    Color.LIGHT_GRAY,
                    Color.BLACK
            );

            label.setFont(list.getFont());
            label.setBorder(BorderFactory.createEmptyBorder(ZoomSupport.scale(2), ZoomSupport.scale(4), ZoomSupport.scale(2), ZoomSupport.scale(4)));

            if (isSelected) {
                label.setBackground(new Color(80,80,80,120));
                label.setOpaque(true);
            } else {
                label.setOpaque(false);
            }
            return label;
        }
    });

    list.addMouseListener(new MouseAdapter() {
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 1) {
                String path = list.getSelectedValue().toString();
                File f = new File(path);

                if (!f.exists()) {
                    String name = f.getName();
                    folderWarningLabel.setText(
                        "File not found; " + name + " has been deleted or moved."
                    );

                    if (folderWarningTimer != null && folderWarningTimer.isRunning())
                        folderWarningTimer.stop();

                    folderWarningTimer = new Timer(5000,new ActionListener(){
                        public void actionPerformed(ActionEvent e){
                            folderWarningLabel.setText(" ");
                        }
                    });

                    folderWarningTimer.setRepeats(false);
                    folderWarningTimer.start();

                    recentJars.remove(path);
                    saveOptions();
                    return;
                }

                dialog.dispose();
                launchJarDirect(path);
            }
        }
    });

    dialog.setContentPane(panel);
    JScrollPane scroll = new JScrollPane(list);
    scroll.setOpaque(false);
    scroll.getViewport().setOpaque(false);
    scroll.setBorder(null);
    dialog.add(scroll);
    dialog.setVisible(true);
}

private void launchJarDirect(String jarPath) {
    launchInternal(jarPath);
}

private void openJavaSettings() {

    final JDialog dialog = new JDialog(this, "Java Settings", true);
    dialog.setSize(ZoomSupport.scaleDimension(new Dimension(500, 300)));
    dialog.setResizable(false);
    dialog.setLayout(new BorderLayout());

    final DefaultListModel model = new DefaultListModel();
    final JList javaList = new JList(model);
    detectJavaInstallations(model);

    if (customJavaPath != null && customJavaPath.length() > 0) {
        if (!model.contains(customJavaPath))
            model.addElement(customJavaPath);
    }

    final JTextField pathField = new JTextField();
    if (customJavaPath != null)
        pathField.setText(customJavaPath);

    JButton browse = new JButton("Browse");
    browse.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            if (chooser.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION) {
                File f = chooser.getSelectedFile();
                if (f.getName().toLowerCase().contains("java")) {
                    pathField.setText(f.getAbsolutePath());
                    if (!model.contains(f.getAbsolutePath()))
                        model.addElement(f.getAbsolutePath());
                } else {
                    JOptionPane.showMessageDialog(dialog,
                        "Selected file is not a valid java executable.");
                }
            }
        }
    });

    JPanel top = new JPanel(new BorderLayout());
    top.add(pathField, BorderLayout.CENTER);
    top.add(browse, BorderLayout.EAST);

    JButton useSelected = new JButton("Use Selected");
    JButton systemDefault = new JButton("Use System Default");

    useSelected.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            if (!javaList.isSelectionEmpty()) {
                customJavaPath = javaList.getSelectedValue().toString();
                saveJavaConfig();
                dialog.dispose();
            }
        }
    });

    systemDefault.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            customJavaPath = null;
            saveJavaConfig();
            dialog.dispose();
        }
    });

    JPanel buttons = new JPanel();
    buttons.add(useSelected);
    buttons.add(systemDefault);

    dialog.add(top, BorderLayout.NORTH);
    dialog.add(new JScrollPane(javaList), BorderLayout.CENTER);
    dialog.add(buttons, BorderLayout.SOUTH);

    dialog.setLocationRelativeTo(this);
    dialog.setVisible(true);
}

public class JvmArgsWindow extends JFrame {

    private JTextArea textArea;

    public JvmArgsWindow() {
        setTitle("JVM Arguments");
        setSize(ZoomSupport.scaleDimension(new Dimension(340, 230)));
        setResizable(false);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        textArea = new JTextArea(NineXCraftLauncher.customJvmArgs);
        textArea.setLineWrap(true);

        JScrollPane scroll = new JScrollPane(textArea);
        scroll.setPreferredSize(ZoomSupport.scaleDimension(new Dimension(300, 150)));
        add(scroll, BorderLayout.CENTER);

        JButton apply = new JButton("Apply");
        apply.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                NineXCraftLauncher.customJvmArgs = textArea.getText().trim();
                saveOptions();
                dispose();
            }
        });

        JPanel bottom = new JPanel();
        bottom.add(apply);
        add(bottom, BorderLayout.SOUTH);

        setVisible(true);
    }
}

private static boolean isSameJvmCategory(String a, String b) {
    if (a.startsWith("-Xms") && b.startsWith("-Xms")) return true;
    if (a.startsWith("-Xmx") && b.startsWith("-Xmx")) return true;

    if (a.startsWith("-D") && b.startsWith("-D")) {
        int idxA = a.indexOf('=');
        int idxB = b.indexOf('=');
        if (idxA > 2 && idxB > 2) {
            String keyA = a.substring(0, idxA);
            String keyB = b.substring(0, idxB);
            return keyA.equals(keyB);
        }
    }

    if (a.startsWith("-XX:") && b.startsWith("-XX:")) {
        return a.equals(b);
    }

    return false;
}

private void detectJavaInstallations(DefaultListModel model) {
    if (customJavaPath != null && customJavaPath.length() > 0 && !model.contains(customJavaPath)) {
        model.addElement(customJavaPath);
    }

    detectMacJavaInstallations(new File("/Library/Java/JavaVirtualMachines"), model);
    detectMacJavaInstallations(new File("/System/Library/Java/JavaVirtualMachines"), model);

    File frameworkVersions = new File("/System/Library/Frameworks/JavaVM.framework/Versions");
    File[] versions = frameworkVersions.listFiles();
    if (versions != null) {
        for (int i = 0; i < versions.length; i++) {
            File javaExec = new File(versions[i], "Home/bin/java");
            if (javaExec.isFile() && !model.contains(javaExec.getAbsolutePath())) {
                model.addElement(javaExec.getAbsolutePath());
            }
        }
    }
}

private void detectMacJavaInstallations(File base, DefaultListModel model) {
    if (!base.exists()) return;
    File[] directories = base.listFiles();
    if (directories == null) return;

    for (int i = 0; i < directories.length; i++) {
        File directory = directories[i];
        if (!directory.isDirectory()) continue;

        File javaExec = new File(directory, "Contents/Home/bin/java");
        if (javaExec.isFile() && !model.contains(javaExec.getAbsolutePath())) {
            model.addElement(javaExec.getAbsolutePath());
        }
    }
}

    private void openReadme() {
        try {
            File readme = new File("README.txt");
            if (readme.exists()) {
                Runtime.getRuntime().exec(new String[] { "open", readme.getAbsolutePath() });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveJavaConfig() {
        try {
            PrintWriter pw = new PrintWriter(new FileWriter(javaConfigFile));
            if (customJavaPath != null)
                pw.println(customJavaPath);
            pw.close();
        } catch (Exception e){}
    }

    private void loadJavaConfig() {
        if (!javaConfigFile.exists()) return;
        try {
            BufferedReader br = new BufferedReader(new FileReader(javaConfigFile));
            customJavaPath = br.readLine();
            br.close();
        } catch (Exception e){}
    }

    private String[] loadVersions() {

    File dir = new File("Versions");
    File[] files = dir.listFiles();
    if (files == null) return new String[] {"No Versions Found"};

    Vector pre = new Vector();
    Vector fourk = new Vector();
    Vector classic = new Vector();
    Vector indev = new Vector();
    Vector infdev = new Vector();
    Vector alpha = new Vector();
    Vector beta = new Vector();
    Vector release = new Vector();
    Vector other = new Vector();

    for (int i = 0; i < files.length; i++) {
        String name = files[i].getName();
        if (!name.toLowerCase().endsWith(".jar"))
            continue;
        if (name.startsWith("Pre_Classic"))
            pre.add(name);
        else if (name.startsWith("4k"))
            fourk.add(name);
        else if (name.startsWith("Classic"))
            classic.add(name);
        else if (name.startsWith("Indev"))
            indev.add(name);
        else if (name.startsWith("Infdev"))
            infdev.add(name);
        else if (name.startsWith("Alpha"))
            alpha.add(name);
        else if (name.startsWith("Beta"))
            beta.add(name);
        else if (name.startsWith("Release"))
            release.add(name);
        else
            other.add(name);
    }

    sortVector(pre);
    sortVector(fourk);
    sortVector(classic);
    sortVector(indev);
    sortVector(infdev);
    sortVector(alpha);
    sortVector(beta);
    sortVector(release);
    sortVector(other);

    Vector all = new Vector();
    all.addAll(pre);
    all.addAll(fourk);
    all.addAll(classic);
    all.addAll(indev);
    all.addAll(infdev);
    all.addAll(alpha);
    all.addAll(beta);
    all.addAll(release);
    all.addAll(other);

    if (all.size() == 0)
        all.add("No Versions Found");

    String[] arr = new String[all.size()];
    all.copyInto(arr);
    return arr;
}

private void sortVector(Vector v) {
    java.util.Collections.sort(v);
}

private String extractKey(String arg) {
    int equalsIndex = arg.indexOf("=");
    if (equalsIndex != -1) {
        return arg.substring(0, equalsIndex);
    }
    if (arg.startsWith("-Xmx"))
        return "-Xmx";
    if (arg.startsWith("-Xms"))
        return "-Xms";
    return arg;
}


private int getLocalResourceProxyPort(String versionName) {
    String name = versionName.toLowerCase();

    if (name.startsWith("classic") || name.startsWith("pre_classic") || name.startsWith("pre-classic"))
        return 11701;
    if (name.startsWith("indev") || name.startsWith("infdev"))
        return 11702;
    if (name.contains("pcgamer") || name.contains("b1.3-demo"))
        return 11704;
    if (name.startsWith("alpha 1.0.8"))
        return 11703;
    if (name.contains("beta 1.9 pre") || name.contains("b1.9-pre"))
        return 11706;
    if (name.startsWith("release "))
        return 11707;
    if (name.startsWith("alpha 1.2") || name.startsWith("alpha 1.3") ||
        name.startsWith("alpha 1.4") || name.startsWith("alpha 1.5") ||
        name.startsWith("alpha 1.6") || name.startsWith("alpha 1.7"))
        return 11705;
    if (name.startsWith("alpha "))
        return 11702;
    if (name.startsWith("beta") || name.startsWith("babric-b1."))
        return 11705;
    return 0;
}

private void reportLaunchFailure(String jarPath, Throwable failure) {
    String detail = failure.getClass().getName();
    if (failure.getMessage() != null && failure.getMessage().length() > 0) {
        detail += ": " + failure.getMessage();
    }

    String advice = "";
    if (failure instanceof NoClassDefFoundError && failure.getMessage() != null && failure.getMessage().indexOf("LocalResourceServer") >= 0) {
        advice = "\n\nLocalResourceServer.class is missing from the packaged launcher. Compile LocalResourceServer.java and include LocalResourceServer*.class in 9xCraftLauncher.jar.";
    }

    try {
        PrintWriter writer = new PrintWriter(new FileWriter("launch-error.txt"));
        writer.println("9xCraft Launcher failed to launch:");
        writer.println("JAR: " + new File(jarPath).getAbsolutePath());
        writer.println("Error: " + detail);
        writer.println();
        failure.printStackTrace(writer);
        writer.close();
    } catch (Throwable ignored) {}

    JOptionPane.showMessageDialog(this,
        "Could not launch Minecraft.\n\n" + detail + advice + "\n\nFull details were written to launch-error.txt.",
        "9xCraft Launch Error",
        JOptionPane.ERROR_MESSAGE);
}

private static File getLauncherSkinsDirectory() {
    try {
        java.security.CodeSource codeSource = NineXCraftLauncher.class.getProtectionDomain().getCodeSource();
        if (codeSource != null && codeSource.getLocation() != null) {
            File codeLocation = new File(codeSource.getLocation().toURI());
            File launcherDirectory = codeLocation.isFile() ? codeLocation.getParentFile() : codeLocation;
            if (launcherDirectory != null) {
                return new File(launcherDirectory, "Skins");
            }
        }
    } catch (Exception ignored) {}
    return new File("Skins");
}

private void openSkinsWindow() {
    SkinWindow window = new SkinWindow(this, skinsDirectory, userField.getText(), selectedSkin, new SkinWindow.SelectionListener() {
        public void skinSelected(String selection) {
            selectedSkin = selection;
            saveOptions();
        }
    });
    window.setVisible(true);
}

private byte[] getSelectedSkinBytes() throws IOException {
    byte[] skinData;

    if (selectedSkin == null || selectedSkin.trim().length() == 0 || selectedSkin.equals("DEFAULT")) {
        InputStream input = getClass().getResourceAsStream("/steve.png");
        if (input == null) {
            throw new IOException("steve.png is missing from the launcher JAR. Add it to the JAR root alongside the other launcher resources.");
        }
        skinData = readSkinBytes(input);
    } else {
        File root = skinsDirectory.getCanonicalFile();
        File skinFile = new File(skinsDirectory, selectedSkin).getCanonicalFile();
        String rootPath = root.getCanonicalPath() + File.separator;

        if (!skinFile.getPath().startsWith(rootPath) || !skinFile.isFile() || !skinFile.getName().toLowerCase().endsWith(".png")) {
            selectedSkin = "DEFAULT";
            saveOptions();
            InputStream input = getClass().getResourceAsStream("/steve.png");
            if (input == null) {
                throw new IOException("The selected skin is missing and steve.png is not packaged in the launcher JAR.");
            }
            skinData = readSkinBytes(input);
        } else {
            java.awt.image.BufferedImage image = ImageIO.read(skinFile);
            if (image == null || image.getWidth() != 64 || (image.getHeight() != 64 && image.getHeight() != 32)) {
                throw new IOException("The selected skin must be a PNG measuring exactly 64x64 or 64x32 pixels: " + skinFile.getName());
            }
            skinData = readSkinBytes(new FileInputStream(skinFile));
        }
    }

    java.awt.image.BufferedImage checkedImage = ImageIO.read(new ByteArrayInputStream(skinData));
    if (checkedImage == null || checkedImage.getWidth() != 64 || (checkedImage.getHeight() != 64 && checkedImage.getHeight() != 32)) {
        throw new IOException("The active skin must be a PNG measuring exactly 64x64 or 64x32 pixels.");
    }

    return skinData;
}

private byte[] readSkinBytes(InputStream input) throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try {
        byte[] buffer = new byte[8192];
        int count;
        while ((count = input.read(buffer)) != -1) {
            output.write(buffer, 0, count);
        }
    } finally {
        input.close();
    }
    return output.toByteArray();
}

private void launchInternal(final String jarPath) {
    LocalResourceServer resourceServer = null;

    try {
        File selectedJar = new File(jarPath);
        if (!selectedJar.isFile()) {
            throw new IOException("Selected Minecraft JAR was not found: " + selectedJar.getAbsolutePath());
        }

        File nativesDir = new File("natives");
        String absNatives = nativesDir.getAbsolutePath();

        String javaExec = (customJavaPath != null && customJavaPath.length() > 0)
                ? customJavaPath : "java";

        Vector cmd = new Vector();
        cmd.add(javaExec);

        String jarFileName = new File(jarPath).getName();
        String jarNameWithoutExtension = jarFileName.substring(0, jarFileName.length() - 4);

        File base = new File(System.getProperty("user.home"), ".9xcraft");
        base.mkdirs();
        File versionDir = new File(base, jarNameWithoutExtension);
        versionDir.mkdirs();

        File resourcesDir = new File(new File(versionDir, ".minecraft"), "resources");
        if (!resourcesDir.isDirectory()) {
            File directResourcesDir = new File(versionDir, "resources");
            if (directResourcesDir.isDirectory()) {
                resourcesDir = directResourcesDir;
            }
        }

        byte[] activeSkinBytes = getSelectedSkinBytes();
        resourceServer = new LocalResourceServer(
            resourcesDir,
            getLocalResourceProxyPort(jarNameWithoutExtension),
            userField.getText(),
            activeSkinBytes,
            "betacraft.ovh",
            80
        );
        final LocalResourceServer activeResourceServer = resourceServer;

        cmd.add("-Duser.home=" + versionDir.getAbsolutePath());
        cmd.add("-Dminecraft.gameDir=" + versionDir.getAbsolutePath());
        cmd.add("-D9xcraft.darkMode=" + String.valueOf(darkMode));
        cmd.add("-D9xcraft.zoom=" + zoomMode);

        ArrayList baseJvmArgs = new ArrayList();
        baseJvmArgs.add("-Xms" + minMemField.getText() + "M");
        baseJvmArgs.add("-Xmx" + maxMemField.getText() + "M");
        baseJvmArgs.add("-Djava.library.path=" + absNatives);
        baseJvmArgs.add("-Dorg.lwjgl.librarypath=" + absNatives);
        baseJvmArgs.add("-Dorg.lwjgl.util.NoChecks=true");
        baseJvmArgs.add("-Dsun.awt.disablegrab=true");
        baseJvmArgs.add("-Djava.awt.headless=false");
        baseJvmArgs.add("-XstartOnFirstThread");
        baseJvmArgs.add("-Dapple.awt.UIElement=true");
        baseJvmArgs.add("-Djava.util.Arrays.useLegacyMergeSort=true");
        baseJvmArgs.add("-Dhttp.proxyHost=127.0.0.1");
        baseJvmArgs.add("-Dhttp.proxyPort=" + activeResourceServer.getPort());
        baseJvmArgs.add("-Dnet.java.games.input.useDefaultPlugin=false");

        for (int i = 0; i < baseJvmArgs.size(); i++) {
            cmd.add(baseJvmArgs.get(i));
        }

        if (softwareGL.isSelected())
            cmd.add("-Dorg.lwjgl.opengl.Display.allowSoftwareOpenGL=true");

        if (noDDraw.isSelected())
            cmd.add("-Dsun.java2d.noddraw=true");

        Set<String> seenKeys = new HashSet<String>();
        Set<String> duplicateKeys = new HashSet<String>();

        for (int i = 0; i < cmd.size(); i++) {
            String arg = (String) cmd.get(i);
            if (arg.startsWith("-")) {
                String key = extractKey(arg);
                if (seenKeys.contains(key)) duplicateKeys.add(key);
                else seenKeys.add(key);
            }
        }

        String customArgs = NineXCraftLauncher.customJvmArgs;
        if (customArgs != null && customArgs.trim().length() > 0) {
            String[] parts = customArgs.trim().split("\\s+");
            for (int i = 0; i < parts.length; i++) {
                String arg = parts[i];
                String lowerArg = arg.toLowerCase();

                if (lowerArg.startsWith("-dhttp.proxy") || lowerArg.startsWith("-dhttp.nonproxyhosts")) {
                    continue;
                }

                if (arg.startsWith("-")) {
                    String key = extractKey(arg);
                    if (seenKeys.contains(key)) duplicateKeys.add(key);
                    else seenKeys.add(key);
                }
                cmd.add(arg);
            }
        }

        String sep = System.getProperty("path.separator");

        if (standaloneMode.isSelected()) {
            cmd.add("-cp");
            cmd.add(jarPath + sep + "9xCraftLauncher.jar" + sep + "lwjgl.jar" + sep + "lwjgl_util.jar" + sep + "jinput.jar");

            String mainClass = null;
            try {
                JarFile jarFile = new JarFile(jarPath);
                Manifest manifest = jarFile.getManifest();
                if (manifest != null) mainClass = manifest.getMainAttributes().getValue("Main-Class");
                jarFile.close();
            } catch (Exception ignored) {}

            if (mainClass == null || mainClass.trim().length() == 0) {
                try {
                    URL[] urls = { new File(jarPath).toURI().toURL() };
                    URLClassLoader loader = new URLClassLoader(urls);
                    String[] fallbacks = {
                        "net.minecraft.client.Minecraft",
                        "net.minecraft.client.MinecraftApplet",
                        "com.mojang.minecraft.MinecraftApplet",
                        "M"
                    };
                    for (int i = 0; i < fallbacks.length; i++) {
                        try {
                            loader.loadClass(fallbacks[i]);
                            mainClass = fallbacks[i];
                            break;
                        } catch (ClassNotFoundException ignored) {}
                    }
                } catch (Exception ignored) {}
            }

            if (mainClass == null) mainClass = "net.minecraft.client.Minecraft";
            cmd.add(mainClass);
            cmd.add(userField.getText());
            cmd.add(widthField.getText());
            cmd.add(heightField.getText());
        } else {
            cmd.add("-cp");
            cmd.add("9xCraftLauncher.jar" + sep + "lwjgl.jar" + sep + "lwjgl_util.jar" + sep + "jinput.jar");
            cmd.add("AppletRunner");
            cmd.add(jarPath);
            cmd.add(userField.getText());
            cmd.add(widthField.getText());
            cmd.add(heightField.getText());
            cmd.add(String.valueOf(fullscreenBox.isSelected()));
        }

        ProcessBuilder pb = new ProcessBuilder(cmd);
        Map<String, String> env = pb.environment();
        env.put("APPDATA", versionDir.getAbsolutePath());

        final Process process = pb.start();

        new Thread(new Runnable() {
            public void run() {
                try {
                    process.waitFor();
                    for (int i = 0; i < 5; i++) {
                        Thread.sleep(1000);
                        File level = new File("level.dat");
                        if (level.exists() && !keepLevelDat.isSelected()) {
                            level.delete();
                            break;
                        }
                    }
                } catch (Exception ignored) {
                } finally {
                    activeResourceServer.stop();
                }
            }
        }, "9xCraft PowerPC Local Resource Server Cleanup").start();

        keepLogWindowGlobal = keepLogOpen.isSelected();
        LogWindow log = new LogWindow(process, this);
        log.appendExternalMessage("Launcher: Serving local resources from " + resourcesDir.getAbsolutePath(), Color.GRAY);
        log.appendExternalMessage("Launcher: Unhandled HTTP requests are forwarded through Betacraft.", Color.GRAY);
        if (!activeResourceServer.isUsingPreferredPort()) {
            log.appendExternalMessage("Launcher: Preferred local resource port " + activeResourceServer.getPreferredPort() + " was busy; using " + activeResourceServer.getPort() + " instead.", Color.ORANGE);
        }

        if (!duplicateKeys.isEmpty()) {
            log.appendExternalMessage("One or more JVM arguments are being called multiple times at once:", Color.ORANGE);
            for (String key : duplicateKeys) {
                log.appendExternalMessage(" - " + key, Color.ORANGE);
            }
        }
    } catch (Throwable e) {
        if (resourceServer != null) resourceServer.stop();
        reportLaunchFailure(jarPath, e);
    }
}

   private void launchGame() {
    String version = (String) versionBox.getSelectedItem();
    if (version == null || version.equals("No Versions Found"))
        return;
    String jarPath = "Versions/" + version;
    launchInternal(jarPath);
}

    private void saveOptions() {
        try {
            PrintWriter pw = new PrintWriter(new FileWriter(optionsFile));
            pw.println("version=" + versionBox.getSelectedItem());
            pw.println("username=" + userField.getText());
            pw.println("width=" + widthField.getText());
            pw.println("height=" + heightField.getText());
            pw.println("minMem=" + minMemField.getText());
            pw.println("maxMem=" + maxMemField.getText());
            pw.println("softwareGL=" + softwareGL.isSelected());
            pw.println("noDDraw=" + noDDraw.isSelected());
            pw.println("keepLogOpen=" + keepLogOpen.isSelected());
            pw.println("fullscreen=" + fullscreenBox.isSelected());
            pw.println("keepLevelDat=" + keepLevelDat.isSelected());
            pw.println("standaloneMode=" + standaloneMode.isSelected());
            pw.println("customJvmArgs=" + customJvmArgs);
            pw.println("skin=" + selectedSkin);
            pw.println("darkMode=" + darkMode);
            pw.println("zoom=" + zoomMode);
            for (int i=0;i<recentJars.size();i++) {
                pw.println("recentJar=" + recentJars.get(i));
            }
            pw.close();
        } catch(Exception e){}
    }

    private void loadOptions() {
        if (!optionsFile.exists()) return;

        try {
            BufferedReader br = new BufferedReader(new FileReader(optionsFile));
            String line;
            while ((line=br.readLine())!=null) {
                if (line.startsWith("version="))
                    versionBox.setSelectedItem(line.substring(8));
                if (line.startsWith("username="))
                    userField.setText(line.substring(9));
                if (line.startsWith("width="))
                    widthField.setText(line.substring(6));
                if (line.startsWith("height="))
                    heightField.setText(line.substring(7));
                if (line.startsWith("minMem="))
                    minMemField.setText(line.substring(7));
                if (line.startsWith("maxMem="))
                    maxMemField.setText(line.substring(7));
                if (line.startsWith("softwareGL="))
                    softwareGL.setSelected(Boolean.valueOf(line.substring(11)).booleanValue());
                if (line.startsWith("noDDraw="))
                    noDDraw.setSelected(Boolean.valueOf(line.substring(8)).booleanValue());
                if (line.startsWith("keepLogOpen="))
                    keepLogOpen.setSelected(Boolean.valueOf(line.substring(12)).booleanValue());
                if (line.startsWith("fullscreen="))
                    fullscreenBox.setSelected(Boolean.valueOf(line.substring(11)).booleanValue());
                if (line.startsWith("keepLevelDat="))
                    keepLevelDat.setSelected(Boolean.valueOf(line.substring(13)).booleanValue());
                if (line.startsWith("standaloneMode="))
                    standaloneMode.setSelected(Boolean.valueOf(line.substring(15)).booleanValue());
                if (line.startsWith("customJvmArgs="))
                    customJvmArgs = line.substring(14);
                if (line.startsWith("skin="))
                    selectedSkin = line.substring(5);
                if (line.startsWith("darkMode="))
                    darkMode = Boolean.valueOf(line.substring(9)).booleanValue();
                if (line.startsWith("zoom="))
                    zoomMode = ZoomSupport.normalizeMode(line.substring(5));
                if (line.startsWith("recentJar="))
                    recentJars.add(line.substring(10));
            }
            br.close();
        } catch(Exception e){}
    }

class OutlineLabel extends JLabel {

    private Color textColor;
    private Color outlineColor;

    public OutlineLabel(String text) {
        this(text, Color.WHITE, Color.BLACK);
    }

    public OutlineLabel(String text, Color textColor, Color outlineColor) {
        super(text);
        this.textColor = textColor;
        this.outlineColor = outlineColor;
        setOpaque(false);
    }

    protected void paintComponent(Graphics g) {
        String text = getText();
        Graphics2D g2 = (Graphics2D) g;
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        int x = 0;
        int y = fm.getAscent();
        g2.setColor(outlineColor);
        g2.drawString(text, x - 1, y);
        g2.drawString(text, x + 1, y);
        g2.drawString(text, x, y - 1);
        g2.drawString(text, x, y + 1);
        g2.setColor(textColor);
        g2.drawString(text, x, y);
    }
}

private class StoneField extends JTextField {

    private Image stone;

    public StoneField(String text) {
        super(text);
        stone = new ImageIcon(
                getClass().getResource("/stone_box.jpg")
        ).getImage();
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(ZoomSupport.scale(5), ZoomSupport.scale(5), ZoomSupport.scale(5), ZoomSupport.scale(5)));
        setForeground(Color.WHITE);
        setCaretColor(Color.WHITE);
        setSelectionColor(new Color(100, 100, 100, 120));
        setSelectedTextColor(Color.WHITE);
    }

    protected void paintComponent(Graphics g) {
        if (stone != null)
            g.drawImage(stone, 0, 0, getWidth(), getHeight(), this);
        super.paintComponent(g);
    }
}

class ShadowPanel extends JPanel {

    private Image shadow;
    private boolean flip;

    public ShadowPanel(String shadowPath, boolean flipVertical) {
        this.shadow = new ImageIcon(getClass().getResource(shadowPath)).getImage();
        this.flip = flipVertical;
        setOpaque(false);
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int width = getWidth();
        int height = getHeight();
        int shadowHeight = ZoomSupport.scale(shadow.getHeight(null));
        Graphics2D g2 = (Graphics2D) g;
        if (!flip) {
            g2.drawImage(shadow, 0, height - shadowHeight, width, shadowHeight, null);
        } else {
            g2.drawImage(shadow,
                    0, 0, width, shadowHeight,
                    0, shadow.getHeight(null), 1, 0,
                    null);
        }
    }
}

    public static void main(String[] args) {
        launcherStartupArguments = args == null ? new String[0] : (String[]) args.clone();
        new NineXCraftLauncher();
    }
}


class DarkMode {
    private static boolean enabled;

    private DarkMode() {}

    public static boolean isEnabled() {
        return enabled;
    }

    public static void apply(boolean useDarkMode) {
        enabled = useDarkMode;
        if (!useDarkMode) return;

        Color background = new Color(32, 32, 32);
        Color panel = new Color(38, 38, 38);
        Color button = new Color(57, 57, 57);
        Color input = new Color(48, 48, 48);
        Color foreground = new Color(235, 235, 235);
        Color muted = new Color(165, 165, 165);
        Color selection = new Color(75, 91, 112);
        Color border = new Color(75, 75, 75);

        UIManager.put("control", button);
        UIManager.put("info", new Color(45, 45, 45));
        UIManager.put("nimbusBase", new Color(55, 55, 55));
        UIManager.put("nimbusBlueGrey", new Color(65, 65, 65));
        UIManager.put("nimbusLightBackground", input);
        UIManager.put("Panel.background", panel);
        UIManager.put("Panel.foreground", foreground);
        UIManager.put("Label.foreground", foreground);
        UIManager.put("Button.background", button);
        UIManager.put("Button.foreground", foreground);
        UIManager.put("ToggleButton.background", button);
        UIManager.put("ToggleButton.foreground", foreground);
        UIManager.put("CheckBox.background", panel);
        UIManager.put("CheckBox.foreground", foreground);
        UIManager.put("RadioButton.background", panel);
        UIManager.put("RadioButton.foreground", foreground);
        UIManager.put("TextField.background", input);
        UIManager.put("TextField.foreground", foreground);
        UIManager.put("TextField.caretForeground", foreground);
        UIManager.put("TextField.selectionBackground", selection);
        UIManager.put("TextField.selectionForeground", Color.WHITE);
        UIManager.put("TextArea.background", input);
        UIManager.put("TextArea.foreground", foreground);
        UIManager.put("TextPane.background", input);
        UIManager.put("TextPane.foreground", foreground);
        UIManager.put("ComboBox.background", input);
        UIManager.put("ComboBox.foreground", foreground);
        UIManager.put("ComboBox.selectionBackground", selection);
        UIManager.put("ComboBox.selectionForeground", Color.WHITE);
        UIManager.put("List.background", input);
        UIManager.put("List.foreground", foreground);
        UIManager.put("List.selectionBackground", selection);
        UIManager.put("List.selectionForeground", Color.WHITE);
        UIManager.put("ScrollPane.background", panel);
        UIManager.put("Viewport.background", panel);
        UIManager.put("MenuBar.background", panel);
        UIManager.put("MenuBar.foreground", foreground);
        UIManager.put("Menu.background", panel);
        UIManager.put("Menu.foreground", foreground);
        UIManager.put("MenuItem.background", panel);
        UIManager.put("MenuItem.foreground", foreground);
        UIManager.put("PopupMenu.background", panel);
        UIManager.put("OptionPane.background", panel);
        UIManager.put("OptionPane.messageForeground", foreground);
        UIManager.put("ToolTip.background", button);
        UIManager.put("ToolTip.foreground", foreground);
        UIManager.put("TitledBorder.titleColor", foreground);
        UIManager.put("Separator.foreground", border);
    }
}

class ZoomSupport {
    private static float factor = 1.0f;
    private static String mode = "MEDIUM";
    private static boolean configured = false;

    private ZoomSupport() {}

    public static synchronized void configure(String requestedMode) {
        if (configured) {
            return;
        }

        String normalized = normalizeMode(requestedMode);
        mode = normalized;

        if ("SMALL".equals(normalized)) {
            factor = 0.70f;
        } else if ("LARGE".equals(normalized)) {
            factor = 1.25f;
        } else {
            factor = 1.0f;
        }

        System.setProperty("9xcraft.zoom", mode);

        if (factor != 1.0f) {
            scaleSwingDefaults();
        }

        configured = true;
    }

    public static void configureFromSystemProperty() {
        configure(System.getProperty("9xcraft.zoom", "MEDIUM"));
    }

    public static String normalizeMode(String value) {
        if (value == null) {
            return "MEDIUM";
        }
        String normalized = value.trim().toUpperCase();
        if (normalized.equals("SMALL") || normalized.equals("LARGE")) {
            return normalized;
        }
        return "MEDIUM";
    }

    public static String getMode() {
        return mode;
    }

    public static float getFactor() {
        return factor;
    }

    public static int scale(int value) {
        int scaled = Math.round(value * factor);
        if (value > 0 && scaled < 1) {
            return 1;
        }
        return scaled;
    }

    public static float scale(float value) {
        return value * factor;
    }

    public static Dimension scaleDimension(Dimension value) {
        if (value == null) {
            return null;
        }
        return new Dimension(scale(value.width), scale(value.height));
    }

    public static Insets scaleInsets(Insets value) {
        if (value == null) {
            return null;
        }
        return new Insets(
            scale(value.top),
            scale(value.left),
            scale(value.bottom),
            scale(value.right)
        );
    }

    public static Font scaleFont(Font font) {
        if (font == null || factor == 1.0f) {
            return font;
        }
        return font.deriveFont(Math.max(1.0f, font.getSize2D() * factor));
    }

    public static ImageIcon scaleImageIcon(ImageIcon icon) {
        if (icon == null || factor == 1.0f) {
            return icon;
        }
        int width = icon.getIconWidth();
        int height = icon.getIconHeight();
        if (width <= 0 || height <= 0) {
            return icon;
        }
        int scaledWidth = scale(width);
        int scaledHeight = scale(height);
        return new ImageIcon(icon.getImage().getScaledInstance(
            scaledWidth,
            scaledHeight,
            java.awt.Image.SCALE_SMOOTH
        ));
    }

    public static void scaleAwtWindow(Window window) {
        if (window == null || factor == 1.0f) {
            return;
        }

        scaleAwtComponentTree(window);
        window.setSize(scaleDimension(window.getSize()));
        window.validate();
    }

    public static void scaleAwtComponentTree(Component root) {
        if (root == null || factor == 1.0f) {
            return;
        }

        Map<Component, Font> originalFonts = new IdentityHashMap<Component, Font>();
        collectAwtFonts(root, originalFonts);

        for (Map.Entry<Component, Font> entry : originalFonts.entrySet()) {
            if (entry.getValue() != null) {
                entry.getKey().setFont(scaleFont(entry.getValue()));
            }
        }
    }

    private static void collectAwtFonts(Component component, Map<Component, Font> fonts) {
        if (component == null) {
            return;
        }
        fonts.put(component, component.getFont());
        if (component instanceof java.awt.Container) {
            Component[] children = ((java.awt.Container)component).getComponents();
            for (int i = 0; i < children.length; i++) {
                collectAwtFonts(children[i], fonts);
            }
        }
    }

    private static void scaleSwingDefaults() {
        UIDefaults defaults = UIManager.getDefaults();
        Enumeration keys = defaults.keys();

        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = defaults.get(key);

            if (value instanceof Font) {
                defaults.put(key, scaleFont((Font)value));
            } else if (value instanceof Insets) {
                defaults.put(key, scaleInsets((Insets)value));
            } else if (value instanceof Dimension) {
                defaults.put(key, scaleDimension((Dimension)value));
            } else if (value instanceof ImageIcon) {
                defaults.put(key, scaleImageIcon((ImageIcon)value));
            }
        }
    }
}

class SkinWindow extends JDialog {

    public interface SelectionListener {
        void skinSelected(String selection);
    }

    private static final String DEFAULT_SKIN = "DEFAULT";
    private static final int MAX_SKIN_BYTES = 2097152;

    private final File skinsDirectory;
    private final String playerName;
    private final SelectionListener listener;
    private final JPanel skinGrid;
    private final JLabel statusLabel;
    private final JButton fetchButton;
    private String selectedSkin;

    public SkinWindow(JFrame parent, File directory, String username, String currentSelection, SelectionListener selectionListener) {
        super(parent, "Skins", true);
        skinsDirectory = directory;
        playerName = username == null ? "" : username.trim();
        listener = selectionListener;
        selectedSkin = currentSelection == null || currentSelection.trim().length() == 0 ? DEFAULT_SKIN : currentSelection;

        setSize(ZoomSupport.scaleDimension(new Dimension(560, 360)));
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(ZoomSupport.scale(8), ZoomSupport.scale(8)));

        if (!skinsDirectory.exists()) {
            skinsDirectory.mkdirs();
        }

        JLabel heading = new JLabel("Select a skin");
        heading.setBorder(BorderFactory.createEmptyBorder(ZoomSupport.scale(10), ZoomSupport.scale(10), 0, ZoomSupport.scale(10)));
        heading.setFont(ZoomSupport.scaleFont(heading.getFont().deriveFont(Font.BOLD, 14.0f)));
        add(heading, BorderLayout.NORTH);

        skinGrid = new JPanel(new GridLayout(2, 0, ZoomSupport.scale(8), ZoomSupport.scale(8)));
        skinGrid.setBorder(BorderFactory.createEmptyBorder(ZoomSupport.scale(8), ZoomSupport.scale(8), ZoomSupport.scale(8), ZoomSupport.scale(8)));
        skinGrid.setBackground(themePanelColor());

        JScrollPane scroll = new JScrollPane(skinGrid);
        scroll.setBorder(BorderFactory.createEtchedBorder());
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        add(scroll, BorderLayout.CENTER);

        statusLabel = new JLabel("Only PNG skins sized 64x64 or 64x32 are shown.");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(ZoomSupport.scale(4), ZoomSupport.scale(8), ZoomSupport.scale(4), ZoomSupport.scale(4)));

        fetchButton = new JButton("Fetch skin from minecraft.net");
        fetchButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                fetchSkin();
            }
        });

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(statusLabel, BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, ZoomSupport.scale(8), ZoomSupport.scale(4)));
        buttonPanel.add(fetchButton);
        bottom.add(buttonPanel, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        refreshSkins();
        setLocationRelativeTo(parent);
    }

    public void applyTheme() {
        skinGrid.setBackground(themePanelColor());
        refreshSkins();
    }

    private Color themePanelColor() {
        Color color = UIManager.getColor("Panel.background");
        return color == null ? Color.WHITE : color;
    }

    private void refreshSkins() {
        skinGrid.removeAll();

        BufferedImage steve = readBundledSteve();
        File[] files = skinsDirectory.listFiles();
        List<File> skinFiles = new ArrayList<File>();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                if (!file.isFile() || !file.getName().toLowerCase().endsWith(".png") || file.getName().equalsIgnoreCase("steve.png")) {
                    continue;
                }
                BufferedImage image = readValidSkin(file);
                if (image != null) {
                    skinFiles.add(file);
                }
            }
        }

        Collections.sort(skinFiles, new Comparator<File>() {
            public int compare(File first, File second) {
                return first.getName().compareToIgnoreCase(second.getName());
            }
        });

        boolean selectionFound = DEFAULT_SKIN.equals(selectedSkin);
        for (int i = 0; i < skinFiles.size(); i++) {
            if (skinFiles.get(i).getName().equals(selectedSkin)) {
                selectionFound = true;
                break;
            }
        }
        if (!selectionFound) {
            selectedSkin = DEFAULT_SKIN;
            if (listener != null) {
                listener.skinSelected(selectedSkin);
            }
        }

        addSkinTile("Steve (Default)", DEFAULT_SKIN, steve, null);

        for (int i = 0; i < skinFiles.size(); i++) {
            File file = skinFiles.get(i);
            BufferedImage image = readValidSkin(file);
            if (image != null) {
                addSkinTile(file.getName(), file.getName(), image, file);
            }
        }

        while (skinGrid.getComponentCount() < 8) {
            JPanel emptyTile = new JPanel();
            emptyTile.setOpaque(false);
            emptyTile.setPreferredSize(ZoomSupport.scaleDimension(new Dimension(116, 116)));
            skinGrid.add(emptyTile);
        }

        skinGrid.revalidate();
        skinGrid.repaint();
    }

    private BufferedImage readBundledSteve() {
        InputStream input = getClass().getResourceAsStream("/steve.png");
        if (input == null) {
            return null;
        }
        try {
            BufferedImage image = ImageIO.read(input);
            if (!isValidDimensions(image)) {
                return null;
            }
            return image;
        } catch (Exception e) {
            return null;
        } finally {
            try { input.close(); } catch (Exception ignored) {}
        }
    }

    private BufferedImage readValidSkin(File file) {
        try {
            BufferedImage image = ImageIO.read(file);
            return isValidDimensions(image) ? image : null;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isValidDimensions(BufferedImage image) {
        return image != null && image.getWidth() == 64 && (image.getHeight() == 64 || image.getHeight() == 32);
    }

    private void addSkinTile(String label, final String selection, BufferedImage image, final File skinFile) {
        final boolean isSelected = selection.equals(selectedSkin);
        final JPanel tile = new JPanel(new BorderLayout(0, ZoomSupport.scale(2)));
        tile.setPreferredSize(ZoomSupport.scaleDimension(new Dimension(116, 116)));
        tile.setMinimumSize(ZoomSupport.scaleDimension(new Dimension(116, 116)));
        boolean dark = DarkMode.isEnabled();
        Color panelColor = themePanelColor();
        tile.setBackground(isSelected
            ? (dark ? new Color(48, 65, 85) : new Color(215, 232, 255))
            : panelColor);
        Color normalBorder = UIManager.getColor("controlShadow");
        if (normalBorder == null) {
            normalBorder = dark ? new Color(85, 85, 85) : Color.LIGHT_GRAY;
        }
        tile.setBorder(BorderFactory.createLineBorder(
            isSelected ? (dark ? new Color(105, 160, 225) : new Color(45, 105, 185)) : normalBorder,
            ZoomSupport.scale(isSelected ? 3 : 1)
        ));

        JButton selectButton = new JButton(wrapSkinLabel(label));
        selectButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        selectButton.setHorizontalTextPosition(SwingConstants.CENTER);
        selectButton.setHorizontalAlignment(SwingConstants.CENTER);
        selectButton.setVerticalAlignment(SwingConstants.CENTER);
        selectButton.setPreferredSize(ZoomSupport.scaleDimension(new Dimension(112, 90)));
        selectButton.setFocusPainted(false);
        selectButton.setBorderPainted(false);
        selectButton.setContentAreaFilled(false);
        selectButton.setOpaque(false);
        selectButton.setMargin(ZoomSupport.scaleInsets(new Insets(2, 2, 2, 2)));
        selectButton.setIconTextGap(ZoomSupport.scale(2));

        if (image != null) {
            int maxWidth = ZoomSupport.scale(44);
            int maxHeight = ZoomSupport.scale(44);
            double scale = Math.min((double)maxWidth / image.getWidth(), (double)maxHeight / image.getHeight());
            int width = Math.max(1, (int)(image.getWidth() * scale));
            int height = Math.max(1, (int)(image.getHeight() * scale));
            Image preview = image.getScaledInstance(width, height, Image.SCALE_FAST);
            selectButton.setIcon(new ImageIcon(preview));
        } else {
            selectButton.setText(wrapSkinLabel(label + " (missing steve.png)"));
            selectButton.setEnabled(false);
        }

        selectButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                selectedSkin = selection;
                if (listener != null) {
                    listener.skinSelected(selectedSkin);
                }
                statusLabel.setText("Selected: " + (DEFAULT_SKIN.equals(selection) ? "Steve (Default)" : selection));
                refreshSkins();
            }
        });

        tile.add(selectButton, BorderLayout.CENTER);

        if (skinFile != null) {
            JButton deleteButton = new JButton("DELETE");
            deleteButton.setFont(ZoomSupport.scaleFont(deleteButton.getFont().deriveFont(Font.BOLD, 9.0f)));
            deleteButton.setMargin(ZoomSupport.scaleInsets(new Insets(1, 2, 1, 2)));
            deleteButton.setFocusPainted(false);
            deleteButton.setPreferredSize(ZoomSupport.scaleDimension(new Dimension(112, 22)));
            deleteButton.setToolTipText("Delete " + label);
            deleteButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    deleteSkin(skinFile, selection);
                }
            });
            tile.add(deleteButton, BorderLayout.SOUTH);
        } else {
            JPanel spacer = new JPanel();
            spacer.setOpaque(false);
            spacer.setPreferredSize(ZoomSupport.scaleDimension(new Dimension(112, 22)));
            tile.add(spacer, BorderLayout.SOUTH);
        }

        skinGrid.add(tile);
    }

    private String wrapSkinLabel(String label) {
        StringBuilder html = new StringBuilder("<html><div style='text-align:center;width:" + ZoomSupport.scale(96) + "px'>");
        int lineLength = 0;
        for (int i = 0; i < label.length(); i++) {
            char c = label.charAt(i);
            if (lineLength >= 13) {
                html.append("<br>");
                lineLength = 0;
            }
            if (c == '&') {
                html.append("&amp;");
            } else if (c == '<') {
                html.append("&lt;");
            } else if (c == '>') {
                html.append("&gt;");
            } else if (c == '"') {
                html.append("&quot;");
            } else {
                html.append(c);
            }
            lineLength++;
        }
        html.append("</div></html>");
        return html.toString();
    }

    private void deleteSkin(File skinFile, String selection) {
        int choice = JOptionPane.showConfirmDialog(
            this,
            "Delete '" + skinFile.getName() + "'? This cannot be undone.",
            "Delete Skin",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            File root = skinsDirectory.getCanonicalFile();
            File target = skinFile.getCanonicalFile();
            String rootPath = root.getPath() + File.separator;

            if (!target.getPath().startsWith(rootPath) || !target.isFile()) {
                throw new IOException("The selected skin file is no longer available in the Skins folder.");
            }

            if (!target.delete()) {
                throw new IOException("The skin file could not be deleted. It may have already been removed or may be locked by another program.");
            }

            if (selection.equals(selectedSkin)) {
                selectedSkin = DEFAULT_SKIN;
                if (listener != null) {
                    listener.skinSelected(selectedSkin);
                }
            }

            statusLabel.setText("Deleted: " + skinFile.getName());
            refreshSkins();
        } catch (Exception e) {
            String message = e.getMessage() == null ? e.toString() : e.getMessage();
            JOptionPane.showMessageDialog(
                this,
                "Could not delete the skin.\n\n" + message,
                "Skin Delete Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void fetchSkin() {
        if (playerName.length() == 0) {
            JOptionPane.showMessageDialog(this, "Enter a Minecraft username in the launcher first.", "Skin Fetch Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!playerName.matches("[A-Za-z0-9_]{1,16}")) {
            JOptionPane.showMessageDialog(this, "The launcher username is not a valid Minecraft username.", "Skin Fetch Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        fetchButton.setEnabled(false);
        statusLabel.setText("Fetching skin for " + playerName + "...");

        Thread worker = new Thread(new Runnable() {
            public void run() {
                String savedName = null;
                String error = null;
                try {
                    savedName = downloadPlayerSkin(playerName);
                } catch (Exception e) {
                    error = e.getMessage() == null ? e.toString() : e.getMessage();
                }

                final String resultName = savedName;
                final String resultError = error;
                javax.swing.SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        fetchButton.setEnabled(true);
                        if (resultName != null) {
                            selectedSkin = resultName;
                            if (listener != null) {
                                listener.skinSelected(selectedSkin);
                            }
                            statusLabel.setText("Fetched and selected " + resultName);
                            refreshSkins();
                        } else {
                            statusLabel.setText("Skin fetch failed.");
                            JOptionPane.showMessageDialog(SkinWindow.this,
                                resultError == null ? "The skin could not be downloaded." : resultError,
                                "Skin Fetch Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                });
            }
        }, "9xCraft Skin Fetch");
        worker.setDaemon(true);
        worker.start();
    }

    private String downloadPlayerSkin(String username) throws Exception {
        String encodedName = URLEncoder.encode(username, "UTF-8");
        byte[] pngData = null;
        IOException firstFailure = null;

        String[] legacyUrls = new String[] {
            "http://skins.minecraft.net/MinecraftSkins/" + encodedName + ".png",
            "http://s3.amazonaws.com/MinecraftSkins/" + encodedName + ".png"
        };

        for (int i = 0; i < legacyUrls.length; i++) {
            HttpURLConnection connection = null;
            try {
                URL skinUrl = new URL(legacyUrls[i]);
                connection = (HttpURLConnection)skinUrl.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(12000);
                connection.setReadTimeout(15000);
                connection.setInstanceFollowRedirects(true);
                connection.setRequestProperty("User-Agent", "9xCraft PowerPC Launcher");
                connection.setRequestProperty("Accept", "image/png,*/*");

                int status = connection.getResponseCode();
                if (status == 200) {
                    pngData = readBytes(connection.getInputStream(), MAX_SKIN_BYTES);
                    BufferedImage check = ImageIO.read(new java.io.ByteArrayInputStream(pngData));
                    if (isValidDimensions(check)) {
                        break;
                    }
                    throw new IOException("The legacy skin server did not return a supported 64x64 or 64x32 PNG.");
                }

                if (status == 404 || status == 403) {
                    firstFailure = new IOException("No downloadable skin was found for '" + username + "'.");
                } else {
                    firstFailure = new IOException("The legacy skin server returned HTTP " + status + ".");
                }
            } catch (IOException e) {
                firstFailure = e;
            } finally {
                if (connection != null) connection.disconnect();
            }

            if (pngData != null) break;
        }

        if (pngData == null) {
            String failureDetail = firstFailure == null || firstFailure.getMessage() == null
                ? "" : "\nLast error: " + firstFailure.getMessage();
            throw new IOException(
                "Could not fetch a skin for '" + username + "' using the legacy HTTP skin services. " +
                "This PowerPC build avoids modern HTTPS/TLS requirements. You can still place a PNG " +
                "measuring 64x64 or 64x32 in the Skins folder manually." + failureDetail
            );
        }

        if (!skinsDirectory.exists() && !skinsDirectory.mkdirs()) {
            throw new IOException("Could not create the Skins folder: " + skinsDirectory.getAbsolutePath());
        }

        String fileName = username + ".png";
        File destination = new File(skinsDirectory, fileName);
        File temporary = new File(skinsDirectory, fileName + ".download.tmp");
        FileOutputStream output = new FileOutputStream(temporary);
        try {
            output.write(pngData);
        } finally {
            output.close();
        }

        if (destination.exists() && !destination.delete()) {
            temporary.delete();
            throw new IOException("Could not replace the existing skin file " + fileName + ".");
        }
        if (!temporary.renameTo(destination)) {
            temporary.delete();
            throw new IOException("Could not finish saving " + fileName + " in the Skins folder.");
        }

        return fileName;
    }

    private byte[] readBytes(InputStream input, int limit) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                if (output.size() + count > limit) {
                    throw new IOException("The downloaded skin image is larger than the allowed size.");
                }
                output.write(buffer, 0, count);
            }
        } finally {
            input.close();
        }
        return output.toByteArray();
    }
}

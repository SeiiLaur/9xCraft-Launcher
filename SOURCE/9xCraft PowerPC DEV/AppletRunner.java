import java.awt.event.*;
import java.applet.*;
import java.awt.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.lang.reflect.*;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.jar.JarEntry;

public class AppletRunner {

private static final boolean DARK_MODE = Boolean.getBoolean("9xcraft.darkMode");
private static final Color DARK_BACKGROUND = new Color(32, 32, 32);
private static final Color DARK_PANEL = new Color(39, 39, 39);
private static final Color DARK_CONTROL = new Color(58, 58, 58);
private static final Color DARK_FOREGROUND = new Color(235, 235, 235);
private static final Color DARK_MUTED = new Color(175, 175, 175);

private static void styleDialog(Dialog dialog) {
    if (DARK_MODE) {
        dialog.setBackground(DARK_BACKGROUND);
        dialog.setForeground(DARK_FOREGROUND);
    }
}

private static void styleFrame(Frame frame) {
    if (DARK_MODE) {
        frame.setBackground(DARK_BACKGROUND);
        frame.setForeground(DARK_FOREGROUND);
    }
}

private static void stylePanel(Panel panel) {
    if (DARK_MODE) {
        panel.setBackground(DARK_PANEL);
        panel.setForeground(DARK_FOREGROUND);
    }
}

private static void styleLabel(Label label) {
    if (DARK_MODE) {
        label.setBackground(DARK_PANEL);
        label.setForeground(DARK_FOREGROUND);
    }
}

private static void styleTextField(TextField field) {
    if (DARK_MODE) {
        field.setBackground(DARK_CONTROL);
        field.setForeground(DARK_FOREGROUND);
        field.setCaretPosition(field.getText().length());
    }
}

private static void styleButton(Button button) {
    if (DARK_MODE) {
        button.setBackground(DARK_CONTROL);
        button.setForeground(DARK_FOREGROUND);
    }
}

static class ServerEntry {
    String name;
    String address;
    boolean online;

    ServerEntry(String n, String a) {
        name = n;
        address = a;
        online = false;
    }

    void checkOnlineAsync(final Panel entry) {
        new Thread(new Runnable() {
            public void run() {
                online = AppletRunner.isServerOnline(address);
                entry.repaint();
            }
        }).start();
    }
}

private static File getServerFile() {
    return new File("servers.txt");
}

private static java.util.List loadServers() {
    java.util.List list = new ArrayList();

    try {
        File f = getServerFile();
        if (!f.exists()) return list;

        BufferedReader br = new BufferedReader(new FileReader(f));
        String line;

        while ((line = br.readLine()) != null) {
            if (line.contains("|")) {
                String[] p = line.split("\\|");
                list.add(new ServerEntry(p[0], p[1]));
            }
        }

        br.close();
    } catch (Exception ignored) {}

    return list;
}

private static void saveServer(ServerEntry entry) {
    try {
        FileWriter fw = new FileWriter(getServerFile(), true);
        fw.write(entry.name + "|" + entry.address + "\n");
        fw.close();
    } catch (Exception ignored) {}
}

private static String askServerName() {

    final Dialog dialog = new Dialog((Frame)null, "Server Name", true);
    dialog.setLayout(new BorderLayout());
    dialog.setResizable(false);
    dialog.setSize(300,120);
    dialog.setLocationRelativeTo(null);
    styleDialog(dialog);

    Label label = new Label("How will this server be called?", Label.CENTER);
    styleLabel(label);
    final TextField field = new TextField("");
    styleTextField(field);

    final String[] result = {null};

    Panel buttons = new Panel();
    stylePanel(buttons);

    Button cancel = new Button("Cancel");
    Button next = new Button("Next");
    styleButton(cancel);
    styleButton(next);

    cancel.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            dialog.dispose();
        }
    });

    next.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            String text = field.getText().trim();
            if (text.length() >= 3) {
                result[0] = text;
                dialog.dispose();
            }
        }
    });

    buttons.add(cancel);
    buttons.add(next);

    dialog.add(label, BorderLayout.NORTH);
    dialog.add(field, BorderLayout.CENTER);
    dialog.add(buttons, BorderLayout.SOUTH);

    ZoomSupport.scaleAwtWindow(dialog);
    dialog.setLocationRelativeTo(null);
    dialog.setVisible(true);

    return result[0];
}

private static boolean isServerOnline(String address) {

    try {

        String host = address;
        int port = 25565;

        if (address.contains(":")) {
            String[] parts = address.split(":");
            host = parts[0];
            port = Integer.parseInt(parts[1]);
        }

        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), 1500);
        socket.close();

        return true;

    } catch (Exception e) {
        return false;
    }
}

private static Panel createServerEntry(
    final ServerEntry s,
    final Frame frame,
    final String[] result,
    final java.util.List servers,
    final Panel listPanel) {

final Panel entry = new Panel(new BorderLayout()) {
    public void paint(Graphics g) {
        super.paint(g);

        g.setFont(ZoomSupport.scaleFont(new Font("Dialog", Font.BOLD, 14)));
        g.setColor(DARK_MODE ? DARK_FOREGROUND : Color.BLACK);
        g.drawString(s.name, ZoomSupport.scale(10), ZoomSupport.scale(20));

        g.setFont(ZoomSupport.scaleFont(new Font("Dialog", Font.PLAIN, 12)));
        g.setColor(DARK_MODE ? DARK_MUTED : Color.DARK_GRAY);
        g.drawString(s.address, ZoomSupport.scale(10), ZoomSupport.scale(40));

        g.setColor(s.online ? Color.GREEN : Color.RED);
        g.fillOval(getWidth() - ZoomSupport.scale(40), ZoomSupport.scale(24), ZoomSupport.scale(10), ZoomSupport.scale(10));

        g.setColor(DARK_MODE ? new Color(78, 78, 78) : Color.LIGHT_GRAY);
        g.drawLine(0, getHeight() - ZoomSupport.scale(1), getWidth(), getHeight() - ZoomSupport.scale(1));

    }
        public Dimension getPreferredSize() {
    return new Dimension(listPanel.getWidth(), ZoomSupport.scale(60));
}
};

Button delete = new Button("X");
delete.setForeground(Color.RED);
delete.setPreferredSize(ZoomSupport.scaleDimension(new Dimension(20, 60)));
delete.setCursor(new Cursor(Cursor.HAND_CURSOR));
delete.setBackground(DARK_MODE ? DARK_CONTROL : new Color(240,240,240));
if (DARK_MODE) delete.setForeground(new Color(255, 105, 105));
Panel right = new Panel(new BorderLayout());
right.setBackground(DARK_MODE ? DARK_PANEL : Color.WHITE);
right.add(delete, BorderLayout.SOUTH);
entry.add(right, BorderLayout.EAST);
if (!s.online) {
    s.checkOnlineAsync(entry);
}

    entry.setPreferredSize(ZoomSupport.scaleDimension(new Dimension(20, 60)));

    entry.setBackground(DARK_MODE ? DARK_PANEL : Color.WHITE);
    entry.setForeground(DARK_MODE ? DARK_FOREGROUND : Color.BLACK);
    entry.setCursor(new Cursor(Cursor.HAND_CURSOR));

    entry.addMouseListener(new MouseAdapter() {
        public void mouseEntered(MouseEvent e) {
            entry.setBackground(DARK_MODE ? new Color(58, 58, 58) : new Color(210,210,210));
            entry.repaint();
        }

        public void mouseExited(MouseEvent e) {
            entry.setBackground(DARK_MODE ? DARK_PANEL : Color.WHITE);
            entry.repaint();
        }

        public void mouseClicked(MouseEvent e) {
            result[0] = s.address;
            frame.setVisible(false);
            frame.dispose();
        }
    });

    delete.addMouseListener(new MouseAdapter() {
    public void mouseClicked(MouseEvent e) {
        e.consume();
    }
});

    delete.addActionListener(new ActionListener() {
    public void actionPerformed(ActionEvent e) {
servers.remove(s);
listPanel.remove(entry);

listPanel.setPreferredSize(new Dimension(
    listPanel.getParent().getWidth() - ZoomSupport.scale(300),
    listPanel.getComponentCount() * ZoomSupport.scale(60)
));

saveAllServers(servers);

Container parent = listPanel.getParent();
while (parent != null) {
    parent.validate();
    parent.doLayout();
    parent = parent.getParent();
}

listPanel.repaint();
    }
});

    return entry;
}

private static void saveAllServers(java.util.List servers) {
    try {
        FileWriter fw = new FileWriter(getServerFile(), false);

        for (Object o : servers) {
            ServerEntry s = (ServerEntry)o;
            fw.write(s.name + "|" + s.address + "\n");
        }

        fw.close();
    } catch (Exception ignored) {}
}

private static String askServerAddress() {

    final Dialog dialog = new Dialog((Frame)null, "Server Address", true);
    dialog.setLayout(new BorderLayout());
    dialog.setResizable(false);
    dialog.setSize(360,120);
    dialog.setLocationRelativeTo(null);
    styleDialog(dialog);

    Label label = new Label("Enter server address (example: mc.example.com:25565)", Label.CENTER);
    styleLabel(label);

    final TextField field = new TextField("");
    styleTextField(field);

    final String[] result = {null};

    Panel buttons = new Panel();
    stylePanel(buttons);

    Button cancel = new Button("Cancel");
    Button add = new Button("Add");
    styleButton(cancel);
    styleButton(add);

    cancel.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            dialog.dispose();
        }
    });

    add.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            String text = field.getText().trim();
            if (text.length() > 0) {
                result[0] = text;
                dialog.dispose();
            }
        }
    });

    buttons.add(cancel);
    buttons.add(add);

    dialog.add(label, BorderLayout.NORTH);
    dialog.add(field, BorderLayout.CENTER);
    dialog.add(buttons, BorderLayout.SOUTH);

    ZoomSupport.scaleAwtWindow(dialog);
    dialog.setLocationRelativeTo(null);
    dialog.setVisible(true);

    return result[0];
}

private static String openServerBrowser() {

    final Frame frame = new Frame("Servers");
    frame.setSize(400,300);
    frame.setResizable(false);
    frame.setLayout(new BorderLayout());
    frame.setLocationRelativeTo(null);
    styleFrame(frame);

    final java.util.List servers = loadServers();

final Panel listPanel = new Panel();
listPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
listPanel.setBackground(DARK_MODE ? DARK_BACKGROUND : listPanel.getBackground());
listPanel.setPreferredSize(new Dimension(
    frame.getWidth() - ZoomSupport.scale(300),
    servers.size() * ZoomSupport.scale(60)
));
    final String[] result = {""};

    for (Object o : servers) {
        ServerEntry s = (ServerEntry)o;
        Panel entry = createServerEntry(s, frame, result, servers, listPanel);
        listPanel.add(entry);
    }

    final ScrollPane scroll = new ScrollPane();
    scroll.setBackground(DARK_MODE ? DARK_BACKGROUND : scroll.getBackground());
    scroll.add(listPanel);

    frame.add(scroll, BorderLayout.CENTER);

    Panel bottom = new Panel();
    stylePanel(bottom);

    Button exit = new Button("Exit");
    Button add = new Button("Add server to list");
    Button direct = new Button("Direct connect..");
    styleButton(exit);
    styleButton(add);
    styleButton(direct);

    exit.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            frame.dispose();
        }
    });

    direct.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            result[0] = askServerIP(frame);
            frame.dispose();
        }
    });

    add.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            String name = askServerName();
            if (name == null) return;

            String ip = askServerAddress();
            if (ip == null) return;

            ServerEntry entryData = new ServerEntry(name, ip);
            saveServer(entryData);
            servers.add(entryData);

            Panel entry = createServerEntry(entryData, frame, result, servers, listPanel);
            ZoomSupport.scaleAwtComponentTree(entry);
            listPanel.add(entry);

            listPanel.setPreferredSize(new Dimension(ZoomSupport.scale(360), listPanel.getComponentCount() * ZoomSupport.scale(60)));

            listPanel.validate();
            scroll.validate();
            scroll.doLayout();
            frame.validate();
        }
    });

    bottom.add(exit);
    bottom.add(add);
    bottom.add(direct);

    frame.add(bottom, BorderLayout.SOUTH);

    ZoomSupport.scaleAwtWindow(frame);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);

    while(frame.isShowing()) {
        try { Thread.sleep(50); } catch(Exception ignored){}
    }

    return result[0];
}

private static String askServerIP(Frame parent) {

    final Dialog dialog = new Dialog(parent, "Server", true);
    dialog.setLayout(new BorderLayout());
    dialog.setResizable(false);
    dialog.setSize(300,120);
    dialog.setLocationRelativeTo(null);
    styleDialog(dialog);

    Label label = new Label("Enter server IP, leave blank for offline play", Label.CENTER);
    styleLabel(label);

    final TextField field = new TextField("");
    field.setForeground(DARK_MODE ? DARK_MUTED : Color.GRAY);
    field.setText("0.0.0.0");
    styleTextField(field);
    if (DARK_MODE) field.setForeground(DARK_MUTED);

    field.addFocusListener(new FocusAdapter() {
        public void focusGained(FocusEvent e) {
            if (field.getText().equals("0.0.0.0")) {
                field.setText("");
                field.setForeground(DARK_MODE ? DARK_FOREGROUND : Color.BLACK);
            }
        }
    });

    Button connect = new Button("Connect");
    styleButton(connect);

    final String[] result = {""};

    connect.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            String text = field.getText().trim();
            if (!text.equals("0.0.0.0")) result[0] = text;
            dialog.dispose();
        }
    });

    Panel center = new Panel(new BorderLayout());
    stylePanel(center);
    center.add(field, BorderLayout.CENTER);

    Panel bottom = new Panel();
    stylePanel(bottom);
    bottom.add(connect);

    dialog.add(label, BorderLayout.NORTH);
    dialog.add(center, BorderLayout.CENTER);
    dialog.add(bottom, BorderLayout.SOUTH);

    ZoomSupport.scaleAwtWindow(dialog);
    dialog.setLocationRelativeTo(null);
    dialog.setVisible(true);

    return result[0];
}

private static void waitForResize(Frame frame) {

    final Canvas canvas = new Canvas() {
        public void paint(Graphics g) {
            g.setColor(Color.WHITE);
            String text = "Resize the window to your liking, then click the screen to start..";
            FontMetrics fm = g.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(text)) / 2;
            int y = getHeight() / 2;
            g.drawString(text, x, y);
        }
    };

    canvas.setBackground(Color.BLACK);
    canvas.setFont(ZoomSupport.scaleFont(canvas.getFont()));

    canvas.addMouseListener(new MouseAdapter() {
        public void mouseClicked(MouseEvent e) {
            canvas.setVisible(false);
        }
    });

    frame.add(canvas, BorderLayout.CENTER);
    frame.validate();

    canvas.setVisible(true);

    while (canvas.isVisible()) {
        try { Thread.sleep(50); } catch (Exception ignored) {}
    }

    frame.remove(canvas);
}

    public static void main(String[] args) {
        try {
            ZoomSupport.configureFromSystemProperty();

            if (args.length < 5) {
                System.out.println("Usage: AppletRunner <jar> <user> <width> <height> <fullscreen>");
                return;
            }

            final String jarPath = args[0];
            final String username = args[1];
            final int width = Integer.parseInt(args[2]);
            final int height = Integer.parseInt(args[3]);
            final boolean fullscreen = Boolean.parseBoolean(args[4]);

            String jarName = new File(jarPath).getName().toLowerCase();

            URL[] urls = { new File(jarPath).toURI().toURL() };
            URLClassLoader loader = new URLClassLoader(urls);

            Class appletClass = null;

            try {
                JarFile jarFile = new JarFile(jarPath);
                Manifest manifest = jarFile.getManifest();

                if (manifest != null) {
                    String mainClassName = manifest.getMainAttributes().getValue("Main-Class");
                    if (mainClassName != null && mainClassName.trim().length() > 0) {
                        if (mainClassName.trim().equals("net.minecraft.client.Minecraft")) {
                            System.out.println("Detected standalone Minecraft class, redirecting to MinecraftApplet");
                            mainClassName = "net.minecraft.client.MinecraftApplet";
                        }

                        try {
                            appletClass = loader.loadClass(mainClassName.trim());
                            System.out.println("Loaded main class from manifest: " + mainClassName);
                        } catch (ClassNotFoundException e) {
                            System.out.println("Manifest main class not found: " + mainClassName);
                        }
                    }
                }

                jarFile.close();
            } catch (Exception e) {
                System.out.println("Could not read manifest: " + e.getMessage());
            }

            if (appletClass == null) {
                try {
                    appletClass = loader.loadClass("net.minecraft.client.MinecraftApplet");
                } catch (ClassNotFoundException ignored) {}
            }

            if (appletClass == null) {
                try {
                    Class rubyClass = loader.loadClass("com.mojang.minecraft.RubyDung");
                    Object rubyInstance = rubyClass.newInstance();
                    Method runMethod = rubyClass.getMethod("run", new Class[0]);
                    runMethod.invoke(rubyInstance, new Object[0]);
                    return;
                } catch (ClassNotFoundException ignored) {}
            }

            if (appletClass == null) {
                try {
                    appletClass = loader.loadClass("com.mojang.minecraft.MinecraftApplet");
                } catch (ClassNotFoundException ignored) {}
            }

            if (appletClass == null) {
                try {
                    appletClass = loader.loadClass("M");
                } catch (ClassNotFoundException ignored) {}
            }

            if (appletClass == null) {
                Class rubyClass = loader.loadClass("com.mojang.rubydung.RubyDung");
                Object rubyInstance = rubyClass.newInstance();
                Method runMethod = rubyClass.getMethod("run", new Class[0]);
                runMethod.invoke(rubyInstance, new Object[0]);
                return;
            }

            final Applet applet = (Applet) appletClass.newInstance();

            final HashMap params = new HashMap();
            params.put("username", username);
            params.put("sessionid", "-");
            params.put("mppass", "0");
            params.put("fullscreen", String.valueOf(fullscreen));
            params.put("stand-alone", "true");
            params.put("haspaid", "true");

            boolean jarLooksIndev = jarName.contains("2010");

            boolean classicMultiplayer =
                   jarName.contains("0.0.15") ||
                   jarName.contains("0.0.16") ||
                   jarName.contains("0.0.17") ||
                   jarName.contains("0.0.18") ||
                   jarName.contains("0.0.19") ||
                   jarName.contains("0.0.2") ||
                   jarName.contains("0.24") ||
                   jarName.contains("0.25") ||
                   jarName.contains("0.26") ||
                   jarName.contains("0.27") ||
                   jarName.contains("0.28") ||
                   jarName.contains("0.29") ||
                   jarName.contains("0.30") ||
                   jarName.contains("200912");

            if (classicMultiplayer && !jarLooksIndev) {
                String ip = openServerBrowser().trim();

                if (ip.length() > 0) {
                    String host = ip;
                    int port = 25565;

                    if (ip.contains(":")) {
                        String[] parts = ip.split(":");
                        host = parts[0];
                        try {
                            port = Integer.parseInt(parts[1]);
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid port entered, using default 25565");
                        }
                    }

                    params.put("server", host);
                    params.put("port", String.valueOf(port));
                    System.out.println("Connecting to " + host + ":" + port);
                } else {
                    System.out.println("No server entered; launching offline.");
                }
            }

            final Frame frame = new Frame("Minecraft");
            frame.setLayout(new BorderLayout());

            if (fullscreen) {
                frame.setUndecorated(true);
                frame.setResizable(false);
                Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
                frame.setSize(screen.width, screen.height);
                frame.setLocation(0, 0);
            } else {
                frame.setSize(width, height);
                frame.setLocationRelativeTo(null);
            }

            frame.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    try {
                        applet.stop();
                        applet.destroy();
                    } catch (Throwable ignored) {}
                    frame.dispose();
                    System.exit(0);
                }
            });

            applet.setStub(new AppletStub() {
                public boolean isActive() { return true; }

                public URL getDocumentBase() {
                    try { return new URL("http://minecraft.net/game/"); }
                    catch (Exception e) { return null; }
                }

                public URL getCodeBase() {
                    try { return new URL("http://minecraft.net/game/"); }
                    catch (Exception e) { return null; }
                }

                public String getParameter(String name) {
                    return (String) params.get(name);
                }

                public AppletContext getAppletContext() {
                    return new AppletContext() {
                        public Applet getApplet(String name) { return null; }
                        public Enumeration getApplets() { return null; }
                        public AudioClip getAudioClip(URL url) { return null; }
                        public Image getImage(URL url) { return null; }
                        public void showDocument(URL url) {}
                        public void showDocument(URL url, String target) {}
                        public void showStatus(String status) {}
                        public void setStream(String key, InputStream stream) {}
                        public InputStream getStream(String key) { return null; }
                        public Iterator getStreamKeys() { return null; }
                    };
                }

                public void appletResize(int w, int h) {
                    frame.setSize(w, h);
                }
            });

            applet.setBackground(Color.BLACK);
            frame.setVisible(true);
            frame.validate();
            frame.repaint();
            applet.setSize(width, height);

            boolean classic =
                   jarName.contains("0.0.") ||
                   jarName.contains("0.0.1") ||
                   jarName.contains("0.0.2") ||
                   jarName.contains("0.24") ||
                   jarName.contains("0.25") ||
                   jarName.contains("0.26") ||
                   jarName.contains("0.27") ||
                   jarName.contains("0.28") ||
                   jarName.contains("0.29") ||
                   jarName.contains("0.30");

            boolean indev =
                   jarName.contains("1207") ||
                   jarName.contains("1223") ||
                   jarName.contains("1231") ||
                   jarName.contains("20100104") ||
                   jarName.contains("20100105") ||
                   jarName.contains("20100106") ||
                   jarName.contains("20100107") ||
                   jarName.contains("20100108") ||
                   jarName.contains("20100109") ||
                   jarName.contains("20100110");

            if (classic || indev) {
                waitForResize(frame);
            }

            frame.add(applet, BorderLayout.CENTER);
            frame.validate();
            applet.init();
            applet.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

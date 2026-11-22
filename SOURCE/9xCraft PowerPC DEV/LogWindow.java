import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class LogWindow extends JFrame {

    private JTextPane area;
    private StyledDocument doc;
    private Style normalStyle;
    private Style errorStyle;
    private Style warnStyle;
    private final Process process;

    private boolean inErrorBlock = false;
    private boolean inWarnBlock = false;

    private JLabel statusLabel;
    private long startTime;

    public LogWindow(Process proc, JFrame parent) {
        this.process = proc;

        setTitle("Game Log");
        setSize(parent.getWidth(), parent.getHeight());
        setLocation(parent.getX() + parent.getWidth(), parent.getY());
        setLayout(new BorderLayout());

        area = new JTextPane();
        area.setEditable(false);
        doc = area.getStyledDocument();

        normalStyle = area.addStyle("normal", null);
        errorStyle = area.addStyle("error", null);
        warnStyle = area.addStyle("warn", null);

        JScrollPane scroll = new JScrollPane(area);
        add(scroll, BorderLayout.CENTER);

        statusLabel = new JLabel("Initializing...");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(ZoomSupport.scale(5), ZoomSupport.scale(5), ZoomSupport.scale(5), ZoomSupport.scale(5)));

        JButton killButton = new JButton("Kill Instance");

        killButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    process.destroy();
                    appendExternalMessage("Launcher: Game process terminated.", Color.RED);
                } catch (Exception ex) {
                    appendExternalMessage("Launcher: Failed to terminate process.", Color.RED);
                }
            }
        });

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(statusLabel, BorderLayout.CENTER);
        bottom.add(killButton, BorderLayout.EAST);

        add(bottom, BorderLayout.SOUTH);

        applyTheme();
        startTime = System.currentTimeMillis();
        startStatusUpdater();

        setVisible(true);

        readStream(process.getInputStream());
        readStream(process.getErrorStream());

        new Thread(new Runnable() {
            public void run() {
                try {
                    process.waitFor();
                    if (!NineXCraftLauncher.keepLogWindowGlobal) {
                        dispose();
                    }
                } catch (Exception e) {
                }
            }
        }).start();
    }

    public void applyTheme() {
        boolean dark = DarkMode.isEnabled();
        area.setBackground(dark ? new Color(32, 32, 32) : Color.WHITE);
        area.setForeground(dark ? new Color(235, 235, 235) : Color.BLACK);
        area.setCaretColor(dark ? new Color(235, 235, 235) : Color.BLACK);
        StyleConstants.setForeground(normalStyle, dark ? new Color(235, 235, 235) : Color.BLACK);
        StyleConstants.setBackground(normalStyle, dark ? new Color(32, 32, 32) : Color.WHITE);
        StyleConstants.setForeground(errorStyle, Color.RED);
        StyleConstants.setBackground(errorStyle, dark ? new Color(32, 32, 32) : Color.WHITE);
        StyleConstants.setForeground(warnStyle, dark ? new Color(230, 170, 75) : new Color(180, 120, 0));
        StyleConstants.setBackground(warnStyle, dark ? new Color(32, 32, 32) : Color.WHITE);
        Style externalStyle = area.getStyle("external");
        if (externalStyle != null) {
            StyleConstants.setBackground(externalStyle, dark ? new Color(32, 32, 32) : Color.WHITE);
        }
        statusLabel.setForeground(dark ? new Color(235, 235, 235) : Color.BLACK);
        repaint();
    }

    private void readStream(final InputStream stream) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        appendLine(line);
                    }
                } catch (IOException e) {
                }
            }
        }).start();
    }

    private void startStatusUpdater() {
        new Thread(new Runnable() {
            public void run() {
                boolean running = true;
                while (running) {
                    try {
                        process.exitValue();
                        running = false;
                    } catch (IllegalThreadStateException e) {
                        running = true;
                    }

                    long totalSeconds = (System.currentTimeMillis() - startTime) / 1000;
                    long hours = totalSeconds / 3600;
                    long minutes = (totalSeconds % 3600) / 60;
                    long seconds = totalSeconds % 60;

                    String timeString;
                    if (hours > 0) {
                        timeString = hours + "h " + minutes + "m " + seconds + "s";
                    } else if (minutes > 0) {
                        timeString = minutes + "m " + seconds + "s";
                    } else {
                        timeString = seconds + "s";
                    }

                    final String text = running
                        ? "Status: Running | Runtime: " + timeString
                        : "Status: Exited";

                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            statusLabel.setText(text);
                        }
                    });

                    try { Thread.sleep(1000); }
                    catch (Exception ignored) {}
                }
            }
        }).start();
    }

    public void appendExternalMessage(final String message, final Color color) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    Style tempStyle = area.addStyle("external", null);
                    Color messageColor = color;
                    if (DarkMode.isEnabled() && Color.GRAY.equals(color)) {
                        messageColor = new Color(175, 175, 175);
                    } else if (DarkMode.isEnabled() && Color.ORANGE.equals(color)) {
                        messageColor = new Color(235, 170, 75);
                    }
                    StyleConstants.setForeground(tempStyle, messageColor);
                    StyleConstants.setBackground(tempStyle, DarkMode.isEnabled() ? new Color(32, 32, 32) : Color.WHITE);
                    doc.insertString(doc.getLength(), message + "\n", tempStyle);
                    area.setCaretPosition(doc.getLength());
                } catch (Exception ignored) {}
            }
        });
    }

    private void appendLine(final String line) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    Style style = normalStyle;
                    String lower = line.toLowerCase();

                    if (lower.contains("exception") || lower.contains("error")) {
                        style = errorStyle;
                        inErrorBlock = true;
                        inWarnBlock = false;
                    } else if (lower.contains("warn")) {
                        style = warnStyle;
                        inWarnBlock = true;
                        inErrorBlock = false;
                    } else if (inErrorBlock && line.startsWith("\tat")) {
                        style = errorStyle;
                    } else if (inWarnBlock && line.startsWith("\tat")) {
                        style = warnStyle;
                    } else {
                        inErrorBlock = false;
                        inWarnBlock = false;
                    }

                    doc.insertString(doc.getLength(), line + "\n", style);
                    area.setCaretPosition(doc.getLength());
                } catch (Exception ignored) {}
            }
        });
    }
}

import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SkinWindow extends JDialog {

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
        URL lookupUrl = new URL("https://api.mojang.com/users/profiles/minecraft/" + encodedName);
        HttpURLConnection lookup = openConnection(lookupUrl);
        int lookupStatus = lookup.getResponseCode();

        if (lookupStatus == 204 || lookupStatus == 404) {
            lookup.disconnect();
            throw new IOException("No Minecraft profile was found for '" + username + "'. Check the username and try again.");
        }
        if (lookupStatus == 429) {
            lookup.disconnect();
            throw new IOException("Mojang's profile lookup is temporarily rate-limited. Please try again later.");
        }
        if (lookupStatus != 200) {
            lookup.disconnect();
            throw new IOException("Mojang's profile lookup returned HTTP " + lookupStatus + ".");
        }

        String profileJson;
        try {
            profileJson = readText(lookup.getInputStream());
        } finally {
            lookup.disconnect();
        }

        String uuid = findJsonValue(profileJson, "id");
        String actualName = findJsonValue(profileJson, "name");
        if (uuid == null || !uuid.matches("[0-9a-fA-F]{32}")) {
            throw new IOException("Mojang returned an invalid profile response for '" + username + "'.");
        }
        if (actualName == null || !actualName.matches("[A-Za-z0-9_]{1,16}")) {
            actualName = username;
        }

        URL sessionUrl = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid);
        HttpURLConnection session = openConnection(sessionUrl);
        int sessionStatus = session.getResponseCode();
        if (sessionStatus != 200) {
            session.disconnect();
            throw new IOException("Mojang's skin profile request returned HTTP " + sessionStatus + ".");
        }

        String sessionJson;
        try {
            sessionJson = readText(session.getInputStream());
        } finally {
            session.disconnect();
        }

        String encodedTextures = findJsonValue(sessionJson, "value");
        if (encodedTextures == null) {
            throw new IOException("The Minecraft profile did not provide skin information.");
        }

        String textureJson;
        try {
            textureJson = new String(decodeBase64(encodedTextures), "UTF-8");
        } catch (Exception e) {
            throw new IOException("Mojang returned unreadable skin information.");
        }

        Matcher skinMatcher = Pattern.compile("\\\"SKIN\\\"\\s*:\\s*\\{.*?\\\"url\\\"\\s*:\\s*\\\"(https?://[^\\\"]+)\\\"", Pattern.DOTALL).matcher(textureJson);
        if (!skinMatcher.find()) {
            throw new IOException("That Minecraft profile exists, but it does not have a custom Java skin to download.");
        }

        URL textureUrl = new URL(skinMatcher.group(1));
        HttpURLConnection texture = openConnection(textureUrl);
        int textureStatus = texture.getResponseCode();
        if (textureStatus != 200) {
            texture.disconnect();
            throw new IOException("The skin texture server returned HTTP " + textureStatus + ".");
        }

        byte[] pngData;
        try {
            pngData = readBytes(texture.getInputStream(), MAX_SKIN_BYTES);
        } finally {
            texture.disconnect();
        }

        BufferedImage image = ImageIO.read(new java.io.ByteArrayInputStream(pngData));
        if (!isValidDimensions(image)) {
            throw new IOException("The downloaded skin is not a supported 64x64 or 64x32 PNG.");
        }

        if (!skinsDirectory.exists() && !skinsDirectory.mkdirs()) {
            throw new IOException("Could not create the Skins folder: " + skinsDirectory.getAbsolutePath());
        }

        String fileName = actualName + ".png";
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

    private byte[] decodeBase64(String value) throws IOException {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int buffer = 0;
        int bits = -8;

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '=') {
                break;
            }
            if (c == '\r' || c == '\n' || c == ' ' || c == '\t') {
                continue;
            }
            int digit = alphabet.indexOf(c);
            if (digit < 0) {
                throw new IOException("Mojang returned invalid Base64 skin information.");
            }
            buffer = (buffer << 6) | digit;
            bits += 6;
            if (bits >= 0) {
                output.write((buffer >> bits) & 255);
                bits -= 8;
            }
        }

        return output.toByteArray();
    }

    private HttpURLConnection openConnection(URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(12000);
        connection.setReadTimeout(15000);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent", "9xCraftLauncher/1.0");
        connection.setRequestProperty("Accept", "application/json,image/png,*/*");
        return connection;
    }

    private String findJsonValue(String json, String key) {
        Pattern pattern = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String readText(InputStream input) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"));
        StringBuffer result = new StringBuffer();
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
        } finally {
            reader.close();
        }
        return result.toString();
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

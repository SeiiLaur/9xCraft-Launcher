import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Window;
import java.util.Enumeration;
import java.util.IdentityHashMap;
import java.util.Map;
import javax.swing.ImageIcon;
import javax.swing.UIManager;
import javax.swing.UIDefaults;

public final class ZoomSupport {
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

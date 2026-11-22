import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.JComponent;
import javax.swing.AbstractButton;
import javax.swing.text.JTextComponent;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.JTree;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.lang.reflect.InvocationTargetException;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DarkMode {
    private static final Map<String, Object> originalValues = new LinkedHashMap<String, Object>();
    private static final Map<Component, ComponentState> componentStates = new IdentityHashMap<Component, ComponentState>();
    private static boolean enabled;

    private static final Color DARK_BACKGROUND = new Color(32, 32, 32);
    private static final Color DARK_PANEL = new Color(38, 38, 38);
    private static final Color DARK_BUTTON = new Color(57, 57, 57);
    private static final Color DARK_INPUT = new Color(48, 48, 48);
    private static final Color LIGHT_TEXT = new Color(235, 235, 235);
    private static final Color DARK_MUTED = new Color(165, 165, 165);
    private static final Color DARK_SELECTION = new Color(75, 91, 112);
    private static final Color DARK_BORDER = new Color(75, 75, 75);

    private DarkMode() {}

    private static class ComponentState {
        private Color foreground;
        private Color background;
        private Color caret;
        private Color selectionBackground;
        private Color selectionForeground;
        private Color grid;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void apply(final boolean useDarkMode) {
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        applyOnEventThread(useDarkMode);
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while changing the launcher theme.", e);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                throw new IllegalStateException("Could not change the launcher theme.", cause == null ? e : cause);
            }
            return;
        }

        applyOnEventThread(useDarkMode);
    }

    private static void applyOnEventThread(boolean useDarkMode) {
        boolean wasEnabled = enabled;
        if (wasEnabled == useDarkMode) {
            refreshWindows();
            return;
        }

        String[] keys = {
            "control", "info", "nimbusBase", "nimbusBlueGrey", "nimbusLightBackground",
            "Panel.background", "Panel.foreground", "Label.foreground",
            "Button.background", "Button.foreground", "Button.select", "Button.focus",
            "ToggleButton.background", "ToggleButton.foreground",
            "CheckBox.background", "CheckBox.foreground", "RadioButton.background", "RadioButton.foreground",
            "TextField.background", "TextField.foreground", "TextField.caretForeground", "TextField.selectionBackground", "TextField.selectionForeground",
            "PasswordField.background", "PasswordField.foreground", "PasswordField.caretForeground", "PasswordField.selectionBackground", "PasswordField.selectionForeground",
            "FormattedTextField.background", "FormattedTextField.foreground",
            "TextArea.background", "TextArea.foreground", "TextArea.caretForeground", "TextArea.selectionBackground", "TextArea.selectionForeground",
            "TextPane.background", "TextPane.foreground", "EditorPane.background", "EditorPane.foreground",
            "ComboBox.background", "ComboBox.foreground", "ComboBox.selectionBackground", "ComboBox.selectionForeground",
            "List.background", "List.foreground", "List.selectionBackground", "List.selectionForeground",
            "Table.background", "Table.foreground", "Table.selectionBackground", "Table.selectionForeground", "Table.gridColor",
            "Tree.background", "Tree.foreground", "Tree.selectionBackground", "Tree.selectionForeground",
            "ScrollPane.background", "Viewport.background", "Separator.foreground",
            "MenuBar.background", "MenuBar.foreground", "Menu.background", "Menu.foreground",
            "MenuItem.background", "MenuItem.foreground", "MenuItem.selectionBackground", "MenuItem.selectionForeground",
            "CheckBoxMenuItem.background", "CheckBoxMenuItem.foreground", "RadioButtonMenuItem.background", "RadioButtonMenuItem.foreground",
            "PopupMenu.background", "PopupMenu.foreground",
            "OptionPane.background", "OptionPane.messageForeground", "OptionPane.buttonBackground", "OptionPane.buttonForeground",
            "FileChooser.background", "FileChooser.foreground",
            "TabbedPane.background", "TabbedPane.foreground", "TabbedPane.selected", "TabbedPane.contentAreaColor",
            "TitledBorder.titleColor", "ToolTip.background", "ToolTip.foreground",
            "ProgressBar.background", "ProgressBar.foreground", "Slider.background", "Slider.foreground",
            "Spinner.background", "Spinner.foreground", "SplitPane.background", "SplitPaneDivider.background",
            "ColorChooser.background", "ColorChooser.foreground",
            "InternalFrame.background", "InternalFrame.foreground", "Desktop.background",
            "controlText", "text", "textText", "textHighlight", "textHighlightText", "window", "windowText",
            "MenuItem.acceleratorForeground", "MenuItem.acceleratorSelectionForeground",
            "Menu.acceleratorForeground", "Menu.acceleratorSelectionForeground"
        };

        Window[] windows = Window.getWindows();

        if (useDarkMode) {
            originalValues.clear();
            componentStates.clear();
            for (int i = 0; i < keys.length; i++) {
                originalValues.put(keys[i], UIManager.get(keys[i]));
            }
            for (int i = 0; i < windows.length; i++) {
                captureComponentTree(windows[i]);
            }
            enabled = true;
            applyDarkDefaults();
        } else {
            enabled = false;
            restoreDefaults(keys);
        }

        for (int i = 0; i < windows.length; i++) {
            Window window = windows[i];
            if (window == null) {
                continue;
            }
            SwingUtilities.updateComponentTreeUI(window);
            if (useDarkMode) {
                applyComponentTree(window);
            } else {
                restoreComponentTree(window);
            }
            applySpecialWindowTheme(window);
            window.invalidate();
            window.validate();
            window.repaint();
        }

        if (!useDarkMode) {
            componentStates.clear();
            originalValues.clear();
        }
    }

    private static void applyDarkDefaults() {
        Color background = DARK_BACKGROUND;
        Color panel = DARK_PANEL;
        Color button = DARK_BUTTON;
        Color input = DARK_INPUT;
        Color foreground = LIGHT_TEXT;
        Color muted = DARK_MUTED;
        Color selection = DARK_SELECTION;
        Color border = DARK_BORDER;

        put("control", button);
        put("info", new Color(45, 45, 45));
        put("nimbusBase", new Color(55, 55, 55));
        put("nimbusBlueGrey", new Color(65, 65, 65));
        put("nimbusLightBackground", input);
        put("Panel.background", panel);
        put("Panel.foreground", foreground);
        put("Label.foreground", foreground);
        put("Button.background", button);
        put("Button.foreground", foreground);
        put("Button.select", selection);
        put("Button.focus", border);
        put("ToggleButton.background", button);
        put("ToggleButton.foreground", foreground);
        put("CheckBox.background", panel);
        put("CheckBox.foreground", foreground);
        put("RadioButton.background", panel);
        put("RadioButton.foreground", foreground);
        put("TextField.background", input);
        put("TextField.foreground", foreground);
        put("TextField.caretForeground", foreground);
        put("TextField.selectionBackground", selection);
        put("TextField.selectionForeground", Color.WHITE);
        put("PasswordField.background", input);
        put("PasswordField.foreground", foreground);
        put("PasswordField.caretForeground", foreground);
        put("PasswordField.selectionBackground", selection);
        put("PasswordField.selectionForeground", Color.WHITE);
        put("FormattedTextField.background", input);
        put("FormattedTextField.foreground", foreground);
        put("TextArea.background", input);
        put("TextArea.foreground", foreground);
        put("TextArea.caretForeground", foreground);
        put("TextArea.selectionBackground", selection);
        put("TextArea.selectionForeground", Color.WHITE);
        put("TextPane.background", input);
        put("TextPane.foreground", foreground);
        put("EditorPane.background", input);
        put("EditorPane.foreground", foreground);
        put("ComboBox.background", input);
        put("ComboBox.foreground", foreground);
        put("ComboBox.selectionBackground", selection);
        put("ComboBox.selectionForeground", Color.WHITE);
        put("List.background", input);
        put("List.foreground", foreground);
        put("List.selectionBackground", selection);
        put("List.selectionForeground", Color.WHITE);
        put("Table.background", input);
        put("Table.foreground", foreground);
        put("Table.selectionBackground", selection);
        put("Table.selectionForeground", Color.WHITE);
        put("Table.gridColor", border);
        put("Tree.background", input);
        put("Tree.foreground", foreground);
        put("Tree.selectionBackground", selection);
        put("Tree.selectionForeground", Color.WHITE);
        put("ScrollPane.background", panel);
        put("Viewport.background", panel);
        put("Separator.foreground", border);
        put("MenuBar.background", panel);
        put("MenuBar.foreground", foreground);
        put("Menu.background", panel);
        put("Menu.foreground", foreground);
        put("MenuItem.background", panel);
        put("MenuItem.foreground", foreground);
        put("MenuItem.selectionBackground", selection);
        put("MenuItem.selectionForeground", Color.WHITE);
        put("CheckBoxMenuItem.background", panel);
        put("CheckBoxMenuItem.foreground", foreground);
        put("RadioButtonMenuItem.background", panel);
        put("RadioButtonMenuItem.foreground", foreground);
        put("PopupMenu.background", panel);
        put("PopupMenu.foreground", foreground);
        put("OptionPane.background", panel);
        put("OptionPane.messageForeground", foreground);
        put("OptionPane.buttonBackground", button);
        put("OptionPane.buttonForeground", foreground);
        put("FileChooser.background", panel);
        put("FileChooser.foreground", foreground);
        put("TabbedPane.background", panel);
        put("TabbedPane.foreground", foreground);
        put("TabbedPane.selected", button);
        put("TabbedPane.contentAreaColor", panel);
        put("TitledBorder.titleColor", foreground);
        put("ToolTip.background", new Color(55, 55, 55));
        put("ToolTip.foreground", foreground);
        put("ProgressBar.background", input);
        put("ProgressBar.foreground", selection);
        put("Slider.background", panel);
        put("Slider.foreground", foreground);
        put("Spinner.background", input);
        put("Spinner.foreground", foreground);
        put("SplitPane.background", panel);
        put("SplitPaneDivider.background", border);
        put("ColorChooser.background", panel);
        put("ColorChooser.foreground", foreground);
        put("InternalFrame.background", panel);
        put("InternalFrame.foreground", foreground);
        put("Desktop.background", background);
        put("controlText", foreground);
        put("text", input);
        put("textText", foreground);
        put("textHighlight", selection);
        put("textHighlightText", Color.WHITE);
        put("window", panel);
        put("windowText", foreground);
        put("MenuItem.acceleratorForeground", muted);
        put("MenuItem.acceleratorSelectionForeground", Color.WHITE);
        put("Menu.acceleratorForeground", muted);
        put("Menu.acceleratorSelectionForeground", Color.WHITE);
    }

    private static void restoreDefaults(String[] keys) {
        for (int i = 0; i < keys.length; i++) {
            Object original = originalValues.get(keys[i]);
            if (original != null) {
                UIManager.put(keys[i], original);
            } else {
                UIManager.getDefaults().remove(keys[i]);
            }
        }
    }

    private static void put(String key, Object value) {
        if (originalValues.containsKey(key) && originalValues.get(key) != null) {
            UIManager.put(key, value);
        }
    }

    private static void captureComponentTree(Component component) {
        if (component == null || componentStates.containsKey(component)) {
            return;
        }

        ComponentState state = new ComponentState();
        state.foreground = component.getForeground();
        state.background = component.getBackground();

        if (component instanceof JTextComponent) {
            JTextComponent text = (JTextComponent)component;
            state.caret = text.getCaretColor();
            state.selectionBackground = text.getSelectionColor();
            state.selectionForeground = text.getSelectedTextColor();
        } else if (component instanceof JList) {
            JList list = (JList)component;
            state.selectionBackground = list.getSelectionBackground();
            state.selectionForeground = list.getSelectionForeground();
        } else if (component instanceof JTable) {
            JTable table = (JTable)component;
            state.selectionBackground = table.getSelectionBackground();
            state.selectionForeground = table.getSelectionForeground();
            state.grid = table.getGridColor();
        }

        componentStates.put(component, state);

        if (component instanceof Container) {
            Component[] children = ((Container)component).getComponents();
            for (int i = 0; i < children.length; i++) {
                captureComponentTree(children[i]);
            }
        }
    }

    private static void applyComponentTree(Component component) {
        if (component == null) {
            return;
        }

        ComponentState state = componentStates.get(component);
        if (state != null && component instanceof JComponent) {
            JComponent swing = (JComponent)component;

            if (state.foreground != null && isDefaultForeground(state.foreground)) {
                swing.setForeground(LIGHT_TEXT);
            }

            if (swing.isOpaque() && state.background != null && isDefaultBackground(state.background)) {
                if (component instanceof JTextComponent || component instanceof JList ||
                    component instanceof JTable || component instanceof JTree ||
                    component instanceof javax.swing.JComboBox || component instanceof javax.swing.JSpinner) {
                    swing.setBackground(DARK_INPUT);
                } else if (component instanceof AbstractButton) {
                    swing.setBackground(DARK_BUTTON);
                } else {
                    swing.setBackground(DARK_PANEL);
                }
            }

            if (component instanceof JTextComponent) {
                JTextComponent text = (JTextComponent)component;
                if (state.caret != null && isDefaultForeground(state.caret)) {
                    text.setCaretColor(LIGHT_TEXT);
                }
                if (state.selectionBackground != null && isDefaultSelection(state.selectionBackground)) {
                    text.setSelectionColor(DARK_SELECTION);
                }
                if (state.selectionForeground != null && isDefaultForeground(state.selectionForeground)) {
                    text.setSelectedTextColor(Color.WHITE);
                }
            } else if (component instanceof JList) {
                JList list = (JList)component;
                if (state.selectionBackground != null && isDefaultSelection(state.selectionBackground)) {
                    list.setSelectionBackground(DARK_SELECTION);
                }
                if (state.selectionForeground != null && isDefaultForeground(state.selectionForeground)) {
                    list.setSelectionForeground(Color.WHITE);
                }
            } else if (component instanceof JTable) {
                JTable table = (JTable)component;
                if (state.selectionBackground != null && isDefaultSelection(state.selectionBackground)) {
                    table.setSelectionBackground(DARK_SELECTION);
                }
                if (state.selectionForeground != null && isDefaultForeground(state.selectionForeground)) {
                    table.setSelectionForeground(Color.WHITE);
                }
                if (state.grid != null && isDefaultBackground(state.grid)) {
                    table.setGridColor(DARK_BORDER);
                }
            }
        }

        if (component instanceof Container) {
            Component[] children = ((Container)component).getComponents();
            for (int i = 0; i < children.length; i++) {
                applyComponentTree(children[i]);
            }
        }
    }

    private static void restoreComponentTree(Component component) {
        if (component == null) {
            return;
        }

        ComponentState state = componentStates.get(component);
        if (state != null) {
            component.setForeground(state.foreground);
            component.setBackground(state.background);

            if (component instanceof JTextComponent) {
                JTextComponent text = (JTextComponent)component;
                text.setCaretColor(state.caret);
                text.setSelectionColor(state.selectionBackground);
                text.setSelectedTextColor(state.selectionForeground);
            } else if (component instanceof JList) {
                JList list = (JList)component;
                list.setSelectionBackground(state.selectionBackground);
                list.setSelectionForeground(state.selectionForeground);
            } else if (component instanceof JTable) {
                JTable table = (JTable)component;
                table.setSelectionBackground(state.selectionBackground);
                table.setSelectionForeground(state.selectionForeground);
                table.setGridColor(state.grid);
            }
        }

        if (component instanceof Container) {
            Component[] children = ((Container)component).getComponents();
            for (int i = 0; i < children.length; i++) {
                restoreComponentTree(children[i]);
            }
        }
    }

    private static boolean isDefaultForeground(Color color) {
        if (color == null) {
            return false;
        }
        if (color.equals(Color.BLACK) || color.equals(new Color(51, 51, 51))) {
            return true;
        }
        String[] names = {
            "Panel.foreground", "Label.foreground", "Button.foreground", "ToggleButton.foreground",
            "CheckBox.foreground", "RadioButton.foreground", "TextField.foreground", "PasswordField.foreground",
            "FormattedTextField.foreground", "TextArea.foreground", "TextPane.foreground", "EditorPane.foreground",
            "ComboBox.foreground", "List.foreground", "Table.foreground", "Tree.foreground",
            "MenuBar.foreground", "Menu.foreground", "MenuItem.foreground", "PopupMenu.foreground",
            "OptionPane.messageForeground", "controlText", "textText", "windowText"
        };
        return matchesOriginal(color, names);
    }

    private static boolean isDefaultBackground(Color color) {
        if (color == null) {
            return false;
        }
        if (color.equals(Color.WHITE) || color.equals(new Color(238, 238, 238)) ||
            color.equals(new Color(240, 240, 240))) {
            return true;
        }
        String[] names = {
            "control", "info", "Panel.background", "Button.background", "ToggleButton.background",
            "CheckBox.background", "RadioButton.background", "TextField.background", "PasswordField.background",
            "FormattedTextField.background", "TextArea.background", "TextPane.background", "EditorPane.background",
            "ComboBox.background", "List.background", "Table.background", "Tree.background",
            "ScrollPane.background", "Viewport.background", "MenuBar.background", "Menu.background",
            "MenuItem.background", "PopupMenu.background", "OptionPane.background", "FileChooser.background",
            "TabbedPane.background", "ProgressBar.background", "Slider.background", "Spinner.background",
            "SplitPane.background", "ColorChooser.background", "InternalFrame.background", "Desktop.background",
            "text", "window"
        };
        return matchesOriginal(color, names);
    }

    private static boolean isDefaultSelection(Color color) {
        if (color == null) {
            return false;
        }
        String[] names = {
            "Button.select", "TextField.selectionBackground", "PasswordField.selectionBackground",
            "TextArea.selectionBackground", "ComboBox.selectionBackground", "List.selectionBackground",
            "Table.selectionBackground", "Tree.selectionBackground", "textHighlight"
        };
        return matchesOriginal(color, names);
    }

    private static boolean matchesOriginal(Color color, String[] names) {
        for (int i = 0; i < names.length; i++) {
            Object original = originalValues.get(names[i]);
            if (color.equals(original)) {
                return true;
            }
        }
        return false;
    }

    private static void applySpecialWindowTheme(Window window) {
        if (window instanceof LogWindow) {
            ((LogWindow)window).applyTheme();
        } else if (window instanceof SkinWindow) {
            ((SkinWindow)window).applyTheme();
        }
    }

    private static void refreshWindows() {
        Window[] windows = Window.getWindows();
        for (int i = 0; i < windows.length; i++) {
            Window window = windows[i];
            if (window != null) {
                SwingUtilities.updateComponentTreeUI(window);
                applySpecialWindowTheme(window);
                window.invalidate();
                window.validate();
                window.repaint();
            }
        }
    }
}

package com.bstudio.ro_toolbox;

import javax.swing.*;
import java.awt.*;

public final class UiTheme {
    public static final Color BG = new Color(9, 15, 22);
    public static final Color PANEL = new Color(21, 29, 38);
    public static final Color PANEL_ALT = new Color(31, 42, 55);
    public static final Color ACCENT = new Color(110, 231, 183);
    public static final Color ACCENT_2 = new Color(125, 211, 252);
    public static final Color TEXT = new Color(233, 241, 249);
    public static final Color MUTED = new Color(166, 180, 196);
    public static final Color BORDER = new Color(56, 73, 92);
    public static final Color BUTTON_BG = new Color(42, 57, 73);

    private UiTheme() {
    }

    public static void install() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
        }

        UIManager.put("control", PANEL);
        UIManager.put("info", PANEL_ALT);
        UIManager.put("nimbusBase", new Color(18, 28, 38));
        UIManager.put("nimbusBorder", BORDER);
        UIManager.put("nimbusFocus", ACCENT_2);
        UIManager.put("text", TEXT);
        UIManager.put("TextField.background", PANEL_ALT);
        UIManager.put("TextField.foreground", TEXT);
        UIManager.put("TextArea.background", PANEL_ALT);
        UIManager.put("TextArea.foreground", TEXT);
        UIManager.put("ComboBox.background", PANEL_ALT);
        UIManager.put("ComboBox.foreground", TEXT);
        UIManager.put("ComboBox.selectionBackground", ACCENT_2);
        UIManager.put("ComboBox.selectionForeground", BG);
        UIManager.put("List.background", PANEL_ALT);
        UIManager.put("List.foreground", TEXT);
        UIManager.put("List.selectionBackground", ACCENT_2);
        UIManager.put("List.selectionForeground", BG);
        UIManager.put("Button.background", BUTTON_BG);
        UIManager.put("Button.foreground", TEXT);
        UIManager.put("Button.disabledText", new Color(132, 144, 156));
        UIManager.put("OptionPane.background", BG);
        UIManager.put("Panel.background", BG);
        UIManager.put("OptionPane.messageForeground", TEXT);
        UIManager.put("ScrollBar.background", BG);
        UIManager.put("ScrollPane.background", BG);
        UIManager.put("PopupMenu.background", PANEL_ALT);
        UIManager.put("PopupMenu.foreground", TEXT);
    }

    public static void styleButton(AbstractButton button) {
        button.setBackground(BUTTON_BG);
        button.setForeground(TEXT);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public static void stylePrimaryButton(AbstractButton button) {
        styleButton(button);
        button.setBackground(new Color(12, 83, 68));
        button.setForeground(new Color(232, 252, 247));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT, 1),
                BorderFactory.createEmptyBorder(7, 14, 7, 14)
        ));
    }

    public static void styleSecondaryButton(AbstractButton button) {
        styleButton(button);
        button.setBackground(new Color(28, 35, 45));
        button.setForeground(MUTED);
    }

    public static void stylePanel(JComponent panel) {
        panel.setBackground(PANEL);
        panel.setForeground(TEXT);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)
        ));
    }

    public static void styleLabel(JLabel label) {
        label.setForeground(TEXT);
        label.setBackground(BG);
    }

    public static void styleFooter(JLabel footer) {
        footer.setForeground(MUTED);
        footer.setHorizontalAlignment(SwingConstants.CENTER);
        footer.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
    }
}

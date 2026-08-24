package com.bstudio.ro_toolbox.utils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public final class UiTheme {
    public static final Image TOOLBOX_ICON = createToolboxIcon();
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

    private static Image createToolboxIcon() {
        BufferedImage icon = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = icon.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        GradientPaint bg = new GradientPaint(0, 0, new Color(9, 15, 22), 0, 256, new Color(16, 121, 95));
        g.setPaint(bg);
        g.fillRoundRect(8, 8, 240, 240, 52, 52);

        g.setColor(new Color(0, 0, 0, 50));
        g.fillRoundRect(20, 22, 216, 196, 36, 36);

        int x = 58;
        int y = 72;
        int w = 140;
        int h = 118;

        // handle
        g.setColor(new Color(199, 137, 74));
        g.fillRoundRect(x + 20, y - 30, w - 40, 30, 18, 18);
        g.setColor(new Color(242, 194, 122));
        g.fillRoundRect(x + 32, y - 22, w - 64, 12, 8, 8);

        // body
        g.setColor(new Color(214, 154, 88));
        g.fillRoundRect(x, y, w, h, 24, 24);
        g.setColor(new Color(121, 82, 42));
        g.fillRoundRect(x + 8, y + 8, w - 16, h - 16, 18, 18);

        // top trim
        g.setColor(new Color(232, 192, 129));
        g.fillRoundRect(x + 16, y + 18, w - 32, 14, 10, 10);

        // clasp / lock
        g.setColor(new Color(76, 53, 34));
        g.fillRoundRect(x + 52, y + 46, 36, 24, 10, 10);
        g.setColor(new Color(239, 204, 149));
        g.fillRoundRect(x + 60, y + 50, 20, 16, 7, 7);

        // side details
        g.setColor(new Color(242, 219, 178));
        g.fillRect(x + 18, y + 54, 10, 18);
        g.fillRect(x + w - 28, y + 54, 10, 18);

        g.setColor(new Color(242, 219, 178));
        g.fillOval(x + 26, y + 88, 12, 12);
        g.fillOval(x + 59, y + 88, 12, 12);
        g.fillOval(x + 92, y + 88, 12, 12);
        g.fillOval(x + 125, y + 88, 12, 12);

        // lower panel
        g.setColor(new Color(255, 244, 223));
        g.fillRoundRect(x + 18, y + 100, w - 36, 42, 14, 14);
        g.setColor(new Color(97, 168, 122));
        g.fillRoundRect(x + 28, y + 106, w - 56, 12, 8, 8);
        g.fillRoundRect(x + 28, y + 122, w - 56, 10, 8, 8);

        g.dispose();
        return icon;
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

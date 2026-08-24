package com.bstudio.ro_toolbox;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

public class ServicesLauncher {
    public static void main(String[] args) {
        UiTheme.install();
        SwingUtilities.invokeLater(ServicesLauncher::createAndShow);
    }

    private static void createAndShow() {
        JFrame frame = new JFrame("RO Toolbox - Services");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                shutdownApp();
            }
        });
        frame.setSize(360, 210);
        frame.setLayout(new BorderLayout(8, 8));
        frame.getContentPane().setBackground(UiTheme.BG);

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(UiTheme.BG);
        JLabel title = new JLabel("Services");
        UiTheme.styleLabel(title);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        title.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        top.add(title, BorderLayout.WEST);

        // Settings (cog) button
        JButton settings = new JButton("⚙");
        settings.setToolTipText("Settings: choose the game installation folder used by Loot Manager.");
        UiTheme.styleButton(settings);
        top.add(settings, BorderLayout.EAST);

        frame.add(top, BorderLayout.NORTH);

        JPanel buttons = new JPanel();
        buttons.setLayout(new GridLayout(0, 1, 6, 6));
        buttons.setBackground(UiTheme.BG);
        buttons.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // instantiate singleton service
        LootManagerService svc = LootManagerService.getInstance();

        JLabel warningLabel = new JLabel("Select a game installation folder in Settings before using Loot Manager.");
        warningLabel.setForeground(new Color(255, 196, 96));
        warningLabel.setHorizontalAlignment(SwingConstants.CENTER);
        warningLabel.setVisible(svc.getSelectedGameBase() == null);
        warningLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        warningLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        // Loot Manager service button
        JButton lootBtn = new JButton("Loot Models");
        UiTheme.styleButton(lootBtn);
        lootBtn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        lootBtn.setToolTipText("Adjusts dropped item models and increases their size for easier looting.");
        lootBtn.addActionListener((ActionEvent e) -> {
            try {
                // hide launcher and open detailed GUI with shared service
                frame.setVisible(false);
                MainGui.open(svc, frame);
            } catch (Throwable t) {
                t.printStackTrace();
                JOptionPane.showMessageDialog(frame, "Failed to open Loot Manager: " + t.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                frame.setVisible(true);
            }
        });

        // disable if no installation base selected
        lootBtn.setEnabled(svc.getSelectedGameBase() != null);
        if (svc.getSelectedGameBase() == null) {
            lootBtn.setToolTipText("Select a game installation folder in Settings before using Loot Manager.");
        }

        buttons.add(warningLabel);
        buttons.add(lootBtn);

        frame.add(buttons, BorderLayout.CENTER);

        JLabel footer = new JLabel("Created by BStudio • v" + AppInfo.getVersion(), SwingConstants.CENTER);
        UiTheme.styleFooter(footer);
        footer.setBorder(BorderFactory.createEmptyBorder(4, 8, 6, 8));
        frame.add(footer, BorderLayout.SOUTH);

        // listen for changes so we can enable the loot button when user saves settings
        svc.addChangeListener(() -> SwingUtilities.invokeLater(() -> {
            boolean enabled = svc.getSelectedGameBase() != null;
            lootBtn.setEnabled(enabled);
            warningLabel.setVisible(!enabled);
            lootBtn.setToolTipText(enabled
                    ? "Adjusts dropped item models and increases their size for easier looting."
                    : "Select a game installation folder in Settings before using Loot Manager.");
        }));

        // Settings action: show current folder and allow change with validation
        settings.addActionListener((ActionEvent e) -> {
            try {
                Path current = svc.getSelectedGameBase();
                String currentText = (current == null) ? "Not set" : current.toAbsolutePath().toString();
                int opt = JOptionPane.showOptionDialog(frame,
                        "Current installation folder:\n" + currentText + "\n\nSelect the game install base folder to enable Loot Manager.",
                        "Settings",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.WARNING_MESSAGE,
                        null,
                        new Object[]{"Change folder", "Close"},
                        "Change folder");
                if (opt != 0) return; // user chose Close or closed dialog

                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                chooser.setDialogTitle("Select game installation folder (base)");
                int res = chooser.showOpenDialog(frame);
                if (res == JFileChooser.APPROVE_OPTION) {
                    Path picked = chooser.getSelectedFile().toPath();
                    Path base;
                    if (picked.endsWith(Path.of("3ddata", "item"))) {
                        // user selected the item folder; derive base (two parents up)
                        Path p = picked.getParent();
                        if (p != null) p = p.getParent();
                        base = (p != null) ? p : picked;
                    } else {
                        base = picked;
                    }

                    Path itemFolder = base.resolve(Path.of("3ddata", "item"));
                    boolean valid = Files.exists(itemFolder) && Files.isDirectory(itemFolder);
                    if (!valid) {
                        int confirm = JOptionPane.showConfirmDialog(frame,
                                "The chosen base folder does not contain '3ddata/item'. Save base anyway?",
                                "Validate installation folder",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.WARNING_MESSAGE);
                        if (confirm != JOptionPane.YES_OPTION) return;
                    }

                    Files.createDirectories(base);
                    svc.saveSelectedGame(base);
                    JOptionPane.showMessageDialog(frame, "Saved installation folder (base): " + base.toAbsolutePath());
                }
            } catch (Exception ex) {
                StringWriter sw = new StringWriter(); ex.printStackTrace(new PrintWriter(sw));
                JOptionPane.showMessageDialog(frame, "Failed to save settings: " + ex.getMessage() + "\n" + sw.toString(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static void shutdownApp() {
        SwingUtilities.invokeLater(() -> {
            System.exit(0);
        });
    }
}

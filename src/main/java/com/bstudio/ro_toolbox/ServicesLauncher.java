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
        SwingUtilities.invokeLater(ServicesLauncher::createAndShow);
    }

    private static void createAndShow() {
        JFrame frame = new JFrame("RO Toolbox - Services");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(360, 180);
        frame.setLayout(new BorderLayout(8, 8));

        JPanel top = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Services");
        title.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        top.add(title, BorderLayout.WEST);

        // Settings (cog) button
        JButton settings = new JButton("⚙");
        settings.setToolTipText("Settings");
        top.add(settings, BorderLayout.EAST);

        frame.add(top, BorderLayout.NORTH);

        JPanel buttons = new JPanel();
        buttons.setLayout(new GridLayout(0, 1, 6, 6));
        buttons.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // instantiate singleton service
        LootManagerService svc = LootManagerService.getInstance();

        // Loot Manager service button
        JButton lootBtn = new JButton("Loot Manager");
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

        buttons.add(lootBtn);

        frame.add(buttons, BorderLayout.CENTER);

        // Settings action: show current folder and allow change with validation
        settings.addActionListener((ActionEvent e) -> {
            try {
                Path current = svc.getSelectedGameBase();
                String currentText = (current == null) ? "Not set" : current.toAbsolutePath().toString();
                int opt = JOptionPane.showOptionDialog(frame,
                        "Current installation folder:\n" + currentText,
                        "Settings",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.INFORMATION_MESSAGE,
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
}

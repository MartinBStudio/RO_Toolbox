package com.bstudio.ro_toolbox;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class ServicesLauncher {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(ServicesLauncher::createAndShow);
    }

    private static void createAndShow() {
        JFrame frame = new JFrame("RO Toolbox - Services");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 150);
        frame.setLayout(new BorderLayout(8, 8));

        JPanel buttons = new JPanel();
        buttons.setLayout(new GridLayout(0, 1, 6, 6));
        buttons.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Loot Manager service button
        JButton lootBtn = new JButton("Loot Manager");
        lootBtn.addActionListener((ActionEvent e) -> {
            // Open the detailed Loot Manager GUI in a separate window
            try {
                MainGui.main(new String[0]);
            } catch (Throwable t) {
                t.printStackTrace();
                JOptionPane.showMessageDialog(frame, "Failed to open Loot Manager: " + t.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        buttons.add(lootBtn);

        // Future services can be added here easily

        frame.add(buttons, BorderLayout.CENTER);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}

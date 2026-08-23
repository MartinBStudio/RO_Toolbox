package com.bstudio.ro_toolbox;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MainGui {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainGui::createAndShowGui);
    }

    private static void createAndShowGui() {
        JFrame frame = new JFrame("RO LootManager - Resources Puller");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(700, 380);

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JButton pullBtn = new JButton("Pull resources");

        JTextArea log = new JTextArea();
        log.setEditable(false);
        JScrollPane logScroll = new JScrollPane(log);
        logScroll.setPreferredSize(new Dimension(680, 140));

        DefaultListModel<String> listModel = new DefaultListModel<>();
        JList<String> folderList = new JList<>(listModel);
        folderList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane folderScroll = new JScrollPane(folderList);
        folderScroll.setPreferredSize(new Dimension(680, 160));

        JPanel resourcesPanel = new JPanel();
        resourcesPanel.setLayout(new BoxLayout(resourcesPanel, BoxLayout.Y_AXIS));
        resourcesPanel.setBorder(BorderFactory.createTitledBorder("Resources"));
        JButton browseBtn = new JButton("Browse resources");
        JButton clearResourcesBtn = new JButton("Clear resources");

        resourcesPanel.add(pullBtn);
        resourcesPanel.add(Box.createVerticalStrut(6));
        resourcesPanel.add(browseBtn);
        resourcesPanel.add(Box.createVerticalStrut(6));
        resourcesPanel.add(clearResourcesBtn);

        JPanel gamePanel = new JPanel();
        gamePanel.setLayout(new BoxLayout(gamePanel, BoxLayout.Y_AXIS));
        gamePanel.setBorder(BorderFactory.createTitledBorder("Game Files"));
        JButton browseItemBtn = new JButton("Browse item folder");
        JButton selectGameBtn = new JButton("Select game folder");
        JButton clearItemBtn = new JButton("Clear item folder");
        JButton patchBtn = new JButton("Patch poc data");
        JLabel destLabel = new JLabel("Destination: resources");

        gamePanel.add(selectGameBtn);
        gamePanel.add(Box.createVerticalStrut(6));
        gamePanel.add(browseItemBtn);
        gamePanel.add(Box.createVerticalStrut(6));
        gamePanel.add(clearItemBtn);
        gamePanel.add(Box.createVerticalStrut(6));
        gamePanel.add(patchBtn);
        gamePanel.add(Box.createVerticalStrut(10));
        gamePanel.add(destLabel);

        JPanel topWrapper = new JPanel(new GridLayout(1, 2, 8, 8));
        topWrapper.add(resourcesPanel);
        topWrapper.add(gamePanel);

        panel.add(topWrapper, BorderLayout.NORTH);
        panel.add(folderScroll, BorderLayout.CENTER);
        panel.add(logScroll, BorderLayout.SOUTH);
        frame.setContentPane(panel);

        // Create service with logger that appends safely to JTextArea
        LootManagerService svc = new LootManagerService(msg -> SwingUtilities.invokeLater(() -> log.append(msg)));

        if (svc.getSelectedGameDest() != null) {
            destLabel.setText("Destination: " + svc.getSelectedGameDest().toAbsolutePath());
            log.append("Loaded saved destination: " + svc.getSelectedGameDest().toAbsolutePath() + "\n");
        }

        Runnable refreshView = () -> {
            Path root = svc.getCurrentResourcesRoot();
            SwingUtilities.invokeLater(() -> {
                listModel.clear();
                try {
                    if (root != null && Files.exists(root) && Files.isDirectory(root)) {
                        try (var ds = Files.newDirectoryStream(root)) {
                            for (Path p : ds) {
                                String name = p.getFileName().toString() + (Files.isDirectory(p) ? java.io.File.separator : "");
                                listModel.addElement(name);
                            }
                        }
                    }
                } catch (Exception ignored) { }
            });
        };

        // Browse resources
        browseBtn.addActionListener((ActionEvent e) -> {
            try {
                Path toOpen = svc.getResourcesDir();
                if (!Files.exists(toOpen)) Files.createDirectories(toOpen);
                if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(toOpen.toFile());
                svc.log("Opened resources folder: " + toOpen.toAbsolutePath());
            } catch (Exception ex) {
                StringWriter sw = new StringWriter(); ex.printStackTrace(new PrintWriter(sw));
                SwingUtilities.invokeLater(() -> log.append("Error opening resources folder: " + ex.getMessage() + "\n" + sw.toString()));
            }
        });

        // Browse item folder
        browseItemBtn.addActionListener((ActionEvent e) -> {
            try {
                Path toOpen = svc.getSelectedGameDest();
                if (toOpen == null) { svc.log("No game destination selected. Use 'Select game folder' first."); return; }
                if (!Files.exists(toOpen)) Files.createDirectories(toOpen);
                if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(toOpen.toFile());
                svc.log("Opened item folder: " + toOpen.toAbsolutePath());
            } catch (Exception ex) {
                StringWriter sw = new StringWriter(); ex.printStackTrace(new PrintWriter(sw));
                SwingUtilities.invokeLater(() -> log.append("Error opening item folder: " + ex.getMessage() + "\n" + sw.toString()));
            }
        });

        // Clear resources
        clearResourcesBtn.addActionListener((ActionEvent e) -> {
            clearResourcesBtn.setEnabled(false);
            new Thread(() -> {
                try {
                    svc.clearResources();
                    SwingUtilities.invokeLater(() -> {
                        log.append("Resources cleared.\n");
                        refreshView.run();
                    });
                } catch (Exception ex) {
                    StringWriter sw = new StringWriter(); ex.printStackTrace(new PrintWriter(sw));
                    SwingUtilities.invokeLater(() -> log.append("Error clearing resources: " + ex.getMessage() + "\n" + sw.toString()));
                } finally {
                    SwingUtilities.invokeLater(() -> clearResourcesBtn.setEnabled(true));
                }
            }).start();
        });

        // Clear item folder
        clearItemBtn.addActionListener((ActionEvent e) -> {
            clearItemBtn.setEnabled(false);
            new Thread(() -> {
                try {
                    svc.clearSelectedItemFolder();
                    SwingUtilities.invokeLater(() -> {
                        log.append("Item folder cleared.\n");
                        refreshView.run();
                    });
                } catch (Exception ex) {
                    StringWriter sw = new StringWriter(); ex.printStackTrace(new PrintWriter(sw));
                    SwingUtilities.invokeLater(() -> log.append("Error clearing item folder: " + ex.getMessage() + "\n" + sw.toString()));
                } finally {
                    SwingUtilities.invokeLater(() -> clearItemBtn.setEnabled(true));
                }
            }).start();
        });

        // Pull resources
        pullBtn.addActionListener((ActionEvent e) -> {
            pullBtn.setEnabled(false);
            log.append("Starting pull from default repo\n");
            new Thread(() -> {
                try {
                    Path dest = svc.getResourcesDir();
                    Files.createDirectories(dest);
                    svc.downloadAndExtract(null, dest);
                    SwingUtilities.invokeLater(() -> {
                        log.append("Done. Resources saved to: " + dest.toAbsolutePath() + "\n");
                        refreshView.run();
                    });
                } catch (Exception ex) {
                    StringWriter sw = new StringWriter(); ex.printStackTrace(new PrintWriter(sw));
                    SwingUtilities.invokeLater(() -> log.append("Error: " + ex.getMessage() + "\n" + sw.toString()));
                } finally {
                    SwingUtilities.invokeLater(() -> pullBtn.setEnabled(true));
                }
            }).start();
        });

        // Select game folder
        selectGameBtn.addActionListener((ActionEvent e) -> {
            try {
                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                chooser.setDialogTitle("Select game installation folder (base)");
                int res = chooser.showOpenDialog(frame);
                if (res == JFileChooser.APPROVE_OPTION) {
                    Path picked = chooser.getSelectedFile().toPath();
                    Path selected;
                    if (picked.endsWith(Paths.get("3ddata", "item"))) selected = picked;
                    else selected = picked.resolve(Paths.get("3ddata", "item"));
                    Files.createDirectories(selected);
                    svc.saveSelectedGame(selected);
                    SwingUtilities.invokeLater(() -> {
                        destLabel.setText("Destination: " + selected.toAbsolutePath());
                        log.append("Selected game destination: " + selected.toAbsolutePath() + "\n");
                        refreshView.run();
                    });
                }
            } catch (Exception ex) {
                StringWriter sw = new StringWriter(); ex.printStackTrace(new PrintWriter(sw));
                SwingUtilities.invokeLater(() -> log.append("Error selecting game folder: " + ex.getMessage() + "\n" + sw.toString()));
            }
        });

        // Patch
        patchBtn.addActionListener((ActionEvent e) -> {
            String input = JOptionPane.showInputDialog(frame, "Enter resources subfolder name to patch from:", "poc");
            if (input == null) return;
            String folderName = input.trim().isEmpty() ? "poc" : input.trim();
            patchBtn.setEnabled(false);
            new Thread(() -> {
                try {
                    if (svc.getSelectedGameDest() == null) { svc.log("No game destination selected. Use 'Select game folder' first."); return; }
                    var pocSource = svc.findPocSource(folderName);
                    if (pocSource == null) { svc.log("Source folder not found: " + folderName); return; }
                    svc.copyDirectoryContents(pocSource, svc.getSelectedGameDest());
                    SwingUtilities.invokeLater(() -> { log.append("Patch completed: " + folderName + " copied.\n"); refreshView.run(); });
                } catch (Exception ex) {
                    StringWriter sw = new StringWriter(); ex.printStackTrace(new PrintWriter(sw));
                    SwingUtilities.invokeLater(() -> log.append("Error patching destination: " + ex.getMessage() + "\n" + sw.toString()));
                } finally {
                    SwingUtilities.invokeLater(() -> patchBtn.setEnabled(true));
                }
            }).start();
        });

        folderList.addListSelectionListener(evt -> {
            if (!evt.getValueIsAdjusting()) {
                String val = folderList.getSelectedValue(); if (val != null) SwingUtilities.invokeLater(() -> log.append("Selected: " + val + "\n"));
            }
        });

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // initial view
        refreshView.run();
    }
}

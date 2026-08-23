package com.bstudio.ro_toolbox;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class MainGui {
    // Open the Loot Manager GUI using the provided service. Parent frame (services launcher) may be passed
    public static void open(LootManagerService svc, JFrame parent) {
        SwingUtilities.invokeLater(() -> createAndShowGui(svc, parent));
    }

    public static void main(String[] args) {
        UiTheme.install();
        // For backward compatibility: use singleton service without GUI logger
        open(LootManagerService.getInstance(), null);
    }

    private static void createAndShowGui(LootManagerService svc, JFrame parent) {

        JFrame frame = new JFrame("RO LootManager - Resources Puller");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(700, 420);
        frame.getContentPane().setBackground(UiTheme.BG);

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(UiTheme.BG);

        // Top bar with back button
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(UiTheme.BG);
        JButton backBtn = new JButton("← Back");
        UiTheme.styleButton(backBtn);
        backBtn.addActionListener((ActionEvent e) -> {
            frame.dispose();
            if (parent != null) parent.setVisible(true);
        });
        topBar.add(backBtn, BorderLayout.WEST);

        panel.add(topBar, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(8,8));
        center.setBackground(UiTheme.BG);
        JButton pullBtn = new JButton("Pull resources");
        UiTheme.styleButton(pullBtn);

        JTextArea log = new JTextArea();
        log.setEditable(false);
        log.setBackground(UiTheme.PANEL_ALT);
        log.setForeground(UiTheme.TEXT);
        log.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        log.setCaretColor(UiTheme.ACCENT_2);
        JScrollPane logScroll = new JScrollPane(log);
        logScroll.setPreferredSize(new Dimension(680, 140));
        logScroll.setBackground(UiTheme.BG);

        DefaultListModel<String> listModel = new DefaultListModel<>();
        JList<String> folderList = new JList<>(listModel);
        folderList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        folderList.setBackground(UiTheme.PANEL_ALT);
        folderList.setForeground(UiTheme.TEXT);
        folderList.setSelectionBackground(UiTheme.ACCENT_2);
        folderList.setSelectionForeground(UiTheme.BG);
        JScrollPane folderScroll = new JScrollPane(folderList);
        folderScroll.setPreferredSize(new Dimension(680, 160));
        folderScroll.setBackground(UiTheme.BG);

        JPanel resourcesPanel = new JPanel();
        resourcesPanel.setLayout(new BoxLayout(resourcesPanel, BoxLayout.Y_AXIS));
        resourcesPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(UiTheme.BORDER), "Resources"));
        resourcesPanel.setBackground(UiTheme.PANEL);
        resourcesPanel.setForeground(UiTheme.TEXT);
        JButton browseBtn = new JButton("Browse resources");
        JButton clearResourcesBtn = new JButton("Clear resources");
        UiTheme.styleButton(browseBtn);
        UiTheme.styleButton(clearResourcesBtn);

        resourcesPanel.add(pullBtn);
        resourcesPanel.add(Box.createVerticalStrut(6));
        resourcesPanel.add(browseBtn);
        resourcesPanel.add(Box.createVerticalStrut(6));
        resourcesPanel.add(clearResourcesBtn);

        JPanel gamePanel = new JPanel();
        gamePanel.setLayout(new BoxLayout(gamePanel, BoxLayout.Y_AXIS));
        gamePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(UiTheme.BORDER), "Game Files"));
        gamePanel.setBackground(UiTheme.PANEL);
        gamePanel.setForeground(UiTheme.TEXT);
        JButton browseItemBtn = new JButton("Browse item folder");
        JButton clearItemBtn = new JButton("Clear item folder");
        JButton patchBtn = new JButton("Patch loot models");
        UiTheme.styleButton(browseItemBtn);
        UiTheme.styleButton(clearItemBtn);
        UiTheme.styleButton(patchBtn);
        JLabel destLabel = new JLabel("Destination: " + (svc.getSelectedGameItemFolder() != null ? svc.getSelectedGameItemFolder().toAbsolutePath() : "resources"));
        UiTheme.styleLabel(destLabel);

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

        center.add(topWrapper, BorderLayout.NORTH);
        center.add(folderScroll, BorderLayout.CENTER);
        center.add(logScroll, BorderLayout.SOUTH);

        panel.add(center, BorderLayout.CENTER);

        JLabel footer = new JLabel("Created by BStudio • v" + AppInfo.getVersion(), SwingConstants.CENTER);
        UiTheme.styleFooter(footer);
        footer.setBorder(BorderFactory.createEmptyBorder(4, 8, 6, 8));
        panel.add(footer, BorderLayout.SOUTH);

        frame.setContentPane(panel);

        // Wire service logger to UI log area
        svc.setLogger(msg -> SwingUtilities.invokeLater(() -> log.append(msg)));

        // enable/disable UI depending on whether item folder is configured
        Runnable updateEnabled = () -> SwingUtilities.invokeLater(() -> {
            boolean itemConfigured = svc.getSelectedGameItemFolder() != null;
            browseItemBtn.setEnabled(itemConfigured);
            clearItemBtn.setEnabled(itemConfigured && svc.isDirectoryNonEmpty(svc.getSelectedGameItemFolder()));
            patchBtn.setEnabled(itemConfigured);
            // resources operations remain available
        });
        // initial state
        updateEnabled.run();
        // listen for changes
        svc.addChangeListener(updateEnabled);

        if (svc.getSelectedGameBase() != null) {
            log.append("Loaded saved installation folder: " + svc.getSelectedGameBase().toAbsolutePath() + "\n");
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
                svc.guiMessage("Opened resources folder: " + toOpen.toAbsolutePath());
            } catch (Exception ex) {
                StringWriter sw = new StringWriter(); ex.printStackTrace(new PrintWriter(sw));
                SwingUtilities.invokeLater(() -> log.append("Error opening resources folder: " + ex.getMessage() + "\n" + sw.toString()));
            }
        });

        // Browse item folder
        browseItemBtn.addActionListener((ActionEvent e) -> {
            try {
                Path toOpen = svc.getSelectedGameItemFolder();
                if (toOpen == null) { svc.guiMessage("No game destination selected. Set it in Services -> Settings."); return; }
                if (!Files.exists(toOpen)) Files.createDirectories(toOpen);
                if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(toOpen.toFile());
                svc.guiMessage("Opened item folder: " + toOpen.toAbsolutePath());
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

        // Patch (choose existing resource subfolder with manifest.json)
        patchBtn.setToolTipText("Applies a loot profile to dropped item models and enlarges them for easier looting.");
        patchBtn.addActionListener((ActionEvent e) -> {
            List<PatchChoice> choices = collectPatchChoices(svc);
            if (choices.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "No resource folders with manifest.json were found under 'resources' or the alternate resources folder.", "No sources", JOptionPane.WARNING_MESSAGE);
                return;
            }

            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
            for (PatchChoice c : choices) model.addElement(c.label);
            JComboBox<String> combo = new JComboBox<>(model);
            combo.setBackground(UiTheme.PANEL_ALT);
            combo.setForeground(UiTheme.TEXT);
            combo.setRenderer(new DefaultListCellRenderer() {
                @Override
                public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    setBackground(isSelected ? UiTheme.ACCENT_2 : UiTheme.PANEL_ALT);
                    setForeground(isSelected ? UiTheme.BG : UiTheme.TEXT);
                    setOpaque(true);
                    return this;
                }
            });

            JPanel dialogPanel = new JPanel(new BorderLayout(8, 8));
            dialogPanel.setBackground(UiTheme.BG);
            JLabel info = new JLabel("Choose a loot profile. This adapts dropped item models and enlarges them for easier looting.");
            info.setForeground(UiTheme.TEXT);
            info.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
            dialogPanel.add(info, BorderLayout.NORTH);
            dialogPanel.add(combo, BorderLayout.CENTER);

            int choice = JOptionPane.showConfirmDialog(frame, dialogPanel, "Select loot profile", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (choice != JOptionPane.OK_OPTION) return;

            String selectedLabel = (String) combo.getSelectedItem();
            if (selectedLabel == null || selectedLabel.trim().isEmpty()) return;
            PatchChoice selected = choices.stream().filter(c -> c.label.equals(selectedLabel)).findFirst().orElse(null);
            if (selected == null) return;

            patchBtn.setEnabled(false);
            new Thread(() -> {
                try {
                    if (svc.getSelectedGameItemFolder() == null) { svc.guiMessage("No game destination selected. Set it in Services -> Settings."); return; }
                    // Clear destination before copy to ensure a clean patch.
                    svc.clearSelectedItemFolder();
                    svc.copyDirectoryContents(selected.source, svc.getSelectedGameItemFolder());
                    SwingUtilities.invokeLater(() -> { log.append("Patch completed: " + selected.source.getFileName() + " copied.\n"); refreshView.run(); });
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

    private static final class PatchChoice {
        private final String label;
        private final Path source;

        private PatchChoice(String label, Path source) {
            this.label = label;
            this.source = source;
        }
    }

    private static List<PatchChoice> collectPatchChoices(LootManagerService svc) {
        List<PatchChoice> results = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        List<Path> roots = new ArrayList<>();
        roots.add(svc.getResourcesDir());
        if (svc.getSelectedGameBase() != null) {
            roots.add(svc.getSelectedGameBase().resolveSibling(svc.getResourcesDir().getFileName()));
        }

        for (Path root : roots) {
            if (root == null || !Files.exists(root) || !Files.isDirectory(root)) continue;
            try (var stream = Files.walk(root)) {
                for (Path p : (Iterable<Path>) stream::iterator) {
                    if (!Files.isDirectory(p)) continue;
                    String name = p.getFileName().toString();
                    if (name.startsWith(".")) continue;
                    Path manifest = p.resolve("manifest.json");
                    if (!Files.exists(manifest) || !Files.isRegularFile(manifest)) continue;
                    String desc = readManifestDescription(manifest);
                    String label = (desc == null || desc.isBlank()) ? name : name + " - " + desc;
                    if (seen.add(label)) {
                        results.add(new PatchChoice(label, p));
                    }
                }
            } catch (IOException ignored) {
            }
        }

        results.sort((a, b) -> a.label.compareToIgnoreCase(b.label));
        return results;
    }

    private static String readManifestDescription(Path manifestFile) {
        try {
            String content = Files.readString(manifestFile);
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\"description\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"").matcher(content);
            if (!matcher.find()) return null;
            String value = matcher.group(1).replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
            return value.trim();
        } catch (Exception e) {
            return null;
        }
    }
}

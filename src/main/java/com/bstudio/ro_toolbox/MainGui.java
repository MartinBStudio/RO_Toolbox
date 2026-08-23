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
        frame.setSize(700, 290);
        frame.getContentPane().setBackground(UiTheme.BG);

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(UiTheme.BG);

        // Top bar with back button
        JPanel topBar = new JPanel(new BorderLayout(8, 8));
        topBar.setBackground(UiTheme.BG);
        topBar.setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));

        JButton backBtn = new JButton("← Services");
        backBtn.setPreferredSize(new Dimension(120, 32));
        UiTheme.styleButton(backBtn);
        backBtn.addActionListener((ActionEvent e) -> {
            frame.dispose();
            if (parent != null) parent.setVisible(true);
        });

        JButton logBtn = new JButton("Log");
        logBtn.setPreferredSize(new Dimension(90, 32));
        UiTheme.styleSecondaryButton(logBtn);

        JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftActions.setOpaque(false);
        leftActions.add(backBtn);

        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightActions.setOpaque(false);
        rightActions.add(logBtn);

        topBar.add(leftActions, BorderLayout.WEST);
        topBar.add(rightActions, BorderLayout.EAST);

        JTextArea log = new JTextArea();
        log.setEditable(false);
        log.setBackground(UiTheme.PANEL_ALT);
        log.setForeground(UiTheme.TEXT);
        log.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        log.setCaretColor(UiTheme.ACCENT_2);

        final JFrame[] logFrameHolder = new JFrame[1];
        logBtn.addActionListener((ActionEvent e) -> {
            JFrame logFrame = logFrameHolder[0];
            if (logFrame == null || !logFrame.isDisplayable()) {
                logFrame = new JFrame("RO LootManager Log");
                logFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                logFrame.setSize(680, 260);
                JTextArea logWindowArea = new JTextArea();
                logWindowArea.setEditable(false);
                logWindowArea.setBackground(UiTheme.PANEL_ALT);
                logWindowArea.setForeground(UiTheme.TEXT);
                logWindowArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
                logWindowArea.setCaretColor(UiTheme.ACCENT_2);
                logWindowArea.setDocument(log.getDocument());
                logWindowArea.setCaretPosition(log.getDocument().getLength());
                logFrame.add(new JScrollPane(logWindowArea));
                logFrame.setLocationRelativeTo(frame);
                logFrame.setVisible(true);
                logFrameHolder[0] = logFrame;
            } else {
                logFrame.toFront();
                logFrame.setVisible(true);
            }
        });
        JLabel installWarning = new JLabel("Select a game installation folder in Settings before using Loot Manager.");
        installWarning.setForeground(new Color(255, 196, 96));
        installWarning.setHorizontalAlignment(SwingConstants.CENTER);
        installWarning.setBorder(BorderFactory.createEmptyBorder(4, 8, 0, 8));
        installWarning.setVisible(svc.getSelectedGameBase() == null);

        JPanel topSection = new JPanel(new BorderLayout(0, 4));
        topSection.setOpaque(false);
        topSection.add(topBar, BorderLayout.NORTH);
        topSection.add(installWarning, BorderLayout.SOUTH);

        panel.add(topSection, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(8,8));
        center.setBackground(UiTheme.BG);
        JButton pullBtn = new JButton("Pull resources");
        UiTheme.stylePrimaryButton(pullBtn);

        JPanel resourcesPanel = new JPanel();
        resourcesPanel.setLayout(new BoxLayout(resourcesPanel, BoxLayout.Y_AXIS));
        resourcesPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(UiTheme.BORDER), "Resources"));
        resourcesPanel.setBackground(UiTheme.PANEL);
        resourcesPanel.setForeground(UiTheme.TEXT);
        JButton browseBtn = new JButton("Browse");
        JButton clearResourcesBtn = new JButton("Clear");
        UiTheme.styleSecondaryButton(browseBtn);
        UiTheme.styleSecondaryButton(clearResourcesBtn);

        JPanel pullRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        pullRow.setOpaque(false);
        pullRow.add(pullBtn);

        JPanel resourcesActions = new JPanel(new GridLayout(1, 2, 6, 0));
        resourcesActions.setOpaque(false);
        resourcesActions.add(browseBtn);
        resourcesActions.add(clearResourcesBtn);

        resourcesPanel.add(pullRow);
        resourcesPanel.add(Box.createVerticalStrut(8));
        resourcesPanel.add(resourcesActions);

        JPanel gamePanel = new JPanel();
        gamePanel.setLayout(new BoxLayout(gamePanel, BoxLayout.Y_AXIS));
        gamePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(UiTheme.BORDER), "Game Files"));
        gamePanel.setBackground(UiTheme.PANEL);
        gamePanel.setForeground(UiTheme.TEXT);
        JButton browseItemBtn = new JButton("Browse");
        JButton clearItemBtn = new JButton("Clear");
        JButton patchBtn = new JButton("Patch loot models");
        UiTheme.styleSecondaryButton(browseItemBtn);
        UiTheme.styleSecondaryButton(clearItemBtn);
        UiTheme.stylePrimaryButton(patchBtn);
        JPanel patchRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        patchRow.setOpaque(false);
        patchRow.add(patchBtn);

        JPanel itemActions = new JPanel(new GridLayout(1, 2, 6, 0));
        itemActions.setOpaque(false);
        itemActions.add(browseItemBtn);
        itemActions.add(clearItemBtn);

        gamePanel.add(patchRow);
        gamePanel.add(Box.createVerticalStrut(8));
        gamePanel.add(itemActions);

        JPanel topWrapper = new JPanel(new GridLayout(1, 2, 8, 8));
        topWrapper.add(resourcesPanel);
        topWrapper.add(gamePanel);

        JLabel loadedProfileLabel = new JLabel("Loaded profile: none");
        UiTheme.styleLabel(loadedProfileLabel);
        loadedProfileLabel.setHorizontalAlignment(SwingConstants.CENTER);
        loadedProfileLabel.setBorder(BorderFactory.createEmptyBorder(10, 8, 4, 8));
        loadedProfileLabel.setVisible(false);

        JPanel profilePanel = new JPanel(new BorderLayout());
        profilePanel.setOpaque(false);
        profilePanel.add(topWrapper, BorderLayout.NORTH);
        profilePanel.add(loadedProfileLabel, BorderLayout.SOUTH);

        center.add(profilePanel, BorderLayout.NORTH);

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
            Path itemFolder = svc.getSelectedGameItemFolder();
            boolean itemHasContent = itemConfigured && itemFolder != null && svc.isDirectoryNonEmpty(itemFolder);
            browseItemBtn.setEnabled(itemConfigured);
            clearItemBtn.setEnabled(itemHasContent);
            patchBtn.setEnabled(itemConfigured);
            installWarning.setVisible(svc.getSelectedGameBase() == null);
            String currentProfile = resolveLoadedProfile(svc.getSelectedGameItemFolder());
            if (currentProfile != null && !currentProfile.isBlank()) {
                loadedProfileLabel.setText("Loaded profile: " + currentProfile);
                loadedProfileLabel.setVisible(true);
            } else {
                loadedProfileLabel.setVisible(false);
            }
            // resources operations remain available
        });
        // initial state
        updateEnabled.run();
        // listen for changes
        svc.addChangeListener(updateEnabled);

        if (svc.getSelectedGameBase() != null) {
            log.append("Loaded saved installation folder: " + svc.getSelectedGameBase().toAbsolutePath() + "\n");
        }

        // Browse resources
        browseBtn.addActionListener((ActionEvent e) -> {
            try {
                Path toOpen = svc.getResourcesDir();
                if (!Files.exists(toOpen)) Files.createDirectories(toOpen);
                if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(toOpen.toFile());
                svc.guiMessage("Opened resources folder: " + toOpen.toAbsolutePath());
            } catch (Exception ex) {
                StringWriter sw = new StringWriter(); ex.printStackTrace(new PrintWriter(sw));
                SwingUtilities.invokeLater(() -> log.append("Error opening resources folder: " + ex.getMessage() + "\n" + sw));
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
                SwingUtilities.invokeLater(() -> log.append("Error opening item folder: " + ex.getMessage() + "\n" + sw));
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
                    updateEnabled.run();
                    });
                } catch (Exception ex) {
                    StringWriter sw = new StringWriter(); ex.printStackTrace(new PrintWriter(sw));
                    SwingUtilities.invokeLater(() -> log.append("Error clearing resources: " + ex.getMessage() + "\n" + sw));
                } finally {
                    SwingUtilities.invokeLater(() -> {
                        clearResourcesBtn.setEnabled(true);
                        updateEnabled.run();
                    });
                }
            }).start();
        });

        // Clear item folder
        clearItemBtn.addActionListener((ActionEvent e) -> {
            clearItemBtn.setEnabled(false);
            new Thread(() -> {
                try {
                    svc.clearSelectedItemFolder();
                    svc.setCurrentLootProfile(null);
                    SwingUtilities.invokeLater(() -> {
                        log.append("Item folder cleared.\n");
                        updateEnabled.run();
                    });
                } catch (Exception ex) {
                    StringWriter sw = new StringWriter(); ex.printStackTrace(new PrintWriter(sw));
                    SwingUtilities.invokeLater(() -> log.append("Error clearing item folder: " + ex.getMessage() + "\n" + sw));
                } finally {
                    SwingUtilities.invokeLater(() -> {
                        clearItemBtn.setEnabled(true);
                        updateEnabled.run();
                    });
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
                    });
                } catch (Exception ex) {
                    StringWriter sw = new StringWriter(); ex.printStackTrace(new PrintWriter(sw));
                    SwingUtilities.invokeLater(() -> log.append("Error: " + ex.getMessage() + "\n" + sw));
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
            combo.setSelectedIndex(0);
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
                    svc.setCurrentLootProfile(selected.label);
                    SwingUtilities.invokeLater(() -> {
                        log.append("Patch completed: " + selected.source.getFileName() + " copied.\n");
                        updateEnabled.run();
                    });
                } catch (Exception ex) {
                    StringWriter sw = new StringWriter(); ex.printStackTrace(new PrintWriter(sw));
                    SwingUtilities.invokeLater(() -> log.append("Error patching destination: " + ex.getMessage() + "\n" + sw));
                } finally {
                    SwingUtilities.invokeLater(() -> {
                        patchBtn.setEnabled(true);
                        updateEnabled.run();
                    });
                }
            }).start();
        });

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static final class PatchChoice {
        private final String label;
        private final Path source;
        private final long versionValue;

        private PatchChoice(String label, Path source, long versionValue) {
            this.label = label;
            this.source = source;
            this.versionValue = versionValue;
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
                    String version = readManifestVersion(manifest);
                    String label = (desc == null || desc.isBlank()) ? name : name + " - " + desc;
                    if (seen.add(label)) {
                        results.add(new PatchChoice(label, p, normalizeVersion(version)));
                    }
                }
            } catch (IOException ignored) {
            }
        }

        results.sort((a, b) -> {
            int versionDiff = Long.compare(b.versionValue, a.versionValue);
            if (versionDiff != 0) return versionDiff;
            return a.label.compareToIgnoreCase(b.label);
        });
        return results;
    }

    private static String resolveLoadedProfile(Path itemFolder) {
        if (itemFolder == null || !Files.exists(itemFolder) || !Files.isDirectory(itemFolder)) return null;
        try (var stream = Files.walk(itemFolder)) {
            for (Path p : (Iterable<Path>) stream::iterator) {
                if (!Files.isRegularFile(p)) continue;
                if (!p.getFileName().toString().equals("manifest.json")) continue;
                String name = readManifestName(p);
                String desc = readManifestDescription(p);
                if (name != null && !name.isBlank()) {
                    return (desc != null && !desc.isBlank()) ? name + " - " + desc : name;
                }
                return desc;
            }
        } catch (IOException ignored) {
        }
        return null;
    }

    private static String readManifestName(Path manifestFile) {
        try {
            String content = Files.readString(manifestFile);
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\"name\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"").matcher(content);
            if (!matcher.find()) return null;
            String value = matcher.group(1).replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
            return value.trim();
        } catch (Exception e) {
            return null;
        }
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

    private static String readManifestVersion(Path manifestFile) {
        try {
            String content = Files.readString(manifestFile);
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\"version\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"").matcher(content);
            if (!matcher.find()) return "0.0.0";
            return matcher.group(1).trim();
        } catch (Exception e) {
            return "0.0.0";
        }
    }

    private static long normalizeVersion(String version) {
        if (version == null || version.isBlank()) return 0L;
        String cleaned = version.trim().replaceFirst("(?i)^v", "");
        String[] parts = cleaned.split("[.-]");
        long value = 0L;
        long multiplier = 1_000_000_000L;
        for (String part : parts) {
            if (part == null || part.isBlank()) continue;
            String digits = part.replaceAll("[^0-9]", "");
            if (digits.isEmpty()) continue;
            value += Long.parseLong(digits) * multiplier;
            multiplier /= 1000L;
        }
        return value;
    }
}

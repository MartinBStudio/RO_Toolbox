package com.bstudio.ro_toolbox.service.lootModels;

import com.bstudio.ro_toolbox.RoToolboxApplication;
import com.bstudio.ro_toolbox.utils.UiTheme;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

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

@Component
public class LootModelsGui {
    private final LootManagerService lootManagerService;
    private final RoToolboxApplication app;

    @Autowired
    public LootModelsGui(LootManagerService lootManagerService, RoToolboxApplication app) {
        this.lootManagerService = lootManagerService;
        this.app = app;
    }

    public void open(JFrame parent) {
        SwingUtilities.invokeLater(() -> createAndShowGui(parent));
    }

    private void createAndShowGui(JFrame parent) {

        JFrame frame = new JFrame("RO LootModels");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(700, 520);
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
        installWarning.setVisible(lootManagerService.getSelectedGameBase() == null);

        JPanel topSection = new JPanel(new BorderLayout(0, 4));
        topSection.setOpaque(false);
        topSection.add(topBar, BorderLayout.NORTH);
        topSection.add(installWarning, BorderLayout.SOUTH);

        panel.add(topSection, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(8,8));
        center.setBackground(UiTheme.BG);
        JButton pullBtn = new JButton("Download");
        UiTheme.stylePrimaryButton(pullBtn);
        pullBtn.setToolTipText("Download models from the online repository");

        JPanel resourcesPanel = new JPanel();
        resourcesPanel.setLayout(new BoxLayout(resourcesPanel, BoxLayout.Y_AXIS));
        resourcesPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(UiTheme.BORDER), "📥 Downloaded Models"));
        resourcesPanel.setBackground(UiTheme.PANEL);
        resourcesPanel.setForeground(UiTheme.TEXT);
        JButton browseBtn = new JButton("Browse");
        JButton clearResourcesBtn = new JButton("Clear");
        UiTheme.styleSecondaryButton(browseBtn);
        UiTheme.styleSecondaryButton(clearResourcesBtn);

        DefaultListModel<String> profilesModel = new DefaultListModel<>();
        JList<String> profilesList = new JList<>(profilesModel);
        profilesList.setBackground(UiTheme.PANEL_ALT);
        profilesList.setForeground(UiTheme.TEXT);
        profilesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane profilesScroll = new JScrollPane(profilesList);
        profilesScroll.setBackground(UiTheme.PANEL_ALT);
        profilesScroll.setPreferredSize(new Dimension(0, 100));

        Runnable refreshProfilesList = () -> {
            SwingUtilities.invokeLater(() -> {
                profilesModel.clear();
                List<String> profiles = collectDownloadedProfiles();
                for (String profile : profiles) {
                    profilesModel.addElement(profile);
                }
            });
        };

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
        resourcesPanel.add(Box.createVerticalStrut(8));
        resourcesPanel.add(profilesScroll);
        
        refreshProfilesList.run();

        JPanel gamePanel = new JPanel();
        gamePanel.setLayout(new BoxLayout(gamePanel, BoxLayout.Y_AXIS));
        gamePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(UiTheme.BORDER), "📦 Installed Models"));
        gamePanel.setBackground(UiTheme.PANEL);
        gamePanel.setForeground(UiTheme.TEXT);
        JButton browseItemBtn = new JButton("Browse");
        JButton clearItemBtn = new JButton("Clear");
        JButton patchBtn = new JButton("Install");
        UiTheme.styleSecondaryButton(browseItemBtn);
        UiTheme.styleSecondaryButton(clearItemBtn);
        UiTheme.stylePrimaryButton(patchBtn);
        patchBtn.setToolTipText("Install the downloaded models to your game files");

        DefaultListModel<String> installedModel = new DefaultListModel<>();
        JList<String> installedList = new JList<>(installedModel);
        installedList.setBackground(UiTheme.PANEL_ALT);
        installedList.setForeground(UiTheme.TEXT);
        installedList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane installedScroll = new JScrollPane(installedList);
        installedScroll.setBackground(UiTheme.PANEL_ALT);
        installedScroll.setPreferredSize(new Dimension(0, 100));

        Runnable refreshInstalledProfile = () -> {
            SwingUtilities.invokeLater(() -> {
                installedModel.clear();
                String profile = resolveLoadedProfile(lootManagerService.getSelectedGameItemFolder());
                if (profile != null && !profile.isBlank()) {
                    installedModel.addElement(profile);
                }
            });
        };

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
        gamePanel.add(Box.createVerticalStrut(8));
        gamePanel.add(installedScroll);

        JPanel topWrapper = new JPanel(new GridLayout(1, 2, 8, 8));
        topWrapper.add(resourcesPanel);
        topWrapper.add(gamePanel);

        center.add(topWrapper, BorderLayout.NORTH);

        panel.add(center, BorderLayout.CENTER);

        JLabel footer = new JLabel("Created by BStudio • v" + app.getVersion(), SwingConstants.CENTER);
        UiTheme.styleFooter(footer);
        footer.setBorder(BorderFactory.createEmptyBorder(4, 8, 6, 8));
        panel.add(footer, BorderLayout.SOUTH);

        frame.setContentPane(panel);

        // Wire service logger to UI log area
        lootManagerService.setLogger(msg -> SwingUtilities.invokeLater(() -> log.append(msg)));

        // enable/disable UI depending on whether item folder is configured
        Runnable updateEnabled = () -> SwingUtilities.invokeLater(() -> {
            boolean itemConfigured = lootManagerService.getSelectedGameItemFolder() != null;
            Path itemFolder = lootManagerService.getSelectedGameItemFolder();
            boolean itemHasContent = itemConfigured && itemFolder != null && lootManagerService.isDirectoryNonEmpty(itemFolder);
            browseItemBtn.setEnabled(itemConfigured);
            clearItemBtn.setEnabled(itemHasContent);
            patchBtn.setEnabled(itemConfigured);
            installWarning.setVisible(lootManagerService.getSelectedGameBase() == null);
            refreshInstalledProfile.run();
            // resources operations remain available
        });
        // initial state
        updateEnabled.run();
        // listen for changes
        lootManagerService.addChangeListener(updateEnabled);

        if (lootManagerService.getSelectedGameBase() != null) {
            log.append("Loaded saved installation folder: " + lootManagerService.getSelectedGameBase().toAbsolutePath() + "\n");
        }

        // Browse resources
        browseBtn.addActionListener((ActionEvent e) -> {
            try {
                Path toOpen = lootManagerService.getResourcesDir();
                if (!Files.exists(toOpen)) Files.createDirectories(toOpen);
                if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(toOpen.toFile());
                lootManagerService.guiMessage("Opened resources folder: " + toOpen.toAbsolutePath());
            } catch (Exception ex) {
                StringWriter sw = new StringWriter(); ex.printStackTrace(new PrintWriter(sw));
                SwingUtilities.invokeLater(() -> log.append("Error opening resources folder: " + ex.getMessage() + "\n" + sw));
            }
        });

        // Browse item folder
        browseItemBtn.addActionListener((ActionEvent e) -> {
            try {
                Path toOpen = lootManagerService.getSelectedGameItemFolder();
                if (toOpen == null) { lootManagerService.guiMessage("No game destination selected. Set it in Services -> Settings."); return; }
                if (!Files.exists(toOpen)) Files.createDirectories(toOpen);
                if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(toOpen.toFile());
                lootManagerService.guiMessage("Opened item folder: " + toOpen.toAbsolutePath());
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
                    lootManagerService.clearResources();
                    SwingUtilities.invokeLater(() -> {
                        log.append("Resources cleared.\n");
                    updateEnabled.run();
                    refreshProfilesList.run();
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
                    lootManagerService.clearSelectedItemFolder();
                    lootManagerService.setCurrentLootProfile(null);
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
            log.append("⬇ Downloading loot profiles from repository...\n");
            new Thread(() -> {
                try {
                    Path dest = lootManagerService.getResourcesDir();
                    Files.createDirectories(dest);
                    lootManagerService.downloadAndExtract(null, dest);
                    SwingUtilities.invokeLater(() -> {
                        log.append("✓ Download complete! Profiles saved to: " + dest.toAbsolutePath() + "\n");
                        refreshProfilesList.run();
                    });
                } catch (Exception ex) {
                    StringWriter sw = new StringWriter(); ex.printStackTrace(new PrintWriter(sw));
                    SwingUtilities.invokeLater(() -> log.append("✗ Error: " + ex.getMessage() + "\n" + sw));
                } finally {
                    SwingUtilities.invokeLater(() -> pullBtn.setEnabled(true));
                }
            }).start();
        });

        // Patch (choose existing resource subfolder with manifest.json)
        patchBtn.setToolTipText("Applies a loot profile to dropped item models and enlarges them for easier looting.");
        patchBtn.addActionListener((ActionEvent e) -> {
            List<PatchChoice> choices = collectPatchChoices();
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

            JTextArea detailsArea = new JTextArea();
            detailsArea.setEditable(false);
            detailsArea.setBackground(UiTheme.PANEL_ALT);
            detailsArea.setForeground(UiTheme.TEXT);
            detailsArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
            detailsArea.setLineWrap(true);
            detailsArea.setWrapStyleWord(true);
            detailsArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

            JButton visitUrlBtn = new JButton("Visit");
            visitUrlBtn.setPreferredSize(new Dimension(70, 24));
            UiTheme.styleSecondaryButton(visitUrlBtn);
            visitUrlBtn.setVisible(false);

            Runnable updateDetails = () -> {
                String selectedLabel = (String) combo.getSelectedItem();
                if (selectedLabel == null || selectedLabel.trim().isEmpty()) {
                    detailsArea.setText("");
                    visitUrlBtn.setVisible(false);
                    return;
                }
                PatchChoice selected = choices.stream().filter(c -> c.label.equals(selectedLabel)).findFirst().orElse(null);
                if (selected == null) {
                    detailsArea.setText("");
                    visitUrlBtn.setVisible(false);
                    return;
                }
                StringBuilder sb = new StringBuilder();
                if (selected.author != null && !selected.author.isBlank()) {
                    sb.append("Author: ").append(selected.author).append("\n");
                }
                if (selected.description != null && !selected.description.isBlank()) {
                    sb.append("Description: ").append(selected.description).append("\n");
                }
                if (selected.createdAt != null && !selected.createdAt.isBlank()) {
                    sb.append("Created: ").append(selected.createdAt);
                }
                visitUrlBtn.setVisible(selected.url != null && !selected.url.isBlank());
                if (visitUrlBtn.isVisible()) {
                    for (java.awt.event.ActionListener al : visitUrlBtn.getActionListeners()) {
                        visitUrlBtn.removeActionListener(al);
                    }
                    visitUrlBtn.addActionListener(evt -> {
                        try {
                            if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(new java.net.URI(selected.url));
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    });
                }
                detailsArea.setText(sb.toString().trim());
            };

            combo.addActionListener(e2 -> updateDetails.run());
            updateDetails.run();

            JPanel detailsPanel = new JPanel(new BorderLayout(8, 0));
            detailsPanel.setBackground(UiTheme.PANEL_ALT);
            detailsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(UiTheme.BORDER), "Details"));
            JScrollPane detailsScroll = new JScrollPane(detailsArea);
            detailsScroll.setBackground(UiTheme.PANEL_ALT);
            detailsPanel.add(detailsScroll, BorderLayout.CENTER);
            detailsPanel.add(visitUrlBtn, BorderLayout.EAST);
            visitUrlBtn.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 8));

            JPanel dialogPanel = new JPanel(new BorderLayout(8, 8));
            dialogPanel.setBackground(UiTheme.BG);
            JLabel info = new JLabel("Choose a loot profile. This adapts dropped item models and enlarges them for easier looting.");
            info.setForeground(UiTheme.TEXT);
            info.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
            dialogPanel.add(info, BorderLayout.NORTH);
            dialogPanel.add(combo, BorderLayout.CENTER);
            dialogPanel.add(detailsPanel, BorderLayout.SOUTH);

            int choice = JOptionPane.showConfirmDialog(frame, dialogPanel, "Select loot profile", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (choice != JOptionPane.OK_OPTION) return;

            String selectedLabel = (String) combo.getSelectedItem();
            if (selectedLabel == null || selectedLabel.trim().isEmpty()) return;
            PatchChoice selected = choices.stream().filter(c -> c.label.equals(selectedLabel)).findFirst().orElse(null);
            if (selected == null) return;

            patchBtn.setEnabled(false);
            new Thread(() -> {
                try {
                    if (lootManagerService.getSelectedGameItemFolder() == null) { lootManagerService.guiMessage("No game destination selected. Set it in Services -> Settings."); return; }
                    log.append("📦 Installing loot profile: " + selected.source.getFileName() + "...\n");
                    // Clear destination before copy to ensure a clean patch.
                    lootManagerService.clearSelectedItemFolder();
                    lootManagerService.copyDirectoryContents(selected.source, lootManagerService.getSelectedGameItemFolder());
                    lootManagerService.setCurrentLootProfile(selected.label);
                    SwingUtilities.invokeLater(() -> {
                        log.append("✓ Installation complete! Profile: " + selected.source.getFileName() + "\n");
                        updateEnabled.run();
                    });
                } catch (Exception ex) {
                    StringWriter sw = new StringWriter(); ex.printStackTrace(new PrintWriter(sw));
                    SwingUtilities.invokeLater(() -> log.append("✗ Error installing profile: " + ex.getMessage() + "\n" + sw));
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
        private final String url;
        private final String author;
        private final String description;
        private final String createdAt;

        private PatchChoice(String label, Path source, long versionValue, String url, String author, String description, String createdAt) {
            this.label = label;
            this.source = source;
            this.versionValue = versionValue;
            this.url = url;
            this.author = author;
            this.description = description;
            this.createdAt = createdAt;
        }
    }

    private List<PatchChoice> collectPatchChoices() {
        List<PatchChoice> results = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        List<Path> roots = new ArrayList<>();
        roots.add(lootManagerService.getResourcesDir());
        if (lootManagerService.getSelectedGameBase() != null) {
            roots.add(lootManagerService.getSelectedGameBase().resolveSibling(lootManagerService.getResourcesDir().getFileName()));
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
                    String version = readManifestVersion(manifest);
                    String url = readManifestUrl(manifest);
                    String author = readManifestAuthor(manifest);
                    String description = readManifestDescription(manifest);
                    String createdAt = readManifestCreatedAt(manifest);
                    if (seen.add(name)) {
                        results.add(new PatchChoice(name, p, normalizeVersion(version), url, author, description, createdAt));
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

    private String resolveLoadedProfile(Path itemFolder) {
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

    private String readManifestName(Path manifestFile) {
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

    private String readManifestDescription(Path manifestFile) {
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

    private String readManifestVersion(Path manifestFile) {
        try {
            String content = Files.readString(manifestFile);
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\"version\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"").matcher(content);
            if (!matcher.find()) return "0.0.0";
            return matcher.group(1).trim();
        } catch (Exception e) {
            return "0.0.0";
        }
    }

    private String readManifestUrl(Path manifestFile) {
        try {
            String content = Files.readString(manifestFile);
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\"url\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"").matcher(content);
            if (!matcher.find()) return null;
            String value = matcher.group(1).replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
            return value.trim();
        } catch (Exception e) {
            return null;
        }
    }

    private String readManifestAuthor(Path manifestFile) {
        try {
            String content = Files.readString(manifestFile);
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\"author\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"").matcher(content);
            if (!matcher.find()) return null;
            String value = matcher.group(1).replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
            return value.trim();
        } catch (Exception e) {
            return null;
        }
    }

    private String readManifestCreatedAt(Path manifestFile) {
        try {
            String content = Files.readString(manifestFile);
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\"createdAt\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"").matcher(content);
            if (!matcher.find()) return null;
            String value = matcher.group(1).replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
            return value.trim();
        } catch (Exception e) {
            return null;
        }
    }

    private long normalizeVersion(String version) {
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

    private List<String> collectDownloadedProfiles() {
        List<String> profiles = new ArrayList<>();
        Path resourcesDir = lootManagerService.getResourcesDir();
        if (resourcesDir == null || !Files.exists(resourcesDir) || !Files.isDirectory(resourcesDir)) {
            return profiles;
        }
        try (var stream = Files.list(resourcesDir)) {
            stream.filter(Files::isDirectory)
                  .filter(p -> !p.getFileName().toString().startsWith("."))
                  .forEach(p -> {
                      Path manifest = p.resolve("manifest.json");
                      if (Files.exists(manifest) && Files.isRegularFile(manifest)) {
                          profiles.add(p.getFileName().toString());
                      }
                  });
        } catch (IOException ignored) {
        }
        profiles.sort(String::compareTo);
        return profiles;
    }
}

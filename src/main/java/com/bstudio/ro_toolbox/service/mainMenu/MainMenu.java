package com.bstudio.ro_toolbox.service.mainMenu;

import com.bstudio.ro_toolbox.RoToolboxApplication;
import com.bstudio.ro_toolbox.utils.UiTheme;
import com.bstudio.ro_toolbox.service.lootModels.LootManagerService;
import com.bstudio.ro_toolbox.service.lootModels.LootModelsGui;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Service;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.awt.Desktop;

@Service
@RequiredArgsConstructor
public class MainMenu {
    private final LootManagerService lootManagerService;
    private final LootModelsGui mainGui;
    private final RoToolboxApplication app;

    public static void main(String[] args) {
        UiTheme.install();
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(RoToolboxApplication.class);
        MainMenu launcher = context.getBean(MainMenu.class);
        SwingUtilities.invokeLater(launcher::createAndShow);
    }

    public void createAndShow() {
        JFrame frame = new JFrame("RO Toolbox");
        
        // Use array to allow forward reference
        final Runnable[] updateProfileDisplayHolder = new Runnable[1];
        
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                shutdownApp();
            }

            @Override
            public void windowActivated(java.awt.event.WindowEvent e) {
                // Refresh profile display when window becomes active (after returning from LootModelsGui)
                if (updateProfileDisplayHolder[0] != null) {
                    updateProfileDisplayHolder[0].run();
                }
            }
        });
        frame.setSize(700, 250);
        frame.setLayout(new BorderLayout(8, 8));
        frame.getContentPane().setBackground(UiTheme.BG);

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(UiTheme.BG);
        JLabel title = new JLabel("Menu");
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

        JLabel warningLabel = new JLabel("Select a game installation folder in Settings.");
        warningLabel.setForeground(new Color(255, 196, 96));
        warningLabel.setHorizontalAlignment(SwingConstants.CENTER);
        warningLabel.setVisible(lootManagerService.getSelectedGameBase() == null);
        warningLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        warningLabel.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        // Main content wrapper with sections
        JPanel mainContent = new JPanel();
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setBackground(UiTheme.BG);
        mainContent.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Add warning if no game selected
        mainContent.add(warningLabel);
        mainContent.add(Box.createVerticalStrut(8));

        // Models Replacer section - Simple table with rows
        JPanel modelsReplacerSection = new JPanel();
        modelsReplacerSection.setLayout(new BoxLayout(modelsReplacerSection, BoxLayout.Y_AXIS));
        modelsReplacerSection.setBackground(UiTheme.BG);
        modelsReplacerSection.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(UiTheme.BORDER), "Models Replacer"));

        // Table row with 2 columns (Button | Info)
        JPanel tableRow = new JPanel(new GridLayout(1, 2, 6, 0));
        tableRow.setBackground(UiTheme.BG);
        tableRow.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        tableRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        // Column 1: Load Models button
        JButton lootBtn = new JButton("📦 Loot Models");
        UiTheme.stylePrimaryButton(lootBtn);
        lootBtn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        lootBtn.setToolTipText("Adjusts dropped item models.");
        lootBtn.addActionListener((ActionEvent e) -> {
           try {
               frame.setVisible(false);
               mainGui.open(frame);
           } catch (Throwable t) {
               t.printStackTrace();
               JOptionPane.showMessageDialog(frame, "Failed to open Loot Models: " + t.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
               frame.setVisible(true);
           }
        });

        lootBtn.setEnabled(lootManagerService.getSelectedGameBase() != null);
        if (lootManagerService.getSelectedGameBase() == null) {
           lootBtn.setToolTipText("Select a game installation folder in Settings.");
        }

        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBackground(UiTheme.PANEL);
        buttonPanel.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        buttonPanel.add(lootBtn, BorderLayout.CENTER);
        tableRow.add(buttonPanel);

        // Column 2: Info display (clickable)
        JTextArea profileDisplay = new JTextArea();
        profileDisplay.setEditable(false);
        profileDisplay.setBackground(UiTheme.PANEL_ALT);
        profileDisplay.setForeground(UiTheme.TEXT);
        profileDisplay.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 9));
        profileDisplay.setLineWrap(true);
        profileDisplay.setWrapStyleWord(true);
        profileDisplay.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        profileDisplay.setCursor(new Cursor(Cursor.HAND_CURSOR));
        profileDisplay.setToolTipText("Click to visit profile author page");

        JScrollPane profileScroll = new JScrollPane(profileDisplay);
        profileScroll.setBackground(UiTheme.PANEL_ALT);
        profileScroll.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        profileScroll.getVerticalScrollBar().setPreferredSize(new Dimension(6, 0));
        tableRow.add(profileScroll);

        modelsReplacerSection.add(tableRow);

        // Add Models Replacer section to main content
        mainContent.add(modelsReplacerSection);

        JScrollPane centerScroll = new JScrollPane(mainContent);
        centerScroll.setBackground(UiTheme.BG);
        frame.add(centerScroll, BorderLayout.CENTER);

        // Update profile display
        // Store URL for click handling
        String[] currentUrl = {null};
        
        profileDisplay.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (currentUrl[0] != null && !currentUrl[0].isBlank()) {
                    try {
                        if (Desktop.isDesktopSupported()) {
                            Desktop.getDesktop().browse(new java.net.URI(currentUrl[0]));
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
        
        Runnable updateProfileDisplay = () -> SwingUtilities.invokeLater(() -> {
            LootManagerService.ProfileInfo info = lootManagerService.getInstalledProfileInfo();
            currentUrl[0] = null;
            
            if (info != null) {
                StringBuilder sb = new StringBuilder();
                if (info.name != null && !info.name.isBlank()) {
                    sb.append(info.name);
                } else {
                    sb.append("(No name)");
                }
                if (info.author != null && !info.author.isBlank()) {
                    sb.append("\nBy: ").append(info.author);
                }
                if (info.description != null && !info.description.isBlank()) {
                    sb.append("\n").append(info.description);
                }
                
                if (info.url != null && !info.url.isBlank()) {
                    currentUrl[0] = info.url;
                    profileDisplay.setToolTipText("Click to visit profile author page");
                }
                
                profileDisplay.setText(sb.toString());
                profileDisplay.setCaretPosition(0);
            } else {
                profileDisplay.setText("(No models installed)");
                profileDisplay.setToolTipText("Click to visit profile author page");
            }
        });
        
        // Store in holder for window listener
        updateProfileDisplayHolder[0] = updateProfileDisplay;
        
        // Initial update and listen for changes
        updateProfileDisplay.run();
        lootManagerService.addChangeListener(updateProfileDisplay);

        JLabel footer = new JLabel("Created by BStudio • v" + app.getVersion(), SwingConstants.CENTER);
        UiTheme.styleFooter(footer);
        footer.setBorder(BorderFactory.createEmptyBorder(4, 8, 6, 8));
        frame.add(footer, BorderLayout.SOUTH);

        // listen for changes so we can enable the loot button when user saves settings
        lootManagerService.addChangeListener(() -> SwingUtilities.invokeLater(() -> {
            boolean enabled = lootManagerService.getSelectedGameBase() != null;
            lootBtn.setEnabled(enabled);
            warningLabel.setVisible(!enabled);
            lootBtn.setToolTipText(enabled
                    ? "Adjusts dropped item models."
                    : "Select a game installation folder in Settings.");
        }));

        // Settings action: show current folder and allow change with validation
        settings.addActionListener((ActionEvent e) -> {
            try {
                Path current = lootManagerService.getSelectedGameBase();
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
                    lootManagerService.saveSelectedGame(base);
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

package com.autoloupe.pipeline.ui;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

public class UserInterfaceUtils {

    static {
        try {
            // Forces the window to use the host OS's native window decorations and controls
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Fall back safely to standard JVM theme if the host OS theme bridge fails
        }
    }

    /**
     * Spawns an explicit visual directory browser anchored to the user's home path.
     * * @return An Optional containing the selected Path, or Optional.empty() if cancelled.
     */
    public static Optional<Path> selectTargetDirectory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Photo Folder for Autoloupe Triage");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setMultiSelectionEnabled(false);
        chooser.setAcceptAllFileFilterUsed(false);

        // Anchor the browser at the user's home profile folder by default
        chooser.setCurrentDirectory(new File(System.getProperty("user.home")));

        int result = chooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null) {
            return Optional.of(chooser.getSelectedFile().toPath());
        }

        return Optional.empty();
    }

    /**
     * Renders a clean native error dialog box for fatal infrastructure road blocks.
     */
    public static void showErrorDialog(String title, String message) {
        JOptionPane.showMessageDialog(
                null,
                message,
                title,
                JOptionPane.ERROR_MESSAGE
        );
    }
}
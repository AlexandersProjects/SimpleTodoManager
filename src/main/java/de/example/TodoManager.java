package de.example;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import javax.swing.SwingWorker;
import java.util.concurrent.ExecutionException;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JDialog;
import javax.swing.BoxLayout;

public class TodoManager extends JFrame {
    private JTextField filePathField;
    private JTextField backupPathField;
    private JTextArea textArea;

    public TodoManager() {
        setTitle("Simple ToDo Manager");
        createAndShowGUI();
    }

    private void createAndShowGUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);

        String standardFilePath = "C:\\Users\\Blaschko\\OneDrive\\Dokumente\\Alexanders Development\\Code examples\\Daily_simple_todo_script\\mytodos.txt";
        filePathField = new JTextField(standardFilePath);
        filePathField.setText(standardFilePath);

        String standardBackupPath = "C:\\Users\\Blaschko\\OneDrive\\Dokumente\\Alexanders Development\\Code examples\\Daily_simple_todo_script\\mytodos_backups\\";
        backupPathField = new JTextField(standardBackupPath);
        backupPathField.setText(standardBackupPath);

        textArea = new JTextArea();

        // Menu
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("Options");
        JMenuItem settingsItem = new JMenuItem("Settings");
        settingsItem.addActionListener(e -> openSettingsDialog());
        menu.add(settingsItem);
        menuBar.add(menu);
        setJMenuBar(menuBar);

        JButton openButton = new JButton("Open");
        openButton.addActionListener(e -> openFile());

        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> saveFile());

        JPanel panel = new JPanel(new GridLayout(0, 1));
        // panel.add(new JLabel("File Path:"));
        // panel.add(filePathField);
        // panel.add(new JLabel("Backup Directory:"));
        // panel.add(backupPathField);
        panel.add(openButton);
        panel.add(saveButton);

        add(panel, BorderLayout.NORTH);
        add(new JScrollPane(textArea), BorderLayout.CENTER);

        loadFile();
        // pack();
        setLocationRelativeTo(null); // Center on screen
        setVisible(true);
    }

    private void openFile() {
        try {
            Path filePath = Paths.get(filePathField.getText());
            List<String> lines = Files.readAllLines(filePath);
            String content = String.join("\n", lines);
            textArea.setText(content);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error opening file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveFile() {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                Path filePath = Paths.get(filePathField.getText());
                String content = textArea.getText();

                // Backup Handling
                createBackup(filePath);

                // Save File
                Files.write(filePath, Arrays.asList(content.split("\n")));
                return null;
            }

            @Override
            protected void done() {
                try {
                    get(); // call get to rethrow exceptions occurred in doInBackground
                    JOptionPane.showMessageDialog(TodoManager.this, "File saved successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                } catch (InterruptedException | ExecutionException ex) {
                    JOptionPane.showMessageDialog(TodoManager.this, "Error saving file: " + ex.getCause().getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void createBackup(Path filePath) throws IOException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd'T'HH_mm_ss");
        String dateTime = dateFormat.format(new Date());
        String backupFileName = "mytodos_backup_" + dateTime + ".txt";
        Path backupDirPath = Paths.get(backupPathField.getText());
        if (!Files.exists(backupDirPath)) {
            Files.createDirectories(backupDirPath);
        }
        Path backupFilePath = backupDirPath.resolve(backupFileName);

        Files.copy(filePath, backupFilePath, StandardCopyOption.REPLACE_EXISTING);
    }

    private void loadFile() {
        Path path = Paths.get(filePathField.getText());
        if (Files.exists(path)) {
            try {
                String content = Files.readString(path);
                textArea.setText(content);
                textArea.setCaretPosition(0); // Scrolls to the top of the text area
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Failed to load file: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void openSettingsDialog() {
        JDialog settingsDialog = new JDialog(this, "Settings", Dialog.ModalityType.APPLICATION_MODAL);
        settingsDialog.setLayout(new BoxLayout(settingsDialog.getContentPane(), BoxLayout.Y_AXIS));

        settingsDialog.add(new JLabel("File Path:"));
        settingsDialog.add(filePathField);
        settingsDialog.add(new JLabel("Backup Directory:"));
        settingsDialog.add(backupPathField);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> settingsDialog.setVisible(false));
        settingsDialog.add(closeButton);

        settingsDialog.pack();
        settingsDialog.setLocationRelativeTo(this);
        settingsDialog.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(TodoManager::new);
    }
}

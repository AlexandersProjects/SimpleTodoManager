package de.example;

import javax.swing.*;
import javax.swing.SwingWorker;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JDialog;
import javax.swing.BoxLayout;
import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;

import mdlaf.MaterialLookAndFeel;
import mdlaf.themes.MaterialOceanicTheme;

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
        setSize(1000, 800);

        // String standardFilePath = "C:\\Users\\Blaschko\\OneDrive\\Dokumente\\Alexanders Development\\Code examples\\Daily_simple_todo_script\\mytodos.txt";
        // For tests:
        String standardFilePath = "C:\\Users\\Blaschko\\OneDrive\\Dokumente\\Alexanders Development\\Code examples\\Daily_simple_todo_script\\UpdateMyNotes\\example.txt";
        filePathField = new JTextField(standardFilePath);
        filePathField.setText(standardFilePath);

        String standardBackupPath = "C:\\Users\\Blaschko\\OneDrive\\Dokumente\\Alexanders Development\\Code examples\\Daily_simple_todo_script\\mytodos_backups\\";
        backupPathField = new JTextField(standardBackupPath);
        backupPathField.setText(standardBackupPath);

        textArea = new JTextArea();

        // Menu
        // Menu Bar setup
        JMenuBar menuBar = new JMenuBar();

        // Options Menu
        JMenu optionsMenu = new JMenu("Options");

        // Open Menu Item
        JMenuItem openItem = new JMenuItem("Open");
        openItem.addActionListener(e -> openFile());
        optionsMenu.add(openItem);

        // Save Menu Item
        JMenuItem saveItem = new JMenuItem("Save");
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        saveItem.addActionListener(e -> saveFile());
        optionsMenu.add(saveItem);

        // Settings Menu Item
        JMenuItem settingsItem = new JMenuItem("Settings");
        settingsItem.addActionListener(e -> openSettingsDialog());
        optionsMenu.add(settingsItem);

        // Add everything to the menu bar
        menuBar.add(optionsMenu);

        // Directly add the 'Move incomplete tasks' menu item to the menu bar
        JMenuItem moveTasksItem = new JMenuItem("Move Uncompleted Tasks");
        moveTasksItem.addActionListener(e -> moveUncompletedTasks());
        moveTasksItem.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                moveTasksItem.setArmed(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                moveTasksItem.setArmed(false);
            }
        });
        menuBar.add(moveTasksItem);

        setJMenuBar(menuBar);

        JPanel panel = new JPanel(new GridLayout(0, 1));

        add(panel, BorderLayout.NORTH);
        add(new JScrollPane(textArea), BorderLayout.CENTER);

//        try {
//            // Set System L&F
//            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//        } catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException |
//                 IllegalAccessException e) {
//            JOptionPane.showMessageDialog(this, "Error getting the system default theme" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
//        }

        switchToDarkMode();

        loadFile();
        // pack();
        setLocationRelativeTo(null); // Center on screen
        setVisible(true);
    }

    private void moveUncompletedTasks() {
        try {
            // Get the path of the file from the text field.
            Path filePath = Paths.get(filePathField.getText());

            // Read all lines from the file into a list.
            List<String> lines = Files.readAllLines(filePath);

            // Create a formatter for the date in the format "dd.MM.yyyy".
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy");

            // Compute the date of tomorrow.
            LocalDate tomorrow = LocalDate.now().plusDays(1);

            // Format tomorrow's date as a String.
            String tomorrowDate = dtf.format(tomorrow);

            // Prepare a new list to collect completed tasks and content that isn't a task (like headers or notes)
            List<String> remainingDateContent = new ArrayList<>();

            // Define a pattern that matches dates in the format "dd.MM.yyyy".
            Pattern datePattern = Pattern.compile("^\\d{2}\\.\\d{2}\\.\\d{4}$");
            // Initialize indices for slicing the file's content.
            int firstDateIndex = -1;
            int nextDateIndex = lines.size();

            // Search for date lines to find the section where tasks are listed.
            for (int i = 0; i < lines.size(); i++) {
                if (datePattern.matcher(lines.get(i)).matches()) {
                    if (firstDateIndex == -1) {
                        // The first date line has been found.
                        firstDateIndex = i;
                    } else {
                        // The next date line has been found, indicating the end of the task list.
                        nextDateIndex = i;
                        break;
                    }
                }
            }

            // Split the file's content into three parts: before, within, and after the latest date.
            List<String> beforeDate = lines.subList(0, firstDateIndex);
            List<String> dateContent = lines.subList(firstDateIndex, nextDateIndex);
            List<String> afterDate = lines.subList(nextDateIndex, lines.size());

            // Define a pattern that matches incomplete tasks (lines that start with a hyphen and do not have an 'x' following).
            // Matches a task that starts with a hyphen or a number and a dot, followed by a space, and is not followed by 'x' or 'X'.
            Pattern incompleteTaskPattern = Pattern.compile("^[\\t ]*(?:- |\\d+\\. ).*");

            // TODO make the for loop work as intended
            // TODO copy (or add to both) TODO and @ lines instead of moving them

            // Filter out incomplete tasks from the dateContent section.
            //List<String> incompleteTasks = dateContent.stream()
            //        .filter(incompleteTaskPattern.asPredicate())
            //        .collect(Collectors.toList());

            List<String> incompleteTasks = new ArrayList<>();
            boolean checkOfLastTaskCompleted = true; // Assume the last task is completed initially.

//            for (String line : dateContent) {
//                if (line.trim().startsWith("TODO") || line.trim().startsWith("@")){
//                    // Always include TODO headers and lines starting with "@".
//                    remainingDateContent.add(line);
//                } else if (incompleteTaskPattern.matcher(line).matches()) {
//                    // This is an incomplete task and will be moved, so don't add it to remainingDateContent
//                    incompleteTasks.add(line);
//                } else {
//                    // This line is not an incomplete task (so it's either a completed task or some other content), keep it in the current date section
//                    remainingDateContent.add(line);
//                }
//            }

            for (String line : dateContent) {
                // Trim the line to remove leading and trailing whitespaces for accurate checks.
                String trimmedLine = line.trim();

                // Keep headers or special instructions intact.
                if (trimmedLine.startsWith("TODO") || trimmedLine.startsWith("@")) {
                    remainingDateContent.add(line);
                }
                // If it's an incomplete task and not following an uncompleted task, mark it to move.
                else if (incompleteTaskPattern.matcher(trimmedLine).matches() && checkOfLastTaskCompleted) {
                    incompleteTasks.add(line);
                }
                // If it's a completed task or doesn't match any of the above, keep it in place.
                else {
                    remainingDateContent.add(line);
                    // If this line is a completed task, reset the flag to true.
                    if (trimmedLine.startsWith("x-") || trimmedLine.startsWith("X-")) {
                        checkOfLastTaskCompleted = true;
                    }
                    // If this line is an incomplete task, set the flag to false.
                    else if (incompleteTaskPattern.matcher(trimmedLine).matches()) {
                        checkOfLastTaskCompleted = false;
                    }
                }
            }

            // Prepare the updated content by adding beforeDate, tomorrow's date, incomplete tasks, a new line, the original dateContent, and afterDate.
            List<String> updatedContent = new ArrayList<>();
            updatedContent.addAll(beforeDate);
            updatedContent.add(tomorrowDate); // Add tomorrow's date.
            updatedContent.addAll(incompleteTasks); // Add incomplete tasks under tomorrow's date.
            updatedContent.add(""); // Add an empty line for separation.
            // updatedContent.addAll(dateContent); // Add the original tasks for today.
            updatedContent.addAll(remainingDateContent); // Add the original tasks for today.
            updatedContent.addAll(afterDate); // Add the content after the date section.

            // Perform a backup before modifying the file.
            createBackup(filePath);

            // Write the updated content back to the file, replacing the existing content.
            Files.write(filePath, updatedContent, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);

            loadFile();
            // Inform the user of success.
            JOptionPane.showMessageDialog(this, "Uncompleted tasks moved to " + tomorrowDate, "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            // If anything goes wrong, show an error message.
            JOptionPane.showMessageDialog(this, "Error moving uncompleted tasks: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openFile() {
        try {
            Path filePath = Paths.get(filePathField.getText());

            // Read the entire content of the file to a String, replacing system-dependent line separators with \n
            String content = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
            content = content.replace(System.getProperty("line.separator"), "\n");

            textArea.setText(content);
            textArea.setCaretPosition(0); // Scrolls to the top of the text area
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error opening file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addMoveTasksMenuItem(JMenuBar menuBar) {
        JMenu optionsMenu = new JMenu("Options");
        JMenuItem moveTasksItem = new JMenuItem("Move Uncompleted Tasks");
        moveTasksItem.addActionListener(e -> moveUncompletedTasks());
        optionsMenu.add(moveTasksItem);
        menuBar.add(optionsMenu);
    }

    private void saveFile() {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                Path filePath = Paths.get(filePathField.getText());
                String content = textArea.getText();

                // Normalize line endings to match the system's newline character
                String normalizedContent = content.replace("\r\n", "\n").replace("\r", "\n");
                String systemLineSeparator = System.getProperty("line.separator");
                normalizedContent = normalizedContent.replace("\n", systemLineSeparator);

                // Backup Handling
                createBackup(filePath);

                // Save File
                // Files.write(filePath, Arrays.asList(content.split("\n")));
                Files.write(filePath, Arrays.asList(normalizedContent.split(systemLineSeparator)));
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
                String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
                content = content.replace(System.getProperty("line.separator"), "\n");
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

    // Method to switch to dark mode at runtime
//    public static void switchToDarkMode(JFrame frame) {
//        MaterialLookAndFeel.changeTheme(new MaterialOceanicTheme());
//        SwingUtilities.updateComponentTreeUI(frame);
//        frame.pack(); // Optional: Adjusts the frame size after theme switch
//    }
    // Method to switch to dark mode
    private void switchToDarkMode() {
        try {
            // Check if the Material look and feel is already set, otherwise set it
            if (!(UIManager.getLookAndFeel() instanceof MaterialLookAndFeel)) {
                UIManager.setLookAndFeel(new MaterialLookAndFeel());
            }
            // Apply the dark theme
            if (UIManager.getLookAndFeel() instanceof MaterialLookAndFeel) {
                MaterialLookAndFeel.changeTheme(new MaterialOceanicTheme());
            }
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        // Update the component tree - this is safe because it's being called from the EDT
        SwingUtilities.updateComponentTreeUI(this);
    }

    public static void main(String[] args) {
//        // Set the look and feel to Material UI
//        try {
//            // Set Material look and feel
//            UIManager.setLookAndFeel(new MaterialLookAndFeel());
//
//            // If you want to change the default theme, you can do
//            if (UIManager.getLookAndFeel() instanceof MaterialLookAndFeel) {
//                // Set the dark theme
//                MaterialLookAndFeel.changeTheme(new MaterialOceanicTheme());
//            }
//        } catch (UnsupportedLookAndFeelException e) {
//            e.printStackTrace();
//        }

        SwingUtilities.invokeLater(TodoManager::new);

//        // Ensure the frame is constructed and shown on the EDT
//        SwingUtilities.invokeLater(() -> {
//            TodoManager frame = new TodoManager();
//            frame.setVisible(true);
//        });

    }
}

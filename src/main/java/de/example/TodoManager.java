package de.example;

import mdlaf.MaterialLookAndFeel;
import mdlaf.themes.JMarsDarkTheme;
import mdlaf.themes.MaterialLiteTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import java.util.prefs.Preferences;

public class TodoManager extends JFrame {
    private JTextField filePathField;
    private JTextField backupPathField;
    private JTextArea textArea;
    private JCheckBoxMenuItem darkModeToggle;
    private JDialog settingsDialog;
    private Preferences prefs;

    public TodoManager() {
        // Initialize your Preferences object first.
        prefs = Preferences.userNodeForPackage(TodoManager.class);

        // Initialize filePathField and backupPathField before calling loadPreferences.
        filePathField = new JTextField();
        backupPathField = new JTextField();
        darkModeToggle = new JCheckBoxMenuItem();

        // Now that filePathField and backupPathField are initialized, you can load preferences.
        loadPreferences(); // Load preferences at startup

        // If filePath is still null after loading preferences, prompt for the file path.
        if (filePathField.getText().isEmpty()) {
            promptForFilePath();
        }

        // Add WindowListener to save preferences when the window is closing
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                savePreferences(); // Save preferences when the window is closing
            }
        });

        // Initialize darkModeToggle and add ActionListener here, not inside openSettingsDialog
        initializeDarkModeToggle();

        // Continue with the rest of the GUI creation.
        createAndShowGUI();
    }

    private void createAndShowGUI() {
        switchToDarkMode();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 800);

        // String filePath = "C:\\Users\\Blaschko\\OneDrive\\Dokumente\\Alexanders Development\\Code examples\\Daily_simple_todo_script\\mytodos.txt";
        // For tests:
//        String filePath = "C:\\Users\\Blaschko\\OneDrive\\Dokumente\\Alexanders Development\\Code examples\\Daily_simple_todo_script\\UpdateMyNotes\\example.txt";
        String filePath = prefs.get("filePath", ""); // Empty string if not set
        filePathField = new JTextField(filePath);
        filePathField.setText(filePath);

//        String backupPath = "C:\\Users\\Blaschko\\OneDrive\\Dokumente\\Alexanders Development\\Code examples\\Daily_simple_todo_script\\mytodos_backups\\";
        String backupPath = prefs.get("backupPath", ""); // Empty string if not set
        backupPathField = new JTextField(backupPath);
        backupPathField.setText(backupPath);

        textArea = new JTextArea();

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
        JMenuItem moveTasksItem = getMoveTasksItem();
        menuBar.add(moveTasksItem);

        setJMenuBar(menuBar);

        JPanel panel = new JPanel(new GridLayout(0, 1));

        add(panel, BorderLayout.NORTH);
        add(new JScrollPane(textArea), BorderLayout.CENTER);

        loadFile();
        // pack();
        setTitle("Simple ToDo Manager");
        setLocationRelativeTo(null); // Center on screen
        setVisible(true);
    }

    private JMenuItem getMoveTasksItem() {
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
        return moveTasksItem;
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
            List<String> lastDateCompletedTasks = new ArrayList<>();

            String firstDateDateLine = "";

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
            List<String> firstDateContent = lines.subList(firstDateIndex, nextDateIndex);
            List<String> afterDateContent = lines.subList(nextDateIndex, lines.size());

            // Define a pattern that matches incomplete tasks (lines that start with a hyphen and do not have an 'x' following).
            // Matches a task that starts with a hyphen or a number and a dot, followed by a space, and is not followed by 'x' or 'X'.
            Pattern uncompletedTaskPattern = Pattern.compile("^[\\t ]*(?:-|\\d+\\.)[^\\d].*");

            List<String> uncompletedTasks = new ArrayList<>();

            for (int i = 0; i < firstDateContent.size(); i++) {
                String actualLine = firstDateContent.get(i);

                // Move the date directly to the list
                if (i == 0 ) {
//                    uncompletedTasks.add(actualLine);
                    firstDateDateLine = actualLine;
                }
                // Trim the line to remove leading and trailing whitespaces for accurate checks.
                String trimmedActualLine = actualLine.trim();

                // Ebene / SpaceCount der aktuellen Zeile
                int actualLineSpaceCount = actualLine.length() - actualLine.replaceAll("^[\\t ]+", "").length();

                // Keep headers or special instructions intact.
                if (trimmedActualLine.startsWith("TODO") || trimmedActualLine.startsWith("@")) {
                    lastDateCompletedTasks.add(actualLine);
                    uncompletedTasks.add(actualLine);
                } else if (trimmedActualLine.startsWith("x-") || trimmedActualLine.startsWith("X-")) {
                    // This is a completed task; add it to completed tasks
                    lastDateCompletedTasks.add(actualLine);
                    // Look for sub-tasks
                    int j = i + 1;
                    while (j < firstDateContent.size()) {
                        String nextLine = firstDateContent.get(j);
                        int nextLineSpaceCount = nextLine.length() - nextLine.replaceAll("^[\\t ]+", "").length();
                        if (nextLineSpaceCount > actualLineSpaceCount) {
                            String replacedNextLine = nextLine.replaceAll("^(\\s*)(\\d+\\.|-)", "$1X$2");
                            lastDateCompletedTasks.add(replacedNextLine);
                            j++;
                        } else {
                            break;
                        }
                    }
                    i = j - 1; // Skip the processed sub-tasks.
                } else if (uncompletedTaskPattern.matcher(trimmedActualLine).matches()) {
                    uncompletedTasks.add(actualLine);
                }
            }

            // Prepare the updated content by adding beforeDate, tomorrow's date, incomplete tasks, a new line, the original firstDateContent, and afterDateContent.
            List<String> updatedContent = new ArrayList<>();
            updatedContent.addAll(beforeDate);
            updatedContent.add(tomorrowDate); // Add tomorrow's date.
            updatedContent.addAll(uncompletedTasks); // Add incomplete tasks under tomorrow's date.
            updatedContent.add(""); // Add an empty line for separation.
            updatedContent.add(firstDateDateLine); // Add the date of the first found Date before adding the new date
            updatedContent.addAll(lastDateCompletedTasks); // Add the original tasks for today.
            updatedContent.addAll(afterDateContent); // Add the content after the date section.

            System.out.println("beforeDate:");
            beforeDate.forEach(line -> System.out.println(line));
            System.out.println("tomorrowDate:\n" + tomorrowDate);
            System.out.println("uncompletedTasks:");
            uncompletedTasks.forEach(line -> System.out.println(line));
            System.out.println("");
            System.out.println("firstDateDateLine:\n" + firstDateDateLine);
            System.out.println("lastDateCompletedTasks:");
            lastDateCompletedTasks.forEach(line -> System.out.println(line));
            System.out.println("afterDateContent:");
            afterDateContent.forEach(line -> System.out.println(line));

            // Perform a backup before modifying the file.
            createBackup(filePath);

            // Write the updated content back to the file, replacing the existing content.
            Files.write(filePath, updatedContent, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);

            loadFile();
            // Inform the user of success.
            JOptionPane.showMessageDialog(this, "Uncompleted tasks moved to " + tomorrowDate, "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            // If anything goes wrong, show an error message.
            System.out.println("Error moving uncompleted tasks: " + ex.getMessage());
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
        if (filePathField.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "File path is not set.", "Error", JOptionPane.ERROR_MESSAGE);
            return; // Do not attempt to save if the path is empty
        }
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
        String filePath = filePathField.getText();
        if (filePath == null || filePath.trim().isEmpty()) {
            // Display a warning or prompt for file path again
            JOptionPane.showMessageDialog(this, "Please chose a file.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            promptForFilePath(); // Prompt if the path is still not set
        }
        Path path = Paths.get(filePath);
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
        settingsDialog = new JDialog(this, "Settings", Dialog.ModalityType.APPLICATION_MODAL);
        settingsDialog.setLayout(new BoxLayout(settingsDialog.getContentPane(), BoxLayout.Y_AXIS));

        settingsDialog.add(new JLabel("File Path:"));
        settingsDialog.add(filePathField);

        settingsDialog.add(new JLabel("Backup Directory:"));
        settingsDialog.add(backupPathField);

        settingsDialog.add(new JLabel("Toggle Dark Mode:"));
        settingsDialog.add(darkModeToggle);

        JButton closeButton = new JButton("Apply Settings and Close");
        closeButton.addActionListener(e -> {
            savePreferences(); // Save preferences before closing
            settingsDialog.setVisible(false);
        });
        settingsDialog.add(closeButton);

        settingsDialog.pack();
        settingsDialog.setLocationRelativeTo(this);
        settingsDialog.setVisible(true);
    }

    // Method to switch to dark mode
    private void switchToDarkMode() {
        try {
            // Check if the Material look and feel is already set, otherwise set it
            if (!(UIManager.getLookAndFeel() instanceof MaterialLookAndFeel)) {
                UIManager.setLookAndFeel(new MaterialLookAndFeel());
            }
            // Apply the dark theme
            if (UIManager.getLookAndFeel() instanceof MaterialLookAndFeel) {
//                MaterialLookAndFeel.changeTheme(new MaterialOceanicTheme());
                MaterialLookAndFeel.changeTheme(new JMarsDarkTheme());
                updateUIComponents();
                darkModeToggle.setState(true);
            }
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        // Update the component tree - this is safe because it's being called from the EDT
        SwingUtilities.updateComponentTreeUI(this);
    }

    private void switchToLightMode() {
        try {
            // Assuming there is a light theme equivalent to the dark theme you're using
            if (UIManager.getLookAndFeel() instanceof MaterialLookAndFeel) {
                MaterialLookAndFeel.changeTheme(new MaterialLiteTheme());
                updateUIComponents();
                darkModeToggle.setState(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        SwingUtilities.updateComponentTreeUI(this);
    }

    private void initializeDarkModeToggle() {
        darkModeToggle = new JCheckBoxMenuItem("Dark Mode");
        darkModeToggle.addActionListener(e -> {
            boolean isDarkMode = darkModeToggle.isSelected();
            if (isDarkMode) {
                switchToDarkMode();
            } else {
                switchToLightMode();
            }
            savePreferences(); // Save the preference directly here
        });
    }

    private void updateUIComponents() {
        // Update the UI of the main frame
        SwingUtilities.updateComponentTreeUI(this);
        this.repaint();
        this.invalidate();

        // If the settings dialog is open, update its UI as well
        if (settingsDialog != null && settingsDialog.isDisplayable()) {
            SwingUtilities.updateComponentTreeUI(settingsDialog);
            settingsDialog.repaint();
            settingsDialog.invalidate();
        }
    }

    public void savePreferences() {
        // Save file path preferences
        prefs.put("filePath", filePathField.getText());
        prefs.put("backupPath", backupPathField.getText());

        // Save UI preferences
        prefs.putBoolean("darkMode", darkModeToggle.isSelected());

        // Possibly save other settings...
    }

    public void loadPreferences() {
        String filePath = prefs.get("filePath", null);
        String backupPath = prefs.get("backupPath", null);
        boolean darkMode = prefs.getBoolean("darkMode", false);

        if (filePath != null) {
            filePathField.setText(filePath);
        }

        if (backupPath != null) {
            backupPathField.setText(backupPath);
        }

        if (darkMode) {
            switchToDarkMode();
        } else {
            switchToLightMode();
        }
    }

    // Call this method when the user changes settings
    public void onSettingsChanged() {
        savePreferences();
    }

    private void promptForFilePath() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select the file for storing todos");
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            filePathField.setText(selectedFile.getAbsolutePath());
            savePreferences(); // Save the file path to preferences
        }
    }

    public static void main(String[] args) {
        // Set the preferred look and feel here before any components are created
        try {
            // Set your dark mode look and feel here
            UIManager.setLookAndFeel(new MaterialLookAndFeel());
            if (UIManager.getLookAndFeel() instanceof MaterialLookAndFeel) {
                MaterialLookAndFeel.changeTheme(new JMarsDarkTheme());
            }
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        // Now create and show the GUI on the EDT
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new TodoManager();
            }
        });
    }
}
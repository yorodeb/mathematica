import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.Vector;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

/**
 * The main class for the Mathematica application, representing the graphical user interface.
 * It extends JFrame to create the main application window and manages all UI components
 * and user interactions, including search, image upload, displaying extracted text,
 * plotting graphs, and managing history.
 */
public class Window extends JFrame {

    // --- UI Components ---
    private JTextField searchBar; // Input field for user queries or expressions
    private JTable historyTable; // Table to display historical queries and results
    private JTextArea extractedTextArea; // Area to display text extracted from images, editable by user
    private JPanel graphDisplayPanel; // Panel where JFreeChart graphs are displayed

    // --- Core Managers/Services ---
    private DataBase crudManager; // Manages interactions with the database (e.g., saving/loading history)
    private TextExtract textExtractor; // Handles OCR (Optical Character Recognition) for image files
    private JFreeChartGrapher jfreeChartGrapher; // Helper class for creating and managing JFreeChart plots

    // --- UI Color Palette Constants ---
    private static final Color PRIMARY_ACCENT = new Color(70, 130, 180); // Main accent color (e.g., for buttons)
    private static final Color BACKGROUND_DARK = new Color(40, 44, 52); // Darkest background color
    private static final Color BACKGROUND_LIGHT_DARKER = new Color(60, 65, 75); // Slightly lighter dark background
    private static final Color TEXT_LIGHT = new Color(120, 180, 250); // Light text color for general content
    private static final Color TEXT_ACCENT = new Color(170, 180, 200); // Accent text color
    private static final Color BORDER_DARK = new Color(80, 85, 95); // Color for borders

    private static final Color DELETE_BUTTON_COLOR = new Color(120, 180, 250); // Specific color for delete button
    private static final Color DELETE_BUTTON_HOVER = new Color(200, 70, 70); // Hover color for delete button (red)

    /**
     * Constructor for the Window class.
     * Initializes the main application window, sets up the UI components,
     * establishes database connection, and loads initial history data.
     */
    public Window() {
        // --- Frame Setup ---
        setTitle("Mathematica"); // Sets the window title
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Specifies behavior on window close
        setSize(1200, 800); // Sets the initial size of the window
        setMinimumSize(new Dimension(1000, 700)); // Sets the minimum size of the window
        setLocationRelativeTo(null); // Centers the window on the screen
        setLayout(new BorderLayout()); // Uses BorderLayout for overall layout

        // --- Look and Feel Setup ---
        try {
            // Attempts to set the system's native look and feel for better integration.
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Could not set look and feel: " + e.getMessage());
        }

        // --- Initialize Core Managers ---
        // Initializes the database manager. Consider making credentials configurable.
        crudManager = new DataBase("root", "dedakira");
        if (!crudManager.isConnected()) {
            // Shows an error dialog if the database connection fails.
            JOptionPane.showMessageDialog(this,
                    "Database connection failed. Please check your MySQL server and credentials.",
                    "Connection Error",
                    JOptionPane.ERROR_MESSAGE);
        }

        // Initializes the text extractor (OCR). Needs a placeholder path, could be refined.
        textExtractor = new TextExtract("");
        // Initializes the JFreeChart grapher.
        jfreeChartGrapher = new JFreeChartGrapher();

        // Sets the background color of the main content pane.
        getContentPane().setBackground(BACKGROUND_DARK);

        // --- UI Layout Construction ---
        // Adds the top panel (title) to the NORTH region of the BorderLayout.
        add(createTopPanel(), BorderLayout.NORTH);

        // Creates a main split pane to divide the central area into input/display and history.
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                                 createInputAndDisplayArea(), createHistoryPanel());
        mainSplitPane.setDividerLocation(800); // Sets initial divider position
        mainSplitPane.setOneTouchExpandable(true); // Allows single-click expansion/collapse
        mainSplitPane.setBorder(null); // Removes default border
        mainSplitPane.setBackground(BACKGROUND_DARK); // Sets background color

        // Adds the main split pane to the CENTER region of the BorderLayout.
        add(mainSplitPane, BorderLayout.CENTER);

        // --- Initial Data Load ---
        loadHistoryData(); // Loads historical data into the history table on startup.
    }

    /**
     * Creates and configures the top panel of the application, which typically contains the title.
     *
     * @return A JPanel representing the top section of the UI.
     */
    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 10));
        panel.setBackground(BACKGROUND_LIGHT_DARKER);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 30, 15, 30));

        // Custom JLabel to draw text with a subtle shadow effect.
        JLabel titleLabel = new JLabel("Mathematica", SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                String text = getText();
                FontMetrics fm = g2d.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(text)) / 2;
                int y = (getHeight() + fm.getAscent()) / 2 - fm.getDescent();

                // Draw shadow
                g2d.setColor(TEXT_LIGHT.darker().darker());
                g2d.drawString(text, x + 2, y + 2);

                // Draw main text
                g2d.setColor(TEXT_LIGHT);
                g2d.drawString(text, x, y);
                g2d.dispose();
            }
        };
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 40));
        panel.add(titleLabel);

        return panel;
    }

    /**
     * Creates a split pane that divides the central area into an input panel
     * (for search bar and buttons) and a display area (for extracted text and graphs).
     *
     * @return A JComponent (JSplitPane) holding the input and display areas.
     */
    private JComponent createInputAndDisplayArea() {
        JSplitPane centralSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                                     createInputPanel(), createDisplayPanels());
        centralSplitPane.setDividerLocation(350);
        centralSplitPane.setOneTouchExpandable(true);
        centralSplitPane.setBorder(null);
        centralSplitPane.setBackground(BACKGROUND_DARK);

        return centralSplitPane;
    }

    /**
     * Creates the input panel containing the search bar and action buttons (Search, Upload Image).
     *
     * @return A JPanel configured for user input and actions.
     */
    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new GridBagLayout()); // Uses GridBagLayout for flexible component placement
        panel.setBackground(BACKGROUND_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 10, 15, 10); // Padding around components
        gbc.fill = GridBagConstraints.HORIZONTAL; // Components expand horizontally
        gbc.gridwidth = 2; // Spans two columns

        // --- Search Bar Setup ---
        searchBar = new JTextField(30);
        searchBar.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        searchBar.setForeground(TEXT_LIGHT);
        searchBar.setBackground(BACKGROUND_LIGHT_DARKER);
        searchBar.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_DARK, 1), // Outer border
            BorderFactory.createEmptyBorder(12, 15, 12, 15) // Inner padding
        ));
        searchBar.setCaretColor(PRIMARY_ACCENT); // Color of the text cursor
        searchBar.setToolTipText("Type your question or expression here");
        searchBar.addActionListener(e -> performSearch()); // Triggers search on Enter key press

        gbc.gridx = 0; // Column 0
        gbc.gridy = 0; // Row 0
        gbc.weightx = 1.0; // Occupy available horizontal space
        panel.add(searchBar, gbc);

        // --- Button Row Panel ---
        JPanel buttonRowPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        buttonRowPanel.setOpaque(false); // Makes the panel transparent

        // --- Search Button ---
        JButton searchBtn = new JButton("Search");
        searchBtn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        searchBtn.setForeground(TEXT_LIGHT);
        searchBtn.setBackground(PRIMARY_ACCENT);
        searchBtn.setFocusPainted(false); // Removes focus border
        searchBtn.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25)); // Padding
        searchBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); // Hand cursor on hover
        searchBtn.setToolTipText("Initiate Search");
        searchBtn.addActionListener(e -> performSearch()); // Action when clicked
        // Mouse listeners for hover effect
        searchBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { searchBtn.setBackground(PRIMARY_ACCENT.darker()); }
            public void mouseExited(MouseEvent e) { searchBtn.setBackground(PRIMARY_ACCENT); }
        });
        buttonRowPanel.add(searchBtn);

        // --- Upload Image Button ---
        JButton uploadBtn = new JButton("Upload Image");
        uploadBtn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        uploadBtn.setForeground(TEXT_LIGHT);
        uploadBtn.setBackground(BACKGROUND_LIGHT_DARKER.darker());
        uploadBtn.setFocusPainted(false);
        uploadBtn.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));
        uploadBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        uploadBtn.setToolTipText("Upload images from your system");
        uploadBtn.addActionListener(e -> openFileChooser()); // Action when clicked
        // Mouse listeners for hover effect
        uploadBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { uploadBtn.setBackground(BACKGROUND_LIGHT_DARKER.darker().darker()); }
            public void mouseExited(MouseEvent e) { uploadBtn.setBackground(BACKGROUND_LIGHT_DARKER.darker()); }
        });
        buttonRowPanel.add(uploadBtn);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weighty = 0; // Don't grow vertically
        gbc.fill = GridBagConstraints.NONE; // Don't expand
        panel.add(buttonRowPanel, gbc);

        // --- Empty Filler for Vertical Spacing ---
        // This panel consumes remaining vertical space to push components to the top.
        JPanel emptyFiller = new JPanel();
        emptyFiller.setOpaque(false);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weighty = 1.0; // Occupy all remaining vertical space
        gbc.fill = GridBagConstraints.BOTH; // Expand in both directions
        panel.add(emptyFiller, gbc);

        return panel;
    }

    /**
     * Creates a vertical split pane for the display area, dividing it into
     * an extracted text panel and a graph display panel.
     *
     * @return A JComponent (JSplitPane) holding the extracted text and graph display areas.
     */
    private JComponent createDisplayPanels() {
        JSplitPane displaySplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                                                     createExtractedTextPanel(), createGraphDisplayPanel());
        displaySplitPane.setDividerLocation(0.5); // Sets divider to middle (50%)
        displaySplitPane.setOneTouchExpandable(true);
        displaySplitPane.setBorder(null);
        displaySplitPane.setBackground(BACKGROUND_DARK);

        return displaySplitPane;
    }

    /**
     * Creates the panel where text extracted from images is displayed and can be edited.
     * It includes a text area and a "Plot" button.
     *
     * @return A JPanel containing the extracted text area.
     */
    private JPanel createExtractedTextPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BACKGROUND_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel title = new JLabel("Extracted Text", SwingConstants.LEFT);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(TEXT_LIGHT);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        panel.add(title, BorderLayout.NORTH);

        // --- Extracted Text Area Setup ---
        extractedTextArea = new JTextArea();
        extractedTextArea.setEditable(true); // Allows user to edit extracted text
        extractedTextArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        extractedTextArea.setForeground(TEXT_LIGHT);
        extractedTextArea.setBackground(BACKGROUND_LIGHT_DARKER);
        extractedTextArea.setCaretColor(PRIMARY_ACCENT);
        extractedTextArea.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_DARK, 1),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        extractedTextArea.setLineWrap(true); // Wraps lines
        extractedTextArea.setWrapStyleWord(true); // Wraps at word boundaries
        extractedTextArea.setText("Extracted text from images will appear here. You can edit it before plotting.");

        JScrollPane scrollPane = new JScrollPane(extractedTextArea); // Adds scrollability to text area
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(BACKGROUND_LIGHT_DARKER); // Sets scroll pane background
        panel.add(scrollPane, BorderLayout.CENTER);

        // --- Plot Extracted Text Button ---
        JButton plotExtractedTextBtn = new JButton("Plot from Extracted Text");
        plotExtractedTextBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        plotExtractedTextBtn.setForeground(TEXT_LIGHT);
        plotExtractedTextBtn.setBackground(PRIMARY_ACCENT.darker());
        plotExtractedTextBtn.setFocusPainted(false);
        plotExtractedTextBtn.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        plotExtractedTextBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        plotExtractedTextBtn.addActionListener(e -> plotExtractedText()); // Action when clicked
        // Mouse listeners for hover effect
        plotExtractedTextBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { plotExtractedTextBtn.setBackground(PRIMARY_ACCENT.darker().darker()); }
            public void mouseExited(MouseEvent e) { plotExtractedTextBtn.setBackground(PRIMARY_ACCENT.darker()); }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT)); // Aligns button to the right
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        buttonPanel.add(plotExtractedTextBtn);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Creates the panel where mathematical graphs generated by JFreeChart are displayed.
     * Initially shows a placeholder message.
     *
     * @return A JPanel configured as the graph display area.
     */
    private JPanel createGraphDisplayPanel() {
        graphDisplayPanel = new JPanel(new BorderLayout());
        graphDisplayPanel.setBackground(BACKGROUND_DARK);
        graphDisplayPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel title = new JLabel("Graph Display", SwingConstants.LEFT);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(TEXT_LIGHT);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        graphDisplayPanel.add(title, BorderLayout.NORTH);

        // --- Placeholder Content for Graph Area ---
        JPanel placeholderContent = new JPanel(new GridBagLayout());
        placeholderContent.setBackground(BACKGROUND_LIGHT_DARKER);
        placeholderContent.setBorder(new LineBorder(BORDER_DARK, 1));
        JLabel placeholderLabel = new JLabel("Graph Would Be Shown Here", SwingConstants.CENTER);
        placeholderLabel.setFont(new Font("Segoe UI", Font.ITALIC, 20));
        placeholderLabel.setForeground(TEXT_ACCENT);
        placeholderContent.add(placeholderLabel, new GridBagConstraints()); // Centers the label

        graphDisplayPanel.add(placeholderContent, BorderLayout.CENTER);

        return graphDisplayPanel;
    }

    /**
     * Creates the history panel, which displays a table of past queries and actions,
     * and includes a button to delete old history entries.
     *
     * @return A JPanel configured for displaying and managing history.
     */
    private JPanel createHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BACKGROUND_LIGHT_DARKER);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 15, 15, 15));

        JLabel historyTitle = new JLabel("History", SwingConstants.CENTER);
        historyTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        historyTitle.setForeground(TEXT_LIGHT);
        historyTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        panel.add(historyTitle, BorderLayout.NORTH);

        // --- History Table Setup ---
        historyTable = new JTable();
        historyTable.setFillsViewportHeight(true); // Table fills the height of its scroll pane
        historyTable.setRowHeight(35); // Sets row height for better readability
        historyTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        historyTable.setForeground(TEXT_LIGHT);
        historyTable.setBackground(BACKGROUND_DARK);
        historyTable.setSelectionBackground(PRIMARY_ACCENT.darker()); // Color when a row is selected
        historyTable.setSelectionForeground(Color.WHITE); // Text color of selected row
        historyTable.setGridColor(BORDER_DARK); // Color of grid lines
        historyTable.setShowVerticalLines(false); // Hides vertical grid lines
        historyTable.setShowHorizontalLines(true); // Shows horizontal grid lines

        // --- Table Header Customization ---
        historyTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        historyTable.getTableHeader().setBackground(BACKGROUND_LIGHT_DARKER.darker());
        historyTable.getTableHeader().setForeground(TEXT_LIGHT);
        historyTable.getTableHeader().setReorderingAllowed(false); // Prevents user from reordering columns
        historyTable.getTableHeader().setPreferredSize(new Dimension(1, 40)); // Sets header height

        // --- Cell Renderer for Centering Content ---
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        historyTable.setDefaultRenderer(Object.class, centerRenderer); // Applies renderer to all cell types

        JScrollPane scrollPane = new JScrollPane(historyTable); // Makes the table scrollable
        scrollPane.setBorder(new LineBorder(BORDER_DARK, 1));
        scrollPane.getViewport().setBackground(BACKGROUND_DARK);
        panel.add(scrollPane, BorderLayout.CENTER);

        // --- Delete History Button ---
        JButton deleteHistoryBtn = new JButton("Delete Old History (15 Days)");
        deleteHistoryBtn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        deleteHistoryBtn.setForeground(TEXT_LIGHT);
        deleteHistoryBtn.setBackground(DELETE_BUTTON_COLOR);
        deleteHistoryBtn.setFocusPainted(false);
        deleteHistoryBtn.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        deleteHistoryBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        deleteHistoryBtn.setToolTipText("Permanently delete history entries older than 15 days");
        deleteHistoryBtn.addActionListener(e -> {
            // Confirmation dialog before deleting
            if (crudManager != null && crudManager.isConnected()) {
                int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to delete all history older than 15 days?\nThis action cannot be undone.",
                    "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (confirm == JOptionPane.YES_OPTION) {
                    int deletedCount = crudManager.Delete(15); // Calls DataBase method to delete
                    JOptionPane.showMessageDialog(this,
                        deletedCount + " old history entries deleted.",
                        "Deletion Complete", JOptionPane.INFORMATION_MESSAGE);
                    loadHistoryData(); // Reloads history to update the table
                }
            } else {
                // Warns user if database is not connected
                JOptionPane.showMessageDialog(this,
                    "Database not connected. Cannot perform deletion.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        // Mouse listeners for hover effect
        deleteHistoryBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { deleteHistoryBtn.setBackground(DELETE_BUTTON_HOVER); }
            public void mouseExited(MouseEvent e) { deleteHistoryBtn.setBackground(DELETE_BUTTON_COLOR); }
        });

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 15)); // Centers the button
        bottomPanel.setOpaque(false);
        bottomPanel.add(deleteHistoryBtn);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Attempts to plot a given mathematical equation using JFreeChartGrapher.
     * Displays the generated graph or an error message if plotting fails.
     *
     * @param equationToParse The mathematical equation string to be parsed and plotted.
     * @param originalQuery   The original query string for display purposes (e.g., in dialogs).
     * @return true if the graph was successfully plotted, false otherwise.
     */
    private boolean attemptPlotEquation(String equationToParse, String originalQuery) {
        String plotTitle = "Plot of " + originalQuery;
        String processedEquation = equationToParse;
        boolean graphWasPlotted = false;

        // Removes "y =" prefix if present for easier parsing by exp4j.
        if (processedEquation.toLowerCase().startsWith("y =")) {
            processedEquation = processedEquation.substring(processedEquation.toLowerCase().indexOf("y =") + "y =".length()).trim();
        }

        try {
            JPanel chartPanel = jfreeChartGrapher.createChartPanelForEquation(processedEquation, plotTitle);

            if (chartPanel != null) {
                displayCustomPanel(chartPanel); // Displays the generated chart
                JOptionPane.showMessageDialog(this, "Graph for '" + originalQuery + "' displayed.", "Plot Success", JOptionPane.INFORMATION_MESSAGE);
                graphWasPlotted = true;
            } else {
                // Informs user if plotting failed (e.g., invalid equation)
                JOptionPane.showMessageDialog(this,
                    "Could not plot equation: '" + originalQuery + "'.\n" +
                    "Please ensure it's a valid 'y = f(x)' format (e.g., 'x^2', '8*x - 9', '(8 - 2*x) / 3').",
                    "Plotting Error", JOptionPane.WARNING_MESSAGE);
                clearGraphDisplay(); // Clears the graph area
                graphWasPlotted = false;
            }
        } catch (Exception e) {
            // Catches unexpected errors during plotting
            JOptionPane.showMessageDialog(this,
                "An unexpected error occurred while plotting '" + originalQuery + "': " + e.getMessage(),
                "Plotting Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            clearGraphDisplay();
            graphWasPlotted = false;
        }
        return graphWasPlotted;
    }

    /**
     * Replaces the content of the graph display panel with a custom JPanel (e.g., a chart panel).
     *
     * @param panel The JPanel to display in the graph area. If null, an error message is shown.
     */
    private void displayCustomPanel(JPanel panel) {
        graphDisplayPanel.removeAll(); // Clears existing content
        graphDisplayPanel.setLayout(new BorderLayout()); // Ensures correct layout for the new panel

        if (panel != null) {
            graphDisplayPanel.add(panel, BorderLayout.CENTER);
        } else {
            // Displays an error message if the panel to display is null
            JLabel errorLabel = new JLabel("Graph panel could not be generated.", SwingConstants.CENTER);
            errorLabel.setForeground(TEXT_ACCENT);
            errorLabel.setFont(new Font("Segoe UI", Font.ITALIC, 16));
            graphDisplayPanel.add(errorLabel, BorderLayout.CENTER);
        }
        graphDisplayPanel.revalidate(); // Re-calculates layout
        graphDisplayPanel.repaint(); // Repaints component
    }

    /**
     * Clears the graph display panel and resets it to its initial placeholder state.
     */
    private void clearGraphDisplay() {
        graphDisplayPanel.removeAll(); // Clears existing content
        graphDisplayPanel.setLayout(new BorderLayout());

        // Re-adds the placeholder content
        JPanel placeholderContent = new JPanel(new GridBagLayout());
        placeholderContent.setBackground(BACKGROUND_LIGHT_DARKER);
        placeholderContent.setBorder(new LineBorder(BORDER_DARK, 1));

        JLabel placeholderLabel = new JLabel("Graph Would Be Shown Here", SwingConstants.CENTER);
        placeholderLabel.setFont(new Font("Segoe UI", Font.ITALIC, 20));
        placeholderLabel.setForeground(TEXT_ACCENT);
        placeholderContent.add(placeholderLabel, new GridBagConstraints());

        graphDisplayPanel.add(placeholderContent, BorderLayout.CENTER);

        graphDisplayPanel.revalidate();
        graphDisplayPanel.repaint();
    }

    /**
     * Initiates the plotting process using the text currently in the `extractedTextArea`.
     * Validates the text before attempting to plot.
     */
    private void plotExtractedText() {
        String textToPlot = extractedTextArea.getText().trim();
        // Checks if the text area contains valid, non-placeholder content.
        if (!textToPlot.isEmpty() &&
            !textToPlot.equalsIgnoreCase("Extracted text from images will appear here. You can edit it before plotting.") &&
            !textToPlot.startsWith("Error extracting text:") &&
            !textToPlot.equalsIgnoreCase("File::Error")) {

            attemptPlotEquation(textToPlot, "Edited/Extracted: " + textToPlot); // Attempts to plot the text
        } else {
            // Warns user if the text area is empty or contains an error/placeholder.
            JOptionPane.showMessageDialog(this,
                "The extracted text area is empty or contains an error message. Please enter a valid equation to plot.",
                "Plotting Error", JOptionPane.WARNING_MESSAGE);
            clearGraphDisplay();
        }
    }

    /**
     * Executes a search query entered in the `searchBar`.
     * Attempts to plot the query, saves it to history, and refreshes the history display.
     */
    private void performSearch() {
        String query = searchBar.getText().trim();
        if (!query.isEmpty()) {
            if (crudManager != null && crudManager.isConnected()) {
                attemptPlotEquation(query, query); // Attempts to plot the query directly
                crudManager.CreateData("N/A (text query)", query); // Saves the text query to database
                JOptionPane.showMessageDialog(this, "Query submitted: \"" + query + "\"\n(Saved to database history)", "Search Action", JOptionPane.INFORMATION_MESSAGE);
                loadHistoryData(); // Reloads history table to show new entry
                // Resets extracted text area to its default message.
                extractedTextArea.setText("Extracted text from images will appear here. You can edit it before plotting.");
            } else {
                // Warns user if database is not connected
                JOptionPane.showMessageDialog(this, "Database not connected. Cannot save search query.", "Error", JOptionPane.ERROR_MESSAGE);
                clearGraphDisplay();
            }
            searchBar.setText(""); // Clears the search bar
        } else {
            // Warns user if search bar is empty
            JOptionPane.showMessageDialog(this, "Please enter a query to search or plot.", "Empty Input", JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Opens a file chooser dialog to allow the user to select an image file.
     * Upon selection, performs OCR on the image, displays extracted text,
     * attempts to plot it, and saves the entry to history.
     */
    private void openFileChooser() {
        JFileChooser fc = new JFileChooser();
        fc.setMultiSelectionEnabled(false); // Allows only single file selection
        // Sets a file filter to show only common image file types.
        fc.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                if (f.isDirectory()) return true;
                String name = f.getName().toLowerCase();
                return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") ||
                       name.endsWith(".gif") || name.endsWith(".bmp") || name.endsWith(".webp");
            }

            @Override
            public String getDescription() {
                return "Image files (*.jpg, *.jpeg, *.png, *.gif, *.bmp, *.webp)";
            }
        });
        int val = fc.showOpenDialog(this); // Shows the dialog
        if (val == JFileChooser.APPROVE_OPTION) { // If user selects a file
            File selectedFile = fc.getSelectedFile();
            if (selectedFile != null) {
                String imagePath = selectedFile.getAbsolutePath();

                // Performs OCR using the TextExtract instance
                String extractedText = textExtractor.perform(imagePath);
                extractedTextArea.setText(extractedText); // Displays extracted text

                // Attempts to plot the extracted text if it's valid
                if (!extractedText.trim().isEmpty() && !extractedText.trim().equalsIgnoreCase("File::Error")) {
                    attemptPlotEquation(extractedText.trim(), extractedText.trim());
                } else {
                    JOptionPane.showMessageDialog(this, "No valid text extracted from image to plot.", "Information", JOptionPane.INFORMATION_MESSAGE);
                    clearGraphDisplay();
                }

                // Saves image path and extracted text to database history
                if (crudManager != null && crudManager.isConnected()) {
                    crudManager.CreateData(imagePath, extractedText);
                    JOptionPane.showMessageDialog(this, "Image selected and text extracted.\nSaved to history.", "Image Upload", JOptionPane.INFORMATION_MESSAGE);
                    loadHistoryData(); // Reloads history to show new entry
                } else {
                    JOptionPane.showMessageDialog(this, "Database not connected. Image uploaded and text extracted, but not saved to history.", "Warning", JOptionPane.WARNING_MESSAGE);
                }
            }
        }
    }

    /**
     * Loads historical data from the database into the `historyTable`.
     * Refreshes the table model and applies visual formatting.
     */
    private void loadHistoryData() {
        if (crudManager != null && crudManager.isConnected()) {
            DefaultTableModel model = crudManager.getHistoryTableModel(); // Retrieves data from DataBase class
            historyTable.setModel(model); // Sets the table model

            // Adjusts column widths for better display (assuming at least 3 columns: ID, Source, Question/Result)
            if (model.getColumnCount() >= 3) {
                historyTable.getColumnModel().getColumn(0).setPreferredWidth(80); // ID
                historyTable.getColumnModel().getColumn(1).setPreferredWidth(180); // Source (e.g., filename)
                historyTable.getColumnModel().getColumn(2).setPreferredWidth(400); // Question/Result snippet
                historyTable.getColumnModel().getColumn(3).setPreferredWidth(150); // Timestamp
            }

            // Centers table header text
            DefaultTableCellRenderer headerRenderer = (DefaultTableCellRenderer) historyTable.getTableHeader().getDefaultRenderer();
            headerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

            // Centers table cell content
            DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer();
            cellRenderer.setHorizontalAlignment(SwingConstants.CENTER);
            historyTable.setDefaultRenderer(Object.class, cellRenderer);

            System.out.println("Database history data loaded into UI.");
        } else {
            System.err.println("Database not connected or manager not initialized. Cannot load history data.");
            // Sets an empty table model if no database connection
            historyTable.setModel(new DefaultTableModel(new Vector<>(), new Vector<>()));
        }
    }

    /**
     * Static nested class responsible for creating JFreeChart panels for mathematical equations.
     * This class uses the exp4j library to parse and evaluate expressions.
     */
    static class JFreeChartGrapher {

        /**
         * Creates a ChartPanel containing a line chart for the given mathematical equation.
         * The equation should be in terms of 'x' (e.g., "x^2 + 2*x - 1").
         *
         * @param equation The mathematical equation string to plot.
         * @param title    The title for the chart.
         * @return A JPanel containing the chart, or null if an error occurs during plotting.
         */
        public JPanel createChartPanelForEquation(String equation, String title) {
            XYSeries series = new XYSeries("y = " + equation);
            try {
                // Builds an expression from the input string, recognizing 'x' as a variable.
                Expression expression = new ExpressionBuilder(equation)
                        .variables("x")
                        .build();

                // Iterates through x-values to calculate corresponding y-values.
                // Plots from x = -10 to 10 with a step of 0.1.
                for (double x = -10; x <= 10; x += 0.1) {
                    try {
                        expression.setVariable("x", x);
                        double y = expression.evaluate();
                        // Only add finite values to avoid issues with division by zero, log of non-positive, etc.
                        if (Double.isFinite(y)) {
                            series.add(x, y);
                        }
                    } catch (IllegalArgumentException e) {
                        // Catches exp4j specific errors for invalid operations at a point (e.g., log(0)).
                        // These points are skipped due to the isFinite() check.
                        // Can be logged for debugging if needed, but often okay to ignore for plotting.
                        System.err.println("Warning: Skipping point for x=" + x + " due to calculation error: " + e.getMessage());
                    }
                }

                XYSeriesCollection dataset = new XYSeriesCollection();
                dataset.addSeries(series);

                // Creates the XY line chart using JFreeChart.
                JFreeChart chart = ChartFactory.createXYLineChart(
                    title,               // Chart title
                    "X",                 // X-axis label
                    "Y",                 // Y-axis label
                    dataset,             // Data
                    PlotOrientation.VERTICAL, // Plot orientation
                    true,                // Show legend
                    true,                // Tooltips
                    false                // URLs
                );

                // --- Customizing Chart Appearance ---
                chart.setBackgroundPaint(new Color(60, 65, 75)); // Chart background
                chart.getTitle().setPaint(new Color(220, 220, 220)); // Title color
                chart.getXYPlot().setBackgroundPaint(new Color(40, 44, 52)); // Plot area background
                chart.getXYPlot().setDomainGridlinePaint(new Color(80, 85, 95)); // X-axis grid lines
                chart.getXYPlot().setRangeGridlinePaint(new Color(80, 85, 95)); // Y-axis grid lines

                chart.getXYPlot().getDomainAxis().setLabelPaint(new Color(170, 180, 200)); // X-axis label color
                chart.getXYPlot().getDomainAxis().setTickLabelPaint(new Color(170, 180, 200)); // X-axis tick label color
                chart.getXYPlot().getRangeAxis().setLabelPaint(new Color(170, 180, 200)); // Y-axis label color
                chart.getXYPlot().getRangeAxis().setTickLabelPaint(new Color(170, 180, 200)); // Y-axis tick label color

                chart.getLegend().setItemPaint(new Color(220, 220, 220)); // Legend item color

                return new ChartPanel(chart); // Returns a JPanel that displays the chart

            } catch (Exception e) {
                // Catches general errors during chart creation (e.g., malformed expression).
                System.err.println("Error creating chart for equation '" + equation + "': " + e.getMessage());
                e.printStackTrace();
                return null; // Returns null if chart cannot be created
            }
        }
    }

    /**
     * Static nested class for drawing a custom search icon.
     * Implements the Icon interface for use in Swing components.
     */
    static class SearchIcon implements Icon {
        private final int size;
        private final Color color;

        /**
         * Constructs a SearchIcon with a specified size and color.
         * @param size The desired size (width and height) of the icon in pixels.
         * @param color The color of the icon.
         */
        public SearchIcon(int size, Color color) {
            this.size = size;
            this.color = color;
        }

        @Override
        public int getIconWidth() { return size; } // Returns the width of the icon
        @Override
        public int getIconHeight() { return size; } // Returns the height of the icon

        /**
         * Paints the search icon (magnifying glass) onto the given Graphics context.
         * @param c The component this icon is being painted on.
         * @param g The graphics context.
         * @param x The X coordinate of the icon's top-left corner.
         * @param y The Y coordinate of the icon's top-left corner.
         */
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); // Smooth edges
            g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); // Sets line thickness and style
            g2.setColor(color);
            // Draw the circle part of the magnifying glass
            g2.drawOval(x, y, (int)(size * 0.7), (int)(size * 0.7));
            // Calculate coordinates for the handle
            int handleStartX = x + (int)(size * 0.5);
            int handleStartY = y + (int)(size * 0.5);
            int handleEndX = x + size;
            int handleEndY = y + size;
            // Draw the handle part
            g2.drawLine(handleStartX, handleStartY, handleEndX, handleEndY);
            g2.dispose(); // Release graphics resources
        }
    }

    /**
     * Static nested class for drawing a custom folder upload icon.
     * Implements the Icon interface for use in Swing components.
     */
    static class FolderUploadIcon implements Icon {
        private final int size;
        private final Color color;

        /**
         * Constructs a FolderUploadIcon with a specified size and color.
         * @param size The desired size (width and height) of the icon in pixels.
         * @param color The color of the icon.
         */
        public FolderUploadIcon(int size, Color color) {
            this.size = size;
            this.color = color;
        }

        @Override
        public int getIconWidth() { return size; } // Returns the width of the icon
        @Override
        public int int getIconHeight() { return size; } // Returns the height of the icon

        /**
         * Paints the folder upload icon onto the given Graphics context.
         * @param c The component this icon is being painted on.
         * @param g The graphics context.
         * @param x The X coordinate of the icon's top-left corner.
         * @param y The Y coordinate of the icon's top-left corner.
         */
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); // Smooth edges
            g2.setColor(color);
            int folderWidth = (int)(size * 0.9);
            int folderHeight = (int)(size * 0.6);
            int tabWidth = (int)(folderWidth * 0.3);

            // Draw the main folder body (rounded rectangle)
            g2.fillRoundRect(x + (size - folderWidth) / 2, y + (int)(size * 0.4), folderWidth, folderHeight, 5, 5);

            // Draw the folder tab (polygon)
            Polygon tab = new Polygon();
            tab.addPoint(x + (size - folderWidth) / 2 + (int)(folderWidth * 0.1), y + (int)(size * 0.4));
            tab.addPoint(x + (size - folderWidth) / 2 + (int)(folderWidth * 0.1), y + (int)(size * 0.4) - (int)(folderHeight * 0.2));
            tab.addPoint(x + (size - folderWidth) / 2 + (int)(folderWidth * 0.1) + tabWidth, y + (int)(size * 0.4) - (int)(folderHeight * 0.2));
            tab.addPoint(x + (size - folderWidth) / 2 + (int)(folderWidth * 0.1) + tabWidth, y + (int)(size * 0.4));
            g2.fillPolygon(tab);
            g2.dispose(); // Release graphics resources
        }
    }

    /**
     * Main method to start the Mathematica application.
     * Sets the system look and feel and creates and displays the main window on the Event Dispatch Thread.
     *
     * @param args Command line arguments (not used).
     */
    public static void main(String[] args) {
        // Attempts to set the system's native look and feel.
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace(); // Prints stack trace if setting L&F fails
        }

        // Ensures that Swing UI updates are performed on the Event Dispatch Thread (EDT).
        SwingUtilities.invokeLater(() -> {
            Window window = new Window(); // Creates an instance of the main window
            window.setVisible(true); // Makes the window visible
        });
    }
}

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.Vector;
import java.sql.Timestamp; // Ensure this is imported for CRUD operations

public class MathematicaWindow extends JFrame {

    private JTextField searchBar;
    private JTable historyTable;
    private JTextArea extractedTextArea;
    private JPanel graphDisplayPanel; // Panel for graph display
    private CRUD crudManager;

    // --- NEW Dark Theme Color Palette ---
    private static final Color PRIMARY_ACCENT = new Color(70, 130, 180); // Deep blue
    private static final Color BACKGROUND_DARK = new Color(40, 44, 52); // Main dark background
    private static final Color BACKGROUND_LIGHT_DARKER = new Color(60, 65, 75); // Slightly lighter dark for contrast
    private static final Color TEXT_LIGHT = new Color(220, 220, 220); // Light text for readability
    private static final Color TEXT_ACCENT = new Color(170, 180, 200); // Muted light text for secondary info
    private static final Color BORDER_DARK = new Color(80, 85, 95); // Subtle dark border

    private static final Color DELETE_BUTTON_COLOR = new Color(170, 50, 50); // Red for danger action
    private static final Color DELETE_BUTTON_HOVER = new Color(200, 70, 70); // Brighter red on hover

    public MathematicaWindow() {
        setTitle("Mathematica");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800); // Increased size for new layout
        setMinimumSize(new Dimension(1000, 700));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Could not set look and feel: " + e.getMessage());
        }

        // Initialize CRUD manager
        try {
            // !! IMPORTANT: CHANGE THESE CREDENTIALS to your MySQL username and password !!
            crudManager = new CRUD("root", "dedakira");
            if (!crudManager.isConnected()) {
                JOptionPane.showMessageDialog(this,
                        "Database connection failed. Please check your MySQL server and credentials in CRUD.java.",
                        "Connection Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error initializing database connection: " + e.getMessage(),
                    "Initialization Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }

        getContentPane().setBackground(BACKGROUND_DARK); // Set overall frame background to dark

        add(createTopPanel(), BorderLayout.NORTH);

        // Main split pane: Input/Display Area (left) vs. History Sidebar (right)
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                                  createInputAndDisplayArea(), createHistoryPanel());
        mainSplitPane.setDividerLocation(800); // Initial width for input/display area
        mainSplitPane.setOneTouchExpandable(true);
        mainSplitPane.setBorder(null); // Remove default split pane border
        mainSplitPane.setBackground(BACKGROUND_DARK); // Set background for split pane itself

        add(mainSplitPane, BorderLayout.CENTER);

        loadHistoryData(); // Load history on startup
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 10)); // Center content
        panel.setBackground(BACKGROUND_LIGHT_DARKER); // Slightly lighter dark background for header
        panel.setBorder(BorderFactory.createEmptyBorder(15, 30, 15, 30));

        JLabel titleLabel = new JLabel("MATHEMATICA", SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                String text = getText();
                FontMetrics fm = g2d.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(text)) / 2;
                int y = (getHeight() + fm.getAscent()) / 2 - fm.getDescent();

                g2d.setColor(TEXT_LIGHT.darker().darker()); // Darker text for subtle shadow
                g2d.drawString(text, x + 2, y + 2);

                g2d.setColor(TEXT_LIGHT); // Light text for title
                g2d.drawString(text, x, y);
                g2d.dispose();
            }
        };
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 40));
        panel.add(titleLabel);

        return panel;
    }

    private JComponent createInputAndDisplayArea() {
        // This will be a JSplitPane dividing into input/actions (left) and display areas (right)
        JSplitPane centralSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                                     createInputPanel(), createDisplayPanels());
        centralSplitPane.setDividerLocation(350); // Width for input panel
        centralSplitPane.setOneTouchExpandable(true);
        centralSplitPane.setBorder(null);
        centralSplitPane.setBackground(BACKGROUND_DARK); // Set background for split pane itself

        return centralSplitPane;
    }

    private JPanel createInputPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout()); // Use GridBagLayout for flexible centering
        panel.setBackground(BACKGROUND_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30)); // Padding

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 10, 15, 10); // Padding around components
        gbc.fill = GridBagConstraints.HORIZONTAL; // Fill horizontally
        gbc.gridwidth = 2; // Span across two conceptual columns for layout

        // Search Bar
        searchBar = new JTextField(30); // Adjusted size
        searchBar.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        searchBar.setForeground(TEXT_LIGHT);
        searchBar.setBackground(BACKGROUND_LIGHT_DARKER); // Darker background for input
        searchBar.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_DARK, 1), // Dark border
            BorderFactory.createEmptyBorder(12, 15, 12, 15) // Inner padding
        ));
        searchBar.setCaretColor(PRIMARY_ACCENT);
        // Optional: Apply rounded corners if using a custom UI look that supports it
        // searchBar.putClientProperty("JTextField.roundRect", true);
        searchBar.setToolTipText("Type your question or expression here");
        searchBar.addActionListener(e -> performSearch()); // Enter key performs search

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0; // Expand horizontally
        panel.add(searchBar, gbc);

        // Buttons Panel (for Search and Upload)
        JPanel buttonRowPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0)); // Flow layout for buttons
        buttonRowPanel.setOpaque(false); // Make it transparent

        // Search Button
        JButton searchBtn = new JButton("Search");
        searchBtn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        searchBtn.setForeground(TEXT_LIGHT);
        searchBtn.setBackground(PRIMARY_ACCENT);
        searchBtn.setFocusPainted(false);
        searchBtn.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25)); // Padding
        searchBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        // Optional: Apply rounded corners if using a custom UI look that supports it
        // searchBtn.putClientProperty("JButton.roundRect", true);
        searchBtn.setToolTipText("Initiate Search");
        searchBtn.addActionListener(e -> performSearch());
        searchBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { searchBtn.setBackground(PRIMARY_ACCENT.darker()); }
            public void mouseExited(MouseEvent e) { searchBtn.setBackground(PRIMARY_ACCENT); }
        });
        buttonRowPanel.add(searchBtn);

        // Upload Image Button
        JButton uploadBtn = new JButton("Upload Image");
        uploadBtn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        uploadBtn.setForeground(TEXT_LIGHT);
        uploadBtn.setBackground(BACKGROUND_LIGHT_DARKER.darker()); // Darker than panel for subtle contrast
        uploadBtn.setFocusPainted(false);
        uploadBtn.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25)); // Padding
        uploadBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        // Optional: Apply rounded corners if using a custom UI look that supports it
        // uploadBtn.putClientProperty("JButton.roundRect", true);
        uploadBtn.setToolTipText("Upload images from your system");
        uploadBtn.addActionListener(e -> openFileChooser());
        uploadBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { uploadBtn.setBackground(BACKGROUND_LIGHT_DARKER.darker().darker()); }
            public void mouseExited(MouseEvent e) { uploadBtn.setBackground(BACKGROUND_LIGHT_DARKER.darker()); }
        });
        buttonRowPanel.add(uploadBtn);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weighty = 0; // Don't expand
        gbc.fill = GridBagConstraints.NONE; // Don't fill
        panel.add(buttonRowPanel, gbc);

        // Placeholder for future results (if any other than extracted text/graph)
        // For now, it will just fill space
        JPanel emptyFiller = new JPanel();
        emptyFiller.setOpaque(false); // Transparent
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weighty = 1.0; // Take up remaining vertical space
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(emptyFiller, gbc);


        return panel;
    }

    private JComponent createDisplayPanels() {
        // This will be a JSplitPane dividing into Extracted Text (top) and Graph Display (bottom)
        JSplitPane displaySplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                                                      createExtractedTextPanel(), createGraphDisplayPanel());
        displaySplitPane.setDividerLocation(0.5); // 50/50 split initially
        displaySplitPane.setOneTouchExpandable(true);
        displaySplitPane.setBorder(null);
        displaySplitPane.setBackground(BACKGROUND_DARK); // Set background for split pane itself

        return displaySplitPane;
    }

    private JPanel createExtractedTextPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BACKGROUND_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel title = new JLabel("Extracted Text", SwingConstants.LEFT);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(TEXT_LIGHT);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        panel.add(title, BorderLayout.NORTH);

        extractedTextArea = new JTextArea();
        extractedTextArea.setEditable(false); // Make it read-only
        extractedTextArea.setFont(new Font("Consolas", Font.PLAIN, 14)); // Monospaced font for code/text
        extractedTextArea.setForeground(TEXT_LIGHT);
        extractedTextArea.setBackground(BACKGROUND_LIGHT_DARKER); // Slightly lighter dark background
        extractedTextArea.setCaretColor(TEXT_ACCENT); // Cursor color
        extractedTextArea.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_DARK, 1),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        extractedTextArea.setLineWrap(true);
        extractedTextArea.setWrapStyleWord(true);
        extractedTextArea.setText("Extracted text from images will appear here.");

        JScrollPane scrollPane = new JScrollPane(extractedTextArea);
        scrollPane.setBorder(null); // No extra border on scroll pane
        scrollPane.getViewport().setBackground(BACKGROUND_LIGHT_DARKER); // Ensure viewport matches text area
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createGraphDisplayPanel() {
        graphDisplayPanel = new JPanel(new BorderLayout());
        graphDisplayPanel.setBackground(BACKGROUND_DARK); // Dark background
        graphDisplayPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel title = new JLabel("Graph Display", SwingConstants.LEFT);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(TEXT_LIGHT);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        graphDisplayPanel.add(title, BorderLayout.NORTH);

        JPanel placeholderContent = new JPanel(new GridBagLayout());
        placeholderContent.setBackground(BACKGROUND_LIGHT_DARKER); // Background for the placeholder
        placeholderContent.setBorder(new LineBorder(BORDER_DARK, 1)); // Subtle border
        JLabel placeholderLabel = new JLabel("Graph Would Be Shown Here", SwingConstants.CENTER);
        placeholderLabel.setFont(new Font("Segoe UI", Font.ITALIC, 20));
        placeholderLabel.setForeground(TEXT_ACCENT);
        placeholderContent.add(placeholderLabel, new GridBagConstraints()); // Center the label

        graphDisplayPanel.add(placeholderContent, BorderLayout.CENTER);

        return graphDisplayPanel;
    }

    private JPanel createHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BACKGROUND_LIGHT_DARKER); // Slightly lighter dark background for history
        panel.setBorder(BorderFactory.createEmptyBorder(20, 15, 15, 15));

        JLabel historyTitle = new JLabel("History", SwingConstants.CENTER);
        historyTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        historyTitle.setForeground(TEXT_LIGHT);
        historyTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        panel.add(historyTitle, BorderLayout.NORTH);

        historyTable = new JTable();
        historyTable.setFillsViewportHeight(true);
        historyTable.setRowHeight(35);
        historyTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        historyTable.setForeground(TEXT_LIGHT);
        historyTable.setBackground(BACKGROUND_DARK); // Dark background for table rows
        historyTable.setSelectionBackground(PRIMARY_ACCENT.darker()); // Darker accent for selection
        historyTable.setSelectionForeground(Color.WHITE);
        historyTable.setGridColor(BORDER_DARK); // Darker grid lines
        historyTable.setShowVerticalLines(false);
        historyTable.setShowHorizontalLines(true);

        // Table Header Styling
        historyTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        historyTable.getTableHeader().setBackground(BACKGROUND_LIGHT_DARKER.darker()); // Even darker header
        historyTable.getTableHeader().setForeground(TEXT_LIGHT);
        historyTable.getTableHeader().setReorderingAllowed(false);
        historyTable.getTableHeader().setPreferredSize(new Dimension(1, 40));

        // Center align table content
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        // Apply custom renderer to all columns
        historyTable.setDefaultRenderer(Object.class, centerRenderer);

        JScrollPane scrollPane = new JScrollPane(historyTable);
        scrollPane.setBorder(new LineBorder(BORDER_DARK, 1)); // Dark subtle border
        scrollPane.getViewport().setBackground(BACKGROUND_DARK); // Ensure viewport matches table rows
        panel.add(scrollPane, BorderLayout.CENTER);

        // Delete History Button
        JButton deleteHistoryBtn = new JButton("Delete Old History (15 Days)");
        deleteHistoryBtn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        deleteHistoryBtn.setForeground(TEXT_LIGHT);
        deleteHistoryBtn.setBackground(DELETE_BUTTON_COLOR); // Red color
        deleteHistoryBtn.setFocusPainted(false);
        deleteHistoryBtn.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        deleteHistoryBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        // Optional: Apply rounded corners if using a custom UI look that supports it
        // deleteHistoryBtn.putClientProperty("JButton.roundRect", true);
        deleteHistoryBtn.setToolTipText("Permanently delete history entries older than 15 days");
        deleteHistoryBtn.addActionListener(e -> {
            if (crudManager != null && crudManager.isConnected()) {
                int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to delete all history older than 15 days?\nThis action cannot be undone.",
                    "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (confirm == JOptionPane.YES_OPTION) {
                    int deletedCount = crudManager.Delete(15);
                    JOptionPane.showMessageDialog(this,
                        deletedCount + " old history entries deleted.",
                        "Deletion Complete", JOptionPane.INFORMATION_MESSAGE);
                    loadHistoryData();
                }
            } else {
                JOptionPane.showMessageDialog(this,
                    "Database not connected. Cannot perform deletion.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        deleteHistoryBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { deleteHistoryBtn.setBackground(DELETE_BUTTON_HOVER); }
            public void mouseExited(MouseEvent e) { deleteHistoryBtn.setBackground(DELETE_BUTTON_COLOR); }
        });

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 15));
        bottomPanel.setOpaque(false);
        bottomPanel.add(deleteHistoryBtn);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void performSearch() {
        String query = searchBar.getText().trim();
        if (!query.isEmpty()) {
            if (crudManager != null && crudManager.isConnected()) {
                // Assuming "N/A (text query)" is the FilePath for text searches
                crudManager.CreateData("N/A (text query)", query, false);
                JOptionPane.showMessageDialog(this, "Query submitted: \"" + query + "\"\n(Saved to database history)", "Search Action", JOptionPane.INFORMATION_MESSAGE);
                loadHistoryData();
                // Clear extracted text and graph placeholder when a new text search is done
                extractedTextArea.setText("Extracted text from images will appear here.");
                // Potentially clear or reset graph display if applicable
            } else {
                 JOptionPane.showMessageDialog(this, "Database not connected. Cannot save search query.", "Error", JOptionPane.ERROR_MESSAGE);
            }
            searchBar.setText(""); // Clear the search bar
        } else {
            JOptionPane.showMessageDialog(this, "Please enter a query to search.", "Empty Search", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void openFileChooser() {
        JFileChooser fc = new JFileChooser();
        fc.setMultiSelectionEnabled(false); // Only allow single image upload for now
        fc.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                if (f.isDirectory()) return true;
                String name = f.getName().toLowerCase();
                return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".gif") || name.endsWith(".bmp") || name.endsWith(".webp");
            }

            @Override
            public String getDescription() {
                return "Image files (*.jpg, *.jpeg, *.png, *.gif, *.bmp, *.webp)";
            }
        });
        int val = fc.showOpenDialog(this);
        if (val == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fc.getSelectedFile();
            if (selectedFile != null) {
                String imagePath = selectedFile.getAbsolutePath();
                
                // --- Integrate TextExtract here ---
                // Create an instance of TextExtract and perform the OCR
                TextExtract textExtractor = new TextExtract(imagePath);
                String extractedText = textExtractor.getExtractedText(); // Get the result

                extractedTextArea.setText(extractedText); // Display extracted text in the UI

                if (crudManager != null && crudManager.isConnected()) {
                    // Save the image path and the extracted text to history
                    crudManager.CreateData(imagePath, extractedText, false);
                    JOptionPane.showMessageDialog(this, "Image selected and text extracted.\nSaved to history.", "Image Upload", JOptionPane.INFORMATION_MESSAGE);
                    loadHistoryData(); // Refresh history table
                } else {
                    JOptionPane.showMessageDialog(this, "Database not connected. Image uploaded and text extracted, but not saved to history.", "Warning", JOptionPane.WARNING_MESSAGE);
                }
            }
        }
    }

    private void loadHistoryData() {
        if (crudManager != null && crudManager.isConnected()) {
            DefaultTableModel model = crudManager.getHistoryTableModel();
            historyTable.setModel(model);

            // Adjust column widths for better display in the sidebar
            if (model.getColumnCount() > 0) {
                // Example adjustments, you might need to fine-tune these
                historyTable.getColumnModel().getColumn(0).setPreferredWidth(80);  // File Path (can be shrunk if paths are long)
                historyTable.getColumnModel().getColumn(1).setPreferredWidth(180); // Question/Extracted Text
                historyTable.getColumnModel().getColumn(2).setPreferredWidth(40);  // Graph Plotted (checkbox/boolean)
                historyTable.getColumnModel().getColumn(3).setPreferredWidth(120); // Date/Time
            }

            // Ensure header renderer is set for center alignment
            DefaultTableCellRenderer headerRenderer = (DefaultTableCellRenderer) historyTable.getTableHeader().getDefaultRenderer();
            headerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

            // This sets the default renderer for all Object columns to center-align
            DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer();
            cellRenderer.setHorizontalAlignment(SwingConstants.CENTER);
            historyTable.setDefaultRenderer(Object.class, cellRenderer);


            System.out.println("Database history data loaded into UI.");
        } else {
            System.err.println("Database not connected or CRUD manager not initialized. Cannot load history data.");
            // Clear the table if no connection
            historyTable.setModel(new DefaultTableModel(new Vector<>(), new Vector<>()));
        }
    }

    // --- Custom Icon classes (These are no longer used directly by buttons
    //     as they are text-based now, but kept in case you want to use icons later) ---
    static class SearchIcon implements Icon {
        private final int size;
        private final Color color;

        public SearchIcon(int size, Color color) {
            this.size = size;
            this.color = color;
        }

        @Override
        public int getIconWidth() { return size; }
        @Override
        public int getIconHeight() { return size; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(color);
            g2.drawOval(x, y, (int)(size * 0.7), (int)(size * 0.7));
            int handleStartX = x + (int)(size * 0.5);
            int handleStartY = y + (int)(size * 0.5);
            int handleEndX = x + size;
            int handleEndY = y + size;
            g2.drawLine(handleStartX, handleStartY, handleEndX, handleEndY);
            g2.dispose();
        }
    }

    static class FolderUploadIcon implements Icon {
        private final int size;
        private final Color color;

        public FolderUploadIcon(int size, Color color) {
            this.size = size;
            this.color = color;
        }

        @Override
        public int getIconWidth() { return size; }
        @Override
        public int getIconHeight() { return size; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            int folderWidth = (int)(size * 0.9);
            int folderHeight = (int)(size * 0.6);
            int tabWidth = (int)(folderWidth * 0.3);
            g2.fillRoundRect(x + (size - folderWidth) / 2, y + (int)(size * 0.4), folderWidth, folderHeight, 5, 5);
            Polygon tab = new Polygon();
            tab.addPoint(x + (size - folderWidth) / 2 + (int)(folderWidth * 0.1), y + (int)(size * 0.4));
            tab.addPoint(x + (size - folderWidth) / 2 + (int)(folderWidth * 0.1), y + (int)(size * 0.4) - (int)(folderHeight * 0.2));
            tab.addPoint(x + (size - folderWidth) / 2 + (int)(folderWidth * 0.1) + tabWidth, y + (int)(size * 0.4) - (int)(folderHeight * 0.2));
            tab.addPoint(x + (size - folderWidth) / 2 + (int)(folderWidth * 0.1) + tabWidth, y + (int)(size * 0.4));
            g2.fillPolygon(tab);
            g2.dispose();
        }
    }

    public static void main(String[] args) {
        // Apply System Look and Feel early for better UI integration
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            MathematicaWindow window = new MathematicaWindow();
            window.setVisible(true);
        });
    }
}
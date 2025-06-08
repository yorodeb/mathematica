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

public class Window extends JFrame {

    private JTextField searchBar;
    private JTable historyTable;
    private JTextArea extractedTextArea;
    private JPanel graphDisplayPanel;
    private DataBase crudManager;
    private TextExtract textExtractor;
    private JFreeChartGrapher jfreeChartGrapher;

    private static final Color PRIMARY_ACCENT = new Color(70, 130, 180);
    private static final Color BACKGROUND_DARK = new Color(40, 44, 52);
    private static final Color BACKGROUND_LIGHT_DARKER = new Color(60, 65, 75);
    private static final Color TEXT_LIGHT = new Color(120, 180, 250);
    private static final Color TEXT_ACCENT = new Color(170, 180, 200);
    private static final Color BORDER_DARK = new Color(80, 85, 95);

    private static final Color DELETE_BUTTON_COLOR = new Color(120, 180, 250);
    private static final Color DELETE_BUTTON_HOVER = new Color(200, 70, 70);

    public Window() {
        setTitle("Mathematica");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setMinimumSize(new Dimension(1000, 700));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Could not set look and feel: " + e.getMessage());
        }

        crudManager = new DataBase("root", "dedakira");
        if (!crudManager.isConnected()) {
            JOptionPane.showMessageDialog(this,
                    "Database connection failed.",
                    "Connection Error",
                    JOptionPane.ERROR_MESSAGE);
        }
        
        textExtractor = new TextExtract("");
        jfreeChartGrapher = new JFreeChartGrapher();

        getContentPane().setBackground(BACKGROUND_DARK);

        add(createTopPanel(), BorderLayout.NORTH);

        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                                   createInputAndDisplayArea(), createHistoryPanel());
        mainSplitPane.setDividerLocation(800);
        mainSplitPane.setOneTouchExpandable(true);
        mainSplitPane.setBorder(null);
        mainSplitPane.setBackground(BACKGROUND_DARK);

        add(mainSplitPane, BorderLayout.CENTER);

        loadHistoryData();
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 10));
        panel.setBackground(BACKGROUND_LIGHT_DARKER);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 30, 15, 30));

        JLabel titleLabel = new JLabel("Mathematica", SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                String text = getText();
                FontMetrics fm = g2d.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(text)) / 2;
                int y = (getHeight() + fm.getAscent()) / 2 - fm.getDescent();

                g2d.setColor(TEXT_LIGHT.darker().darker());
                g2d.drawString(text, x + 2, y + 2);

                g2d.setColor(TEXT_LIGHT);
                g2d.drawString(text, x, y);
                g2d.dispose();
            }
        };
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 40));
        panel.add(titleLabel);

        return panel;
    }

    private JComponent createInputAndDisplayArea() {
        JSplitPane centralSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                                      createInputPanel(), createDisplayPanels());
        centralSplitPane.setDividerLocation(350);
        centralSplitPane.setOneTouchExpandable(true);
        centralSplitPane.setBorder(null);
        centralSplitPane.setBackground(BACKGROUND_DARK);

        return centralSplitPane;
    }

    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BACKGROUND_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 10, 15, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 2;

        searchBar = new JTextField(30);
        searchBar.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        searchBar.setForeground(TEXT_LIGHT);
        searchBar.setBackground(BACKGROUND_LIGHT_DARKER);
        searchBar.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_DARK, 1),
            BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));
        searchBar.setCaretColor(PRIMARY_ACCENT);
        searchBar.setToolTipText("Type your question or expression here");
        searchBar.addActionListener(e -> performSearch());

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        panel.add(searchBar, gbc);

        JPanel buttonRowPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        buttonRowPanel.setOpaque(false);

        JButton searchBtn = new JButton("Search");
        searchBtn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        searchBtn.setForeground(TEXT_LIGHT);
        searchBtn.setBackground(PRIMARY_ACCENT);
        searchBtn.setFocusPainted(false);
        searchBtn.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));
        searchBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        searchBtn.setToolTipText("Initiate Search");
        searchBtn.addActionListener(e -> performSearch());
        searchBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { searchBtn.setBackground(PRIMARY_ACCENT.darker()); }
            public void mouseExited(MouseEvent e) { searchBtn.setBackground(PRIMARY_ACCENT); }
        });
        buttonRowPanel.add(searchBtn);

        JButton uploadBtn = new JButton("Upload Image");
        uploadBtn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        uploadBtn.setForeground(TEXT_LIGHT);
        uploadBtn.setBackground(BACKGROUND_LIGHT_DARKER.darker());
        uploadBtn.setFocusPainted(false);
        uploadBtn.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));
        uploadBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        uploadBtn.setToolTipText("Upload images from your system");
        uploadBtn.addActionListener(e -> openFileChooser());
        uploadBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { uploadBtn.setBackground(BACKGROUND_LIGHT_DARKER.darker().darker()); }
            public void mouseExited(MouseEvent e) { uploadBtn.setBackground(BACKGROUND_LIGHT_DARKER.darker()); }
        });
        buttonRowPanel.add(uploadBtn);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(buttonRowPanel, gbc);

        JPanel emptyFiller = new JPanel();
        emptyFiller.setOpaque(false);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(emptyFiller, gbc);

        return panel;
    }

    private JComponent createDisplayPanels() {
        JSplitPane displaySplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                                                      createExtractedTextPanel(), createGraphDisplayPanel());
        displaySplitPane.setDividerLocation(0.5);
        displaySplitPane.setOneTouchExpandable(true);
        displaySplitPane.setBorder(null);
        displaySplitPane.setBackground(BACKGROUND_DARK);

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
        extractedTextArea.setEditable(true);
        extractedTextArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        extractedTextArea.setForeground(TEXT_LIGHT);
        extractedTextArea.setBackground(BACKGROUND_LIGHT_DARKER);
        extractedTextArea.setCaretColor(PRIMARY_ACCENT);
        extractedTextArea.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_DARK, 1),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        extractedTextArea.setLineWrap(true);
        extractedTextArea.setWrapStyleWord(true);
        extractedTextArea.setText("Extracted text from images will appear here. You can edit it before plotting.");

        JScrollPane scrollPane = new JScrollPane(extractedTextArea);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(BACKGROUND_LIGHT_DARKER);
        panel.add(scrollPane, BorderLayout.CENTER);

        JButton plotExtractedTextBtn = new JButton("Plot from Extracted Text");
        plotExtractedTextBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        plotExtractedTextBtn.setForeground(TEXT_LIGHT);
        plotExtractedTextBtn.setBackground(PRIMARY_ACCENT.darker());
        plotExtractedTextBtn.setFocusPainted(false);
        plotExtractedTextBtn.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        plotExtractedTextBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        plotExtractedTextBtn.addActionListener(e -> plotExtractedText());
        plotExtractedTextBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { plotExtractedTextBtn.setBackground(PRIMARY_ACCENT.darker().darker()); }
            public void mouseExited(MouseEvent e) { plotExtractedTextBtn.setBackground(PRIMARY_ACCENT.darker()); }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        buttonPanel.add(plotExtractedTextBtn);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createGraphDisplayPanel() {
        graphDisplayPanel = new JPanel(new BorderLayout());
        graphDisplayPanel.setBackground(BACKGROUND_DARK);
        graphDisplayPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel title = new JLabel("Graph Display", SwingConstants.LEFT);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(TEXT_LIGHT);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        graphDisplayPanel.add(title, BorderLayout.NORTH);

        JPanel placeholderContent = new JPanel(new GridBagLayout());
        placeholderContent.setBackground(BACKGROUND_LIGHT_DARKER);
        placeholderContent.setBorder(new LineBorder(BORDER_DARK, 1));
        JLabel placeholderLabel = new JLabel("Graph Would Be Shown Here", SwingConstants.CENTER);
        placeholderLabel.setFont(new Font("Segoe UI", Font.ITALIC, 20));
        placeholderLabel.setForeground(TEXT_ACCENT);
        placeholderContent.add(placeholderLabel, new GridBagConstraints());

        graphDisplayPanel.add(placeholderContent, BorderLayout.CENTER);

        return graphDisplayPanel;
    }

    private JPanel createHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BACKGROUND_LIGHT_DARKER);
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
        historyTable.setBackground(BACKGROUND_DARK);
        historyTable.setSelectionBackground(PRIMARY_ACCENT.darker());
        historyTable.setSelectionForeground(Color.WHITE);
        historyTable.setGridColor(BORDER_DARK);
        historyTable.setShowVerticalLines(false);
        historyTable.setShowHorizontalLines(true);

        historyTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        historyTable.getTableHeader().setBackground(BACKGROUND_LIGHT_DARKER.darker());
        historyTable.getTableHeader().setForeground(TEXT_LIGHT);
        historyTable.getTableHeader().setReorderingAllowed(false);
        historyTable.getTableHeader().setPreferredSize(new Dimension(1, 40));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        historyTable.setDefaultRenderer(Object.class, centerRenderer);

        JScrollPane scrollPane = new JScrollPane(historyTable);
        scrollPane.setBorder(new LineBorder(BORDER_DARK, 1));
        scrollPane.getViewport().setBackground(BACKGROUND_DARK);
        panel.add(scrollPane, BorderLayout.CENTER);

        JButton deleteHistoryBtn = new JButton("Delete Old History (15 Days)");
        deleteHistoryBtn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        deleteHistoryBtn.setForeground(TEXT_LIGHT);
        deleteHistoryBtn.setBackground(DELETE_BUTTON_COLOR);
        deleteHistoryBtn.setFocusPainted(false);
        deleteHistoryBtn.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        deleteHistoryBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
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

    private boolean attemptPlotEquation(String equationToParse, String originalQuery) {
        String plotTitle = "Plot of " + originalQuery;
        String processedEquation = equationToParse;
        boolean graphWasPlotted = false;

        if (processedEquation.toLowerCase().startsWith("y =")) {
            processedEquation = processedEquation.substring(processedEquation.toLowerCase().indexOf("y =") + "y =".length()).trim();
        }

        try {
            JPanel chartPanel = jfreeChartGrapher.createChartPanelForEquation(processedEquation, plotTitle);

            if (chartPanel != null) {
                displayCustomPanel(chartPanel);
                JOptionPane.showMessageDialog(this, "Graph for '" + originalQuery + "' displayed.", "Plot Success", JOptionPane.INFORMATION_MESSAGE);
                graphWasPlotted = true;
            } else {
                JOptionPane.showMessageDialog(this,
                    "Could not plot equation: '" + originalQuery + "'.\n" +
                    "Please ensure it's a valid 'y = f(x)' format (e.g., 'x^2', '8*x - 9', '(8 - 2*x) / 3').",
                    "Plotting Error", JOptionPane.WARNING_MESSAGE);
                clearGraphDisplay();
                graphWasPlotted = false;
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "An unexpected error occurred while plotting '" + originalQuery + "': " + e.getMessage(),
                "Plotting Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            clearGraphDisplay();
            graphWasPlotted = false;
        }
        return graphWasPlotted;
    }

    private void displayCustomPanel(JPanel panel) {
        graphDisplayPanel.removeAll();
        graphDisplayPanel.setLayout(new BorderLayout());

        if (panel != null) {
            graphDisplayPanel.add(panel, BorderLayout.CENTER);
        } else {
            JLabel errorLabel = new JLabel("Graph panel could not be generated.", SwingConstants.CENTER);
            errorLabel.setForeground(TEXT_ACCENT);
            errorLabel.setFont(new Font("Segoe UI", Font.ITALIC, 16));
            graphDisplayPanel.add(errorLabel, BorderLayout.CENTER);
        }
        graphDisplayPanel.revalidate();
        graphDisplayPanel.repaint();
    }

    private void clearGraphDisplay() {
        graphDisplayPanel.removeAll();
        graphDisplayPanel.setLayout(new BorderLayout());
        
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

    private void plotExtractedText() {
        String textToPlot = extractedTextArea.getText().trim();
        if (!textToPlot.isEmpty() &&
            !textToPlot.equalsIgnoreCase("Extracted text from images will appear here. You can edit it before plotting.") &&
            !textToPlot.startsWith("Error extracting text:") &&
            !textToPlot.equalsIgnoreCase("File::Error")) {
            
            attemptPlotEquation(textToPlot, "Edited/Extracted: " + textToPlot);
        } else {
            JOptionPane.showMessageDialog(this,
                "The extracted text area is empty or contains an error message. Please enter a valid equation to plot.",
                "Plotting Error", JOptionPane.WARNING_MESSAGE);
            clearGraphDisplay();
        }
    }

    private void performSearch() {
        String query = searchBar.getText().trim();
        if (!query.isEmpty()) {
            if (crudManager != null && crudManager.isConnected()) {
                // No longer need to store graphPlotted status here as it's not in DB
                attemptPlotEquation(query, query); // Still attempt to plot for user experience

                // Changed to call CreateData without the graphPlotted boolean
                crudManager.CreateData("N/A (text query)", query); 
                
                JOptionPane.showMessageDialog(this, "Query submitted: \"" + query + "\"\n(Saved to database history)", "Search Action", JOptionPane.INFORMATION_MESSAGE);
                loadHistoryData();
                extractedTextArea.setText("Extracted text from images will appear here. You can edit it before plotting.");
            } else {
                JOptionPane.showMessageDialog(this, "Database not connected. Cannot save search query.", "Error", JOptionPane.ERROR_MESSAGE);
                clearGraphDisplay();
            }
            searchBar.setText("");
        } else {
            JOptionPane.showMessageDialog(this, "Please enter a query to search or plot.", "Empty Input", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void openFileChooser() {
        JFileChooser fc = new JFileChooser();
        fc.setMultiSelectionEnabled(false);
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
                
                String extractedText = textExtractor.perform(imagePath);

                extractedTextArea.setText(extractedText);

                // No longer store graphPlotted boolean here directly for DB saving
                // Still call attemptPlotEquation to display the graph to the user
                if (!extractedText.trim().isEmpty() && !extractedText.trim().equalsIgnoreCase("File::Error")) {
                    attemptPlotEquation(extractedText.trim(), extractedText.trim());
                } else {
                    JOptionPane.showMessageDialog(this, "No valid text extracted from image to plot.", "Information", JOptionPane.INFORMATION_MESSAGE);
                    clearGraphDisplay();
                }

                if (crudManager != null && crudManager.isConnected()) {
                    // Changed to call CreateData without the graphPlotted boolean
                    crudManager.CreateData(imagePath, extractedText);
                    JOptionPane.showMessageDialog(this, "Image selected and text extracted.\nSaved to history.", "Image Upload", JOptionPane.INFORMATION_MESSAGE);
                    loadHistoryData();
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

            // Adjusted column count check and removed the Graph_Plotted column's width setting
            if (model.getColumnCount() >= 3) { // Assuming 3 columns now: ID, Image Path/Query, Extracted Text/Result
                historyTable.getColumnModel().getColumn(0).setPreferredWidth(80);
                historyTable.getColumnModel().getColumn(1).setPreferredWidth(180);
                historyTable.getColumnModel().getColumn(2).setPreferredWidth(40);
                // Removed: historyTable.getColumnModel().getColumn(3).setPreferredWidth(120);
            }

            DefaultTableCellRenderer headerRenderer = (DefaultTableCellRenderer) historyTable.getTableHeader().getDefaultRenderer();
            headerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

            DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer();
            cellRenderer.setHorizontalAlignment(SwingConstants.CENTER);
            historyTable.setDefaultRenderer(Object.class, cellRenderer);

            System.out.println("Database history data loaded into UI.");
        } else {
            System.err.println("Database not connected or manager not initialized. Cannot load history data.");
            historyTable.setModel(new DefaultTableModel(new Vector<>(), new Vector<>()));
        }
    }

    static class JFreeChartGrapher {

        public JPanel createChartPanelForEquation(String equation, String title) {
            XYSeries series = new XYSeries("y = " + equation);
            try {
                Expression expression = new ExpressionBuilder(equation)
                        .variables("x")
                        .build();

                for (double x = -10; x <= 10; x += 0.1) {
                    try {
                        expression.setVariable("x", x);
                        double y = expression.evaluate();
                        if (Double.isFinite(y)) {
                            series.add(x, y);
                        }
                    } catch (IllegalArgumentException e) {
                        // This catches cases like log(0), sqrt(-ve) which result in NaN/Infinity
                        // and are properly handled by isFinite() check for exclusion.
                        // Can be left empty if you don't need specific error logging for each point.
                    }
                }

                XYSeriesCollection dataset = new XYSeriesCollection();
                dataset.addSeries(series);

                JFreeChart chart = ChartFactory.createXYLineChart(
                    title,
                    "X",
                    "Y",
                    dataset,
                    PlotOrientation.VERTICAL,
                    true,
                    true,
                    false
                );

                chart.setBackgroundPaint(new Color(60, 65, 75));
                chart.getTitle().setPaint(new Color(220, 220, 220));
                chart.getXYPlot().setBackgroundPaint(new Color(40, 44, 52));
                chart.getXYPlot().setDomainGridlinePaint(new Color(80, 85, 95));
                chart.getXYPlot().setRangeGridlinePaint(new Color(80, 85, 95));

                chart.getXYPlot().getDomainAxis().setLabelPaint(new Color(170, 180, 200));
                chart.getXYPlot().getDomainAxis().setTickLabelPaint(new Color(170, 180, 200));
                chart.getXYPlot().getRangeAxis().setLabelPaint(new Color(170, 180, 200));
                chart.getXYPlot().getRangeAxis().setTickLabelPaint(new Color(170, 180, 200));
                
                chart.getLegend().setItemPaint(new Color(220, 220, 220));

                return new ChartPanel(chart);

            } catch (Exception e) {
                System.err.println("Error creating chart for equation '" + equation + "': " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        }
    }

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
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            Window window = new Window();
            window.setVisible(true);
        });
    }
}

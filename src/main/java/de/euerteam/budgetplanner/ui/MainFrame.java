package de.euerteam.budgetplanner.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

import de.euerteam.budgetplanner.service.CategoryManager;
import de.euerteam.budgetplanner.service.TransactionService;

public class MainFrame extends JFrame {

    private boolean darkMode = false;
    private final JButton themeButton;

    public MainFrame() {
        super("BudgetPlanner");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 650);
        setLocationRelativeTo(null);

        TransactionService transactionService = new TransactionService();
        CategoryManager categoryManager = new CategoryManager();

        JTabbedPane tabs = new JTabbedPane();

        TransactionsPanel transactionsPanel = new TransactionsPanel(transactionService, categoryManager);
        StatisticPanel statisticPanel = new StatisticPanel(transactionService);
        BudgetsPanel budgetsPanel = new BudgetsPanel(transactionService, categoryManager);

        tabs.addTab("Buchungen", transactionsPanel);
        tabs.addTab("Statistiken", statisticPanel);
        tabs.addTab("Budgets", budgetsPanel);

        tabs.addChangeListener(e -> {
            if (tabs.getSelectedComponent() == statisticPanel){
                statisticPanel.refreshCharts();
            }
            if (tabs.getSelectedComponent() == budgetsPanel){
                budgetsPanel.refreshData();
            }
        });

        // Layout: CENTER = tabs, EAST = panel with theme button (right aligned)
        setLayout(new BorderLayout());

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        themeButton = new JButton("🌙 Dark");
        themeButton.setFocusable(false);
        rightPanel.add(themeButton);

        themeButton.addActionListener(e -> toggleTheme());

        add(tabs, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);
    }

    private void toggleTheme() {
        try {
            // save current window size/position/state so we can restore it
            Dimension currentSize = this.getSize();
            Point currentLocation = this.getLocation();
            int state = this.getExtendedState();

            darkMode = !darkMode;

            if (darkMode) {
                FlatDarkLaf.setup();
                themeButton.setText("☀ Light");
            } else {
                FlatLightLaf.setup();
                themeButton.setText("🌙 Dark");
            }

            // update UI tree but keep the same window size/position
            SwingUtilities.updateComponentTreeUI(this);
            this.setSize(currentSize);
            this.setLocation(currentLocation);
            this.setExtendedState(state);
            this.revalidate();
            this.repaint();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

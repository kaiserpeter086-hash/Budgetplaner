package de.euerteam.budgetplanner.ui;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;

import de.euerteam.budgetplanner.service.TransactionService;

public class MainFrame extends JFrame {

    public MainFrame() {
        super("BudgetPlanner");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 650);
        setLocationRelativeTo(null);

        TransactionService transactionService = new TransactionService();

        JTabbedPane tabs = new JTabbedPane();

        TransactionsPanel transactionsPanel = new TransactionsPanel(transactionService);
        StatisticPanel statisticPanel = new StatisticPanel(transactionService);
        BudgetsPanel budgetsPanel = new BudgetsPanel(transactionService);

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

        setContentPane(tabs);
    }
}

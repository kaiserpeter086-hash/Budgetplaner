package de.euerteam.budgetplanner.ui;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;

public class MainFrame extends JFrame {

    public MainFrame() {
        super("BudgetPlanner");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 650);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Buchungen", new TransactionsPanel());
        tabs.addTab("Statistiken", new StatisticsPanel());
        tabs.addTab("Budgets", new BudgetsPanel());

        setContentPane(tabs);
    }
}

package de.euerteam.budgetplanner.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerDateModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import de.euerteam.budgetplanner.service.CategoryManager;
import de.euerteam.budgetplanner.service.TransactionService;

public class BudgetsPanel extends JPanel {
    private final TransactionService transactionService;
    private final CategoryManager categoryManager;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.GERMANY);
    private DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MM.yyyy");

    private final JSpinner monthSpinner = new JSpinner(new SpinnerDateModel());
    private final JComboBox<String> categoryComboBox = new JComboBox<>();
    private final JFormattedTextField budgetAmountField = new JFormattedTextField(currencyFormat);

    private final DefaultTableModel tableModel = new DefaultTableModel(
        new Object[] {"Kategorie", "Budget", "Ausgaben", "Restbetrag", "Fortschritt"}, 0) 
        {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;   
        }
    };

    private final JTable budgetTable = new JTable(tableModel);
    
    public BudgetsPanel(TransactionService transactionService, CategoryManager categoryManager){
        this.transactionService = transactionService;
        this.categoryManager = categoryManager;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        monthSpinner.setEditor(new JSpinner.DateEditor(monthSpinner, "MM.yyyy"));
        categoryComboBox.setModel(new DefaultComboBoxModel<>(categoryManager.getCategories().toArray(new String[0])));
        categoryManager.addListener(() -> {
            categoryComboBox.setModel(new DefaultComboBoxModel<>(categoryManager.getCategories().toArray(new String[0])));
        });

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.add(new JLabel("Monat:"));
        controls.add(monthSpinner);
        controls.add(new JLabel("Kategorie:"));
        controls.add(categoryComboBox);
        controls.add(new JLabel("Budget:"));

        budgetAmountField.setColumns(10);
        budgetAmountField.setValue(0);
        controls.add(budgetAmountField);

        JButton saveBudgetButton = new JButton("Budget speichern");
        controls.add(saveBudgetButton);
        add(controls, BorderLayout.NORTH);

        budgetTable.setRowHeight(26);
        budgetTable.getColumnModel().getColumn(4).setCellRenderer(new ProgressRenderer());
        budgetTable.getColumnModel().getColumn(1).setCellRenderer(new CurrencyRenderer());
        budgetTable.getColumnModel().getColumn(2).setCellRenderer(new CurrencyRenderer());
        budgetTable.getColumnModel().getColumn(3).setCellRenderer(new CurrencyRenderer());

        add(new JScrollPane(budgetTable), BorderLayout.CENTER);

        monthSpinner.addChangeListener(e -> refreshData());
        saveBudgetButton.addActionListener(e -> saveBudget());

        refreshData();

    }

    public void refreshData(){
       YearMonth month = getSelectedMonth();
        Map<String, BigDecimal> budgets = transactionService.getBudgetsForMonth(month);
        Map<String, BigDecimal> expenses = transactionService.getExpensesCategoryForMonth(month);

        tableModel.setRowCount(0);
        for (String category : categoryManager.getCategories()) {
            
            BigDecimal budget = budgets.getOrDefault(category, BigDecimal.ZERO);
            BigDecimal expense = expenses.getOrDefault(category, BigDecimal.ZERO);
            BigDecimal remaining = budget.subtract(expense);

            int usagePercent;
            if (budget.compareTo(BigDecimal.ZERO) <= 0) {
                usagePercent = expense.compareTo(BigDecimal.ZERO) > 0 ? 200 : 0;
            } else {
                usagePercent = expense
                        .multiply(BigDecimal.valueOf(100))
                        .divide(budget, 0, RoundingMode.HALF_UP)
                        .intValue();
            }

            tableModel.addRow(new Object[] {category, budget, expense, remaining, usagePercent});
        }
    }

    private void saveBudget() {
        try {
            String category = (String) categoryComboBox.getSelectedItem();
            if (category == null || category.isBlank()) {
                throw new IllegalArgumentException("Bitte eine gültige Kategorie wählen.");
            }

            BigDecimal budgetValue = parseBudgetValue();
            transactionService.setMonthlyBudget(getSelectedMonth(), category, budgetValue);
            refreshData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Budget konnte nicht gespeichert werden: " + ex.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    private BigDecimal parseBudgetValue() {
        Object value = budgetAmountField.getValue();
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue()).max(BigDecimal.ZERO);
        }

        String cleaned = budgetAmountField.getText().replaceAll("[^0-9,.-]", "").replace(',', '.').trim();
        if (cleaned.isBlank()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(cleaned).max(BigDecimal.ZERO);
    }

     private YearMonth getSelectedMonth() {
        String value = ((JSpinner.DateEditor) monthSpinner.getEditor()).getFormat().format(monthSpinner.getValue());
        return YearMonth.parse(value, monthFormatter);
    }

     private class CurrencyRenderer extends DefaultTableCellRenderer {
        public CurrencyRenderer() {
            setHorizontalAlignment(LEFT);
        }
        @Override
        protected void setValue(Object value) {
            if (value instanceof BigDecimal) {
                setText(currencyFormat.format((BigDecimal) value));
                return;
            }
            if (value instanceof Number) {
                setText(currencyFormat.format(((Number) value).doubleValue()));
                return;
            }
            super.setValue(value);
        }
    }

     private static class ProgressRenderer extends DefaultTableCellRenderer{
        private static final Color LOW_USAGWE_COLOR = new Color(46, 125, 50);
        private static final Color MEDIUM_USAGE_COLOR = new Color(251, 192, 45);
        private static final Color HIGH_USAGE_COLOR = new Color(198, 40, 40);

        public ProgressRenderer() {
            setHorizontalAlignment(CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            int percent = value instanceof Number ? ((Number) value).intValue() : 0;

            setText(percent + " %");

            if (!isSelected) {
                if (percent < 80){
                    setForeground(LOW_USAGWE_COLOR);
                } else if (percent < 100) {
                    setForeground(MEDIUM_USAGE_COLOR);
                } else {
                    setForeground(HIGH_USAGE_COLOR);
                }
            }
            return this;
        }

    }

}

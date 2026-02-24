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
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerDateModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import de.euerteam.budgetplanner.model.CategoryType;
import de.euerteam.budgetplanner.service.TransactionService;

public class BudgetsPanel extends JPanel {
    private final TransactionService transactionService;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.GERMANY);
    private DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MM.yyyy");

    private final JSpinner monthSpinner = new JSpinner(new SpinnerDateModel());
    private final JComboBox<CategoryType> categoryComboBox = new JComboBox<>();
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
    
    public BudgetsPanel(TransactionService transactionService){
        this.transactionService = transactionService;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        monthSpinner.setEditor(new JSpinner.DateEditor(monthSpinner, "MM.yyyy"));
        categoryComboBox.setModel(new DefaultComboBoxModel<>(getBudgetCategories().toArray(new CategoryType[0])));

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
        budgetTable.getColumnModel().getColumn(1).setCellRenderer(new ProgressRenderer());
        budgetTable.getColumnModel().getColumn(2).setCellRenderer(new ProgressRenderer());
        budgetTable.getColumnModel().getColumn(3).setCellRenderer(new ProgressRenderer());

        add(new JScrollPane(budgetTable), BorderLayout.CENTER);

        monthSpinner.addChangeListener(e -> refreshData());
        saveBudgetButton.addActionListener(e -> saveBudget());

        refreshData();

    }

    public void refreshData(){
       YearMonth month = getSelectedMonth();
        Map<CategoryType, BigDecimal> budgets = transactionService.getBudgetsForMonth(month);
        Map<CategoryType, BigDecimal> expenses = transactionService.getExpensesCategoryForMonth(month);

        tableModel.setRowCount(0);
        for (CategoryType category : getBudgetCategories()) {
            
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

    private List<CategoryType> getBudgetCategories(){
        return Arrays.stream(CategoryType.values())
                .filter(c -> c != CategoryType.Auswahl)
                .filter(c -> !c.name().equalsIgnoreCase("FORMAT"))
                .filter(c -> !c.name().equalsIgnoreCase("DISPLAY"))
                .collect(Collectors.toList());
    }

    private void saveBudget() {
        try {
            CategoryType category = (CategoryType) categoryComboBox.getSelectedItem();
            if (category == null || category == CategoryType.Auswahl) {
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
            setHorizontalAlignment(RIGHT);
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

     private static class ProgressRenderer implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {
            int percent = value instanceof Number ? ((Number) value).intValue() : 0;

            JProgressBar bar = new JProgressBar(0, 100);
            bar.setValue(Math.max(0, Math.min(100, percent)));
            bar.setStringPainted(true);
            bar.setString(percent + " %");

            if (percent < 80) {
                bar.setForeground(new Color(46, 125, 50));
            } else if (percent <= 100) {
                bar.setForeground(new Color(251, 192, 45));
            } else {
                bar.setForeground(new Color(198, 40, 40));
            }

            if (isSelected) {
                bar.setBackground(table.getSelectionBackground());
            } else {
                bar.setBackground(table.getBackground());
            }
            return bar;
        }

    }

}

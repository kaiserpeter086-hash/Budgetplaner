package de.euerteam.budgetplanner.ui;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import de.euerteam.budgetplanner.model.Transaction;
import de.euerteam.budgetplanner.model.TransactionType;
import de.euerteam.budgetplanner.service.TransactionService;

public class TransactionsPanel extends JPanel {
    private final TransactionService transactionService = new TransactionService();
    private final JLabel balanceLabel = new JLabel("Monatliches Guthaben: 0,00 €");

    public TransactionsPanel() {
        JButton addTestButton = new JButton("Test-Transaktion hinzufügen");
        addTestButton.addActionListener(e -> {
            Transaction t = new Transaction("Test", new BigDecimal("10.00"), TransactionType.EXPENSE, LocalDate.now(), "Essen");
            transactionService.addTransaction(t);
            YearMonth currentMonth = YearMonth.now();
            BigDecimal balance = transactionService.getMonthlyBalance(currentMonth);
            balanceLabel.setText("Monatliches Guthaben: " + balance + " €");
        });
        add(addTestButton);
        add(balanceLabel);
    }
}

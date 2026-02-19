package de.euerteam.budgetplanner.ui;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import java.awt.*;

import de.euerteam.budgetplanner.model.Transaction;
import de.euerteam.budgetplanner.model.TransactionType;
import de.euerteam.budgetplanner.service.TransactionService;

public class TransactionsPanel extends JPanel {
    private final TransactionService transactionService = new TransactionService();
    private final DefaultTableModel tableModel = new DefaultTableModel(
        new Object[]{"Beschreibung", "Betrag", "Typ", "Datum", "Kategorie"}, 0
    ){
        @Override
        public boolean isCellEditable(int row, int column) {
            return false; // Alle Zellen nicht editierbar
        }
    };

    private final JTable table = new JTable(tableModel);
    private final JLabel balanceLabel = new JLabel("Monatliches Guthaben: 0,00 €");

    private final JTextField dateField = new JTextField(10);
    private final JTextField descriptionField = new JTextField(15);
    private final JTextField amountField = new JTextField(10);
    private final JComboBox<TransactionType> typeComboBox = new JComboBox<>(TransactionType.values());
    private final JTextField categoryField = new JTextField(15);

    public TransactionsPanel() {
        setLayout(new BorderLayout());

        dateField.setText(LocalDate.now().toString());
        
    JPanel formPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        formPanel.add(new JLabel("Datum:"));
        formPanel.add(dateField);

        formPanel.add(new JLabel("Beschreibung:"));
        formPanel.add(descriptionField);

        formPanel.add(new JLabel("Betrag:"));
        formPanel.add(amountField);

        formPanel.add(new JLabel("Typ:"));
        formPanel.add(typeComboBox);

        formPanel.add(new JLabel("Kategorie:"));
        formPanel.add(categoryField);

    JButton addButton = new JButton("Transaktion hinzufügen");
    // Sichtbarer machen: feste Größe, kontrastreiche Farben und kein Fokus-Highlight
    addButton.setPreferredSize(new Dimension(180, 30));
    addButton.setFocusable(false);
    addButton.setBackground(new Color(59, 89, 182));
    addButton.setForeground(Color.WHITE);
    addButton.setOpaque(true);
    addButton.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
    formPanel.add(addButton);

    formPanel.add(Box.createHorizontalStrut(20)); // Abstand
    formPanel.add(balanceLabel);

    addButton.addActionListener(e -> onAddTransaction());

    // Initiales Guthaben anzeigen
    updateBalance();

        JScrollPane scrollPane = new JScrollPane(table);

        add(formPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        
    }

    private void onAddTransaction() {
        try {
            LocalDate date = LocalDate.parse(dateField.getText().trim());
            TransactionType type = (TransactionType) typeComboBox.getSelectedItem();
            String description = descriptionField.getText().trim();
            BigDecimal amount = new BigDecimal(amountField.getText().trim());
            String category = categoryField.getText().trim();

            Transaction t = new Transaction(description, amount, type, date, category);
            addTransactionToTable(t);
            updateBalance();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Ungültige Eingabe: " + ex.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addTransactionToTable(Transaction t) {
        tableModel.addRow(new Object[]{
            t.getDescription(),
            t.getAmount(),
            t.getType(),
            t.getDate(),
            t.getCategory()
        });
    }

    private void updateBalance() {
        YearMonth currentMonth = YearMonth.now();
        BigDecimal balance = transactionService.getMonthlyBalance(currentMonth);
        balanceLabel.setText("Monatliches Guthaben: " + balance + " €");
    }
}

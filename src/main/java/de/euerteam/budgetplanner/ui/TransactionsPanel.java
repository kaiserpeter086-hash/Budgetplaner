package de.euerteam.budgetplanner.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import de.euerteam.budgetplanner.model.CategoryType;
import de.euerteam.budgetplanner.model.Transaction;
import de.euerteam.budgetplanner.model.TransactionType;
import de.euerteam.budgetplanner.service.TransactionService;

public class TransactionsPanel extends JPanel {
    private final TransactionService transactionService = new TransactionService();
    private final DefaultTableModel tableModel = new DefaultTableModel(
        new Object[]{"Beschreibung", "Betrag", "Typ", "Datum", "Kategorie", "_OBJ_"}, 0
    ){
        @Override
        public boolean isCellEditable(int row, int column) {
            return false; // Alle Zellen nicht editierbar
        }
    };

    private final JTable table = new JTable(tableModel);
    {
        table.getColumnModel().getColumn(5).setMinWidth(0);
        table.getColumnModel().getColumn(5).setMaxWidth(0);
        table.getColumnModel().getColumn(5).setWidth(0);
    }

    private final JLabel balanceLabel = new JLabel("Monatliches Guthaben: 0,00 €");

    private final JTextField dateField = new JTextField(10);
    private final JTextField descriptionField = new JTextField(15);
    private final JFormattedTextField amountField = new JFormattedTextField(NumberFormat.getCurrencyInstance(Locale.GERMANY));
    private final JComboBox<TransactionType> typeComboBox = new JComboBox<>(TransactionType.values());
    private final JComboBox<CategoryType> categoryComboBox = new JComboBox<>(CategoryType.values());

    

    public TransactionsPanel() {
        setLayout(new BorderLayout());

        // Deutsches Datumsformat: TT.MM.JJJJ
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy").withLocale(Locale.GERMANY);
        dateField.setText(LocalDate.now().format(dateFormatter));
        // Betragsfeld konfigurieren (wird mit deutschem Währungsformat initialisiert)
        amountField.setColumns(10);
        amountField.setValue(0.00);
        amountField.setToolTipText("Betrag (z.B. 1.234,56 €)");
        dateField.setToolTipText("Datum im Format TT.MM.JJJJ");
        
        // Verwende eine vertikale Anordnung: Felder oben, Button + Guthaben darunter
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel fieldsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        fieldsPanel.add(new JLabel("Datum:"));
        fieldsPanel.add(dateField);

        fieldsPanel.add(new JLabel("Beschreibung:"));
        fieldsPanel.add(descriptionField);

        fieldsPanel.add(new JLabel("Betrag:"));
        fieldsPanel.add(amountField);

        fieldsPanel.add(new JLabel("Typ:"));
        fieldsPanel.add(typeComboBox);

    fieldsPanel.add(new JLabel("Kategorie:"));
    // Ermögliche Tippen und wähle Größe
    categoryComboBox.setEditable(true);
    categoryComboBox.setPreferredSize(new Dimension(150, 25));
    categoryComboBox.setToolTipText("Kategorie auswählen oder neuen Namen tippen");
    categoryComboBox.setMaximumRowCount(12);
    fieldsPanel.add(categoryComboBox);

        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addButton = new JButton("Transaktion hinzufügen");
        // Sichtbarer machen: feste Größe, kontrastreiche Farben und kein Fokus-Highlight
        addButton.setPreferredSize(new Dimension(180, 30));
        addButton.setFocusable(false);
        addButton.setBackground(new Color(59, 89, 182));
        addButton.setForeground(Color.WHITE);
        addButton.setOpaque(true);
        addButton.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        controlsPanel.add(addButton);
        controlsPanel.add(Box.createHorizontalStrut(20)); // Abstand
        controlsPanel.add(balanceLabel);

        addButton.addActionListener(e -> onAddTransaction());

        // Initiales Guthaben anzeigen
        updateBalance();

        formPanel.add(fieldsPanel);
        formPanel.add(Box.createVerticalStrut(8));
        formPanel.add(controlsPanel);

        JScrollPane scrollPane = new JScrollPane(table);

        add(formPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        
    }

    private void onAddTransaction() {
        try {
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy").withLocale(Locale.GERMANY);
            LocalDate date = LocalDate.parse(dateField.getText().trim(), dateFormatter);
            TransactionType type = (TransactionType) typeComboBox.getSelectedItem();
            String description = descriptionField.getText().trim();
            // Versuche, den formatierten Betrag zu parsen (z.B. "1.234,56 €")
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.GERMANY);
            BigDecimal amount;
            try {
                Number parsed = currencyFormat.parse(amountField.getText().trim());
                amount = BigDecimal.valueOf(parsed.doubleValue());
            } catch (ParseException pe) {
                // Fallback: direkte BigDecimal-Parsung (ohne Währungszeichen)
                String cleaned = amountField.getText().replaceAll("[^0-9,.-]", "").replace(',', '.').trim();
                amount = new BigDecimal(cleaned);
            }
            Object sel = categoryComboBox.getSelectedItem();
            CategoryType category;
            if (sel instanceof CategoryType) {
                category = (CategoryType) sel;
            } else if (sel instanceof String) {
                String s = ((String) sel).trim();
                CategoryType matched = null;
                for (CategoryType ct : CategoryType.values()) {
                    if (ct.name().equalsIgnoreCase(s)) {
                        matched = ct;
                        break;
                    }
                }
                if (matched == null) {
                    throw new IllegalArgumentException("Unbekannte Kategorie: '" + s + "'. Bitte wähle eine bestehende Kategorie aus.");
                }
                category = matched;
            } else {
                throw new IllegalArgumentException("Keine Kategorie ausgewählt");
            }

            Transaction t = new Transaction(description, amount, type, date, category);
            // Persist the transaction in the service so balance calculations work
            transactionService.addTransaction(t);
            addTransactionToTable(t);
            updateBalance();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Ungültige Eingabe: " + ex.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addTransactionToTable(Transaction t) {
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.GERMANY);
        String formattedAmount = currencyFormat.format(t.getAmount());
        tableModel.addRow(new Object[]{
            t.getDescription(),
            formattedAmount,
            t.getType(),
            // Datum im deutschen Format anzeigen
            t.getDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy").withLocale(Locale.GERMANY)),
            t.getCategory(),
            t
        });
    }

    private void updateBalance() {
        YearMonth currentMonth = YearMonth.now();
        BigDecimal balance = transactionService.getMonthlyBalance(currentMonth);
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.GERMANY);
        balanceLabel.setText("Monatliches Guthaben: " + currencyFormat.format(balance));
    }

    private void delteTransaction(){
        int row = table.getSelectedRow();
        if (row < 0){
            JOptionPane.showMessageDialog(this,"Bitte eine Transaktion auswählen");
            
        }
        int modelRow = table.convertRowIndexToModel(row);
        Transaction t = (Transaction) tableModel.getValueAt(modelRow, 5);

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.GERMANY);
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy").withLocale(Locale.GERMANY);

        int confirm = JOptionPane.showConfirmDialog(
            this, 
            "Möchten Sie die folgende Transaktion wirklich löschen?\n" 
            + t.getDate().format(dateTimeFormatter) + " - "
            + t.getDescription() + " - "
            + currencyFormat.format(t.getAmount()) + " - "
            + t.getCategory(),
            "Transaktion löschen",
            JOptionPane.YES_NO_OPTION
        );
        if (confirm == JOptionPane.YES_OPTION) {
            transactionService.removeTransactionById(t.getId());
            tableModel.removeRow(modelRow);
            updateBalance();
        }
    }

    private void editTransaction(){
        int row = table.getSelectedRow();
        if (row < 0){
            JOptionPane.showMessageDialog(this,"Bitte eine Transaktion auswählen");
            
        }

        int modelRow = table.convertRowIndexToModel(row);
        Transaction oldT = (Transaction) tableModel.getValueAt(modelRow, 5);

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy").withLocale(Locale.GERMANY);
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.GERMANY);

        JTextField date = new JTextField(oldT.getDate().format(dateTimeFormatter),10);
        JTextField description = new JTextField(oldT.getDescription(),15);
        JFormattedTextField amount = new JFormattedTextField(currencyFormat);
        amount.setValue(oldT.getAmount());
        JComboBox<TransactionType> type = new JComboBox<>(TransactionType.values());
        type.setSelectedItem(oldT.getType());
        JComboBox<CategoryType> category = new JComboBox<>(CategoryType.values());
        category.setEditable(true);
        category.setSelectedItem(oldT.getCategory());

        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));
        panel.add(new JLabel("Datum:"));
        panel.add(date);
        panel.add(new JLabel("Beschreibung:"));
        panel.add(description);
        panel.add(new JLabel("Betrag:"));
        panel.add(amount);
        panel.add(new JLabel("Typ:"));
        panel.add(type);
        panel.add(new JLabel("Kategorie:"));
        panel.add(category);

        int result = JOptionPane.showConfirmDialog(this, panel, "Transaktion bearbeiten", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;
        
        try {
            LocalDate newDate = LocalDate.parse(date.getText().trim(), dateTimeFormatter);
            String newDescription = description.getText().trim();
            TransactionType newType = (TransactionType) type.getSelectedItem();

            BigDecimal newAmount;
            try {
                Number parsed = currencyFormat.parse(amount.getText().trim());
                newAmount = BigDecimal.valueOf(parsed.doubleValue());
            } catch (ParseException pe) {
                String cleaned = amount.getText().replaceAll("[^0-9,.-]", "").replace(',', '.').trim();
                newAmount = new BigDecimal(cleaned);
            }

            Object sel = category.getSelectedItem();
            CategoryType newCategory;
            if (sel instanceof CategoryType) {
                newCategory = (CategoryType) sel;
            } else if (sel instanceof String) {
                String s = ((String) sel).trim();
                CategoryType matched = null;
                for (CategoryType ct : CategoryType.values()) {
                    if (ct.name().equalsIgnoreCase(s)) {
                        matched = ct;
                        break;
                    }
                }
                if (matched == null) {
                    throw new IllegalArgumentException("Unbekannte Kategorie: '" + s + "'. Bitte wähle eine bestehende Kategorie aus.");
                }
                newCategory = matched;
            } else {
                throw new IllegalArgumentException("Keine Kategorie ausgewählt");
            }
                
            Transaction updatedTransaction = new Transaction(oldT.getId(), newDescription, newAmount, newType, newDate, newCategory);
            boolean ok = transactionService.updateTransaction(updatedTransaction);
            if (!ok) {
                JOptionPane.showMessageDialog(this, "Transaktion konnte nicht aktualisiert werden.", "Fehler", JOptionPane.ERROR_MESSAGE);
            }

            tableModel.setValueAt(updatedTransaction.getDescription(), modelRow, 0);
            tableModel.setValueAt(currencyFormat.format(updatedTransaction.getAmount()), modelRow, 1);
            tableModel.setValueAt(updatedTransaction.getType(), modelRow, 2);
            tableModel.setValueAt(updatedTransaction.getDate().format(dateTimeFormatter), modelRow, 3);
            tableModel.setValueAt(updatedTransaction.getCategory(), modelRow, 4);
            tableModel.setValueAt(updatedTransaction, modelRow, 5);

            updateBalance();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Ungültige Eingabe: " + ex.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }
}

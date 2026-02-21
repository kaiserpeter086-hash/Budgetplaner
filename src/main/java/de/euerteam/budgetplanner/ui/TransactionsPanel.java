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
import java.util.List;
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
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import de.euerteam.budgetplanner.model.CategoryType;
import de.euerteam.budgetplanner.model.Transaction;
import de.euerteam.budgetplanner.model.TransactionType;
import de.euerteam.budgetplanner.service.TransactionService;

import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import de.euerteam.budgetplanner.persistence.CsvPersistence;

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

    private final JTextField searchField = new JTextField(20);
    private final TableRowSorter<DefaultTableModel> rowSorter = new TableRowSorter<>(tableModel);

    private final JTable table = new JTable(tableModel);
    {
        table.getColumnModel().getColumn(5).setMinWidth(0);
        table.getColumnModel().getColumn(5).setMaxWidth(0);
        table.getColumnModel().getColumn(5).setWidth(0);
        table.setRowSorter(rowSorter);
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

        // Buttons für Export/Import
        JButton exportButton = new JButton("Exportieren");
        JButton importButton = new JButton("Importieren");

        // Buttons für Bearbeiten/Löschen 
        JButton editButton = new JButton("Bearbeiten");
        JButton deleteButton = new JButton("Löschen");

        controlsPanel.add(addButton);
        controlsPanel.add(editButton);
        controlsPanel.add(deleteButton);
        controlsPanel.add(Box.createHorizontalStrut(15));
        controlsPanel.add(exportButton);
        controlsPanel.add(importButton);
        controlsPanel.add(Box.createHorizontalStrut(20)); // Abstand
        controlsPanel.add(balanceLabel);

        // Action Listener verknüpfen
        addButton.addActionListener(e -> onAddTransaction());
        editButton.addActionListener(e -> editTransaction());
        deleteButton.addActionListener(e -> deleteTransaction());
        exportButton.addActionListener(e -> exportData());
        importButton.addActionListener(e -> importData());

        // Initiales Guthaben anzeigen
        updateBalance();

        formPanel.add(fieldsPanel);
        formPanel.add(Box.createVerticalStrut(8));
        formPanel.add(controlsPanel);

        JScrollPane scrollPane = new JScrollPane(table);

        add(formPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        searchPanel.add(new JLabel("Suchen:"));
        searchPanel.add(searchField);

        JButton clearSearchButton = new JButton("X");
        clearSearchButton.setFocusable(false);
        searchPanel.add(clearSearchButton);
        clearSearchButton.addActionListener(e -> searchField.setText(""));

        // `formPanel` enthält bereits `fieldsPanel` und die Steuerelemente (Button + Guthaben).
        // Daher fügen wir nur das `searchPanel` im Süden hinzu und belassen `formPanel` im Norden.
        add(searchPanel, BorderLayout.SOUTH);
        
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {applySearchFilter(); }
            @Override
            public void removeUpdate (DocumentEvent e) {applySearchFilter(); }
            @Override
            public void changedUpdate (DocumentEvent e) {applySearchFilter(); }
        });

        searchField.addActionListener(e -> table.requestFocusInWindow());
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

    private void deleteTransaction(){
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

    private void applySearchFilter() {
        String text = searchField.getText();
        if (text == null || text.trim().isEmpty()) {
            rowSorter.setRowFilter(null);
            return;
        }
        
        String query = text.trim().toLowerCase();
        rowSorter.setRowFilter(new RowFilter<DefaultTableModel,Integer>() {
            @Override
            public boolean include(RowFilter.Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                for (int col = 0; col <= 4; col++) {
                    Object value = entry.getValue(col);
                    if (value != null && value.toString().toLowerCase().contains(query)) {
                        return true;
                    }
                }
                return false;
            }
        });
        updateSearchBalance();
    }

    private void updateSearchBalance() {
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.GERMANY);
        BigDecimal balance = BigDecimal.ZERO;

        for (int viewRow = 0; viewRow < table.getRowCount(); viewRow++) {
            Object amountObj = table.getValueAt(viewRow, 1);
            Object typeObj = table.getValueAt(viewRow, 2);

            BigDecimal amount;
            try {
                Number parsed = currencyFormat.parse(amountObj.toString().trim());
                amount = BigDecimal.valueOf(parsed.doubleValue());
            } catch (ParseException e) {
                String cleaned = amountObj.toString().replaceAll("[^0-9,.-]", "").replace(',', '.').trim();
                amount = new BigDecimal(cleaned);
            }

            TransactionType type = (TransactionType) typeObj;
            if (type == TransactionType.Einnahmen) {
                balance = balance.add(amount);
            } else if (type == TransactionType.Ausgaben) {
                balance = balance.subtract(amount);
            }
        }
        balanceLabel.setText("Monatliches Guthaben: " + currencyFormat.format(balance));
    }

    private void exportData() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Speicherort für CSV wählen");
        fileChooser.setFileFilter(new FileNameExtensionFilter("CSV Dateien (*.csv)", "csv"));
        
        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            String filePath = fileToSave.getAbsolutePath();
            
            // Automatisch .csv anhängen, falls vergessen
            if (!filePath.toLowerCase().endsWith(".csv")) {
                filePath += ".csv";
            }
            
            try {
                CsvPersistence.exportToCSV(transactionService.getTransactions(), filePath);
                JOptionPane.showMessageDialog(this, "Daten erfolgreich exportiert!", "Erfolg", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Fehler beim Exportieren:\n" + ex.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void importData() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("CSV-Datei zum Importieren wählen");
        fileChooser.setFileFilter(new FileNameExtensionFilter("CSV Dateien (*.csv)", "csv"));
        
        int userSelection = fileChooser.showOpenDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToOpen = fileChooser.getSelectedFile();
            
            try {
                // 1. Daten aus CSV laden
                List<Transaction> importedTransactions = CsvPersistence.importFromCSV(fileToOpen.getAbsolutePath());
                
                // 2. Service aktualisieren
                transactionService.setTransactions(importedTransactions);
                
                // 3. UI Tabelle leeren und neu befüllen
                tableModel.setRowCount(0); 
                for (Transaction t : importedTransactions) {
                    addTransactionToTable(t);
                }
                
                // 4. Guthaben aktualisieren
                updateBalance();
                
                JOptionPane.showMessageDialog(this, "Daten erfolgreich importiert!", "Erfolg", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Fehler beim Importieren:\n" + ex.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}

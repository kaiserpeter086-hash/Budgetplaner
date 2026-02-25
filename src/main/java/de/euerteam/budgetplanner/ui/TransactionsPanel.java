package de.euerteam.budgetplanner.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
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
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
 
import de.euerteam.budgetplanner.model.CategoryType;
import de.euerteam.budgetplanner.model.Transaction;
import de.euerteam.budgetplanner.model.TransactionType;
import de.euerteam.budgetplanner.persistence.CsvPersistence;
import de.euerteam.budgetplanner.service.TransactionService;

public class TransactionsPanel extends JPanel {
    private static final DateTimeFormatter TABLE_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy").withLocale(Locale.GERMANY);
    private final TransactionService transactionService;
    private final DefaultTableModel incomeTableModel = new DefaultTableModel(
        new Object[]{"Beschreibung", "Betrag", "Typ", "Datum", "Kategorie", "_OBJ_"}, 0
    ){
        @Override
        public boolean isCellEditable(int row, int column) { return false; }
    };

    private final DefaultTableModel expenseTableModel = new DefaultTableModel(
        new Object[]{"Beschreibung", "Betrag", "Typ", "Datum", "Kategorie", "_OBJ_"}, 0
    ){
        @Override
        public boolean isCellEditable(int row, int column) { return false; }
    };

    private final JTextField searchField = new JTextField(20);
    private final TableRowSorter<DefaultTableModel> incomeRowSorter = new TableRowSorter<>(incomeTableModel);
    private final TableRowSorter<DefaultTableModel> expenseRowSorter = new TableRowSorter<>(expenseTableModel);

    private final JTable incomeTable = new JTable(incomeTableModel);
    private final JTable expenseTable = new JTable(expenseTableModel);
    {
        incomeTable.setRowSorter(incomeRowSorter);
        expenseTable.setRowSorter(expenseRowSorter);

        configureDateSorting(incomeRowSorter);
        configureDateSorting(expenseRowSorter);
        // hide object column in both tables
        incomeTable.getColumnModel().getColumn(5).setMinWidth(0);
        incomeTable.getColumnModel().getColumn(5).setMaxWidth(0);
        incomeTable.getColumnModel().getColumn(5).setWidth(0);
        expenseTable.getColumnModel().getColumn(5).setMinWidth(0);
        expenseTable.getColumnModel().getColumn(5).setMaxWidth(0);
        expenseTable.getColumnModel().getColumn(5).setWidth(0);
    }

    private final JLabel balanceLabel = new JLabel("Monatliches Guthaben: 0,00 €");
    private final JTextField dateField = new JTextField(10);
    private final JTextField descriptionField = new JTextField(15);
    private final JFormattedTextField amountField = new JFormattedTextField(NumberFormat.getCurrencyInstance(Locale.GERMANY));
    private final JComboBox<TransactionType> typeComboBox = new JComboBox<>(TransactionType.values());
    private final JComboBox<CategoryType> categoryComboBox = new JComboBox<>(CategoryType.values());
    private JScrollPane incomeScroll;
    private JScrollPane expenseScroll;



    public TransactionsPanel(TransactionService transactionService) {
        this.transactionService = transactionService;
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
        formPanel.setBorder(BorderFactory.createEmptyBorder(16, 16, 12, 16));

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
        JButton addButton = new JButton("Neue Transaktion");
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

       JButton editButton = new JButton("Bearbeiten");
       editButton.setFocusable(false);
       controlsPanel.add(editButton);
       editButton.addActionListener(e -> editTransaction()); 

       JButton deleteButton = new JButton("Löschen");
       deleteButton.setFocusable(false);
       controlsPanel.add(deleteButton);
       deleteButton.addActionListener(e -> deleteTransaction());

       JButton exportButton = new JButton("Exportieren");
       exportButton.setFocusable(false);
       controlsPanel.add(exportButton);
       exportButton.addActionListener(e -> exportToCSV());

       JButton importButton = new JButton("Importieren");
       importButton.setFocusable(false);
       controlsPanel.add(importButton);
       importButton.addActionListener(e -> importFromCSV());

        addButton.addActionListener(e -> showNewTransactionDialog());

       

        // Initiales Guthaben anzeigen
        updateBalance();
        

        // Tabellen modern stylen
        styleTable(incomeTable);
        styleTable(expenseTable);

        // Betrag-Spalte formatieren (rechts + farbig)
        installAmountRenderer(incomeTable);
        installAmountRenderer(expenseTable);

        // Felder für direkte Eingabe nicht mehr direkt im Hauptpanel anzeigen.
        // Stattdessen öffnet der Button ein Dialogfenster zur Eingabe.
        formPanel.add(controlsPanel);
        
        // zwei Tabellen nebeneinander: Einnahmen | Ausgaben
        JPanel tablesPanel = new JPanel(new GridLayout(1, 2, 20, 20));
        tablesPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        incomeScroll = new JScrollPane(incomeTable);
        expenseScroll = new JScrollPane(expenseTable);

        // ScrollPane Border entfernen (wichtig!)
        incomeScroll.setBorder(null);
        expenseScroll.setBorder(null);

        // Cards erzeugen
        JPanel incomeCard = createCard("Einnahmen", new Color(0, 160, 70), incomeScroll);
        JPanel expenseCard = createCard("Ausgaben", new Color(220, 60, 60), expenseScroll);

        tablesPanel.add(incomeCard);
        tablesPanel.add(expenseCard);

        add(formPanel, BorderLayout.NORTH);
        add(tablesPanel, BorderLayout.CENTER);

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

        searchField.addActionListener(e -> incomeTable.requestFocusInWindow());
    }

    private void styleTable(JTable table) {
        table.setRowHeight(28);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);
    table.getTableHeader().setReorderingAllowed(false);
}

private void installAmountRenderer(JTable table) {
    DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
        @Override
        public Component getTableCellRendererComponent(
                JTable t, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

            Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);

            if (!isSelected) {
                Object typeObj = t.getValueAt(row, 2); // Typ-Spalte
                if (typeObj != null && typeObj.toString().equalsIgnoreCase("Einnahmen")) {
                    c.setForeground(new Color(0, 128, 0));
                } else if (typeObj != null && typeObj.toString().equalsIgnoreCase("Ausgaben")) {
                    c.setForeground(Color.RED.darker());
                } else {
                    c.setForeground(UIManager.getColor("Table.foreground"));
                }
            }

            setHorizontalAlignment(RIGHT);
            return c;
        }
    };

    // Betrag-Spalte = 1
    table.getColumnModel().getColumn(1).setCellRenderer(renderer);
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

            if (type == TransactionType.Ausgaben && category != CategoryType.Auswahl){
                YearMonth month = YearMonth.from(date);
                BigDecimal budget = transactionService.getBudgetForMonth(month, category);
                BigDecimal currentExpenses = transactionService
                        .getExpensesCategoryForMonth(month)
                        .getOrDefault(category, BigDecimal.ZERO);
                BigDecimal newExpenseTotal = currentExpenses.add(amount);

                if (budget.compareTo(BigDecimal.ZERO) > 0  && newExpenseTotal.compareTo(budget) > 0){
                    NumberFormat warningFormat = NumberFormat.getCurrencyInstance(Locale.GERMANY);
                    JOptionPane.showMessageDialog(
                        this,
                        "Achtung: Budget für " + category + " in " + month + " überschritten!\n"
                        + "Budget: " + warningFormat.format(budget) + " | Neu: " + warningFormat.format(newExpenseTotal),
                        "Budgetwarnung",
                        JOptionPane.WARNING_MESSAGE);
                }
            }

            // Persist the transaction in the service so balance calculations work
            transactionService.addTransaction(t);
            addTransactionToTable(t);
            updateBalance();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Ungültige Eingabe: " + ex.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showNewTransactionDialog() {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy").withLocale(Locale.GERMANY);
        JTextField date = new JTextField(LocalDate.now().format(dateFormatter), 10);
        JTextField description = new JTextField(15);
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.GERMANY);
        JFormattedTextField amount = new JFormattedTextField(currencyFormat);
        amount.setValue(0.00);
        JComboBox<TransactionType> type = new JComboBox<>(TransactionType.values());
        JComboBox<CategoryType> category = new JComboBox<>(CategoryType.values());
        category.setEditable(true);

        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));
        panel.add(new JLabel("Datum:")); panel.add(date);
        panel.add(new JLabel("Beschreibung:")); panel.add(description);
        panel.add(new JLabel("Betrag:")); panel.add(amount);
        panel.add(new JLabel("Typ:")); panel.add(type);
        panel.add(new JLabel("Kategorie:")); panel.add(category);

        // recurrence holder
        final boolean[] isRecurring = new boolean[] { false };
        final String[] recurFreq = new String[] { "MONTHLY" };
        final LocalDate[] recurEndDate = new LocalDate[] { null };
        final Integer[] recurOccurrences = new Integer[] { null };

        JButton recurButton = new JButton("Dauerauftrag konfigurieren");
        recurButton.addActionListener(ev -> {
            JPanel rpanel = new JPanel(new GridLayout(0,2,6,6));
            JComboBox<String> freqBox = new JComboBox<>(new String[]{"Monatlich","Jährlich"});
            JTextField endDateField = new JTextField(10);
            JTextField occField = new JTextField(5);
            rpanel.add(new JLabel("Intervall:")); rpanel.add(freqBox);
            rpanel.add(new JLabel("Enddatum (TT.MM.JJJJ) - optional:")); rpanel.add(endDateField);
            rpanel.add(new JLabel("Anzahl Wiederholungen - optional:")); rpanel.add(occField);

            int rr = JOptionPane.showConfirmDialog(this, rpanel, "Dauerauftrag", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (rr != JOptionPane.OK_OPTION) return;
            isRecurring[0] = true;
            recurFreq[0] = freqBox.getSelectedItem().toString().equals("Monatlich") ? "MONTHLY" : "YEARLY";
            String ed = endDateField.getText().trim();
            if (!ed.isEmpty()) {
                try {
                    recurEndDate[0] = LocalDate.parse(ed, dateFormatter);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Ungültiges Enddatum: " + ex.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
                    recurEndDate[0] = null;
                }
            }
            String oc = occField.getText().trim();
            if (!oc.isEmpty()) {
                try { recurOccurrences[0] = Integer.parseInt(oc); } catch (NumberFormatException nfe) { recurOccurrences[0] = null; }
            }
        });

        panel.add(new JLabel("")); panel.add(recurButton);

        int result = JOptionPane.showConfirmDialog(this, panel, "Neue Transaktion", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        try {
            LocalDate parsedDate = LocalDate.parse(date.getText().trim(), dateFormatter);
            String desc = description.getText().trim();
            TransactionType ttype = (TransactionType) type.getSelectedItem();

            BigDecimal parsedAmount;
            try {
                Number parsed = currencyFormat.parse(amount.getText().trim());
                parsedAmount = BigDecimal.valueOf(parsed.doubleValue());
            } catch (ParseException pe) {
                String cleaned = amount.getText().replaceAll("[^0-9,.-]", "").replace(',', '.').trim();
                parsedAmount = new BigDecimal(cleaned);
            }

            Object sel = category.getSelectedItem();
            CategoryType cat;
            if (sel instanceof CategoryType) {
                cat = (CategoryType) sel;
            } else if (sel instanceof String) {
                String s = ((String) sel).trim();
                CategoryType matched = null;
                for (CategoryType ct : CategoryType.values()) {
                    if (ct.name().equalsIgnoreCase(s)) { matched = ct; break; }
                }
                if (matched == null) throw new IllegalArgumentException("Unbekannte Kategorie: '" + s + "'. Bitte wähle eine bestehende Kategorie aus.");
                cat = matched;
            } else {
                throw new IllegalArgumentException("Keine Kategorie ausgewählt");
            }

            // create first transaction
            Transaction newT = new Transaction(desc, parsedAmount, ttype, parsedDate, cat);
            transactionService.addTransaction(newT);
            addTransactionToTable(newT);

            // if recurring: generate follow-ups
            if (isRecurring[0]) {
                LocalDate current = parsedDate;
                int generated = 0;
                while (true) {
                    // advance
                    if (recurFreq[0].equals("MONTHLY")) current = current.plusMonths(1);
                    else current = current.plusYears(1);

                    if (recurEndDate[0] != null && current.isAfter(recurEndDate[0])) break;
                    if (recurOccurrences[0] != null && generated >= recurOccurrences[0]) break;

                    Transaction t = new Transaction(desc, parsedAmount, ttype, current, cat);
                    transactionService.addTransaction(t);
                    addTransactionToTable(t);
                    generated++;
                }
            }

            updateBalance();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Ungültige Eingabe: " + ex.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addTransactionToTable(Transaction t) {
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.GERMANY);
        String formattedAmount = currencyFormat.format(t.getAmount());
        Object[] row = new Object[]{
            t.getDescription(),
            formattedAmount,
            t.getType(),
            // Datum im deutschen Format anzeigen
            t.getDate().format(TABLE_DATE_FORMATTER),
            t.getCategory(),
            t
        };
        if (t.getType() == TransactionType.Einnahmen) {
            incomeTableModel.addRow(row);
            incomeRowSorter.sort();
        } else {
            expenseTableModel.addRow(row);
            expenseRowSorter.sort();
        }
    }

    private void updateBalance() {
        YearMonth currentMonth = YearMonth.now();
        BigDecimal balance = transactionService.getMonthlyBalance(currentMonth);
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.GERMANY);
        balanceLabel.setText("Monatliches Guthaben: " + currencyFormat.format(balance));
        updateBalanceColor(balance);
    }

    private void deleteTransaction(){
        int viewRow = incomeTable.getSelectedRow();
        JTable activeTable = null;
        DefaultTableModel activeModel = null;
        if (viewRow >= 0) {
            activeTable = incomeTable;
            activeModel = incomeTableModel;
        } else {
            viewRow = expenseTable.getSelectedRow();
            if (viewRow >= 0) {
                activeTable = expenseTable;
                activeModel = expenseTableModel;
            }
        }
        if (activeTable == null) {
            JOptionPane.showMessageDialog(this,"Bitte eine Transaktion auswählen");
            return;
        }

        int modelRow = activeTable.convertRowIndexToModel(viewRow);
        Transaction t = (Transaction) activeModel.getValueAt(modelRow, 5);

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.GERMANY);
        DateTimeFormatter dateTimeFormatter = TABLE_DATE_FORMATTER;
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
            activeModel.removeRow(modelRow);
            updateBalance();
        }
    }

    private void editTransaction(){
        int viewRow = incomeTable.getSelectedRow();
        JTable activeTable = null;
        DefaultTableModel activeModel = null;
        if (viewRow >= 0) {
            activeTable = incomeTable;
            activeModel = incomeTableModel;
        } else {
            viewRow = expenseTable.getSelectedRow();
            if (viewRow >= 0) {
                activeTable = expenseTable;
                activeModel = expenseTableModel;
            }
        }
        if (activeTable == null) {
            JOptionPane.showMessageDialog(this,"Bitte eine Transaktion auswählen");
            return;
        }

        int modelRow = activeTable.convertRowIndexToModel(viewRow);
        Transaction oldT = (Transaction) activeModel.getValueAt(modelRow, 5);

        DateTimeFormatter dateTimeFormatter = TABLE_DATE_FORMATTER;
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

            // Wenn sich der Typ nicht geändert hat, aktualisiere die Zeile inplace.
            if (updatedTransaction.getType() == oldT.getType()) {
                activeModel.setValueAt(updatedTransaction.getDescription(), modelRow, 0);
                activeModel.setValueAt(currencyFormat.format(updatedTransaction.getAmount()), modelRow, 1);
                activeModel.setValueAt(updatedTransaction.getType(), modelRow, 2);
                activeModel.setValueAt(updatedTransaction.getDate().format(dateTimeFormatter), modelRow, 3);
                activeModel.setValueAt(updatedTransaction.getCategory(), modelRow, 4);
                activeModel.setValueAt(updatedTransaction, modelRow, 5);

                if (activeTable == incomeTable) incomeRowSorter.sort();
                else expenseRowSorter.sort();
            } else {
                // Typ hat sich geändert: entferne aus altem Modell und füge in das andere ein.
                activeModel.removeRow(modelRow);
                addTransactionToTable(updatedTransaction);
            }

            updateBalance();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Ungültige Eingabe: " + ex.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void applySearchFilter() {
        String text = searchField.getText();
        if (text == null || text.trim().isEmpty()) {
            incomeRowSorter.setRowFilter(null);
            expenseRowSorter.setRowFilter(null);
            return;
        }
        
        String query = text.trim().toLowerCase();
        RowFilter<DefaultTableModel,Integer> rf = new RowFilter<DefaultTableModel,Integer>() {
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
        };
        incomeRowSorter.setRowFilter(rf);
        expenseRowSorter.setRowFilter(rf);
        updateSearchBalance();
    }

    private void updateSearchBalance() {
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.GERMANY);
        BigDecimal balance = BigDecimal.ZERO;
        // sum visible rows in both tables
        JTable[] tables = new JTable[]{incomeTable, expenseTable};
        for (JTable t : tables) {
            for (int viewRow = 0; viewRow < t.getRowCount(); viewRow++) {
                Object amountObj = t.getValueAt(viewRow, 1);
                Object typeObj = t.getValueAt(viewRow, 2);

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
        }
        balanceLabel.setText("Monatliches Guthaben: " + currencyFormat.format(balance));
        updateBalanceColor(balance);
    }

    private void updateBalanceColor(BigDecimal balance) {
        if (balance.compareTo(BigDecimal.ZERO) > 0) {
            balanceLabel.setForeground(new Color(0, 128, 0));
        } else if (balance.compareTo(BigDecimal.ZERO) < 0) {
            balanceLabel.setForeground(Color.RED);
        } else {
            balanceLabel.setForeground(Color.BLACK);
        }
    }

    private void exportToCSV() {
        try {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("CSV-Datei exportieren");
            fileChooser.setFileFilter(new FileNameExtensionFilter("CSV Dateien (*.csv)", "csv"));
            fileChooser.setSelectedFile(new java.io.File("Transaktionen.csv"));

            int returnValue = fileChooser.showSaveDialog(this);
            if (returnValue != JFileChooser.APPROVE_OPTION) {
                return;
            }

            java.io.File selectedFile = fileChooser.getSelectedFile();
            String filePath = selectedFile.getAbsolutePath();

            // Stelle sicher, dass die Dateiendung ".csv" hat
            if (!filePath.toLowerCase().endsWith(".csv")) {
                filePath += ".csv";
            }

            CsvPersistence.exportToCSV(transactionService.getTransactions(), filePath);
            JOptionPane.showMessageDialog(this, "Transaktionen erfolgreich exportiert nach:\n" + filePath, "Erfolg", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Fehler beim Export: " + ex.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void importFromCSV() {
        try {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("CSV-Datei importieren");
            fileChooser.setFileFilter(new FileNameExtensionFilter("CSV Dateien (*.csv)", "csv"));

            int returnValue = fileChooser.showOpenDialog(this);
            if (returnValue != JFileChooser.APPROVE_OPTION) {
                return;
            }

            java.io.File selectedFile = fileChooser.getSelectedFile();
            String filePath = selectedFile.getAbsolutePath();

            java.util.List<Transaction> importedTransactions = CsvPersistence.importFromCSV(filePath);

            // Frage ob vorhandene Transaktionen gelöscht werden sollen
            int confirm = JOptionPane.showConfirmDialog(
                this,
                "Möchten Sie die vorhandenen Transaktionen ersetzen oder die importierten Transaktionen hinzufügen?\n\n" +
                "Ja = Ersetzen, Nein = Hinzufügen",
                "Transaktionen importieren",
                JOptionPane.YES_NO_CANCEL_OPTION
            );

            if (confirm == JOptionPane.CANCEL_OPTION) {
                return;
            }

            if (confirm == JOptionPane.YES_OPTION) {
                // Ersetze alle Transaktionen
                transactionService.setTransactions(importedTransactions);
                incomeTableModel.setRowCount(0);
                expenseTableModel.setRowCount(0);
            } else {
                // Füge importierte Transaktionen hinzu
                for (Transaction t : importedTransactions) {
                    transactionService.addTransaction(t);
                }
            }

            // Zeige alle importierten Transaktionen in den Tabellen
            for (Transaction t : importedTransactions) {
                addTransactionToTable(t);
            }

            updateBalance();
            JOptionPane.showMessageDialog(this, "Erfolgreich " + importedTransactions.size() + " Transaktionen importiert", "Erfolg", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Fehler beim Import: " + ex.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void configureDateSorting(TableRowSorter<? extends TableModel> sorter){
        sorter.setComparator(3, (left, right) -> {
            LocalDate firstDate = LocalDate.parse(left.toString(), TABLE_DATE_FORMATTER);
            LocalDate secondDate = LocalDate.parse(right.toString(), TABLE_DATE_FORMATTER);
            return firstDate.compareTo(secondDate);
        });
        sorter.setSortKeys(List.of(new javax.swing.RowSorter.SortKey(3, javax.swing.SortOrder.ASCENDING)));
        sorter.setSortsOnUpdates(true);
    }

private JPanel createCard(String title, Color accentColor, JScrollPane content) {

    JPanel card = new JPanel(new BorderLayout());
    card.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    // Innenbereich (weiße / dunkle Fläche)
    JPanel inner = new JPanel(new BorderLayout());
    inner.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(accentColor, 3), // Farb-Akzent
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
    ));

    // Titel
    JLabel titleLabel = new JLabel(title);
    titleLabel.setFont(titleLabel.getFont().deriveFont(14f));
    titleLabel.setForeground(accentColor);
    titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));

    inner.add(titleLabel, BorderLayout.NORTH);
    inner.add(content, BorderLayout.CENTER);

    card.add(inner, BorderLayout.CENTER);
    card.setBackground(UIManager.getColor("Panel.background"));

    return card;
}
}

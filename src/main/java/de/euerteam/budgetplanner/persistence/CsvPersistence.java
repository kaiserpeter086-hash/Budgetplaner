package de.euerteam.budgetplanner.persistence;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import de.euerteam.budgetplanner.model.CategoryType;
import de.euerteam.budgetplanner.model.Transaction;
import de.euerteam.budgetplanner.model.TransactionType;

public class CsvPersistence {
    private static final String DELIMITER = ";";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    
    // CSV Header
    private static final String HEADER = "ID;Description;Amount;Type;Date;Category";

    /**
     *  Exportiert Transaktionen in eine CSV-Datei
     */
    public static void exportToCSV(List<Transaction> transactions, String filePath) throws IOException {
        try (PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(
                        new FileOutputStream(filePath),
                        StandardCharsets.UTF_8))) {
            
            // Schreibe Header
            writer.println(HEADER);
            
            // Schreibe Transaktionen
            for (Transaction transaction : transactions) {
                String line = formatTransactionForCSV(transaction);
                writer.println(line);
            }
        }
    }

    /**
     * Importiert Transaktionen aus einer CSV-Datei
     */
    public static List<Transaction> importFromCSV(String filePath) throws IOException {
        List<Transaction> transactions = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        new FileInputStream(filePath),
                        StandardCharsets.UTF_8))) {
            
            String line;
            boolean isFirstLine = true;
            
            while ((line = reader.readLine()) != null) {
                // Überspringe Header
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }
                
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                try {
                    Transaction transaction = parseTransactionFromCSV(line);
                    transactions.add(transaction);
                } catch (Exception e) {
                    System.err.println("Fehler beim Parsing der Zeile: " + line);
                    System.err.println("Grund: " + e.getMessage());
                }
            }
        }
        
        return transactions;
    }

    /**
     * Formatiert eine Transaktion als CSV-Zeile
     */
    private static String formatTransactionForCSV(Transaction transaction) {
        return String.format("%s;%s;%s;%s;%s;%s",
                transaction.getId(),
                escapeCSVField(transaction.getDescription()),
                transaction.getAmount(),
                transaction.getType(),
                transaction.getDate().format(DATE_FORMATTER),
                transaction.getCategory()
        );
    }

    /**
     * Parst eine CSV-Zeile zu einer Transaktion
     */
    private static Transaction parseTransactionFromCSV(String line) {
        String[] parts = parseCSVLine(line);
        
        if (parts.length < 6) {
            throw new IllegalArgumentException("Ungültige CSV-Zeile: Zu wenige Felder");
        }
        
        UUID id = UUID.fromString(parts[0].trim());
        String description = parts[1].trim();
        BigDecimal amount = new BigDecimal(parts[2].trim());
        TransactionType type = TransactionType.valueOf(parts[3].trim());
        LocalDate date = LocalDate.parse(parts[4].trim(), DATE_FORMATTER);
        CategoryType category = CategoryType.valueOf(parts[5].trim());
        
        return new Transaction(id, description, amount, type, date, category);
    }

    /**
     * Parst eine CSV-Zeile unter Beachtung von Anführungszeichen
     */
    private static String[] parseCSVLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ';' && !inQuotes) {
                fields.add(field.toString());
                field = new StringBuilder();
            } else {
                field.append(c);
            }
        }
        
        fields.add(field.toString());
        return fields.toArray(new String[0]);
    }

    /**
     * Escaped CSV-Felder mit Semikolon oder Anführungszeichen
     */
    private static String escapeCSVField(String field) {
        if (field.contains(";") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }
}


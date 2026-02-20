package de.euerteam.budgetplanner.service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import de.euerteam.budgetplanner.model.CategoryType;
import de.euerteam.budgetplanner.model.Transaction;
import de.euerteam.budgetplanner.model.TransactionType;

public class TransactionService {
    private List<Transaction> transactions = new ArrayList<>();

    public void addTransaction(Transaction transaction) {
        transactions.add(transaction);
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public BigDecimal getMonthlyBalance(YearMonth month) {
        BigDecimal balance = BigDecimal.ZERO;
        for (Transaction t : transactions) {
            if (YearMonth.from(t.getDate()).equals(month)) {
                if (t.getType() == TransactionType.Einnahmen) {
                    balance = balance.add(t.getAmount());
                } else {
                    balance = balance.subtract(t.getAmount());
                }
            }
        }
        return balance; 
    }

    public boolean removeTransactionById(java.util.UUID id) {
        return transactions.removeIf(t -> t.getId().equals(id));
    }

    public boolean updateTransaction(Transaction updatedTransaction) {
        for (int i = 0; i < transactions.size(); i++) {
            if (transactions.get(i).getId().equals(updatedTransaction.getId())) {
                transactions.set(i, updatedTransaction);
                return true;
            }
        }
        return false;
    }

    public List<Transaction> filterByCategory(CategoryType category) {
        return transactions.stream()
                .filter(t -> t.getCategory() == category)
                .toList();
    }

    public List<Transaction> filterByType(TransactionType type) {
        return transactions.stream()
                .filter(t -> t.getType() == type)
                .toList();
    }

    public List<Transaction> searchTransactions(String description) {
        return transactions.stream()
                .filter(t -> t.getDescription().toLowerCase().contains(description.toLowerCase()))
                .toList();
    }
}

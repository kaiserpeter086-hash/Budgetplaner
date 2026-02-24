package de.euerteam.budgetplanner.service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import de.euerteam.budgetplanner.model.CategoryType;
import de.euerteam.budgetplanner.model.Transaction;
import de.euerteam.budgetplanner.model.TransactionType;

public class TransactionService {
    private final List<Transaction> transactions = new ArrayList<>();
    private final Map<YearMonth, Map<CategoryType, BigDecimal>> monthlyBudgets = new HashMap<>();

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

    public Map<CategoryType, BigDecimal> getExpensesCategoryForMonth(YearMonth month){
        Map<CategoryType, BigDecimal> expensesByCategory = new EnumMap<>(CategoryType.class);
        for (Transaction t : transactions) {
            if(t.getType() != TransactionType.Ausgaben) continue;
            if (!YearMonth.from(t.getDate()).equals(month)) continue;
            if (t.getCategory() == null || t.getCategory() == CategoryType.Auswahl) continue;

            BigDecimal current = expensesByCategory.getOrDefault(t.getCategory(), BigDecimal.ZERO);
            expensesByCategory.put(t.getCategory(), current.add(t.getAmount()));
        }
        return expensesByCategory;
    }

    public void setMonthlyBudget(YearMonth month, CategoryType category, BigDecimal budget) {
        if (month == null || category == null || category == CategoryType.Auswahl) {
            return;
        }
        Map<CategoryType, BigDecimal> monthBudgets = monthlyBudgets.computeIfAbsent(month, m -> new EnumMap<>(CategoryType.class));
        BigDecimal normalized = budget == null ? BigDecimal.ZERO : budget.max(BigDecimal.ZERO);
        monthBudgets.put(category, normalized);
    }

    public BigDecimal getBudgetForMonth(YearMonth month, CategoryType category) {
        if (month == null || category == null || category == CategoryType.Auswahl) {
            return BigDecimal.ZERO;
        }
        return monthlyBudgets
                .getOrDefault(month, Map.of())
                .getOrDefault(category, BigDecimal.ZERO);
    }

    public Map<CategoryType, BigDecimal> getBudgetsForMonth(YearMonth month){
        Map<CategoryType, BigDecimal> budgets = new EnumMap<>(CategoryType.class);
        budgets.putAll(monthlyBudgets.getOrDefault(month, Map.of()));
        return budgets;
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
    
    // Neue Methode für den Import
    public void setTransactions(List<Transaction> newTransactions) {
        this.transactions.clear();
        this.transactions.addAll(newTransactions);
    }
}


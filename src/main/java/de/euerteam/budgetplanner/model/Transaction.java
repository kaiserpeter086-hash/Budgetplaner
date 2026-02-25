package de.euerteam.budgetplanner.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class Transaction {
    private final UUID id;
    private final String description;
    private final BigDecimal amount;
    private final TransactionType type;
    private final LocalDate date;
    private final String category;

    public Transaction(String description, BigDecimal amount, TransactionType type, LocalDate date, String category) {
        this.id = UUID.randomUUID();
        this.description = description;
        this.amount = amount;
        this.type = type;
        this.date = date;
        this.category = category;
    }

    public Transaction(UUID id, String description, BigDecimal amount, TransactionType type, LocalDate date, String category) {
        this.id = id;
        this.description = description;
        this.amount = amount;
        this.type = type;
        this.date = date;
        this.category = category;
    }

    public UUID getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public TransactionType getType() {
        return type;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getCategory() {
        return category;
    }
}

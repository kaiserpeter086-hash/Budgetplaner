package de.euerteam.budgetplanner.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Verwaltet benutzerdefinierte Kategorien dynamisch.
 * Unterstützt Hinzufügen, Löschen und Listener-Benachrichtigung.
 */
public class CategoryManager {
    private final List<String> categories = new ArrayList<>();
    private final List<Runnable> listeners = new ArrayList<>();

    public CategoryManager() {
        // Standard-Kategorien
        categories.add("Lebensmittel");
        categories.add("Miete");
        categories.add("Abos");
        categories.add("Transport");
        categories.add("Unterhaltung");
        categories.add("Gesundheit");
        categories.add("Bildung");
        categories.add("Shopping");
    }

    public List<String> getCategories() {
        return Collections.unmodifiableList(categories);
    }

    public void addCategory(String name) {
        if (name == null || name.isBlank()) return;
        String trimmed = name.trim();
        // Duplikate vermeiden (case-insensitive)
        for (String cat : categories) {
            if (cat.equalsIgnoreCase(trimmed)) return;
        }
        categories.add(trimmed);
        Collections.sort(categories);
        fireListeners();
    }

    public void removeCategory(String name) {
        if (name == null) return;
        categories.removeIf(c -> c.equals(name));
        fireListeners();
    }

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    private void fireListeners() {
        for (Runnable r : listeners) {
            r.run();
        }
    }
}

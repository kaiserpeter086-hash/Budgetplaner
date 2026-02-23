package de.euerteam.budgetplanner.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

import de.euerteam.budgetplanner.model.Transaction;
import de.euerteam.budgetplanner.model.TransactionType;
import de.euerteam.budgetplanner.service.TransactionService;

public class StatisticPanel extends JPanel {

    private final TransactionService transactionService;
    private final JTabbedPane chartsTabs = new JTabbedPane();

    public StatisticPanel(TransactionService transactionService) {
        this.transactionService = Objects.requireNonNull(transactionService);

        setLayout(new BorderLayout());

        JButton refreshButton = new JButton("Aktualisieren");
        refreshButton.addActionListener(e -> refreshCharts());

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topPanel.add(refreshButton);

        add(topPanel, BorderLayout.NORTH);
        add(chartsTabs, BorderLayout.CENTER);
        refreshCharts();

    }

    public final void refreshCharts() {
        chartsTabs.removeAll();
        
        List<Transaction> transactions = loadTransactions();

        chartsTabs.addTab("Balken", createBarChart(transactions));
        chartsTabs.addTab("Kreis", createPieChart(transactions));
        chartsTabs.addTab("Linien", createLineChart(transactions));

        revalidate();
        repaint();
    }

    private List<Transaction> loadTransactions() {
        try {
            List<Transaction> transactions = transactionService.getTransactions();
            if (transactions == null) return Collections.emptyList();
            return new ArrayList<>(transactions);
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    private JComponent createBarChart(List<Transaction> transactions) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        Map<String, BigDecimal> expensesByCategory = transactions.stream()
            .filter(t -> t != null && t.getType() == TransactionType.Ausgaben)
            .collect(Collectors.groupingBy(
                t -> t.getCategory() != null ? t.getCategory().name() : "Unbekannt",
                Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
            ));
        if (expensesByCategory.isEmpty()) {
            return emptyState("Keine Ausgaben vorhanden");
        }

        expensesByCategory.entrySet().stream()
            .filter(e -> e.getValue() != null && e.getValue().compareTo(BigDecimal.ZERO) > 0)
            .sorted((a,b) -> b.getValue().compareTo(a.getValue()))
            .forEach(e -> dataset.addValue(e.getValue(), "Ausgaben", e.getKey()));

        if (dataset.getColumnCount() == 0) {
            return emptyState("Keine positiven Ausgabenwerte vorhanden");
        }

        JFreeChart chart = ChartFactory.createBarChart(
            "Ausgaben nach Kategorie",
            "Kategorie",
            "Betrag (€)",
            dataset
        );

        CategoryPlot plot = chart.getCategoryPlot();
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setShadowVisible(false);
        renderer.setDrawBarOutline(false);

        return wrapChart(chart);
    }

    private JComponent createPieChart(List<Transaction> transactions) {
        DefaultPieDataset dataset = new DefaultPieDataset();

        Map<String, BigDecimal> expensesByCategory = transactions.stream()
                .filter(t -> t != null && t.getType() == TransactionType.Ausgaben)
                .collect(Collectors.groupingBy(
                        t -> (t.getCategory() != null ? t.getCategory().name() : "UNBEKANNT"),
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ));

        if (expensesByCategory.isEmpty()) {
            return emptyState("Keine Ausgaben-Daten vorhanden.");
        }

        expensesByCategory.forEach((cat, amount) -> {
            if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
                dataset.setValue(cat, amount.doubleValue());
            }
        });

        if (dataset.getItemCount() == 0) {
            return emptyState("Keine positiven Ausgabenwerte für die Torte vorhanden.");
        }

        JFreeChart chart = ChartFactory.createPieChart(
                "Ausgabenverteilung",
                dataset,
                true,
                true,
                false
        );

        return wrapChart(chart);
    }

    // ----------------------------
    // Liniendiagramm: Kontoverlauf (Saldo über Zeit)
    // ----------------------------
    private JComponent createLineChart(List<Transaction> transactions) {
        List<Transaction> sorted = transactions.stream()
                .filter(Objects::nonNull)
                .filter(t -> t.getDate() != null)
                .sorted(Comparator.comparing(Transaction::getDate))
            .collect(Collectors.toList());

        if (sorted.isEmpty()) {
            return emptyState("Keine Datumswerte vorhanden für den Verlauf.");
        }

        TimeSeries series = new TimeSeries("Kontostand");

        BigDecimal balance = BigDecimal.ZERO;

        // Transaktionen chronologisch durchlaufen und Saldo aufbauen
        for (Transaction t : sorted) {
            LocalDate date = t.getDate();
            BigDecimal amount = safeAmount(t.getAmount());

            // Annahme: amount ist positiv. Income add, Expense subtract.
            if (t.getType() == TransactionType.Einnahmen) {
                balance = balance.add(amount);
            } else if (t.getType() == TransactionType.Ausgaben) {
                balance = balance.subtract(amount);
            }

            Day day = new Day(date.getDayOfMonth(), date.getMonthValue(), date.getYear());
            series.addOrUpdate(day, toDouble(balance));
        }

        if (series.getItemCount() == 0) {
            return emptyState("Keine Datenpunkte für den Kontostand.");
        }

        TimeSeriesCollection dataset = new TimeSeriesCollection(series);

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                "Kontoverlauf",
                "Datum",
                "Saldo (€)",
                dataset,
                true,
                true,
                false
        );

        XYPlot plot = chart.getXYPlot();
        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();

        renderer.setDefaultShapesVisible(true);
        renderer.setDefaultLinesVisible(true);


        return wrapChart(chart);
    }

    // ----------------------------
    // Helpers
    // ----------------------------
    private JComponent wrapChart(JFreeChart chart) {
        ChartPanel panel = new ChartPanel(chart);
        panel.setMouseWheelEnabled(true);
        panel.setDomainZoomable(true);
        panel.setRangeZoomable(true);
        return panel;
    }

    private JComponent emptyState(String message) {
        JPanel p = new JPanel(new GridBagLayout());
        JLabel label = new JLabel(message);
        label.setForeground(Color.DARK_GRAY);
        p.add(label);
        return p;
    }

    private BigDecimal safeAmount(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private double toDouble(BigDecimal value) {
        if (value == null) return 0.0;
        return value.doubleValue();
    }
}




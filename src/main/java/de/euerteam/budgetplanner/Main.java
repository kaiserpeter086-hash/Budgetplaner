package de.euerteam.budgetplanner;

import javax.swing.SwingUtilities;
import de.euerteam.budgetplanner.ui.MainFrame;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new MainFrame().setVisible(true);
        });
    }
}

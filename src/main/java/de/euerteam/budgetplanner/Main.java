package de.euerteam.budgetplanner;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Font;
import com.formdev.flatlaf.FlatLightLaf;
// optional später: import com.formdev.flatlaf.FlatDarkLaf;

import de.euerteam.budgetplanner.ui.MainFrame;

public class Main {

    public static void main(String[] args) {

        try {
            FlatLightLaf.setup();

            UIManager.put("Button.arc", 12);
            UIManager.put("Component.arc", 12);
            UIManager.put("TextComponent.arc", 12);
            UIManager.put("ScrollBar.width", 12);

            Font uiFont = new Font("SansSerif", Font.PLAIN, 13);
            UIManager.put("defaultFont", uiFont);
            UIManager.put("TableHeader.font", uiFont.deriveFont(Font.BOLD, 13f));

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            new MainFrame().setVisible(true);
        });
    }
}
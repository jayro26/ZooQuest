package main;

import java.awt.Component;
import javax.swing.JOptionPane;

// Small message/choice dialog helper used by the game screens.
public final class GameDialog {
    private GameDialog() { }

    public static void show(Component parent, String title, String message) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    // Shows custom options and returns the selected option index, or -1 when closed.
    public static int show(Component parent, String title, String message, String... options) {
        if (options == null || options.length == 0) {
            show(parent, title, message);
            return -1;
        }
        return JOptionPane.showOptionDialog(
                parent,
                message,
                title,
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);
    }
}

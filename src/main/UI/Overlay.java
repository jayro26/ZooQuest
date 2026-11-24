package main.UI;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;

/** A dimmed layer over the current screen with a card in the middle (the pop-ups of the layout PDF). */
public final class Overlay {
    private Overlay() {}

    /** Builds the layer. A click outside the content calls onOutsideClick (may be null). */
    public static JPanel build(JComponent content, Runnable onOutsideClick) {
        JPanel p = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(new Color(0, 0, 0, 150));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        p.setOpaque(false);
        p.add(content);
        p.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (onOutsideClick != null && !content.getBounds().contains(e.getPoint())) onOutsideClick.run();
            }
        });
        return p;
    }

    /** Shows the content over the window (glass pane). */
    public static void show(ZooQuestGUI app, JComponent content, Runnable onOutsideClick) {
        JPanel p = build(content, onOutsideClick);
        app.setGlassPane(p);
        p.setVisible(true);
        p.revalidate();
        p.repaint();
    }

    public static void hide(ZooQuestGUI app) {
        Component g = app.getGlassPane();
        if (g != null) g.setVisible(false);
    }
}

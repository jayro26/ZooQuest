package main.UI;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import main.*;

/** Shared Swing and Java2D helpers used throughout the ZooQuest screens. */
final class GuiKit {
    public static final Color GREEN = new Color(46, 150, 80);
    public static final Color ORANGE = new Color(200, 140, 30);
    public static final Color BLUE = new Color(50, 120, 190);
    public static final Color RED = new Color(170, 60, 60);
    public static final Color GREY = new Color(90, 90, 90);
    public static final Color SOFT_WHITE = new Color(235, 245, 235);

    private GuiKit() { }

    public static void quality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    public static JPanel transparent(LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.setOpaque(false);
        return p;
    }

    public static JLabel label(String text, int fontSize, boolean bold, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, fontSize));
        label.setForeground(color);
        return label;
    }

    public static List<String> wrap(String text, FontMetrics fm, int width) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) return lines;
        int maxWidth = Math.max(1, width);
        for (String paragraph : text.split("\\n", -1)) {
            StringBuilder current = new StringBuilder();
            for (String word : paragraph.trim().split("\\s+")) {
                if (word.isEmpty()) continue;
                String candidate = current.length() == 0 ? word : current + " " + word;
                if (fm.stringWidth(candidate) > maxWidth && current.length() > 0) {
                    lines.add(current.toString());
                    current.setLength(0);
                    current.append(word);
                } else {
                    current.setLength(0);
                    current.append(candidate);
                }
            }
            lines.add(current.toString());
        }
        return lines;
    }

    public static String friendly(Achievement achievement) {
        if (achievement == null || achievement.getBadgeName() == null) return "Achievement";
        String name = achievement.getBadgeName();
        if (name.endsWith("_no_clue_badge")) {
            return name.substring(0, name.length() - "_no_clue_badge".length()) + " (no clue)";
        }
        if (name.endsWith("_badge")) {
            return name.substring(0, name.length() - "_badge".length()) + " - Clear badge";
        }
        return name;
    }

    /** Translucent dark plate used behind text over an image. */
    public static void plate(Graphics2D g, int x, int y, int w, int h, int arc) {
        g.setColor(new Color(0, 0, 0, 105));
        g.fillRoundRect(x, y, w, h, arc, arc);
    }

    /** Filled five-point star centred at (cx, cy). */
    public static void star(Graphics2D g, int cx, int cy, int radius, Color color) {
        int[] xs = new int[10];
        int[] ys = new int[10];
        for (int i = 0; i < 10; i++) {
            double angle = -Math.PI / 2 + i * Math.PI / 5;
            double r = (i % 2 == 0) ? radius : radius * 0.45;
            xs[i] = (int) Math.round(cx + Math.cos(angle) * r);
            ys[i] = (int) Math.round(cy + Math.sin(angle) * r + radius * 0.08);
        }
        g.setColor(color);
        g.fillPolygon(xs, ys, 10);
    }

    /** Draws a simple padlock centred at (cx, cy). */
    public static void padlock(Graphics2D g, int cx, int cy, int size) {
        int s = Math.max(12, size);
        int bodyW = (int) (s * 0.62);
        int bodyH = (int) (s * 0.48);
        g.setStroke(new BasicStroke(Math.max(2f, s / 9f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(230, 230, 230));
        g.drawArc(cx - bodyW / 3, cy - s / 2, bodyW * 2 / 3, (int) (s * 0.7), 0, 180);
        g.setColor(new Color(230, 190, 80));
        g.fillRoundRect(cx - bodyW / 2, cy - bodyH / 6, bodyW, bodyH, 8, 8);
        g.setColor(new Color(40, 40, 40));
        g.fillOval(cx - s / 14, cy + bodyH / 8, Math.max(2, s / 7), Math.max(2, s / 7));
        g.fillRect(cx - s / 40, cy + bodyH / 4, Math.max(2, s / 20), Math.max(2, s / 8));
    }

    /** Rounded button with hover, pressed, and disabled states. */
    public static class Btn extends JButton {
        private Color base;
        private boolean selected;

        public Btn(String text, Color base, int width, int height, int fontSize) {
            super(text);
            this.base = base == null ? GREY : base;
            setFont(new Font("SansSerif", Font.BOLD, fontSize));
            setForeground(Color.WHITE);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            Dimension d = new Dimension(width, height);
            setPreferredSize(d);
            setMaximumSize(d);
            setMinimumSize(d);
        }

        public Btn(String text, Color base) {
            this(text, base, 260, 48, 18);
        }

        public void setBase(Color color) {
            base = color == null ? GREY : color;
            repaint();
        }

        public void setSelectedLook(boolean selected) {
            this.selected = selected;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            quality(g);
            Color fill = base;
            if (!isEnabled()) fill = new Color(70, 78, 72);
            else if (getModel().isPressed()) fill = base.darker();
            else if (getModel().isRollover()) fill = base.brighter();
            g.setColor(fill);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
            if (selected) {
                g.setColor(ZooQuestGUI.GOLD);
                g.setStroke(new BasicStroke(2.5f));
                g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 24, 24);
            }
            g.dispose();
            setForeground(isEnabled() ? Color.WHITE : new Color(150, 158, 152));
            super.paintComponent(graphics);
        }
    }
}

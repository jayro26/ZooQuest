package main.UI;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import main.Achievement;

//** Shared Swing and Java2D helpers used throughout the ZooQuest screens.
public final class GuiKit {
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

    // Shadow under gold and title text: black at 40%, a little down and to the right.
    public static final int SHADOW_PERCENT = 40;

    private static Color shadowColor() {
        return new Color(0, 0, 0, Math.round(255 * SHADOW_PERCENT / 100f));
    }

    private static int shadowOffset(Font f) {
        return Math.max(2, f.getSize() / 14);
    }

    // Draws the text with the shadow under it, in the graphics' current font and colour.
    public static void shadowString(Graphics2D g, String text, int x, int y) {
        Color c = g.getColor();
        int off = shadowOffset(g.getFont());
        g.setColor(shadowColor());
        g.drawString(text, x + off, y + off);
        g.setColor(c);
        g.drawString(text, x, y);
    }

    // A label whose text has the shadow (used for the titles and gold texts).
    public static class ShadowLabel extends JLabel {
        private boolean shadowPass;

        public ShadowLabel(String text) {
            super(text);
        }

        @Override
        public Color getForeground() {
            return shadowPass ? shadowColor() : super.getForeground();
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            int off = shadowOffset(getFont());
            return new Dimension(d.width + off, d.height + off);       // room for the shadow
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D sg = (Graphics2D) g.create();
            int off = shadowOffset(getFont());
            sg.translate(off, off);
            shadowPass = true;
            super.paintComponent(sg);
            shadowPass = false;
            sg.dispose();
            super.paintComponent(g);
        }
    }

    // Like label(...), but with the shadow: for titles and gold texts.
    public static JLabel title(String text, int fontSize, boolean bold, Color color) {
        JLabel label = new ShadowLabel(text);
        label.setFont(new Font(ZooQuestGUI.FONT_NAME, bold ? Font.BOLD : Font.PLAIN, fontSize));
        label.setForeground(color);
        return label;
    }

    public static JLabel label(String text, int fontSize, boolean bold, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(ZooQuestGUI.FONT_NAME, bold ? Font.BOLD : Font.PLAIN, fontSize));
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

    // Translucent dark plate used behind text over an image.
    public static void plate(Graphics2D g, int x, int y, int w, int h, int arc) {
        g.setColor(new Color(0, 0, 0, 105));
        g.fillRoundRect(x, y, w, h, arc, arc);
    }

    // Filled five-point star centred at (cx, cy). 
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

    // Draws a simple padlock centred at (cx, cy). 
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

    // Rounded button with hover, pressed, and disabled states.
    public static class Btn extends JButton {
        private Color base;
        private boolean selected;
        private boolean ghost;                                  // see ghost()
        private boolean lightBorder;                            // see lightBorder()

        public Btn(String text, Color base, int width, int height, int fontSize) {
            super(text);
            this.base = base == null ? GREY : base;
            Font f = new Font(ZooQuestGUI.FONT_NAME, Font.BOLD, fontSize);
            if (text != null && f.canDisplayUpTo(text) != -1)          // e.g. the arrows of the Back buttons: the program's font
                f = new Font("SansSerif", Font.BOLD, fontSize);         // lacks them, so those buttons use a font that has them
            setFont(f);
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

        // Back / Exit style: the fill is mostly see-through and the button gets a white border.
        public static final int GHOST_OPACITY = 75;             // fill opacity in percent (25 = 75% transparent)

        public Btn ghost() {
            ghost = true;
            repaint();
            return this;
        }

        // Solid button with a border in its own colour, a little lighter.
        public Btn lightBorder() {
            lightBorder = true;
            repaint();
            return this;
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
            if (ghost) {
                int a = Math.round(255 * GHOST_OPACITY / 100f);
                if (!isEnabled()) a = a * 2 / 3;
                else if (getModel().isPressed()) a = Math.min(255, a * 2 + 40);
                else if (getModel().isRollover()) a = Math.min(255, a * 2);
                g.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), a));
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                g.setColor(new Color(255, 255, 255, isEnabled() ? 255 : 120));
                g.setStroke(new BasicStroke(2f));
                g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 24, 24);
            } else {
                g.setColor(fill);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
            }
            if (lightBorder && !ghost) {
                Color b = isEnabled() ? base : fill;
                g.setColor(new Color(b.getRed() + (255 - b.getRed()) * 40 / 100,
                        b.getGreen() + (255 - b.getGreen()) * 40 / 100,
                        b.getBlue() + (255 - b.getBlue()) * 40 / 100));
                g.setStroke(new BasicStroke(2f));
                g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 24, 24);
            }
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

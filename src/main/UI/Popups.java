package main.UI;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;
import javax.swing.*;
import main.Animal;

// The two pop-ups of the layout PDF: animal details and "Choose Difficulty". Put them in an Overlay.
public final class Popups {
    private Popups() {}

    private static String esc(String s) { 
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); 
    }
    
    // Big picture on the left (animals/<name>.png), name under it, Back bottom-left, Description on the right. 
    public static JPanel animalDetail(Animal a, Runnable onBack) {
        JPanel card = ZooQuestGUI.card(0);
        card.setLayout(new BorderLayout(24, 0));
        card.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        card.setPreferredSize(new Dimension(800, 520));

        JPanel left = GuiKit.transparent(new BorderLayout(0, 10));
        JComponent pic = new JComponent() {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                GuiKit.quality(g);
                int s = Math.min(getWidth(), getHeight());
                g.setColor(ZooQuestGUI.GOLD);
                g.fillRoundRect(0, 0, s, s, 30, 30);
                GuiImages.drawAnimal(g, a, 4, 4, s - 8, s - 8, 26);
                g.dispose();
            }
        };
        pic.setPreferredSize(new Dimension(300, 300));
        left.add(pic, BorderLayout.NORTH);
        JPanel names = GuiKit.transparent(null);
        names.setLayout(new BoxLayout(names, BoxLayout.Y_AXIS));
        JLabel name = GuiKit.title(a.getName(), 28, true, ZooQuestGUI.GOLD);
        JLabel species = GuiKit.label(a.getSpecies(), 15, false, Color.WHITE);
        species.setFont(new Font(ZooQuestGUI.FONT_NAME, Font.ITALIC, 15));
        names.add(name);
        names.add(species);
        left.add(names, BorderLayout.CENTER);
        GuiKit.Btn back = new GuiKit.Btn("\u2190 Back", GuiKit.GREY, 130, 42, 16).ghost();
        back.addActionListener(e -> onBack.run());
        JPanel south = GuiKit.transparent(new FlowLayout(FlowLayout.LEFT, 0, 0));
        south.add(back);
        left.add(south, BorderLayout.SOUTH);
        card.add(left, BorderLayout.WEST);

        JPanel right = GuiKit.transparent(new BorderLayout(0, 10));
        JLabel title = GuiKit.title("Description", 22, true, ZooQuestGUI.GOLD);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        right.add(title, BorderLayout.NORTH);
        String clue = a.getClue() == null ? "-" : a.getClue().getHintText();
        String desc = a.getDescription();
        int k = desc.indexOf("Unique feature:");
        String feature = k >= 0 ? desc.substring(k + "Unique feature:".length()).trim() : "";
        String html = "<html><body>"
                + "<b>Type:</b> " + esc(a.getAnimalType()) + " - " + esc(a.getMovementStyle()) + "<br><br>"
                + "<b>Habitat:</b> " + esc(a.getHabitat()) + "<br><br>"
                + "<b>Diet:</b> " + esc(a.getDiet()) + "<br><br>"
                + "<b>Status:</b> " + esc(a.getStatus()) + "<br><br>"
                + "<b>Behaviour:</b> " + esc(a.getBehaviour()) + "<br><br>"
                + "<b>Unique feature:</b> " + esc(feature) + "<br><br>"
                + "<b>Clue:</b> " + esc(clue) + "</body></html>";
        // an HTML text pane instead of a JLabel, so the text wraps to the width of the box (a JLabel cannot)
        JEditorPane text = new JEditorPane("text/html", html);
        text.setEditable(false);
        text.setOpaque(false);
        text.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        text.setFont(new Font(ZooQuestGUI.FONT_NAME, Font.PLAIN, 15));
        text.setForeground(Color.WHITE);
        text.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        text.setCaretPosition(0);
        JScrollPane sp = new JScrollPane(text, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setBorder(BorderFactory.createEmptyBorder());
        right.add(sp, BorderLayout.CENTER);
        card.add(right, BorderLayout.CENTER);
        return card;
    }

    // "Choose Difficulty": With Clues / Without Clues. onPick receives true for "with clues".
    public static JPanel difficulty(Consumer<Boolean> onPick, Runnable onBack) {
        JPanel card = ZooQuestGUI.card(0);
        card.setLayout(new BorderLayout(0, 16));
        card.setBorder(BorderFactory.createEmptyBorder(24, 30, 22, 30));
        card.setPreferredSize(new Dimension(780, 500));
        JLabel title = GuiKit.title("Choose Difficulty", 30, true, ZooQuestGUI.GOLD);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(title, BorderLayout.NORTH);

        JPanel row = GuiKit.transparent(new GridLayout(1, 2, 26, 0));
        row.add(new ModeCard(true, onPick));
        row.add(new ModeCard(false, onPick));
        card.add(row, BorderLayout.CENTER);

        GuiKit.Btn back = new GuiKit.Btn("\u2190 Back", GuiKit.GREY, 130, 42, 16).ghost();
        back.addActionListener(e -> onBack.run());
        JPanel south = GuiKit.transparent(new FlowLayout(FlowLayout.LEFT, 0, 0));
        south.add(back);
        card.add(south, BorderLayout.SOUTH);
        return card;
    }

    // One of the two difficulty cards. Pictures: modes/with_clues.png, modes/without_clues.png. 
    private static class ModeCard extends JButton {
        private final boolean withClues;

        ModeCard(boolean withClues, Consumer<Boolean> onPick) {
            this.withClues = withClues;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addActionListener(e -> onPick.accept(withClues));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            GuiKit.quality(g);
            int w = getWidth(), h = getHeight();
            boolean hover = getModel().isRollover();
            g.setColor(new Color(255, 255, 255, hover ? 40 : 20));
            g.fillRoundRect(0, 0, w, h, 26, 26);
            g.setColor(hover ? Color.WHITE : ZooQuestGUI.GOLD);
            g.setStroke(new BasicStroke(hover ? 3f : 2f));
            g.drawRoundRect(1, 1, w - 3, h - 3, 26, 26);

            int ix = 20, iy = 20, iw = w - 40, ih = (int) (h * 0.44);
            BufferedImage img = GuiImages.load(withClues ? "modes/with_clues.png" : "modes/without_clues.png");
            if (img != null) {
                GuiImages.drawCover(g, img, ix, iy, iw, ih, 22);
            } else {
                g.setPaint(new GradientPaint(ix, iy, new Color(70, 80, 74), ix, iy + ih, new Color(34, 44, 38)));
                g.fillRoundRect(ix, iy, iw, ih, 22, 22);
                drawIcon(g, ix + iw / 2, iy + ih / 2, Math.min(iw, ih) / 2);
            }
            g.setFont(new Font(ZooQuestGUI.FONT_NAME, Font.BOLD, 26));
            g.setColor(Color.WHITE);
            String label = withClues ? "With Clues" : "Without Clues";
            FontMetrics fm = g.getFontMetrics();
            GuiKit.shadowString(g, label, (w - fm.stringWidth(label)) / 2, iy + ih + 44);
            g.setFont(new Font(ZooQuestGUI.FONT_NAME, Font.PLAIN, 15));
            g.setColor(new Color(215, 235, 215));
            String desc = withClues ? "The clue is shown when you tap an animal."
                    : "Clues stay hidden. Finish without revealing one to earn the No-Clue badge.";
            int y = iy + ih + 78;
            for (String line : GuiKit.wrap(desc, g.getFontMetrics(), w - 50)) {
                g.drawString(line, 25, y);
                y += 22;
            }
            g.dispose();
        }

        private void drawIcon(Graphics2D g, int cx, int cy, int r) {
            g.setColor(ZooQuestGUI.GOLD);
            g.setStroke(new BasicStroke(Math.max(4f, r / 7f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawOval(cx - r / 2 - r / 6, cy - r / 2 - r / 6, r, r);
            g.drawLine(cx + r / 4, cy + r / 4, cx + r * 2 / 3, cy + r * 2 / 3);
            if (!withClues) {
                g.setColor(new Color(255, 120, 100));
                g.drawLine(cx - r * 2 / 3, cy + r / 2, cx + r * 2 / 3, cy - r * 2 / 3);
            }
        }
    }
}

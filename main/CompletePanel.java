package main;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import javax.swing.*;

/** CONGRATULATIONS (layout PDF): picture with all animals, "Congratulations!!", "You Completed <Biome>", Next. */
public class CompletePanel extends ScenePanel implements ZooQuestGUI.Refreshable {
    private final ZooQuestGUI app;
    private final JLabel count = GuiKit.label(" ", 28, false, Color.WHITE);
    private final JLabel completed = GuiKit.label(" ", 22, false, Color.WHITE);
    private final JLabel stats = GuiKit.label(" ", 15, false, new Color(215, 235, 215));

    public CompletePanel(ZooQuestGUI app) {
        this.app = app;
        setBorder(BorderFactory.createEmptyBorder(20, 60, 24, 60));
        ((BorderLayout) getLayout()).setVgap(12);

        JLabel found = GuiKit.label("Animals Found", 24, true, Color.WHITE);
        found.setAlignmentX(CENTER_ALIGNMENT);
        count.setAlignmentX(CENTER_ALIGNMENT);
        JPanel north = GuiKit.transparent(null);
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(found);
        north.add(count);
        add(north, BorderLayout.NORTH);

        add(new Picture(), BorderLayout.CENTER);

        JLabel big = GuiKit.label("Congratulations!!", 40, true, Color.WHITE);
        big.setAlignmentX(CENTER_ALIGNMENT);
        completed.setAlignmentX(CENTER_ALIGNMENT);
        stats.setAlignmentX(CENTER_ALIGNMENT);
        GuiKit.Btn next = new GuiKit.Btn("Next", GuiKit.GREEN, 260, 48, 19);
        next.setAlignmentX(CENTER_ALIGNMENT);
        next.addActionListener(e -> app.show(ZooQuestGUI.UNLOCKED));
        JPanel south = GuiKit.transparent(null);
        south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));
        south.add(big);
        south.add(Box.createVerticalStrut(4));
        south.add(completed);
        south.add(Box.createVerticalStrut(2));
        south.add(stats);
        south.add(Box.createVerticalStrut(10));
        south.add(next);
        add(south, BorderLayout.SOUTH);
    }

    @Override
    public void onShow() {
        if (Flow.biome == null) { app.show(ZooQuestGUI.MENU); return; }
        setBiome(Flow.biome);
        int n = Flow.biome.getAnimals().size();
        count.setText(n + "/" + n);
        completed.setText("You Completed " + Flow.completedLabel());
        stats.setText("Wrong guesses: " + Flow.wrongGuesses + "   |   Clues used: " + (Flow.cluesUsed ? "yes" : "none"));
        repaint();
    }

    /** backgrounds/<biome>_complete.png, rounded. */
    private static class Picture extends JComponent {
        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            GuiKit.quality(g);
            int w = getWidth(), h = getHeight();
            if (Flow.biome == null) { g.dispose(); return; }
            BufferedImage img = GuiImages.scene(Flow.biome, "complete");
            if (img != null) {
                GuiImages.drawCover(g, img, 0, 0, w, h, 30);
            } else {
                g.setPaint(new GradientPaint(0, 0, ScenePanel.top(Flow.biome.getBiomeType()), 0, h, ScenePanel.bottom(Flow.biome.getBiomeType()).brighter()));
                g.fill(new RoundRectangle2D.Float(0, 0, w, h, 30, 30));
                java.util.List<Animal> animals = Flow.biome.getAnimals();
                int n = animals.size(), cols = 5, rows = (n + cols - 1) / cols;
                int cell = Math.min((w - 40) / cols, (h - 30) / rows);
                int side = cell - 14;
                int x0 = (w - cols * cell) / 2, y0 = (h - rows * cell) / 2;
                for (int i = 0; i < n; i++)
                    GuiImages.drawAnimal(g, animals.get(i), x0 + (i % cols) * cell + 7, y0 + (i / cols) * cell + 7, side, side, 18);
            }
            g.setColor(ZooQuestGUI.GOLD);
            g.setStroke(new BasicStroke(3f));
            g.drawRoundRect(1, 1, w - 3, h - 3, 30, 30);
            g.dispose();
        }
    }
}

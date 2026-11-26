package main.UI;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.*;
import main.Achievement;
import main.Biome;
import main.Flow;
import main.exceptions.ZoneLockedException;

/*
- NEW BIOME UNLOCKED (layout PDF): new-achievement box, title, picture of the new biome, name,
[Go to Biomes] [Play Again] [Main Menu]. Pictures: backgrounds/<biome>_clean.png (no animals).
*/
public class UnlockedPanel extends ScenePanel implements ZooQuestGUI.Refreshable {
    private final ZooQuestGUI app;
    private final JLabel title = GuiKit.label(" ", 36, true, Color.WHITE);
    private final JLabel caption = GuiKit.label(" ", 24, false, Color.WHITE);
    private final JPanel achievementBox = new JPanel(new BorderLayout());
    private final JLabel achievementText = GuiKit.label(" ", 15, false, Color.WHITE);
    private JPanel achievementBoxHolder;
    private Biome shown;

    public UnlockedPanel(ZooQuestGUI app) {
        this.app = app;
        setFadeEnabled(true);                          // fade in / out between screens
        setBorder(BorderFactory.createEmptyBorder(20, 50, 26, 50));
        ((BorderLayout) getLayout()).setVgap(12);

        achievementBox.setOpaque(false);
        achievementBox.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        achievementBox.add(achievementText, BorderLayout.CENTER);
        JPanel box = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                GuiKit.quality(g);
                g.setColor(ZooQuestGUI.PANEL_BG);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
                g.setColor(ZooQuestGUI.GOLD);
                g.setStroke(new BasicStroke(2f));
                g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 22, 22);
                g.dispose();
            }
        };
        box.setOpaque(false);
        box.add(achievementBox);
        box.setPreferredSize(new Dimension(330, 72));
        achievementBoxHolder = box;

        title.setHorizontalAlignment(SwingConstants.CENTER);
        // The title used to sit between two 340 px blocks and was cut to "New Bio..." at the normal window
        // size. It now has its own full-width row and the achievement box sits centred under it.
        title.setAlignmentX(CENTER_ALIGNMENT);
        JPanel boxRow = GuiKit.transparent(new FlowLayout(FlowLayout.CENTER, 0, 0));
        boxRow.add(box);
        boxRow.setAlignmentX(CENTER_ALIGNMENT);
        JPanel north = GuiKit.transparent(null);
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(title);
        north.add(Box.createVerticalStrut(6));
        north.add(boxRow);
        add(north, BorderLayout.NORTH);

        add(new Picture(), BorderLayout.CENTER);

        caption.setAlignmentX(CENTER_ALIGNMENT);
        JPanel buttons = GuiKit.transparent(new FlowLayout(FlowLayout.CENTER, 24, 0));
        GuiKit.Btn toBiomes = new GuiKit.Btn("Go to Biomes", GuiKit.GREEN, 250, 48, 18);
        toBiomes.addActionListener(e -> app.showWithLoading(ZooQuestGUI.BIOMES));   // same dim + "Loading...." as opening a biome
        GuiKit.Btn again = new GuiKit.Btn("Play Again", GuiKit.BLUE, 250, 48, 18);
        again.addActionListener(e -> playAgain());
        GuiKit.Btn menu = new GuiKit.Btn("Main Menu", GuiKit.GREY, 250, 48, 18);
        menu.addActionListener(e -> app.showWithLoading(ZooQuestGUI.MENU));
        buttons.add(toBiomes);
        buttons.add(again);
        buttons.add(menu);
        JPanel south = GuiKit.transparent(null);
        south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));
        south.add(caption);
        south.add(Box.createVerticalStrut(10));
        south.add(buttons);
        add(south, BorderLayout.SOUTH);
        caption.setHorizontalAlignment(SwingConstants.CENTER);
    }

    @Override
    public void onShow() {
        if (Flow.biome == null) { app.show(ZooQuestGUI.MENU); return; }
        List<Biome> all = app.getGameManager().getAllBiomes();
        shown = Flow.newlyUnlocked && Flow.nextIndex < all.size() ? all.get(Flow.nextIndex) : Flow.biome;
        setBiome(shown);
        title.setText(Flow.newlyUnlocked ? "New Biome Unlocked" : Flow.hasNext ? "Biome Completed" : "All Biomes Completed!");
        caption.setText(shown.getBiomeName() + " (" + shown.getBiomeType() + ")");
        if (Flow.newBadges.isEmpty()) {
            achievementBoxHolder.setVisible(false);
        } else {
            StringBuilder sb = new StringBuilder("<html><b>New Achievement:</b>");
            for (Achievement a : Flow.newBadges) sb.append("<br>").append(GuiKit.friendly(a));
            achievementText.setText(sb.append("</html>").toString());
            achievementBoxHolder.setVisible(true);
        }
        repaint();
    }

    private void playAgain() {
        try {
            app.getGameManager().startBiome(Flow.completedIndex);
            app.showWithLoading(ZooQuestGUI.ANIMALS);
        } catch (ZoneLockedException ex) {
            app.showWithLoading(ZooQuestGUI.BIOMES);
        }
    }

    // backgrounds/<biome>_clean.png of the new biome.
    private class Picture extends JComponent {
        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            GuiKit.quality(g);
            int w = getWidth(), h = getHeight();
            if (shown == null) { g.dispose(); return; }
            BufferedImage img = GuiImages.scene(shown, "clean");
            if (img != null) {
                GuiImages.drawCover(g, img, 0, 0, w, h, 30);
            } else {
                g.setPaint(new GradientPaint(0, 0, ScenePanel.top(shown.getBiomeType()), 0, h, ScenePanel.bottom(shown.getBiomeType()).brighter()));
                g.fill(new RoundRectangle2D.Float(0, 0, w, h, 30, 30));
                g.setColor(new Color(255, 255, 255, 40));
                g.fillOval(w - 240, -90, 340, 340);
            }
            g.setColor(ZooQuestGUI.GOLD);
            g.setStroke(new BasicStroke(3f));
            g.drawRoundRect(1, 1, w - 3, h - 3, 30, 30);
            g.dispose();
        }
    }
}
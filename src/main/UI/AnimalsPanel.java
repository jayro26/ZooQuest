package main.UI;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;
import javax.swing.*;
import main.Animal;
import main.Biome;
import main.Flow;
import main.GameManager;

// After clicking a biome, it goes to this panel providing the animals on that specific biome.

public class AnimalsPanel extends ScenePanel implements ZooQuestGUI.Refreshable {
    private final ZooQuestGUI app;
    private final GameManager gm;
    private final JLabel title = new JLabel(" ");
    private final JPanel grid = GuiKit.transparent(new GridLayout(3, 4, 14, 14));
    private final Set<String> explored = new HashSet<>();
    private final MysteryButton mystery = new MysteryButton();
    private boolean reviewOnly;

    public AnimalsPanel(ZooQuestGUI app) {
        this.app = app;
        this.gm = app.getGameManager();
        title.setFont(new Font("SansSerif", Font.BOLD, 36));
        title.setForeground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(26, 40, 30, 40));
        ((BorderLayout) getLayout()).setVgap(18);

        GuiKit.Btn exit = new GuiKit.Btn("Exit Biome", GuiKit.GREY, 160, 42, 16);
        exit.addActionListener(e -> { gm.exitCurrentBiome(); app.show(ZooQuestGUI.BIOMES); });
        JPanel header = GuiKit.transparent(new BorderLayout());
        header.add(title, BorderLayout.WEST);
        header.add(exit, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
        add(grid, BorderLayout.CENTER);

        mystery.addActionListener(e -> startMystery());
    }

    @Override
    public void onShow() {
        Biome b = gm.getCurrentBiome();
        if (b == null) { app.show(ZooQuestGUI.BIOMES); return; }
        setBiome(b);
        explored.clear();
        reviewOnly = gm.getBiomeStatus(gm.getCurrentBiomeIndex()) == GameManager.BiomeStatus.PERFECT;
        title.setText(b.getBiomeName());
        grid.removeAll();
        for (Animal a : b.getAnimals()) grid.add(new AnimalTile(a));
        while (grid.getComponentCount() < 11) grid.add(GuiKit.transparent(null));
        grid.add(mystery);
        refreshMystery();
        grid.revalidate();
        grid.repaint();
    }

    // The player opened an animal's details. 
    void markExplored(Animal a) {
        explored.add(a.getName());
        refreshMystery();
        grid.repaint();
    }

    private void open(Animal a) {
        markExplored(a);
        Overlay.show(app, Popups.animalDetail(a, () -> Overlay.hide(app)), () -> Overlay.hide(app));
    }

    private void refreshMystery() {
        int total = gm.getCurrentBiome().getAnimals().size();
        mystery.count = explored.size();
        mystery.total = total;
        mystery.setEnabled(explored.size() == total && !reviewOnly);
        mystery.setToolTipText(reviewOnly ? "Both badges earned - review mode"
                : explored.size() == total ? "Start the mystery game" : "Open every animal first");
        mystery.repaint();
    }

    private void startMystery() {
        int idx = gm.getCurrentBiomeIndex();
        if (gm.hasNoClueBadge(idx)) {     // nothing to gain from hiding clues: play with clues
            Flow.withClues = true;
            app.showWithLoading(ZooQuestGUI.GAME);          // "Loading...." for 3 seconds, then the game
            return;
        }
        Overlay.show(app, Popups.difficulty(with -> {
            Overlay.hide(app);
            Flow.withClues = with;
            gm.setClueMode(with);
            app.showWithLoading(ZooQuestGUI.GAME);          // "Loading...." for 3 seconds, then the game
        }, () -> Overlay.hide(app)), () -> Overlay.hide(app));
    }

    // One animal card: picture filling the card, name on a plate, tick when opened.
    private static final double PICTURE_SCALE = 0.8;   // animal picture size inside its card (1.0 = fills the card)

    private class AnimalTile extends JButton {
        private final Animal animal;

        AnimalTile(Animal animal) {
            this.animal = animal;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addActionListener(e -> open(animal));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            GuiKit.quality(g);
            int w = getWidth(), h = getHeight();
            boolean hover = getModel().isRollover();

            // the animal is drawn complete (fit, not cropped), centred, at 80 % of the card so it stays clear of the
            // corners; the card shows no name. Change PICTURE_SCALE to make the pictures bigger or smaller.
            GuiImages.drawAnimal(g, animal, 0, 0, w, h, 24, 0, PICTURE_SCALE);
            g.setColor(hover ? Color.WHITE : explored.contains(animal.getName()) ? new Color(60, 190, 110) : ZooQuestGUI.GOLD);
            g.setStroke(new BasicStroke(hover ? 4f : 2.5f));
            g.drawRoundRect(1, 1, w - 3, h - 3, 24, 24);
            if (explored.contains(animal.getName())) {
                g.setColor(new Color(60, 190, 110));
                g.fillOval(w - 40, 10, 28, 28);
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawPolyline(new int[]{w - 33, w - 27, w - 18}, new int[]{24, 30, 19}, 3);
            }
            g.dispose();
        }
    }

    // "Mystery Game  n/10" in the bottom-right cell.
    private static class MysteryButton extends JButton {
        int count, total = 10;

        MysteryButton() {
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            GuiKit.quality(g);
            int w = getWidth(), h = getHeight();
            boolean on = isEnabled();
            Color base = on ? GuiKit.GREEN : new Color(70, 78, 72);
            g.setColor(on && getModel().isRollover() ? base.brighter() : base);
            g.fillRoundRect(0, 0, w, h, 24, 24);
            g.setColor(on ? ZooQuestGUI.GOLD : new Color(110, 118, 112));
            g.setStroke(new BasicStroke(2.5f));
            g.drawRoundRect(1, 1, w - 3, h - 3, 24, 24);
            g.setColor(on ? Color.WHITE : new Color(170, 178, 172));
            g.setFont(new Font("SansSerif", Font.BOLD, 22));
            FontMetrics fm = g.getFontMetrics();
            g.drawString("Mystery Game", (w - fm.stringWidth("Mystery Game")) / 2, h / 2 - 2);
            g.setFont(new Font("SansSerif", Font.PLAIN, 20));
            fm = g.getFontMetrics();
            String c = count + "/" + total;
            g.drawString(c, (w - fm.stringWidth(c)) / 2, h / 2 + 28);
            g.dispose();
        }
    }
}
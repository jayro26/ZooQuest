package main.UI;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.*;
import main.Biome;
import main.GameDialog;
import main.GameManager;
import main.exceptions.ZoneLockedException;

/**
- Biome Panel: "<- Back", title, three tall cards (picture + name), arrows when there are more than three.
- Pictures: biomes/<name>.png (falls back to the biome's clean background, then a placeholder).
*/
public class BiomePanel extends BackgroundPanel implements ZooQuestGUI.Refreshable {
    private static final int PER_PAGE = 3;

    private final ZooQuestGUI app;
    private final JPanel row = GuiKit.transparent(new GridLayout(1, PER_PAGE, 22, 0));
    private final GuiKit.Btn prev = new GuiKit.Btn("\u2190", GuiKit.GREY, 60, 60, 28);
    private final GuiKit.Btn next = new GuiKit.Btn("\u2192", GuiKit.GREY, 60, 60, 28);
    private int page = 0;

    public BiomePanel(ZooQuestGUI app) {
        super("main_screen_background.png");
        setFadeEnabled(true);                          // fade in / out between screens
        this.app = app;
        setLayout(new BorderLayout(0, 14));
        setBorder(BorderFactory.createEmptyBorder(24, 40, 30, 40));

        GuiKit.Btn back = new GuiKit.Btn("\u2190 Back", GuiKit.GREY, 130, 44, 17);
        back.addActionListener(e -> app.show(ZooQuestGUI.MENU));
        JLabel title = GuiKit.label("Choose Biome", 34, true, ZooQuestGUI.GOLD);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel header = GuiKit.transparent(new BorderLayout(10, 0));
        header.add(back, BorderLayout.WEST);
        header.add(title, BorderLayout.CENTER);
        header.add(Box.createHorizontalStrut(130), BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        prev.addActionListener(e -> { page--; onShow(); });
        next.addActionListener(e -> { page++; onShow(); });
        JPanel west = GuiKit.transparent(new GridBagLayout());
        west.add(prev);
        JPanel east = GuiKit.transparent(new GridBagLayout());
        east.add(next);
        JPanel centre = GuiKit.transparent(new BorderLayout(14, 0));
        centre.add(west, BorderLayout.WEST);
        centre.add(row, BorderLayout.CENTER);
        centre.add(east, BorderLayout.EAST);
        add(centre, BorderLayout.CENTER);
    }

    @Override
    public void onShow() {
        GameManager gm = app.getGameManager();
        List<Biome> biomes = gm.getAllBiomes();
        int pages = Math.max(1, (biomes.size() + PER_PAGE - 1) / PER_PAGE);
        page = Math.max(0, Math.min(page, pages - 1));
        prev.setVisible(pages > 1 && page > 0);
        next.setVisible(pages > 1 && page < pages - 1);
        prev.getParent().setVisible(pages > 1);
        next.getParent().setVisible(pages > 1);
        row.removeAll();
        for (int i = page * PER_PAGE; i < (page + 1) * PER_PAGE; i++) {
            if (i < biomes.size()) row.add(new BiomeCard(i, biomes.get(i), gm));
            else row.add(GuiKit.transparent(null));
        }
        row.revalidate();
        row.repaint();
    }

    private void open(int index) {
        try {
            app.getGameManager().startBiome(index);
            app.showWithLoading(ZooQuestGUI.ANIMALS);        // "Loading...." for 3 seconds, then the biome
        } catch (ZoneLockedException ex) {
            GameDialog.show(this, "Zone locked", ex.getMessage());
        }
    }

    private class BiomeCard extends JButton {
        private final int index;
        private final Biome biome;
        private final GameManager.BiomeStatus status;
        private final int badges;
        private final String prevName;

        BiomeCard(int index, Biome biome, GameManager gm) {
            this.index = index;
            this.biome = biome;
            this.status = gm.getBiomeStatus(index);
            this.badges = gm.getBadgeCount(index);
            this.prevName = index > 0 ? gm.getAllBiomes().get(index - 1).getBiomeName() : "";
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addActionListener(e -> open(index));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            GuiKit.quality(g);
            int w = getWidth(), h = getHeight();
            boolean locked = status == GameManager.BiomeStatus.LOCKED;
            boolean hover = getModel().isRollover() && !locked;
            g.setColor(ZooQuestGUI.PANEL_BG);
            g.fillRoundRect(0, 0, w, h, 36, 36);
            g.setColor(locked ? new Color(120, 130, 124) : hover ? Color.WHITE : ZooQuestGUI.GOLD);
            g.setStroke(new BasicStroke(hover ? 3.5f : 2.5f));
            g.drawRoundRect(1, 1, w - 3, h - 3, 36, 36);

            int ix = 20, iy = 20, iw = w - 40, ih = (int) (iw * 0.70);
            if (ih > h * 0.5) ih = (int) (h * 0.5);
            BufferedImage img = GuiImages.load(GuiImages.biomeCardFile(biome));
            if (img == null) img = GuiImages.scene(biome, "clean");
            if (img != null) {
                GuiImages.drawCover(g, img, ix, iy, iw, ih, 26);
            } else {
                g.setPaint(new GradientPaint(ix, iy, ScenePanel.top(biome.getBiomeType()), ix, iy + ih, ScenePanel.bottom(biome.getBiomeType())));
                g.fillRoundRect(ix, iy, iw, ih, 26, 26);
                g.setFont(new Font("SansSerif", Font.BOLD, 20));
                g.setColor(new Color(255, 255, 255, 220));
                String label = locked ? "Locked Biome" : "Biome " + (index + 1);
                FontMetrics fm = g.getFontMetrics();
                g.drawString(label, ix + (iw - fm.stringWidth(label)) / 2, iy + ih / 2 + fm.getAscent() / 3);
            }
            if (locked) {
                g.setColor(new Color(0, 0, 0, 150));
                g.fill(new RoundRectangle2D.Float(ix, iy, iw, ih, 26, 26));
                GuiImages.drawLock(g, ix + iw / 2, iy + ih / 2 - 8, Math.min(70, ih / 2));
            }

            int x = 24, y = iy + ih + 44;
            g.setFont(new Font("SansSerif", Font.BOLD, 32));
            g.setColor(locked ? new Color(190, 196, 192) : Color.WHITE);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(biome.getBiomeName(), (w - fm.stringWidth(biome.getBiomeName())) / 2, y);

            g.setFont(new Font("SansSerif", Font.PLAIN, 15));
            g.setColor(GuiKit.SOFT_WHITE);
            int ty = y + 28, lines = 0;
            for (String line : GuiKit.wrap(biome.getDescription(), g.getFontMetrics(), w - 2 * x)) {
                if (lines++ == 3) break;
                g.drawString(line, x, ty);
                ty += 20;
            }

            String text;
            switch (status) {
                case LOCKED: text = "Complete " + prevName + " to unlock"; break;
                case UNLOCKED: text = "Ready to play"; break;
                case COMPLETED: text = "Completed"; break;
                default: text = "Perfect run!"; break;
            }
            g.setFont(new Font("SansSerif", Font.BOLD, 16));
            g.setColor(locked ? new Color(200, 205, 202) : status == GameManager.BiomeStatus.UNLOCKED ? new Color(150, 235, 160) : ZooQuestGUI.GOLD);
            g.drawString(text, x, h - 28);
            if (!locked && status != GameManager.BiomeStatus.UNLOCKED)
                for (int s = 0; s < GameManager.BADGES_PER_BIOME; s++)
                    GuiKit.star(g, w - 36 - s * 34, h - 34, 14, s < badges ? ZooQuestGUI.GOLD : new Color(255, 255, 255, 60));
            g.dispose();
        }
    }
}

package main.UI;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import main.Achievement;
import main.Biome;
import main.GameManager;
import main.UserProfile;

public class AchievementsPanel extends BackgroundPanel implements ZooQuestGUI.Refreshable {
    private final ZooQuestGUI app;
    private final JPanel grid = GuiKit.transparent(new GridLayout(2, 3, 18, 18));

    public AchievementsPanel(ZooQuestGUI app) {
        super("main_screen_background.png");
        setFadeEnabled(true);                          // fade in / out between screens
        this.app = app;
        setLayout(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(26, 44, 30, 44));

        JLabel title = GuiKit.title("Achievements", 36, true, ZooQuestGUI.GOLD);
        GuiKit.Btn back = new GuiKit.Btn("\u2190 Back", GuiKit.GREY, 130, 44, 17).ghost();
        back.addActionListener(e -> app.show(ZooQuestGUI.MENU));
        JPanel header = GuiKit.transparent(new BorderLayout());
        header.add(title, BorderLayout.WEST);
        header.add(back, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
        add(grid, BorderLayout.CENTER);
    }

    @Override
    public void onShow() {
        GameManager gm = app.getGameManager();
        UserProfile player = gm.getCurrentPlayer();
        List<Biome> biomes = gm.getAllBiomes();
        grid.removeAll();
        ((GridLayout) grid.getLayout()).setColumns(biomes.size());
        for (int row = 0; row < 2; row++) {
            boolean noClue = row == 1;
            for (Biome b : biomes) {
                String name = b.getBiomeName();
                Achievement a = player.findAchievement(noClue ? GameManager.noClueBadgeName(name) : GameManager.clearBadgeName(name)).orElse(null);
                grid.add(new BadgeCard(name, noClue, a));
            }
        }
        grid.revalidate();
        grid.repaint();
    }

    private static class BadgeCard extends JComponent {
        private final String biomeName;
        private final boolean noClue;
        private final Achievement badge;

        BadgeCard(String biomeName, boolean noClue, Achievement badge) {
            this.biomeName = biomeName;
            this.noClue = noClue;
            this.badge = badge;
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            GuiKit.quality(g);
            int w = getWidth(), h = getHeight();
            boolean got = badge != null;
            g.setColor(ZooQuestGUI.PANEL_BG);
            g.fillRoundRect(0, 0, w, h, 30, 30);
            g.setColor(got ? ZooQuestGUI.GOLD : new Color(120, 130, 124));
            g.setStroke(new BasicStroke(got ? 2.6f : 1.8f));
            g.drawRoundRect(1, 1, w - 3, h - 3, 30, 30);

            // the badge leaves room for a two-line description, so every card has the same layout
            int size = Math.min(h - 132, w - 40);
            size = Math.max(50, Math.min(size, 150));
            GuiImages.drawBadge(g, biomeName, noClue, got, (w - size) / 2, 18, size);

            int y = 18 + size + 28;
            g.setFont(new Font(ZooQuestGUI.FONT_NAME, Font.BOLD, 18));
            g.setColor(got ? ZooQuestGUI.GOLD : new Color(175, 185, 177));
            String t = GuiKit.badgeTitle(biomeName, noClue);
            FontMetrics fm = g.getFontMetrics();
            GuiKit.shadowString(g, t, (w - fm.stringWidth(t)) / 2, y);
            g.setFont(new Font(ZooQuestGUI.FONT_NAME, Font.PLAIN, 14));
            g.setColor(got ? Color.WHITE : new Color(170, 180, 172));
            String d = main.GameManager.badgeDescription(biomeName, noClue);
            y += 24;
            for (String line : GuiKit.wrap(d, g.getFontMetrics(), w - 36)) {
                g.drawString(line, (w - g.getFontMetrics().stringWidth(line)) / 2, y);
                y += 19;
            }
            g.setFont(new Font(ZooQuestGUI.FONT_NAME, Font.BOLD, 13));
            g.setColor(got ? new Color(150, 235, 160) : new Color(150, 160, 152));
            String s = got ? "Earned " + badge.getDateEarned() : "Locked";
            g.drawString(s, (w - g.getFontMetrics().stringWidth(s)) / 2, h - 14);
            g.dispose();
        }
    }
}
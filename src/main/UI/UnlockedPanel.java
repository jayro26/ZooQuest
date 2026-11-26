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
public class UnlockedPanel extends BackgroundPanel implements ZooQuestGUI.Refreshable {
    private final ZooQuestGUI app;
    private final JLabel title = GuiKit.title(" ", 36, true, Color.WHITE);
    private final JLabel caption = GuiKit.label(" ", 24, false, Color.WHITE);
    private Biome shown;

    // Each new achievement gets its own frame in the top left corner (one name per frame). Several achievements are
    // stacked, the next one below the one before. Every frame fades in, stays, fades out.
    private static final int ACH_START_DELAY_MS = 600;     // wait after the screen opens before the first one
    private static final int ACH_FADE_IN_MS = 1000;
    private static final int ACH_SHOW_MS = 4000;           // how long each one stays fully visible
    private static final int ACH_FADE_OUT_MS = 1000;
    private static final int ACH_STAGGER_MS = 400;         // the next frame appears this much after the one above it
    private final java.util.List<String> achNames = new java.util.ArrayList<>();
    private final java.util.List<String> achDescs = new java.util.ArrayList<>();
    private float[] achAlpha = new float[0];
    private javax.swing.Timer achTimer;

    public UnlockedPanel(ZooQuestGUI app) {
        super("main_screen_background.png");          // same background as the main screen
        setLayout(new BorderLayout());
        this.app = app;
        setFadeEnabled(true);                          // fade in / out between screens
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentHidden(java.awt.event.ComponentEvent e) {
                stopAchievements();                    // leaving the screen ends the pop-ups
            }
        });
        setBorder(BorderFactory.createEmptyBorder(20, 50, 26, 50));
        ((BorderLayout) getLayout()).setVgap(12);

        title.setHorizontalAlignment(SwingConstants.CENTER);
        title.setAlignmentX(CENTER_ALIGNMENT);
        JPanel north = GuiKit.transparent(null);
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(title);
        add(north, BorderLayout.NORTH);

        add(new Picture(), BorderLayout.CENTER);

        caption.setAlignmentX(CENTER_ALIGNMENT);
        JPanel buttons = GuiKit.transparent(new FlowLayout(FlowLayout.CENTER, 24, 0));
        GuiKit.Btn toBiomes = new GuiKit.Btn("Go to Biomes", GuiKit.GREEN, 250, 48, 18).lightBorder();
        toBiomes.addActionListener(e -> app.showWithLoading(ZooQuestGUI.BIOMES));   // same dim + "Loading...." as opening a biome
        GuiKit.Btn again = new GuiKit.Btn("Play Again", GuiKit.BLUE, 250, 48, 18).lightBorder();
        again.addActionListener(e -> playAgain());
        GuiKit.Btn menu = new GuiKit.Btn("Main Menu", GuiKit.GREY, 250, 48, 18).lightBorder();
        menu.addActionListener(e -> app.showWithLoading(ZooQuestGUI.MENU));
        buttons.add(toBiomes);
        buttons.add(again);
        buttons.add(menu);
        JPanel south = GuiKit.transparent(null);
        south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));
        south.add(buttons);
        add(south, BorderLayout.SOUTH);
        caption.setHorizontalAlignment(SwingConstants.CENTER);
    }

    @Override
    public void onShow() {
        if (Flow.biome == null) { app.show(ZooQuestGUI.MENU); return; }
        List<Biome> all = app.getGameManager().getAllBiomes();
        shown = Flow.newlyUnlocked && Flow.nextIndex < all.size() ? all.get(Flow.nextIndex) : Flow.biome;
        title.setText(Flow.newlyUnlocked ? "New Biome Unlocked" : Flow.hasNext ? "Biome Completed" : "All Biomes Completed!");
        caption.setText(shown.getBiomeName());
        stopAchievements();
        if (!Flow.newBadges.isEmpty()) startAchievements(new java.util.ArrayList<>(Flow.newBadges));
        repaint();
    }

    private void stopAchievements() {
        if (achTimer != null) achTimer.stop();
        achNames.clear();
        achDescs.clear();
        achAlpha = new float[0];
        repaint();
    }

    // Starts the pop-ups: every achievement fades in, stays, fades out (see the ACH_ constants), stacked top left.
    private void startAchievements(List<Achievement> list) {
        achNames.clear();
        achDescs.clear();
        for (Achievement a : list) { achNames.add(GuiKit.friendly(a)); achDescs.add(GuiKit.describe(a)); }
        achAlpha = new float[achNames.size()];
        final int n = achNames.size();
        final int total = ACH_FADE_IN_MS + ACH_SHOW_MS + ACH_FADE_OUT_MS;
        final long[] last = {System.nanoTime()};
        final double[] t = {-ACH_START_DELAY_MS};          // negative: still waiting to start
        achTimer = new javax.swing.Timer(15, null);
        achTimer.setCoalesce(true);
        achTimer.addActionListener(e -> {
            long now = System.nanoTime();
            t[0] += Math.min(40.0, (now - last[0]) / 1e6);   // a slow frame must not skip part of a fade
            last[0] = now;
            if (t[0] < 0) return;
            for (int i = 0; i < n; i++) {
                double c = t[0] - i * (double) ACH_STAGGER_MS;
                double a;
                if (c < 0) a = 0;
                else if (c < ACH_FADE_IN_MS) a = c / ACH_FADE_IN_MS;
                else if (c < ACH_FADE_IN_MS + ACH_SHOW_MS) a = 1;
                else if (c < total) a = 1 - (c - ACH_FADE_IN_MS - ACH_SHOW_MS) / ACH_FADE_OUT_MS;
                else a = 0;
                achAlpha[i] = (float) a;
            }
            repaint(achievementArea());
            if (t[0] > total + (n - 1) * (double) ACH_STAGGER_MS) {
                ((javax.swing.Timer) e.getSource()).stop();
                Rectangle area = achievementArea();
                achNames.clear();
                achDescs.clear();
                achAlpha = new float[0];
                repaint(area);
            }
        });
        achTimer.start();
    }

    // ---- the achievement frames (painted over the screen, top left of the 16:9 area)
    private double achScale() {
        return Math.max(0.8, Math.min(1.6, ZooQuestGUI.stageBox(getWidth(), getHeight()).height / 648.0));
    }

    // Every frame has the same size: ACH_WIDTH wide (a little longer than the longest name) and as high as its two lines.
    private static final int ACH_WIDTH = 310;              // frame width (at the normal window size)
    private static final int ACH_LINE_GAP = 6;             // space between "New Achievement:" and the name
    private static final int ACH_DESC_GAP = 3;             // space between the name and the description
    private static final int ACH_DESC_LINES = 2;           // the description may use up to two lines; the frame is only as high as its text needs
    private static final int ACH_PAD = 14;                 // space between the frame's edge and its text
    private static final int ACH_GAP = 12;                 // space between stacked frames

    private Font achHeadFont(double sc) { return new Font(ZooQuestGUI.FONT_NAME, Font.BOLD, Math.max(11, (int) Math.round(12 * sc))); }

    private Font achNameFont(double sc) { return new Font(ZooQuestGUI.FONT_NAME, Font.BOLD, Math.max(14, (int) Math.round(18 * sc))); }

    private Font achDescFont(double sc) { return new Font(ZooQuestGUI.FONT_NAME, Font.PLAIN, Math.max(11, (int) Math.round(13 * sc))); }

    // the description of achievement i, wrapped to the frame's width (at most ACH_DESC_LINES lines)
    private java.util.List<String> achDescLines(int i, double sc) {
        int inner = (int) Math.round(ACH_WIDTH * sc) - 2 * (int) Math.round(ACH_PAD * sc);
        java.util.List<String> all = GuiKit.wrap(i < achDescs.size() ? achDescs.get(i) : "", getFontMetrics(achDescFont(sc)), inner);
        return all.size() > ACH_DESC_LINES ? all.subList(0, ACH_DESC_LINES) : all;
    }

    // all frames have the same width; the height follows the lines of text (one or two description lines)
    private int achBoxHeight(int i, double sc) {
        int pad = (int) Math.round(ACH_PAD * sc);
        int lines = achDescLines(i, sc).size();
        return 2 * pad + getFontMetrics(achHeadFont(sc)).getHeight() + ACH_LINE_GAP + getFontMetrics(achNameFont(sc)).getHeight()
                + (lines == 0 ? 0 : ACH_DESC_GAP + lines * getFontMetrics(achDescFont(sc)).getHeight());
    }

    private Rectangle achievementArea() {
        Rectangle st = ZooQuestGUI.stageBox(getWidth(), getHeight());
        double sc = achScale();
        int stack = 0;
        for (int i = 0; i < achNames.size(); i++) stack += achBoxHeight(i, sc) + (int) Math.round(ACH_GAP * sc);
        return new Rectangle(st.x, st.y, (int) Math.round((24 + ACH_WIDTH) * sc) + 20, (int) Math.round(22 * sc) + stack + 10);
    }

    @Override
    protected void paintChildren(Graphics g) {
        super.paintChildren(g);
        if (achNames.isEmpty()) return;
        Graphics2D g2 = (Graphics2D) g.create();
        GuiKit.quality(g2);
        Rectangle st = ZooQuestGUI.stageBox(getWidth(), getHeight());
        double sc = achScale();
        int x = st.x + (int) Math.round(24 * sc), y = st.y + (int) Math.round(22 * sc);
        int w = (int) Math.round(ACH_WIDTH * sc);
        int gap = (int) Math.round(ACH_GAP * sc), pad = (int) Math.round(ACH_PAD * sc);
        Font head = achHeadFont(sc), name = achNameFont(sc), desc = achDescFont(sc);
        FontMetrics hm = g2.getFontMetrics(head), nm = g2.getFontMetrics(name), dm = g2.getFontMetrics(desc);
        for (int i = 0; i < achNames.size() && i < achAlpha.length; i++) {
            int boxH = achBoxHeight(i, sc);
            float alpha = achAlpha[i] * (isFadeEnabled() ? getContentAlpha() : 1f);
            if (alpha > 0.002f) {
                Graphics2D b = (Graphics2D) g2.create();
                b.setComposite(AlphaComposite.SrcOver.derive(Math.min(1f, alpha)));
                b.setColor(ZooQuestGUI.PANEL_BG);
                b.fillRoundRect(x, y, w, boxH, 22, 22);
                b.setColor(ZooQuestGUI.GOLD);
                b.setStroke(new BasicStroke(2f));
                b.drawRoundRect(x + 1, y + 1, w - 3, boxH - 3, 22, 22);
                b.setColor(Color.WHITE);
                b.setFont(head);
                int hy = y + pad + hm.getAscent();
                b.drawString("New Achievement:", x + pad, hy);
                b.setFont(name);
                int ny = hy + hm.getDescent() + ACH_LINE_GAP + nm.getAscent();
                b.setColor(ZooQuestGUI.GOLD);                        // same gold + shadow as on the Achievements and Profile screens
                GuiKit.shadowString(b, achNames.get(i), x + pad, ny);
                b.setFont(desc);
                b.setColor(new Color(225, 232, 228));
                int dy = ny + nm.getDescent() + ACH_DESC_GAP + dm.getAscent();
                java.util.List<String> lines = achDescLines(i, sc);
                for (int k = 0; k < lines.size(); k++) { b.drawString(lines.get(k), x + pad, dy); dy += dm.getHeight(); }
                b.dispose();
            }
            y += boxH + gap;                                 // the next achievement is stacked below this one
        }
        g2.dispose();
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
            Rectangle fr = ZooQuestGUI.pictureFrame(this);          // same size as on the Completed screen
            Font nameFont = caption.getFont().deriveFont(Font.BOLD);
            FontMetrics nfm = g.getFontMetrics(nameFont);
            int nameH = nfm.getHeight() + 10;                       // the biome's name sits right under the frame
            int fx = fr.x, fw = fr.width, fh = fr.height;
            int fy = Math.max(0, Math.min(fr.y - nameH / 2, h - fh - nameH));
            if (img != null) {
                GuiImages.drawCover(g, img, fx, fy, fw, fh, 30);
            } else {
                g.setPaint(new GradientPaint(0, fy, ScenePanel.top(shown.getBiomeType()), 0, fy + fh, ScenePanel.bottom(shown.getBiomeType()).brighter()));
                g.fill(new RoundRectangle2D.Float(fx, fy, fw, fh, 30, 30));
                g.setColor(new Color(255, 255, 255, 40));
                g.fillOval(fx + fw - 240, fy - 90, 340, 340);
            }
            g.setColor(ZooQuestGUI.GOLD);
            g.setStroke(new BasicStroke(3f));
            g.drawRoundRect(fx + 1, fy + 1, fw - 3, fh - 3, 30, 30);
            String name = caption.getText();
            g.setFont(nameFont);
            int nx = (w - nfm.stringWidth(name)) / 2, ny = fy + fh + 8 + nfm.getAscent();
            g.setColor(Color.WHITE);
            GuiKit.shadowString(g, name, nx, ny);
            g.dispose();
        }
    }
}
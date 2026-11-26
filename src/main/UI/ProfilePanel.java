package main.UI;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import javax.swing.*;
import main.Achievement;
import main.Biome;
import main.GameManager;
import main.UserProfile;

/*
- PROFILE (layout PDF): left card = name + biomes completed, right card = achievements list, Edit + Back at the bottom.
- Edit turns the name into a text field; Save calls GameManager.renamePlayer.
*/
public class ProfilePanel extends BackgroundPanel implements ZooQuestGUI.Refreshable {
    private final ZooQuestGUI app;
    private final JLabel nameLabel = new JLabel(" ");
    private final JTextField nameField = new JTextField(12);
    private final JLabel message = GuiKit.label(" ", 14, true, new Color(255, 160, 130));
    private final JPanel thumbs = GuiKit.transparent(new GridLayout(0, 2, 12, 12));
    private final JPanel achList = GuiKit.transparent(null);
    private final GuiKit.Btn editBtn = new GuiKit.Btn("Edit", GuiKit.BLUE, 150, 42, 17);
    private boolean editing;

    public ProfilePanel(ZooQuestGUI app) {
        super("main_screen_background.png");
        setFadeEnabled(true);                          // fade in / out between screens
        this.app = app;
        JPanel outer = ZooQuestGUI.card(0);
        outer.setLayout(new BorderLayout(0, 16));
        outer.setBorder(BorderFactory.createEmptyBorder(24, 28, 22, 28));
        outer.setPreferredSize(new Dimension(920, 560));

        // ---- left: profile
        JPanel left = ZooQuestGUI.card(0);
        left.setLayout(new BorderLayout(0, 10));
        left.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        JLabel title = GuiKit.label("Profile", 36, true, ZooQuestGUI.GOLD);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        left.add(title, BorderLayout.NORTH);

        nameLabel.setFont(new Font("SansSerif", Font.PLAIN, 22));
        nameLabel.setForeground(Color.WHITE);
        nameField.setFont(new Font("SansSerif", Font.PLAIN, 20));
        nameField.setVisible(false);
        nameField.addActionListener(e -> save());
        JPanel nameRow = GuiKit.transparent(new FlowLayout(FlowLayout.LEFT, 8, 0));
        nameRow.add(GuiKit.label("Name:", 22, false, Color.WHITE));
        nameRow.add(nameLabel);
        nameRow.add(nameField);
        JPanel body = GuiKit.transparent(null);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        nameRow.setAlignmentX(LEFT_ALIGNMENT);
        message.setAlignmentX(LEFT_ALIGNMENT);
        JLabel done = GuiKit.label("Biomes Completed:", 18, false, Color.WHITE);
        done.setAlignmentX(LEFT_ALIGNMENT);
        thumbs.setAlignmentX(LEFT_ALIGNMENT);
        body.add(Box.createVerticalStrut(8));
        body.add(nameRow);
        body.add(message);
        body.add(Box.createVerticalStrut(12));
        body.add(done);
        body.add(Box.createVerticalStrut(8));
        body.add(thumbs);
        left.add(body, BorderLayout.CENTER);

        // ---- right: achievements
        JPanel right = ZooQuestGUI.card(0);
        right.setLayout(new BorderLayout(0, 10));
        right.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        JLabel rt = GuiKit.label("Achievements", 36, true, ZooQuestGUI.GOLD);
        rt.setHorizontalAlignment(SwingConstants.CENTER);
        right.add(rt, BorderLayout.NORTH);
        achList.setLayout(new BoxLayout(achList, BoxLayout.Y_AXIS));
        JScrollPane sp = new JScrollPane(achList);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setBorder(null);
        right.add(sp, BorderLayout.CENTER);

        JPanel two = GuiKit.transparent(new GridLayout(1, 2, 22, 0));
        two.add(left);
        two.add(right);
        outer.add(two, BorderLayout.CENTER);

        editBtn.addActionListener(e -> { if (editing) save(); else startEdit(); });
        GuiKit.Btn back = new GuiKit.Btn("Back", GuiKit.GREY, 150, 42, 17);
        back.addActionListener(e -> app.show(ZooQuestGUI.MENU));
        JPanel south = GuiKit.transparent(new BorderLayout());
        south.add(editBtn, BorderLayout.WEST);
        south.add(back, BorderLayout.EAST);
        outer.add(south, BorderLayout.SOUTH);
        add(outer);
    }

    @Override
    public void onShow() {
        editing = false;
        message.setText(" ");
        refresh();
    }

    void refresh() {
        GameManager gm = app.getGameManager();
        UserProfile p = gm.getCurrentPlayer();
        if (p == null) return;
        nameLabel.setText("<html><u>" + p.getUsername().replace("&", "&amp;").replace("<", "&lt;") + "</u></html>");
        nameLabel.setVisible(!editing);
        nameField.setVisible(editing);
        if (editing) nameField.setText(p.getUsername());
        editBtn.setText(editing ? "Save" : "Edit");

        List<Biome> biomes = gm.getAllBiomes();
        thumbs.removeAll();
        for (int i = 0; i < biomes.size(); i++) thumbs.add(new Thumb(biomes.get(i), gm.getBiomeStatus(i)));

        achList.removeAll();
        List<Achievement> earned = p.getUserAchievements();
        if (earned.isEmpty()) {
            achList.add(GuiKit.label("No achievements yet - finish a biome!", 16, false, new Color(215, 235, 215)));
        } else {
            for (Achievement a : earned) {
                AchRow row = new AchRow(a);
                row.setAlignmentX(LEFT_ALIGNMENT);
                achList.add(row);
                achList.add(Box.createVerticalStrut(8));
            }
        }
        achList.revalidate();
        achList.repaint();
        thumbs.revalidate();
        repaint();
    }

    private void startEdit() {
        editing = true;
        message.setText(" ");
        refresh();
        nameField.requestFocusInWindow();
    }

    private void save() {
        message.setForeground(new Color(255, 160, 130));
        try {
            boolean changed = app.getGameManager().renamePlayer(nameField.getText());
            editing = false;
            if (changed) { message.setForeground(new Color(150, 235, 160)); message.setText("Name changed. Progress kept."); }
            else message.setText(" ");
        } catch (IllegalArgumentException ex) {
            message.setText(ex.getMessage());
        } catch (IOException ex) {
            editing = false;
            message.setText("Name changed, but the save file could not be updated.");
        }
        refresh();
    }

    // A biome tile: picture, name, tick when completed, dimmed otherwise.
    private static class Thumb extends JComponent {
        private final Biome biome;
        private final GameManager.BiomeStatus status;

        Thumb(Biome biome, GameManager.BiomeStatus status) {
            this.biome = biome;
            this.status = status;
            setPreferredSize(new Dimension(150, 90));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            GuiKit.quality(g);
            int w = getWidth(), h = getHeight();
            boolean done = status == GameManager.BiomeStatus.COMPLETED || status == GameManager.BiomeStatus.PERFECT;
            BufferedImage img = GuiImages.load(GuiImages.biomeCardFile(biome));
            if (img == null) img = GuiImages.scene(biome, "clean");
            if (img != null) GuiImages.drawCover(g, img, 0, 0, w, h, 18);
            else {
                g.setPaint(new GradientPaint(0, 0, ScenePanel.top(biome.getBiomeType()), 0, h, ScenePanel.bottom(biome.getBiomeType())));
                g.fillRoundRect(0, 0, w, h, 18, 18);
            }
            if (!done) { g.setColor(new Color(0, 0, 0, 150)); g.fillRoundRect(0, 0, w, h, 18, 18); }
            g.setFont(new Font("SansSerif", Font.BOLD, 17));
            g.setColor(Color.WHITE);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(biome.getBiomeName(), (w - fm.stringWidth(biome.getBiomeName())) / 2, h / 2 + 6);
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            String s = done ? "Completed" : "Not completed";
            g.setColor(done ? new Color(150, 235, 160) : new Color(200, 205, 202));
            g.drawString(s, (w - g.getFontMetrics().stringWidth(s)) / 2, h - 12);
            g.setColor(done ? ZooQuestGUI.GOLD : new Color(255, 255, 255, 70));
            g.setStroke(new BasicStroke(2f));
            g.drawRoundRect(1, 1, w - 3, h - 3, 18, 18);
            g.dispose();
        }
    }

    // One earned achievement in the list.
    private static class AchRow extends JComponent {
        private final Achievement a;

        AchRow(Achievement a) {
            this.a = a;
            Dimension d = new Dimension(100, 80);
            setPreferredSize(d);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            GuiKit.quality(g);
            int w = getWidth(), h = getHeight();
            g.setColor(new Color(255, 214, 102, 30));
            g.fillRoundRect(0, 0, w, h, 20, 20);
            g.setColor(ZooQuestGUI.GOLD);
            g.setStroke(new BasicStroke(1.8f));
            g.drawRoundRect(1, 1, w - 3, h - 3, 20, 20);
            GuiImages.drawBadgeFor(g, a, 10, 10, h - 20);
            int x = h + 6;
            g.setFont(new Font("SansSerif", Font.BOLD, 16));
            g.setColor(ZooQuestGUI.GOLD);
            g.drawString(GuiKit.friendly(a), x, 30);
            g.setFont(new Font("SansSerif", Font.PLAIN, 13));
            g.setColor(Color.WHITE);
            String d = a.getDescription();
            FontMetrics fm = g.getFontMetrics();
            while (fm.stringWidth(d) > w - x - 10 && d.length() > 4) d = d.substring(0, d.length() - 2);
            g.drawString(d.equals(a.getDescription()) ? d : d + "...", x, 50);
            g.setFont(new Font("SansSerif", Font.BOLD, 12));
            g.setColor(new Color(150, 235, 160));
            g.drawString("Earned " + a.getDateEarned(), x, 68);
            g.dispose();
        }
    }
}

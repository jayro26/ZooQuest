package main.UI;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import main.UserProfile;

// Main menu. Mirrors MenuCoordinator.mainMenu(): Start Game, Achievements, View Profile, Exit.
public class MainMenuPanel extends BackgroundPanel implements ZooQuestGUI.Refreshable {
    public static final String LOGO_FILE = "zoo-quest-logo.png";

    private static final double CONTENT_W = 924, CONTENT_H = 709;   // size of the whole block (logo ... Exit)
    private static final double MENU_SIZE = 0.85;                    // overall size of the menu: 1.0 = as large as the window allows, smaller = smaller
    private static final double MENU_LIFT = 0.05;                    // moves the whole menu up by this share of the window height (0 = exactly centred)
    private static final double LOGO_H = 170;
    private static final double GREETING_H = 40;
    private static final double SIDE = 200, BIG = 300, GAP = 37;     // button squares and the space between them
    private static final double EXIT_H = 75, EXIT_GAP = 38;

    private final ZooQuestGUI app;
    private final LogoView logo = new LogoView();
    private final TextLine welcome = new TextLine("", Font.BOLD, Color.WHITE, SwingConstants.CENTER);
    private final TextLine footerLeft = new TextLine("A Project made for the course: Object Oriented Programming",
            Font.BOLD, Color.WHITE, SwingConstants.LEFT);
    private final TextLine footerRight = new TextLine("@ cup of code, 2026",
            Font.BOLD, Color.WHITE, SwingConstants.RIGHT);
    private final MenuButton achievements;
    private final MenuButton start;
    private final MenuButton profile;
    private final MenuButton exit;

    public MainMenuPanel(ZooQuestGUI app) {
        super("main_screen_background.png");
        setFadeEnabled(true);                          // fade in / out between screens
        this.app = app;

        start = new MenuButton("Start Game", new Color(46, 150, 80), () -> app.show(ZooQuestGUI.BIOMES));
        achievements = new MenuButton("Achievements", new Color(200, 140, 30), () -> app.show(ZooQuestGUI.ACHIEVEMENTS));
        profile = new MenuButton("View Profile", new Color(50, 120, 190), () -> app.show(ZooQuestGUI.PROFILE));
        exit = new MenuButton("Exit", new Color(170, 60, 60), app::exitGame);

        JPanel stage = new JPanel(null) {
            @Override
            public void doLayout() {
                arrange(getWidth(), getHeight());
            }
        };
        stage.setOpaque(false);
        stage.add(logo);
        stage.add(welcome);
        stage.add(achievements);
        stage.add(start);
        stage.add(profile);
        stage.add(exit);
        stage.add(footerLeft);
        stage.add(footerRight);

        // the stage fills the whole panel (BackgroundPanel uses a GridBagLayout)
        GridBagConstraints fill = new GridBagConstraints();
        fill.weightx = 1;
        fill.weighty = 1;
        fill.fill = GridBagConstraints.BOTH;
        add(stage, fill);
    }

    // Places every part for a stage of w x h pixels.
    private void arrange(int w, int h) {
        final int footerStrip = Math.max(40, h / 14);                       // space kept free at the bottom (and, for balance, the top)
        double s = Math.min(1.5, Math.min((w - 40) / CONTENT_W, (h - 2 * footerStrip - 20) / CONTENT_H));
        s = Math.max(0.3, s * MENU_SIZE);

        double y = Math.max(10, (h - CONTENT_H * s) / 2 - h * MENU_LIFT);    // centred left-to-right, a little above the middle top-to-bottom
        int cx = w / 2;

        int logoH = (int) Math.round(LOGO_H * s);
        int logoW = Math.min(w - 40, (int) Math.round(logoH * 1.75));
        logo.setBounds(cx - logoW / 2, (int) Math.round(y), logoW, logoH);
        y += logoH + 12 * s;

        welcome.setFontSize(Math.max(15, (int) Math.round(30 * s)));
        welcome.setBounds(0, (int) Math.round(y), w, (int) Math.round(GREETING_H * s) + 6);
        y += GREETING_H * s + 24 * s;

        int side = (int) Math.round(SIDE * s), big = (int) Math.round(BIG * s), gap = (int) Math.round(GAP * s);
        int rowW = side * 2 + big + gap * 2;
        int x0 = cx - rowW / 2;
        int rowTop = (int) Math.round(y);
        int sideTop = rowTop + (big - side) / 2;                              // the small squares sit level with the middle of the big one
        achievements.setBounds(x0, sideTop, side, side);
        start.setBounds(x0 + side + gap, rowTop, big, big);
        profile.setBounds(x0 + side + gap + big + gap, sideTop, side, side);
        achievements.setFontSize(Math.max(13, (int) Math.round(20 * s)));
        profile.setFontSize(Math.max(13, (int) Math.round(25 * s)));
        start.setFontSize(Math.max(15, (int) Math.round(40 * s)));

        int exitH = (int) Math.round(EXIT_H * s);
        exit.setBounds(x0 + side + gap, rowTop + big + (int) Math.round(EXIT_GAP * s), big, exitH);
        exit.setFontSize(Math.max(13, (int) Math.round(30 * s)));

        int footerSize = Math.max(11, Math.min(26, Math.round(w / 75f)));
        int footerH = footerSize + 14;
        int footerY = h - footerH - Math.max(8, h / 60);
        footerLeft.setFontSize(footerSize);
        footerRight.setFontSize(footerSize);
        footerLeft.setBounds(20, footerY, w / 2 + 40, footerH);
        footerRight.setBounds(w / 2 + 60, footerY, w - w / 2 - 80, footerH);
    }

    // Called every time the menu appears, so progress is always current.
    @Override
    public void onShow() {
        UserProfile p = app.getGameManager().getCurrentPlayer();
        if (p == null)
            return;
        welcome.setText("Hello " + p.getUsername() + "!"); // (the "Biomes completed / Badges" line was removed from this screen)
    }

    // ------------------------------------------------------------ parts

    // The logo picture, as large as fits and never stretched; plain text if the file is missing.
    private static class LogoView extends JComponent {
        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            GuiKit.quality(g);
            BufferedImage img = GuiImages.load(LOGO_FILE);
            if (img != null) {
                GuiImages.drawContain(g, img, 0, 0, getWidth(), getHeight());
            } else {
                g.setFont(new Font("SansSerif", Font.BOLD, Math.max(20, getHeight() / 2)));
                g.setColor(ZooQuestGUI.GOLD);
                FontMetrics fm = g.getFontMetrics();
                String t = "Zoo Quest";
                g.drawString(t, (getWidth() - fm.stringWidth(t)) / 2, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            }
            g.dispose();
        }
    }

    // One line of text with a soft shadow, so it can be read on top of the picture. 
    private static class TextLine extends JComponent {
        private String text;
        private final int style;
        private final Color color;
        private final int align;
        private int size = 16;

        TextLine(String text, int style, Color color, int align) {
            this.text = text;
            this.style = style;
            this.color = color;
            this.align = align;
        }

        void setText(String t) {
            text = t;
            repaint();
        }

        void setFontSize(int s) {
            if (s != size) {
                size = s;
                repaint();
            }
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            GuiKit.quality(g);
            g.setFont(new Font("SansSerif", style, size));
            FontMetrics fm = g.getFontMetrics();
            int tw = fm.stringWidth(text);
            int x = align == SwingConstants.CENTER ? (getWidth() - tw) / 2
                    : align == SwingConstants.RIGHT ? getWidth() - tw : 0;
            int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
            g.setColor(new Color(0, 0, 0, 120));
            g.drawString(text, x + 2, y + 2);
            g.setColor(new Color(0, 0, 0, 70));
            g.drawString(text, x + 3, y + 3);
            g.setColor(color);
            g.drawString(text, x, y);
            g.dispose();
        }
    }

    // A big rounded square button; the text is centred and the colour brightens under the mouse. 
    private static class MenuButton extends JButton {
        private final Color base;
        private int size = 20;

        MenuButton(String text, Color base, Runnable action) {
            super(text);
            this.base = base;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setForeground(Color.WHITE);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addActionListener(e -> action.run());
        }

        void setFontSize(int s) {
            if (s != size) {
                size = s;
                setFont(new Font("SansSerif", Font.BOLD, size));
            }
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            GuiKit.quality(g);
            int w = getWidth(), h = getHeight();
            int arc = Math.max(16, Math.min(w, h) / 6);
            Color c = getModel().isPressed() ? base.darker() : getModel().isRollover() ? base.brighter() : base;
            g.setColor(new Color(0, 0, 0, 70));                       // soft shadow under the tile
            g.fillRoundRect(3, 5, w - 4, h - 4, arc, arc);
            g.setColor(c);
            g.fillRoundRect(0, 0, w - 4, h - 4, arc, arc);
            g.setColor(new Color(255, 255, 255, getModel().isRollover() ? 230 : 120));
            g.setStroke(new BasicStroke(getModel().isRollover() ? 3f : 2f));
            g.drawRoundRect(1, 1, w - 6, h - 6, arc, arc);
            g.setFont(getFont());
            FontMetrics fm = g.getFontMetrics();
            String t = getText();
            g.setColor(Color.WHITE);
            g.drawString(t, (w - 4 - fm.stringWidth(t)) / 2, (h - 4 + fm.getAscent() - fm.getDescent()) / 2);
            g.dispose();
        }
    }
}

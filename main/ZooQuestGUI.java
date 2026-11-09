package main;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.swing.*;

/**
 * Main window. Owns the single GameManager and a CardLayout that switches
 * between screens.
 * Teammates add their screens with register("biomes", panel) and move with
 * show("biomes").
 */
public class ZooQuestGUI extends JFrame {

    // screen names (shared contract between teammates)
    public static final String START = "start";
    public static final String MENU = "menu";
    public static final String BIOMES = "biomes";
    public static final String ACHIEVEMENTS = "achievements";
    public static final String PROFILE = "profile";

    public static final Color GOLD = new Color(255, 214, 102);
    public static final Color PANEL_BG = new Color(20, 40, 28, 205);

    private final GameManager gm = new GameManager(Path.of("saves"));
    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);

    public ZooQuestGUI() {
        super("ZooQuest");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                exitGame();
            }
        });
        setContentPane(root);
        setMinimumSize(new Dimension(800, 560));
        setSize(1000, 680);
        setLocationRelativeTo(null);

        register(START, new StartPanel(this));
        register(MENU, new MainMenuPanel(this));
        // Placeholders until the teammates plug in the real panels
        register(BIOMES, placeholder("Biome Selection"));
        register(ACHIEVEMENTS, placeholder("Achievements"));
        register(PROFILE, placeholder("Profile"));
        show(START);
    }

    public GameManager getGameManager() {
        return gm;
    }

    public void register(String name, JPanel panel) {
        root.add(panel, name);
    }

    public void show(String name) {
        cards.show(root, name);
        Component c = null;
        for (Component comp : root.getComponents())
            if (comp.isVisible())
                c = comp;
        if (c instanceof Refreshable r)
            r.onShow(); // lets a screen reload fresh data
    }

    /** Implement on a panel to be told when it becomes the visible screen. */
    public interface Refreshable {
        void onShow();
    }

    public void exitGame() {
        gm.exitCurrentBiome(); // discards unfinished biome, same as the console version
        dispose();
        System.exit(0);
    }

    private JPanel placeholder(String title) {
        BackgroundPanel p = new BackgroundPanel(" ");
        JPanel card = card(30);
        JLabel l = new JLabel(title + " (coming soon)", SwingConstants.CENTER);
        l.setForeground(Color.WHITE);
        l.setFont(new Font("SansSerif", Font.BOLD, 24));
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        JButton back = button("Back", new Color(90, 90, 90));
        back.setAlignmentX(Component.CENTER_ALIGNMENT);
        back.addActionListener(e -> show(MENU));
        card.add(l);
        card.add(Box.createVerticalStrut(20));
        card.add(back);
        p.add(card);
        return p;
    }

    // ------------------------------------------------------------ shared UI
    // helpers

    /** Rounded translucent box layout panel. */
    public static JPanel card(int pad) {
        JPanel p = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(PANEL_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 36, 36);
                g2.setColor(GOLD);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 36, 36);
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(pad, pad + 20, pad, pad + 20));
        return p;
    }

    /** Rounded button with hover effect. */
    public static JButton button(String text, Color base) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color c = getModel().isPressed() ? base.darker()
                        : getModel().isRollover() ? base.brighter() : base;
                g2.setColor(c);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(new Font("SansSerif", Font.BOLD, 18));
        b.setForeground(Color.WHITE);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Dimension d = new Dimension(260, 48);
        b.setPreferredSize(d);
        b.setMaximumSize(d);
        b.setMinimumSize(d);
        return b;
    }

    /**
     * Looks for the image in the usual resource folders; returns null if not found.
     */
    public static BufferedImage loadImage(String fileName) {
        String[] dirs = { "main/resources/", "resources/", "../resources/", "src/main/resources/" };
        for (String d : dirs) {
            File f = new File(d + fileName);
            if (f.exists()) {
                try {
                    return ImageIO.read(f);
                } catch (IOException ignored) {
                }
            }
        }
        return null;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ZooQuestGUI().setVisible(true));
    }
}
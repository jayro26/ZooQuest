package main.UI;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.swing.*;
import main.*;

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
    public static final String ANIMALS = "animals";
    public static final String GAME = "game";
    public static final String COMPLETE = "complete";
    public static final String UNLOCKED = "unlocked";


    public static final Color GOLD = new Color(255, 214, 102);
    public static final Color PANEL_BG = new Color(20, 40, 28, 205);

    private final GameManager gm = new GameManager(GameManager.defaultSaveDirectory()); // NEW CODE (was: Path.of("saves"))
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
        register(BIOMES, new BiomePanel(this));
        register(ANIMALS, new AnimalsPanel(this));
        register(GAME, new GamePanel(this));
        register(COMPLETE, new CompletePanel(this));
        register(UNLOCKED, new UnlockedPanel(this));
        register(ACHIEVEMENTS, new AchievementsPanel(this));
        register(PROFILE, new ProfilePanel(this));
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
     * Loads an image from the project's resources folder. The folder is resolved
     * through ProjectPaths so this works when launched from the project root,
     * src, src/main, or the older main/resources layout. A classpath lookup is
     * also supported for packaged builds that include resources/ in the JAR.
     */
    public static BufferedImage loadImage(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }

        // Accept either slash style, but always resolve the name inside resources.
        String normalizedName = fileName.trim().replace('\\', '/');
        while (normalizedName.startsWith("/")) {
            normalizedName = normalizedName.substring(1);
        }

        final Path relativePath;
        try {
            relativePath = Path.of(normalizedName).normalize();
        } catch (InvalidPathException ignored) {
            return null;
        }

        // Do not allow a resource name to escape the resources directory.
        if (relativePath.isAbsolute() || relativePath.startsWith("..")) {
            return null;
        }

        Path resourcesRoot = ProjectPaths.resourcesDirectory().toAbsolutePath().normalize();
        Path imagePath = resourcesRoot.resolve(relativePath).normalize();
        if (imagePath.startsWith(resourcesRoot) && Files.isRegularFile(imagePath)) {
            try {
                BufferedImage image = ImageIO.read(imagePath.toFile());
                if (image != null) {
                    return image;
                }
            } catch (IOException ignored) {
                // Try the classpath copy below, if one is available.
            }
        }

        String classpathName = "resources/" + relativePath.toString().replace('\\', '/');
        try (InputStream input = ZooQuestGUI.class.getClassLoader()
                .getResourceAsStream(classpathName)) {
            if (input != null) {
                return ImageIO.read(input);
            }
        } catch (IOException ignored) {
            // A missing or unreadable optional image is handled by the caller's fallback.
        }

        return null;
    }
}
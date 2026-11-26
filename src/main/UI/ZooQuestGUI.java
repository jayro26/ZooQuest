package main.UI;

import java.awt.*;
import java.awt.image.BufferedImage; 
import java.io.File;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import javax.imageio.ImageIO;
import javax.swing.*; 
import main.GameManager;
import main.ProjectPaths;

/*
- Main window. Owns the single GameManager and a CardLayout that switches
between screens.
- add their screens with register("biomes", panel) and move with
show("biomes").
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

    private final GameManager gm = new GameManager(GameManager.defaultSaveDirectory());
    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);

    // screen transition (only screens with setFadeEnabled(true) fade; their background picture stays)
    private static final int FADE_OUT_MS = 180, FADE_IN_MS = 260;
    private boolean fadingOut;
    private String pendingTarget;
    private int switchGen;
    private Fadable startupFade;     // first screen: fades in once the window is on screen

    // "Loading...." screen shown before a biome opens and before the mystery game starts
    private static final int DIM_MS = 450;        // dim out / dim in between the finished game and the next screens
    private static final int LOADING_MS = 3000;   // how long the loading screen stays (milliseconds)
    private boolean loading;
    private final java.util.Map<String, JPanel> screens = new java.util.HashMap<>();

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
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowOpened(java.awt.event.WindowEvent e) {
                Fadable p = startupFade;
                startupFade = null;
                if (p != null) {
                    if (((Component) p).isShowing()) p.fadeTo(1f, FADE_IN_MS, null);
                    else p.setContentAlpha(1f);
                }
            }
        });
        show(START);
    }

    public GameManager getGameManager() {
        return gm;
    }

    public void register(String name, JPanel panel) {
        screens.put(name, panel);
        root.add(panel, name);
    }

    public void show(String name) {
        if (!SwingUtilities.isEventDispatchThread()) { // not on the UI thread: switch at once, no animation
            switchTo(name, false);
            return;
        }
        if (fadingOut) { // a fade-out is already running: just change where it ends up
            pendingTarget = name;
            return;
        }
        Component current = visibleScreen();
        if (current instanceof Fadable cur && cur.isFadeEnabled() && current.isShowing()
                && cur.getContentAlpha() > 0.01f) {
            fadingOut = true;
            pendingTarget = name;
            cur.fadeTo(0f, FADE_OUT_MS, () -> {
                fadingOut = false;
                String target = pendingTarget;
                pendingTarget = null;
                switchTo(target, true);
                cur.setContentAlpha(1f); // it is hidden now; ready for its next visit
            });
        } else {
            switchTo(name, true);
        }
    }

    /*
    - Dims the window by 50 % with "Loading...." at the bottom left for {@link #LOADING_MS}, then moves to the
    screen and lifts the dimming. Clicks are ignored while it is showing.
    */
    public void showWithLoading(String name) {
        if (!SwingUtilities.isEventDispatchThread()) { // not on the UI thread: no loading screen
            show(name);
            return;
        }
        if (loading) return;
        loading = true;
        System.out.println("[ZooQuest] Loading screen started (" + LOADING_MS + " ms) -> " + name); // shows in the console if this build is running
        LoadingOverlay ov = new LoadingOverlay(true, 0.5f);
        setGlassPane(ov);
        ov.setVisible(true);
        ov.fadeTo(1f, 250, null);                                  // darken
        Timer warm = new Timer(450, e -> warmUp(name));            // slow work (pictures) once the screen is fully dimmed
        warm.setRepeats(false);
        warm.start();
        Timer hold = new Timer(LOADING_MS, e -> {
            show(name);                                            // the new screen is prepared under the dimming
            Timer reveal = new Timer(FADE_OUT_MS + 60, ev ->       // wait for the old screen's fade-out to finish
                    ov.fadeTo(0f, 350, () -> {
                        ov.setVisible(false);
                        loading = false;
                    }));
            reveal.setRepeats(false);
            reveal.start();
        });
        hold.setRepeats(false);
        hold.start();
    }

    /** Same as {@link #showWithDim(String, Runnable)} with nothing to do while the screen is black. */
    public void showWithDim(String name) {
        showWithDim(name, null);
    }

    /*
    - Dims the whole window out to black, switches to {@code name} while it is black (running {@code whileBlack}
    first, if given), then dims back in to show the new screen. Clicks are ignored meanwhile.
    */
    public void showWithDim(String name, Runnable whileBlack) {
        if (!SwingUtilities.isEventDispatchThread()) { // not on the UI thread: switch at once, no animation
            if (whileBlack != null) whileBlack.run();
            show(name);
            return;
        }
        if (loading) return;
        loading = true;
        LoadingOverlay ov = new LoadingOverlay(false, 1f);
        setGlassPane(ov);
        ov.setVisible(true);
        ov.fadeTo(1f, DIM_MS, () -> {
            if (whileBlack != null) whileBlack.run();
            show(name);
            Timer reveal = new Timer(FADE_OUT_MS + 120, ev ->           // let the old screen finish fading out
                    ov.fadeTo(0f, DIM_MS, () -> {
                        ov.setVisible(false);
                        loading = false;
                    }));
            reveal.setRepeats(false);
            reveal.start();
        });
    }

    // Prepares a screen out of sight (loads its pictures, builds its first frame) so it opens without a stall.
    private void warmUp(String name) {
        JPanel p = screens.get(name);
        if (p == null || root.getWidth() < 10 || root.getHeight() < 10) return;
        try {
            if (p instanceof Refreshable r) r.onShow();
            p.setSize(root.getSize());
            p.validate();
            BufferedImage img = new BufferedImage(root.getWidth(), root.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            p.paint(g);
            g.dispose();
        } catch (RuntimeException ignored) {
            // warming up is only an optimisation; the real switch below still works without it
        }
    }

    private Component visibleScreen() {
        Component c = null;
        for (Component comp : root.getComponents())
            if (comp.isVisible())
                c = comp;
        return c;
    }

    private void switchTo(String name, boolean animate) {
        int gen = ++switchGen;
        cards.show(root, name);
        Component c = visibleScreen();
        Fadable fade = animate && c instanceof Fadable b && b.isFadeEnabled() ? b : null;
        if (fade != null)
            fade.setContentAlpha(0f); // starts invisible, so there is no flash before the fade-in
        if (c instanceof Refreshable r)
            r.onShow(); // lets a screen reload fresh data
        if (gen != switchGen)
            return; // onShow sent the player somewhere else; that switch handles its own fade
        if (fade != null) {
            if (isShowing()) fade.fadeTo(1f, FADE_IN_MS, null);
            else startupFade = fade;
        } else if (c instanceof Fadable b) {
            b.setContentAlpha(1f);
        }
    }

    // Implement on a panel to be told when it becomes the visible screen.
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

    // Rounded translucent box layout panel.
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

    // Rounded button with hover effect.
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

    // Looks for the image in the usual resource folders; returns null if not found.
    public static BufferedImage loadImage(String fileName) {
        if (fileName == null || fileName.isBlank()) return null;
        try {
            File inResources = ProjectPaths.resourcesDirectory().resolve(fileName).toFile();
            if (inResources.isFile()) {
                try {
                    return ImageIO.read(inResources);
                } catch (IOException ignored) {
                }
            }
        } catch (InvalidPathException ignored) {
        }
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
}
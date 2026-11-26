package main.UI;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.*;
import main.Animal;
import main.Biome;
import main.Flow;
import main.GameDialog;
import main.GameManager;
import main.Hotspots;
import main.MysteryClue;
import main.exceptions.AnimalNotFoundException;

/*
- Hidden-animal seek-and-find screen (mystery game).
- 1) Main scene: only the Exit Biome button, "Animals Found n/10", the clue mode and the scene with the hidden animals.
- 2) Tap a hidden animal: the scene zooms in on it and the clue (With Clues mode) and four answers rise from the
bottom. Back zooms out and lowers them again. A right answer turns the animal to colour, then zooms out.
- 3) After the last animal: the scene shows every animal, the clue mode and Exit Biome vanish, and the game goes on
to the Completed screen with a dim-out.
*/
public class GamePanel extends ScenePanel implements ZooQuestGUI.Refreshable {
    private final ZooQuestGUI app;
    private final GameManager gm;

    // ---- timing (milliseconds) -------------------------------------------------------------------------------
    private static final int ZOOM_IN_MS = 650, RISE_MS = 450, RISE_DELAY_MS = 380;      // tap an animal
    private static final int LOWER_MS = 350, ZOOM_OUT_MS = 600, ZOOM_OUT_DELAY_MS = 200; // back / after a right answer
    private static final int SHOW_ANSWER_MS = 1100;                                      // right answer stays visible
    private static final int FINISH_HOLD_MS = 1800;                                      // all animals shown, then dim-out

    // ---- look ------------------------------------------------------------------------------------------------
    private static final double SCENE_SIZE = 0.75;     // size of the scene in the main scene (1.0 = as large as fits)
    private static final double SCENE_DOWN = 0.06;     // how far the scene is moved down from the centre (fraction of the height)
    private static final double TEXT_SIZE = 0.80;      // size of "Animals Found" and the mode text (1.0 = the old size)

    private final GuiKit.Btn exitBtn = new GuiKit.Btn("Exit Biome", GuiKit.GREY, 150, 40, 15).ghost();
    private final GuiKit.Btn backBtn = new GuiKit.Btn("Back", GuiKit.GREY, 150, 40, 15).ghost();
    private final OptionButton[] options = new OptionButton[4];

    private enum Mode { BROWSE, CHOOSING, ANSWERED, LEAVING }

    private Biome biome;
    private Mode mode = Mode.BROWSE;
    private boolean finishing;
    private boolean nextWasLocked;
    private boolean debug;
    private int hover = -1;
    private int[] imgRect = {0, 0, 1, 1};

    private int selected = -1;                         // index of the tapped animal while zoomed
    private List<String> choices = List.of();
    private final Set<Integer> wrong = new HashSet<>();
    private int correctOption = -1;
    private String clueText;                           // null = no clue box (Without Clues mode)
    private String answeredText;

    // animation: zoom 0 = whole scene, 1 = zoomed on the animal; rise 0 = answers hidden, 1 = fully risen
    private double zoomV, riseV;
    private final Tween zoomTween = new Tween(), riseTween = new Tween();
    private double clock;
    private long lastTick;
    private Runnable afterSettle;
    private final javax.swing.Timer anim = new javax.swing.Timer(15, e -> tick());
    private javax.swing.Timer timer;

    public GamePanel(ZooQuestGUI app) {
        this.app = app;
        this.gm = app.getGameManager();
        setLayout(null);
        setBorder(null);

        add(exitBtn);
        add(backBtn);
        for (int i = 0; i < options.length; i++) {
            final int n = i;
            options[i] = new OptionButton();
            options[i].addActionListener(e -> choose(n));
            add(options[i]);
        }
        backBtn.setVisible(false);
        for (OptionButton o : options) o.setVisible(false);

        exitBtn.addActionListener(e -> exitBiome());
        backBtn.addActionListener(e -> back());

        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int hit = canTap() ? hit(e.getPoint()) : -1;
                if (hit != hover) {
                    hover = hit;
                    setCursor(Cursor.getPredefinedCursor(hit >= 0 ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                }
            }

            @Override public void mouseClicked(MouseEvent e) {
                if (!canTap()) return;
                int hit = hit(e.getPoint());
                if (hit >= 0) tapAnimal(hit);
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);

        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_H, 0), "hotspots");
        getActionMap().put("hotspots", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                debug = !debug;
                repaint();
            }
        });
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "back");
        getActionMap().put("back", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                if (mode == Mode.CHOOSING) back();
            }
        });
    }

    @Override
    public void onShow() {
        biome = gm.getCurrentBiome();
        if (biome == null) {
            app.show(ZooQuestGUI.BIOMES);
            return;
        }
        setBiome(biome);
        if (timer != null) timer.stop();
        anim.stop();
        afterSettle = null;
        finishing = false;
        mode = Mode.BROWSE;
        selected = -1;
        hover = -1;
        zoomV = riseV = 0;
        zoomTween.active = riseTween.active = false;
        wrong.clear();
        int currentIndex = gm.getCurrentBiomeIndex();
        nextWasLocked = currentIndex + 1 < gm.getAllBiomes().size()
                && gm.getAllBiomes().get(currentIndex + 1).isLocked();
        if (getWidth() > 100 && getHeight() > 100) {
            try {
                Rectangle fit = fitRect(stW(), stH());
                HiddenSceneRenderer.warm(biome, stageRect(stW(), stH()).width,
                        HiddenSceneRenderer.zoomWidth(fit.width));   // prepared while the loading screen shows
            } catch (RuntimeException ignored) {
                // only an optimisation
            }
        }
        updateControls();
        repaint();
    }

    // ------------------------------------------------------------ layout of the controls

    @Override
    public void doLayout() {
        updateControls();
    }

    // The elements live in the centred 16:9 stage (the background fills the whole window). Everything below is worked
    // out in stage coordinates; ox()/oy() move it to the window.
    private Rectangle box() { return ZooQuestGUI.stageBox(getWidth(), getHeight()); }
    private int stW() { return box().width; }
    private int stH() { return box().height; }
    private int ox() { return box().x; }
    private int oy() { return box().y; }
    private double sx() { return stW() / 1920.0; }
    private double sy() { return stH() / 1080.0; }

    private boolean withClue() { return clueText != null; }

    // Rectangles of the bottom area for the current size (final resting place, risen).
    private Rectangle clueRect() {
        int w = stW(), h = stH();
        int x = (int) Math.round(108 * sx());
        int bottom = h - (int) Math.round(75 * sy());
        int ch = Math.max(110, (int) Math.round(215 * sy()));
        int cw = (int) Math.round(518 * sx());
        cw = Math.max(cw, 240);
        return new Rectangle(x, bottom - ch, cw, ch);
    }

    private Rectangle optionRect(int i) {
        int w = stW(), h = stH();
        int bottom = h - (int) Math.round(75 * sy());
        int oh = Math.max(60, (int) Math.round((withClue() ? 108 : 119) * sy()));
        int left, right;
        int gap = Math.max(10, (int) Math.round(30 * sx()));
        if (withClue()) {
            Rectangle c = clueRect();
            left = c.x + c.width + Math.max(14, (int) Math.round(42 * sx()));
        } else {
            left = (int) Math.round(108 * sx());
        }
        right = w - (int) Math.round(108 * sx());
        int ow = (right - left - 3 * gap) / 4;
        return new Rectangle(left + i * (ow + gap), bottom - oh, ow, oh);
    }

    private Rectangle backRect() {
        Rectangle top = withClue() ? clueRect() : optionRect(0);
        int bh = 40, bw = 150;
        int gap = Math.max(10, (int) Math.round(21 * sy()));
        return new Rectangle(top.x, top.y - gap - bh, bw, bh);
    }

    // Moves the bottom controls (they slide up from below the window while riseV goes 0 -> 1).
    private void updateControls() {
        int w = stW(), h = stH();
        if (w <= 0 || h <= 0) return;
        int ox = ox(), oy = oy();
        boolean browse = mode == Mode.BROWSE && !finishing;
        exitBtn.setBounds(ox + (int) Math.round(70 * sx()), oy + Math.max(10, (int) Math.round(55 * sy())), 150, 40);
        exitBtn.setVisible(browse && zoomV < 0.01);

        boolean shown = riseV > 0.002 && selected >= 0;
        double e = 1 - riseV;
        Rectangle back = backRect();
        int drop = (int) Math.round(e * (h + oy - Math.min(back.y, h) + 30));   // slides up from below the window
        backBtn.setBounds(ox + back.x, oy + back.y + drop, back.width, back.height);
        backBtn.setVisible(shown);
        backBtn.setEnabled(mode == Mode.CHOOSING);
        for (int i = 0; i < options.length; i++) {
            Rectangle r = optionRect(i);
            options[i].setBounds(ox + r.x, oy + r.y + drop, r.width, r.height);
            options[i].setVisible(shown && i < choices.size());
        }
    }

    // ------------------------------------------------------------ taps

    private boolean canTap() {
        return mode == Mode.BROWSE && !finishing && biome != null;
    }

    private Rectangle rect(int index) {
        return HiddenSceneRenderer.toRectangle(imgRect, Hotspots.forBiome(biome)[index]);
    }

    private boolean isFound(int index) {
        return finishing || gm.isAnimalFound(biome.getAnimals().get(index).getName());
    }

    private int hit(Point point) {
        if (biome == null || imgRect[2] <= 1) return -1;
        int best = -1;
        double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i < biome.getAnimals().size(); i++) {
            if (isFound(i)) continue;
            Rectangle r = rect(i);
            if (!r.contains(point)) continue;
            double d = point.distanceSq(r.getCenterX(), r.getCenterY());
            if (d < bestDistance) {
                bestDistance = d;
                best = i;
            }
        }
        return best;
    }

    // The player taps a hidden animal: zoom in, then the clue and the four answers rise.
    private void tapAnimal(int index) {
        Animal animal = biome.getAnimals().get(index);
        if (gm.isAnimalFound(animal.getName())) return;
        try {
            MysteryClue clue = gm.selectHiddenAnimal(animal.getName());   // GameManager builds the 4 answers
            choices = gm.getCurrentChoices();
            clueText = gm.isClueShown() ? clue.getHintText() : null;
        } catch (AnimalNotFoundException | IllegalStateException ex) {
            GameDialog.show(this, "Unable to find animal", ex.getMessage(), "Continue");
            return;
        }
        selected = index;
        wrong.clear();
        correctOption = -1;
        answeredText = null;
        hover = -1;
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        for (int i = 0; i < options.length; i++) {
            options[i].reset(i < choices.size() ? choices.get(i) : "");
        }
        mode = Mode.CHOOSING;
        start(zoomTween, zoomV, 1, 0, ZOOM_IN_MS);
        start(riseTween, riseV, 1, RISE_DELAY_MS, RISE_MS);
        updateControls();
    }

    // Back: lower the clue and answers, zoom out, and let the player tap an animal again.
    private void back() {
        if (mode != Mode.CHOOSING) return;
        leave(() -> {
            mode = Mode.BROWSE;
            selected = -1;
            updateControls();
            repaint();
        });
    }

    private void leave(Runnable whenDone) {
        mode = Mode.LEAVING;
        afterSettle = whenDone;
        start(riseTween, riseV, 0, 0, LOWER_MS);
        start(zoomTween, zoomV, 0, ZOOM_OUT_DELAY_MS, ZOOM_OUT_MS);
        updateControls();
    }

    private void choose(int option) {
        if (mode != Mode.CHOOSING || selected < 0 || option >= choices.size()) return;
        Animal animal = biome.getAnimals().get(selected);
        GameManager.GuessResult result = gm.processGuess(choices.get(option));
        if (!result.correct()) {
            wrong.add(option);
            options[option].markWrong();
            repaint();
            return;
        }
        // right answer: the animal turns to colour where it hides; keep it in view for a moment, then zoom out
        mode = Mode.ANSWERED;
        correctOption = option;
        options[option].markCorrect();
        for (int i = 0; i < options.length; i++) if (i != option) options[i].setEnabled(false);
        answeredText = "Correct! You found the " + animal.getName() + ".";
        backBtn.setEnabled(false);
        hover = -1;
        boolean done = result.biomeCompleted();
        if (done) prepareFlow(result);
        repaint();
        javax.swing.Timer hold = new javax.swing.Timer(SHOW_ANSWER_MS, e -> {
            if (done) {
                finishing = true;          // every animal shows, the clue mode and Exit Biome vanish
                leave(() -> {
                    mode = Mode.BROWSE;
                    selected = -1;
                    updateControls();
                    repaint();
                    timer = new javax.swing.Timer(FINISH_HOLD_MS, ev -> toComplete());
                    timer.setRepeats(false);
                    timer.start();
                });
            } else {
                leave(() -> {
                    mode = Mode.BROWSE;
                    selected = -1;
                    updateControls();
                    repaint();
                });
            }
        });
        hold.setRepeats(false);
        hold.start();
    }

    private void exitBiome() {
        if (finishing) {
            if (timer != null) timer.stop();
            gm.exitCurrentBiome();
            finishing = false;
            app.show(ZooQuestGUI.BIOMES);
            return;
        }
        int choice = GameDialog.show(this, "Leave this biome?",
                "Your unfinished progress in this biome will be lost.",
                "Stay and keep playing", "Leave biome");
        if (choice == 1) {
            // same dim + "Loading...." as entering a biome; the game is closed just before the biome list appears
            app.showWithLoading(ZooQuestGUI.BIOMES, gm::exitCurrentBiome);
        }
    }

    private void prepareFlow(GameManager.GuessResult result) {
        int index = gm.getCurrentBiomeIndex();
        Flow.biome = biome;
        Flow.completedIndex = index;
        Flow.hasNext = gm.hasNextBiome();
        Flow.nextIndex = index + 1;
        Flow.newlyUnlocked = Flow.hasNext && nextWasLocked;
        Flow.wrongGuesses = gm.getWrongGuessCount();
        Flow.cluesUsed = gm.hasUsedClues();
        Flow.newBadges = new ArrayList<>(result.newAchievements());
    }

    private void toComplete() {
        if (!finishing) return;
        // the finished picture stays on screen while the window dims out; the game is closed once it is black
        app.showWithDim(ZooQuestGUI.COMPLETE, () -> {
            finishing = false;
            gm.exitCurrentBiome(); // GameManager has already saved a completed biome.
        });
    }

    // ------------------------------------------------------------ animation

    private static final class Tween {
        boolean active;
        double from, to, start, delay, dur;

        double at(double clock) {
            if (!active) return to;
            double t = Math.max(0, Math.min(1, (clock - start - delay) / dur));
            t = t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;       // ease in-out
            return from + (to - from) * t;
        }

        boolean done(double clock) {
            return !active || clock - start - delay >= dur;
        }
    }

    private void start(Tween tw, double from, double to, double delay, double dur) {
        tw.active = true;
        tw.from = from;
        tw.to = to;
        tw.start = clock;
        tw.delay = delay;
        tw.dur = Math.max(1, dur);
        if (!anim.isRunning()) {
            lastTick = System.nanoTime();
            anim.start();
        }
    }

    private void tick() {
        long now = System.nanoTime();
        clock += Math.min(40.0, (now - lastTick) / 1e6);        // a slow frame must not make the animation jump
        lastTick = now;
        zoomV = zoomTween.at(clock);
        riseV = riseTween.at(clock);
        updateControls();
        repaint();
        if (zoomTween.done(clock) && riseTween.done(clock)) {
            zoomTween.active = riseTween.active = false;
            anim.stop();
            Runnable r = afterSettle;
            afterSettle = null;
            if (r != null) r.run();
        }
    }

    // ------------------------------------------------------------ painting

    // The whole scene, as large as possible, centred in the window.
    private static Rectangle fitRect(int w, int h) {
        int fw = (int) Math.min(w, Math.floor(h * SceneLayouts.ASPECT));
        int fh = (int) Math.round(fw / SceneLayouts.ASPECT);
        return new Rectangle((w - fw) / 2, (h - fh) / 2, fw, fh);
    }

    // The scene of the main scene: SCENE_SIZE of the largest fit, centred and moved a little down (border drawn around it).
    private static Rectangle stageRect(int w, int h) {
        Rectangle fit = fitRect(w, h);
        int sw = (int) Math.round(fit.width * SCENE_SIZE), sh = (int) Math.round(fit.height * SCENE_SIZE);
        int y = (h - sh) / 2 + (int) Math.round(h * SCENE_DOWN);
        y = Math.min(y, h - sh);                       // never below the window
        return new Rectangle((w - sw) / 2, y, sw, sh);
    }

    // Where the scene sits when fully zoomed on the selected animal: the animal centred above the answers.
    private Rectangle zoomedRect(Rectangle base, int w, int h) {
        double[] hs = Hotspots.forBiome(biome)[Math.max(0, selected)];
        double nx = hs[0] + hs[2] / 2, ny = hs[1] + hs[3] / 2;
        Rectangle bottomTop = withClue() ? clueRect() : optionRect(0);
        int top = (int) Math.round(h * 0.17);
        int free = Math.max(80, bottomTop.y - (withClue() ? backRect().height + 30 : backRect().height + 30) - top);
        int centreY = top + free / 2;
        int centreX = w / 2;
        double zMax = HiddenSceneRenderer.zoomWidth(fitRect(w, h).width) / (double) base.width;
        double z = Math.min(0.46 * free / Math.max(1.0, hs[3] * base.height), 0.42 * w / Math.max(1.0, hs[2] * base.width));
        z = Math.max(1.5, Math.min(zMax, z));
        int zw = (int) Math.round(base.width * z), zh = (int) Math.round(base.height * z);
        int x = (int) Math.round(centreX - nx * zw), y = (int) Math.round(centreY - ny * zh);
        // the animal is kept in the middle even near the scene's edge; only a narrow blurred strip may show beside the scene
        int slackX = (int) (w * 0.28), slackY = (int) (h * 0.12);
        x = zw >= w ? Math.max(w - zw - slackX, Math.min(slackX, x)) : (w - zw) / 2;
        y = zh >= h ? Math.max(h - zh - slackY, Math.min(slackY, y)) : (h - zh) / 2;
        return new Rectangle(x, y, zw, zh);
    }

    private static int lerp(int a, int b, double t) {
        return (int) Math.round(a + (b - a) * t);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        GuiKit.quality(g);
        int w = stW();
        int h = stH();
        int ox = ox(), oy = oy();
        if (biome == null) {
            g.dispose();
            return;
        }

        boolean[] found = new boolean[biome.getAnimals().size()];
        for (int i = 0; i < found.length; i++) found[i] = isFound(i);
        Rectangle base = stageRect(w, h);
        Rectangle panel = new Rectangle(-ox, -oy, getWidth(), getHeight());   // the picture may fill the whole window
        Rectangle scene = base;
        int renderWidth = base.width;
        if (zoomV > 0.0005 && selected >= 0) {
            Rectangle target = zoomedRect(base, w, h);
            scene = new Rectangle(lerp(base.x, target.x, zoomV), lerp(base.y, target.y, zoomV),
                    lerp(base.width, target.width, zoomV), lerp(base.height, target.height, zoomV));
            renderWidth = HiddenSceneRenderer.zoomWidth(fitRect(w, h).width);
        }
        Graphics2D gs = (Graphics2D) g.create();
        gs.translate(ox, oy);                           // from here on: stage coordinates
        HiddenSceneRenderer.drawView(gs, biome, panel, scene, renderWidth, finishing, found, false);
        paintBorder(gs, scene, 1.0);                    // the border zooms together with the picture
        imgRect = new int[]{scene.x + ox, scene.y + oy, scene.width, scene.height};   // in window coordinates (taps)

        if (debug) {
            for (int i = 0; i < biome.getAnimals().size(); i++) {
                Rectangle r = rect(i);
                g.setColor(Color.YELLOW);
                g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                        10f, new float[]{6f, 5f}, 0f));
                g.drawRect(r.x, r.y, r.width, r.height);
                g.setFont(new Font(ZooQuestGUI.FONT_NAME, Font.BOLD, 12));
                g.drawString(biome.getAnimals().get(i).getName(), r.x + 3, r.y + 14);
            }
        }

        paintTopTexts(gs, w, h);
        if (withClue() && riseV > 0.002 && selected >= 0) paintClueBox(gs, h + oy);
        gs.dispose();
        g.dispose();
    }

    // A frame around the scene picture (it moves and grows with the scene while zooming).
    private static void paintBorder(Graphics2D g0, Rectangle scene, double alpha) {
        if (alpha <= 0.01) return;
        Graphics2D g = (Graphics2D) g0.create();
        g.setComposite(AlphaComposite.SrcOver.derive((float) alpha));
        g.setColor(new Color(0, 0, 0, 150));
        g.setStroke(new BasicStroke(8f));
        g.drawRect(scene.x - 1, scene.y - 1, scene.width + 1, scene.height + 1);
        g.setColor(ZooQuestGUI.GOLD);
        g.setStroke(new BasicStroke(4f));
        g.drawRect(scene.x - 1, scene.y - 1, scene.width + 1, scene.height + 1);
        g.dispose();
    }

    // "Animals Found n/10" in the middle and the clue mode on the right, drawn straight on the scene (white with a shadow).
    private void paintTopTexts(Graphics2D g, int w, int h) {
        int big = (int) Math.max(20 * TEXT_SIZE, Math.min(42 * TEXT_SIZE, h * 0.045 * TEXT_SIZE));
        int small = (int) Math.max(15 * TEXT_SIZE, Math.min(32 * TEXT_SIZE, h * 0.03 * TEXT_SIZE));
        int y = Math.max(34, (int) Math.round(h * 0.08));
        shadowed(g, "Animals Found", new Font(ZooQuestGUI.FONT_NAME, Font.BOLD, big), w / 2, y, 0);
        shadowed(g, gm.getFoundCount() + "/" + gm.getTotalCount(), new Font(ZooQuestGUI.FONT_NAME, Font.PLAIN, big), w / 2,
                y + (int) (big * 1.2), 0);
        if (!finishing) {
            String mode = Flow.withClues ? "With Clue" : "Without Clue";
            int right = w - (int) Math.round(108 * sx());
            shadowed(g, "Mode: " + mode, new Font(ZooQuestGUI.FONT_NAME, Font.BOLD, small), right, y, 1);
        }
    }

    // White text with a 40% shadow under it. align: 0 centre, 1 right.
    private static void shadowed(Graphics2D g0, String text, Font font, int x, int baseline, int align) {
        Graphics2D g = (Graphics2D) g0.create();
        GuiKit.quality(g);
        g.setFont(font);
        int tw = g.getFontMetrics().stringWidth(text);
        int px = align == 0 ? x - tw / 2 : x - tw;
        g.setColor(Color.WHITE);
        GuiKit.shadowString(g, text, px, baseline);
        g.dispose();
    }

    // The clue box (With Clues mode), sliding up with the answers.
    private void paintClueBox(Graphics2D g0, int h) {
        Graphics2D g = (Graphics2D) g0.create();
        GuiKit.quality(g);
        Rectangle r = clueRect();
        double e = 1 - riseV;
        int drop = (int) Math.round(e * (h - Math.min(backRect().y, h) + 30));
        r.y += drop;
        g.setColor(new Color(12, 32, 22, 238));
        g.fillRoundRect(r.x, r.y, r.width, r.height, 24, 24);
        g.setColor(ZooQuestGUI.GOLD);
        g.setStroke(new BasicStroke(2.2f));
        g.drawRoundRect(r.x + 1, r.y + 1, r.width - 3, r.height - 3, 24, 24);
        int pad = Math.max(14, r.width / 20);
        g.setColor(ZooQuestGUI.GOLD);
        g.setFont(new Font(ZooQuestGUI.FONT_NAME, Font.BOLD, Math.max(15, Math.min(24, r.height / 7))));
        GuiKit.shadowString(g, "Clue", r.x + pad, r.y + pad + g.getFontMetrics().getAscent());
        String text = answeredText != null ? answeredText : clueText;
        int top = r.y + pad + g.getFontMetrics().getHeight() + 4;
        int availW = r.width - 2 * pad, availH = r.y + r.height - pad - top;
        int size = Math.max(14, Math.min(24, r.height / 8));
        List<String> lines;
        FontMetrics fm;
        while (true) {
            g.setFont(new Font(ZooQuestGUI.FONT_NAME, Font.PLAIN, size));
            fm = g.getFontMetrics();
            lines = wrap(text, fm, availW);
            if (lines.size() * fm.getHeight() <= availH || size <= 11) break;
            size--;
        }
        g.setColor(Color.WHITE);
        int ty = top + fm.getAscent();
        for (String line : lines) {
            g.drawString(line, r.x + pad, ty);
            ty += fm.getHeight();
        }
        g.dispose();
    }

    private static List<String> wrap(String text, FontMetrics fm, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String test = line.length() == 0 ? word : line + " " + word;
            if (fm.stringWidth(test) > width && line.length() > 0) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(test);
            }
        }
        if (line.length() > 0) lines.add(line.toString());
        return lines;
    }

    // One of the four answers (text only), as a rounded box.
    private static final class OptionButton extends JButton {
        private boolean wrong, correct;

        OptionButton() {
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        void reset(String text) {
            wrong = correct = false;
            setText(text);
            setEnabled(true);
        }

        void markWrong() {
            wrong = true;
            setEnabled(false);
            repaint();
        }

        void markCorrect() {
            correct = true;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            GuiKit.quality(g);
            int w = getWidth(), h = getHeight();
            boolean hover = isEnabled() && getModel().isRollover();
            Color fill = correct ? new Color(34, 130, 70, 245)
                    : wrong ? new Color(120, 40, 40, 235)
                    : hover ? new Color(46, 92, 64, 245) : new Color(17, 43, 31, 232);
            g.setColor(fill);
            g.fillRoundRect(0, 0, w, h, 24, 24);
            g.setColor(correct ? new Color(139, 242, 151) : wrong ? new Color(220, 120, 120)
                    : hover ? Color.WHITE : ZooQuestGUI.GOLD);
            g.setStroke(new BasicStroke(hover || correct ? 3.5f : 2.2f));
            g.drawRoundRect(1, 1, w - 3, h - 3, 24, 24);

            String text = getText() == null ? "" : getText();
            int size = Math.max(14, Math.min(26, h / 4));
            FontMetrics fm;
            List<String> lines;
            while (true) {                                          // shrink, then wrap onto two lines
                g.setFont(new Font(ZooQuestGUI.FONT_NAME, Font.BOLD, size));
                fm = g.getFontMetrics();
                lines = new ArrayList<>(List.of(text));
                if (fm.stringWidth(text) <= w - 18) break;
                lines = wrap(text, fm, w - 18);
                boolean fits = true;
                for (String l : lines) if (fm.stringWidth(l) > w - 18) fits = false;
                if (fits && lines.size() * fm.getHeight() <= h - 10) break;
                if (size <= 11) break;
                size--;
            }
            g.setColor(wrong ? new Color(235, 190, 190) : Color.WHITE);
            int total = lines.size() * fm.getHeight();
            int ty = (h - total) / 2 + fm.getAscent();
            for (String line : lines) {
                g.drawString(line, (w - fm.stringWidth(line)) / 2, ty);
                ty += fm.getHeight();
            }
            g.dispose();
        }
    }
}

package main.UI;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
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
- Hidden-animal seek-and-find screen.
- The top strip shows the animals to locate. Their sprites are drawn into the
biome scene using the same hotspot rectangles that receive mouse clicks.
*/
public class GamePanel extends ScenePanel implements ZooQuestGUI.Refreshable {
    private final ZooQuestGUI app;
    private final GameManager gm;

    private final GuiKit.Btn exitBtn = new GuiKit.Btn("Exit Biome", GuiKit.GREY, 145, 40, 15);
    private final JLabel foundCount = GuiKit.label("0 / 10", 25, true, Color.WHITE);
    private final JLabel modeLabel = new JLabel(" ");
    private static final String START_HINT = "Find each animal hidden in the environment. Click an animal when you spot it!";
    private final JLabel instruction = GuiKit.label(START_HINT, 15, true, Color.WHITE);
    private final JPanel targetStrip = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 5)) {
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            GuiKit.quality(g);
            g.setColor(new Color(7, 25, 17, 188));
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
            g.setColor(new Color(255, 214, 102, 125));
            g.setStroke(new BasicStroke(1.3f));
            g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 20, 20);
            g.dispose();
        }
    };
    private final JPanel header;
    private final JPanel topStack = GuiKit.transparent(new BorderLayout(0, 5));
    private final JPanel footer = new JPanel(new BorderLayout()) {
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            GuiKit.quality(g);
            GuiKit.plate(g, 0, 0, getWidth(), getHeight(), 22);
            g.dispose();
        }
    };
    private Biome biome;
    private boolean finishing;
    private boolean nextWasLocked;
    private boolean debug;
    private int hover = -1;
    private int[] imgRect = {0, 0, 1, 1};
    private javax.swing.Timer timer;
    private List<TargetChip> targetChips = new ArrayList<>();

    public GamePanel(ZooQuestGUI app) {
        this.app = app;
        this.gm = app.getGameManager();
        setBorder(BorderFactory.createEmptyBorder(14, 22, 14, 22));

        header = new JPanel(new BorderLayout(10, 0)) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                GuiKit.quality(g);
                g.setColor(new Color(5, 22, 14, 195));
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                g.setColor(new Color(255, 214, 102, 155));
                g.setStroke(new BasicStroke(1.4f));
                g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 24, 24);
                g.dispose();
            }
        };
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 14));

        JPanel west = GuiKit.transparent(new FlowLayout(FlowLayout.LEFT, 0, 0));
        west.add(exitBtn);
        west.setPreferredSize(new Dimension(158, 42));

        JPanel centre = GuiKit.transparent(new GridLayout(2, 1, 0, 0));
        JLabel title = GuiKit.label("Find It! – Spot All Hidden Animals", 21, true, ZooQuestGUI.GOLD);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        foundCount.setHorizontalAlignment(SwingConstants.CENTER);
        centre.add(title);
        centre.add(foundCount);

        modeLabel.setForeground(Color.WHITE);
        modeLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        modeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        modeLabel.setPreferredSize(new Dimension(170, 40));

        header.add(west, BorderLayout.WEST);
        header.add(centre, BorderLayout.CENTER);
        header.add(modeLabel, BorderLayout.EAST);

        targetStrip.setOpaque(false);
        targetStrip.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        targetStrip.setPreferredSize(new Dimension(600, 68));
        topStack.add(header, BorderLayout.NORTH);
        topStack.add(targetStrip, BorderLayout.SOUTH);
        add(topStack, BorderLayout.NORTH);

        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(9, 16, 9, 16));
        footer.add(instruction, BorderLayout.CENTER);
        instruction.setHorizontalAlignment(SwingConstants.CENTER);
        add(footer, BorderLayout.SOUTH);

        exitBtn.addActionListener(e -> exitBiome());

        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int hit = !finishing ? hit(e.getPoint()) : -1;
                if (hit != hover) {
                    hover = hit;
                    setCursor(Cursor.getPredefinedCursor(hit >= 0 ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                    repaint();
                }
            }

            @Override public void mouseClicked(MouseEvent e) {
                if (finishing) return;
                int hit = hit(e.getPoint());
                if (hit >= 0) findAnimal(hit);
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
        finishing = false;
        hover = -1;
        instruction.setText(START_HINT);
        int currentIndex = gm.getCurrentBiomeIndex();
        nextWasLocked = currentIndex + 1 < gm.getAllBiomes().size()
                && gm.getAllBiomes().get(currentIndex + 1).isLocked();
        updateTargetStrip();
        updateChrome();
        repaint();
    }

    private void updateChrome() {
        if (biome == null) return;
        foundCount.setText(gm.getFoundCount() + " / " + gm.getTotalCount() + " animals found");
        modeLabel.setText("<html><div style='text-align:right'><b>Mode</b><br>" +
                (Flow.withClues ? "With Clues" : "No Clues") + "</div></html>");
        modeLabel.setVisible(!finishing);
        exitBtn.setVisible(true);
        topStack.revalidate();
        topStack.repaint();
    }

    private void updateTargetStrip() {
        targetStrip.removeAll();
        targetChips = new ArrayList<>();
        if (biome == null) return;
        for (Animal animal : biome.getAnimals()) {
            TargetChip chip = new TargetChip(animal);
            targetChips.add(chip);
            targetStrip.add(chip);
        }
        targetStrip.revalidate();
        targetStrip.repaint();
    }

    private void refreshTargetStrip() {
        for (TargetChip chip : targetChips) chip.repaint();
        targetStrip.repaint();
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
            gm.exitCurrentBiome();
            app.show(ZooQuestGUI.BIOMES);
        }
    }

    // ------------------------------------------------------------ hotspots

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

    // ------------------------------------------------------------ find animal

    private void findAnimal(int index) {
        Animal animal = biome.getAnimals().get(index);
        if (gm.isAnimalFound(animal.getName())) return;

        try {
            // Keep the existing GameManager rules: selecting a target reveals a clue
            // in With Clues mode, and preserves eligibility for the No-Clue badge otherwise.
            MysteryClue clue = gm.selectHiddenAnimal(animal.getName());
            boolean clueWasShown = gm.isClueShown();
            GameManager.GuessResult result = gm.processGuess(animal.getName());
            if (!result.correct()) return;

            hover = -1;
            feedbackText(animal, clueWasShown, clue);
            updateChrome();
            refreshTargetStrip();
            repaint();

            if (result.biomeCompleted()) finish(result);
        } catch (AnimalNotFoundException | IllegalStateException ex) {
            GameDialog.show(this, "Unable to find animal", ex.getMessage(), "Continue");
        }
    }

    private void feedbackText(Animal animal, boolean clueShown, MysteryClue clue) {
        if (clueShown) {
            instruction.setText("Found " + animal.getName() + "! Clue: " + clue.getHintText());
        } else {
            instruction.setText("Great find! You discovered the " + animal.getName() + ". " +
                    animal.getRevealMessage());
        }
    }

    private void finish(GameManager.GuessResult result) {
        int index = gm.getCurrentBiomeIndex();
        Flow.biome = biome;
        Flow.completedIndex = index;
        Flow.hasNext = gm.hasNextBiome();
        Flow.nextIndex = index + 1;
        Flow.newlyUnlocked = Flow.hasNext && nextWasLocked;
        Flow.wrongGuesses = gm.getWrongGuessCount();
        Flow.cluesUsed = gm.hasUsedClues();
        Flow.newBadges = new ArrayList<>(result.newAchievements());

        finishing = true;
        updateChrome();
        instruction.setText("Amazing! You found every animal in " + biome.getBiomeName() + "!");
        repaint();
        refreshTargetStrip();
        timer = new javax.swing.Timer(1800, e -> toComplete());
        timer.setRepeats(false);
        timer.start();
    }

    private void toComplete() {
        if (!finishing) return;
        // the finished picture stays on screen while the window dims out; the game is closed once it is black
        app.showWithDim(ZooQuestGUI.COMPLETE, () -> {
            finishing = false;
            gm.exitCurrentBiome(); // GameManager has already saved a completed biome.
        });
    }

    // ------------------------------------------------------------ painting

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        GuiKit.quality(g);
        int w = getWidth();
        int h = getHeight();
        if (biome == null) {
            g.dispose();
            return;
        }

        boolean[] found = new boolean[biome.getAnimals().size()];
        for (int i = 0; i < found.length; i++) found[i] = isFound(i);
        int sceneTop = topStack.getY() + topStack.getHeight() + 4;
        int sceneBottom = footer.getY() - 4;
        if (sceneBottom - sceneTop < 120) { sceneTop = 0; sceneBottom = h; }   // not laid out yet / tiny window
        imgRect = HiddenSceneRenderer.drawScene(g, biome, new Rectangle(0, 0, w, h),
                new Rectangle(0, sceneTop, w, sceneBottom - sceneTop), finishing, found);

        if (!finishing && hover >= 0 && hover < biome.getAnimals().size()) {
            Rectangle r = rect(hover);
            g.setColor(ZooQuestGUI.GOLD);
            g.setStroke(new BasicStroke(3.5f));
            g.drawRoundRect(r.x - 3, r.y - 3, r.width + 6, r.height + 6, 22, 22);
        }

        if (debug) {
            for (int i = 0; i < biome.getAnimals().size(); i++) {
                Rectangle r = rect(i);
                g.setColor(Color.YELLOW);
                g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                        10f, new float[]{6f, 5f}, 0f));
                g.drawRect(r.x, r.y, r.width, r.height);
                g.setFont(new Font("SansSerif", Font.BOLD, 12));
                g.drawString(biome.getAnimals().get(i).getName(), r.x + 3, r.y + 14);
            }
        }
        g.dispose();
    }

    // Small target card in the top row. A green check appears after the animal is found.
    private class TargetChip extends JComponent {
        private final Animal animal;

        TargetChip(Animal animal) {
            this.animal = animal;
            setPreferredSize(new Dimension(45, 42));
            setMinimumSize(new Dimension(45, 42));
            setToolTipText(animal.getName());
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            GuiKit.quality(g);
            int w = getWidth();
            int h = getHeight();
            boolean found = gm.isAnimalFound(animal.getName());

            g.setColor(found ? new Color(34, 112, 64, 235) : new Color(17, 43, 31, 225));
            g.fillRoundRect(0, 0, w, h, 15, 15);
            g.setColor(found ? new Color(139, 242, 151) : new Color(255, 214, 102, 190));
            g.setStroke(new BasicStroke(found ? 2.5f : 1.3f));
            g.drawRoundRect(1, 1, w - 3, h - 3, 15, 15);

            BufferedImage sprite = GuiImages.load(GuiImages.animalFile(animal));
            if (sprite == null) sprite = GuiImages.load(animal.getSpriteTemplate());
            if (sprite != null) {
                int areaW = w - 12;
                int areaH = h - 10;
                double scale = Math.min(areaW / (double) sprite.getWidth(), areaH / (double) sprite.getHeight());
                int dw = Math.max(1, (int) (sprite.getWidth() * scale));
                int dh = Math.max(1, (int) (sprite.getHeight() * scale));
                Composite original = g.getComposite();
                g.setComposite(AlphaComposite.SrcOver.derive(found ? 1f : 0.88f));
                g.drawImage(sprite, (w - dw) / 2, (h - dh) / 2, dw, dh, null);
                g.setComposite(original);
            }

            if (found) {
                int size = 19;
                g.setColor(new Color(41, 190, 86));
                g.fillOval(w - size, -1, size, size);
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(2.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawPolyline(new int[]{w - 14, w - 10, w - 4}, new int[]{8, 12, 4}, 3);
            }
            g.dispose();
        }
    }
}



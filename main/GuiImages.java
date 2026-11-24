package main;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public final class GuiImages {
    public static final String HIDDEN_FILE = "hidden_animal.png";
    public static final String LOCK_FILE = "lock.png";


    private static final Map<String, BufferedImage> CACHE = new HashMap<>();
    private GuiImages() { }

    public static String slug (String s) {
        return s.toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
    }

    /**
     * Animal artwork in this project is stored directly in main/resources/.
     * A few display names differ from the filenames, so map those explicitly.
     */
    public static String animalFile(Animal a) {
        String name = a.getName().trim().toLowerCase(java.util.Locale.ROOT);
        switch (name) {
            case "hippopotamus": return "hippo.png";
            case "nile crocodile": return "crocodile.png";
            case "sea lion": return "sealion.png";
            case "octupus":
            case "octopus": return "octopus.png";
            case "stringray": return "sting_ray.png";
            case "manta ray": return "manta_ray.png";
            case "komodo dragon": return "komodo dragon.png";
            default: return slug(name) + ".png";
        }
    }
    
    public static String biomeCardFile(Biome b) { return "biomes/" + slug(b.getBiomeName()) + ".png"; }
    public static String badgeFile(String biomeName, boolean noClue) {
        return "images/badges/" + biomeName + (noClue ? "_no_clue" : "") + ".png";   // same path GameManager stores
    }

    // ---- scene pictures: clean = no animals, hidden = animals hidden in it, complete = all animals visible
    public static String sceneFile(Biome b, String kind) { return "backgrounds/" + slug(b.getBiomeName()) + "_" + kind + ".png"; }
    public static String zoomFile(Animal a) { return "zoom/" + slug(a.getName()) + ".png"; }

    /** kind = "clean" | "hidden" | "complete"; falls back through the other kinds, then Biome.getBackgroundTemplate(). */
    public static BufferedImage scene(Biome b, String kind) {
        String[] order = kind.equals("complete") ? new String[]{"complete", "hidden", "clean"}
                : kind.equals("clean") ? new String[]{"clean", "hidden", "complete"} : new String[]{"hidden", "complete", "clean"};
        for (String k : order) { BufferedImage i = load(sceneFile(b, k)); if (i != null) return i; }
        return load(b.getBackgroundTemplate());
    }

    /** Draws the image over the whole w x h area (cover, cropped). Returns {x, y, drawnWidth, drawnHeight}. */
    public static int[] drawFull(Graphics2D g, BufferedImage img, int w, int h) {
        double s = Math.max(w / (double) img.getWidth(), h / (double) img.getHeight());
        int sw = (int) Math.ceil(img.getWidth() * s), sh = (int) Math.ceil(img.getHeight() * s);
        int x = (w - sw) / 2, y = (h - sh) / 2;
        g.drawImage(img, x, y, sw, sh, null);
        return new int[]{x, y, sw, sh};
    }

    /** Badge picture for an Achievement (works for loaded saves too). */
    public static void drawBadgeFor(Graphics2D g, Achievement a, int x, int y, int size) {
        String n = a.getBadgeName();
        boolean noClue = n.endsWith("_no_clue_badge");
        String biome = noClue ? n.substring(0, n.length() - "_no_clue_badge".length())
                : n.endsWith("_badge") ? n.substring(0, n.length() - "_badge".length()) : n;
        drawBadge(g, biome, noClue, true, x, y, size);
    }

    public static BufferedImage load(String file) {
        if (file == null) return null;
        if (CACHE.containsKey(file)) return CACHE.get(file);
        BufferedImage img = ZooQuestGUI.loadImage(file);
        CACHE.put(file, img);
        return img;
    }

    public static void clearCache() { CACHE.clear(); }

    // ------------------------------------------------------------ colours

    public static Color typeColor(String type) {
        switch (type) {
            case "Safari": return new Color(214, 130, 50);
            case "Aquatic": return new Color(40, 130, 200);
            default: return new Color(46, 150, 80);
        }
    }

    // ------------------------------------------------------------ drawing

    /** Draws the image scaled to cover the rectangle (cropped), with rounded corners. */
    public static void drawCover(Graphics2D g0, BufferedImage img, int x, int y, int w, int h, int arc) {
        Graphics2D g = (Graphics2D) g0.create();
        GuiKit.quality(g);
        g.setClip(new RoundRectangle2D.Float(x, y, w, h, arc, arc));
        double s = Math.max(w / (double) img.getWidth(), h / (double) img.getHeight());
        int sw = (int) Math.ceil(img.getWidth() * s), sh = (int) Math.ceil(img.getHeight() * s);
        g.drawImage(img, x + (w - sw) / 2, y + (h - sh) / 2, sw, sh, null);
        g.dispose();
    }

    /** Placeholder tile: gradient with one big letter. */
    public static void drawPlaceholder(Graphics2D g0, String letter, Color base, int x, int y, int w, int h, int arc) {
        Graphics2D g = (Graphics2D) g0.create();
        GuiKit.quality(g);
        g.setPaint(new GradientPaint(x, y, base.brighter(), x + w, y + h, base.darker()));
        g.fillRoundRect(x, y, w, h, arc, arc);
        g.setColor(new Color(255, 255, 255, 230));
        g.setFont(new Font("SansSerif", Font.BOLD, Math.max(12, Math.min(w, h) / 2)));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(letter, x + (w - fm.stringWidth(letter)) / 2, y + (h - fm.getHeight()) / 2 + fm.getAscent());
        g.dispose();
    }

    /** animals/<name>.png, else the animal type's sprite template, else a lettered placeholder. */
    public static void drawAnimal(Graphics2D g, Animal a, int x, int y, int w, int h, int arc) {
        BufferedImage img = load(animalFile(a));
        if (img == null) img = load(a.getSpriteTemplate());
        if (img != null) drawCover(g, img, x, y, w, h, arc);
        else drawPlaceholder(g, a.getName().substring(0, 1).toUpperCase(), typeColor(a.getAnimalType()), x, y, w, h, arc);
    }

    /** hidden_animal.png, else a dark tile with a "?". */
    public static void drawHidden(Graphics2D g0, int x, int y, int w, int h, int arc) {
        BufferedImage img = load(HIDDEN_FILE);
        if (img != null) { drawCover(g0, img, x, y, w, h, arc); return; }
        Graphics2D g = (Graphics2D) g0.create();
        GuiKit.quality(g);
        g.setPaint(new GradientPaint(x, y, new Color(60, 80, 66), x, y + h, new Color(28, 44, 34)));
        g.fillRoundRect(x, y, w, h, arc, arc);
        g.setColor(ZooQuestGUI.GOLD);
        g.setFont(new Font("SansSerif", Font.BOLD, Math.max(16, Math.min(w, h) * 6 / 10)));
        FontMetrics fm = g.getFontMetrics();
        g.drawString("?", x + (w - fm.stringWidth("?")) / 2, y + (h - fm.getHeight()) / 2 + fm.getAscent());
        g.dispose();
    }

    /** lock.png, else a drawn padlock, centred on (cx, cy). */
    public static void drawLock(Graphics2D g, int cx, int cy, int size) {
        BufferedImage img = load(LOCK_FILE);
        if (img != null) {
            double s = size / (double) Math.max(img.getWidth(), img.getHeight());
            int w = (int) (img.getWidth() * s), h = (int) (img.getHeight() * s);
            g.drawImage(img, cx - w / 2, cy - h / 2, w, h, null);
        } else {
            GuiKit.padlock(g, cx, cy, size);
        }
    }

    /** Badge picture (images/badges/...), else a medal with a star. Locked badges are dimmed with a padlock. */
    public static void drawBadge(Graphics2D g0, String biomeName, boolean noClue, boolean earned, int x, int y, int size) {
        Graphics2D g = (Graphics2D) g0.create();
        GuiKit.quality(g);
        BufferedImage img = load(badgeFile(biomeName, noClue));
        if (img != null) {
            drawCover(g, img, x, y, size, size, size / 4);
        } else {
            Color ring = noClue ? new Color(120, 90, 200) : ZooQuestGUI.GOLD;
            g.setPaint(new GradientPaint(x, y, ring.brighter(), x + size, y + size, ring.darker()));
            g.fillOval(x, y, size, size);
            g.setColor(new Color(20, 40, 28, 220));
            g.fillOval(x + size / 8, y + size / 8, size * 3 / 4, size * 3 / 4);
            GuiKit.star(g, x + size / 2, y + size / 2, size / 4, ring.brighter());
            if (noClue) {   // a small bar marks the no-clue badge
                g.setColor(ring.brighter());
                g.setStroke(new BasicStroke(Math.max(2f, size / 16f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawLine(x + size / 4, y + size * 4 / 5, x + size * 3 / 4, y + size * 4 / 5);
            }
        }
        if (!earned) {
            g.setColor(new Color(10, 20, 14, 175));
            if (img != null) g.fillRoundRect(x, y, size, size, size / 4, size / 4);
            else g.fillOval(x, y, size, size);
            drawLock(g, x + size / 2, y + size / 2, size / 2);
        }
        g.dispose();
    }
    
}

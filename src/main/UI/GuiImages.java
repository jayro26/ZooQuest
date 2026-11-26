package main.UI;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import main.Achievement;
import main.Animal;
import main.Biome;

public final class GuiImages {
    public static final String HIDDEN_FILE = "hidden_animal.png";
    public static final String LOCK_FILE = "lock.png";

    private static final Map<String, BufferedImage> CACHE = new HashMap<>();
    private GuiImages() { }

    public static String slug (String s) {
        return s.toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
    }

    /*
    - Animal artwork in this project is stored directly in main/resources/.
    - A few display names differ from the filenames, so map those explicitly.
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

    // kind = "clean" | "hidden" | "complete"; falls back through the other kinds, then Biome.getBackgroundTemplate().
    public static BufferedImage scene(Biome b, String kind) {
        String[] order = kind.equals("complete") ? new String[]{"complete", "hidden", "clean"}
                : kind.equals("clean") ? new String[]{"clean", "hidden", "complete"} : new String[]{"hidden", "complete", "clean"};
        for (String k : order) { BufferedImage i = load(sceneFile(b, k)); if (i != null) return i; }
        return load(b.getBackgroundTemplate());
    }

    // Draws the image over the whole w x h area (cover, cropped). Returns {x, y, drawnWidth, drawnHeight}.
    public static int[] drawFull(Graphics2D g, BufferedImage img, int w, int h) {
        double s = Math.max(w / (double) img.getWidth(), h / (double) img.getHeight());
        int sw = (int) Math.ceil(img.getWidth() * s), sh = (int) Math.ceil(img.getHeight() * s);
        int x = (w - sw) / 2, y = (h - sh) / 2;
        g.drawImage(img, x, y, sw, sh, null);
        return new int[]{x, y, sw, sh};
    }

    // Badge picture for an Achievement (works for loaded saves too).
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

    // Draws the image scaled to cover the rectangle (cropped), with rounded corners. 
    public static void drawCover(Graphics2D g0, BufferedImage img, int x, int y, int w, int h, int arc) {
        Graphics2D g = (Graphics2D) g0.create();
        GuiKit.quality(g);
        g.setClip(new RoundRectangle2D.Float(x, y, w, h, arc, arc));
        double s = Math.max(w / (double) img.getWidth(), h / (double) img.getHeight());
        int sw = (int) Math.ceil(img.getWidth() * s), sh = (int) Math.ceil(img.getHeight() * s);
        g.drawImage(img, x + (w - sw) / 2, y + (h - sh) / 2, sw, sh, null);
        g.dispose();
    }

    // Placeholder tile: gradient with one big letter.
    public static void drawPlaceholder(Graphics2D g0, String letter, Color base, int x, int y, int w, int h, int arc) {
        Graphics2D g = (Graphics2D) g0.create();
        GuiKit.quality(g);
        g.setPaint(new GradientPaint(x, y, base.brighter(), x + w, y + h, base.darker()));
        g.fillRoundRect(x, y, w, h, arc, arc);
        g.setColor(new Color(255, 255, 255, 230));
        g.setFont(new Font(ZooQuestGUI.FONT_NAME, Font.BOLD, Math.max(12, Math.min(w, h) / 2)));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(letter, x + (w - fm.stringWidth(letter)) / 2, y + (h - fm.getHeight()) / 2 + fm.getAscent());
        g.dispose();
    }

    /*
    - animals/<name>.png, else the animal type's sprite template, else a lettered placeholder.
    - the whole animal is now shown (fit inside the box, never cropped). It used to go through
    drawCover, which scales the picture until it fills the box and cuts off whatever sticks out
    (tiger's head, eagle's wings, koala's ears ...).
    */
    public static void drawAnimal(Graphics2D g, Animal a, int x, int y, int w, int h, int arc) {
        drawAnimal(g, a, x, y, w, h, arc, 0);
    }


    public static void drawAnimal(Graphics2D g0, Animal a, int x, int y, int w, int h, int arc, int bottomReserve) {
        drawAnimal(g0, a, x, y, w, h, arc, bottomReserve, 1.0);
    }

    // Same, with the picture drawn at {@code scale} (0.8 = 80 %) of the room it would otherwise take, still centred.
    public static void drawAnimal(Graphics2D g0, Animal a, int x, int y, int w, int h, int arc, int bottomReserve,
                                  double scale) {
        BufferedImage img = load(animalFile(a));
        if (img == null) img = load(a.getSpriteTemplate());
        if (img == null) {
            drawPlaceholder(g0, a.getName().substring(0, 1).toUpperCase(), typeColor(a.getAnimalType()), x, y, w, h, arc);
            return;
        }
        Graphics2D g = (Graphics2D) g0.create();
        GuiKit.quality(g);
        g.setPaint(new GradientPaint(x, y, new Color(52, 78, 60), x, y + h, new Color(24, 40, 30)));
        g.fillRoundRect(x, y, w, h, arc, arc);
        int pad = Math.max(6, Math.min(w, h) / 24);
        int rw = w - 2 * pad, rh = h - 2 * pad - Math.max(0, bottomReserve);
        int sw = Math.max(1, (int) Math.round(rw * scale)), sh = Math.max(1, (int) Math.round(rh * scale));
        drawContain(g, img, x + pad + (rw - sw) / 2, y + pad + (rh - sh) / 2, sw, sh);
        g.dispose();
    }

    /*
    - draws the picture as large as possible while staying completely inside the rectangle,
    centred, keeping its proportions. Transparent margins of the file are ignored, so a sprite that has
    empty space around the animal (whale, swordfish) is not drawn tiny.
    */
    public static void drawContain(Graphics2D g, BufferedImage img, int x, int y, int w, int h) {
        if (w < 1 || h < 1) return;
        Rectangle box = opaqueBounds(img);
        double s = Math.min(w / (double) box.width, h / (double) box.height);
        int dw = Math.max(1, (int) Math.round(box.width * s));
        int dh = Math.max(1, (int) Math.round(box.height * s));
        BufferedImage part = img.getSubimage(box.x, box.y, box.width, box.height);
        g.drawImage(smooth(part, dw, dh), x + (w - dw) / 2, y + (h - dh) / 2, null);
    }

    // rectangle of the visible (not transparent) pixels, worked out once per picture
    private static final Map<BufferedImage, Rectangle> OPAQUE = new java.util.IdentityHashMap<>();

    public static synchronized Rectangle opaqueBounds(BufferedImage img) {
        Rectangle cached = OPAQUE.get(img);
        if (cached != null) return cached;
        int w = img.getWidth(), h = img.getHeight();
        int minX = w, minY = h, maxX = -1, maxY = -1;
        if (img.getColorModel().hasAlpha()) {
            int[] row = new int[w];
            for (int yy = 0; yy < h; yy++) {
                img.getRGB(0, yy, w, 1, row, 0, w);
                for (int xx = 0; xx < w; xx++) {
                    if ((row[xx] >>> 24) > 12) {
                        if (xx < minX) minX = xx;
                        if (xx > maxX) maxX = xx;
                        if (yy < minY) minY = yy;
                        if (yy > maxY) maxY = yy;
                    }
                }
            }
        }
        Rectangle r = maxX < 0 ? new Rectangle(0, 0, w, h)
                : new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
        OPAQUE.put(img, r);
        return r;
    }

    // the picture as a flat black shape (same outline, same transparency): the "hidden" look of an animal
    private static final Map<BufferedImage, BufferedImage> BLACKOUT = new java.util.IdentityHashMap<>();

    public static synchronized BufferedImage blackout(BufferedImage img) {
        BufferedImage cached = BLACKOUT.get(img);
        if (cached != null) return cached;
        BufferedImage out = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
        int[] row = new int[img.getWidth()];
        for (int y = 0; y < img.getHeight(); y++) {
            img.getRGB(0, y, img.getWidth(), 1, row, 0, img.getWidth());
            for (int x = 0; x < row.length; x++) row[x] &= 0xFF000000;   // keep only the transparency
            out.setRGB(0, y, img.getWidth(), 1, row, 0, img.getWidth());
        }
        BLACKOUT.put(img, out);
        return out;
    }

    // big pictures are shrunk in halving steps (much smoother than one jump from 2000 px to 200 px);
    // the last few results are kept so hovering a tile does not redo the work
    private static final Map<String, BufferedImage> SMOOTH = new java.util.LinkedHashMap<>(16, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, BufferedImage> e) { return size() > 80; }
    };

    private static synchronized BufferedImage smooth(BufferedImage src, int w, int h) {
        String key = System.identityHashCode(src) + ":" + src.getWidth() + "x" + src.getHeight() + ">" + w + "x" + h;
        BufferedImage hit = SMOOTH.get(key);
        if (hit != null) return hit;
        BufferedImage cur = shrink(src, w, h);
        SMOOTH.put(key, cur);
        return cur;
    }

    // scales the picture to w x h; a big reduction is done in halving steps, which looks much smoother.
    public static BufferedImage shrink(BufferedImage src, int w, int h) {
        BufferedImage cur = src;
        int cw = src.getWidth(), ch = src.getHeight();
        while (cw / 2 >= w && ch / 2 >= h) {
            cw /= 2;
            ch /= 2;
            cur = step(cur, cw, ch);
        }
        if (cw != w || ch != h) cur = step(cur, w, h);
        return cur;
    }

    private static BufferedImage step(BufferedImage src, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    // hidden_animal.png, else a dark tile with a "?".
    public static void drawHidden(Graphics2D g0, int x, int y, int w, int h, int arc) {
        BufferedImage img = load(HIDDEN_FILE);
        if (img != null) { drawCover(g0, img, x, y, w, h, arc); return; }
        Graphics2D g = (Graphics2D) g0.create();
        GuiKit.quality(g);
        g.setPaint(new GradientPaint(x, y, new Color(60, 80, 66), x, y + h, new Color(28, 44, 34)));
        g.fillRoundRect(x, y, w, h, arc, arc);
        g.setColor(ZooQuestGUI.GOLD);
        g.setFont(new Font(ZooQuestGUI.FONT_NAME, Font.BOLD, Math.max(16, Math.min(w, h) * 6 / 10)));
        FontMetrics fm = g.getFontMetrics();
        g.drawString("?", x + (w - fm.stringWidth("?")) / 2, y + (h - fm.getHeight()) / 2 + fm.getAscent());
        g.dispose();
    }

    // lock.png, else a drawn padlock, centred on (cx, cy).
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

    // Badge picture (images/badges/...), else a medal with a star. Locked badges are dimmed with a padlock.
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
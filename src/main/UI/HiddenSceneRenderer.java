package main.UI;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import main.Animal;
import main.Biome;
import main.Hotspots;

public final class HiddenSceneRenderer {
    private static final int MIN_WIDTH = 512;     // composed image width is a multiple of 256 between these two
    private static final int MAX_WIDTH = 3584;    // the zoomed view of the mystery game needs a bigger picture
    private static final Map<String, Composed> CACHE = new java.util.LinkedHashMap<>(4, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, Composed> eldest) {
            return size() > 3;                      // normal size + zoomed size (+ one spare after a window resize)
        }
    };
    private static final java.util.Set<String> MISSING = new java.util.HashSet<>();   // biomes whose pictures are not there

    // Green frame and tick around a found animal. Set to false for the animal to just turn to colour.
    private static final boolean SHOW_FOUND_MARK = true;

    private HiddenSceneRenderer() { }

    // The scene composed at one size, plus a tiny copy used for the blurred border.
    private static final class Composed {
        final int width;
        final String biomeType;
        final BufferedImage scene;      // every animal still hidden (black silhouette)
        final BufferedImage tiny;
        BufferedImage soft;             // the tiny copy enlarged step by step, so the border is smooth, not blocky
        BufferedImage revealed;         // every animal in colour, in its own place among the scenery (built when first needed)
        final Map<String, Patch> patches = new HashMap<>();   // small coloured areas of found animals
        Composed(int width, String biomeType, BufferedImage scene, BufferedImage tiny) {
            this.width = width;
            this.biomeType = biomeType;
            this.scene = scene;
            this.tiny = tiny;
        }
        int height() { return scene.getHeight(); }
    }

    // A small piece of the scene (position in composed pixels) with one or more animals in colour.
    private static final class Patch {
        final int x, y;
        final BufferedImage image;
        Patch(int x, int y, BufferedImage image) { this.x = x; this.y = y; this.image = image; }
    }

    // Draws the scene over the whole w x h area. Returns {x, y, width, height} of the scene picture. 
    public static int[] drawScene(Graphics2D g, Biome biome, int width, int height,
                                  boolean completed, boolean[] found) {
        Rectangle all = new Rectangle(0, 0, width, height);
        return drawScene(g, biome, all, all, completed, found);
    }

    /**
    - Fills {@code panel} with the blurred border and shows the whole scene, as large as possible, centred inside
    {@code area} (for example the part of the game window between the header and the footer).
    - completed = every animal is shown in colour, each in its own place and stacking order (finished game / complete picture).
    - Returns {x, y, width, height} of the scene picture; Hotspots are fractions of that rectangle.
    */
    public static int[] drawScene(Graphics2D g, Biome biome, Rectangle panel, Rectangle area,
                                  boolean completed, boolean[] found) {
        int fitW = (int) Math.min(area.width, Math.floor(area.height * SceneLayouts.ASPECT));
        int fitH = (int) Math.round(fitW / SceneLayouts.ASPECT);
        if (fitW < 16 || fitH < 9) return new int[]{area.x, area.y, 1, 1};
        int x = area.x + (area.width - fitW) / 2;
        int y = area.y + (area.height - fitH) / 2;
        int[] imageRect = {x, y, fitW, fitH};

        Composed composed = compose(biome.getBiomeType(), fitW);
        if (composed == null) {                                   // pictures missing: plain colours + the animal sprites
            drawFallback(g, biome, panel, imageRect, completed, found);
            return imageRect;
        }

        // 1) blurred border over the whole panel: a tiny copy of the scene, stretched (cover) and darkened
        drawBorder(g, composed, panel);

        // 2) the scene itself, whole: animals hidden, or all in colour when the game is completed
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(completed ? revealedOf(composed) : composed.scene, x, y, fitW, fitH, null);
        g.setColor(new Color(0, 0, 0, 90));
        g.setStroke(new BasicStroke(1f));
        g.drawRect(x, y, fitW - 1, fitH - 1);

        // 3) animals the player has found: each black silhouette is replaced by the coloured animal in exactly the same
        //    place and the same stacking order (still behind any tree or rock the PowerPoint puts in front of it).
        if (!completed && found != null) drawFound(g, composed, biome, found, imageRect, SHOW_FOUND_MARK);
        return imageRect;
    }

    public static Rectangle toRectangle(int[] imageRect, double[] hotspot) {
        return new Rectangle(
                imageRect[0] + (int) Math.round(hotspot[0] * imageRect[2]),
                imageRect[1] + (int) Math.round(hotspot[1] * imageRect[3]),
                Math.max(1, (int) Math.round(hotspot[2] * imageRect[2])),
                Math.max(1, (int) Math.round(hotspot[3] * imageRect[3])));
    }

    // ------------------------------------------------------------ composing

    private static synchronized Composed compose(String biomeType, int fitWidth) {
        int width = Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, (int) Math.ceil(fitWidth / 256.0) * 256));
        String type = biomeType == null ? "" : biomeType.toLowerCase();
        String key = type + "@" + width;
        Composed cached = CACHE.get(key);
        if (cached != null) return cached;
        if (MISSING.contains(type)) return null;   // tried before, pictures are missing

        BufferedImage scene = composeLayers(biomeType, width, false);
        if (scene == null) {
            System.err.println("ZooQuest: the scene pictures were not found in "
                    + main.ProjectPaths.resourcesDirectory().resolve("scenes") + " - copy the scenes folder there.");
            MISSING.add(type);
            return null;
        }

        BufferedImage tiny = new BufferedImage(96, 54, BufferedImage.TYPE_INT_RGB);   // blurred by being enlarged again
        Graphics2D tg = tiny.createGraphics();
        tg.setColor(new Color(40, 60, 50));
        tg.fillRect(0, 0, 96, 54);
        tg.drawImage(GuiImages.shrink(scene, 96, 54), 0, 0, null);
        tg.dispose();

        Composed made = new Composed(width, biomeType, scene, tiny);
        CACHE.put(key, made);
        return made;
    }

    // Draws all layers of a biome's scene into an image; animals as silhouettes or in colour. Null if no picture loaded.
    private static BufferedImage composeLayers(String biomeType, int width, boolean inColour) {
        int height = (int) Math.round(width / SceneLayouts.ASPECT);
        BufferedImage scene = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scene.createGraphics();
        GuiKit.quality(g);
        int drawn = 0;
        for (SceneLayouts.Layer layer : SceneLayouts.forType(biomeType)) {
            if (paintLayer(g, layer, 0, 0, width, height, true, !inColour)) drawn++;
        }
        g.dispose();
        return drawn == 0 ? null : scene;
    }

    private static synchronized Patch foundPatch(Composed c, Biome biome, int index, SceneLayouts.Layer layer,
                                                 boolean[] found) {
        int w = c.width, h = c.height();
        Rectangle box = toRectangle(new int[]{0, 0, w, h}, SceneLayouts.bounds(layer));
        Rectangle area = new Rectangle(box.x - 6, box.y - 6, box.width + 12, box.height + 12)
                .intersection(new Rectangle(0, 0, w, h));
        List<Animal> animals = biome.getAnimals();
        // which animals are in colour inside this area (this animal and found animals overlapping it)
        java.util.Set<SceneLayouts.Layer> coloured = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        StringBuilder key = new StringBuilder().append(index).append(':');
        for (int i = 0; i < animals.size(); i++) {
            SceneLayouts.Layer l = SceneLayouts.animalLayer(biome.getBiomeType(), animals.get(i).getName());
            if (l == null) continue;
            boolean on = i == index || (i < found.length && found[i]
                    && area.intersects(toRectangle(new int[]{0, 0, w, h}, SceneLayouts.bounds(l))));
            if (on) { coloured.add(l); key.append(i).append(','); }
        }
        Patch cached = c.patches.get(key.toString());
        if (cached != null) return cached;

        BufferedImage img = new BufferedImage(Math.max(1, area.width), Math.max(1, area.height), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        GuiKit.quality(g);
        for (SceneLayouts.Layer l : SceneLayouts.forType(biome.getBiomeType())) {
            boolean hide = l.animal() != null && !coloured.contains(l);
            paintLayer(g, l, -area.x, -area.y, w, h, true, hide);
        }
        g.dispose();
        Patch made = new Patch(area.x, area.y, img);
        c.patches.put(key.toString(), made);
        return made;
    }

    // Draws the coloured patches (and, if wanted, the green marks) of the found animals over a scene drawn at imageRect.
    private static void drawFound(Graphics2D g, Composed composed, Biome biome, boolean[] found, int[] imageRect,
                                  boolean marks) {
        int x = imageRect[0], y = imageRect[1], fitW = imageRect[2], fitH = imageRect[3];
        List<Animal> animals = biome.getAnimals();
        for (int i = 0; i < animals.size(); i++) {
            if (i >= found.length || !found[i]) continue;
            SceneLayouts.Layer layer = SceneLayouts.animalLayer(biome.getBiomeType(), animals.get(i).getName());
            if (layer == null) continue;
            Patch patch = foundPatch(composed, biome, i, layer, found);
            Graphics2D sg = (Graphics2D) g.create();
            sg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            sg.clipRect(x, y, fitW, fitH);
            sg.translate(x, y);
            sg.scale(fitW / (double) composed.width, fitH / (double) composed.height());
            sg.clip(new Rectangle(patch.x, patch.y, patch.image.getWidth(), patch.image.getHeight()));
            sg.drawImage(patch.image, patch.x, patch.y, null);
            sg.dispose();
            if (marks && SHOW_FOUND_MARK) {
                Graphics2D mg = (Graphics2D) g.create();
                mg.clipRect(x, y, fitW, fitH);
                drawFoundMark(mg, toRectangle(imageRect, SceneLayouts.bounds(layer)));
                mg.dispose();
            }
        }
    }

    // The tiny copy enlarged to 384x216 in smooth steps and blurred, so the border looks soft instead of blocky.
    private static BufferedImage smoothBorder(BufferedImage tiny) {
        BufferedImage step = tiny;
        for (int k = 0; k < 2; k++) {                                   // 96x54 -> 192x108 -> 384x216
            BufferedImage bigger = new BufferedImage(step.getWidth() * 2, step.getHeight() * 2, BufferedImage.TYPE_INT_RGB);
            Graphics2D sg = bigger.createGraphics();
            sg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            sg.drawImage(step, 0, 0, bigger.getWidth(), bigger.getHeight(), null);
            sg.dispose();
            step = bigger;
        }
        int n = 9;                                                      // box blur 9x9, twice
        float[] k = new float[n * n];
        java.util.Arrays.fill(k, 1f / (n * n));
        java.awt.image.ConvolveOp blur = new java.awt.image.ConvolveOp(new java.awt.image.Kernel(n, n, k),
                java.awt.image.ConvolveOp.EDGE_NO_OP, null);
        for (int pass = 0; pass < 2; pass++) step = blur.filter(step, null);
        return step;
    }

    // The blurred, darkened copy of the scene that fills the whole panel behind the scene picture.
    private static void drawBorder(Graphics2D g, Composed composed, Rectangle panel) {
        Graphics2D bg = (Graphics2D) g.create();
        bg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        if (composed.soft == null) composed.soft = smoothBorder(composed.tiny);
        double cover = Math.max(panel.width / (double) composed.soft.getWidth(),
                panel.height / (double) composed.soft.getHeight());
        int bw = (int) Math.ceil(composed.soft.getWidth() * cover), bh = (int) Math.ceil(composed.soft.getHeight() * cover);
        bg.setClip(panel);
        bg.drawImage(composed.soft, panel.x + (panel.width - bw) / 2, panel.y + (panel.height - bh) / 2, bw, bh, null);
        bg.setColor(new Color(0, 0, 0, 70));
        bg.fillRect(panel.x, panel.y, panel.width, panel.height);
        bg.dispose();
    }

    /**
     * Width (in pixels) of the picture used for the zoomed view of the mystery game, for a scene shown
     * {@code fitWidth} pixels wide: about three times larger, so the zoomed animal stays sharp.
     */
    public static int zoomWidth(int fitWidth) {
        return Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, (int) Math.ceil(fitWidth * 3.0 / 256.0) * 256));
    }

    // Prepares the pictures of a scene (normal size, all-animals-in-colour and zoomed) so the game opens without a stall.
    public static void warm(Biome biome, int normalWidth, int zoomRenderWidth) {
        Composed c = compose(biome.getBiomeType(), normalWidth);
        if (c == null) return;
        revealedOf(c);
        compose(biome.getBiomeType(), zoomRenderWidth);
    }

    /**
     * Draws the scene scaled and moved to {@code sceneRect} (it may be larger than the panel, which shows only part
     * of it) using the picture composed for {@code renderWidth}. Used for the mystery game's zoom. Hotspots are
     * fractions of sceneRect. completed = all animals in colour; found = animals the player has found;
     * marks = draw the green frame and tick around found animals.
     */
    public static void drawView(Graphics2D g, Biome biome, Rectangle panel, Rectangle sceneRect, int renderWidth,
                                boolean completed, boolean[] found, boolean marks) {
        int[] rect = {sceneRect.x, sceneRect.y, sceneRect.width, sceneRect.height};
        Composed composed = compose(biome.getBiomeType(), renderWidth);
        if (composed == null) {
            drawFallback(g, biome, panel, rect, completed, found);
            return;
        }
        drawBorder(g, composed, panel);
        BufferedImage picture = composed.scene;
        boolean patchesForAll = false;
        if (completed) {
            if (composed.revealed != null) picture = composed.revealed;
            else patchesForAll = true;                          // not built at this size: colour patches do the same job
        }
        Graphics2D sg = (Graphics2D) g.create();
        sg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        sg.clip(panel);
        sg.translate(sceneRect.x, sceneRect.y);
        sg.scale(sceneRect.width / (double) composed.width, sceneRect.height / (double) composed.height());
        sg.drawImage(picture, 0, 0, null);
        sg.dispose();
        boolean[] show = found;
        if (patchesForAll) {
            show = new boolean[biome.getAnimals().size()];
            java.util.Arrays.fill(show, true);
        }
        if ((!completed || patchesForAll) && show != null) {
            Graphics2D fg = (Graphics2D) g.create();
            fg.clip(panel);
            drawFound(fg, composed, biome, show, rect, marks && !completed);
            fg.dispose();
        }
    }

    // The scene with every animal in colour (built the first time an animal is found). 
    private static synchronized BufferedImage revealedOf(Composed c) {
        if (c.revealed == null) {
            BufferedImage made = composeLayers(c.biomeType, c.width, true);
            c.revealed = made != null ? made : c.scene;
        }
        return c.revealed;
    }

    /*
    - Draws one layer into a scene whose top-left corner is (ox, oy) and whose size is sw x sh.
    - The picture is placed like in PowerPoint: the layer's box, rotated and mirrored around its centre, with the
    picture stretched by the stretch offsets and cropped to the box. Returns false if the picture is missing.
    */
    private static boolean paintLayer(Graphics2D g0, SceneLayouts.Layer l, double ox, double oy,
                                      double sw, double sh, boolean compose, boolean hideAnimals) {
        double x = ox + l.x() * sw, y = oy + l.y() * sh, w = l.w() * sw, h = l.h() * sh;
        if (w < 1 || h < 1) return false;
        BufferedImage image = null;
        if (!l.isGradient()) {
            image = GuiImages.load(l.file());
            if (image == null) return false;
        }

        Graphics2D g = (Graphics2D) g0.create();
        GuiKit.quality(g);
        g.setComposite(AlphaComposite.SrcOver.derive((float) Math.max(0, Math.min(1, l.alpha()))));
        double cx = x + w / 2, cy = y + h / 2;
        g.translate(cx, cy);
        g.rotate(Math.toRadians(l.rot()));
        g.scale(l.flipH() ? -1 : 1, l.flipV() ? -1 : 1);
        g.translate(-cx, -cy);

        if (l.isGradient()) {
            double rad = Math.toRadians(l.angle());          // 90 = from the top edge to the bottom edge
            double dx = Math.cos(rad) * w / 2, dy = Math.sin(rad) * h / 2;
            g.setPaint(new GradientPaint((float) (cx - dx), (float) (cy - dy), withAlpha(l.color0(), l.alpha0()),
                    (float) (cx + dx), (float) (cy + dy), withAlpha(l.color1(), l.alpha1())));
            g.fill(new Rectangle2D.Double(x, y, w, h));
            g.dispose();
            return true;
        }

        boolean stretched = l.fl() != 0 || l.ft() != 0 || l.fr() != 0 || l.fb() != 0;
        double dx = x + l.fl() * w, dy = y + l.ft() * h;
        double dw = w * (1 - l.fl() - l.fr()), dh = h * (1 - l.ft() - l.fb());
        if (dw < 1 || dh < 1) { g.dispose(); return false; }
        if (stretched) g.clip(new Rectangle2D.Double(x, y, w, h));
        int iw = (int) Math.round(dw), ih = (int) Math.round(dh);
        if (hideAnimals && l.animal() != null) {
            drawSilhouette(g, l, image, dx, dy, dw, dh);          // not found yet: only the black shape
        } else {
            BufferedImage src = image;
            if (compose && (iw < image.getWidth() / 2 || ih < image.getHeight() / 2)) src = GuiImages.shrink(image, iw, ih);
            g.drawImage(src, (int) Math.round(dx), (int) Math.round(dy), iw, ih, null);
        }
        g.dispose();
        return true;
    }

    /*
    - Draws the hidden (blacked-out) version of an animal where its coloured picture would be: the visible part of the
    silhouette is fitted onto the visible part of the coloured picture, so both have the same size and place.
    */
    private static void drawSilhouette(Graphics2D g, SceneLayouts.Layer l, BufferedImage colour,
                                       double dx, double dy, double dw, double dh) {
        BufferedImage file = l.hidden() == null ? null : GuiImages.load(l.hidden());
        BufferedImage black = file != null ? file : GuiImages.blackout(colour);   // no file: black out the colour picture
        Rectangle cb = GuiImages.opaqueBounds(colour), sb = GuiImages.opaqueBounds(black);
        int tx = (int) Math.round(dx + cb.x * dw / colour.getWidth());
        int ty = (int) Math.round(dy + cb.y * dh / colour.getHeight());
        int tw = Math.max(1, (int) Math.round(cb.width * dw / colour.getWidth()));
        int th = Math.max(1, (int) Math.round(cb.height * dh / colour.getHeight()));
        BufferedImage part = black.getSubimage(sb.x, sb.y, sb.width, sb.height);
        g.drawImage(GuiImages.shrink(part, tw, th), tx, ty, null);
    }

    private static Color withAlpha(String hex, double alpha) {
        Color c = Color.decode("#" + hex);
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) Math.round(255 * Math.max(0, Math.min(1, alpha))));
    }

    // ------------------------------------------------------------ marks and fallback

    // Green frame and tick around a found animal.
    private static void drawFoundMark(Graphics2D g, Rectangle r) {
        g.setColor(new Color(54, 210, 110, 225));
        g.setStroke(new BasicStroke(3f));
        g.drawRoundRect(r.x - 3, r.y - 3, r.width + 6, r.height + 6, 18, 18);
        int cx = r.x + r.width + 3;
        int cy = r.y - 3;
        g.fillOval(cx - 18, cy - 8, 25, 25);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawPolyline(new int[]{cx - 11, cx - 5, cx + 4}, new int[]{cy + 3, cy + 9, cy - 2}, 3);
    }

    // Used only when src/resources/scenes is missing: plain colours with each animal's own picture.
    private static void drawFallback(Graphics2D g, Biome biome, Rectangle panel, int[] imageRect,
                                     boolean completed, boolean[] found) {
        g.setPaint(new GradientPaint(0, panel.y, ScenePanel.top(biome.getBiomeType()),
                0, panel.y + panel.height, ScenePanel.bottom(biome.getBiomeType())));
        g.fillRect(panel.x, panel.y, panel.width, panel.height);
        List<Animal> animals = biome.getAnimals();
        double[][] hotspots = Hotspots.forBiome(biome);
        for (int i = 0; i < animals.size(); i++) {
            Rectangle r = toRectangle(imageRect, hotspots[i]);
            BufferedImage sprite = GuiImages.load(GuiImages.animalFile(animals.get(i)));
            boolean isFound = completed || (found != null && i < found.length && found[i]);
            if (sprite != null) GuiImages.drawContain(g, isFound ? sprite : GuiImages.blackout(sprite), r.x, r.y, r.width, r.height);
            if (isFound && !completed) drawFoundMark(g, r);
        }
    }
}
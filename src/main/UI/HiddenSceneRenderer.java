package main.UI;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import main.*;

/**
 * Builds the seek-and-find scene at paint time from the original biome artwork,
 * separate background-assets, and transparent animal sprites.
 */
public final class HiddenSceneRenderer {
    private static final String ASSET_FOLDER = "background assets (if need)/";
    private static final Map<String, BufferedImage> DECOR_CACHE = new HashMap<>();

    private HiddenSceneRenderer() { }

    public static int[] drawScene(Graphics2D g, Biome biome, int width, int height,
                                  boolean completed, boolean[] found) {
        BufferedImage background = GuiImages.scene(biome, completed ? "complete" : "hidden");
        int[] imageRect;
        if (background != null) {
            imageRect = GuiImages.drawFull(g, background, width, height);
        } else {
            g.setPaint(new java.awt.GradientPaint(0, 0,
                    ScenePanel.top(biome.getBiomeType()), 0, height,
                    ScenePanel.bottom(biome.getBiomeType())));
            g.fillRect(0, 0, width, height);
            imageRect = new int[]{0, 0, width, height};
        }

        drawBackDecor(g, biome, width, height);

        List<Animal> animals = biome.getAnimals();
        double[][] hotspots = Hotspots.forBiome(biome);
        for (int i = 0; i < animals.size(); i++) {
            Rectangle bounds = toRectangle(imageRect, hotspots[i]);
            boolean isFound = completed || (found != null && i < found.length && found[i]);
            drawAnimal(g, animals.get(i), bounds, isFound);
        }

        drawFrontDecor(g, biome, width, height);
        return imageRect;
    }

    public static Rectangle toRectangle(int[] imageRect, double[] hotspot) {
        return new Rectangle(
                imageRect[0] + (int) Math.round(hotspot[0] * imageRect[2]),
                imageRect[1] + (int) Math.round(hotspot[1] * imageRect[3]),
                Math.max(1, (int) Math.round(hotspot[2] * imageRect[2])),
                Math.max(1, (int) Math.round(hotspot[3] * imageRect[3])));
    }

    private static void drawAnimal(Graphics2D g, Animal animal, Rectangle bounds, boolean found) {
        BufferedImage sprite = found
                ? GuiImages.load(GuiImages.animalFile(animal))
                : GuiImages.load(GuiImages.hiddenAnimalFile(animal));
        if (sprite == null) sprite = GuiImages.load(GuiImages.animalFile(animal));
        if (sprite == null) sprite = GuiImages.load(animal.getSpriteTemplate());
        if (sprite == null) return;

        // Fit, don't crop: the complete animal remains visible in its hiding place.
        int padding = Math.max(8, Math.min(bounds.width, bounds.height) / 5);
        int x = bounds.x + padding;
        int y = bounds.y + padding;
        int maxW = Math.max(1, bounds.width - padding * 2);
        int maxH = Math.max(1, bounds.height - padding * 2);
        double scale = Math.min(maxW / (double) sprite.getWidth(), maxH / (double) sprite.getHeight());
        int drawW = Math.max(1, (int) Math.round(sprite.getWidth() * scale));
        int drawH = Math.max(1, (int) Math.round(sprite.getHeight() * scale));
        x += (maxW - drawW) / 2;
        y += (maxH - drawH) / 2;

        Composite oldComposite = g.getComposite();
        g.setComposite(AlphaComposite.SrcOver.derive(found ? 1.0f : 0.90f));
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(sprite, x, y, drawW, drawH, null);
        g.setComposite(oldComposite);

        if (found) {
            g.setColor(new java.awt.Color(54, 210, 110, 225));
            g.setStroke(new java.awt.BasicStroke(3f));
            g.drawRoundRect(bounds.x + 2, bounds.y + 2, bounds.width - 4, bounds.height - 4, 18, 18);
            int cx = bounds.x + bounds.width - 4;
            int cy = bounds.y + 4;
            g.fillOval(cx - 18, cy - 8, 25, 25);
            g.setColor(java.awt.Color.WHITE);
            g.setStroke(new java.awt.BasicStroke(2.8f, java.awt.BasicStroke.CAP_ROUND,
                    java.awt.BasicStroke.JOIN_ROUND));
            g.drawPolyline(new int[]{cx - 11, cx - 5, cx + 4},
                    new int[]{cy + 3, cy + 9, cy - 2}, 3);
        }
    }

    private static void drawBackDecor(Graphics2D g, Biome biome, int w, int h) {
        String type = biome.getBiomeType();
        if ("Jungle".equalsIgnoreCase(type)) {
            drawLayer(g, "jng_mountain_scenery.png", 0, (int) (h * 0.14), w, (int) (h * 0.28), 0.55f);
            drawLayer(g, "jng_tree_big.png", -(int) (w * 0.04), (int) (h * 0.19),
                    (int) (w * 0.28), (int) (h * 0.56), 0.64f);
            drawLayer(g, "jng_tree_big.png", (int) (w * 0.77), (int) (h * 0.16),
                    (int) (w * 0.27), (int) (h * 0.58), 0.64f);
            drawLayer(g, "jng_vine_one.png", 0, 0, (int) (w * 0.42), (int) (h * 0.15), 0.92f);
            drawLayer(g, "jng_vine_two.png", (int) (w * 0.61), 0, (int) (w * 0.38), (int) (h * 0.13), 0.92f);
        } else if ("Safari".equalsIgnoreCase(type)) {
            drawLayer(g, "svnh_clouds.png", 0, 0, w, (int) (h * 0.24), 0.70f);
            drawLayer(g, "svnh_trees.png", 0, (int) (h * 0.45), w, (int) (h * 0.40), 0.73f);
        } else {
            drawLayer(g, "ocean_bubbles.png", 0, (int) (h * 0.04), w, (int) (h * 0.17), 0.70f);
            drawLayer(g, "ocean_fish_background.png", 0, (int) (h * 0.23), w, (int) (h * 0.28), 0.38f);
        }
    }

    private static void drawFrontDecor(Graphics2D g, Biome biome, int w, int h) {
        String type = biome.getBiomeType();
        if ("Jungle".equalsIgnoreCase(type)) {
            drawLayer(g, "jng_bush.png", -(int) (w * 0.06), (int) (h * 0.73),
                    (int) (w * 0.40), (int) (h * 0.29), 0.94f);
            drawLayer(g, "jng_bush.png", (int) (w * 0.68), (int) (h * 0.73),
                    (int) (w * 0.40), (int) (h * 0.29), 0.94f);
        } else if ("Safari".equalsIgnoreCase(type)) {
            drawLayer(g, "svnh_bush.png", 0, (int) (h * 0.79), (int) (w * 0.43), (int) (h * 0.20), 0.96f);
            drawLayer(g, "svnh_bush.png", (int) (w * 0.59), (int) (h * 0.79), (int) (w * 0.43), (int) (h * 0.20), 0.96f);
            drawLayer(g, "svnh_grass.png", 0, (int) (h * 0.89), w, (int) (h * 0.12), 0.95f);
        } else {
            drawLayer(g, "ocean_kelp.png", 0, (int) (h * 0.63), w, (int) (h * 0.37), 0.92f);
            drawLayer(g, "ocean_coral_two.png", 0, (int) (h * 0.84), w, (int) (h * 0.17), 0.90f);
            drawLayer(g, "ocean_coral_three.png", (int) (w * 0.77), (int) (h * 0.74),
                    (int) (w * 0.18), (int) (h * 0.24), 0.92f);
        }
    }

    private static void drawLayer(Graphics2D g, String file, int x, int y, int width, int height, float alpha) {
        BufferedImage image = loadDecor(file);
        if (image == null || width <= 0 || height <= 0) return;
        Composite old = g.getComposite();
        g.setComposite(
            AlphaComposite.SrcOver.derive(
                Math.max(0f, Math.min(1f, alpha))));
        g.drawImage(
            image, x, y, width, height, null);
        g.setComposite(old);
    }

    /**
     * Some older scenery assets are RGB PNGs with a white canvas instead of alpha.
     * Convert near-white pixels to transparency so those assets can be layered.
     */
    private static BufferedImage loadDecor(String file) {
        if (DECOR_CACHE.containsKey(file)) return DECOR_CACHE.get(file);
        BufferedImage source = GuiImages.load(ASSET_FOLDER + file);
        if (source == null) {
            DECOR_CACHE.put(file, null);
            return null;
        }
        if (source.getColorModel().hasAlpha()) {
            DECOR_CACHE.put(file, source);
            return source;
        }

        BufferedImage cleaned = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int rgb = source.getRGB(x, y);
                int red = (rgb >>> 16) & 255;
                int green = (rgb >>> 8) & 255;
                int blue = rgb & 255;
                int minimum = Math.min(red, Math.min(green, blue));
                int maximum = Math.max(red, Math.max(green, blue));
                int alpha = 255;
                if (minimum > 248 && maximum > 248) alpha = 0;
                else if (minimum > 232 && maximum - minimum < 16) alpha = Math.max(0, 255 - (minimum - 232) * 11);
                cleaned.setRGB(x, y, (alpha << 24) | (rgb & 0x00FFFFFF));
            }
        }
        DECOR_CACHE.put(file, cleaned);
        return cleaned;
    }
}
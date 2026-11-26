package main;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import main.UI.GuiImages;
import main.UI.SceneLayouts;

/*
- Normalized clickable areas for the ten animal sprites in each biome scene.
- Values are {x, y, width, height} measured against the scene picture (see HiddenSceneRenderer.drawScene).
- The areas now come from the reference PowerPoint (SceneLayouts: the box of each animal's picture).
- The hand-placed arrays below are only used for an animal that is not in the PowerPoint layout.
- Optional resources/hotspots/<biome>.properties files can still override any animal.
*/
public final class Hotspots {
    private static final String[] DIRS = {
            "main/resources/", "resources/", "../resources/", "src/main/resources/"
    };
    private static final Map<String, double[][]> CACHE = new HashMap<>();

    // Slightly irregular placements make the scenes feel like seek-and-find puzzles.
    private static final double[][] JUNGLE = {
            {0.10, 0.42, 0.12, 0.20}, {0.30, 0.63, 0.12, 0.20},
            {0.39, 0.28, 0.11, 0.19}, {0.68, 0.29, 0.11, 0.20},
            {0.80, 0.44, 0.11, 0.19}, {0.10, 0.60, 0.11, 0.18},
            {0.28, 0.42, 0.11, 0.19}, {0.51, 0.62, 0.12, 0.19},
            {0.67, 0.66, 0.12, 0.19}, {0.80, 0.66, 0.11, 0.19}
    };
    private static final double[][] SAFARI = {
            {0.10, 0.35, 0.12, 0.20}, {0.27, 0.38, 0.12, 0.20},
            {0.46, 0.65, 0.12, 0.20}, {0.66, 0.29, 0.11, 0.20},
            {0.80, 0.42, 0.12, 0.20}, {0.10, 0.65, 0.12, 0.20},
            {0.29, 0.65, 0.12, 0.20}, {0.49, 0.42, 0.12, 0.20},
            {0.68, 0.66, 0.12, 0.20}, {0.82, 0.65, 0.11, 0.20}
    };
    private static final double[][] AQUATIC = {
            {0.10, 0.39, 0.12, 0.19}, {0.27, 0.32, 0.12, 0.19},
            {0.47, 0.36, 0.14, 0.19}, {0.67, 0.30, 0.11, 0.20},
            {0.81, 0.40, 0.12, 0.19}, {0.10, 0.66, 0.12, 0.19},
            {0.30, 0.62, 0.12, 0.19}, {0.51, 0.63, 0.12, 0.19},
            {0.69, 0.66, 0.12, 0.19}, {0.82, 0.63, 0.11, 0.19}
    };

    private Hotspots() { }

    public static double[][] forBiome(Biome biome) {
        String key = biome.getBiomeName();
        double[][] cached = CACHE.get(key);
        if (cached != null && cached.length == biome.getAnimals().size()) return cached;

        int count = biome.getAnimals().size();
        double[][] result = new double[count][4];
        Properties properties = load("hotspots/" + GuiImages.slug(key) + ".properties");

        double[][] defaults = "Safari".equalsIgnoreCase(biome.getBiomeType()) ? SAFARI
                : "Aquatic".equalsIgnoreCase(biome.getBiomeType()) ? AQUATIC : JUNGLE;
        for (int i = 0; i < count; i++) {
            result[i] = defaults[i % defaults.length].clone();
            SceneLayouts.Layer layer = SceneLayouts.animalLayer(biome.getBiomeType(), biome.getAnimals().get(i).getName());
            if (layer != null) result[i] = clickable(SceneLayouts.bounds(layer));
            String animalKey = GuiImages.slug(biome.getAnimals().get(i).getName());
            String value = properties == null ? null : properties.getProperty(animalKey);
            if (value == null) continue;
            try {
                String[] parts = value.split(",");
                if (parts.length == 4) {
                    double[] parsed = new double[4];
                    for (int p = 0; p < 4; p++) parsed[p] = Double.parseDouble(parts[p].trim());
                    if (parsed[2] > 0 && parsed[3] > 0) result[i] = parsed;
                }
            } catch (NumberFormatException ignored) {
                // Keep the default rectangle if a custom value is invalid.
            }
        }

        CACHE.put(key, result);
        return result;
    }

    // a tiny animal must still be easy to click: the area is grown (around its centre) to a minimum size.
    private static double[] clickable(double[] box) {
        final double minW = 0.045, minH = 0.075;   // fractions of the scene: about 40 x 38 px in a 900 px wide scene
        double w = Math.max(box[2], minW), h = Math.max(box[3], minH);
        double x = box[0] + box[2] / 2 - w / 2, y = box[1] + box[3] / 2 - h / 2;
        x = Math.max(0, Math.min(1 - w, x));
        y = Math.max(0, Math.min(1 - h, y));
        return new double[]{x, y, w, h};
    }

    private static Properties load(String relativePath) {
        // src/resources first, then the older relative folders
        java.util.List<File> candidates = new java.util.ArrayList<>();
        candidates.add(ProjectPaths.resourcesDirectory().resolve(relativePath).toFile());
        for (String directory : DIRS) candidates.add(new File(directory + relativePath));
        for (File file : candidates) {
            if (!file.isFile()) continue;
            Properties properties = new Properties();
            try (FileInputStream input = new FileInputStream(file)) {
                properties.load(input);
                return properties;
            } catch (IOException ignored) {
                return null;
            }
        }
        return null;
    }
}

package main;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import main.UI.GuiImages;

/**
 * Normalized clickable areas for the ten animal sprites in each biome scene.
 * Values are {x, y, width, height} measured against the cover-scaled scene image.
 * Optional resources/hotspots/<biome>.properties files can override these defaults.
 */
public final class Hotspots {
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

    private static Properties load(String relativePath) {
        Path resourcesRoot = ProjectPaths.resourcesDirectory().toAbsolutePath().normalize();
        Path file = resourcesRoot.resolve(relativePath).normalize();
        if (!file.startsWith(resourcesRoot) || !Files.isRegularFile(file)) return null;

        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            properties.load(input);
            return properties;
        } catch (IOException ignored) {
            return null;
        }
    }
}

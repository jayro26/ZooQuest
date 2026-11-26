package main.UI;

import java.util.Locale;

public final class SceneLayouts {
    public static final double ASPECT = 16.0 / 9.0;

    public record Layer(String file, double x, double y, double w, double h, double rot,
                        boolean flipH, boolean flipV, double alpha,
                        double fl, double ft, double fr, double fb, String animal, String hidden,
                        String color0, double alpha0, String color1, double alpha1, double angle) {
        public boolean isGradient() { return file == null; }
    }

    private static Layer img(String file, double x, double y, double w, double h, double rot,
                             boolean flipH, boolean flipV, double alpha,
                             double fl, double ft, double fr, double fb, String animal, String hidden) {
        return new Layer(file, x, y, w, h, rot, flipH, flipV, alpha, fl, ft, fr, fb, animal, hidden, null, 0, null, 0, 0);
    }

    // A translucent colour gradient over a box (angle 90 = top to bottom)
    private static Layer grad(double x, double y, double w, double h,
                              String color0, double alpha0, String color1, double alpha1, double angle) {
        return new Layer(null, x, y, w, h, 0, false, false, 1, 0, 0, 0, 0, null, null, color0, alpha0, color1, alpha1, angle);
    }

    // ---- Jungle
    private static final Layer[] JUNGLE = {
        grad(0, 0, 1, 1, "84A9FF", 1, "A9BFF1", 1, 90),
        img("scenes/jungle/image1.png", 0, 0, 1.00489, 0.80964, 0, false, false, 1, -0.00606, -0.31517, -0.00418, 0, null, null),
        img("scenes/jungle/image2.png", 0.46899, -0.14609, 0.83113, 1.01213, 0, false, false, 1, 0, 0, 0, 0, null, null),
        img("scenes/jungle/image3.png", 0.08176, 0.06174, 0.27697, 0.61936, 0, false, false, 1, 0, 0, 0, 0, null, null),
        img("scenes/jungle/image4.png", 0.3933, 0.41753, 0.08845, 0.2016, 0, false, false, 1, 0, 0, 0, 0, null, null),
        img("scenes/jungle/image4.png", 0.32887, 0.34692, 0.11602, 0.26443, 0, false, false, 1, 0, 0, 0, 0, null, null),
        img("scenes/jungle/animal_koala.png", 0.41253, 0.32825, 0.03395, 0.06421, 174.61838, false, false, 1, 0, 0, 0, 0, "koala", "koala_hidden.png"),
        img("scenes/jungle/image4.png", 0.33963, 0.24712, 0.1616, 0.36832, 0, false, false, 1, 0, 0, 0, 0, null, null),
        img("scenes/jungle/image6.png", 0.17479, 0.40119, 0.378, 0.51811, 0, false, false, 1, -0.5319, -0.43693, -0.57249, -0.43956, null, null),
        img("scenes/jungle/image7.png", 0.08749, 0.44672, 0.22784, 0.65861, 0, true, false, 1, 0, 0, 0, 0, null, null),
        img("scenes/jungle/image6.png", -0.02628, 0.35386, 0.1999, 0.54249, 0, false, false, 1, 0, -0.3365, -2.97931, -0.45567, null, null),
        img("scenes/jungle/image9.png", 0.48175, 0.37806, 0.08688, 0.23581, 0, false, false, 1, 0, 0, 0, 0, null, null),
        img("scenes/jungle/image10.png", 0.00052, 0.26622, 0.7362, 0.73378, 0, false, false, 1, -0.01492, -0.20022, 0, -0.04433, null, null),
        img("scenes/jungle/image10.png", 0.73568, 0.26622, 0.7362, 0.73378, 0, true, false, 1, -0.01492, -0.20022, 0, -0.04433, null, null),
        img("scenes/jungle/image11.png", 0.14712, 0.32451, 0.13081, 0.09302, 0, false, false, 1, 0, 0, 0, 0, null, null),
        img("scenes/jungle/animal_gorilla.png", 0.27474, 0.35232, 0.06856, 0.12763, 7.04983, false, false, 1, 0, 0, 0, 0, "gorilla", "gorilla_hidden.png"),
        img("scenes/jungle/image9.png", 0.09072, 0.23087, 0.08688, 0.23581, 0, false, false, 1, 0, 0, 0, 0, null, null),
        img("scenes/jungle/image9.png", 0.24255, 0.24712, 0.08688, 0.23581, 0, false, false, 1, 0, 0, 0, 0, null, null),
        img("scenes/jungle/image9.png", 0.51143, 0.39025, 0.08688, 0.23581, 0, false, false, 1, 0, 0, 0, 0, null, null),
        img("scenes/jungle/animal_tiger.png", 0.04738, 0.33203, 0.07005, 0.11644, 0, true, false, 1, 0, 0, 0, 0, "tiger", "tiger_hidden.png"),
        img("scenes/jungle/image14.png", -0.01507, -0.01664, 0.346, 0.74261, 0, false, false, 1, 0, 0, -0.57811, 0, null, null),
        img("scenes/jungle/animal_komodo_dragon.png", 0.88566, 0.77116, 0.09777, 0.13644, 8.77167, false, false, 1, 0, 0, 0, 0, "komodo_dragon", "komodo_dragon_hidden.png"),
        img("scenes/jungle/image16.png", 0.76738, 0.72597, 0.25102, 0.16735, 0, false, false, 1, 0, 0, 0, 0, null, null),
        img("scenes/jungle/animal_monkey.png", 0.68598, 0.70062, 0.07601, 0.19166, 0, false, false, 1, 0, 0, 0, 0, "monkey", "monkey_hidden.png"),
        img("scenes/jungle/animal_eagle.png", 0.77497, 0.1906, 0.07694, 0.20114, 0, false, false, 1, 0, 0, 0, 0, "eagle", "eagle_hidden.png"),
        img("scenes/jungle/image19.png", 0.85908, 0.22657, 0.34263, 0.8176, 0, false, false, 1, 0, 0, 0, 0, null, null),
        img("scenes/jungle/image19.png", 0.70219, 0.38213, 0.34263, 0.8176, 0, false, false, 1, 0, 0, 0, 0, null, null),
        img("scenes/jungle/image19.png", 0.61098, 0.21336, 0.35841, 0.85526, 0, false, false, 1, 0, 0, 0, 0, null, null),
        img("scenes/jungle/image19.png", 0.48737, 0.41176, 0.35841, 0.85526, 0, false, false, 1, 0, 0, 0, 0, null, null),
        img("scenes/jungle/animal_deer.png", -0.0416, 0.54636, 0.19571, 0.4799, 0, true, false, 1, 0, 0, 0, 0, "deer", "deer_hidden.png"),
        img("scenes/jungle/animal_capybara.png", 0.21252, 0.86438, 0.09522, 0.16673, 0, false, false, 1, 0, 0, 0, 0, "capybara", "capybara_hidden.png"),
        img("scenes/jungle/image22.png", -0.01507, 0.68459, 0.2679, 0.35958, 0, false, false, 1, 0, 0, 0, 0, null, null),
        img("scenes/jungle/animal_lemur.png", 0.42794, 0.75229, 0.08653, 0.15618, -29.64947, false, false, 1, 0, 0, 0, 0, "lemur", "lemur_hidden.png"),
        img("scenes/jungle/animal_parrot.png", 0.0247, 0.30494, 0.05757, 0.13292, 0, true, false, 1, 0, 0, 0, 0, "parrot", "parrot_hidden.png")
    };

    // ---- Safari
    private static final Layer[] SAFARI = {
        grad(0, 0, 1, 1, "6FABC1", 1, "82D3F1", 1, 90),
        img("scenes/safari/image25.png", -0.02775, 0.025, 0.25071, 0.22731, 0, false, false, 1, 0, 0, 0, 0, null, null),
        img("scenes/safari/image25.png", 0.22245, 0.18785, 0.14975, 0.13578, 0, false, false, 1, 0, 0, 0, 0, null, null),
        img("scenes/safari/image25.png", 0.33762, 0.03192, 0.19839, 0.17987, 0, false, false, 1, 0, 0, 0, 0, null, null),
        img("scenes/safari/image25.png", 0.54554, -0.07857, 0.19839, 0.17987, 0, false, false, 1, 0, 0, 0, 0, null, null),
        img("scenes/safari/image25.png", 0.57367, 0.15984, 0.14975, 0.13578, 0, false, false, 1, 0, 0, 0, 0, null, null),
        img("scenes/safari/image25.png", 0.7791, -0.00736, 0.20157, 0.18276, 0, false, false, 1, 0, 0, 0, 0, null, null),
        img("scenes/safari/image26.png", 0.4896, 0.4555, 0.49317, 0.24888, 0, true, false, 1, -0.24019, 0, -0.21301, -1.17572, null, null),
        img("scenes/safari/image26.png", -0.00253, 0.45624, 0.49317, 0.24888, 0, false, false, 1, -0.24019, 0, -0.21301, -1.17572, null, null),
        img("scenes/safari/image27.png", 0.19331, 0.18785, 0.64951, 0.86601, 0, false, false, 1, 0, 0, 0, 0, null, null),
        img("scenes/safari/image28.png", 0.72122, 0.5859, 0.19278, 0.13709, 0, false, false, 1, 0, 0, 0, 0, null, null),
        img("scenes/safari/animal_vulture.png", 0.89496, 0.08297, 0.08571, 0.14476, -13.29623, false, false, 1, 0, 0, 0, 0, "vulture", "vulture_hidden.png"),
        img("scenes/safari/image30.png", 0.84098, -0.17227, 0.2459, 0.4, 0, false, false, 1, 0, 0, 0, 0, null, null),
        img("scenes/safari/animal_giraffe.png", 0.68847, 0.57331, 0.04179, 0.14425, 0, false, false, 1, 0, 0, 0, 0, "giraffe", "giraffe_hidden.png"),
        img("scenes/safari/animal_buffalo.png", 0.70653, 0.68237, 0.16894, 0.25979, 0, false, false, 1, 0, 0, 0, 0, "buffalo", "african_buffalo_hidden.png"),
        img("scenes/safari/image32.png", 0.74499, 0.1361, 0.32596, 0.93422, 0, false, false, 1, -1.14952, 0, 0, 0, null, null),
        img("scenes/safari/image33.png", 0.63744, 0.73795, 0.30631, 0.40841, 0, false, false, 1, 0, 0, 0, 0, null, null),
        img("scenes/safari/image27.png", -0.45463, 0.18785, 0.64951, 0.86601, 0, true, false, 1, 0, 0, 0, 0, null, null),
        img("scenes/safari/image34.png", -0.18281, 0.69327, 0.45362, 0.33064, -2.02355, false, false, 1, 0, 0, 0, 0, null, null),
        img("scenes/safari/image30.png", 0.27537, 0.42491, 0.16452, 0.26762, 0, false, false, 1, 0, 0, 0, 0, null, null),
        img("scenes/safari/animal_zebra.png", 0.45234, 0.57331, 0.0905, 0.18284, 0, false, false, 1, 0, 0, 0, 0, "zebra", "zebra_hidden.png"),
        img("scenes/safari/animal_ostrich.png", 0.28936, 0.60899, 0.0703, 0.13296, 0, true, false, 1, 0, 0, 0, 0, "ostrich", "ostrich_hidden.png"),
        img("scenes/safari/image28.png", 0.29168, 0.60261, 0.22805, 0.16217, 0, false, false, 1, 0, 0, 0, 0, null, null),
        img("scenes/safari/image30.png", 0.02659, 0.37272, 0.207, 0.33672, 0, false, false, 1, 0, 0, 0, 0, null, null),
        img("scenes/safari/image30.png", 0.50989, 0.45725, 0.12755, 0.20748, 0, false, false, 1, 0, 0, 0, 0, null, null),
        img("scenes/safari/image30.png", 0.59687, 0.47373, 0.1418, 0.23065, 0, false, false, 1, 0, 0, 0, 0, null, null),
        img("scenes/safari/animal_elephant.png", 0.56055, 0.60238, 0.08138, 0.10706, 0, false, false, 1, 0, 0, 0, 0, "elephant", "elephant_hidden.png"),
        img("scenes/safari/animal_hyena.png", 0.06673, 0.63396, 0.05483, 0.07116, 0, false, false, 1, 0, 0, 0, 0, "hyena", "hyena_hidden.png"),
        img("scenes/safari/image39.png", 0.07413, 0.63267, 0.09316, 0.07897, 0, false, false, 1, 0, 0, 0, -0.00664, null, null),
        img("scenes/safari/image39.png", 0.5819, 0.62913, 0.10432, 0.08843, 0, false, false, 1, 0, 0, 0, -0.00664, null, null),
        img("scenes/safari/image30.png", 0.17342, 0.52284, 0.09807, 0.15953, 0, false, false, 1, 0, 0, 0, 0, null, null),
        img("scenes/safari/animal_rhino.png", -0.04267, 0.56099, 0.08534, 0.14413, 0, true, false, 1, 0, 0, 0, 0, "rhino", "rhino_hidden.png"),
        img("scenes/safari/animal_hippopotamus.png", 0.13927, 0.66363, 0.11225, 0.15665, 0, false, false, 1, 0, 0, 0, 0, "hippopotamus", "hippo_hidden.png"),
        img("scenes/safari/animal_nile_crocodile.png", 0, 0.91111, 0.12774, 0.12149, 0, true, false, 1, 0, 0, 0, 0, "nile_crocodile", "nile_crocodile_hidden.png")
    };

    // ---- Aquatic 
    private static final Layer[] AQUATIC = {
        grad(0, 0, 1, 1, "50A8FF", 1, "74D1FE", 1, 90),
        img("scenes/aquatic/image44.png", -0.12758, -0.35278, 0.57781, 0.62146, 0, false, false, 1, 0, 0, 0, 0, null, null),
        img("scenes/aquatic/image44.png", 0.32373, -0.31154, 0.57781, 0.62146, 0, false, false, 1, 0, 0, 0, 0, null, null),
        img("scenes/aquatic/image44.png", 0.75127, -0.33725, 0.57781, 0.62146, 0, false, false, 1, 0, 0, 0, 0, null, null),
        img("scenes/aquatic/image45.png", -0.00007, 0.35571, 0.99919, 0.74606, 0, false, false, 0.6, 0, 0, 0, 0, null, null),
        img("scenes/aquatic/animal_stringray.png", 0.91048, 0.3308, 0.12203, 0.15512, 0, false, false, 1, 0, 0, 0, 0, "stringray", "sting_ray_hidden.png"),
        img("scenes/aquatic/animal_manta_ray.png", 0.54694, 0.67645, 0.12061, 0.15546, 0, false, false, 1, 0, 0, 0, 0, "manta_ray", "manta_ray_hidden.png"),
        img("scenes/aquatic/animal_whale.png", 0.15941, 0.4486, 0.17288, 0.1506, 4.73463, false, false, 1, 0, 0, 0, 0, "whale", "scenes/aquatic/hidden_whale.png"),
        img("scenes/aquatic/image50.png", -0.00607, 0.32874, 0.225, 0.4, 0, true, false, 1, 0, 0, 0, 0, null, null),
        img("scenes/aquatic/image52.png", 0.29952, 0.32023, 0.39055, 0.43395, 0, false, false, 1, 0, 0, 0, 0, null, null),
        img("scenes/aquatic/image50.png", 0.75715, 0.3828, 0.225, 0.4, 0, false, false, 1, 0, 0, 0, 0, null, null),
        img("scenes/aquatic/animal_shark.png", 0.86942, 0.64336, 0.1721, 0.20805, 0, false, false, 1, 0, 0, 0, 0, "shark", "shark_hidden.png"),
        img("scenes/aquatic/animal_dolphin.png", 0.40671, 0.29618, 0.08062, 0.15166, 35.4416, false, false, 1, 0, 0, 0, 0, "dolphin", "dolphin_hidden.png"),
        img("scenes/aquatic/image56.png", 0.11275, 0.43495, 0.16621, 0.59098, 0, false, false, 1, 0, 0, 0, 0, null, null),
        img("scenes/aquatic/image56.png", 0.57918, 0.63733, 0.10279, 0.36547, 0, false, false, 1, 0, 0, 0, 0, null, null),
        img("scenes/aquatic/image56.png", 0.818, 0.55703, 0.12593, 0.44776, 0, false, false, 1, 0, 0, 0, 0, null, null),
        img("scenes/aquatic/image57.png", 0.07482, 0.65588, 0.23196, 0.4, 0, false, false, 1, 0, 0, 0, 0, null, null),
        img("scenes/aquatic/image57.png", 0.81951, 0.68149, 0.2022, 0.34868, 0, false, false, 1, 0, 0, 0, 0, null, null),
        img("scenes/aquatic/image57.png", 0.63058, 0.67359, 0.2022, 0.34868, 0, false, false, 1, 0, 0, 0, 0, null, null),
        img("scenes/aquatic/image57.png", 0.42289, 0.76439, 0.2022, 0.34868, 0, false, false, 1, 0, 0, 0, 0, null, null),
        img("scenes/aquatic/image59.png", 0.48511, 0.83581, 0.11719, 0.18646, 0, false, false, 1, 0, 0, 0, 0, null, null),
        img("scenes/aquatic/animal_tuna.png", -0.01077, 0.60651, 0.11019, 0.09536, 0, true, false, 1, -0.00708, 0, -0.00708, 0, "tuna", "tuna_hidden.png"),
        img("scenes/aquatic/animal_octopus.png", 0.38921, 0.80409, 0.11358, 0.19183, 0, false, false, 1, 0, 0, 0, 0, "octopus", "octopus_hidden.png"),
        img("scenes/aquatic/image62.png", -0.05316, 0.56533, 0.15259, 0.42058, 0, false, false, 1, 0, 0, 0, 0, null, null),
        img("scenes/aquatic/image62.png", 0.30604, 0.70188, 0.11136, 0.30693, 0, false, false, 1, 0, 0, 0, 0, null, null),
        img("scenes/aquatic/image63.png", 0.37915, 0.64748, 0.29423, 0.41573, 0, true, false, 1, -0.24266, 0, -0.08198, 0, null, null),
        img("scenes/aquatic/image63.png", -0.01008, 0.64288, 0.38975, 0.41573, 0, false, false, 1, 0, 0, 0, 0, null, null),
        img("scenes/aquatic/image63.png", 0.67025, 0.63733, 0.35019, 0.41573, 0, true, false, 1, -0.06336, 0, -0.04959, 0, null, null),
        img("scenes/aquatic/image64.png", 0.68725, 0.76439, 0.30716, 0.27303, 0, false, false, 1, 0, 0, 0, 0, null, null),
        img("scenes/aquatic/image65.png", 0.12721, 0.84722, 0.09689, 0.16364, 0, false, false, 1, 0, 0, 0, 0, null, null),
        img("scenes/aquatic/image66.png", -0.11147, -1.12637, 1.21254, 2.15563, 0, false, false, 0.51, 0, 0, 0, 0, null, null),
        img("scenes/aquatic/image68.png", 0.8308, 0.0031, 0.34694, 0.50268, 0, true, false, 1, 0, 0, 0, 0, null, null),
        img("scenes/aquatic/image69.png", -0.02197, 0.12873, 0.18718, 0.21463, 0, false, false, 1, 0, 0, 0, 0, null, null),
        img("scenes/aquatic/image70.png", 0.17885, 0.1842, 0.14798, 0.11049, 0, false, false, 1, 0, 0, 0, 0, null, null),
        grad(-0.02654, 0.29403, 1.02456, 0.76886, "8DEAFF", 0.5, "4E80AF", 0.5, 90),
        img("scenes/aquatic/image71.png", -0.0025, 0.22884, 0.21788, 0.06585, 0, false, false, 0.5, 0, 0, 0, 0, null, null),
        img("scenes/aquatic/image71.png", 0.2151, 0.22884, 0.21788, 0.06585, 0, true, false, 0.5, 0, 0, 0, 0, null, null),
        img("scenes/aquatic/image71.png", 0.43291, 0.22884, 0.21788, 0.06585, 0, false, false, 0.5, 0, 0, 0, 0, null, null),
        img("scenes/aquatic/image71.png", 0.65036, 0.22884, 0.21788, 0.06585, 0, true, false, 0.5, 0, 0, 0, 0, null, null),
        img("scenes/aquatic/image71.png", 0.86807, 0.22856, 0.21788, 0.06585, 0, false, false, 0.5, 0, 0, 0, 0, null, null),
        img("scenes/aquatic/animal_penguin.png", -0.00607, 0.05342, 0.06751, 0.1341, 0, false, false, 1, 0, 0, 0, 0, "penguin", "penguin_hidden.png"),
        img("scenes/aquatic/animal_swordfish.png", 0.5883, 0.11609, 0.10441, 0.08446, -19.0777, true, false, 1, 0, 0, 0, 0, "swordfish", "scenes/aquatic/hidden_swordfish.png"),
        img("scenes/aquatic/animal_sea_lion.png", 0.21105, 0.11388, 0.08359, 0.12556, 0, false, false, 1, 0, 0, 0, 0, "sea_lion", "sealion_hidden.png")
    };

    private SceneLayouts() { }

    // The layers of the scene for a biome type ("Jungle", "Safari" or "Aquatic"), back to front.
    public static Layer[] forType(String biomeType) {
        if ("Safari".equalsIgnoreCase(biomeType)) return SAFARI;
        if ("Aquatic".equalsIgnoreCase(biomeType)) return AQUATIC;
        return JUNGLE;
    }

    // Name used to match an animal to its picture (the data's "Octupus" typo is accepted too).
    public static String animalKey(String animalName) {
        String key = GuiImages.slug(animalName == null ? "" : animalName.trim().toLowerCase(Locale.ROOT));
        return key.equals("octupus") ? "octopus" : key;
    }

    // The layer that shows this animal in the scene, or null.
    public static Layer animalLayer(String biomeType, String animalName) {
        String key = animalKey(animalName);
        for (Layer l : forType(biomeType)) if (l.animal() != null && l.animal().equals(key)) return l;
        return null;
    }

    // The animal's box in scene fractions {x, y, width, height}: the rotated box, cut to the scene. This is also what the player can click.
    public static double[] bounds(Layer l) {
        // rotate in "slide units" (16 wide, 9 high) so the angle looks right
        double w = l.w() * 16, h = l.h() * 9;
        double rad = Math.toRadians(l.rot());
        double bw = Math.abs(w * Math.cos(rad)) + Math.abs(h * Math.sin(rad));
        double bh = Math.abs(w * Math.sin(rad)) + Math.abs(h * Math.cos(rad));
        double cx = (l.x() + l.w() / 2) * 16, cy = (l.y() + l.h() / 2) * 9;
        double x0 = Math.max(0, (cx - bw / 2) / 16), y0 = Math.max(0, (cy - bh / 2) / 9);
        double x1 = Math.min(1, (cx + bw / 2) / 16), y1 = Math.min(1, (cy + bh / 2) / 9);
        return new double[]{x0, y0, Math.max(0.01, x1 - x0), Math.max(0.01, y1 - y0)};
    }
}

package main;

/** NEW CODE: safari zone, created by Biome.create('S', ...). */
public class SafariBiome extends Biome {

    public SafariBiome(String biomeName, String description, boolean isLocked) {
        super(biomeName, description, isLocked);
    }

    @Override
    public String getBiomeType() { return "Safari"; }

    @Override
    public char getAnimalTypeCode() { return 'S'; }

    @Override
    public String getBackgroundTemplate() { return "backgrounds/safari_background.png"; }

    @Override
    public String[] getScenery() {
        return new String[] {
                "            \\ | /          ~ THE SAFARI PLAINS ~",
                "          -- (   ) --",
                "   ,,,,,,,,,,,,,,,,,,,,,,,   golden grass, acacia trees" };
    }
}

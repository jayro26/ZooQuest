package main;

// Safari zone, created by Biome.create('S', ...). 
public class SafariBiome extends Biome {

    public SafariBiome(String biomeName, String description, boolean isLocked) {
        super(biomeName, description, isLocked);
    }

    @Override
    public String getBiomeType() { return "Safari"; }

    @Override
    public char getAnimalTypeCode() { return 'S'; }

    @Override
    public String getBackgroundTemplate() { return "savannah_scene.png"; }

    @Override
    public String[] getScenery() {
        return new String[] {
                "            \\ | /          ~ THE SAFARI PLAINS ~",
                "          -- (   ) --",
                "   ,,,,,,,,,,,,,,,,,,,,,,,   golden grass, acacia trees" };
    }
}

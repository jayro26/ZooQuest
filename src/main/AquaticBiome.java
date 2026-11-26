package main;

// aquatic zone, created by Biome.create('A', ...).
public class AquaticBiome extends Biome {

    public AquaticBiome(String biomeName, String description, boolean isLocked) {
        super(biomeName, description, isLocked);
    }

    @Override
    public String getBiomeType() { return "Aquatic"; }

    @Override
    public char getAnimalTypeCode() { return 'A'; }

    @Override
    public String getBackgroundTemplate() { return "ocean_scene.png"; }

    @Override
    public String[] getScenery() {
        return new String[] {
                "   ~~~~  ~~~~  ~~~~  ~~~~     ~ THE OPEN WATER ~",
                "    ~~~~  ~~~~  ~~~~  ~~~~",
                "   ~~~~  ~~~~  ~~~~  ~~~~     bubbles, coral, currents" };
    }
}

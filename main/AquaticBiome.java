package main;

/** NEW CODE: aquatic zone, created by Biome.create('A', ...). */
public class AquaticBiome extends Biome {

    public AquaticBiome(String biomeName, String description, boolean isLocked) {
        super(biomeName, description, isLocked);
    }

    @Override
    public String getBiomeType() { return "Aquatic"; }

    @Override
    public char getAnimalTypeCode() { return 'A'; }

    @Override
    public String getBackgroundTemplate() { return "backgrounds/aquatic_background.png"; }

    @Override
    public String[] getScenery() {
        return new String[] {
                "   ~~~~  ~~~~  ~~~~  ~~~~     ~ THE OPEN WATER ~",
                "    ~~~~  ~~~~  ~~~~  ~~~~",
                "   ~~~~  ~~~~  ~~~~  ~~~~     bubbles, coral, currents" };
    }
}

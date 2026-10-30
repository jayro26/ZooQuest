package main;

/** NEW CODE: jungle zone, created by Biome.create('J', ...). */
public class JungleBiome extends Biome {

    public JungleBiome(String biomeName, String description, boolean isLocked) {
        super(biomeName, description, isLocked);
    }

    @Override
    public String getBiomeType() { return "Jungle"; }

    @Override
    public char getAnimalTypeCode() { return 'J'; }

    @Override
    public String getBackgroundTemplate() { return "backgrounds/jungle_background.png"; }

    @Override
    public String[] getScenery() {
        return new String[] {
                "   @@@@   @@@@   @@@@     ~ THE JUNGLE CANOPY ~",
                "  @@@@@@ @@@@@@ @@@@@@",
                "    ||     ||     ||      vines, ferns, rustling leaves" };
    }
}

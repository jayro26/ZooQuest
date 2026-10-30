package main;

import java.util.*;

/** A habitat (zone) holding 10 animals and one mystery game. */
public abstract class Biome {
    private final String biomeName;
    private final String description;
    private boolean isLocked;
    private final List<Animal> animalList = new ArrayList<>();
    private MysteryGame biomeMystery;

    protected Biome(String biomeName, String description, boolean isLocked) {
        this.biomeName = biomeName;
        this.description = description;
        this.isLocked = isLocked;
    }

    // NEW CODE: factory - 'J' jungle, 'S' safari, 'A' aquatic
    public static Biome create(char type, String biomeName, String description, boolean isLocked) {
        switch (type) {
            case 'J':
                return new JungleBiome(biomeName, description, isLocked);
            case 'S':
                return new SafariBiome(biomeName, description, isLocked);
            case 'A':
                return new AquaticBiome(biomeName, description, isLocked);
            default:
                throw new IllegalArgumentException("Unknown biome type: " + type);
        }
    }

    // NEW CODE: what makes each zone type different (polymorphic)
    public abstract String getBiomeType();
    /** The Animal.create(...) type code of the animals that live in this zone. */
    public abstract char getAnimalTypeCode();
    public abstract String getBackgroundTemplate();
    public abstract String[] getScenery();
    public void addAnimal(Animal animal) {
        animalList.add(animal);
        biomeMystery = null;   // rebuilt lazily so it always matches the animal list
    }

    public void unlockBiome() { isLocked = false; }
    public boolean isLocked() { return isLocked; }
    public List<Animal> getAnimals() { return Collections.unmodifiableList(animalList); }
    public String getBiomeName() { return biomeName; }
    public String getDescription() { return description; }

    public MysteryGame getBiomeMystery() {
        if (biomeMystery == null) {
            biomeMystery = new MysteryGame(biomeName + " Mystery", biomeName, animalList);
        }
        return biomeMystery;
    }

    public void resetProgress() { getBiomeMystery().reset(); }
}

package main; // NEW CODE (was: package mysterygame;)

import java.util.*;

/** A habitat (zone) holding 10 animals and one mystery game. */
public class Biome {
    private final String biomeName;
    private final String description;
    private boolean isLocked;
    private final List<Animal> animalList = new ArrayList<>();
    private MysteryGame biomeMystery;

    public Biome(String biomeName, String description, boolean isLocked) {
        this.biomeName = biomeName;
        this.description = description;
        this.isLocked = isLocked;
    }

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

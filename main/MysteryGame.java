package main;

import java.util.*;
import main.ZooQuest.AnimalNotFoundException;

public class MysteryGame {
    public static final int CHOICE_COUNT = 4;

    private final String mysteryTitle;
    private final String biomeName;
    private final List<Animal> animals;
    private final Set<String> foundAnimals = new LinkedHashSet<>();
    private final Random random = new Random();
    private final Map<String, Mystery> rounds = new LinkedHashMap<>(); // NEW CODE: one Mystery (answer tracker) per tapped animal

    private Animal targetAnimal;               // animal the player tapped, awaiting a guess
    private List<String> currentChoices = List.of();
    private boolean isSolved;

    public MysteryGame(String mysteryTitle, String biomeName, List<Animal> animals) {
        this.mysteryTitle = mysteryTitle;
        this.biomeName = biomeName;
        this.animals = List.copyOf(animals);
    }

    /** Player taps a hidden animal: reveals its clue and builds the multiple-choice list. */
    public MysteryClue selectHiddenAnimal(String animalName) throws AnimalNotFoundException {
        Animal animal = animals.stream()
                .filter(a -> a.getName().equalsIgnoreCase(animalName))
                .findFirst()
                .orElseThrow(() -> new AnimalNotFoundException(animalName, biomeName));
        if (foundAnimals.contains(animal.getName())) {
            throw new IllegalStateException(animal.getName() + " has already been found.");
        }
        targetAnimal = animal;
        rounds.computeIfAbsent(animal.getName(), n -> new Mystery(mysteryTitle + " / " + n, n)); // NEW CODE
        currentChoices = buildChoices(animal);
        return animal.getClue();   // UI displays getHintText()
    }

    private List<String> buildChoices(Animal correct) {
        List<String> distractors = new ArrayList<>();
        for (Animal a : animals) {
            if (a != correct) distractors.add(a.getName());
        }
        Collections.shuffle(distractors, random);
        List<String> choices = new ArrayList<>(distractors.subList(0, Math.min(CHOICE_COUNT - 1, distractors.size())));
        choices.add(correct.getName());
        Collections.shuffle(choices, random);
        return choices;
    }

    /** @return true if the guess is right. A right guess marks the animal found. */
    public boolean verifyGuess(String guess) {
        if (targetAnimal == null) throw new IllegalStateException("No animal selected.");
        // NEW CODE (was: if (!targetAnimal.getName().equalsIgnoreCase(guess)) return false;) - same rule, now via Mystery
        if (!rounds.get(targetAnimal.getName()).checkAnswer(guess)) return false;

        targetAnimal.getClue().reveal();
        foundAnimals.add(targetAnimal.getName());
        targetAnimal = null;
        currentChoices = List.of();
        if (allCluesFound()) solveMystery();
        return true;
    }

    public boolean allCluesFound() { return foundAnimals.size() == animals.size(); }
    private void solveMystery() { isSolved = true; } // NEW CODE: private (was public) - only a real completion may solve it

    /** Wipes all in-biome progress. */
    public void reset() {
        foundAnimals.clear();
        rounds.clear(); // NEW CODE
        animals.forEach(a -> a.getClue().hide());
        targetAnimal = null;
        currentChoices = List.of();
        isSolved = false;
    }

    public boolean isAnimalFound(String name) { return foundAnimals.contains(name); }
    public Set<String> getFoundAnimals() { return Collections.unmodifiableSet(foundAnimals); }
    public int getFoundCount() { return foundAnimals.size(); }
    public int getTotalCount() { return animals.size(); }
    public boolean isSolved() { return isSolved; }
    public Animal getTargetAnimal() { return targetAnimal; }
    public List<String> getCurrentChoices() { return List.copyOf(currentChoices); } // NEW CODE (was: return currentChoices;)
    public int getRoundCount() { return rounds.size(); } // NEW CODE: animals tapped so far
    public int getSolvedRoundCount() { // NEW CODE
        return (int) rounds.values().stream().filter(Mystery::isSolved).count();
    }
    public String getMysteryTitle() { return mysteryTitle; }
}

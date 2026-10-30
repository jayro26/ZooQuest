package main;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import main.ZooQuest.AnimalNotFoundException;
import main.ZooQuest.InsufficientCluesException;
import main.ZooQuest.ZoneLockedException;

/**
 * Controls the overall game flow: loading biomes, running the mystery game,
 * handling biome completion (achievement + unlock) and saving progress.
 *
 * Saving rule: progress is written ONLY when a biome is completed.
 * Leaving a biome mid-game discards that biome's progress and saves nothing.
 */
public class GameManager {
    public static final int BIOME_COUNT = 3;
    public static final int ANIMALS_PER_BIOME = 10;

    private static final char[] BIOME_TYPES = {'J', 'S', 'A'}; 
    private static final String[] BIOME_NAMES = {"Jungle", "Safari", "Aqua"};
    private static final String[] BIOME_DESCRIPTIONS = {"Description 1", "Description 2", "Description 3"};
  
    // Format per animal: { name, species, diet, status, behaviour, uniqueFeature (description), clueHint }
    private static final String[][][] ANIMAL_DATA = {
    // Biome 0: Jungle ('J') - 10 Animals
    {
        {"Jaguar", "Panthera onca", "Carnivore", "Near Threatened",
         "Stalks prey silently through dense undergrowth",
         "Powerful jaws and rosette-patterned camouflage coat",
         "I am the largest cat in the Americas and love to swim."},
        {"Toucan", "Ramphastos toco", "Omnivore", "Least Concern",
         "Hops between canopy branches plucking fruit",
         "Oversized colorful bill that regulates body heat",
         "My giant bright beak is lightweight and made of keratin."},
        {"Animal 3", "Species 3", "Diet 3", "Status 3",
         "Behaviour 3",
         "Unique Feature",
         "CLue 3"},
        {"Animal 4", "Species 4", "Diet 4", "Status 4",
         "Behaviour 4",
         "Unique Feature 4",
         "CLue 4"},
        {"Animal 5", "Species 5", "Diet 5", "Status 5",
         "Behaviour 5",
         "Unique Feature 5",
         "CLue 5"},
        {"Animal 6", "Species 6", "Diet 6", "Status 6",
         "Behaviour 6",
         "Unique Feature 6",
         "CLue 6"},
        {"Animal 7", "Species 7", "Diet 7", "Status 7",
         "Behaviour 7",
         "Unique Feature 7",
         "CLue 7"},
        {"Animal 8", "Species 8", "Diet 8", "Status 8",
         "Behaviour 8",
         "Unique Feature 8",
         "CLue 8"},
        {"Animal 9", "Species 9", "Diet 9", "Status 9",
         "Behaviour 9",
         "Unique Feature 9",
         "CLue 9"},
        {"Animal 10", "Species 10", "Diet 10", "Status 10",
         "Behaviour 10",
         "Unique Feature 10",
         "CLue 10"},
    },
    // Biome 1: Safari ('S') - 10 Animals
    {
        {"African Lion", "Panthera leo", "Carnivore", "Vulnerable",
         "Rests in prides during the heat and hunts at dusk",
         "Majestic mane on males and a roar heard 5 miles away",
         "I live in social prides and am known as the king of beasts."},
        {"Animal 3", "Species 3", "Diet 3", "Status 3",
         "Behaviour 3",
         "Unique Feature",
         "CLue 3"},
        {"Animal 4", "Species 4", "Diet 4", "Status 4",
         "Behaviour 4",
         "Unique Feature 4",
         "CLue 4"},
        {"Animal 5", "Species 5", "Diet 5", "Status 5",
         "Behaviour 5",
         "Unique Feature 5",
         "CLue 5"},
        {"Animal 6", "Species 6", "Diet 6", "Status 6",
         "Behaviour 6",
         "Unique Feature 6",
         "CLue 6"},
        {"Animal 7", "Species 7", "Diet 7", "Status 7",
         "Behaviour 7",
         "Unique Feature 7",
         "CLue 7"},
        {"Animal 8", "Species 8", "Diet 8", "Status 8",
         "Behaviour 8",
         "Unique Feature 8",
         "CLue 8"},
        {"Animal 9", "Species 9", "Diet 9", "Status 9",
         "Behaviour 9",
         "Unique Feature 9",
         "CLue 9"},
        {"Animal 10", "Species 10", "Diet 10", "Status 10",
         "Behaviour 10",
         "Unique Feature 10",
         "CLue 10"},
        {"Animal 11", "Species 11", "Diet 11", "Status 11",
         "Behaviour 11",
         "Unique Feature 11",
         "CLue 11"},
    },
    // Biome 2: Aquatic ('A') - 10 Animals
    {
        {"Bottlenose Dolphin", "Tursiops truncatus", "Carnivore", "Least Concern",
         "Communicates with clicks and leaps across waves",
         "High intelligence and built-in echolocation sonar",
         "I breathe air through a blowhole and use sonar to hunt fish."},
        {"Animal 3", "Species 3", "Diet 3", "Status 3",
         "Behaviour 3",
         "Unique Feature",
         "CLue 3"},
        {"Animal 4", "Species 4", "Diet 4", "Status 4",
         "Behaviour 4",
         "Unique Feature 4",
         "CLue 4"},
        {"Animal 5", "Species 5", "Diet 5", "Status 5",
         "Behaviour 5",
         "Unique Feature 5",
         "CLue 5"},
        {"Animal 6", "Species 6", "Diet 6", "Status 6",
         "Behaviour 6",
         "Unique Feature 6",
         "CLue 6"},
        {"Animal 7", "Species 7", "Diet 7", "Status 7",
         "Behaviour 7",
         "Unique Feature 7",
         "CLue 7"},
        {"Animal 8", "Species 8", "Diet 8", "Status 8",
         "Behaviour 8",
         "Unique Feature 8",
         "CLue 8"},
        {"Animal 9", "Species 9", "Diet 9", "Status 9",
         "Behaviour 9",
         "Unique Feature 9",
         "CLue 9"},
        {"Animal 10", "Species 10", "Diet 10", "Status 10",
         "Behaviour 10",
         "Unique Feature 10",
         "CLue 10"},
        {"Animal 11", "Species 11", "Diet 11", "Status 11",
         "Behaviour 11",
         "Unique Feature 11",
         "CLue 11"},
    }
};

    private final List<Biome> allBiomes = new ArrayList<>();
    private final Path saveDirectory;
    private UserProfile currentPlayer;
    private Biome currentBiome;
    private int currentBiomeIndex = -1;

    /** Result of one guess, handed to the UI. */
    public record GuessResult(boolean correct, Animal revealedAnimal, int foundCount,
                              int totalCount, boolean biomeCompleted, Achievement achievementEarned) { }

    public GameManager(Path saveDirectory) {
        this.saveDirectory = saveDirectory;
    }

    // ---------------------------------------------------------------- setup

    /** Builds the biomes, creates the player and restores any saved progress. */
    public void initializeGame(String username) {
        currentPlayer = new UserProfile(username);
        currentBiome = null;
        currentBiomeIndex = -1;
        buildBiomes();
        loadProgress();
    }

    private void buildBiomes() {
    allBiomes.clear();
    for (int b = 0; b < BIOME_COUNT; b++) {
        String biomeName = BIOME_NAMES[b];
        Biome biome = new Biome(biomeName, BIOME_DESCRIPTIONS[b], b != 0);

        for (int a = 0; a < ANIMALS_PER_BIOME; a++) {
            String[] data = ANIMAL_DATA[b][a];
            String animalName = data[0];
            String species    = data[1];
            String diet       = data[2];
            String status     = data[3];
            String behaviour  = data[4];
            String feature    = data[5]; // Shown in getDescription()
            String clueText   = data[6];

            MysteryClue clue = new MysteryClue(
                    biomeName + "_clue_" + (a + 1), clueText, animalName);

            Animal animal = Animal.create(
                    BIOME_TYPES[b], animalName, species, biomeName,
                    diet, status, behaviour, feature);

            animal.setAssociatedClue(clue);
            biome.addAnimal(animal);
        }
        allBiomes.add(biome);
    }
}

    // ------------------------------------------------------------ gameplay

    /** Enters a biome with a fresh state. Throws if the biome is still locked. */
    public void startBiome(int biomeIndex) throws ZoneLockedException {
        requireInitialized();
        if (biomeIndex < 0 || biomeIndex >= allBiomes.size()) {
            throw new IllegalArgumentException("No biome at index " + biomeIndex);
        }
        Biome biome = allBiomes.get(biomeIndex);
        if (biome.isLocked()) {
            throw new ZoneLockedException(biome.getBiomeName(), biomeIndex - 1);
        }
        biome.resetProgress();          // every attempt starts from zero
        currentBiome = biome;
        currentBiomeIndex = biomeIndex;
    }

    /** Player taps a hidden animal; returns the clue to show. Choices come from getCurrentChoices(). */
    public MysteryClue selectHiddenAnimal(String animalName) throws AnimalNotFoundException {
        requireActiveBiome();
        return currentBiome.getBiomeMystery().selectHiddenAnimal(animalName);
    }

    public List<String> getCurrentChoices() {
        requireActiveBiome();
        return currentBiome.getBiomeMystery().getCurrentChoices();
    }

    /** Checks the player's choice. Completes the biome automatically when the last animal is found. */
    public GuessResult processGuess(String guess) {
        requireActiveBiome();
        MysteryGame mystery = currentBiome.getBiomeMystery();
        Animal target = mystery.getTargetAnimal();

        if (!mystery.verifyGuess(guess)) {
            return new GuessResult(false, null, mystery.getFoundCount(), mystery.getTotalCount(), false, null);
        }

        Achievement earned = null;
        boolean completed = mystery.allCluesFound();
        if (completed) {
            try {
                earned = handleBiomeCompletion();
            } catch (InsufficientCluesException e) {
                throw new IllegalStateException(e);   // cannot happen: allCluesFound() was true
            }
        }
        return new GuessResult(true, target, mystery.getFoundCount(), mystery.getTotalCount(), completed, earned);
    }

    /** Awards the badge, marks progress, unlocks the next biome and saves. */
    public Achievement handleBiomeCompletion() throws InsufficientCluesException {
        requireActiveBiome();
        MysteryGame mystery = currentBiome.getBiomeMystery();
        if (!mystery.allCluesFound()) {
            throw new InsufficientCluesException(mystery.getFoundCount(), mystery.getTotalCount());
        }

        Achievement badge = new Achievement(
                currentBiome.getBiomeName() + "_badge",
                "Solved every mystery in " + currentBiome.getBiomeName(),
                "images/badges/" + currentBiome.getBiomeName() + ".png",
                LocalDate.now());
        currentPlayer.addAchievement(badge);
        currentPlayer.markBiomeCompleted(currentBiomeIndex);

        if (currentBiomeIndex + 1 < allBiomes.size()) {
            allBiomes.get(currentBiomeIndex + 1).unlockBiome();
        }

        try {
            saveProgress();            // the only place progress is written
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save progress", e);
        }
        return badge;
    }

    /**
     * Player leaves the biome (back button, window closed, etc.).
     * Unfinished progress is discarded and nothing is saved.
     */
    public void exitCurrentBiome() {
        if (currentBiome != null) {
            currentBiome.resetProgress();
            currentBiome = null;
            currentBiomeIndex = -1;
        }
    }

    // ---------------------------------------------------------- persistence

    /** Writes completed biomes and badge dates. Called only from handleBiomeCompletion(). */
    public void saveProgress() throws IOException {
        requireInitialized();
        Files.createDirectories(saveDirectory);
        Properties props = new Properties();
        props.setProperty("username", currentPlayer.getUsername());

        StringJoiner completed = new StringJoiner(",");
        for (int index : currentPlayer.getUserProgress()) completed.add(String.valueOf(index));
        props.setProperty("completedBiomes", completed.toString());

        for (Achievement a : currentPlayer.getUserAchievements()) {
            props.setProperty("badgeDate." + a.getBadgeName(), a.getDateEarned().toString());
        }
        try (OutputStream out = Files.newOutputStream(saveFile())) {
            props.store(out, "Mystery game save");
        }
    }

    private void loadProgress() {
        Path file = saveFile();
        if (!Files.exists(file)) return;

        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            props.load(in);
        } catch (IOException e) {
            return;   // unreadable save: start fresh
        }

        String completed = props.getProperty("completedBiomes", "");
        for (String token : completed.split(",")) {
            if (token.isBlank()) continue;
            int index = Integer.parseInt(token.trim());
            if (index < 0 || index >= allBiomes.size()) continue;

            Biome biome = allBiomes.get(index);
            biome.unlockBiome();
            currentPlayer.markBiomeCompleted(index);
            if (index + 1 < allBiomes.size()) allBiomes.get(index + 1).unlockBiome();

            String badgeName = biome.getBiomeName() + "_badge";
            LocalDate date = LocalDate.parse(props.getProperty("badgeDate." + badgeName, LocalDate.now().toString()));
            currentPlayer.addAchievement(new Achievement(badgeName,
                    "Solved every mystery in " + biome.getBiomeName(),
                    "images/badges/" + biome.getBiomeName() + ".png", date));
        }
    }

    private Path saveFile() {
        String safe = currentPlayer.getUsername().replaceAll("[^a-zA-Z0-9_-]", "_");
        return saveDirectory.resolve(safe + ".save");
    }

    // ------------------------------------------------------------ accessors

    public List<Biome> getAllBiomes() { return Collections.unmodifiableList(allBiomes); }
    public UserProfile getCurrentPlayer() { return currentPlayer; }
    public Biome getCurrentBiome() { return currentBiome; }
    public int getCurrentBiomeIndex() { return currentBiomeIndex; }
    public boolean hasNextBiome() { return currentBiomeIndex + 1 < allBiomes.size(); }

    private void requireInitialized() {
        if (currentPlayer == null) throw new IllegalStateException("Call initializeGame() first.");
    }

    private void requireActiveBiome() {
        requireInitialized();
        if (currentBiome == null) throw new IllegalStateException("No biome started. Call startBiome() first.");
    }
}

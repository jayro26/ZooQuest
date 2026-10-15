package mysterygame;

import mysterygame.Main.ZoneLockedException;
import mysterygame.Main.InsufficientCluesException;
import mysterygame.Main.AnimalNotFoundException;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

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

    private static final String[] NUMBER_WORDS =
            {"one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten"};

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
            String biomeName = "biome_" + NUMBER_WORDS[b];
            Biome biome = new Biome(biomeName, "Description of " + biomeName, b != 0); // only the first starts unlocked
            for (int a = 0; a < ANIMALS_PER_BIOME; a++) {
                String animalName = "animal_" + NUMBER_WORDS[a];
                MysteryClue clue = new MysteryClue(biomeName + "_clue_" + (a + 1),
                        "Clue for " + animalName + " in " + biomeName, animalName);
                biome.addAnimal(new Animal(animalName, "species_" + NUMBER_WORDS[a], biomeName,
                        "diet_" + NUMBER_WORDS[a], "feature_" + NUMBER_WORDS[a],
                        "images/" + biomeName + "/" + animalName + ".png", clue));
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

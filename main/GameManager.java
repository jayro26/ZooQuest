package main;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException; // NEW CODE
import java.util.*;
import main.ZooQuest.AnimalNotFoundException;
import main.ZooQuest.InsufficientCluesException;
import main.ZooQuest.ZoneLockedException;

/**
 * Controls the overall game flow: loading biomes, running the mystery game,
 * handling biome completion (achievement + unlock) and saving progress.
 *
 * Saving rule: progress is written ONLY when a biome is completed (or the player is renamed).
 * Leaving a biome mid-game discards that biome's progress and saves nothing.
 *
 * NEW CODE: this class now also owns the achievement rules (two badges per biome), the clue mode of a run,
 * renaming and the read-only queries the UI needs, so a UI never has to touch the domain objects directly.
 */
public class GameManager {
    public static final int BIOME_COUNT = 3;
    public static final int ANIMALS_PER_BIOME = 10;
    public static final int BADGES_PER_BIOME = 2; // NEW CODE: Clear badge + No-Clue badge

    private static final char[] BIOME_TYPES = {'J', 'S', 'A'}; // NEW CODE: Jungle, Safari, Aquatic
    private static final String[] BIOME_NAMES = {"Jungle", "Safari", "Aqua"};
    private static final String[] BIOME_DESCRIPTIONS = {"Description 1", "Description 2", "Description 3"};
    private static final String HIDDEN_CLUE_TEXT = "(clue hidden)"; // NEW CODE

    // NEW CODE: the animal descriptions (name, species, diet, status, behaviour, unique feature, clue hint)
    // no longer live here. Each animal type owns its animals: see Jungle, Safari and Aquatic.
    // Animal.createAnimalsFor(type, biomeName) returns them with their clues attached.

    private final List<Biome> allBiomes = new ArrayList<>();
    private final Path saveDirectory;
    private UserProfile currentPlayer;
    private Biome currentBiome;
    private int currentBiomeIndex = -1;

    // NEW CODE: state of the run in progress (reset whenever a biome is started or left)
    private boolean cluesOnTap = true;   // true = clue comes with every tap, false = no-clue challenge
    private boolean cluesUsed;           // a clue was delivered during this run -> No-Clue badge is out of reach
    private boolean clueShown;           // the clue of the currently tapped animal was delivered
    private int wrongGuesses;

    /** Result of one guess, handed to the UI. */
    /**
     * NEW CODE: achievementEarned is the biome's Clear badge (null until the biome is completed; on a replay it is
     * the badge the player already owned). newAchievements lists only the badges actually awarded by this guess.
     */
    public record GuessResult(boolean correct, Animal revealedAnimal, int foundCount,
                              int totalCount, boolean biomeCompleted, Achievement achievementEarned,
                              List<Achievement> newAchievements) { }

    /** NEW CODE: what the biome list shows for a biome. PERFECT = both badges earned. */
    public enum BiomeStatus { LOCKED, UNLOCKED, COMPLETED, PERFECT }

    /** NEW CODE: one entry of the saved-player list (start screen): name + a short progress summary. */
    public record SavedPlayer(String username, int completedBiomes, int badges) { }

    /**
     * NEW CODE: where the saves live - the "saves" folder inside the project's main folder (main/saves).
     * Works whether the program is started from the project folder (which contains main/) or from inside main/.
     * A "saves" folder left next to main/ by an earlier version is moved here once, so existing players are kept.
     */
    public static Path defaultSaveDirectory() {
        Path here = Path.of("").toAbsolutePath();
        boolean insideMain = here.getFileName() != null && here.getFileName().toString().equals("main");
        Path target = insideMain ? here.resolve("saves") : here.resolve("main").resolve("saves");

        Path legacy = here.resolve("saves");                         // the old location
        if (!legacy.equals(target) && Files.isDirectory(legacy) && !Files.exists(target)) {
            try {
                Files.createDirectories(target.getParent());
                Files.move(legacy, target);
            } catch (IOException e) {
                // could not move it: carry on with the new (empty) folder
            }
        }
        return target;
    }

    public GameManager(Path saveDirectory) {
        this.saveDirectory = saveDirectory;
    }

    // ---------------------------------------------------------------- setup

    /** Builds the biomes, creates the player and restores any saved progress. */
    public void initializeGame(String username) {
        currentPlayer = new UserProfile(username);
        currentBiome = null;
        currentBiomeIndex = -1;
        resetRunState(); // NEW CODE
        buildBiomes();
        loadProgress();
    }

    private void buildBiomes() {
        allBiomes.clear();
        for (int b = 0; b < BIOME_COUNT; b++) {
            String biomeName = BIOME_NAMES[b];
            String biomeDesc = BIOME_DESCRIPTIONS[b];

            Biome biome = Biome.create(BIOME_TYPES[b], biomeName, biomeDesc, b != 0);

            // NEW CODE (replaces the loop that copied rows of ANIMAL_DATA into Animal.create(...) and MysteryClue):
            // the animal type that lives in this biome supplies its animals, each with its clue attached
            List<Animal> animals = Animal.createAnimalsFor(biome.getAnimalTypeCode(), biomeName);
            if (animals.size() != ANIMALS_PER_BIOME) {
                throw new IllegalStateException(biomeName + " has " + animals.size()
                        + " animals but ANIMALS_PER_BIOME is " + ANIMALS_PER_BIOME);
            }
            animals.forEach(biome::addAnimal);
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
        resetRunState();                // NEW CODE: default clue mode = clues on tap
    }

    /**
     * NEW CODE: chooses the clue mode of this run. true (default) = the clue is delivered with every tap;
     * false = no-clue challenge: clues stay hidden until revealClue() is called, which forfeits the No-Clue badge.
     * Only allowed before the first animal is tapped.
     */
    public void setClueMode(boolean cluesOnTap) {
        requireActiveBiome();
        if (currentBiome.getBiomeMystery().getRoundCount() > 0) {
            throw new IllegalStateException("The clue mode can only be chosen before the first animal is tapped.");
        }
        this.cluesOnTap = cluesOnTap;
    }

    /**
     * Player taps a hidden animal; returns the clue to show. Choices come from getCurrentChoices().
     * NEW CODE: in the no-clue challenge the returned clue is a masked placeholder (its text is "(clue hidden)")
     * until revealClue() is called; use isClueShown() to know whether the real clue was delivered.
     */
    public MysteryClue selectHiddenAnimal(String animalName) throws AnimalNotFoundException {
        requireActiveBiome();
        MysteryClue clue = currentBiome.getBiomeMystery().selectHiddenAnimal(animalName);
        clueShown = cluesOnTap || cluesUsed;       // once a clue was used, the badge is gone: clues keep coming
        if (clueShown) {
            cluesUsed = true;
            return clue;
        }
        return new MysteryClue(clue.getClueId(), HIDDEN_CLUE_TEXT, clue.getAssociatedAnimalName());
    }

    /** NEW CODE: reveals the real clue of the tapped animal. Counts as using a clue (forfeits the No-Clue badge). */
    public String revealClue() {
        requireActiveBiome();
        Animal target = currentBiome.getBiomeMystery().getTargetAnimal();
        if (target == null) throw new IllegalStateException("No animal selected.");
        clueShown = true;
        cluesUsed = true;
        return target.getClue().getHintText();
    }

    public boolean isClueShown() { return clueShown; }       // NEW CODE: real clue delivered for the current tap
    public boolean hasUsedClues() { return cluesUsed; }      // NEW CODE: any clue delivered during this run

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
            wrongGuesses++; // NEW CODE
            return new GuessResult(false, null, mystery.getFoundCount(), mystery.getTotalCount(), false, null, List.of());
        }
        clueShown = false; // NEW CODE: the tap is over

        Achievement clearBadge = null;                        // NEW CODE
        List<Achievement> newBadges = List.of();              // NEW CODE
        boolean completed = mystery.allCluesFound();
        if (completed) {
            try {
                newBadges = awardCompletionBadges();          // NEW CODE (was: earned = handleBiomeCompletion();)
                clearBadge = currentPlayer.findAchievement(clearBadgeName(currentBiome.getBiomeName())).orElse(null);
            } catch (InsufficientCluesException e) {
                throw new IllegalStateException(e);   // cannot happen: allCluesFound() was true
            }
        }
        return new GuessResult(true, target, mystery.getFoundCount(), mystery.getTotalCount(), completed,
                clearBadge, newBadges);
    }

    /**
     * Awards the Clear badge, marks progress, unlocks the next biome and saves.
     * NEW CODE: returns the biome's Clear badge (a replay returns the one the player already owns);
     * the No-Clue badge is awarded too when the run was clue-free.
     */
    public Achievement handleBiomeCompletion() throws InsufficientCluesException {
        awardCompletionBadges();
        return currentPlayer.findAchievement(clearBadgeName(currentBiome.getBiomeName())).orElseThrow();
    }

    /** NEW CODE: the actual completion logic. @return the badges newly awarded by this completion. */
    private List<Achievement> awardCompletionBadges() throws InsufficientCluesException {
        requireActiveBiome();
        MysteryGame mystery = currentBiome.getBiomeMystery();
        if (!mystery.allCluesFound()) {
            throw new InsufficientCluesException(mystery.getFoundCount(), mystery.getTotalCount());
        }

        String biomeName = currentBiome.getBiomeName();
        List<Achievement> earned = new ArrayList<>();

        // Badge 1: completing the mystery game
        if (!currentPlayer.hasAchievement(clearBadgeName(biomeName))) {
            Achievement badge = new Achievement(
                    clearBadgeName(biomeName),
                    "Solved every mystery in " + biomeName,
                    "images/badges/" + biomeName + ".png",
                    LocalDate.now());
            currentPlayer.addAchievement(badge);
            earned.add(badge);
        }
        // Badge 2: completing it without any clue being delivered
        if (!cluesUsed && !currentPlayer.hasAchievement(noClueBadgeName(biomeName))) {
            Achievement badge = new Achievement(
                    noClueBadgeName(biomeName),
                    "Solved every mystery in " + biomeName + " without using a clue",
                    "images/badges/" + biomeName + "_no_clue.png",
                    LocalDate.now());
            currentPlayer.addAchievement(badge);
            earned.add(badge);
        }
        currentPlayer.markBiomeCompleted(currentBiomeIndex);

        if (currentBiomeIndex + 1 < allBiomes.size()) {
            allBiomes.get(currentBiomeIndex + 1).unlockBiome();
        }

        try {
            saveProgress();            // the only place progress is written
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save progress", e);
        }
        return earned;
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
        resetRunState(); // NEW CODE
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

            String badgeName = clearBadgeName(biome.getBiomeName()); // NEW CODE (was: biome.getBiomeName() + "_badge")
            LocalDate date = LocalDate.parse(props.getProperty("badgeDate." + badgeName, LocalDate.now().toString()));
            currentPlayer.addAchievement(new Achievement(badgeName,
                    "Solved every mystery in " + biome.getBiomeName(),
                    "images/badges/" + biome.getBiomeName() + ".png", date));

            // NEW CODE: restore the No-Clue badge when the save contains one
            String noClueDate = props.getProperty("badgeDate." + noClueBadgeName(biome.getBiomeName()));
            if (noClueDate != null) {
                currentPlayer.addAchievement(new Achievement(noClueBadgeName(biome.getBiomeName()),
                        "Solved every mystery in " + biome.getBiomeName() + " without using a clue",
                        "images/badges/" + biome.getBiomeName() + "_no_clue.png", parseDateOrToday(noClueDate)));
            }
        }
    }

    private Path saveFile() {
        return saveFileFor(currentPlayer.getUsername()); // NEW CODE (rule moved to saveFileFor)
    }

    // NEW CODE
    private Path saveFileFor(String username) {
        String safe = username.replaceAll("[^a-zA-Z0-9_-]", "_");
        return saveDirectory.resolve(safe + ".save");
    }

    // NEW CODE
    private static LocalDate parseDateOrToday(String text) {
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            return LocalDate.now();
        }
    }

    // NEW CODE: renaming - UserProfile validates the name, the save file follows the new name
    /**
     * Renames the player and moves the save file along with it.
     * @return false when the name did not change
     * @throws IllegalArgumentException if the name is blank/too long or already belongs to another saved player
     */
    public boolean renamePlayer(String newName) throws IOException {
        requireInitialized();
        String candidate = UserProfile.validateUsername(newName);
        if (candidate.equals(currentPlayer.getUsername())) return false;

        Path oldFile = saveFile();
        Path newFile = saveFileFor(candidate);
        boolean sameFile = oldFile.equals(newFile);     // e.g. "a b" and "a_b" map to one file
        if (!sameFile && Files.exists(newFile)) {
            throw new IllegalArgumentException("Another player already uses the name '" + candidate + "'.");
        }
        currentPlayer.setUsername(candidate);
        // NEW CODE (was: only when there was progress): a registered player has a save file even with no progress
        if (!currentPlayer.getUserProgress().isEmpty() || Files.exists(oldFile)) saveProgress();
        if (!sameFile) Files.deleteIfExists(oldFile);
        return true;
    }

    // ------------------------------------------------------------ saved players (NEW CODE)
    // Every player owns exactly one save file (saves/<name>.save) holding their completed biomes and badges.
    // Loading one player never touches another player's file.

    /** All players that have a save file, sorted by name. Never throws; an unreadable file is skipped. */
    public List<SavedPlayer> listSavedPlayers() {
        if (!Files.isDirectory(saveDirectory)) return List.of();
        List<SavedPlayer> players = new ArrayList<>();
        try (var files = Files.list(saveDirectory)) {
            for (Path file : (Iterable<Path>) files::iterator) {
                String fileName = file.getFileName().toString();
                if (!fileName.endsWith(".save") || !Files.isRegularFile(file)) continue;

                Properties props = new Properties();
                try (InputStream in = Files.newInputStream(file)) {
                    props.load(in);
                } catch (IOException e) {
                    continue;
                }
                String name = props.getProperty("username", "").trim();
                if (name.isEmpty()) name = fileName.substring(0, fileName.length() - ".save".length());
                players.add(new SavedPlayer(name, countCompletedBiomes(props), countBadges(props)));
            }
        } catch (IOException e) {
            return List.of();
        }
        players.sort(Comparator.comparing((SavedPlayer p) -> p.username().toLowerCase()));
        return List.copyOf(players);
    }

    /**
     * Registers a new player: starts a fresh profile and writes its (empty) save file right away,
     * so the player shows up in listSavedPlayers() next time the game starts.
     * @throws IllegalArgumentException if the name is blank/too long or already belongs to a saved player
     */
    public void createPlayer(String username) throws IOException {
        String name = UserProfile.validateUsername(username);
        boolean taken = Files.exists(saveFileFor(name))
                || listSavedPlayers().stream().anyMatch(p -> p.username().equalsIgnoreCase(name));
        if (taken) {
            throw new IllegalArgumentException("The name '" + name + "' is already taken by another player.");
        }
        initializeGame(name);
        saveProgress();
    }

    /**
     * Switches to an existing player and restores their biomes and achievements.
     * @throws IllegalArgumentException if no save exists for that name
     */
    public void loadPlayer(String username) {
        String name = UserProfile.validateUsername(username);
        if (!Files.exists(saveFileFor(name))) {
            throw new IllegalArgumentException("There is no saved player named '" + name + "'.");
        }
        initializeGame(name);
    }

    private static int countCompletedBiomes(Properties props) {
        Set<Integer> done = new HashSet<>();
        for (String token : props.getProperty("completedBiomes", "").split(",")) {
            try {
                int index = Integer.parseInt(token.trim());
                if (index >= 0 && index < BIOME_COUNT) done.add(index);
            } catch (NumberFormatException ignored) {
                // blank or corrupt token
            }
        }
        return done.size();
    }

    private static int countBadges(Properties props) {
        return (int) props.stringPropertyNames().stream().filter(k -> k.startsWith("badgeDate.")).count();
    }

    // ------------------------------------------------------------ badge rules (NEW CODE)

    public static String clearBadgeName(String biomeName) { return biomeName + "_badge"; }
    public static String noClueBadgeName(String biomeName) { return biomeName + "_no_clue_badge"; }

    public boolean hasClearBadge(int biomeIndex) {
        requireInitialized();
        return currentPlayer.hasAchievement(clearBadgeName(allBiomes.get(biomeIndex).getBiomeName()));
    }

    public boolean hasNoClueBadge(int biomeIndex) {
        requireInitialized();
        return currentPlayer.hasAchievement(noClueBadgeName(allBiomes.get(biomeIndex).getBiomeName()));
    }

    public int getBadgeCount(int biomeIndex) {
        return (hasClearBadge(biomeIndex) ? 1 : 0) + (hasNoClueBadge(biomeIndex) ? 1 : 0);
    }

    public BiomeStatus getBiomeStatus(int biomeIndex) {
        requireInitialized();
        if (!currentPlayer.getUserProgress().contains(biomeIndex)) {
            return allBiomes.get(biomeIndex).isLocked() ? BiomeStatus.LOCKED : BiomeStatus.UNLOCKED;
        }
        return hasNoClueBadge(biomeIndex) ? BiomeStatus.PERFECT : BiomeStatus.COMPLETED;
    }

    // ------------------------------------------------- run queries for the UI (NEW CODE)

    public String getMysteryTitle() { requireActiveBiome(); return currentBiome.getBiomeMystery().getMysteryTitle(); }
    public int getFoundCount() { requireActiveBiome(); return currentBiome.getBiomeMystery().getFoundCount(); }
    public int getTotalCount() { requireActiveBiome(); return currentBiome.getBiomeMystery().getTotalCount(); }
    public boolean isAnimalFound(String animalName) {
        requireActiveBiome();
        return currentBiome.getBiomeMystery().isAnimalFound(animalName);
    }
    public int getRoundCount() { requireActiveBiome(); return currentBiome.getBiomeMystery().getRoundCount(); }
    public int getSolvedRoundCount() { requireActiveBiome(); return currentBiome.getBiomeMystery().getSolvedRoundCount(); }
    public int getWrongGuessCount() { return wrongGuesses; }

    private void resetRunState() {
        cluesOnTap = true;
        cluesUsed = false;
        clueShown = false;
        wrongGuesses = 0;
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
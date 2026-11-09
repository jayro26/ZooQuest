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

    // Format per animal: { name, species, diet, status, behaviour, uniqueFeature (description), clueHint }
    private static final String[][][] ANIMAL_DATA = {
    // Biome 0: Jungle ('J') - 10 Animals
    {
        {"Tiger", "Panthera tigris", "Carnivore", "Endangered",
         "Ambushes prey alone and swims across rivers with ease",
         "Orange coat with black stripes that are unique to every tiger",
         "I am the biggest cat in the world and my stripes are like fingerprints."},

        {"Gorilla", "Gorilla gorilla", "Herbivore", "Critically Endangered",
         "Lives in a family troop led by a silverback",
         "Silver-grey back fur on adult males and strong arms for knuckle-walking",
         "I am the largest living primate and troop follows a silverback leader."},

        {"Monkey", "Cebus capucinus", "Omnivore", "Varies by Species",
         "Travles in noisy troops and grooms friends to bond",
         "Long grasping tail that works like a fifth hand",
         "I swing through the treetops using my tail and never travel alone"},

        {"Eagle", "Harpia harpyja", "Carnivore", "Near Threatened",
         "Hunts from a perch and snatches monkeys and sloths from the trees",
         "Huge talons as big as a bear's claws and a crown of feathers",
         "I rule the rainforest sky and my talons are as big as a bear's claws."},

        {"Lemur", "Lemur catta", "Herbivore", "Endangered",
         "Sunbathes in groups and scent-marks with its tail",
         "Black-and-white ringed tail held high like a flag",
         "I come from Madagascar and wave my striped tail like a flag"},

        {"Koala", "Phascolarctos cinereus", "Herbivore", "Vulnerable",
         "Sleeps 18-22 hours a day and spends most of its life alone",
         "Fluffy round ears and fingerprints almost like a human's",
         "I'm not a bear! I munch eucalyptus leaves and sleep most of the day."},

        {"Deer", "Cervus elaphus", "Herbivore", "Varies by species",
         "Most active at dawn and dusk; males clash antlers in rutting season",
         "Antlers that fall off and regrow every year",
         "I grow a new set of antlers every year and come out at sunrise and sunset."},

        {"Parrot", "Ara macao", "Herbivore", "Many species threatened",
         "Flocks together and copies the sounds it hears",
         "Bright feathers and a strong curved beak for cracking nuts",
         "I wear all the colors of the rainbow and can copy your voice."},

        {"Capybara", "Hydrochoerus hydrochoerus", "Herbivore", "Least Concern",
         "Semi-aquatic and lives in groups of up to about 20",
         "Webbed feet and eyes and nose set high on its head for swimming",
         "I am the world's largest rodent and I love a good swim."},

        {"Komodo dragon", "Varanus komodoensis", "Carnivore", "Endangered",
         "Ambushes prey alone, then tracks it after a venomous bite",
         "Forked tongue that smells the air and a venomous bite",
         "I am the largest lizard alive and I live on islands in Indonesia."},
    },
    // Biome 1: Safari ('S') - 10 Animals
    {
        {"Zebra", "Equus quagga", "Herbivore", "Near Threatened",
         "Travels in herds, where moving stripes confuse predators",
         "Black-and-white stripes that are unique to each zebra",
         "No two of my stripe patterns are the same."},

        {"Rhino", "Diceros bicornis", "Herbivore", "Critically Endangered",
         "Lives alone with poor eyesight but sharp hearing and smell",
         "Two horns made of keratin, the same stuff as your fingernails",
         "My two horns are made of the same stuff as your fingernails."},

        {"Hippopotamus", "Hippopotamus amphibius", "Herbivore", "Vulnerable",
         "Rests in water by day and grazes on land at night",
         "Enormous jaws and skin that oozes a red, sunscreen-like liquid",
         "I soak in the river all day and come out at night to eat grass."},

        {"Giraffe", "Giraffa camelopardalis", "Herbivore", "Endangered",
         "Lives in loose herds and sleeps only in short naps",
         "Tallest animal on land with a long dark tongue for grabbing leaves",
         "I am the tallest animal on Earth and I eat the highest acacia leaves."},

        {"Elephant", "Loxodonta africana", "Herbivore", "Vulnerable",
         "Female-led herds with a famous memory",
         "Trunk with about 40,000 muscles and huge fan-shaped ears",
         "My trunk is my nose, my hand and my drinking straw all in one."},

        {"Hyena", "Crocuta crocuta", "Omnivore", "Least Concern",
         "Lives in female-led clans and makes laughing calls",
         "Powerful jaws that can crush bones",
         "I may sound like I'm laughing, but I'm the boss of my clan."},

        {"Ostrich", "Struthio camelus", "Omnivore", "Least Concern",
         "Cannot fly but runs up to about 70 km/h",
         "Largest bird on Earth with enormous eyes",
         "I can't fly, but I can outrun almost anything on the savanna."},

        {"Buffalo", "Syncerus caffer", "Herbivore", "Near Threatened",
         "Herds stand together to protect their calves from predators",
         "Heavy curved horns that meet across the forehead",
         "My herd always has my back, and my curved horns are heavy."},

        {"Nile Crocodile", "Crocodylus niloticus", "Carnivore", "Least Concern",
         "Basks in the sun and ambushes prey at the water's edge",
         "Eyes and nostrils on top of its head so it can hide underwater",
         "I float like a log in the river, then SNAP!"},

        {"Vulture", "Gyps africanus", "Carnivore", "Critically Endangered",
         "Soars on warm air currents and cleans up carcasses",
         "Bald head that stays clean while feeding and very strong stomach acid",
         "I'm nature's clean-up crew and I ride warm air high above the savanna."},
    },
    // Biome 2: Aquatic ('A') - 10 Animals
    {
        {"Tuna", "Thunnus thynnus", "Carnivore", "Varies (southern bluefin: Endangered)",
         "Swims in fast schools and migrates across whole oceans",
         "Torpedo-shaped body built for speed",
         "I'm built like a torpedo and I cross entire oceans."},

        {"Shark", "Carcharodon carcharias", "Carnivore", "Vulnerable",
         "Ambushes seals from below and sometimes leaps out of the water",
         "Rows of teeth that are replaced all through its life",
         "I lose thousands of teeth in my life, but I always grow more."},

        {"Whale", "Balaenoptera musculus", "Carnivore", "Endangered",
         "Sings long, deep calls to communicate across the ocean",
         "Largest animal ever with a heart the size of a small car",
         "I am the largest animal on Earth and I sing songs across the sea."},

        {"Penguin", "Aptenodytes forsteri", "Carnivore", "Near threatened",
         "Males huddle together to keep the eggs warm",
         "Flipper-like wings for swimming and thick, waterproof feathers",
         "I can't fly, but I zoom underwater and dad keeps our egg warm."}, 

        {"Swordfish", "Xiphias gladius", "Carnivore", "Least Concern",
         "Slashes at prey with its bill and usually swims alone",
         "Long, flat bill shaped like a sword",
         "I swim alone and my long, flat bill is like a sword."},

        {"Sea lion", "Zalophus californianus", "Carnivore", "Least Concern",
         "Lives in noisy colonies and walks on its flippers",
         "Visible ear flaps and front flippers that can turn to walk",
         "I bark loudly and have little ear flaps, unlike seals."},

        {"Dolphin", "Tursiops truncatus", "Carnivore", "Least concern",
         "Hunts in pods and finds food using echolocation",
         "Sleeps with half its brain awake and uses whistles as names",
         "I click and whistle to find food and call my friends."},

        {"Octupus", "Octupus vulgaris", "Carnivore", "Least Concern",
         "Hides in crevices and changes color to disappear",
         "Three hearts, blue blood and eight clever arms",
         "I have eight arms, three hearts and I can change my color."},

        {"Stringray", "Dasyatis americana", "Carnivore", "Varies by species",
         "Hides under the sand and defends itself with a barbed tail",
         "Flat, wing-like body with eyes on top of its head",
         "I look like a flying carpet and I hide under the sand."},

        {"Manta ray", "Mobula birostris", "Carnivore", "Endagered",
         "Visits cleaning stations where small fish clean its skin",
         "Wingspan up to about 7 meters and a big brain for a fish",
         "My wings can stretch wider than a boat and I seat tiny plankton"},
    }
};

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
        
        for (int a = 0; a < ANIMALS_PER_BIOME; a++) {
            String[] data = ANIMAL_DATA[b][a];
            String animalName    = data[0];
            String species       = data[1];
            String diet          = data[2];
            String status        = data[3];
            String behaviour     = data[4];
            String uniqueFeature = data[5];
            String clueHint      = data[6];

            MysteryClue clue = new MysteryClue(
                    biomeName + "_clue_" + (a + 1),
                    clueHint,
                    animalName
            );

            Animal animal = Animal.create(
                    biome.getAnimalTypeCode(),
                    animalName,
                    species,
                    biomeName, // habitat
                    diet,
                    status,
                    behaviour,
                    uniqueFeature
            );
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
        if (!currentPlayer.getUserProgress().isEmpty()) saveProgress();   // a brand-new profile has nothing to write
        if (!sameFile) Files.deleteIfExists(oldFile);
        return true;
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

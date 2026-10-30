package main;

import main.ZooQuest.AnimalNotFoundException;
import main.ZooQuest.InsufficientCluesException;
import main.ZooQuest.ZoneLockedException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Orchestration layer. Drives a complete playthrough through the public APIs of the
 * existing classes without changing any of their internal logic:
 *
 *   ZooQuest (entry) -> AppCoordinator -> GameManager -> UserProfile / Achievement
 *                                                     -> Biome -> MysteryGame -> Animal (+Jungle/Safari/Aquatic)
 *                                                                              -> MysteryClue
 *   Mystery is used here as a per-round answer tracker (one Mystery per tapped animal).
 */
public class AppCoordinator {

    private final Path saveDir;
    private final List<Mystery> roundLog = new ArrayList<>();
    private GameManager gm;

    /** Uses a fresh temp folder so repeated runs never inherit old saves. */
    public AppCoordinator() throws IOException {
        this(Files.createTempDirectory("zooquest-saves"));
    }

    public AppCoordinator(Path saveDir) {
        this.saveDir = saveDir;
    }

    public void run(String username) throws Exception {
        System.out.println("\n=========== AppCoordinator: full playthrough ===========");

        // 1. Boot: GameManager builds Biomes -> Animals (via factory) -> MysteryClues, creates UserProfile, loads save
        gm = new GameManager(saveDir);
        gm.initializeGame(username);
        System.out.println(gm.getCurrentPlayer().showProgress());
        for (Biome b : gm.getAllBiomes()) {
            Animal first = b.getAnimals().get(0);
            System.out.println("  " + b.getBiomeName() + " [" + first.getAnimalType() + ", "
                    + first.getMovementStyle() + "] animals=" + b.getAnimals().size() + " locked=" + b.isLocked());
        }

        // 2. Guard rails: exercise the three game exceptions
        try {
            gm.startBiome(2);
        } catch (ZoneLockedException e) {
            System.out.println("ZoneLockedException -> " + e.getMessage() + " (needs biome index " + e.getRequiredZoneIndex() + ")");
        }
        gm.startBiome(0);
        try {
            gm.selectHiddenAnimal("animal_eleven");
        } catch (AnimalNotFoundException e) {
            System.out.println("AnimalNotFoundException -> " + e.getMessage());
        }
        try {
            gm.handleBiomeCompletion();
        } catch (InsufficientCluesException e) {
            System.out.println("InsufficientCluesException -> missing " + e.getMissingClueCount() + " animals");
        }

        // 3. Accidental exit mid-biome: progress resets and nothing is written to disk
        playRounds(3);
        gm.exitCurrentBiome();
        roundLog.clear();
        gm.startBiome(0);
        System.out.println("After accidental exit -> found " + gm.getCurrentBiome().getBiomeMystery().getFoundCount()
                + "/10, save files on disk: " + countSaveFiles());

        // 4. Real playthrough of all three biomes
        for (int i = 0; i < GameManager.BIOME_COUNT; i++) {
            if (i > 0) gm.startBiome(i);   // biome 0 is already started
            System.out.println("\n--- " + gm.getCurrentBiome().getBiomeName() + " ---");
            playRounds(GameManager.ANIMALS_PER_BIOME);
            System.out.println("Save files on disk: " + countSaveFiles());
        }

        // 5. Summary
        long solved = roundLog.stream().filter(Mystery::isSolved).count();
        System.out.println("\n" + gm.getCurrentPlayer().showProgress());
        gm.getCurrentPlayer().getUserAchievements().forEach(a -> System.out.println("  " + a.viewBadge()));
        System.out.println("Mystery rounds solved: " + solved + "/" + roundLog.size());

        // 6. Persistence check with a brand-new GameManager
        GameManager reloaded = new GameManager(saveDir);
        reloaded.initializeGame(username);
        System.out.println("Reloaded -> " + reloaded.getCurrentPlayer().showProgress());
    }

    /** Plays the first {@code limit} animals of the current biome. */
    private void playRounds(int limit) throws AnimalNotFoundException {
        Biome biome = gm.getCurrentBiome();
        List<Animal> animals = biome.getAnimals();

        for (int i = 0; i < limit; i++) {
            Animal animal = animals.get(i);
            MysteryClue clue = gm.selectHiddenAnimal(animal.getName());   // tap hidden animal -> clue + choices
            List<String> choices = gm.getCurrentChoices();

            Mystery round = new Mystery(biome.getBiomeName() + " / round " + (i + 1), animal.getName());
            roundLog.add(round);

            if (i == 0) {   // one deliberately wrong guess per biome
                String wrong = choices.stream().filter(c -> !c.equals(animal.getName())).findFirst().orElseThrow();
                boolean tracker = round.checkAnswer(wrong);
                boolean manager = gm.processGuess(wrong).correct();
                System.out.println("Wrong guess '" + wrong + "' -> tracker=" + tracker + ", manager=" + manager);
            }

            boolean tracker = round.checkAnswer(animal.getName());
            GameManager.GuessResult r = gm.processGuess(animal.getName());
            System.out.println("Found " + r.foundCount() + "/" + r.totalCount() + " " + r.revealedAnimal().getName()
                    + " [" + r.revealedAnimal().getAnimalType() + "] clue='" + clue.getHintText()
                    + "' revealed=" + clue.isRevealed() + " tracker=" + tracker);

            if (r.biomeCompleted()) {
                System.out.println("BIOME COMPLETE -> " + r.achievementEarned().viewBadge()
                        + (gm.hasNextBiome() ? " | next biome unlocked" : " | final biome done"));
            }
        }
    }

    private long countSaveFiles() throws IOException {
        if (!Files.exists(saveDir)) return 0;
        try (Stream<Path> files = Files.list(saveDir)) {
            return files.filter(p -> p.toString().endsWith(".save")).count();
        }
    }
}

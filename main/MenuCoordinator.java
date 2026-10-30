package main;

import main.ZooQuest.AnimalNotFoundException;
import main.ZooQuest.ZoneLockedException;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Set;

/**
 * Interactive console front-end that connects every class of the project into the
 * application workflow:
 *
 * <pre>
 * Main menu ──┬─ 1. Start Game ── Biome list ── Animals section ── Mystery game ── Achievement ── next biome
 *             ├─ 2. Achievements
 *             └─ 3. View Profile (view / edit name, biomes unlocked)
 * </pre>
 *
 * Class responsibilities during the flow:
 * <ul>
 *   <li>{@link ZooQuest}      - entry point; launches this class</li>
 *   <li>{@link GameManager}   - the only gateway to game state (biomes, guesses, saving)</li>
 *   <li>{@link UserProfile}, {@link Achievement} - profile + achievements screens</li>
 *   <li>{@link Biome}, {@link MysteryGame}, {@link MysteryClue} - biome list, hidden-animal rounds, clues</li>
 *   <li>{@link Animal} (+ {@link Jungle}/{@link Safari}/{@link Aquatic}) - animals section and scenery</li>
 *   <li>{@link Mystery}       - per-round answer tracker (same role as in AppCoordinator)</li>
 *   <li>{@link AppCoordinator} - scripted auto-playthrough, still reachable via {@code ZooQuest --demo}</li>
 * </ul>
 *
 * No existing class is modified by this class; it only uses their public APIs.
 */
public class MenuCoordinator {

    private static final String DEFAULT_NAME = "Player";
    private static final int MAX_NAME_LENGTH = 20;

    private final Path saveDir;
    private final Scanner in;
    private final PrintStream out;
    private final GameManager gm;

    /** Answer trackers for the biome currently being played (one {@link Mystery} per tapped animal). */
    private final List<Mystery> roundLog = new ArrayList<>();
    private int wrongGuesses;

    public MenuCoordinator(Path saveDir) {
        this(saveDir, System.in, System.out);
    }

    /** Streams are injectable so the flow can be scripted/tested. */
    public MenuCoordinator(Path saveDir, InputStream input, PrintStream output) {
        this.saveDir = saveDir;
        this.in = new Scanner(input);
        this.out = output;
        this.gm = new GameManager(saveDir);
    }

    // ================================================================ entry

    public void run() {
        try {
            out.println("==========================================");
            out.println("            Z O O   Q U E S T");
            out.println("==========================================");
            String name = ask("Enter your name (blank = " + DEFAULT_NAME + "): ");
            gm.initializeGame(name.isBlank() ? DEFAULT_NAME : clipName(name));
            out.println("Welcome, " + gm.getCurrentPlayer().getUsername() + "!");
            mainMenu();
        } catch (NoSuchElementException e) {       // input stream closed (Ctrl+D / piped input ended)
            out.println("\nInput closed. Leaving the game.");
            gm.exitCurrentBiome();                  // discards any unfinished biome, saves nothing
        }
    }

    // ============================================================ main menu

    private void mainMenu() {
        while (true) {
            out.println("\n=============== MAIN MENU ===============");
            out.println("  1. Start Game");
            out.println("  2. Achievements");
            out.println("  3. View Profile");
            out.println("  0. Exit");
            switch (askInt("Choose: ", 0, 3)) {
                case 1 -> biomeSelection();
                case 2 -> achievementsScreen();
                case 3 -> profileScreen();
                default -> {
                    out.println("Goodbye, " + gm.getCurrentPlayer().getUsername() + "!");
                    return;
                }
            }
        }
    }

    // ================================================== 1. START GAME: biomes

    private void biomeSelection() {
        while (true) {
            List<Biome> biomes = gm.getAllBiomes();
            out.println("\n================= BIOMES =================");
            for (int i = 0; i < biomes.size(); i++) {
                Biome b = biomes.get(i);
                out.printf("  %d) %-12s %-28s %s%n", i + 1, b.getBiomeName(), b.getDescription(), biomeStatus(i, b));
            }
            out.println("  0) Back");

            int choice = askInt("Choose a biome: ", 0, biomes.size());
            if (choice == 0) return;

            Integer next = choice - 1;
            while (next != null) {          // lets the player flow straight into the next biome
                next = playBiome(next);
            }
        }
    }

    private String biomeStatus(int index, Biome biome) {
        if (gm.getCurrentPlayer().getUserProgress().contains(index)) return "[COMPLETED]";
        return biome.isLocked() ? "[LOCKED]" : "[UNLOCKED]";
    }

    /**
     * Runs one biome: animals section -> mystery game.
     * @return index of the next biome to play immediately, or null to go back to the biome list.
     */
    private Integer playBiome(int index) {
        try {
            gm.startBiome(index);                                   // throws if the biome is still locked
        } catch (ZoneLockedException e) {
            out.println("\n[LOCKED] " + e.getMessage());
            return null;
        }
        roundLog.clear();
        wrongGuesses = 0;

        boolean alreadyCompleted = gm.getCurrentPlayer().getUserProgress().contains(index);
        if (!animalsSection(gm.getCurrentBiome(), alreadyCompleted)) {
            gm.exitCurrentBiome();
            return null;
        }
        return gameSection(index);
    }

    // ===================================================== animals section

    /**
     * Lets the player inspect every animal and read its clue.
     * @return true when the player chooses to proceed to the mystery game.
     */
    private boolean animalsSection(Biome biome, boolean alreadyCompleted) {
        List<Animal> animals = biome.getAnimals();
        Set<String> explored = new HashSet<>();

        while (true) {
            boolean allExplored = explored.size() == animals.size();
            boolean canPlay = allExplored && !alreadyCompleted;

            out.println("\n========== " + biome.getBiomeName() + " : ANIMALS ==========");
            out.println(biome.getDescription());
            for (int i = 0; i < animals.size(); i++) {
                String name = animals.get(i).getName();
                out.printf("  %2d) [%s] %s%n", i + 1, explored.contains(name) ? "x" : " ", name);
            }
            if (alreadyCompleted) {
                out.println("Biome already completed - review mode (badge already earned, game replay disabled).");
            } else if (allExplored) {
                out.println("You explored every animal! The mystery game is now available.");
            } else {
                out.printf("Explored %d/%d - explore every animal to unlock the mystery game.%n",
                        explored.size(), animals.size());
            }
            out.println("Enter an animal number" + (canPlay ? ", G = start the mystery game" : "")
                    + ", B = back to biomes");

            String input = ask("> ").toLowerCase();
            if (input.equals("b")) return false;
            if (input.equals("g")) {
                if (canPlay) return true;
                out.println(alreadyCompleted ? "This biome is already completed."
                                             : "Explore all animals first.");
                continue;
            }
            try {
                int n = Integer.parseInt(input);
                if (n >= 1 && n <= animals.size()) {
                    Animal animal = animals.get(n - 1);
                    showAnimal(animal);
                    explored.add(animal.getName());
                    continue;
                }
            } catch (NumberFormatException ignored) {
                // falls through to the invalid-choice message
            }
            out.println("Invalid choice.");
        }
    }

    private void showAnimal(Animal a) {
        out.println("\n--- " + a.getDescription());
        out.println("Type     : " + a.getAnimalType());
        out.println("Habitat  : " + a.getHabitat());
        out.println("Diet     : " + a.getDiet());
        out.println("Status   : " + a.getStatus());
        out.println("Sprite   : " + a.getSpriteTemplate());
        out.println("Behaviour: " + a.displayBehaviour());
        out.println("CLUE     : " + a.getClue().getHintText());
    }

    // ======================================================== mystery game

    /** @return index of the next biome to play immediately, or null to return to the biome list. */
    private Integer gameSection(int biomeIndex) {
        Biome biome = gm.getCurrentBiome();
        MysteryGame mystery = biome.getBiomeMystery();
        List<Animal> animals = biome.getAnimals();

        out.println("\n======= MYSTERY GAME: " + mystery.getMysteryTitle() + " =======");
        out.println("Tap a hidden animal, read its clue, then choose the right animal.");

        while (true) {
            renderScene(biome, mystery, animals);

            int slot = askInt("Tap a hidden animal (0 = leave biome): ", 0, animals.size());
            if (slot == 0) {
                if (confirm("Leave? Progress in this biome will be lost (y/n): ")) {
                    gm.exitCurrentBiome();                          // discards progress, saves nothing
                    out.println("You left the biome. Unfinished progress was discarded.");
                    return null;
                }
                continue;
            }

            Animal target = animals.get(slot - 1);
            if (mystery.isAnimalFound(target.getName())) {
                out.println("That animal was already found. Tap a hidden one!");
                continue;
            }
            if (tapAnimal(slot, target)) {
                return completionMenu(biomeIndex);
            }
        }
    }

    /** Draws the game scene: background scenery, hidden animals and the interface bar. */
    private void renderScene(Biome biome, MysteryGame mystery, List<Animal> animals) {
        out.println();
        for (String line : scenery(animals.get(0).getAnimalType())) out.println(line);
        out.println("  Biome: " + biome.getBiomeName() + "   Found: " + mystery.getFoundCount()
                + "/" + mystery.getTotalCount());
        out.println("  ------------------------------------------");
        for (int i = 0; i < animals.size(); i++) {
            Animal a = animals.get(i);
            if (mystery.isAnimalFound(a.getName())) {
                out.printf("  %2d) [FOUND] %s  (%s)%n", i + 1, a.getName(), a.getAnimalType());
            } else {
                out.printf("  %2d) [  ?  ] hidden animal%n", i + 1);
            }
        }
    }

    private static String[] scenery(String animalType) {
        return switch (animalType) {
            case "Jungle" -> new String[] {
                    "   @@@@   @@@@   @@@@     ~ THE JUNGLE CANOPY ~",
                    "  @@@@@@ @@@@@@ @@@@@@",
                    "    ||     ||     ||      vines, ferns, rustling leaves" };
            case "Safari" -> new String[] {
                    "            \\ | /          ~ THE SAFARI PLAINS ~",
                    "          -- (   ) --",
                    "   ,,,,,,,,,,,,,,,,,,,,,,,   golden grass, acacia trees" };
            case "Aquatic" -> new String[] {
                    "   ~~~~  ~~~~  ~~~~  ~~~~     ~ THE OPEN WATER ~",
                    "    ~~~~  ~~~~  ~~~~  ~~~~",
                    "   ~~~~  ~~~~  ~~~~  ~~~~     bubbles, coral, currents" };
            default -> new String[] { "   ~ SCENE ~" };
        };
    }

    /**
     * One round: tap -> clue + choices -> guess (repeat until right or the player backs out).
     * @return true only if this round completed the whole biome.
     */
    private boolean tapAnimal(int slot, Animal target) {
        MysteryClue clue;
        try {
            clue = gm.selectHiddenAnimal(target.getName());
        } catch (AnimalNotFoundException e) {
            out.println(e.getMessage());
            return false;
        }

        Mystery round = new Mystery(gm.getCurrentBiome().getBiomeName() + " / hidden animal " + slot,
                target.getName());
        roundLog.add(round);

        out.println("\nYou tapped hidden animal #" + slot + "!");
        out.println("CLUE: " + clue.getHintText());

        while (true) {
            List<String> choices = gm.getCurrentChoices();
            out.println("Which animal is it?");
            for (int i = 0; i < choices.size(); i++) {
                out.println("  " + (i + 1) + ") " + choices.get(i));
            }
            out.println("  0) Look at another hidden animal");

            int pick = askInt("Your answer: ", 0, choices.size());
            if (pick == 0) return false;

            String guess = choices.get(pick - 1);
            round.checkAnswer(guess);                                   // per-round tracker
            GameManager.GuessResult result = gm.processGuess(guess);    // real game state

            if (!result.correct()) {
                wrongGuesses++;
                out.println("Not quite - try again!");
                continue;
            }

            Animal found = result.revealedAnimal();
            out.println("\nCORRECT! It was " + found.getName() + "!");
            out.println(found.getDescription());
            out.println("Clue revealed: " + clue.isRevealed() + "   Found "
                    + result.foundCount() + "/" + result.totalCount());

            if (result.biomeCompleted()) {
                long solved = roundLog.stream().filter(Mystery::isSolved).count();
                out.println("\n*********** BIOME COMPLETE! ***********");
                out.println("Rounds solved: " + solved + "/" + roundLog.size()
                        + "   Wrong guesses: " + wrongGuesses);
                out.println("Achievement unlocked: " + result.achievementEarned().viewBadge());
                return true;
            }
            return false;
        }
    }

    /** Shown after the final animal: continue to the (already unlocked) next biome or go back. */
    private Integer completionMenu(int biomeIndex) {
        boolean hasNext = gm.hasNextBiome();
        String nextName = hasNext ? gm.getAllBiomes().get(biomeIndex + 1).getBiomeName() : null;
        gm.exitCurrentBiome();      // progress is already saved by GameManager; just leave the biome

        if (!hasNext) {
            out.println("\nYou have completed every biome. Congratulations, Zoo Ranger!");
            ask("Press Enter to return... ");
            return null;
        }
        out.println("\n  1) Continue to the next biome (" + nextName + ")");
        out.println("  2) Back to biome list");
        return askInt("Choose: ", 1, 2) == 1 ? biomeIndex + 1 : null;
    }

    // ===================================================== 2. ACHIEVEMENTS

    private void achievementsScreen() {
        List<Achievement> badges = gm.getCurrentPlayer().getUserAchievements();
        out.println("\n=============== ACHIEVEMENTS ===============");
        if (badges.isEmpty()) {
            out.println("No achievements yet. Complete a biome to earn your first badge!");
        } else {
            for (int i = 0; i < badges.size(); i++) {
                out.println("  " + (i + 1) + ". " + badges.get(i).viewBadge());
            }
        }
        ask("\nPress Enter to return... ");
    }

    // ======================================================== 3. PROFILE

    private void profileScreen() {
        while (true) {
            UserProfile player = gm.getCurrentPlayer();
            List<Biome> biomes = gm.getAllBiomes();
            long unlocked = biomes.stream().filter(b -> !b.isLocked()).count();

            out.println("\n================= PROFILE =================");
            out.println("Name            : " + player.getUsername());
            out.println("Biomes unlocked : " + unlocked + "/" + biomes.size());
            out.println(player.showProgress());
            for (int i = 0; i < biomes.size(); i++) {
                out.println("  - " + biomes.get(i).getBiomeName() + " " + biomeStatus(i, biomes.get(i)));
            }
            out.println("\n  1) Edit name");
            out.println("  0) Back");
            if (askInt("Choose: ", 0, 1) == 0) return;

            String newName = ask("New name: ");
            if (newName.isBlank()) {
                out.println("Name cannot be empty.");
            } else {
                renameProfile(clipName(newName));
            }
        }
    }

    /**
     * UserProfile has no setter for the username, and it must stay untouched. Renaming is therefore
     * done through GameManager: start a profile under the new name and carry the progress over, then
     * persist it under the new save file and remove the old one.
     */
    private void renameProfile(String newName) {
        UserProfile old = gm.getCurrentPlayer();
        if (newName.equals(old.getUsername())) {
            out.println("That is already your name.");
            return;
        }

        Path oldFile = saveFileFor(old.getUsername());
        Path newFile = saveFileFor(newName);
        boolean sameFile = oldFile.equals(newFile);           // e.g. "a b" -> "a_b" share one save file
        if (!sameFile && Files.exists(newFile)) {
            out.println("Another player already uses that name. Choose a different one.");
            return;
        }

        List<Integer> completed = new ArrayList<>(old.getUserProgress());
        List<Achievement> badges = new ArrayList<>(old.getUserAchievements());

        gm.initializeGame(newName);                           // fresh profile + biomes (+ save if same file)

        if (!sameFile) {
            UserProfile renamed = gm.getCurrentPlayer();
            List<Biome> biomes = gm.getAllBiomes();
            for (int index : completed) {
                biomes.get(index).unlockBiome();
                renamed.markBiomeCompleted(index);
                if (index + 1 < biomes.size()) biomes.get(index + 1).unlockBiome();
            }
            badges.forEach(renamed::addAchievement);

            try {
                if (!completed.isEmpty()) gm.saveProgress();  // nothing to persist for a brand-new profile
                Files.deleteIfExists(oldFile);
            } catch (IOException e) {
                out.println("Name changed, but the save file could not be updated: " + e.getMessage());
            }
        }
        out.println("Name changed to '" + newName + "'. Progress kept.");
    }

    /** Mirrors GameManager's private file-name rule so the old save can be removed on rename. */
    private Path saveFileFor(String username) {
        return saveDir.resolve(username.replaceAll("[^a-zA-Z0-9_-]", "_") + ".save");
    }

    // ============================================================ input

    private String ask(String prompt) {
        out.print(prompt);
        out.flush();
        return in.nextLine().trim();
    }

    private int askInt(String prompt, int min, int max) {
        while (true) {
            try {
                int value = Integer.parseInt(ask(prompt));
                if (value >= min && value <= max) return value;
            } catch (NumberFormatException ignored) {
                // falls through to the message below
            }
            out.println("Please enter a number between " + min + " and " + max + ".");
        }
    }

    private boolean confirm(String prompt) {
        return ask(prompt).toLowerCase().startsWith("y");
    }

    private static String clipName(String name) {
        String trimmed = name.trim();
        return trimmed.length() > MAX_NAME_LENGTH ? trimmed.substring(0, MAX_NAME_LENGTH) : trimmed;
    }
}

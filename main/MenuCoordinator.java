package main;

import main.ZooQuest.AnimalNotFoundException;
import main.ZooQuest.ZoneLockedException;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Path;
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
 * Main menu ──┬─ 1. Start Game ── Biome list ── Animals section ── Mystery game ── 2 achievements ── next biome
 *             ├─ 2. Achievements
 *             └─ 3. View Profile (view / edit name, biomes unlocked)
 * </pre>
 *
 * This class is only a presentation layer: all rules (locking, clue mode, the two badges per biome, saving,
 * renaming) live in {@link GameManager}; the screen only asks it questions and prints the answers.
 *
 * Class responsibilities during the flow:
 * <ul>
 *   <li>{@link ZooQuest}      - entry point; launches this class</li>
 *   <li>{@link GameManager}   - the only gateway to game state (biomes, guesses, badges, saving)</li>
 *   <li>{@link UserProfile}, {@link Achievement} - profile + achievements screens</li>
 *   <li>{@link Biome} (+ {@link JungleBiome}/{@link SafariBiome}/{@link AquaticBiome}) - biome list and scenery</li>
 *   <li>{@link MysteryGame}, {@link MysteryClue}, {@link Mystery} - hidden-animal rounds, clues, per-round answers</li>
 *   <li>{@link Animal} (+ {@link Jungle}/{@link Safari}/{@link Aquatic}) - animals section and reveal messages</li>
 *   <li>{@link AppCoordinator} - scripted auto-playthrough, still reachable via {@code ZooQuest --demo}</li>
 * </ul>
 *
 * Achievements - two per biome:
 * <ol>
 *   <li><b>Clear badge</b>   - completing the mystery game.</li>
 *   <li><b>No-Clue badge</b> - completing it without any clue shown during the game. The player picks a clue
 *       mode when the game starts: with clues (shown on tap) or a no-clue challenge (clues hidden; revealing
 *       one forfeits the badge). Clues viewed in the animals section do not count.</li>
 * </ol>
 * A biome can be replayed while it still has an unearned badge.
 */
public class MenuCoordinator {

    private static final String DEFAULT_NAME = "Player";

    private final Scanner in;
    private final PrintStream out;
    private final GameManager gm;

    private boolean showClues;   // clue mode picked for the game on screen (UI choice, enforced by GameManager)

    public MenuCoordinator(Path saveDir) {
        this(saveDir, System.in, System.out);
    }

    /** Streams are injectable so the flow can be scripted/tested. */
    public MenuCoordinator(Path saveDir, InputStream input, PrintStream output) {
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
            startSession();
            mainMenu();
        } catch (NoSuchElementException e) {       // input stream closed (Ctrl+D / piped input ended)
            out.println("\nInput closed. Leaving the game.");
            gm.exitCurrentBiome();                  // discards any unfinished biome, saves nothing
        }
    }

    private void startSession() {
        while (true) {
            String name = ask("Enter your name (blank = " + DEFAULT_NAME + "): ");
            try {
                gm.initializeGame(name.isBlank() ? DEFAULT_NAME : name);   // GameManager restores any save
                break;
            } catch (IllegalArgumentException e) {
                out.println(e.getMessage());
            }
        }
        out.println("Welcome, " + gm.getCurrentPlayer().getUsername() + "!");
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
                out.printf("  %d) %-12s %-28s %s%n", i + 1, b.getBiomeName(), b.getDescription(), biomeStatus(i));
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

    private String biomeStatus(int index) {
        return switch (gm.getBiomeStatus(index)) {
            case LOCKED -> "[LOCKED]";
            case UNLOCKED -> "[UNLOCKED]";
            default -> "[COMPLETED " + gm.getBadgeCount(index) + "/" + GameManager.BADGES_PER_BIOME + " badges]";
        };
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

        boolean reviewOnly = gm.getBiomeStatus(index) == GameManager.BiomeStatus.PERFECT;   // nothing left to earn
        if (!animalsSection(gm.getCurrentBiome(), index, reviewOnly)) {
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
    private boolean animalsSection(Biome biome, int biomeIndex, boolean reviewOnly) {
        List<Animal> animals = biome.getAnimals();
        Set<String> explored = new HashSet<>();

        while (true) {
            boolean allExplored = explored.size() == animals.size();
            boolean canPlay = allExplored && !reviewOnly;

            out.println("\n========== " + biome.getBiomeName() + " : ANIMALS ==========");
            out.println(biome.getDescription());
            for (int i = 0; i < animals.size(); i++) {
                String name = animals.get(i).getName();
                out.printf("  %2d) [%s] %s%n", i + 1, explored.contains(name) ? "x" : " ", name);
            }
            if (gm.hasClearBadge(biomeIndex) && !reviewOnly) {
                out.println("Replay: finish the game without any clue to earn the No-Clue badge.");
            }
            if (reviewOnly) {
                out.println("Both badges already earned - review mode (game replay disabled).");
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
                out.println(reviewOnly ? "Both badges are already earned."
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
        List<Animal> animals = biome.getAnimals();

        out.println("\n======= MYSTERY GAME: " + gm.getMysteryTitle() + " =======");
        out.println("Tap a hidden animal, read its clue, then choose the right animal.");
        chooseClueMode(biomeIndex);

        while (true) {
            renderScene(biome, animals);

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
            if (gm.isAnimalFound(target.getName())) {
                out.println("That animal was already found. Tap a hidden one!");
                continue;
            }
            if (tapAnimal(slot, target)) {
                return completionMenu(biomeIndex);
            }
        }
    }

    /** Lets the player pick between normal play and the no-clue challenge (only while that badge is unearned). */
    private void chooseClueMode(int biomeIndex) {
        showClues = true;
        if (gm.hasNoClueBadge(biomeIndex)) return;      // badge already earned: nothing to gain from hiding clues

        out.println("\nClue mode:");
        out.println("  1) With clues        - the clue is shown when you tap an animal");
        out.println("  2) No-clue challenge - clues stay hidden; finish without revealing one to earn the No-Clue badge");
        showClues = askInt("Choose clue mode: ", 1, 2) == 1;
        gm.setClueMode(showClues);                      // GameManager enforces the rule from here on
    }

    /** Draws the game scene: background scenery (from the biome type), hidden animals and the interface bar. */
    private void renderScene(Biome biome, List<Animal> animals) {
        out.println();
        for (String line : biome.getScenery()) out.println(line);
        out.println("  Biome: " + biome.getBiomeName() + "   Found: " + gm.getFoundCount()
                + "/" + gm.getTotalCount());
        out.println("  ------------------------------------------");
        for (int i = 0; i < animals.size(); i++) {
            Animal a = animals.get(i);
            if (gm.isAnimalFound(a.getName())) {
                out.printf("  %2d) [FOUND] %s  (%s)%n", i + 1, a.getName(), a.getAnimalType());
            } else {
                out.printf("  %2d) [  ?  ] hidden animal%n", i + 1);
            }
        }
    }

    /**
     * One round: tap -> (clue) + choices -> guess (repeat until right or the player backs out).
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

        out.println("\nYou tapped hidden animal #" + slot + "!");
        if (gm.isClueShown()) {
            out.println("CLUE: " + clue.getHintText());
        } else {
            out.println("(Clue hidden - no-clue challenge.)");
        }

        while (true) {
            List<String> choices = gm.getCurrentChoices();
            boolean clueHidden = !gm.isClueShown();
            out.println("Which animal is it?");
            for (int i = 0; i < choices.size(); i++) {
                out.println("  " + (i + 1) + ") " + choices.get(i));
            }
            if (clueHidden) out.println("  H) Reveal the clue (forfeits the No-Clue badge)");
            out.println("  0) Look at another hidden animal");

            String raw = ask("Your answer: ").toLowerCase();
            if (clueHidden && raw.equals("h")) {
                out.println("CLUE: " + gm.revealClue());
                out.println("(No-Clue badge forfeited for this run.)");
                continue;
            }
            int pick = parseChoice(raw, 0, choices.size());
            if (pick < 0) {
                out.println("Please enter a number between 0 and " + choices.size() + (clueHidden ? ", or H." : "."));
                continue;
            }
            if (pick == 0) return false;

            GameManager.GuessResult result = gm.processGuess(choices.get(pick - 1));
            if (!result.correct()) {
                out.println("Not quite - try again!");
                continue;
            }

            Animal found = result.revealedAnimal();
            out.println("\nCORRECT! It was " + found.getName() + "!");
            out.println(found.getRevealMessage());          // each animal type reacts in its own way
            out.println(found.getDescription());
            out.println("Found " + result.foundCount() + "/" + result.totalCount());

            if (result.biomeCompleted()) {
                out.println("\n*********** BIOME COMPLETE! ***********");
                out.println("Rounds solved: " + gm.getSolvedRoundCount() + "/" + gm.getRoundCount()
                        + "   Wrong guesses: " + gm.getWrongGuessCount()
                        + "   Clues used: " + (gm.hasUsedClues() ? "yes" : "none"));
                reportBadges(result);
                return true;
            }
            return false;
        }
    }

    /** Prints what GameManager awarded for this completion. */
    private void reportBadges(GameManager.GuessResult result) {
        String clearName = GameManager.clearBadgeName(gm.getCurrentBiome().getBiomeName());
        boolean clearIsNew = false;
        for (Achievement badge : result.newAchievements()) {
            boolean isClear = badge.getBadgeName().equals(clearName);
            out.println((isClear ? "Achievement unlocked: " : "BONUS achievement unlocked: ") + badge.viewBadge());
            clearIsNew |= isClear;
        }
        if (!clearIsNew) {
            out.println("Clear badge was already earned - nothing new there.");
        }
        if (!gm.hasNoClueBadge(gm.getCurrentBiomeIndex())) {
            out.println("A clue was used, so no No-Clue badge this time. Replay the biome to earn it.");
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
        UserProfile player = gm.getCurrentPlayer();
        List<Biome> biomes = gm.getAllBiomes();
        int earned = player.getUserAchievements().size();

        out.println("\n=============== ACHIEVEMENTS ===============");
        out.println("Earned " + earned + "/" + biomes.size() * GameManager.BADGES_PER_BIOME
                + "  (two badges per biome)");
        if (earned == 0) {
            out.println("No achievements yet. Complete a biome to earn your first badge!");
        }
        for (Biome b : biomes) {
            out.println("\n  " + b.getBiomeName());
            printBadgeSlot(GameManager.clearBadgeName(b.getBiomeName()),
                    "Solve every mystery in " + b.getBiomeName());
            printBadgeSlot(GameManager.noClueBadgeName(b.getBiomeName()),
                    "Solve every mystery in " + b.getBiomeName() + " without using a clue");
        }
        ask("\nPress Enter to return... ");
    }

    private void printBadgeSlot(String badgeName, String requirement) {
        gm.getCurrentPlayer().findAchievement(badgeName).ifPresentOrElse(
                badge -> out.println("    [x] " + badge.viewBadge()),
                () -> out.println("    [ ] LOCKED - " + requirement));
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
            out.println("Badges earned   : " + player.getUserAchievements().size() + "/"
                    + biomes.size() * GameManager.BADGES_PER_BIOME);
            out.println(player.showProgress());
            for (int i = 0; i < biomes.size(); i++) {
                out.println("  - " + biomes.get(i).getBiomeName() + " " + biomeStatus(i));
            }
            out.println("\n  1) Edit name");
            out.println("  0) Back");
            if (askInt("Choose: ", 0, 1) == 0) return;

            try {
                if (gm.renamePlayer(ask("New name: "))) {
                    out.println("Name changed to '" + gm.getCurrentPlayer().getUsername() + "'. Progress kept.");
                } else {
                    out.println("That is already your name.");
                }
            } catch (IllegalArgumentException e) {            // blank, too long or already taken
                out.println(e.getMessage());
            } catch (IOException e) {
                out.println("Name changed, but the save file could not be updated: " + e.getMessage());
            }
        }
    }

    // ============================================================ input

    private String ask(String prompt) {
        out.print(prompt);
        out.flush();
        return in.nextLine().trim();
    }

    private int askInt(String prompt, int min, int max) {
        while (true) {
            int value = parseChoice(ask(prompt), min, max);
            if (value >= 0) return value;
            out.println("Please enter a number between " + min + " and " + max + ".");
        }
    }

    /** @return the number if it is within [min, max], otherwise -1. */
    private static int parseChoice(String raw, int min, int max) {
        try {
            int value = Integer.parseInt(raw);
            return value >= min && value <= max ? value : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private boolean confirm(String prompt) {
        return ask(prompt).toLowerCase().startsWith("y");
    }
}

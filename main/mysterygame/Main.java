package mysterygame;

import java.nio.file.Path;

/** Console demo that exercises GameManager the way the UI will. */
public class Main {
    public static void main(String[] args) throws Exception {
        Path saveDir = Path.of("saves");
        GameManager gm = new GameManager(saveDir);
        gm.initializeGame("demo_player");

        // Locked biome check
        try { gm.startBiome(1); } catch (ZoneLockedException e) { System.out.println("Blocked: " + e.getMessage()); }

        // Play biome 0 halfway, then quit -> progress resets, nothing saved
        gm.startBiome(0);
        play(gm, 5);
        System.out.println("Exiting mid-biome...");
        gm.exitCurrentBiome();

        // Replay biome 0 fully
        gm.startBiome(0);
        play(gm, 10);

        // New session: progress should be restored from disk
        GameManager gm2 = new GameManager(saveDir);
        gm2.initializeGame("demo_player");
        System.out.println("Reloaded -> " + gm2.getCurrentPlayer().showProgress());
        gm2.getAllBiomes().forEach(b -> System.out.println(b.getBiomeName() + " locked=" + b.isLocked()));
    }

    private static void play(GameManager gm, int rounds) throws Exception {
        var animals = gm.getCurrentBiome().getAnimals();
        for (int i = 0; i < rounds; i++) {
            String name = animals.get(i).getName();
            var clue = gm.selectHiddenAnimal(name);
            String wrong = gm.getCurrentChoices().stream().filter(c -> !c.equals(name)).findFirst().get();
            if (i == 0) System.out.println("Clue: " + clue.getHintText() + " | choices " + gm.getCurrentChoices()
                    + " | wrong guess correct? " + gm.processGuess(wrong).correct());
            var r = gm.processGuess(name);
            System.out.println("Found " + r.foundCount() + "/" + r.totalCount() + " " + r.revealedAnimal().getName()
                    + (r.biomeCompleted() ? " -> BIOME COMPLETE, badge: " + r.achievementEarned().viewBadge() : ""));
        }
    }

    // ------------------------------------------------------------------
    // Game exceptions (used only by the game mode)
    // ------------------------------------------------------------------

    /** Thrown when a player tries to enter a biome that has not been unlocked yet. */
    public static class ZoneLockedException extends Exception {
        private final String targetZoneName;
        private final int requiredZoneIndex;

        public ZoneLockedException(String targetZoneName, int requiredZoneIndex) {
            super("'" + targetZoneName + "' is locked. Complete biome #" + (requiredZoneIndex + 1) + " first.");
            this.targetZoneName = targetZoneName;
            this.requiredZoneIndex = requiredZoneIndex;
        }

        public String getTargetZoneName() { return targetZoneName; }
        public int getRequiredZoneIndex() { return requiredZoneIndex; }
    }

    /** Thrown when a biome is completed before all animals have been discovered. */
    public static class InsufficientCluesException extends Exception {
        private final int currentClues;
        private final int requiredClues;

        public InsufficientCluesException(int currentClues, int requiredClues) {
            super("Found " + currentClues + " of " + requiredClues + " animals.");
            this.currentClues = currentClues;
            this.requiredClues = requiredClues;
        }

        public int getMissingClueCount() { return requiredClues - currentClues; }
    }

    /** Thrown when an animal cannot be found in the active biome. */
    public static class AnimalNotFoundException extends Exception {
        private final String searchedName;
        private final String biomeName;

        public AnimalNotFoundException(String searchedName, String biomeName) {
            super("Animal '" + searchedName + "' does not exist in " + biomeName);
            this.searchedName = searchedName;
            this.biomeName = biomeName;
        }

        public String getSearchedName() { return searchedName; }
        public String getBiomeName() { return biomeName; }
    }
}

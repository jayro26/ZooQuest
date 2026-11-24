package main;

import java.util.ArrayList;
import java.util.List;

/** Small hand-over between screens (difficulty choice, result of the finished biome). */
final class Flow {
    static boolean withClues = true;          // chosen on the "Choose Difficulty" popup

    // filled by GamePanel when the last animal is found; read by CompletePanel / UnlockedPanel
    static Biome biome;                       // the biome that was just completed
    static int completedIndex = -1;
    static boolean hasNext;
    static int nextIndex = -1;
    static boolean newlyUnlocked;             // the next biome was locked before this run
    static int wrongGuesses;
    static boolean cluesUsed;
    static List<Achievement> newBadges = new ArrayList<>();

    private Flow() {}

    static String completedLabel() {
        return biome == null ? "" : biome.getBiomeName() + " (" + biome.getBiomeType() + ")";
    }
}

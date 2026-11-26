package main;

import java.util.ArrayList;
import java.util.List;

// Small hand-over between screens (difficulty choice, result of the finished biome).
public final class Flow {
    public static boolean withClues = true;          // chosen on the "Choose Difficulty" popup

    // filled by GamePanel when the last animal is found; read by CompletePanel / UnlockedPanel
    public static Biome biome;                       // the biome that was just completed
    public static int completedIndex = -1;
    public static boolean hasNext;
    public static int nextIndex = -1;
    public static boolean newlyUnlocked;             // the next biome was locked before this run
    public static int wrongGuesses;
    public static boolean cluesUsed;
    public static List<Achievement> newBadges = new ArrayList<>();

    private Flow() {}

    public static String completedLabel() {
        return biome == null ? "" : biome.getBiomeName() + " (" + biome.getBiomeType() + ")";
    }
}

package main.exceptions;

// Thrown when a biome is completed before all animals have been discovered.
public class InsufficientCluesException extends Exception {
    private final int currentClues;
    private final int requiredClues;

    public InsufficientCluesException(int currentClues, int requiredClues) {
        super("Found " + currentClues + " of " + requiredClues + " animals.");
        this.currentClues = currentClues;
        this.requiredClues = requiredClues;
    }

    public int getMissingClueCount() { return requiredClues - currentClues; }
}

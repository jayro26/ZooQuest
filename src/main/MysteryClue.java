package main;

/** A single clue tied to one animal. */
public class MysteryClue {
    private final String clueId;
    private final String hintText;
    private final String associatedAnimalName;
    private boolean revealed;

    public MysteryClue(String clueId, String hintText, String associatedAnimalName) {
        this.clueId = clueId;
        this.hintText = hintText;
        this.associatedAnimalName = associatedAnimalName;
        this.revealed = false;
    }

    void reveal() {
        revealed = true;
    }

    void hide() {
        revealed = false;
    }

    public boolean isRevealed() {
        return revealed;
    }

    public String getHintText() {
        return hintText;
    }

    public String getClueId() {
        return clueId;
    }

    public String getAssociatedAnimalName() {
        return associatedAnimalName;
    }
}

package main;

public class Mystery {
    private String mysteryName;
    private String correctAnswer;
    private boolean solved;

    public Mystery(String mysteryName, String correctAnswer) {
        this.mysteryName = mysteryName;
        this.correctAnswer = correctAnswer;
        this.solved = false;
    }

    public boolean checkAnswer(String answer) {
        if (correctAnswer.equalsIgnoreCase(answer)) {
            solved = true;
            return true;
        }
        return false;
    }

    public boolean isSolved() {
        return solved;
    }

    public String getMysteryName() {
        return mysteryName;
    }
}

package main;

import java.time.LocalDate;

public class Achievement {
    private String badgeName;
    private String description;
    private String imagePath;
    private LocalDate dateEarned;

    public Achievement(String bageName, String description, String imagePath, LocalDate dateEarned) {
        this.badgeName = bageName;
        this.description = description;
        this.imagePath = imagePath;
        this.dateEarned = dateEarned;
    }

    public String getBadgeName() {
        return badgeName;
    }

    public LocalDate getDateEarned() {
        return dateEarned;
    }

    public String viewBadge() {
        return badgeName + " - " + description + " (Earned: " + dateEarned + ")";
    }
}

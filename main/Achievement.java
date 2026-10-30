package main;

import java.time.LocalDate;

public class Achievement {
    private final String badgeName; // NEW CODE: final
    private final String description; // NEW CODE: final
    private final String imagePath; // NEW CODE: final
    private final LocalDate dateEarned; // NEW CODE: final

    public Achievement(String bageName, String description, String imagePath, LocalDate dateEarned) {
        this.badgeName = bageName;
        this.description = description;
        this.imagePath = imagePath;
        this.dateEarned = dateEarned;
    }

    public String getBadgeName() {
        return badgeName;
    }

    // NEW CODE: read-only accessors for the remaining fields
    public String getDescription() {
        return description;
    }

    public String getImagePath() {
        return imagePath;
    }

    public LocalDate getDateEarned() {
        return dateEarned;
    }

    public String viewBadge() {
        return badgeName + " - " + description + " (Earned: " + dateEarned + ")";
    }
}

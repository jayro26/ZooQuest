package main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List; // NEW CODE
import java.util.Optional;    // NEW CODE

public class UserProfile {
    public static final int MAX_USERNAME_LENGTH = 20; // NEW CODE
    private String username;
    private List<Integer> completedBiomes;
    private List<Achievement> achievements;

    public UserProfile(String username) {
        this.username = validateUsername(username); // NEW CODE (was: this.username = username;)
        this.completedBiomes = new ArrayList<>();
        this.achievements = new ArrayList<>();
    }

    public String getUsername() {
        return username;
    }

    // NEW CODE: validated rename (the profile previously had no way to change its name)
    public void setUsername(String username) {
        this.username = validateUsername(username);
    }

    // NEW CODE: trims the name and rejects blank / too long names
    public static String validateUsername(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty.");
        }
        if (trimmed.length() > MAX_USERNAME_LENGTH) {
            throw new IllegalArgumentException("Name can be at most " + MAX_USERNAME_LENGTH + " characters.");
        }
        return trimmed;
    }

    public List<Integer> getUserProgress() {
        return Collections.unmodifiableList(completedBiomes); // NEW CODE (was: return completedBiomes;)
    }

    public List<Achievement> getUserAchievements() {
        return Collections.unmodifiableList(achievements); // NEW CODE (was: return achievements;)
    }

    public void addAchievement(Achievement achievement) {
        if (achievement != null) {
            achievements.add(achievement);
        }
    }

    // NEW CODE
    public boolean hasAchievement(String badgeName) {
        return findAchievement(badgeName).isPresent();
    }

    // NEW CODE
    public Optional<Achievement> findAchievement(String badgeName) {
        return achievements.stream().filter(a -> a.getBadgeName().equals(badgeName)).findFirst();
    }


    public void markBiomeCompleted(int biomeIndex) {
        if (!completedBiomes.contains(biomeIndex)) {
            completedBiomes.add(biomeIndex);
        }
    }

    public String showProgress() {
        return "Player: " + username + ", | Completed biomes: " + completedBiomes.size() + " | Achievements: "
                + achievements.size();
    }

}

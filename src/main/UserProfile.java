package main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List; 
import java.util.Optional; 

public class UserProfile {
    public static final int MAX_USERNAME_LENGTH = 20; 
    private String username;
    private List<Integer> completedBiomes;
    private List<Achievement> achievements;

    public UserProfile(String username) {
        this.username = validateUsername(username);
        this.completedBiomes = new ArrayList<>();
        this.achievements = new ArrayList<>();
    }

    public String getUsername() {
        return username;
    }

    // validated rename (the profile previously had no way to change its name)
    public void setUsername(String username) {
        this.username = validateUsername(username);
    }

    // trims the name and rejects blank / too long names
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
        return Collections.unmodifiableList(completedBiomes); 
    }

    public List<Achievement> getUserAchievements() {
        return Collections.unmodifiableList(achievements); 
    }

    public void addAchievement(Achievement achievement) {
        if (achievement != null) {
            achievements.add(achievement);
        }
    }

    public boolean hasAchievement(String badgeName) {
        return findAchievement(badgeName).isPresent();
    }

    public Optional<Achievement> findAchievement(String badgeName) {
        return achievements.stream().filter(a -> a.getBadgeName().equals(badgeName)).findFirst();
    }


    public void markBiomeCompleted(int biomeIndex) {
        if (!completedBiomes.contains(biomeIndex)) {
            completedBiomes.add(biomeIndex);
        }
    }

    public String showProgress() {
        return " Completed biomes: " + completedBiomes.size() + " | Achievements: "
                + achievements.size();
    }

}

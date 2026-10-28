package main;

import java.util.ArrayList;
import java.util.List;

public class UserProfile {
    private String username;
    private List<Integer> completedBiomes;
    private List<Achievement> achievements;

    public UserProfile(String username) {
        this.username = username;
        this.completedBiomes = new ArrayList<>();
        this.achievements = new ArrayList<>();
    }

    public String getUsername() {
        return username;
    }

    public List<Integer> getUserProgress() {
        return completedBiomes;
    }

    public List<Achievement> getUserAchievements() {
        return achievements;
    }

    public void addAchievement(Achievement achievement) {
        if (achievement != null) {
            achievements.add(achievement);
        }
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

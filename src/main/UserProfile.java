package main;

import java.util.*;

/** Player profile: name, completed biomes and earned badges. */
public class UserProfile {
    public static final int MAX_NAME_LENGTH = 20;

    private String username;
    private final List<Achievement> userAchievements = new ArrayList<>();
    private final Set<Integer> userProgress = new TreeSet<>();

    public UserProfile(String username) { 
        this.username = validateUsername(username); 
    }

    /** @return the trimmed name; throws IllegalArgumentException when blank or too long. */
    public static String validateUsername(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Name cannot be blank.");
        String n = name.trim();
        if (n.length() > MAX_NAME_LENGTH) throw new IllegalArgumentException("Name must be at most " + MAX_NAME_LENGTH + " characters.");
        return n;
    }

    public String getUsername() { 
        return username;  
    }

    public void setUsername(String name) { 
        this.username = validateUsername(name); 
    }

    public List<Achievement> getUserAchievements() { 
        return Collections.unmodifiableList(userAchievements); 
    }
    public Set<Integer> getUserProgress() { 
        return Collections.unmodifiableSet(userProgress); 
    }

    public void addAchievement(Achievement a) {
         if (!hasAchievement(a.getBadgeName())) userAchievements.add(a);
         }

    public boolean hasAchievement(String badgeName) { 
        return findAchievement(badgeName).isPresent(); 
    }
    public Optional<Achievement> findAchievement(String badgeName) {
        return userAchievements.stream().filter(a -> a.getBadgeName().equals(badgeName)).findFirst();
    }
    public void markBiomeCompleted(int biomeIndex) { 
        userProgress.add(biomeIndex); 
    }

    public String showProgress() {
        return "Biomes completed: " + userProgress.size() + "/" + GameManager.BIOME_COUNT
                + "  |  Badges: " + userAchievements.size() + "/" + GameManager.BIOME_COUNT * GameManager.BADGES_PER_BIOME;
    }
}

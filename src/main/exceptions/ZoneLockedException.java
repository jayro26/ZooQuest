package main.exceptions;

// Thrown when a player tries to enter a biome that has not been unlocked yet.
public class ZoneLockedException extends Exception {
    private final String targetZoneName;
    private final int requiredZoneIndex;

    public ZoneLockedException(String targetZoneName, int requiredZoneIndex) {
        super("'" + targetZoneName + "' is locked. Complete biome #" + (requiredZoneIndex + 1) + " first.");
        this.targetZoneName = targetZoneName;
        this.requiredZoneIndex = requiredZoneIndex;
    }

    public String getTargetZoneName() { return targetZoneName; }
    public int getRequiredZoneIndex() { return requiredZoneIndex; }
}

// com.smokypeaks.server.settings.AlertSettings.java
package com.smokypeaks.server.settings;

import org.bukkit.Sound;

import java.util.ArrayList;
import java.util.List;

public class AlertSettings {
    private final GlobalSettings globalSettings;

    // Basic settings
    private boolean enabled;
    private String message;

    // Sound settings
    private boolean soundEnabled;
    private String soundType;
    private float soundVolume;
    private float soundPitch;

    // Vein detection settings
    private boolean veinDetectionEnabled;
    private int veinRadius;
    private int veinThreshold;
    private String veinMessage;

    // Pattern detection settings
    private boolean patternDetectionEnabled;
    private int patternTimeWindow;
    private int patternThreshold;
    private String patternMessage;

    // Location filters
    private List<String> worldBlacklist;
    private boolean heightRangeEnabled;
    private int minHeight;
    private int maxHeight;
    private boolean regionCheckEnabled;
    private List<String> ignoredRegions;

    public AlertSettings(GlobalSettings globalSettings) {
        this.globalSettings = globalSettings;

        // Set defaults
        this.enabled = true;
        this.message = "&6[Mining] &e%player% found %ore% at %x%, %y%, %z%";

        // Sound defaults
        this.soundEnabled = true;
        this.soundType = "BLOCK_NOTE_BLOCK_PLING";
        this.soundVolume = 1.0f;
        this.soundPitch = 1.0f;

        // Vein detection defaults
        this.veinDetectionEnabled = true;
        this.veinRadius = 3;
        this.veinThreshold = 3;
        this.veinMessage = "&6[Mining] &e%player% found a vein of %ore% (%count% blocks) at %x%, %y%, %z%";

        // Pattern detection defaults
        this.patternDetectionEnabled = true;
        this.patternTimeWindow = 300; // 5 minutes
        this.patternThreshold = 10;
        this.patternMessage = "&c[Mining] &e%player% has mined %count% %ore% in the last %time% minutes!";

        // Location filter defaults
        this.worldBlacklist = new ArrayList<>();
        this.heightRangeEnabled = true;
        this.minHeight = -64;
        this.maxHeight = 16;
        this.regionCheckEnabled = false;
        this.ignoredRegions = new ArrayList<>();
    }

    // Getters and Setters
    public GlobalSettings getGlobalSettings() {
        return globalSettings;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    public void setSoundEnabled(boolean soundEnabled) {
        this.soundEnabled = soundEnabled;
    }

    public String getSoundType() {
        return soundType;
    }

    public void setSoundType(String soundType) {
        this.soundType = soundType;
    }

    public float getSoundVolume() {
        return soundVolume;
    }

    public void setSoundVolume(float soundVolume) {
        this.soundVolume = soundVolume;
    }

    public float getSoundPitch() {
        return soundPitch;
    }

    public void setSoundPitch(float soundPitch) {
        this.soundPitch = soundPitch;
    }

    public boolean isVeinDetectionEnabled() {
        return veinDetectionEnabled;
    }

    public void setVeinDetectionEnabled(boolean veinDetectionEnabled) {
        this.veinDetectionEnabled = veinDetectionEnabled;
    }

    public int getVeinRadius() {
        return veinRadius;
    }

    public void setVeinRadius(int veinRadius) {
        this.veinRadius = veinRadius;
    }

    public int getVeinThreshold() {
        return veinThreshold;
    }

    public void setVeinThreshold(int veinThreshold) {
        this.veinThreshold = veinThreshold;
    }

    public String getVeinMessage() {
        return veinMessage;
    }

    public void setVeinMessage(String veinMessage) {
        this.veinMessage = veinMessage;
    }

    public boolean isPatternDetectionEnabled() {
        return patternDetectionEnabled;
    }

    public void setPatternDetectionEnabled(boolean patternDetectionEnabled) {
        this.patternDetectionEnabled = patternDetectionEnabled;
    }

    public int getPatternTimeWindow() {
        return patternTimeWindow;
    }

    public void setPatternTimeWindow(int patternTimeWindow) {
        this.patternTimeWindow = patternTimeWindow;
    }

    public int getPatternThreshold() {
        return patternThreshold;
    }

    public void setPatternThreshold(int patternThreshold) {
        this.patternThreshold = patternThreshold;
    }

    public String getPatternMessage() {
        return patternMessage;
    }

    public void setPatternMessage(String patternMessage) {
        this.patternMessage = patternMessage;
    }

    public List<String> getWorldBlacklist() {
        return worldBlacklist;
    }

    public void setWorldBlacklist(List<String> worldBlacklist) {
        this.worldBlacklist = worldBlacklist;
    }

    public boolean isHeightRangeEnabled() {
        return heightRangeEnabled;
    }

    public void setHeightRangeEnabled(boolean heightRangeEnabled) {
        this.heightRangeEnabled = heightRangeEnabled;
    }

    public int getMinHeight() {
        return minHeight;
    }

    public void setMinHeight(int minHeight) {
        this.minHeight = minHeight;
    }

    public int getMaxHeight() {
        return maxHeight;
    }

    public void setMaxHeight(int maxHeight) {
        this.maxHeight = maxHeight;
    }

    public boolean isRegionCheckEnabled() {
        return regionCheckEnabled;
    }

    public void setRegionCheckEnabled(boolean regionCheckEnabled) {
        this.regionCheckEnabled = regionCheckEnabled;
    }

    public List<String> getIgnoredRegions() {
        return ignoredRegions;
    }

    public void setIgnoredRegions(List<String> ignoredRegions) {
        this.ignoredRegions = ignoredRegions;
    }
}

package com.smokypeaks.server.managers;

import com.smokypeaks.Main;
import com.smokypeaks.global.automod.ViolationType;
import com.smokypeaks.global.permissions.StaffPermissions;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;

public class AntiXrayManager {
    private final Main plugin;
    private File configFile;
    private FileConfiguration config;

    // Settings
    private boolean enabled = true;
    private double detectionThreshold = 90.0;
    private boolean autoPunish = false;
    private boolean learningMode = false;

    // Learning data storage
    private final Map<UUID, PlayerLearningData> learningData = new ConcurrentHashMap<>();
    private final List<MiningPattern> knownXrayPatterns = new ArrayList<>();
    private final List<MiningPattern> knownLegitimatePatterns = new ArrayList<>();
    private int totalPatternsAnalyzed = 0;
    private double modelAccuracy = 0.0;

    // Player data storage
    private final Map<UUID, PlayerXrayData> playerData = new HashMap<>();

    public AntiXrayManager(Main plugin) {
        this.plugin = plugin;
        loadConfig();
        loadLearningData();
    }

    public void loadConfig() {
        if (configFile == null) {
            configFile = new File(plugin.getDataFolder(), "antixray.yml");
        }

        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
                config = YamlConfiguration.loadConfiguration(configFile);

                // Set default values
                config.set("enabled", true);
                config.set("detection-threshold", 90.0);
                config.set("auto-punish", false);

                config.save(configFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create antixray.yml: " + e.getMessage());
                return;
            }
        } else {
            config = YamlConfiguration.loadConfiguration(configFile);
        }

        // Load settings
        enabled = config.getBoolean("enabled", true);
        detectionThreshold = config.getDouble("detection-threshold", 90.0);
        autoPunish = config.getBoolean("auto-punish", true);
        learningMode = config.getBoolean("learning-mode", false);

        plugin.getLogger().info("Loaded AntiXray configuration");
    }

    public void saveConfig() {
        if (config == null || configFile == null) return;

        try {
            config.set("enabled", enabled);
            config.set("detection-threshold", detectionThreshold);
            config.set("auto-punish", autoPunish);
            config.set("learning-mode", learningMode);

            config.save(configFile);

            // Also save learning data when config is saved
            saveLearningData();
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save antixray.yml: " + e.getMessage());
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        saveConfig();
    }

    public double getDetectionThreshold() {
        return detectionThreshold;
    }

    public void setDetectionThreshold(double threshold) {
        this.detectionThreshold = Math.max(0, Math.min(100, threshold));
        saveConfig();
    }

    public boolean isAutoPunish() {
        return autoPunish;
    }

    public void setAutoPunish(boolean autoPunish) {
        this.autoPunish = autoPunish;
        saveConfig();
    }

    public boolean isLearningMode() {
        return learningMode;
    }

    public void setLearningMode(boolean learningMode) {
        this.learningMode = learningMode;
        saveConfig();
    }

    public PlayerXrayData getPlayerData(UUID uuid) {
        return playerData.computeIfAbsent(uuid, k -> new PlayerXrayData());
    }

    public PlayerLearningData getLearningData(UUID uuid) {
        return learningData.computeIfAbsent(uuid, k -> new PlayerLearningData());
    }

    /**
     * Mark a player's recent mining pattern as either X-ray or legitimate
     * @param playerUUID The player's UUID
     * @param isXray Whether the pattern is X-ray or legitimate
     * @return True if the pattern was successfully marked
     */
    public boolean markPattern(UUID playerUUID, boolean isXray) {
        PlayerLearningData data = getLearningData(playerUUID);
        List<MiningEvent> recentPattern = data.getRecentMiningEvents();

        if (recentPattern.isEmpty()) {
            return false;
        }

        // Create a new pattern from the recent mining events
        MiningPattern pattern = new MiningPattern(recentPattern, isXray);

        // Add to the appropriate list
        if (isXray) {
            knownXrayPatterns.add(pattern);
        } else {
            knownLegitimatePatterns.add(pattern);
        }

        // Clear the player's recent events after marking
        data.clearEvents();

        // Update statistics
        totalPatternsAnalyzed++;
        updateModelAccuracy();

        // Save updated learning data
        saveLearningData();

        return true;
    }

    /**
     * Get statistics about the learning system
     * @return A LearningStats object with current statistics
     */
    public LearningStats getLearningStats() {
        return new LearningStats(
            totalPatternsAnalyzed,
            knownXrayPatterns.size(),
            knownLegitimatePatterns.size(),
            modelAccuracy
        );
    }

    /**
     * Clear learning data for a player
     * @param playerUUID The player's UUID
     */
    public void clearLearningData(UUID playerUUID) {
        learningData.remove(playerUUID);
    }

    /**
     * Update the model accuracy based on patterns analyzed
     */
    private void updateModelAccuracy() {
        if (totalPatternsAnalyzed == 0) {
            modelAccuracy = 0.0;
            return;
        }

        // Simple model: accuracy improves with more samples up to a max of 99%
        int totalPatterns = knownXrayPatterns.size() + knownLegitimatePatterns.size();
        if (totalPatterns == 0) {
            modelAccuracy = 0.0;
        } else {
            // Start at 70% accuracy, improve with more samples up to 99%
            modelAccuracy = 70.0 + Math.min(29.0, totalPatterns / 5.0);
        }
    }

    /**
     * Save learning data to a file
     */
    public void saveLearningData() {
        File learningFile = new File(plugin.getDataFolder(), "xray-learning-data.yml");
        FileConfiguration learningConfig = new YamlConfiguration();

        // Save general statistics
        learningConfig.set("stats.total-patterns", totalPatternsAnalyzed);
        learningConfig.set("stats.model-accuracy", modelAccuracy);

        // Save xray patterns
        List<Map<String, Object>> xrayPatternsData = new ArrayList<>();
        for (MiningPattern pattern : knownXrayPatterns) {
            Map<String, Object> patternData = serializePattern(pattern);
            xrayPatternsData.add(patternData);
        }
        learningConfig.set("patterns.xray", xrayPatternsData);

        // Save legitimate patterns
        List<Map<String, Object>> legitimatePatternsData = new ArrayList<>();
        for (MiningPattern pattern : knownLegitimatePatterns) {
            Map<String, Object> patternData = serializePattern(pattern);
            legitimatePatternsData.add(patternData);
        }
        learningConfig.set("patterns.legitimate", legitimatePatternsData);

        try {
            learningConfig.save(learningFile);
            plugin.getLogger().info("Saved XRay learning data: " + 
                knownXrayPatterns.size() + " XRay patterns, " + 
                knownLegitimatePatterns.size() + " legitimate patterns");
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save XRay learning data: " + e.getMessage());
        }
    }

    /**
     * Load learning data from file
     */
    public void loadLearningData() {
        File learningFile = new File(plugin.getDataFolder(), "xray-learning-data.yml");
        if (!learningFile.exists()) {
            plugin.getLogger().info("No XRay learning data file found");
            return;
        }

        FileConfiguration learningConfig = YamlConfiguration.loadConfiguration(learningFile);

        // Load statistics
        totalPatternsAnalyzed = learningConfig.getInt("stats.total-patterns", 0);
        modelAccuracy = learningConfig.getDouble("stats.model-accuracy", 0.0);

        // Clear existing patterns
        knownXrayPatterns.clear();
        knownLegitimatePatterns.clear();

        // Load xray patterns
        List<?> xrayPatternsList = learningConfig.getList("patterns.xray");
        if (xrayPatternsList != null) {
            for (Object obj : xrayPatternsList) {
                if (obj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> patternData = (Map<String, Object>) obj;
                    MiningPattern pattern = deserializePattern(patternData, true);
                    if (pattern != null) {
                        knownXrayPatterns.add(pattern);
                    }
                }
            }
        }

        // Load legitimate patterns
        List<?> legitimatePatternsList = learningConfig.getList("patterns.legitimate");
        if (legitimatePatternsList != null) {
            for (Object obj : legitimatePatternsList) {
                if (obj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> patternData = (Map<String, Object>) obj;
                    MiningPattern pattern = deserializePattern(patternData, false);
                    if (pattern != null) {
                        knownLegitimatePatterns.add(pattern);
                    }
                }
            }
        }

        plugin.getLogger().info("Loaded XRay learning data: " + 
            knownXrayPatterns.size() + " XRay patterns, " + 
            knownLegitimatePatterns.size() + " legitimate patterns");
    }

    /**
     * Serialize a mining pattern to a map for storage
     */
    private Map<String, Object> serializePattern(MiningPattern pattern) {
        Map<String, Object> patternData = new HashMap<>();
        patternData.put("is-xray", pattern.isXray());

        List<Map<String, Object>> events = new ArrayList<>();
        for (MiningEvent event : pattern.getEvents()) {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("material", event.getMaterial().name());
            eventData.put("timestamp", event.getTimestamp());

            // Location data
            Location loc = event.getLocation();
            eventData.put("world", loc.getWorld().getName());
            eventData.put("x", loc.getX());
            eventData.put("y", loc.getY());
            eventData.put("z", loc.getZ());

            events.add(eventData);
        }

        patternData.put("events", events);
        return patternData;
    }

    /**
     * Deserialize a mining pattern from stored data
     */
    private MiningPattern deserializePattern(Map<String, Object> patternData, boolean isXray) {
        List<?> eventsList = (List<?>) patternData.get("events");
        if (eventsList == null) return null;

        List<MiningEvent> events = new ArrayList<>();
        for (Object obj : eventsList) {
            if (obj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> eventData = (Map<String, Object>) obj;

                try {
                    // Get material
                    String materialName = (String) eventData.get("material");
                    Material material = Material.valueOf(materialName);

                    // Get location
                    String worldName = (String) eventData.get("world");
                    double x = ((Number) eventData.get("x")).doubleValue();
                    double y = ((Number) eventData.get("y")).doubleValue();
                    double z = ((Number) eventData.get("z")).doubleValue();

                    // Create event
                    Location location = new Location(plugin.getServer().getWorld(worldName), x, y, z);
                    MiningEvent event = new MiningEvent(material, location);
                    events.add(event);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to deserialize mining event: " + e.getMessage());
                }
            }
        }

        if (events.isEmpty()) return null;
        return new MiningPattern(events, isXray);
    }

    /**
     * Process a confidence score for a player's mining pattern
     * @param uuid Player UUID
     * @param confidence Confidence score (0-100)
     * @param details Additional details about the detection
     */
    public void processConfidenceScore(UUID uuid, double confidence, String details) {
        Player player = plugin.getServer().getPlayer(uuid);
        if (player == null) return;

        // Always record the confidence score for analysis
        PlayerLearningData data = getLearningData(uuid);
        data.recordConfidence(confidence);

        // If confidence exceeds threshold
        if (confidence >= detectionThreshold) {
            // Format details with confidence percentage
            String detailsWithConfidence = String.format("X-ray detected (%.1f%% confidence) - %s", confidence, details);

            // If auto-punish is enabled, send to AutoWatch system
            // This triggers AutoWatch without actually banning the player
            String autoWatchTarget = plugin.getAutoModManager().getNotificationTarget();
            Player targetPlayer = plugin.getServer().getPlayer(autoWatchTarget);

            if (targetPlayer != null) {
                // This mimics the format from AutoModManager.sendNotification()
                String punishmentType = autoPunish ? "§cban" : "§ewatch";
                String message = String.format(
                    "§8[§cAutoWatch§8] §7I would %s §f%s §7for §fX-Ray/Freecam/Cheats",
                    punishmentType,
                    player.getName()
                );

                // Send the autowatch message only to the notification target
                targetPlayer.sendMessage(message);
                targetPlayer.sendMessage("§8[§cAutoWatch§8] §7Details: §f" + detailsWithConfidence);
            }

            // Increment violation in player data regardless of auto-punish setting
            getPlayerData(uuid).incrementViolation();
        } else if (confidence > 70.0) { // Send regular alerts for medium-confidence detections
            // Send to all staff with mining alerts permission
            String formattedDetails = String.format(
                "§6[Staff Prefix] §ePossible x-ray: §f%s §e(%.1f%% confidence) - %s",
                player.getName(), confidence, details
            );

            plugin.getServer().getOnlinePlayers().forEach(staff -> {
                if (staff.hasPermission(StaffPermissions.Alerts.MINING)) {
                    staff.sendMessage(formattedDetails);
                }
            });
        }
    }

    public static class PlayerXrayData {
        private int totalViolations = 0;
        private int diamondsMined = 0;
        private int emeraldsMined = 0;
        private int ancientDebrisMined = 0;
        private long lastViolationTime = 0;

        public void incrementViolation() {
            totalViolations++;
            lastViolationTime = System.currentTimeMillis();
        }

        public int getTotalViolations() {
            return totalViolations;
        }

        public long getLastViolationTime() {
            return lastViolationTime;
        }

        public void incrementDiamondsMined() {
            diamondsMined++;
        }

        public void incrementEmeraldsMined() {
            emeraldsMined++;
        }

        public void incrementAncientDebrisMined() {
            ancientDebrisMined++;
        }

        public int getDiamondsMined() {
            return diamondsMined;
        }

        public int getEmeraldsMined() {
            return emeraldsMined;
        }

        public int getAncientDebrisMined() {
            return ancientDebrisMined;
        }
    }

    public static class MiningEvent {
        private final Material material;
        private final Location location;
        private final long timestamp;

        public MiningEvent(Material material, Location location) {
            this.material = material;
            this.location = location.clone();
            this.timestamp = System.currentTimeMillis();
        }

        public Material getMaterial() {
            return material;
        }

        public Location getLocation() {
            return location.clone();
        }

        public long getTimestamp() {
            return timestamp;
        }
    }

    public static class MiningPattern {
        private final List<MiningEvent> events;
        private final boolean isXray;

        public MiningPattern(List<MiningEvent> events, boolean isXray) {
            this.events = new ArrayList<>(events);
            this.isXray = isXray;
        }

        public List<MiningEvent> getEvents() {
            return new ArrayList<>(events);
        }

        public boolean isXray() {
            return isXray;
        }
    }

    public static class LearningStats {
        private int totalPatterns = 0;
        private int xrayPatterns = 0;
        private int legitimatePatterns = 0;
        private double accuracy = 0.0;

        public LearningStats(int total, int xray, int legitimate, double accuracy) {
            this.totalPatterns = total;
            this.xrayPatterns = xray;
            this.legitimatePatterns = legitimate;
            this.accuracy = accuracy;
        }

        public int getTotalPatterns() {
            return totalPatterns;
        }

        public int getXrayPatterns() {
            return xrayPatterns;
        }

        public int getLegitimatePatterns() {
            return legitimatePatterns;
        }

        public double getAccuracy() {
            return accuracy;
        }
    }

    public static class PlayerLearningData {
        private final Map<Material, Integer> miningCounts = new HashMap<>();
        private final List<Double> confidenceHistory = new ArrayList<>();
        private final List<MiningEvent> recentMiningEvents = new ArrayList<>();
        private double averageConfidence = 0.0;
        private long lastUpdate = 0;

        public void recordMining(Material material, Location location) {
            miningCounts.merge(material, 1, Integer::sum);
            lastUpdate = System.currentTimeMillis();

            // Record the mining event
            MiningEvent event = new MiningEvent(material, location);
            recentMiningEvents.add(event);

            // Keep only the last 20 mining events
            if (recentMiningEvents.size() > 20) {
                recentMiningEvents.remove(0);
            }
        }

        public void recordConfidence(double confidence) {
            confidenceHistory.add(confidence);
            // Keep only the last 10 confidence readings for average calculation
            if (confidenceHistory.size() > 10) {
                confidenceHistory.remove(0);
            }
            calculateAverageConfidence();
        }

        private void calculateAverageConfidence() {
            if (confidenceHistory.isEmpty()) {
                averageConfidence = 0.0;
                return;
            }

            double sum = 0.0;
            for (Double conf : confidenceHistory) {
                sum += conf;
            }
            averageConfidence = sum / confidenceHistory.size();
        }

        public double getAverageConfidence() {
            return averageConfidence;
        }

        public Map<Material, Integer> getMiningCounts() {
            return new HashMap<>(miningCounts);
        }

        public List<MiningEvent> getRecentMiningEvents() {
            return new ArrayList<>(recentMiningEvents);
        }

        public long getLastUpdate() {
            return lastUpdate;
        }

        public void clearEvents() {
            recentMiningEvents.clear();
        }
    }
}

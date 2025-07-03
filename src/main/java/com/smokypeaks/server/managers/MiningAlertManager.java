package com.smokypeaks.server.managers;

import com.smokypeaks.Main;
import com.smokypeaks.global.permissions.StaffPermissions;
import com.smokypeaks.server.settings.AlertSettings;
import com.smokypeaks.server.settings.GlobalSettings;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MiningAlertManager {
    private final Main plugin;
    private final Set<UUID> alertsEnabled = ConcurrentHashMap.newKeySet();
    private final Map<Material, AlertSettings> alertSettings = new HashMap<>();
    private final Map<UUID, Map<Material, Long>> playerCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Map<Material, List<MiningRecord>>> playerMiningHistory = new ConcurrentHashMap<>();

    public MiningAlertManager(Main plugin) {
        this.plugin = plugin;
        loadSettings();
    }

    public void loadSettings() {
        alertSettings.clear();
        ConfigurationSection config = plugin.getConfig().getConfigurationSection("mining-alerts");

        if (config == null) {
            plugin.getLogger().warning("No mining-alerts section found in config, creating defaults...");
            setDefaultSettings();
            return;
        }

        // Load global settings
        GlobalSettings globalSettings = loadGlobalSettings(config.getConfigurationSection("global"));

        // Load ore-specific settings
        for (Material material : new Material[]{
                Material.DIAMOND_ORE,
                Material.DEEPSLATE_DIAMOND_ORE,
                Material.ANCIENT_DEBRIS
        }) {
            String materialKey = material.name().toLowerCase();
            ConfigurationSection materialSection = config.getConfigurationSection(materialKey);

            if (materialSection == null) {
                // Create default settings for this material
                AlertSettings settings = new AlertSettings(globalSettings);
                settings.setEnabled(true);
                settings.setMessage("&6[Mining] &e%player% found %ore% at %x%, %y%, %z%");
                alertSettings.put(material, settings);
            } else {
                AlertSettings settings = loadMaterialSettings(materialSection, globalSettings);
                alertSettings.put(material, settings);
            }
        }

        // Save settings to ensure defaults are written
        saveSettings();
    }

    private GlobalSettings loadGlobalSettings(ConfigurationSection config) {
        GlobalSettings settings = new GlobalSettings();
        if (config == null) return settings;

        settings.setDefaultEnabled(config.getBoolean("default-enabled", true));

        ConfigurationSection staffPrefs = config.getConfigurationSection("staff-preferences");
        if (staffPrefs != null) {
            settings.setAlertRadius(staffPrefs.getInt("alert-radius", 100));

            ConfigurationSection format = staffPrefs.getConfigurationSection("format-options");
            if (format != null) {
                settings.setShowCoordinates(format.getBoolean("show-coordinates", true));
                settings.setShowWorld(format.getBoolean("show-world", true));
                settings.setUseRelativeCoordinates(format.getBoolean("use-relative-coordinates", false));
            }

            ConfigurationSection colors = staffPrefs.getConfigurationSection("colors");
            if (colors != null) {
                settings.setSuspiciousColor(colors.getString("suspicious", "&c"));
                settings.setNormalColor(colors.getString("normal", "&e"));
                settings.setVeinColor(colors.getString("vein", "&a"));
            }
        }

        ConfigurationSection timeRestrictions = config.getConfigurationSection("time-restrictions");
        if (timeRestrictions != null) {
            settings.setCooldown(timeRestrictions.getInt("cooldown", 30));

            ConfigurationSection quietHours = timeRestrictions.getConfigurationSection("quiet-hours");
            if (quietHours != null && quietHours.getBoolean("enabled", false)) {
                settings.setQuietHoursEnabled(true);
                settings.setQuietHoursStart(quietHours.getString("start", "23:00"));
                settings.setQuietHoursEnd(quietHours.getString("end", "07:00"));
            }
        }

        return settings;
    }

    private AlertSettings loadMaterialSettings(ConfigurationSection config, GlobalSettings globalSettings) {
        AlertSettings settings = new AlertSettings(globalSettings);

        settings.setEnabled(config.getBoolean("enabled", true));
        settings.setMessage(config.getString("message",
                "&6[Mining] &e%player% found %ore% at %x%, %y%, %z%"));

        // Load sound settings
        ConfigurationSection sound = config.getConfigurationSection("sound");
        if (sound != null) {
            settings.setSoundEnabled(sound.getBoolean("enabled", true));
            settings.setSoundType(sound.getString("type", "BLOCK_NOTE_BLOCK_PLING"));
            settings.setSoundVolume((float) sound.getDouble("volume", 1.0));
            settings.setSoundPitch((float) sound.getDouble("pitch", 1.0));
        }

        // Load vein detection settings
        ConfigurationSection vein = config.getConfigurationSection("vein-detection");
        if (vein != null) {
            settings.setVeinDetectionEnabled(vein.getBoolean("enabled", true));
            settings.setVeinRadius(vein.getInt("radius", 3));
            settings.setVeinThreshold(vein.getInt("threshold", 3));
            settings.setVeinMessage(vein.getString("message",
                    "&6[Mining] &e%player% found a vein of %ore% (%count% blocks) at %x%, %y%, %z%"));
        }

        // Load pattern detection settings
        ConfigurationSection pattern = config.getConfigurationSection("pattern-detection");
        if (pattern != null) {
            settings.setPatternDetectionEnabled(pattern.getBoolean("enabled", true));
            settings.setPatternTimeWindow(pattern.getInt("time-window", 300));
            settings.setPatternThreshold(pattern.getInt("threshold", 10));
            settings.setPatternMessage(pattern.getString("message",
                    "&c[Mining] &e%player% has mined %count% %ore% in the last %time% minutes!"));
        }

        // Load location filters
        ConfigurationSection location = config.getConfigurationSection("location-filters");
        if (location != null) {
            settings.setWorldBlacklist(location.getStringList("world-blacklist"));

            ConfigurationSection height = location.getConfigurationSection("height-range");
            if (height != null) {
                settings.setHeightRangeEnabled(height.getBoolean("enabled", true));
                settings.setMinHeight(height.getInt("min", -64));
                settings.setMaxHeight(height.getInt("max", 16));
            }

            ConfigurationSection regions = location.getConfigurationSection("regions");
            if (regions != null) {
                settings.setRegionCheckEnabled(regions.getBoolean("enabled", false));
                settings.setIgnoredRegions(regions.getStringList("ignored-regions"));
            }
        }

        return settings;
    }

    // ... (continued from previous message)

    private void setDefaultSettings() {
        GlobalSettings globalSettings = new GlobalSettings();
        setDefaultForMaterial(Material.DIAMOND_ORE, globalSettings);
        setDefaultForMaterial(Material.DEEPSLATE_DIAMOND_ORE, globalSettings);
        setDefaultForMaterial(Material.ANCIENT_DEBRIS, globalSettings);
        saveSettings();
    }

    private void setDefaultForMaterial(Material material, GlobalSettings globalSettings) {
        AlertSettings settings = new AlertSettings(globalSettings);
        alertSettings.put(material, settings);
    }

    public void saveSettings() {
        ConfigurationSection config = plugin.getConfig().createSection("mining-alerts");

        // Save global settings
        saveGlobalSettings(config.createSection("global"));

        // Save material specific settings
        for (Map.Entry<Material, AlertSettings> entry : alertSettings.entrySet()) {
            ConfigurationSection materialSection = config.createSection(entry.getKey().name().toLowerCase());
            saveMaterialSettings(materialSection, entry.getValue());
        }

        plugin.saveConfig();
    }

    private void saveGlobalSettings(ConfigurationSection config) {
        GlobalSettings settings = alertSettings.values().iterator().next().getGlobalSettings();

        config.set("default-enabled", settings.isDefaultEnabled());

        ConfigurationSection staffPrefs = config.createSection("staff-preferences");
        staffPrefs.set("alert-radius", settings.getAlertRadius());

        ConfigurationSection format = staffPrefs.createSection("format-options");
        format.set("show-coordinates", settings.isShowCoordinates());
        format.set("show-world", settings.isShowWorld());
        format.set("use-relative-coordinates", settings.isUseRelativeCoordinates());

        ConfigurationSection colors = staffPrefs.createSection("colors");
        colors.set("suspicious", settings.getSuspiciousColor());
        colors.set("normal", settings.getNormalColor());
        colors.set("vein", settings.getVeinColor());

        ConfigurationSection timeRestrictions = config.createSection("time-restrictions");
        timeRestrictions.set("cooldown", settings.getCooldown());

        ConfigurationSection quietHours = timeRestrictions.createSection("quiet-hours");
        quietHours.set("enabled", settings.isQuietHoursEnabled());
        quietHours.set("start", settings.getQuietHoursStart());
        quietHours.set("end", settings.getQuietHoursEnd());
    }

    private void saveMaterialSettings(ConfigurationSection config, AlertSettings settings) {
        config.set("enabled", settings.isEnabled());
        config.set("message", settings.getMessage());

        ConfigurationSection sound = config.createSection("sound");
        sound.set("enabled", settings.isSoundEnabled());
        sound.set("type", settings.getSoundType());
        sound.set("volume", settings.getSoundVolume());
        sound.set("pitch", settings.getSoundPitch());

        ConfigurationSection vein = config.createSection("vein-detection");
        vein.set("enabled", settings.isVeinDetectionEnabled());
        vein.set("radius", settings.getVeinRadius());
        vein.set("threshold", settings.getVeinThreshold());
        vein.set("message", settings.getVeinMessage());

        ConfigurationSection pattern = config.createSection("pattern-detection");
        pattern.set("enabled", settings.isPatternDetectionEnabled());
        pattern.set("time-window", settings.getPatternTimeWindow());
        pattern.set("threshold", settings.getPatternThreshold());
        pattern.set("message", settings.getPatternMessage());

        ConfigurationSection location = config.createSection("location-filters");
        location.set("world-blacklist", settings.getWorldBlacklist());

        ConfigurationSection height = location.createSection("height-range");
        height.set("enabled", settings.isHeightRangeEnabled());
        height.set("min", settings.getMinHeight());
        height.set("max", settings.getMaxHeight());

        ConfigurationSection regions = location.createSection("regions");
        regions.set("enabled", settings.isRegionCheckEnabled());
        regions.set("ignored-regions", settings.getIgnoredRegions());
    }

    public boolean toggleAlerts(Player player) {
        UUID uuid = player.getUniqueId();
        if (alertsEnabled.contains(uuid)) {
            alertsEnabled.remove(uuid);
            return false;
        } else {
            alertsEnabled.add(uuid);
            return true;
        }
    }

    public boolean shouldNotifyPlayer(Player staff, Location mineLocation) {
        if (!alertsEnabled.contains(staff.getUniqueId())) return false;

        AlertSettings settings = getDefaultSettings();
        int alertRadius = settings.getGlobalSettings().getAlertRadius();

        if (alertRadius > 0) {
            return staff.getWorld().equals(mineLocation.getWorld()) &&
                    staff.getLocation().distance(mineLocation) <= alertRadius;
        }

        return true;
    }

    public void handleBlockMine(Player player, Material material, Location location) {
        AlertSettings settings = alertSettings.get(material);
        if (settings == null) {
            plugin.getLogger().warning("No settings found for material: " + material);
            return;
        }

        if (!settings.isEnabled()) {
            plugin.getLogger().fine("Mining alerts disabled for: " + material);
            return;
        }

        // Check quiet hours
        if (settings.getGlobalSettings().isQuietHoursEnabled() && isQuietHours()) {
            return;
        }

        // Check cooldown
        if (isOnCooldown(player.getUniqueId(), material)) {
            return;
        }

        // Check location filters
        if (!isValidMiningLocation(location, settings)) {
            return;
        }

        // Check for vein
        int veinSize = 0;
        if (settings.isVeinDetectionEnabled()) {
            veinSize = calculateVeinSize(location, material, settings.getVeinRadius());
        }

        // Record mining history for pattern detection
        recordMining(player.getUniqueId(), material, location);

        // Check for suspicious patterns
        boolean isSuspicious = false;
        if (settings.isPatternDetectionEnabled()) {
            isSuspicious = checkSuspiciousPattern(player.getUniqueId(), material, settings);
        }

        // Send notifications
        sendNotifications(player, material, location, veinSize, isSuspicious, settings);
    }

    private boolean isQuietHours() {
        LocalTime now = LocalTime.now();
        LocalTime start = LocalTime.parse(getDefaultSettings().getGlobalSettings().getQuietHoursStart());
        LocalTime end = LocalTime.parse(getDefaultSettings().getGlobalSettings().getQuietHoursEnd());

        if (start.isAfter(end)) {
            return !now.isAfter(end) || !now.isBefore(start);
        } else {
            return !now.isBefore(start) && !now.isAfter(end);
        }
    }

    // ... (continued from previous message)

    private boolean isOnCooldown(UUID playerUUID, Material material) {
        Map<Material, Long> cooldowns = playerCooldowns.computeIfAbsent(playerUUID, k -> new HashMap<>());
        Long lastNotification = cooldowns.get(material);

        if (lastNotification == null) return false;

        long cooldownTime = getSettings(material).getGlobalSettings().getCooldown() * 1000L;
        if (System.currentTimeMillis() - lastNotification < cooldownTime) {
            return true;
        }

        cooldowns.put(material, System.currentTimeMillis());
        return false;
    }

    private boolean isValidMiningLocation(Location location, AlertSettings settings) {
        // Check world blacklist
        if (settings.getWorldBlacklist().contains(location.getWorld().getName())) {
            return false;
        }

        // Check height range
        if (settings.isHeightRangeEnabled()) {
            int y = location.getBlockY();
            if (y < settings.getMinHeight() || y > settings.getMaxHeight()) {
                return false;
            }
        }

        return true;
    }

    private int calculateVeinSize(Location center, Material material, int radius) {
        Set<Location> checked = new HashSet<>();
        return countConnectedOres(center, material, radius, checked);
    }

    private int countConnectedOres(Location location, Material material, int radius, Set<Location> checked) {
        if (checked.contains(location)) return 0;
        checked.add(location);

        if (!location.getBlock().getType().equals(material)) return 0;

        int count = 1;
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;

                    Location nearby = location.clone().add(x, y, z);
                    if (nearby.distance(location) <= radius) {
                        count += countConnectedOres(nearby, material, radius, checked);
                    }
                }
            }
        }

        return count;
    }

    private void recordMining(UUID playerUUID, Material material, Location location) {
        Map<Material, List<MiningRecord>> playerRecords =
                playerMiningHistory.computeIfAbsent(playerUUID, k -> new HashMap<>());

        List<MiningRecord> records = playerRecords.computeIfAbsent(material, k -> new ArrayList<>());
        records.add(new MiningRecord(System.currentTimeMillis(), location));

        // Clean up old records
        long cutoff = System.currentTimeMillis() - (30 * 60 * 1000); // 30 minutes
        records.removeIf(record -> record.timestamp() < cutoff);
    }

    private boolean checkSuspiciousPattern(UUID playerUUID, Material material, AlertSettings settings) {
        Map<Material, List<MiningRecord>> playerRecords = playerMiningHistory.get(playerUUID);
        if (playerRecords == null) return false;

        List<MiningRecord> records = playerRecords.get(material);
        if (records == null) return false;

        long timeWindow = settings.getPatternTimeWindow() * 1000L;
        long cutoff = System.currentTimeMillis() - timeWindow;

        long recentCount = records.stream()
                .filter(record -> record.timestamp() > cutoff)
                .count();

        return recentCount >= settings.getPatternThreshold();
    }

    private void sendNotifications(Player player, Material material, Location location, int veinSize, boolean isSuspicious, AlertSettings settings) {
        String message;
        String color;

        if (isSuspicious) {
            message = settings.getPatternMessage();
            color = settings.getGlobalSettings().getSuspiciousColor();
        } else if (veinSize >= settings.getVeinThreshold()) {
            message = settings.getVeinMessage();
            color = settings.getGlobalSettings().getVeinColor();
        } else {
            message = settings.getMessage();
            color = settings.getGlobalSettings().getNormalColor();
        }

        message = formatMessage(message, player, material, location, veinSize, settings.getPatternTimeWindow());
        message = color + message;

        // Send to staff
        for (Player staff : plugin.getServer().getOnlinePlayers()) {
            if (shouldNotifyPlayer(staff, location)) {
                staff.sendMessage(message);

                if (settings.isSoundEnabled()) {
                    try {
                        // Convert sound name format to lowercase with dots (Minecraft 1.20+ format)
                        String soundName = settings.getSoundType().toLowerCase().replace("_", ".");
                        staff.playSound(staff.getLocation(),
                                soundName,
                                settings.getSoundVolume(),
                                settings.getSoundPitch());
                    } catch (Exception e) {
                        // Fallback to a default sound if there's an error
                        staff.playSound(staff.getLocation(), "block.note_block.pling", 1.0f, 1.0f);
                        plugin.getLogger().warning("Error playing sound: " + e.getMessage());
                    }
                }
            }
        }
    }

    private String formatMessage(String message, Player player, Material material, Location location, int veinSize, int timeWindow) {
        return message
                .replace("%player%", player.getName())
                .replace("%ore%", formatMaterialName(material))
                .replace("%x%", String.valueOf(location.getBlockX()))
                .replace("%y%", String.valueOf(location.getBlockY()))
                .replace("%z%", String.valueOf(location.getBlockZ()))
                .replace("%world%", location.getWorld().getName())
                .replace("%count%", String.valueOf(veinSize))
                .replace("%time%", String.valueOf(timeWindow / 60)) // Convert seconds to minutes
                .replace("&", "§");
    }

    private String formatMaterialName(Material material) {
        return material.name()
                .replace("_", " ")
                .toLowerCase()
                .replace("deepslate", "Deepslate ");
    }

    public AlertSettings getSettings(Material material) {
        return alertSettings.getOrDefault(material, getDefaultSettings());
    }

    private AlertSettings getDefaultSettings() {
        return alertSettings.values().iterator().next();
    }

    public void cleanup() {
        alertsEnabled.clear();
        playerCooldowns.clear();
        playerMiningHistory.clear();
    }

    private record MiningRecord(long timestamp, Location location) {}
}

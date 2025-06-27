// com.smokypeaks.global.utils.SettingsManager.java
package com.smokypeaks.global.utils;

import com.smokypeaks.bungee.BungeeMain;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SettingsManager {
    private final BungeeMain plugin;
    private Configuration config;
    private final Set<UUID> staffChatSoundDisabled = new HashSet<>();
    private final Set<UUID> adminChatSoundDisabled = new HashSet<>();

    public SettingsManager(BungeeMain plugin) {
        this.plugin = plugin;
        loadConfig();
        loadSettings();
    }

    private void loadConfig() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdir();
        }

        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            try (InputStream in = plugin.getResourceAsStream("config.yml")) {
                Files.copy(in, configFile.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class)
                    .load(new File(plugin.getDataFolder(), "config.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reloadConfig() {
        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class)
                    .load(new File(plugin.getDataFolder(), "config.yml"));
            loadSettings();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadSettings() {
        // Load disabled sounds lists from config
        config.getStringList("data.staff-chat-sound-disabled")
                .forEach(uuid -> staffChatSoundDisabled.add(UUID.fromString(uuid)));
        config.getStringList("data.admin-chat-sound-disabled")
                .forEach(uuid -> adminChatSoundDisabled.add(UUID.fromString(uuid)));
    }

    public void saveSettings() {
        config.set("data.staff-chat-sound-disabled",
                staffChatSoundDisabled.stream().map(UUID::toString).toList());
        config.set("data.admin-chat-sound-disabled",
                adminChatSoundDisabled.stream().map(UUID::toString).toList());

        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class)
                    .save(config, new File(plugin.getDataFolder(), "config.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getFormat(String chatType, String group) {
        String groupFormat = config.getString(chatType + ".group-formats." + group);
        return groupFormat != null ? groupFormat : config.getString(chatType + ".default-format");
    }

    public boolean isSoundEnabled(UUID uuid, String chatType) {
        if (chatType.equals("staff")) {
            return !staffChatSoundDisabled.contains(uuid);
        } else {
            return !adminChatSoundDisabled.contains(uuid);
        }
    }

    public void toggleSound(UUID uuid, String chatType) {
        Set<UUID> disabledSet = chatType.equals("staff") ?
                staffChatSoundDisabled : adminChatSoundDisabled;

        if (disabledSet.contains(uuid)) {
            disabledSet.remove(uuid);
        } else {
            disabledSet.add(uuid);
        }
        saveSettings();
    }

    public String getSoundEffect(String chatType) {
        return config.getString(chatType + ".sound.type");
    }

    public float getSoundVolume(String chatType) {
        return (float) config.getDouble(chatType + ".sound.volume");
    }

    public float getSoundPitch(String chatType) {
        return (float) config.getDouble(chatType + ".sound.pitch");
    }

    // Add this method to expose the config
    public Configuration getConfig() {
        return config;
    }
}

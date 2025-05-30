// com.smokypeaks.global.config.Config.java
package com.smokypeaks.global.config;

import com.smokypeaks.Main;
import org.bukkit.configuration.file.FileConfiguration;

public class Config {
    private final Main plugin;
    private FileConfiguration config;

    public Config(Main plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        plugin.saveDefaultConfig();
        config = plugin.getConfig();

        // Set defaults if they don't exist
        config.addDefault("github.repo", "Blakeoo2/UltimateStaff");
        config.addDefault("github.branch", "main");
        config.addDefault("version", plugin.getDescription().getVersion());
        config.addDefault("auto-update", true);

        config.options().copyDefaults(true);
        plugin.saveConfig();
    }

    public String getGithubRepo() {
        return config.getString("github.repo");
    }

    public boolean isAutoUpdateEnabled() {
        return config.getBoolean("auto-update");
    }
}

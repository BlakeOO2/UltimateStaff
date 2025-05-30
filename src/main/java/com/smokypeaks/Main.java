package com.smokypeaks;

import com.smokypeaks.global.utils.UpdateChecker;
import com.smokypeaks.server.commands.UpdateCheckCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.PluginCommand;

import java.io.File;

public class Main extends JavaPlugin {
    private static Main instance;
    private boolean isBungee = false;
    private UpdateChecker updateChecker;

    @Override

    public void onLoad() {
        instance = this;
        System.out.println("=== UltimateStaff is loading ==="); // This will show up even if plugin logging isn't initialized
        getLogger().info("UltimateStaff is loading...");
    }


    @Override
    public void onEnable() {
        // Add these debug lines at the very start
        System.out.println("=== UltimateStaff Enable Start ===");
        System.out.println("Plugin Data Folder: " + getDataFolder().getAbsolutePath());
        System.out.println("Config File Exists: " + new File(getDataFolder(), "config.yml").exists());

        // Check if we're running on Bungee or Paper
        try {
            Class.forName("net.md_5.bungee.api.ProxyServer");
            isBungee = true;
            getLogger().info("Running in BungeeCord mode");
        } catch (ClassNotFoundException e) {
            isBungee = false;
            getLogger().info("Running in Paper mode");
        }

        // Initialize config
        saveDefaultConfig();

        // Initialize UpdateChecker
        this.updateChecker = new UpdateChecker(
                this,
                getConfig().getString("github.repo", "BlakeOO2/UltimateStaff"),
                getDescription().getVersion()
        );

        if (isBungee) {
            initializeBungee();
        } else {
            initializeServer();
        }

        // Register commands safely
        registerCommands();

        // Start update checker
        updateChecker.checkForUpdates();

        getLogger().info("UltimateStaff has been enabled successfully!");
    }

    @Override
    public void onDisable() {
        getLogger().info("UltimateStaff is shutting down...");

        if (!isBungee) {
            // Save any necessary data
        }

        getLogger().info("UltimateStaff has been disabled!");
    }

    private void initializeBungee() {
        getLogger().info("Initializing BungeeCord features...");
        // Initialize BungeeCord specific features
    }

    private void initializeServer() {
        getLogger().info("Initializing Paper server features...");
        // Initialize Paper specific features
    }

    private void registerCommands() {
        if (!isBungee) {
            getLogger().info("Registering Paper commands...");
            PluginCommand updateCommand = getCommand("ultimatestaffupdate");
            if (updateCommand != null) {
                updateCommand.setExecutor(new UpdateCheckCommand(this));
                getLogger().info("Registered update command successfully");
            } else {
                getLogger().warning("Failed to register update command - command not found in plugin.yml!");
            }
        } else {
            getLogger().info("Registering BungeeCord commands...");
            // Register BungeeCord commands here
        }
    }

    public static Main getInstance() {
        return instance;
    }

    public boolean isBungee() {
        return isBungee;
    }

    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }
}

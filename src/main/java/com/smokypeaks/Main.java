// Main.java
package com.smokypeaks;

import com.smokypeaks.global.utils.UpdateChecker;
import com.smokypeaks.server.commands.UpdateCheckCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    private static Main instance;
    private boolean isBungee = false;
    private UpdateChecker updateChecker;

    @Override
    public void onEnable() {
        instance = this;

        // Check if we're running on Bungee or Paper
        try {
            Class.forName("net.md_5.bungee.api.ProxyServer");
            isBungee = true;
        } catch (ClassNotFoundException e) {
            isBungee = false;
        }

        // Initialize config first
        saveDefaultConfig();

        // Initialize UpdateChecker
        this.updateChecker = new UpdateChecker(
                this,
                getConfig().getString("github.repo", "BlakeOO2/UltimateStaff"),
                getDescription().getVersion()
        );

        // Initialize appropriate systems
        if (isBungee) {
            initializeBungee();
        } else {
            initializeServer();
        } //test

        // Register commands
        getCommand("ultimatestaffupdate").setExecutor(new UpdateCheckCommand(this));

        // Start update checker
        updateChecker.checkForUpdates();
    }

    private void initializeBungee() {
        // Bungee specific initialization
    }

    private void initializeServer() {
        // Server specific initialization
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

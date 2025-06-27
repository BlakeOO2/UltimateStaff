package com.smokypeaks.bungee;

import com.smokypeaks.bungee.commands.*;
import com.smokypeaks.bungee.discord.DiscordManager;
import com.smokypeaks.bungee.listeners.AdminChatListener;
import com.smokypeaks.bungee.listeners.StaffChatListener;
import com.smokypeaks.global.utils.SettingsManager;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.md_5.bungee.api.plugin.Plugin;

import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class BungeeMain extends Plugin {
    private SettingsManager settingsManager;
    private LuckPerms luckPerms;
    private StaffChatCommand staffChatCommand;
    private AdminChatCommand adminChatCommand;
    private DiscordManager discordManager;

    @Override
    public void onEnable() {
        getProxy().registerChannel("ultimatestaff:main");
        // Initialize settings
        settingsManager = new SettingsManager(this);
        this.discordManager = new DiscordManager(this);

        // Initialize LuckPerms
        try {
            luckPerms = LuckPermsProvider.get();
        } catch (IllegalStateException e) {
            getLogger().severe("LuckPerms not found! Disabling plugin...");
            return;
        }

        // Initialize commands
        staffChatCommand = new StaffChatCommand(this);  // Remove luckPerms parameter
        adminChatCommand = new AdminChatCommand(this);  // Remove luckPerms parameter

        getProxy().getPluginManager().registerCommand(this, staffChatCommand);
        getProxy().getPluginManager().registerCommand(this, adminChatCommand);
        getProxy().getPluginManager().registerCommand(this, new DiscordReloadCommand(this));
        getProxy().getPluginManager().registerCommand(this, new PreviewChatFormatCommand(this));
        getProxy().getPluginManager().registerCommand(this,
                new ToggleChatSoundCommand(this, "staff"));
        getProxy().getPluginManager().registerCommand(this,
                new ToggleChatSoundCommand(this, "admin"));

        // Register listeners
        getProxy().getPluginManager().registerListener(this,
                new StaffChatListener(this, staffChatCommand));
        getProxy().getPluginManager().registerListener(this,
                new AdminChatListener(this, adminChatCommand));
    }
    @Override
    public void onDisable() {
        getProxy().unregisterChannel("ultimatestaff:main");
        // Rest of the code...
        if (discordManager != null) {
            discordManager.shutdown();
        }
    }

    public DiscordManager getDiscordManager() {
        return discordManager;
    }

    public Configuration getConfig() {
        return settingsManager.getConfig();
    }

    // Update SettingsManager to expose the config
// In SettingsManager.java


    public void setDiscordManager(DiscordManager discordManager) {
        this.discordManager = discordManager;
    }

    public void reloadConfig() {
        settingsManager = new SettingsManager(this);
    }

    public SettingsManager getSettingsManager() {
        return settingsManager;
    }

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }
}

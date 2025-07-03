// Update Main.java
package com.smokypeaks;

import com.smokypeaks.bungee.discord.DiscordManager;
import com.smokypeaks.global.automod.AutoModManager;
import com.smokypeaks.global.utils.UpdateChecker;
import com.smokypeaks.server.commands.*;
import com.smokypeaks.server.listeners.*;
import com.smokypeaks.server.managers.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.PluginCommand;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class Main extends JavaPlugin {
    private static Main instance;
    private boolean isBungee = false;
    private UpdateChecker updateChecker;
    private StaffModeManager staffModeManager;
    private FreezeManager freezeManager;
    private InventoryManager inventoryManager;
    private MiningAlertManager miningAlertManager;
    private boolean worldGuardEnabled = false;
    private TeleportManager teleportManager;
    private PunishmentManager punishmentManager;
    private DeathManager deathManager;
    private ChatFilterManager chatFilterManager;
    private LagManager lagManager;
    private AutoModManager autoModManager;
    private AntiXrayManager antiXrayManager;

    @Override
    public void onLoad() {
        instance = this;
        getLogger().info("=== UltimateStaff is loading ===");
        getLogger().info("UltimateStaff is loading...");

        // Check for WorldGuard
        if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            worldGuardEnabled = true;
            getLogger().info("WorldGuard found - enabling region support");
        }
    }

    @Override
    public void onEnable() {
        // Add these debug lines at the very start
        getLogger().info("=== UltimateStaff Enable Start ===");
        getLogger().info("Plugin Data Folder: " + getDataFolder().getAbsolutePath());
        getLogger().info("Config File Exists: " + new File(getDataFolder(), "config.yml").exists());

        // Check if we need to apply an update from a previous download
        checkAndApplyUpdate();

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

        // Initialize managers
        this.updateChecker = new UpdateChecker(
                this,
                getConfig().getString("github.repo", "BlakeOO2/UltimateStaff"),
                getDescription().getVersion()
        );
        this.staffModeManager = new StaffModeManager(this);
        this.freezeManager = new FreezeManager(this);
        this.inventoryManager = new InventoryManager(this);
        this.miningAlertManager = new MiningAlertManager(this);
        this.teleportManager = new TeleportManager(this);
        this.punishmentManager = new PunishmentManager(this);
        this.deathManager = new DeathManager(this);
        this.chatFilterManager = new ChatFilterManager(this);
        this.lagManager = new LagManager(this);
        this.autoModManager = new AutoModManager(this);
        this.antiXrayManager = new AntiXrayManager(this);
        getServer().getPluginManager().registerEvents(new PunishmentMenuListener(this), this);

        if (isBungee) {
            initializeBungee();
        } else {
            initializeServer();
        }

        // Register plugin message channels
        if (!isBungee) {
            getServer().getMessenger().registerIncomingPluginChannel(this, "ultimatestaff:main",
                    new ServerPluginMessageListener(this));
            getServer().getMessenger().registerOutgoingPluginChannel(this, "ultimatestaff:main");
            getLogger().info("Registered plugin messaging channels");
        }

        // Register listeners
        registerListeners();

        // Register commands
        registerCommands();

        // Start update checker
        updateChecker.checkForUpdates(false);

        getLogger().info("UltimateStaff has been enabled successfully!");
    }

    private void registerListeners() {
        //getServer().getPluginManager().registerEvents(new StaffToolListener(this), this);
        getServer().getPluginManager().registerEvents(new MiningListener(this), this);
        getServer().getPluginManager().registerEvents(new DeathListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatFilterListener(this), this);
        // PunishmentMenuListener is already registered in onEnable method
        getLogger().info("Using existing PunishmentMenuListener registration");

        // Register AutoMod listeners
        getServer().getPluginManager().registerEvents(new com.smokypeaks.server.listeners.automod.chat.AutoModChatListener(this), this);
        getServer().getPluginManager().registerEvents(new AutoModPlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new com.smokypeaks.server.listeners.automod.antixray.AntiXrayListener(this), this);
        getLogger().info("Registered AutoMod listeners");

        // Register other listeners as needed
    }

    @Override
    public void onDisable() {
        getLogger().info("UltimateStaff is shutting down...");


        if (!isBungee) {
            // Cleanup managers
            staffModeManager.cleanup();
            freezeManager.cleanup();
            inventoryManager.cleanup();
            miningAlertManager.cleanup();
            teleportManager.cleanup();
            deathManager.cleanup();

            // Save AntiXray learning data
            if (antiXrayManager != null) {
                antiXrayManager.saveLearningData();
            }

            // Unregister plugin message channels
            try {
                getServer().getMessenger().unregisterIncomingPluginChannel(this, "ultimatestaff:main");
                getServer().getMessenger().unregisterOutgoingPluginChannel(this, "ultimatestaff:main");
                getLogger().info("Unregistered plugin messaging channels");
            } catch (Exception e) {
                getLogger().warning("Error unregistering plugin messaging channels: " + e.getMessage());
            }
        }

        getLogger().info("UltimateStaff has been disabled!");
    }

    private void initializeBungee() {
        getLogger().info("Initializing BungeeCord features...");
        // Currently no BungeeCord-specific initialization needed
    }


    private void initializeServer() {
        getLogger().info("Initializing Paper server features...");
        // Initialize Paper-specific features
        if (!isBungee) {
            // Register event listeners
            getServer().getPluginManager().registerEvents(new StaffToolListener(this), this);
            getServer().getPluginManager().registerEvents(new MiningListener(this), this);
            // Add any other Paper-specific initialization
        }
    }

    public PunishmentManager getPunishmentManager() {
        return punishmentManager;
    }

    public DeathManager getDeathManager() {
        return deathManager;
    }

    public ChatFilterManager getChatFilterManager() {
        return chatFilterManager;
    }

    public LagManager getLagManager() {
        return lagManager;
    }

    public AutoModManager getAutoModManager() {
        return autoModManager;
    }

    public AntiXrayManager getAntiXrayManager() {
        return antiXrayManager;
    }

    private void registerCommands() {
        if (!isBungee) {
            getLogger().info("Registering Paper commands...");

            // Update Command
            PluginCommand updateCommand = getCommand("ultimatestaffupdate");
            if (updateCommand != null) {
                updateCommand.setExecutor(new UpdateCheckCommand(this));
                getLogger().info("Registered update command successfully");
            }

            // Staff Mode Command
            PluginCommand staffModeCommand = getCommand("staffmode");
            if (staffModeCommand != null) {
                staffModeCommand.setExecutor(new StaffModeCommand(this));
                getLogger().info("Registered staff mode command successfully");
            }

            // Inventory Commands
            PluginCommand invseeCommand = getCommand("invsee");
            if (invseeCommand != null) {
                invseeCommand.setExecutor(new InvseeCommand(this));
                getLogger().info("Registered invsee command successfully");
            } else {
                getLogger().warning("Failed to register invsee command - command not found in plugin.yml");
            }

            // Clear chat
            PluginCommand clearChatCmd = getCommand("clearchat");
            if (clearChatCmd != null) {
                ClearChatCommand cmd = new ClearChatCommand(this);
                clearChatCmd.setExecutor(cmd);
                clearChatCmd.setTabCompleter(cmd);
                getLogger().info("Registered clear chat command successfully");
            }


            PluginCommand enderseeCommand = getCommand("endersee");
            if (enderseeCommand != null) {
                enderseeCommand.setExecutor(new EnderseeCommand(this));
                getLogger().info("Registered endersee command successfully");
            }

            // Teleport Command
            PluginCommand teleportCommand = getCommand("stp");
            if (teleportCommand != null) {
                teleportCommand.setExecutor(new StaffTeleportCommand(this));
                getLogger().info("Registered staff teleport command successfully");
            }

            // Mining Alert Commands
            PluginCommand miningAlertsCmd = getCommand("miningalerts");
            if (miningAlertsCmd != null) {
                miningAlertsCmd.setExecutor(new MiningAlertsCommand(this));
                getLogger().info("Registered mining alerts command successfully");
            }

            PluginCommand miningConfigCmd = getCommand("miningconfig");
            if (miningConfigCmd != null) {
                MiningAlertSettingsCommand settingsCommand = new MiningAlertSettingsCommand(this);
                miningConfigCmd.setExecutor(settingsCommand);
                miningConfigCmd.setTabCompleter(settingsCommand);
                getLogger().info("Registered mining config command successfully");
            }

            PluginCommand miningDebugCmd = getCommand("miningdebug");
            if (miningDebugCmd != null) {
                MiningAlertDebugCommand debugCommand = new MiningAlertDebugCommand(this);
                miningDebugCmd.setExecutor(debugCommand);
                miningDebugCmd.setTabCompleter(debugCommand);
                getLogger().info("Registered mining debug command successfully");
            }

            // Restart Command
            PluginCommand restartCommand = getCommand("restart");
            if (restartCommand != null) {
                ServerRestartCommand cmd = new ServerRestartCommand(this);
                restartCommand.setExecutor(cmd);
                restartCommand.setTabCompleter(cmd);
                getLogger().info("Registered restart command successfully");
            }

            // UltimateStaff Command
            PluginCommand ultimateStaffCommand = getCommand("ultimatestaff");
            if (ultimateStaffCommand != null) {
                UltimateStaffCommand cmd = new UltimateStaffCommand(this);
                ultimateStaffCommand.setExecutor(cmd);
                ultimateStaffCommand.setTabCompleter(cmd);
                getLogger().info("Registered ultimatestaff command successfully");
            }

            // AutoMod Command
            PluginCommand autoModCommand = getCommand("automod");
            if (autoModCommand != null) {
                AutoModCommand cmd = new AutoModCommand(this);
                autoModCommand.setExecutor(cmd);
                autoModCommand.setTabCompleter(cmd);
                getLogger().info("Registered automod command successfully");
            } else {
                getLogger().warning("Failed to register automod command - command not found in plugin.yml");
            }

            // Punish Command
            PluginCommand punishCommand = getCommand("punish");
            if (punishCommand != null) {
                PunishCommand cmd = new PunishCommand(this);
                punishCommand.setExecutor(cmd);
                punishCommand.setTabCompleter(cmd);
                getLogger().info("Registered punish command successfully");
            }

            // Death Location Command
            PluginCommand deathLocationCommand = getCommand("deathlocation");
            if (deathLocationCommand != null) {
                DeathLocationCommand cmd = new DeathLocationCommand(this);
                deathLocationCommand.setExecutor(cmd);
                deathLocationCommand.setTabCompleter(cmd);
                getLogger().info("Registered death location command successfully");
            }

            // Restore Death Items Command
            PluginCommand restoreDeathItemsCommand = getCommand("restoredeathitems");
            if (restoreDeathItemsCommand != null) {
                RestoreDeathItemsCommand cmd = new RestoreDeathItemsCommand(this);
                restoreDeathItemsCommand.setExecutor(cmd);
                restoreDeathItemsCommand.setTabCompleter(cmd);
                getLogger().info("Registered restore death items command successfully");
            }

            // Chat Filter Command
            PluginCommand chatFilterCommand = getCommand("chatfilter");
            if (chatFilterCommand != null) {
                ChatFilterCommand cmd = new ChatFilterCommand(this);
                chatFilterCommand.setExecutor(cmd);
                chatFilterCommand.setTabCompleter(cmd);
                getLogger().info("Registered chat filter command successfully");
            }

            // Punishment Debug Command
            PluginCommand punishDebugCommand = getCommand("punishdebug");
            if (punishDebugCommand != null) {
                com.smokypeaks.server.commands.debug.PunishmentDebugCommand cmd = new com.smokypeaks.server.commands.debug.PunishmentDebugCommand(this);
                punishDebugCommand.setExecutor(cmd);
                punishDebugCommand.setTabCompleter(cmd);
                getLogger().info("Registered punishment debug command successfully");
            }

            // X-ray Command
            PluginCommand xrayCommand = getCommand("xray");
            if (xrayCommand != null) {
                com.smokypeaks.server.commands.antixray.XrayCommand cmd = new com.smokypeaks.server.commands.antixray.XrayCommand(this);
                xrayCommand.setExecutor(cmd);
                xrayCommand.setTabCompleter(cmd);
                getLogger().info("Registered xray command successfully");
            }

            // X-ray Learning Command
            PluginCommand xrayLearnCommand = getCommand("xraylearn");
            if (xrayLearnCommand != null) {
                com.smokypeaks.server.commands.antixray.XrayLearningCommand cmd = new com.smokypeaks.server.commands.antixray.XrayLearningCommand(this);
                xrayLearnCommand.setExecutor(cmd);
                xrayLearnCommand.setTabCompleter(cmd);
                getLogger().info("Registered xray learning command successfully");
            }

            // Approve Message Command
            PluginCommand approveMessageCommand = getCommand("approvemessage");
            if (approveMessageCommand != null) {
                com.smokypeaks.server.commands.automod.ApproveMessageCommand cmd = new com.smokypeaks.server.commands.automod.ApproveMessageCommand(this);
                approveMessageCommand.setExecutor(cmd);
                approveMessageCommand.setTabCompleter(cmd);
                getLogger().info("Registered approve message command successfully");
            }

            // Deny Message Command
            PluginCommand denyMessageCommand = getCommand("denymessage");
            if (denyMessageCommand != null) {
                com.smokypeaks.server.commands.automod.DenyMessageCommand cmd = new com.smokypeaks.server.commands.automod.DenyMessageCommand(this);
                denyMessageCommand.setExecutor(cmd);
                denyMessageCommand.setTabCompleter(cmd);
                getLogger().info("Registered deny message command successfully");
            }
        } else {
            getLogger().info("Registering BungeeCord commands...");
            // BungeeCord commands are registered in BungeeMain
        }
    }

    // Getters
    public static Main getInstance() {
        return instance;
    }

    public boolean isBungee() {
        return isBungee;
    }

    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }

    public StaffModeManager getStaffModeManager() {
        return staffModeManager;
    }
    public TeleportManager getTeleportManager() {
        return teleportManager;
    }

    public FreezeManager getFreezeManager() {
        return freezeManager;
    }

    public InventoryManager getInventoryManager() {
        return inventoryManager;
    }

    public MiningAlertManager getMiningAlertManager() {
        return miningAlertManager;
    }

    public boolean hasWorldGuard() {
        return worldGuardEnabled;
    }

    /**
     * Reloads the plugin configuration
     */
    public void reloadPlugin() {
        reloadConfig();
        miningAlertManager.loadSettings();
        getLogger().info("Configuration reloaded");
    }

    /**
     * Checks for and applies any pending plugin updates
     */
    private void checkAndApplyUpdate() {
        File updateMarker = new File(getDataFolder(), "update.marker");
        if (!updateMarker.exists()) {
            return;
        }

        getLogger().info("Found update marker file, applying update...");

        try {
            // Read update information
            String currentJarPath;
            String updateJarPath;

            try (BufferedReader reader = new BufferedReader(new FileReader(updateMarker))) {
                currentJarPath = reader.readLine();
                updateJarPath = reader.readLine();
            }

            File currentJar = new File(currentJarPath);
            File updateJar = new File(updateJarPath);

            if (!updateJar.exists()) {
                getLogger().warning("Update file does not exist: " + updateJarPath);
                updateMarker.delete();
                return;
            }

            // Rename the update jar to the current jar name
            File targetJar = new File(currentJar.getParentFile(), currentJar.getName());

            getLogger().info("Applying update: Replacing " + targetJar.getAbsolutePath() + 
                          " with " + updateJar.getAbsolutePath());

            // Delete the current jar if it exists
            if (targetJar.exists() && !targetJar.delete()) {
                getLogger().warning("Failed to delete current jar file. Update may not be applied correctly.");
            }

            // Move the update jar to the target location
            if (!updateJar.renameTo(targetJar)) {
                getLogger().warning("Failed to rename update jar. Update may not be applied correctly.");
            } else {
                getLogger().info("Successfully applied update!");
            }

            // Clean up the marker file
            updateMarker.delete();

        } catch (Exception e) {
            getLogger().severe("Error applying update: " + e.getMessage());
            if (getConfig().getBoolean("debug", false)) {
                e.printStackTrace();
            }
        }
    }
}

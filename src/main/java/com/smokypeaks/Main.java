// Update Main.java
package com.smokypeaks;

import com.smokypeaks.bungee.discord.DiscordManager;
import com.smokypeaks.global.utils.UpdateChecker;
import com.smokypeaks.server.commands.*;
import com.smokypeaks.server.listeners.*;
import com.smokypeaks.server.managers.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.PluginCommand;


import java.io.File;

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
        getServer().getPluginManager().registerEvents(new StaffToolListener(this), this);
        getServer().getPluginManager().registerEvents(new MiningListener(this), this);
        getServer().getPluginManager().registerEvents(new DeathListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatFilterListener(this), this);
        getServer().getPluginManager().registerEvents(new PunishmentMenuListener(this), this);
        getLogger().info("Registered PunishmentMenuListener");

        // Register AutoMod listeners
        getServer().getPluginManager().registerEvents(new AutoModChatListener(this), this);
        getServer().getPluginManager().registerEvents(new AutoModPlayerListener(this), this);
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
}

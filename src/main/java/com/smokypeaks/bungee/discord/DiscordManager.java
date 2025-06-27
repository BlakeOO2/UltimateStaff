// com.smokypeaks.bungee.discord.DiscordManager.java
package com.smokypeaks.bungee.discord;

import com.smokypeaks.bungee.BungeeMain;
import com.smokypeaks.global.permissions.StaffPermissions;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;

import java.util.EnumSet;

public class DiscordManager extends ListenerAdapter {
    private final BungeeMain plugin;
    private JDA jda;
    private String staffChannelId;
    private String adminChannelId;
    private String discordToMinecraftFormat;
    private String minecraftToDiscordFormat;
    private boolean enabled;

    public DiscordManager(BungeeMain plugin) {
        this.plugin = plugin;
        loadConfig();
        if (enabled) {
            initializeBot();
        }
    }

    private void loadConfig() {
        // Get values directly from the plugin's config
        enabled = plugin.getConfig().getBoolean("discord.enabled", false);
        if (!enabled) {
            plugin.getLogger().info("Discord integration is disabled in config");
            return;
        }

        String token = plugin.getConfig().getString("discord.bot-token", "");
        if (token.isEmpty() || token.equals("your_bot_token_here")) {
            plugin.getLogger().warning("Discord bot token not configured. Discord integration will be disabled.");
            enabled = false;
            return;
        }

        staffChannelId = plugin.getConfig().getString("discord.channels.staff-chat", "");
        adminChannelId = plugin.getConfig().getString("discord.channels.admin-chat", "");

        discordToMinecraftFormat = plugin.getConfig().getString("discord.format.discord-to-minecraft",
                "&9[Discord] &b%username%&f: %message%");
        minecraftToDiscordFormat = plugin.getConfig().getString("discord.format.minecraft-to-discord",
                "**[MC]** %username%: %message%");
    }

    private void initializeBot() {
        try {
            String token = plugin.getConfig().getString("discord.bot-token", "");
            jda = JDABuilder.createDefault(token, EnumSet.of(
                    GatewayIntent.GUILD_MESSAGES,
                    GatewayIntent.MESSAGE_CONTENT
            )).build();

            jda.addEventListener(this);
            jda.awaitReady();
            plugin.getLogger().info("Discord bot connected successfully!");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize Discord bot: " + e.getMessage());
            enabled = false;
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!enabled || event.getAuthor().isBot()) return;

        String channelId = event.getChannel().getId();
        String message = event.getMessage().getContentDisplay();
        String username = event.getAuthor().getName();

        if (channelId.equals(staffChannelId)) {
            // Discord staff chat -> Minecraft
            sendToMinecraftStaffChat(username, message);
        } else if (channelId.equals(adminChannelId)) {
            // Discord admin chat -> Minecraft
            sendToMinecraftAdminChat(username, message);
        }
    }

    private void sendToMinecraftStaffChat(String username, String message) {
        String formatted = discordToMinecraftFormat
                .replace("%username%", username)
                .replace("%message%", message)
                .replace("&", "§");

        // Send to all players with staff chat permission
        for (ProxiedPlayer player : plugin.getProxy().getPlayers()) {
            if (player.hasPermission(StaffPermissions.Staff.CHAT)) {
                player.sendMessage(formatted);
            }
        }

        // Log to console
        plugin.getLogger().info("[Staff Chat] [Discord] " + username + ": " + message);
    }

    private void sendToMinecraftAdminChat(String username, String message) {
        String formatted = discordToMinecraftFormat
                .replace("%username%", username)
                .replace("%message%", message)
                .replace("&", "§");

        // Send to all players with admin chat permission
        for (ProxiedPlayer player : plugin.getProxy().getPlayers()) {
            if (player.hasPermission(StaffPermissions.Admin.CHAT)) {
                player.sendMessage(formatted);
            }
        }

        // Log to console
        plugin.getLogger().info("[Admin Chat] [Discord] " + username + ": " + message);
    }

    public void sendToDiscordStaffChat(String username, String message) {
        if (!enabled || staffChannelId.isEmpty()) return;

        String formatted = minecraftToDiscordFormat
                .replace("%username%", username)
                .replace("%message%", message);

        TextChannel channel = jda.getTextChannelById(staffChannelId);
        if (channel != null) {
            channel.sendMessage(formatted).queue();
        }
    }

    public void sendToDiscordAdminChat(String username, String message) {
        if (!enabled || adminChannelId.isEmpty()) return;

        String formatted = minecraftToDiscordFormat
                .replace("%username%", username)
                .replace("%message%", message);

        TextChannel channel = jda.getTextChannelById(adminChannelId);
        if (channel != null) {
            channel.sendMessage(formatted).queue();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void shutdown() {
        if (jda != null) {
            jda.shutdown();
        }
    }
}

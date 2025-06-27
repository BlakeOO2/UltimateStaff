package com.smokypeaks.bungee.commands;

import com.smokypeaks.Main;
import com.smokypeaks.bungee.BungeeMain;
import com.smokypeaks.global.permissions.StaffPermissions;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class AdminChatCommand extends Command {
    private final BungeeMain plugin;
    private final Set<UUID> toggledPlayers = new HashSet<>();

    public AdminChatCommand(BungeeMain plugin) {
        super("adminchat", StaffPermissions.Admin.CHAT, "ac");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return;
        }

        if (args.length == 0) {
            // Toggle admin chat mode
            if (toggledPlayers.contains(player.getUniqueId())) {
                toggledPlayers.remove(player.getUniqueId());
                player.sendMessage("§c[Admin] §eAdmin chat disabled");
            } else {
                toggledPlayers.add(player.getUniqueId());
                player.sendMessage("§c[Admin] §eAdmin chat enabled");
            }
        } else {
            // Send a one-time message
            String message = String.join(" ", args);
            broadcastAdminMessage(player, message);
        }
    }

    public void broadcastAdminMessage(ProxiedPlayer sender, String message) {
        User user = plugin.getLuckPerms().getUserManager().getUser(sender.getUniqueId());
        if (user == null) return;

        Group primaryGroup = plugin.getLuckPerms().getGroupManager().getGroup(
                user.getPrimaryGroup());
        String groupName = primaryGroup != null ? primaryGroup.getName() : "default";

        // Get format from config
        String format = plugin.getSettingsManager().getFormat("chat.admin", groupName);

        if (plugin.getDiscordManager() != null && plugin.getDiscordManager().isEnabled()) {
            plugin.getDiscordManager().sendToDiscordAdminChat(sender.getName(), message);
        }

        // Replace placeholders
        String serverName = sender.getServer().getInfo().getName();
        String prefix = user.getCachedData().getMetaData().getPrefix();
        prefix = prefix != null ? prefix : "";

        String formattedMessage = format
                .replace("%server%", serverName)
                .replace("%prefix%", prefix)
                .replace("%name%", sender.getName())
                .replace("%message%", message)
                .replace("&", "§");

        // Broadcast to all admin members
        for (ProxiedPlayer player : plugin.getProxy().getPlayers()) {
            if (player.hasPermission(StaffPermissions.Admin.CHAT)) {
                player.sendMessage(formattedMessage);

                // Play sound if enabled
                if (plugin.getSettingsManager().isSoundEnabled(player.getUniqueId(), "admin")) {
                    // Send sound through plugin message
                    String soundEffect = plugin.getSettingsManager().getSoundEffect("chat.admin");
                    float volume = plugin.getSettingsManager().getSoundVolume("chat.admin");
                    float pitch = plugin.getSettingsManager().getSoundPitch("chat.admin");

                    player.getServer().sendData("UltimateStaff",
                            ("PLAY_SOUND:" + soundEffect + ":" + volume + ":" + pitch)
                                    .getBytes());
                }
            }
        }

        // Log to console
        plugin.getLogger().info("[AdminChat] [" + serverName + "] " +
                sender.getName() + ": " + message);
    }

    public boolean isInAdminChat(UUID playerUUID) {
        return toggledPlayers.contains(playerUUID);
    }
}

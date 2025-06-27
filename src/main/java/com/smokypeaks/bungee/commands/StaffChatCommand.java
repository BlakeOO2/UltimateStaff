package com.smokypeaks.bungee.commands;

import com.smokypeaks.bungee.BungeeMain;
import com.smokypeaks.global.permissions.StaffPermissions;
import net.luckperms.api.model.user.User;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class StaffChatCommand extends Command {
    private final BungeeMain plugin;
    private final Set<UUID> toggledPlayers = new HashSet<>();

    public StaffChatCommand(BungeeMain plugin) {
        super("staffchat", StaffPermissions.Staff.CHAT, "sc");
        this.plugin = plugin;
    }



    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return;
        }

        if (args.length == 0) {
            // Toggle staff chat mode
            if (toggledPlayers.contains(player.getUniqueId())) {
                toggledPlayers.remove(player.getUniqueId());
                player.sendMessage("§6[Staff] §eStaff chat disabled");
            } else {
                toggledPlayers.add(player.getUniqueId());
                player.sendMessage("§6[Staff] §eStaff chat enabled");
            }
        } else {
            // Send a one-time message
            String message = String.join(" ", args);
            broadcastStaffMessage(player, message);
        }
    }

    public void broadcastStaffMessage(ProxiedPlayer sender, String message) {
        User user = plugin.getLuckPerms().getUserManager().getUser(sender.getUniqueId());
        if (user == null) return;

        // Get player's group
        String groupName = user.getPrimaryGroup();

        // Get format from config
        String format = plugin.getSettingsManager().getFormat("chat.staff", groupName);


        if (plugin.getDiscordManager() != null && plugin.getDiscordManager().isEnabled()) {
            plugin.getDiscordManager().sendToDiscordStaffChat(sender.getName(), message);
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

        // Broadcast to all staff members
        for (ProxiedPlayer player : plugin.getProxy().getPlayers()) {
            if (player.hasPermission(StaffPermissions.Staff.CHAT)) {
                player.sendMessage(formattedMessage);

                // Play sound if enabled
                if (plugin.getSettingsManager().isSoundEnabled(player.getUniqueId(), "staff")) {
                    String soundEffect = plugin.getSettingsManager().getSoundEffect("chat.staff");
                    float volume = plugin.getSettingsManager().getSoundVolume("chat.staff");
                    float pitch = plugin.getSettingsManager().getSoundPitch("chat.staff");

                    player.getServer().sendData("UltimateStaff",
                            ("PLAY_SOUND:" + soundEffect + ":" + volume + ":" + pitch)
                                    .getBytes());
                }
            }
        }

        // Log to console
        plugin.getLogger().info("[StaffChat] [" + serverName + "] " +
                sender.getName() + ": " + message);
    }

    public boolean isInStaffChat(UUID playerUUID) {
        return toggledPlayers.contains(playerUUID);
    }
}

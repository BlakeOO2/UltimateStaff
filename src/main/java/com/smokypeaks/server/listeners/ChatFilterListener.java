package com.smokypeaks.server.listeners;

import com.smokypeaks.Main;
import com.smokypeaks.global.permissions.StaffPermissions;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatFilterListener implements Listener {
    private final Main plugin;

    public ChatFilterListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        // Skip filtering for staff with bypass permission
        if (player.hasPermission(StaffPermissions.Staff.BYPASS_FILTER)) {
            return;
        }

        String originalMessage = event.getMessage();
        String filteredMessage = plugin.getChatFilterManager().filterMessage(originalMessage);

        // If message was filtered, update it
        if (!originalMessage.equals(filteredMessage)) {
            event.setMessage(filteredMessage);

            // Notify staff of filtered message
            notifyStaff(player, originalMessage, filteredMessage);
        }
    }

    /**
     * Notifies staff that a message was filtered
     * @param player The player who sent the message
     * @param original The original message
     * @param filtered The filtered message
     */
    private void notifyStaff(Player player, String original, String filtered) {
        String notification = "§6[Chat Filter] §e" + player.getName() + 
                " tried to say: §c" + original + " §e(filtered to: §7" + filtered + "§e)";

        plugin.getServer().getOnlinePlayers().forEach(p -> {
            if (p.hasPermission(StaffPermissions.Staff.FILTER_ALERTS)) {
                p.sendMessage(notification);
            }
        });

        // Log to console
        plugin.getLogger().info("[Chat Filter] " + player.getName() + 
                " tried to say: " + original + " (filtered to: " + filtered + ")");
    }
}

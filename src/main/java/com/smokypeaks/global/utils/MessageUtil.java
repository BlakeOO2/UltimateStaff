// com.smokypeaks.global.utils.MessageUtil.java
package com.smokypeaks.global.utils;

import com.smokypeaks.Main;
import com.smokypeaks.global.permissions.StaffPermissions;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import org.bukkit.Bukkit;

public class MessageUtil {
    public static void notifyAdmins(Main plugin, String message) {
        if (plugin.isBungee()) {
            // Bungee notification
            ProxyServer.getInstance().getPlayers().stream()
                    .filter(player -> player.hasPermission(StaffPermissions.Admin.UPDATE_NOTIFY))
                    .forEach(player -> player.sendMessage(
                            net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('§', message)
                    ));
        } else {
            // Bukkit notification
            Bukkit.getOnlinePlayers().stream()
                    .filter(player -> player.hasPermission(StaffPermissions.Admin.UPDATE_NOTIFY))
                    .forEach(player -> player.sendMessage(
                            org.bukkit.ChatColor.translateAlternateColorCodes('§', message)
                    ));
        }
        // Also log to console
        plugin.getLogger().info(ChatColor.stripColor(message));
    }
}

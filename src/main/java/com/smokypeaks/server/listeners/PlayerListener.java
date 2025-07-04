// com.smokypeaks.server.listeners.PlayerListener.java
package com.smokypeaks.server.listeners;

import com.smokypeaks.Main;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class PlayerListener implements Listener {
    private final Main plugin;

    public PlayerListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Schedule a delayed task to save the player's inventory after they've fully loaded
        new BukkitRunnable() {
            @Override
            public void run() {
                Player player = event.getPlayer();
                if (player != null && player.isOnline()) {
                    plugin.getInventoryManager().savePlayerData(player);
                    plugin.getLogger().info("Saved initial inventory data for " + player.getName());
                }
            }
        }.runTaskLater(plugin, 40L); // 2-second delay (40 ticks)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getInventoryManager().savePlayerData(player);
        plugin.getLogger().info("Saved inventory data for " + player.getName() + " on disconnect");
    }
}

// com.smokypeaks.server.listeners.PlayerListener.java
package com.smokypeaks.server.listeners;

import com.smokypeaks.Main;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {
    private final Main plugin;

    public PlayerListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getInventoryManager().savePlayerData(event.getPlayer());
    }
}

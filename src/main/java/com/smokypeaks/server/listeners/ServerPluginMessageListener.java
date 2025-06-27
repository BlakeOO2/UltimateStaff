// com.smokypeaks.server.listeners.PluginMessageListener.java
package com.smokypeaks.server.listeners;

import com.smokypeaks.Main;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

public class ServerPluginMessageListener implements PluginMessageListener {
    private final Main plugin;

    public ServerPluginMessageListener(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("ultimatestaff:main")) return;

        String[] data = new String(message).split(":");
        if (data[0].equals("PLAY_SOUND")) {
            try {
                Sound sound = Sound.valueOf(data[1]);
                float volume = Float.parseFloat(data[2]);
                float pitch = Float.parseFloat(data[3]);
                player.playSound(player.getLocation(), sound, volume, pitch);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to play sound: " + e.getMessage());
            }
        }
    }
}

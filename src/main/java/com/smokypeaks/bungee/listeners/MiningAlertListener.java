// com.smokypeaks.bungee.listeners.MiningAlertListener.java
package com.smokypeaks.bungee.listeners;

import com.smokypeaks.bungee.BungeeMain;
import com.smokypeaks.global.permissions.StaffPermissions;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

public class MiningAlertListener implements Listener {
    private final BungeeMain plugin;

    public MiningAlertListener(BungeeMain plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getTag().equals("UltimateStaff")) return;

        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(event.getData()));
            String subChannel = in.readUTF();

            if (subChannel.equals("MiningAlert")) {
                String playerName = in.readUTF();
                String type = in.readUTF();
                String message = in.readUTF();

                // Broadcast to all staff across the network
                for (ProxiedPlayer staff : plugin.getProxy().getPlayers()) {
                    if (staff.hasPermission(StaffPermissions.Staff.MINING_ALERTS)) {
                        staff.sendMessage(message);
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error handling mining alert: " + e.getMessage());
        }
    }
}

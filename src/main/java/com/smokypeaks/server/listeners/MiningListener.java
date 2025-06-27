package com.smokypeaks.server.listeners;

import com.smokypeaks.Main;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class MiningListener implements Listener {
    private final Main plugin;

    public MiningListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Material type = block.getType();

        // Check if this is a monitored ore type
        if (isMonitoredOre(type)) {
            Player player = event.getPlayer();

            // Don't trigger alerts for staff in staff mode
            if (plugin.getStaffModeManager().isInStaffMode(player)) {
                return;
            }

            // Handle the mining event locally
            plugin.getMiningAlertManager().handleBlockMine(player, type, block.getLocation());

            // Send cross-server notification
            sendCrossServerNotification(player, type, block.getLocation());
        }
    }

    private void sendCrossServerNotification(Player player, Material type, org.bukkit.Location location) {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(stream);

            out.writeUTF("MiningAlert");
            out.writeUTF(player.getName());
            out.writeUTF(type.name());
            out.writeUTF(String.format("%s,%d,%d,%d",
                    location.getWorld().getName(),
                    location.getBlockX(),
                    location.getBlockY(),
                    location.getBlockZ()));

            player.sendPluginMessage(plugin, "ultimatestaff:main", stream.toByteArray());
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to send mining alert: " + e.getMessage());
        }
    }

    private boolean isMonitoredOre(Material type) {
        return switch (type) {
            case DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE, ANCIENT_DEBRIS -> true;
            default -> false;
        };
    }
}

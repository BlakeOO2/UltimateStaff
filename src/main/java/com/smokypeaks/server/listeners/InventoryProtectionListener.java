// com.smokypeaks.server.listeners.InventoryProtectionListener.java
package com.smokypeaks.server.listeners;

import com.smokypeaks.Main;
import com.smokypeaks.global.permissions.StaffPermissions;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class InventoryProtectionListener implements Listener {
    private final Main plugin;

    public InventoryProtectionListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player staff)) return;

        // Check if this is a staff inventory view
        if (plugin.getInventoryManager().isViewingInventory(staff)) {
            Player target = plugin.getInventoryManager().getViewTarget(staff);

            // Allow modification only for admins with the modify permission
            if (!staff.hasPermission(StaffPermissions.Admin.INVENTORY_MODIFY)) {
                event.setCancelled(true);
                staff.sendMessage("§c[Staff] §eYou don't have permission to modify inventories!");
                return;
            }

            // Log the modification
            if (!event.isCancelled()) {
                logInventoryModification(staff, target, event.getCurrentItem(), event.getCursor(), "click");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player staff)) return;

        if (plugin.getInventoryManager().isViewingInventory(staff)) {
            if (!staff.hasPermission(StaffPermissions.Admin.INVENTORY_MODIFY)) {
                event.setCancelled(true);
                staff.sendMessage("§c[Staff] §eYou don't have permission to modify inventories!");
                return;
            }

            Player target = plugin.getInventoryManager().getViewTarget(staff);
            if (!event.isCancelled()) {
                logInventoryModification(staff, target, null, event.getOldCursor(), "drag");
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player staff)) return;

        if (plugin.getInventoryManager().isViewingInventory(staff)) {
            Player target = plugin.getInventoryManager().getViewTarget(staff);
            if (target != null) {
                // Save the inventory state for offline players
                plugin.getInventoryManager().savePlayerData(target);
            }
        }
    }

    private void logInventoryModification(Player staff, Player target,
                                          org.bukkit.inventory.ItemStack removed,
                                          org.bukkit.inventory.ItemStack added,
                                          String action) {
        String message = String.format(
                "§6[Staff] §e%s modified %s's inventory (%s): Removed: %s, Added: %s",
                staff.getName(),
                target.getName(),
                action,
                removed != null ? removed.getType().toString() : "nothing",
                added != null ? added.getType().toString() : "nothing"
        );

        // Log to console and notify staff
        plugin.getLogger().info(message);
        plugin.getServer().getOnlinePlayers().stream()
                .filter(p -> p.hasPermission(StaffPermissions.Staff.INVSEE_NOTIFY))
                .forEach(p -> p.sendMessage(message));
    }
}

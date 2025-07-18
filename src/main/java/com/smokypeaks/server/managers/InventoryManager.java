// Update com.smokypeaks.server.managers.InventoryManager.java
package com.smokypeaks.server.managers;

import com.smokypeaks.Main;
import com.smokypeaks.server.storage.InventoryStorage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.UUID;

public class InventoryManager {
    private final Main plugin;
    private final HashMap<UUID, UUID> inventoryViewers = new HashMap<>();
    private final InventoryStorage storage;

    public InventoryManager(Main plugin) {
        this.plugin = plugin;
        this.storage = new InventoryStorage(plugin);
    }

    public void openInventory(Player staff, Player target) {
        inventoryViewers.put(staff.getUniqueId(), target.getUniqueId());

        // Create a copy of the inventory with a valid size
        Inventory copy = Bukkit.createInventory(null, 36, "§8" + target.getName() + "'s Inventory");

        // Copy main inventory contents (36 slots)
        ItemStack[] contents = target.getInventory().getContents();
        for (int i = 0; i < Math.min(36, contents.length); i++) {
            if (contents[i] != null) {
                copy.setItem(i, contents[i].clone());
            }
        }

        staff.openInventory(copy);
    }

    public void openEnderChest(Player staff, Player target) {
        staff.openInventory(target.getEnderChest());
    }

    public void openOfflineInventory(Player staff, OfflinePlayer target) {
        // Log the attempt to access offline inventory
        plugin.getLogger().info("Staff member " + staff.getName() + " is accessing offline inventory of " + target.getName() + " (UUID: " + target.getUniqueId() + ")");

        // Add a debug message to check if the player has played before
        if (!target.hasPlayedBefore()) {
            staff.sendMessage("§c[Staff] §eWarning: Player " + target.getName() + " has never played on this server before.");
            plugin.getLogger().warning("Attempted to access inventory of a player who hasn't played before: " + target.getName());
        }

        ItemStack[] inventory = storage.loadInventory(target.getUniqueId());
        if (inventory == null) {
            staff.sendMessage("§c[Staff] §eNo saved inventory found for " + target.getName() + ". They may need to reconnect for data to be saved.");
            plugin.getLogger().warning("Failed to load offline inventory for " + target.getName() + " - no data found");
            return;
        }

        Inventory view = Bukkit.createInventory(null, 36,
                "§8" + target.getName() + "'s Inventory");
        view.setContents(inventory);
        staff.openInventory(view);

        // Notify staff with permission about this access
        notifyStaff(staff.getName() + " is viewing the offline inventory of " + target.getName());
    }

    /**
     * Notifies staff members with the notification permission
     * @param message The message to send to staff
     */
    private void notifyStaff(String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("ultimatestaff.staff.invsee.notify")) {
                player.sendMessage("§b[Staff] §f" + message);
            }
        }
        plugin.getLogger().info("[StaffNotification] " + message);
    }

    public void openOfflineEnderChest(Player staff, OfflinePlayer target) {
        ItemStack[] enderChest = storage.loadEnderChest(target.getUniqueId());
        if (enderChest == null) {
            staff.sendMessage("§c[Staff] §eNo saved enderchest found for that player!");
            return;
        }

        Inventory view = Bukkit.createInventory(null, 27,
                "§8" + target.getName() + "'s Enderchest");
        view.setContents(enderChest);
        staff.openInventory(view);
    }

    public void savePlayerData(Player player) {
        if (player == null) {
            plugin.getLogger().warning("Attempted to save inventory data for null player");
            return;
        }

        try {
            storage.saveInventory(player.getUniqueId(),
                    player.getInventory().getContents(),
                    player.getEnderChest().getContents());

            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("Saved inventory data for " + player.getName() + 
                    " (" + player.getUniqueId() + ")");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error saving inventory data for " + player.getName() + ": " + e.getMessage());
            if (plugin.getConfig().getBoolean("debug", false)) {
                e.printStackTrace();
            }
        }
    }

    public boolean isViewingInventory(Player staff) {
        return inventoryViewers.containsKey(staff.getUniqueId());
    }

    public Player getViewTarget(Player staff) {
        UUID targetUUID = inventoryViewers.get(staff.getUniqueId());
        return targetUUID != null ? Bukkit.getPlayer(targetUUID) : null;
    }

    public void cleanup() {
        inventoryViewers.clear();
        storage.cleanup();
    }
}

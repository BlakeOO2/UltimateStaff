// com.smokypeaks.server.listeners.StaffToolListener.java
package com.smokypeaks.server.listeners;

import com.smokypeaks.Main;
import com.smokypeaks.global.permissions.StaffPermissions;
import com.smokypeaks.server.utils.StaffItems;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class StaffToolListener implements Listener {
    private final Main plugin;

    public StaffToolListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!plugin.getStaffModeManager().isInStaffMode(player)) return;

        // Prevent moving staff tools
        if (isStaffTool(event.getCurrentItem()) || isStaffTool(event.getCursor())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getStaffModeManager().isInStaffMode(player)) return;

        // Prevent dropping staff tools
        if (isStaffTool(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getStaffModeManager().isInStaffMode(player)) return;

        ItemStack item = event.getItem();
        if (item == null) return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            // Vanish Toggle
            if (StaffItems.isVanishTool(item)) {
                event.setCancelled(true);
                plugin.getStaffModeManager().toggleVanish(player);
            }
            // Random Teleport
            else if (StaffItems.isRandomTpTool(item)) {
                event.setCancelled(true);
                plugin.getTeleportManager().teleportToRandomPlayer(player);
            }
            // Staff List
            else if (StaffItems.isStaffListTool(item)) {
                event.setCancelled(true);
                showOnlineStaff(player);
            }
            // Punishment Menu
            else if (StaffItems.isPunishmentTool(item)) {
                event.setCancelled(true);
                openPunishmentMenu(player);
            }
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getStaffModeManager().isInStaffMode(player)) return;
        if (!(event.getRightClicked() instanceof Player target)) return;

        ItemStack item = player.getInventory().getItemInMainHand();

        // Freeze Player
        if (StaffItems.isFreezeTool(item)) {
            event.setCancelled(true);
            plugin.getFreezeManager().toggleFreeze(target, player);
        }
        // Inventory Inspection
        else if (StaffItems.isInvseeTool(item)) {
            event.setCancelled(true);
            plugin.getInventoryManager().openInventory(player, target);
        }
        // Enderchest Inspection
        else if (StaffItems.isEnderseeTool(item)) {
            event.setCancelled(true);
            plugin.getInventoryManager().openEnderChest(player, target);
        }
        // Target for punishment
        else if (StaffItems.isPunishmentTool(item)) {
            event.setCancelled(true);
            openPunishmentMenu(player, target);
        }
    }

    private boolean isStaffTool(ItemStack item) {
        if (item == null) return false;
        return StaffItems.isStaffTool(item);
    }

    private void showOnlineStaff(Player player) {
        player.sendMessage("§6=== Online Staff ===");
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (online.hasPermission(StaffPermissions.Staff.MODE)) {
                boolean inStaffMode = plugin.getStaffModeManager().isInStaffMode(online);
                boolean vanished = plugin.getStaffModeManager().isVanished(online);
                player.sendMessage(String.format("§e%s %s %s",
                        online.getName(),
                        inStaffMode ? "§a[Staff Mode]" : "§c[Normal]",
                        vanished ? "§7[Vanished]" : ""
                ));
            }
        }
    }

    private void openPunishmentMenu(Player staff) {
        // Open punishment menu without target
        plugin.getPunishmentManager().openMenu(staff, null);
    }

    private void openPunishmentMenu(Player staff, Player target) {
        // Open punishment menu with target
        plugin.getPunishmentManager().openMenu(staff, target);
    }
}

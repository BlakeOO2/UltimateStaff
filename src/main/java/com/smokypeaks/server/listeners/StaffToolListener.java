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
    private final java.util.Map<java.util.UUID, Long> lastActionTime = new java.util.HashMap<>();
    private static final long TOGGLE_COOLDOWN_MS = 500; // 500ms cooldown between any staff actions

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

                // Check cooldown to prevent double-toggling
                if (checkAndUpdateCooldown(player)) {
                    plugin.getStaffModeManager().toggleVanish(player);
                }
            }
            // Spectator Mode Toggle
            else if (StaffItems.isSpectatorTool(item)) {
                event.setCancelled(true);

                // Check cooldown to prevent double-toggling
                if (checkAndUpdateCooldown(player)) {
                    plugin.getStaffModeManager().toggleSpectatorMode(player);
                }
            }
            // Random Teleport
            else if (StaffItems.isRandomTpTool(item)) {
                event.setCancelled(true);
                if (checkAndUpdateCooldown(player)) {
                    plugin.getTeleportManager().teleportToRandomPlayer(player);
                }
            }
            // Staff List
            else if (StaffItems.isStaffListTool(item)) {
                event.setCancelled(true);
                if (checkAndUpdateCooldown(player)) {
                    showOnlineStaff(player);
                }
            }
            // Punishment Menu
            else if (StaffItems.isPunishmentTool(item)) {
                event.setCancelled(true);
                if (checkAndUpdateCooldown(player)) {
                    openPunishmentMenu(player);
                }
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
            if (checkAndUpdateCooldown(player)) {
                plugin.getFreezeManager().toggleFreeze(target, player);
            }
        }
        // Inventory Inspection
        else if (StaffItems.isInvseeTool(item)) {
            event.setCancelled(true);
            if (checkAndUpdateCooldown(player)) {
                try {
                    plugin.getInventoryManager().openInventory(player, target);
                } catch (Exception e) {
                    player.sendMessage("§c[Staff] §eError opening inventory: " + e.getMessage());
                    plugin.getLogger().warning("Error opening inventory: " + e.getMessage());
                }
            }
        }
        // Enderchest Inspection
        else if (StaffItems.isEnderseeTool(item)) {
            event.setCancelled(true);
            if (checkAndUpdateCooldown(player)) {
                plugin.getInventoryManager().openEnderChest(player, target);
            }
        }
        // Target for punishment
        else if (StaffItems.isPunishmentTool(item)) {
            event.setCancelled(true);
            if (checkAndUpdateCooldown(player)) {
                openPunishmentMenu(player, target);
            }
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
        // Instead of passing null, we'll show a list of online players
        staff.sendMessage("§6[Staff] §ePlease use §f/punish <player> §eto open the punishment menu.");
        staff.sendMessage("§6[Staff] §eOnline players:");

        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (!online.equals(staff)) {
                staff.sendMessage("§7- §f" + online.getName());
            }
        }
    }

    private void openPunishmentMenu(Player staff, Player target) {
        // Open punishment menu with target
        plugin.getPunishmentManager().openMenu(staff, target);
    }

    /**
     * Check if enough time has passed since the last action and update the timestamp
     * @param player The player to check cooldown for
     * @return true if action should proceed, false if on cooldown
     */
    private boolean checkAndUpdateCooldown(Player player) {
        long currentTime = System.currentTimeMillis();
        java.util.UUID playerUUID = player.getUniqueId();
        Long lastAction = lastActionTime.getOrDefault(playerUUID, 0L);

        if (currentTime - lastAction >= TOGGLE_COOLDOWN_MS) {
            lastActionTime.put(playerUUID, currentTime);
            return true;
        }
        return false;
    }
}

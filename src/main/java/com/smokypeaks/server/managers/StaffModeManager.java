// com.smokypeaks.server.managers.StaffModeManager.java
package com.smokypeaks.server.managers;

import com.smokypeaks.Main;
import com.smokypeaks.global.permissions.StaffPermissions;
import com.smokypeaks.server.utils.StaffItems;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

public class StaffModeManager {
    private final Main plugin;
    private final HashSet<UUID> staffMode = new HashSet<>();
    private final HashSet<UUID> vanished = new HashSet<>();
    private final Map<UUID, ItemStack[]> savedInventories = new HashMap<>();
    private final Map<UUID, ItemStack[]> savedArmor = new HashMap<>();
    private final Map<UUID, GameMode> savedGameModes = new HashMap<>();

    public StaffModeManager(Main plugin) {
        this.plugin = plugin;
    }

    public void toggleStaffMode(Player player) {
        if (isInStaffMode(player)) {
            disableStaffMode(player);
        } else {
            enableStaffMode(player);
        }

    }
    public void toggleVanish(Player player) {
        setVanished(player, !isVanished(player));
    }

    public void enableStaffMode(Player player) {
        UUID uuid = player.getUniqueId();

        // Save current state
        savedInventories.put(uuid, player.getInventory().getContents());
        savedArmor.put(uuid, player.getInventory().getArmorContents());
        savedGameModes.put(uuid, player.getGameMode());

        // Clear inventory
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);

        // Set gamemode
        player.setGameMode(GameMode.CREATIVE);

        // Give staff tools
        giveStaffTools(player);

        // Enable vanish by default
        setVanished(player, true);

        // Add to staff mode set
        staffMode.add(uuid);

        // Send message
        player.sendMessage("§6[Staff] §eStaff mode enabled");
        plugin.getLogger().info("Staff mode enabled for " + player.getName());
    }

    public void disableStaffMode(Player player) {
        UUID uuid = player.getUniqueId();

        // Disable vanish
        setVanished(player, false);

        // Restore inventory
        if (savedInventories.containsKey(uuid)) {
            player.getInventory().setContents(savedInventories.get(uuid));
            savedInventories.remove(uuid);
        }

        if (savedArmor.containsKey(uuid)) {
            player.getInventory().setArmorContents(savedArmor.get(uuid));
            savedArmor.remove(uuid);
        }

        // Restore gamemode
        if (savedGameModes.containsKey(uuid)) {
            player.setGameMode(savedGameModes.get(uuid));
            savedGameModes.remove(uuid);
        }

        // Remove from staff mode set
        staffMode.remove(uuid);

        // Send message
        player.sendMessage("§6[Staff] §eStaff mode disabled");
        plugin.getLogger().info("Staff mode disabled for " + player.getName());
    }

    private void giveStaffTools(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);

        // Set staff items
        player.getInventory().setItem(0, StaffItems.createVanishItem(isVanished(player)));
        player.getInventory().setItem(1, StaffItems.createFreezeTool());
        player.getInventory().setItem(2, StaffItems.createInvseeItem());
        player.getInventory().setItem(3, StaffItems.createEnderChestItem());
        player.getInventory().setItem(4, StaffItems.createRandomTpItem());
        player.getInventory().setItem(8, StaffItems.createStaffListItem());
        player.getInventory().setItem(7, StaffItems.createPunishmentTool());

        player.updateInventory();
    }

    public void setVanished(Player player, boolean vanish) {
        if (vanish) {
            vanished.add(player.getUniqueId());
            for (Player online : plugin.getServer().getOnlinePlayers()) {
                if (!online.hasPermission(StaffPermissions.Staff.VANISH)) {
                    online.hidePlayer(plugin, player);
                }
            }
            player.sendMessage("§6[Staff] §eVanish enabled");
        } else {
            vanished.remove(player.getUniqueId());
            for (Player online : plugin.getServer().getOnlinePlayers()) {
                online.showPlayer(plugin, player);
            }
            player.sendMessage("§6[Staff] §eVanish disabled");
        }

        // Update vanish item if in staff mode
        if (isInStaffMode(player)) {
            player.getInventory().setItem(0, StaffItems.createVanishItem(vanish));
            player.updateInventory();
        }
    }

    public boolean isInStaffMode(Player player) {
        return staffMode.contains(player.getUniqueId());
    }

    public boolean isVanished(Player player) {
        return vanished.contains(player.getUniqueId());
    }

    public void cleanup() {
        // Disable staff mode for all online players
        for (UUID uuid : new HashSet<>(staffMode)) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                disableStaffMode(player);
            }
        }

        // Clear all sets and maps
        staffMode.clear();
        vanished.clear();
        savedInventories.clear();
        savedArmor.clear();
        savedGameModes.clear();
    }
}
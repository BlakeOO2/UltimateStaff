package com.smokypeaks.server.managers;

import com.smokypeaks.Main;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DeathManager {
    private final Main plugin;
    private final Map<UUID, Location> deathLocations = new HashMap<>();
    private final Map<UUID, ItemStack[]> deathItems = new HashMap<>();

    public DeathManager(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Records a player's death location and optionally their inventory contents
     * @param player The player who died
     * @param items The items the player had when they died (can be null)
     */
    public void recordDeath(Player player, ItemStack[] items) {
        UUID playerUuid = player.getUniqueId();
        deathLocations.put(playerUuid, player.getLocation().clone());

        if (items != null && items.length > 0) {
            deathItems.put(playerUuid, items.clone());
        }

        plugin.getLogger().info("Recorded death for " + player.getName() + 
                " at " + formatLocation(player.getLocation()));
    }

    /**
     * Gets the last death location for a player
     * @param playerUuid The UUID of the player
     * @return The player's last death location, or null if not found
     */
    public Location getDeathLocation(UUID playerUuid) {
        return deathLocations.get(playerUuid);
    }

    /**
     * Gets the last death items for a player
     * @param playerUuid The UUID of the player
     * @return The player's last death items, or null if not found
     */
    public ItemStack[] getDeathItems(UUID playerUuid) {
        ItemStack[] items = deathItems.get(playerUuid);
        if (items != null) {
            return items.clone(); // Return a clone to prevent modification
        }
        return null;
    }

    /**
     * Clears the death items for a player
     * @param playerUuid The UUID of the player
     */
    public void clearDeathItems(UUID playerUuid) {
        deathItems.remove(playerUuid);
    }

    /**
     * Checks if a player has recorded death items
     * @param playerUuid The UUID of the player
     * @return True if death items exist, false otherwise
     */
    public boolean hasDeathItems(UUID playerUuid) {
        return deathItems.containsKey(playerUuid);
    }

    /**
     * Format a location for display
     * @param loc The location to format
     * @return A formatted string representation of the location
     */
    private String formatLocation(Location loc) {
        return String.format("%s: %.1f, %.1f, %.1f", 
                loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ());
    }

    /**
     * Cleanup method to be called on plugin disable
     */
    public void cleanup() {
        deathLocations.clear();
        deathItems.clear();
    }
}

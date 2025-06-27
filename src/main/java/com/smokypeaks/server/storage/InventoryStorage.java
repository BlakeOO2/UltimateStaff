// com.smokypeaks.server.storage.InventoryStorage.java
package com.smokypeaks.server.storage;

import com.smokypeaks.Main;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class InventoryStorage {
    private final Main plugin;
    private final File storageDir;

    public InventoryStorage(Main plugin) {
        this.plugin = plugin;
        this.storageDir = new File(plugin.getDataFolder(), "inventories");
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
    }

    public void saveInventory(UUID playerUUID, ItemStack[] inventory, ItemStack[] enderChest) {
        File playerFile = new File(storageDir, playerUUID.toString() + ".yml");
        YamlConfiguration config = new YamlConfiguration();

        config.set("inventory", inventory);
        config.set("enderchest", enderChest);
        config.set("last-updated", System.currentTimeMillis());

        try {
            config.save(playerFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save inventory for " + playerUUID + ": " + e.getMessage());
        }
    }

    public ItemStack[] loadInventory(UUID playerUUID) {
        File playerFile = new File(storageDir, playerUUID.toString() + ".yml");
        if (!playerFile.exists()) return null;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        return ((config.get("inventory") instanceof ItemStack[]) ?
                (ItemStack[]) config.get("inventory") : null);
    }

    public ItemStack[] loadEnderChest(UUID playerUUID) {
        File playerFile = new File(storageDir, playerUUID.toString() + ".yml");
        if (!playerFile.exists()) return null;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        return ((config.get("enderchest") instanceof ItemStack[]) ?
                (ItemStack[]) config.get("enderchest") : null);
    }

    public void cleanup() {
        // Delete files older than 7 days
        long maxAge = 7 * 24 * 60 * 60 * 1000L; // 7 days in milliseconds
        long now = System.currentTimeMillis();

        File[] files = storageDir.listFiles();
        if (files != null) {
            for (File file : files) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                long lastUpdated = config.getLong("last-updated", 0);
                if (now - lastUpdated > maxAge) {
                    file.delete();
                }
            }
        }
    }
}

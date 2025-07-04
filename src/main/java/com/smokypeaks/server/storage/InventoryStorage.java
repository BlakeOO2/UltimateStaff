// com.smokypeaks.server.storage.InventoryStorage.java
package com.smokypeaks.server.storage;

import com.smokypeaks.Main;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.List;
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
        if (playerUUID == null) {
            plugin.getLogger().warning("Attempted to save inventory with null UUID");
            return;
        }

        if (inventory == null) {
            plugin.getLogger().warning("Attempted to save null inventory for " + playerUUID);
            return;
        }

        // Create directory if it doesn't exist
        if (!storageDir.exists() && !storageDir.mkdirs()) {
            plugin.getLogger().severe("Failed to create inventory storage directory: " + storageDir.getAbsolutePath());
            return;
        }

        File playerFile = new File(storageDir, playerUUID.toString() + ".yml");
        YamlConfiguration config = new YamlConfiguration();

        // Store inventory details for debugging
        config.set("inventory", inventory);
        config.set("enderchest", enderChest != null ? enderChest : new ItemStack[0]);
        config.set("last-updated", System.currentTimeMillis());
        config.set("inventory-size", inventory.length);

        try {
            config.save(playerFile);
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("Successfully saved inventory data for " + playerUUID + 
                    " to " + playerFile.getAbsolutePath());
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save inventory for " + playerUUID + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public ItemStack[] loadInventory(UUID playerUUID) {
        File playerFile = new File(storageDir, playerUUID.toString() + ".yml");
        if (!playerFile.exists()) {
            plugin.getLogger().warning("No inventory file found for UUID: " + playerUUID);
            return null;
        }

        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
            Object invObj = config.get("inventory");

            if (invObj == null) {
                plugin.getLogger().warning("Inventory section is null for UUID: " + playerUUID);
                return null;
            }

            // Handle different types of inventory storage
            if (invObj instanceof ItemStack[]) {
                return (ItemStack[]) invObj;
            } else if (invObj instanceof List<?>) {
                List<?> list = (List<?>) invObj;
                ItemStack[] inventory = new ItemStack[36]; // Standard inventory size

                for (int i = 0; i < list.size() && i < inventory.length; i++) {
                    Object item = list.get(i);
                    if (item instanceof ItemStack) {
                        inventory[i] = (ItemStack) item;
                    }
                }

                plugin.getLogger().info("Successfully converted ArrayList to ItemStack[] for UUID: " + playerUUID);
                return inventory;
            } else {
                plugin.getLogger().warning("Inventory is not a compatible type for UUID: " + playerUUID + 
                    " (found " + invObj.getClass().getName() + " instead)");
                return null;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error loading inventory for UUID: " + playerUUID + ": " + e.getMessage());
            if (plugin.getConfig().getBoolean("debug", false)) {
                e.printStackTrace();
            }
            return null;
        }
    }

    public ItemStack[] loadEnderChest(UUID playerUUID) {
        File playerFile = new File(storageDir, playerUUID.toString() + ".yml");
        if (!playerFile.exists()) {
            plugin.getLogger().warning("No inventory file found for UUID: " + playerUUID);
            return null;
        }

        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
            Object enderObj = config.get("enderchest");

            if (enderObj == null) {
                plugin.getLogger().warning("Enderchest section is null for UUID: " + playerUUID);
                return null;
            }

            // Handle different types of inventory storage
            if (enderObj instanceof ItemStack[]) {
                return (ItemStack[]) enderObj;
            } else if (enderObj instanceof List<?>) {
                List<?> list = (List<?>) enderObj;
                ItemStack[] enderChest = new ItemStack[27]; // Standard enderchest size

                for (int i = 0; i < list.size() && i < enderChest.length; i++) {
                    Object item = list.get(i);
                    if (item instanceof ItemStack) {
                        enderChest[i] = (ItemStack) item;
                    }
                }

                plugin.getLogger().info("Successfully converted ArrayList to ItemStack[] for enderchest UUID: " + playerUUID);
                return enderChest;
            } else {
                plugin.getLogger().warning("Enderchest is not a compatible type for UUID: " + playerUUID + 
                    " (found " + enderObj.getClass().getName() + " instead)");
                return null;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error loading enderchest for UUID: " + playerUUID + ": " + e.getMessage());
            if (plugin.getConfig().getBoolean("debug", false)) {
                e.printStackTrace();
            }
            return null;
        }
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

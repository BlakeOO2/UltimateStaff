package com.smokypeaks.server.listeners;

import com.smokypeaks.Main;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class PunishmentMenuListener implements Listener {
    private static final String MENU_PREFIX = "§c§l";
    private final Main plugin;

    public PunishmentMenuListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();

        // Check if this is one of our punishment menus
        if (!title.startsWith(MENU_PREFIX)) return;

        // Cancel the event to prevent item movement
        event.setCancelled(true);

        // Get the clicked item
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        // Log the click for debugging
        plugin.getLogger().info("Punishment menu clicked: " + title + ", item: " + item.getType());

        // Extract information from the item
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;

        String itemName = item.getItemMeta().getDisplayName();
        String categoryKey = null;
        String violationKey = null;
        String actionName = null;

        // Extract both category and violation keys from lore if present
        if (item.getItemMeta().hasLore()) {
            List<String> lore = item.getItemMeta().getLore();
            if (lore != null) {
                for (String line : lore) {
                    if (line.startsWith("§8Category:")) {
                        categoryKey = line.substring("§8Category: ".length());
                    } else if (line.startsWith("§8Violation:")) {
                        violationKey = line.substring("§8Violation: ".length());
                    } else if (line.startsWith("§8Action:")) {
                        actionName = line.substring("§8Action: ".length());
                    }
                }
            }

            // Log what we found for debugging
            plugin.getLogger().info("Found in lore - category: " + categoryKey + ", violation: " + violationKey + ", action: " + actionName);
        }

        // Handle main punishment menu clicks
        if (title.contains("Punishment Menu")) {
            if (categoryKey != null) {
                plugin.getLogger().info("Opening category: " + categoryKey);
                plugin.getPunishmentManager().openCategory(player, categoryKey);
            }
        }
        // Handle category menu clicks
        else if (title.contains(" - ") && !title.contains("Confirm")) {
            // Check if this is the back button
            if (itemName.equals("§c§lBack")) {
                // Return to main menu
                String targetName = title.substring(title.indexOf(" - ") + 3);
                Player target = plugin.getServer().getPlayer(targetName);
                if (target != null) {
                    plugin.getPunishmentManager().openMenu(player, target);
                }
            }
            // This is a violation item - open the violation menu
            else if (categoryKey != null && violationKey != null) {
                plugin.getPunishmentManager().openViolation(player, categoryKey, violationKey);
            }
            // If we have category but not violation, try to recover
            else if (categoryKey != null) {
                // Try to get violation from session or display name
                plugin.getLogger().warning("Missing violation key in lore. Using session data if available.");
                String sessionCategory = plugin.getPunishmentManager().getSessionCategory(player.getUniqueId());
                plugin.getPunishmentManager().openViolation(player, sessionCategory, violationKey);
            }
            else {
                plugin.getLogger().severe("CRITICAL: Missing punishment data in clicked item!");
                player.sendMessage("§cError: Could not determine punishment information");
            }
        }
        // Handle violation menu (action selection) clicks
        else if (title.contains(" - ") && !title.contains("Confirm")) {
            // Check if this is the back button
            if (itemName.equals("§c§lBack")) {
                // Return to category menu
                if (categoryKey != null) {
                    plugin.getPunishmentManager().openCategory(player, categoryKey);
                } else {
                    // Try to get category from session
                    String sessionCategory = plugin.getPunishmentManager().getSessionCategory(player.getUniqueId());
                    if (sessionCategory != null) {
                        plugin.getPunishmentManager().openCategory(player, sessionCategory);
                    } else {
                        plugin.getLogger().warning("No category found for back button. Returning to main menu.");
                        // If all else fails, just open the main menu
                        String targetName = title.substring(title.indexOf(" - ") + 3);
                        Player target = plugin.getServer().getPlayer(targetName);
                        if (target != null) {
                            plugin.getPunishmentManager().openMenu(player, target);
                        }
                    }
                }
            }
            // Execute punishment if we have all necessary info
            else if (categoryKey != null && violationKey != null && actionName != null) {
                plugin.getLogger().info("Executing punishment with complete data - category: " + categoryKey + ", violation: " + violationKey + ", action: " + actionName);
                plugin.getPunishmentManager().executePunishment(player, categoryKey, violationKey, actionName);
            }
            // Try to get action info from session
            else if (categoryKey != null && violationKey != null) {
                // Use display name as action name if not in lore
                actionName = itemName.replaceAll("§[0-9a-fk-or]", "");
                plugin.getLogger().info("Executing punishment with item name as action - category: " + categoryKey + ", violation: " + violationKey + ", action: " + actionName);
                plugin.getPunishmentManager().executePunishment(player, categoryKey, violationKey, actionName);
            }
            else {
                plugin.getLogger().severe("CRITICAL: Missing punishment data in clicked item!");
                plugin.getLogger().severe("Category: " + categoryKey + ", Violation: " + violationKey + ", Action: " + actionName);
                player.sendMessage("§cError: Could not determine punishment information");
            }
        }
        // Handle confirmation menu clicks
        else if (title.equals(MENU_PREFIX + "Confirm Punishment")) {
            if (itemName.equals("§a§lConfirm")) {
                plugin.getPunishmentManager().handleConfirmation(player, true);
            } else if (itemName.equals("§c§lCancel")) {
                plugin.getPunishmentManager().handleConfirmation(player, false);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        String title = event.getView().getTitle();

        // Ignore non-punishment menus
        if (!title.startsWith(MENU_PREFIX)) return;

        // Don't clear session when closing confirmation menu (will be handled by the confirmation handler)
        if (title.equals(MENU_PREFIX + "Confirm Punishment")) return;

        // Optionally clear the session on menu close
        // plugin.getPunishmentManager().clearSession(player.getUniqueId());
    }
}

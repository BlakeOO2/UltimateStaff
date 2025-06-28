package com.smokypeaks.server.listeners;

import com.smokypeaks.Main;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class PunishmentMenuListener implements Listener {
    private static final String MENU_PREFIX = "§c§l";
    private static final String MAIN_MENU_TITLE = MENU_PREFIX + "Punishment Menu";
    private static final String CONFIRM_MENU_TITLE = MENU_PREFIX + "Confirm Punishment";
    
    private final Main plugin;

    public PunishmentMenuListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();

        if (!title.startsWith(MENU_PREFIX)) return;

        // Always cancel the event for punishment menus
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        plugin.getLogger().info("Punishment menu clicked: " + title + ", item: " + clicked.getType());


        if (title.startsWith(MAIN_MENU_TITLE)) {
            handleMainMenu(player, clicked);
        } else if (title.equals(CONFIRM_MENU_TITLE)) {
            handleConfirmationMenu(player, clicked);
        } else if (title.contains(" - ")) {
            // Handle category or violation menu
            String[] parts = title.substring(MENU_PREFIX.length()).split(" - ", 2);
            String menuType = parts[0];

            if (menuType.equals("Bans") || menuType.equals("Mutes") || menuType.equals("Warns")) {
                // This is a category menu
                handleCategoryMenu(player, menuType.toLowerCase(), clicked);
            } else {
                // This is a violation menu (showing punishment actions)
                handleViolationMenu(player, clicked);
            }
        }
    }

    private void handleConfirmationMenu(Player player, ItemStack clicked) {
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;
        
        String name = meta.getDisplayName();
        if (name.equals("§a§lConfirm")) {
            plugin.getPunishmentManager().handleConfirmation(player, true);
        } else if (name.equals("§c§lCancel")) {
            plugin.getPunishmentManager().handleConfirmation(player, false);
        }
    }

    private void handleMainMenu(Player player, ItemStack clicked) {
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;
        
        String name = meta.getDisplayName();
        if (name.contains("Bans")) {
            plugin.getPunishmentManager().openCategory(player, "bans");
        } else if (name.contains("Mutes")) {
            plugin.getPunishmentManager().openCategory(player, "mutes");
        } else if (name.contains("Warns")) {
            plugin.getPunishmentManager().openCategory(player, "warns");
        } else {
            // Check lore for category key
            if (meta.hasLore() && meta.getLore() != null) {
                for (String loreLine : meta.getLore()) {
                    if (loreLine.contains("Category:")) {
                        String categoryKey = loreLine.substring(loreLine.indexOf("Category: ") + 10).trim();
                        plugin.getPunishmentManager().openCategory(player, categoryKey);
                        break;
                    }
                }
            }
        }
    }

    private void handleCategoryMenu(Player player, String category, ItemStack clicked) {
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;
        
        String name = meta.getDisplayName();
        if (name.contains("Back")) {
            // Get target from active menu
            String targetName = getTargetFromTitle(player);
            if (targetName != null) {
                plugin.getPunishmentManager().openMenu(player, plugin.getServer().getPlayer(targetName));
            }
        } else {
            // This is a violation item, get the violation key from the item
            String violationKey = getViolationKey(name);
            plugin.getPunishmentManager().openViolation(player, category, violationKey);
        }
    }

    private String getTargetFromTitle(Player player) {
        String title = player.getOpenInventory().getTitle();
        if (title.contains(" - ")) {
            String[] parts = title.split(" - ", 2);
            if (parts.length > 1) {
                return parts[1];
            }
        }
        return null;
    }

    private String getViolationKey(String displayName) {
        // Clean up formatting codes and convert to lowercase for key
        return displayName.replaceAll("§[0-9a-fk-or]", "").toLowerCase().replace(" ", "_");
    }

    /**
     * Find the category key from the configuration based on the violation name
     * @param violationName The name of the violation
     * @return The category key if found, or null
     */
    private String findCategoryFromConfig(String violationName) {
        String sanitizedName = violationName.toLowerCase().replaceAll("§[0-9a-fk-or]", "").replace(" ", "_");

        // Search through the config for a matching violation name
        for (String categoryKey : plugin.getConfig().getConfigurationSection("punishments").getKeys(false)) {
            ConfigurationSection categorySection = plugin.getConfig().getConfigurationSection("punishments." + categoryKey + ".violations");
            if (categorySection == null) continue;

            for (String violationKey : categorySection.getKeys(false)) {
                String configViolationName = categorySection.getString(violationKey + ".name", "").toLowerCase().replace(" ", "_");
                if (configViolationName.contains(sanitizedName) || sanitizedName.contains(configViolationName)) {
                    return categoryKey;
                }
            }
        }

        // If we get to this point, try to use the section name that matches most closely
        for (String categoryKey : plugin.getConfig().getConfigurationSection("punishments").getKeys(false)) {
            if (sanitizedName.contains(categoryKey.toLowerCase()) || 
                categoryKey.toLowerCase().contains(sanitizedName)) {
                return categoryKey;
            }
        }

        plugin.getLogger().warning("Could not find category for violation: " + violationName);
        return null;
    }

    private void handleViolationMenu(Player player, ItemStack clicked) {
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        String name = meta.getDisplayName();
        plugin.getLogger().info("Violation menu item clicked: " + name);
        if (name.contains("Back")) {
            // Extract category from the title and go back to that category
            String title = player.getOpenInventory().getTitle();
            String targetName = null;
            String categoryName = null;

            if (title.contains(" - ")) {
                String[] parts = title.substring(MENU_PREFIX.length()).split(" - ", 2);
                if (parts.length > 1) {
                    // The violation name is in parts[0], target in parts[1]
                    targetName = parts[1];

                    // Try to determine category from violation name (hacky but works)
                    // Alternatively, we could store this in player metadata
                    if (title.toLowerCase().contains("ban")) {
                        categoryName = "bans";
                    } else if (title.toLowerCase().contains("mute")) {
                        categoryName = "mutes";
                    } else if (title.toLowerCase().contains("warn")) {
                        categoryName = "warns";
                    }
                }
            }

            if (categoryName != null) {
                plugin.getPunishmentManager().openCategory(player, categoryName);
            } else {
                // Fallback to main menu if we can't determine the category
                Player target = targetName != null ? plugin.getServer().getPlayer(targetName) : null;
                plugin.getPunishmentManager().openMenu(player, target);
            }
        } else {
            // This is a punishment action - extract needed info from title and item metadata
            String title = player.getOpenInventory().getTitle();
            if (title.contains(" - ")) {
                // First, try to extract information from item lore (more reliable)
                String categoryKey = null;
                String violationKey = null;
                String actionName = null;

                if (meta.hasLore()) {
                    for (String loreLine : meta.getLore()) {
                        if (loreLine.contains("Category:")) {
                            categoryKey = loreLine.substring(loreLine.indexOf("Category: ") + 10).trim();
                        } else if (loreLine.contains("Violation:")) {
                            violationKey = loreLine.substring(loreLine.indexOf("Violation: ") + 11).trim();
                        } else if (loreLine.contains("Action:")) {
                            actionName = loreLine.substring(loreLine.indexOf("Action: ") + 8).trim();
                        }
                    }
                }

                // If we couldn't find the info in lore, fall back to title parsing
                if (categoryKey == null || violationKey == null) {
                    String[] parts = title.substring(MENU_PREFIX.length()).split(" - ", 2);
                    if (parts.length > 1) {
                        String violationName = parts[0];
                        String targetName = parts[1];

                        // Try to extract category from active session first
                        if (plugin.getPunishmentManager().getSessionCategory(player.getUniqueId()) != null) {
                            categoryKey = plugin.getPunishmentManager().getSessionCategory(player.getUniqueId());
                        } else {
                            // Extract category from the config structure based on the violation name
                            // Look through all categories to find the matching violation
                            categoryKey = findCategoryFromConfig(violationName);
                        }

                        violationKey = violationName.toLowerCase().replace(" ", "_");
                    }
                }

                // If action name wasn't in lore, use display name
                if (actionName == null) {
                    actionName = name.replaceAll("§[0-9a-fk-or]", "");
                }

                // Now execute the punishment if we have all the required info
                if (categoryKey != null && violationKey != null && actionName != null) {
                    plugin.getLogger().info("Executing punishment: category=" + categoryKey + ", violation=" + violationKey + ", action=" + actionName);
                    plugin.getPunishmentManager().executePunishment(player, categoryKey, violationKey, actionName);
                } else {
                    plugin.getLogger().warning("Missing information for punishment execution: category=" + categoryKey + ", violation=" + violationKey + ", action=" + actionName);
                    player.sendMessage("§cCould not execute punishment: missing information");
                }
                }
            }
        }
    }

// com.smokypeaks.server.managers.PunishmentManager.java
package com.smokypeaks.server.managers;

import com.smokypeaks.Main;
import com.smokypeaks.global.permissions.StaffPermissions;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class PunishmentManager {
    private final Main plugin;
    private final Map<String, PunishmentCategory> categories;
    private final Map<String, String> activeMenus;
    private final Map<String, PendingPunishment> pendingPunishments = new HashMap<>();


    public PunishmentManager(Main plugin) {
        this.plugin = plugin;
        this.categories = new HashMap<>();
        this.activeMenus = new HashMap<>();
        loadPunishments();
    }
    private record PendingPunishment(String category, String actionName, String targetName, String command) {}


    public void loadPunishments() {
        ConfigurationSection config = plugin.getConfig().getConfigurationSection("punishments");
        if (config == null) return;

        for (String categoryName : config.getKeys(false)) {
            ConfigurationSection categorySection = config.getConfigurationSection(categoryName);
            if (categorySection == null) continue;

            List<PunishmentAction> actions = new ArrayList<>();
            for (String key : categorySection.getKeys(false)) {
                ConfigurationSection actionSection = categorySection.getConfigurationSection(key);
                if (actionSection == null) continue;

                actions.add(new PunishmentAction(
                        actionSection.getString("name"),
                        actionSection.getString("duration", ""),
                        actionSection.getString("command"),
                        createItemFromConfig(actionSection.getConfigurationSection("item"))
                ));
            }
            categories.put(categoryName, new PunishmentCategory(categoryName, actions));
        }
    }

    public void openMenu(Player staff, Player target) {
        Inventory menu = Bukkit.createInventory(null, 27, "§c§lPunishment Menu");

        // Load categories from config
        ConfigurationSection categories = plugin.getConfig().getConfigurationSection("punishments.categories");
        if (categories != null) {
            for (String key : categories.getKeys(false)) {
                ConfigurationSection category = categories.getConfigurationSection(key);
                if (category == null) continue;

                ItemStack icon = createCategoryIcon(category);
                int slot = category.getInt("slot", 0);
                menu.setItem(slot, icon);
            }
        }

        if (target != null) {
            ItemStack targetInfo = createTargetInfo(target);
            menu.setItem(4, targetInfo);
            activeMenus.put(staff.getUniqueId().toString(), target.getName());
        }

        staff.openInventory(menu);
    }



    private ItemStack createCategoryIcon(ConfigurationSection category) {
        Material material = Material.valueOf(category.getString("icon", "STONE").toUpperCase());
        String name = category.getString("name", "Unknown");
        List<String> lore = new ArrayList<>();
        lore.add("§7Click to view " + name.toLowerCase() + " options");

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createTargetInfo(Player target) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§e§lTarget: §f" + target.getName());
        List<String> lore = new ArrayList<>();
        lore.add("§7Click a category to");
        lore.add("§7punish this player");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createItemFromConfig(ConfigurationSection config) {
        if (config == null) return new ItemStack(Material.STONE);

        Material material = Material.valueOf(config.getString("material", "STONE").toUpperCase());
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(config.getString("name", "").replace("&", "§"));

        List<String> lore = config.getStringList("lore");
        meta.setLore(lore.stream().map(s -> s.replace("&", "§")).toList());

        item.setItemMeta(meta);
        return item;
    }

    public void executePunishment(Player staff, String category, String actionName) {
        PunishmentCategory cat = categories.get(category.toLowerCase());
        if (cat == null) return;

        String targetName = activeMenus.get(staff.getUniqueId().toString());
        if (targetName == null) return;

        for (PunishmentAction action : cat.actions) {
            if (action.name.equalsIgnoreCase(actionName)) {
                String command = action.command
                        .replace("%player%", targetName)
                        .replace("%duration%", action.duration)
                        .replace("%reason%", action.name);

                // Open confirmation menu instead of executing directly
                openConfirmationMenu(staff, category, actionName, targetName, command);
                break;
            }
        }
    }

    public void openConfirmationMenu(Player staff, String category, String actionName, String targetName, String command) {
        Inventory menu = Bukkit.createInventory(null, 27, "§c§lConfirm Punishment");

        // Target info
        ItemStack targetInfo = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta targetMeta = targetInfo.getItemMeta();
        targetMeta.setDisplayName("§e§lTarget: §f" + targetName);
        targetInfo.setItemMeta(targetMeta);
        menu.setItem(4, targetInfo);

        // Punishment info
        ItemStack punishmentInfo = new ItemStack(Material.PAPER);
        ItemMeta punishMeta = punishmentInfo.getItemMeta();
        punishMeta.setDisplayName("§6§lPunishment Details");
        List<String> punishLore = new ArrayList<>();
        punishLore.add("§7Category: §f" + category);
        punishLore.add("§7Action: §f" + actionName);
        punishLore.add("§7Command: §f" + command);
        punishMeta.setLore(punishLore);
        punishmentInfo.setItemMeta(punishMeta);
        menu.setItem(13, punishmentInfo);

        // Confirm button
        ItemStack confirm = new ItemStack(Material.LIME_DYE);
        ItemMeta confirmMeta = confirm.getItemMeta();
        confirmMeta.setDisplayName("§a§lConfirm");
        confirmMeta.setLore(Arrays.asList("§7Click to confirm punishment"));
        confirm.setItemMeta(confirmMeta);
        menu.setItem(11, confirm);

        // Cancel button
        ItemStack cancel = new ItemStack(Material.RED_DYE);
        ItemMeta cancelMeta = cancel.getItemMeta();
        cancelMeta.setDisplayName("§c§lCancel");
        cancelMeta.setLore(Arrays.asList("§7Click to cancel"));
        cancel.setItemMeta(cancelMeta);
        menu.setItem(15, cancel);

        // Store pending punishment
        pendingPunishments.put(staff.getUniqueId().toString(),
                new PendingPunishment(category, actionName, targetName, command));

        staff.openInventory(menu);
    }

    public void handleConfirmation(Player staff, boolean confirmed) {
        String staffUuid = staff.getUniqueId().toString();
        PendingPunishment pending = pendingPunishments.remove(staffUuid);

        if (pending == null) {
            staff.sendMessage("§cNo pending punishment found!");
            return;
        }

        if (confirmed) {
            // Execute the punishment
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), pending.command());
            staff.sendMessage("§6[Staff] §ePunishment executed on §f" + pending.targetName());

            // Log the punishment
            logPunishment(staff, pending);
        } else {
            staff.sendMessage("§6[Staff] §ePunishment cancelled");
        }

        // Return to main menu
        openMenu(staff, Bukkit.getPlayer(pending.targetName()));
    }

    private void logPunishment(Player staff, PendingPunishment punishment) {
        String logMessage = String.format(
                "[Punishment] %s executed %s:%s on %s",
                staff.getName(),
                punishment.category(),
                punishment.actionName(),
                punishment.targetName()
        );
        plugin.getLogger().info(logMessage);
    }

    private record PunishmentCategory(String name, List<PunishmentAction> actions) {}

    private record PunishmentAction(String name, String duration, String command, ItemStack item) {}

    public void reloadPunishments() {
        categories.clear();
        loadPunishments();
    }
    public void openCategory(Player staff, String category) {
        // Reload punishments to ensure latest config
        reloadPunishments();

        PunishmentCategory cat = categories.get(category.toLowerCase());
        if (cat == null) {
            staff.sendMessage("§cCategory not found!");
            return;
        }

        // Calculate inventory size (multiple of 9)
        int size = Math.min(54, ((cat.actions.size() + 8) / 9) * 9 + 9);
        Inventory menu = Bukkit.createInventory(null, size, "§c§l" + category);

        // Add punishment options
        for (int i = 0; i < cat.actions.size() && i < size - 9; i++) {
            menu.setItem(i, cat.actions.get(i).item);
        }

        // Add back button in the bottom row
        ItemStack backButton = createCategoryButton(Material.BARRIER, "§c§lBack", "§7Return to main menu");
        menu.setItem(size - 1, backButton);

        // Open the menu for the player
        staff.openInventory(menu);
    }
    private ItemStack createCategoryButton(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }



    // Add command to reload punishments

}

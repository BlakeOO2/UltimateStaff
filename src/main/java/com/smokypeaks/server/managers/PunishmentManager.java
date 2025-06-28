package com.smokypeaks.server.managers;

import com.smokypeaks.Main;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class PunishmentManager {
    private final Main plugin;
    private final Map<String, Category> categories;
    private final Map<UUID, MenuSession> activeSessions;
    private final Map<String, PendingPunishment> pendingPunishments;
    private final Map<String, String> activeMenus;

    // Default inventory sizes
    private static final int DEFAULT_CATEGORY_SIZE = 27; // 3 rows
    private static final int DEFAULT_VIOLATION_SIZE = 36; // 4 rows
    private static final int DEFAULT_ACTION_SIZE = 36;    // 4 rows
    private static final int CONFIRM_MENU_SIZE = 27;       // 3 rows

    public PunishmentManager(Main plugin) {
        this.plugin = plugin;
        this.categories = new HashMap<>();
        this.activeSessions = new HashMap<>();
        this.pendingPunishments = new HashMap<>();
        this.activeMenus = new HashMap<>();
        loadPunishments();
    }

    // Track session state for each player using punishment menus
    private static class MenuSession {
        private final UUID targetUUID;
        private final String targetName;
        private String currentCategory;
        private String currentViolation;

        public MenuSession(Player target) {
            this.targetUUID = target.getUniqueId();
            this.targetName = target.getName();
        }

        public UUID getTargetUUID() {
            return targetUUID;
        }

        public String getTargetName() {
            return targetName;
        }

        public String getCurrentCategory() {
            return currentCategory;
        }

        public void setCurrentCategory(String category) {
            this.currentCategory = category;
        }

        public String getCurrentViolation() {
            return currentViolation;
        }

        public void setCurrentViolation(String violation) {
            this.currentViolation = violation;
        }
    }

    private record PendingPunishment(String category, String violation, String actionName, String targetName, String command) {}
    private record Category(String name, Material icon, int slot, Map<String, Violation> violations) {}
    private record Violation(String name, ItemStack item, List<PunishmentAction> actions) {}
    private record PunishmentAction(String name, String duration, String command, ItemStack item) {}

    public void loadPunishments() {
        try {
            categories.clear();
            ConfigurationSection config = plugin.getConfig().getConfigurationSection("punishments");
            if (config == null) {
                plugin.getLogger().warning("No 'punishments' section found in config.yml");
                return;
            }

            for (String categoryKey : config.getKeys(false)) {
                ConfigurationSection categorySection = config.getConfigurationSection(categoryKey);
                if (categorySection == null) continue;

                String categoryName = categorySection.getString("name", categoryKey);
                Material icon;
                try {
                    String iconStr = categorySection.getString("icon", "PAPER");
                    icon = Material.valueOf(iconStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid material for category " + categoryKey + ": " + e.getMessage());
                    icon = Material.PAPER;
                }

                int slot = categorySection.getInt("slot", -1);

                Map<String, Violation> violations = new HashMap<>();
                ConfigurationSection violationsSection = categorySection.getConfigurationSection("violations");

                if (violationsSection != null) {
                    for (String violationKey : violationsSection.getKeys(false)) {
                        ConfigurationSection violationSection = violationsSection.getConfigurationSection(violationKey);
                        if (violationSection == null) continue;

                        String violationName = violationSection.getString("name", violationKey);
                        ItemStack violationItem = createItemFromConfig(violationSection.getConfigurationSection("item"));
                        List<PunishmentAction> actions = loadActions(violationSection.getConfigurationSection("actions"));

                        violations.put(violationKey, new Violation(violationName, violationItem, actions));
                    }
                }

                categories.put(categoryKey, new Category(categoryName, icon, slot, violations));
                plugin.getLogger().info("Loaded punishment category: " + categoryName + " with " + violations.size() + " violations");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error loading punishments: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private List<PunishmentAction> loadActions(ConfigurationSection actionsSection) {
        List<PunishmentAction> actions = new ArrayList<>();
        if (actionsSection == null) return actions;

        try {
            for (String key : actionsSection.getKeys(false)) {
                String name = actionsSection.getString(key + ".name", "Unknown");
                String duration = actionsSection.getString(key + ".duration", "");
                String command = actionsSection.getString(key + ".command", "");

                Material material = Material.PAPER;
                try {
                    String materialStr = actionsSection.getString(key + ".item.material");
                    if (materialStr != null) {
                        material = Material.valueOf(materialStr.toUpperCase());
                    }
                } catch (IllegalArgumentException e) {
                    // Fallback to default
                }

                ItemStack item = new ItemStack(material);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("§e§l" + name);
                    List<String> lore = new ArrayList<>();
                    if (!duration.isEmpty()) {
                        lore.add("§7Duration: §f" + duration);
                    }
                    lore.add("§7Click to execute");
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }

                actions.add(new PunishmentAction(name, duration, command, item));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error loading punishment actions: " + e.getMessage());
        }

        return actions;
    }

                /**
                 * Opens the main punishment menu for a staff member to punish a target player
                 * @param staff The staff member using the punishment system
                 * @param target The player to be potentially punished
                 */
                public void openMenu(Player staff, Player target) {
        try {
            if (target == null) {
                staff.sendMessage("§cError: Target player is null");
                return;
            }

            // Create a new session for this staff member
            activeSessions.put(staff.getUniqueId(), new MenuSession(target));

            // Calculate inventory size - min 3 rows, max 6 rows
            int itemCount = categories.size();
            int rows = Math.max(3, Math.min(6, (int) Math.ceil(itemCount / 7.0) + 1)); // +1 for padding/navigation
            int size = rows * 9;

            // Create inventory
            Inventory menu = Bukkit.createInventory(null, size, "§c§lPunishment Menu - " + target.getName());

            // First, add the target player head in the center top row
            ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta meta = playerHead.getItemMeta();
            if (meta instanceof SkullMeta skullMeta) {
                skullMeta.setOwningPlayer(target);
            }
            meta.setDisplayName("§f" + target.getName());
            List<String> lore = new ArrayList<>();
            lore.add("§7Select a punishment category below");
            meta.setLore(lore);
            playerHead.setItemMeta(meta);
            menu.setItem(4, playerHead);

            // Add category buttons
            for (Map.Entry<String, Category> entry : categories.entrySet()) {
                String key = entry.getKey();
                Category category = entry.getValue();

                ItemStack item = new ItemStack(category.icon);
                ItemMeta itemMeta = item.getItemMeta();
                if (itemMeta != null) {
                    itemMeta.setDisplayName("§6§l" + category.name);
                    List<String> itemLore = new ArrayList<>();
                    itemLore.add("§7Click to view violations");
                    itemLore.add("§8Category: " + key); // Store category key for retrieval
                    itemMeta.setLore(itemLore);
                    item.setItemMeta(itemMeta);
                }

                // Calculate positions - place items in middle rows starting from slot 18
                int slot;
                if (category.slot >= 0 && category.slot < size && !isReservedSlot(category.slot)) {
                    slot = category.slot;
                } else {
                    // Find next available slot starting from middle rows
                    slot = findNextAvailableSlot(menu, 18);
                }

                if (slot < size) {
                    menu.setItem(slot, item);
                } else {
                    plugin.getLogger().warning("Not enough space in menu for category: " + category.name);
                }
            }

            activeMenus.put(staff.getUniqueId().toString(), target.getName());
            staff.openInventory(menu);
        } catch (Exception e) {
            plugin.getLogger().severe("Error opening punishment menu: " + e.getMessage());
            e.printStackTrace();
            staff.sendMessage("§cAn error occurred while opening the menu");
        }
    }

    public void openCategory(Player staff, String categoryKey) {
        Category category = categories.get(categoryKey);
        if (category == null) {
            staff.sendMessage("§cCategory not found!");
            return;
        }

        String targetName = activeMenus.get(staff.getUniqueId().toString());
        if (targetName == null) {
            staff.sendMessage("§cNo target selected!");
            return;
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            staff.sendMessage("§cTarget player is offline!");
            activeMenus.remove(staff.getUniqueId().toString());
            return;
        }

        int rows = (int) Math.ceil(category.violations.size() / 9.0) + 1;
        int size = Math.min(6, rows) * 9;
        Inventory menu = Bukkit.createInventory(null, size, 
            "§c§l" + category.name + " - " + targetName);

        // Add violation buttons
        category.violations.forEach((key, violation) -> {
            int slot = menu.firstEmpty();
            if (slot >= 0 && slot < size - 9) { // Leave bottom row for navigation
                menu.setItem(slot, violation.item);
            }
        });

        // Add back button
        ItemStack backButton = createButton(Material.BARRIER, "§c§lBack", "§7Return to main menu");
        menu.setItem(size - 5, backButton);

        staff.openInventory(menu);
    }

    public void openViolation(Player staff, String categoryKey, String violationKey) {
        Category category = categories.get(categoryKey);
        if (category == null) {
            staff.sendMessage("§cCategory not found!");
            return;
        }

        Violation violation = category.violations.get(violationKey);
        if (violation == null) {
            staff.sendMessage("§cViolation not found!");
            return;
        }

        String targetName = activeMenus.get(staff.getUniqueId().toString());
        if (targetName == null) {
            staff.sendMessage("§cNo target selected!");
            return;
        }

        // Check if target is still online
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            staff.sendMessage("§cTarget player is offline!");
            activeMenus.remove(staff.getUniqueId().toString());
            return;
        }

        int rows = (int) Math.ceil(violation.actions.size() / 9.0) + 1;
        int size = Math.min(6, rows) * 9;
        Inventory menu = Bukkit.createInventory(null, size, 
            "§c§l" + violation.name + " - " + targetName);

        // Add action buttons
        for (int i = 0; i < violation.actions.size(); i++) {
            if (i >= 0 && i < size - 9) { // Leave bottom row for navigation
                menu.setItem(i, violation.actions.get(i).item);
            }
        }

        // Add back button
        ItemStack backButton = createButton(Material.BARRIER, "§c§lBack", "§7Return to category");
        menu.setItem(size - 5, backButton);

        staff.openInventory(menu);

    }

    public void executePunishment(Player staff, String categoryKey, String violationKey, String actionName) {
        Category category = categories.get(categoryKey);
        if (category == null) return;

        Violation violation = category.violations.get(violationKey);
        if (violation == null) return;

        String targetName = activeMenus.get(staff.getUniqueId().toString());
        if (targetName == null) return;

        for (PunishmentAction action : violation.actions) {
            if (action.name.equalsIgnoreCase(actionName)) {
                String command = action.command
                    .replace("%player%", targetName)
                    .replace("%duration%", action.duration);

                openConfirmationMenu(staff, categoryKey, violationKey, actionName, targetName, command);
                break;
            }
        }
    }

    public void openConfirmationMenu(Player staff, String category, String violation, 
                                   String actionName, String targetName, String command) {
        Inventory menu = Bukkit.createInventory(null, 27, "§c§lConfirm Punishment");

        // Target info
        ItemStack targetInfo = createButton(Material.PLAYER_HEAD, 
            "§e§lTarget: §f" + targetName, "§7Will be punished");
        menu.setItem(4, targetInfo);

        // Punishment info
        ItemStack punishmentInfo = createButton(Material.PAPER, "§6§lPunishment Details",
            "§7Category: §f" + category,
            "§7Violation: §f" + violation,
            "§7Action: §f" + actionName,
            "§7Command: §f" + command);
        menu.setItem(13, punishmentInfo);

        // Confirm button
        ItemStack confirm = createButton(Material.LIME_DYE, "§a§lConfirm", "§7Click to execute punishment");
        menu.setItem(11, confirm);

        // Cancel button
        ItemStack cancel = createButton(Material.RED_DYE, "§c§lCancel", "§7Click to cancel");
        menu.setItem(15, cancel);

                    pendingPunishments.put(staff.getUniqueId().toString(),
            new PendingPunishment(category, violation, actionName, targetName, command));

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
            logPunishment(staff, pending);
        } else {
            staff.sendMessage("§6[Staff] §ePunishment cancelled");
        }

        // Return to main menu
        openMenu(staff, Bukkit.getPlayer(pending.targetName()));
    }

    private void logPunishment(Player staff, PendingPunishment punishment) {
        String logMessage = String.format(
            "[Punishment] %s executed %s:%s:%s on %s",
            staff.getName(),
            punishment.category(),
            punishment.violation(),
            punishment.actionName(),
            punishment.targetName()
        );
        plugin.getLogger().info(logMessage);
    }

    private ItemStack createButton(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

            private boolean isReservedSlot(int slot) {
        // Reserved slots: top row (0-8) for header/info items
        if (slot < 9) return true;

        // Bottom row reserved for navigation - use inventory size to determine bottom row
        // We don't know the exact inventory size here, so we'll use a generic approach
        int inventorySize = 27; // Default size, minimum 3 rows
        int bottomRowStart = inventorySize - 9;
        if (slot >= bottomRowStart && slot < inventorySize) return true;

        // Center slot in top row (slot 4) reserved for player head
        return slot == 4;
            }

            private int findNextAvailableSlot(Inventory inventory, int startFrom) {
        for (int i = startFrom; i < inventory.getSize(); i++) {
            if (!isReservedSlot(i) && inventory.getItem(i) == null) {
                return i;
            }
        }

        // If we couldn't find a slot in the preferred range, look through the whole inventory
        for (int i = 0; i < inventory.getSize(); i++) {
            if (i != 4 && inventory.getItem(i) == null) { // Always keep slot 4 (player head) reserved
                return i;
            }
        }

        // If inventory is completely full, return the first non-critical slot
        return 9; // First slot in second row
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

    public void reloadPunishments() {
        loadPunishments();
    }
}
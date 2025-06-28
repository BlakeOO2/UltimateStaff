// com.smokypeaks.server.utils.StaffItems.java
package com.smokypeaks.server.utils;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class StaffItems {
    public static ItemStack createVanishItem(boolean vanished) {
        return createItem(
                vanished ? Material.LIME_DYE : Material.GRAY_DYE,
                "§6Vanish: " + (vanished ? "§aEnabled" : "§cDisabled"),
                "§7Click to toggle vanish"
        );
    }

    public static ItemStack createSpectatorItem(boolean spectator) {
        return createItem(
                spectator ? Material.ENDER_EYE : Material.ENDER_PEARL,
                "§5Spectator Mode: " + (spectator ? "§aEnabled" : "§cDisabled"),
                "§7Click to toggle between spectator and survival"
        );
    }

    public static ItemStack createFreezeItem() {
        return createItem(
                Material.PACKED_ICE,
                "§bFreeze Player",
                "§7Click a player to freeze them"
        );
    }
    public static ItemStack createEnderChestItem() {
        return createItem(
                Material.ENDER_CHEST,
                "§5Enderchest Inspector",
                "§7Click a player to view their enderchest"
        );
    }

    public static ItemStack createPunishmentTool() {
        return createItem(
                Material.BLAZE_ROD,
                "§cPunishment Tool",
                "§7Click a player to open punishment menu",
                "§7or right-click air to open general menu"
        );
    }

    public static ItemStack createInvseeItem() {
        return createItem(
                Material.CHEST,
                "§eInventory Inspector",
                "§7Click a player to view their inventory"
        );
    }

    public static ItemStack createRandomTpItem() {
        return createItem(
                Material.ENDER_PEARL,
                "§dRandom Teleport",
                "§7Click to teleport to a random player"
        );
    }

    public static ItemStack createStaffListItem() {
        return createItem(
                Material.BOOK,
                "§6Online Staff",
                "§7Click to view online staff"
        );
    }

    private static ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        meta.addEnchant(Enchantment.DURABILITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }
    // Add identification methods
    public static boolean isVanishTool(ItemStack item) {
        return isTool(item, "Vanish:");
    }

    public static boolean isSpectatorTool(ItemStack item) {
        return isTool(item, "Spectator Mode:");
    }

    public static boolean isFreezeTool(ItemStack item) {
        return isTool(item, "Freeze Player");
    }

    public static boolean isInvseeTool(ItemStack item) {
        return isTool(item, "Inventory Inspector");
    }

    public static boolean isEnderseeTool(ItemStack item) {
        return isTool(item, "Enderchest Inspector");
    }

    public static boolean isRandomTpTool(ItemStack item) {
        return isTool(item, "Random Teleport");
    }

    public static boolean isStaffListTool(ItemStack item) {
        return isTool(item, "Online Staff");
    }

    public static boolean isPunishmentTool(ItemStack item) {
        return isTool(item, "Punishment Tool");
    }

    public static boolean isStaffTool(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return false;
        }
        return item.getItemMeta().hasEnchant(Enchantment.DURABILITY);
    }
    // Add to StaffItems.java
    public static ItemStack createFreezeTool() {
        return createItem(
                Material.ICE,
                "§bFreeze Player",
                "§7Click a player to freeze/unfreeze them"
        );
    }


    private static boolean isTool(ItemStack item, String nameContains) {
        return item != null &&
                item.hasItemMeta() &&
                item.getItemMeta().hasDisplayName() &&
                item.getItemMeta().getDisplayName().contains(nameContains);
    }
}

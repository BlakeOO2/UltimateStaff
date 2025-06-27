// com.smokypeaks.server.utils.StaffTools.java
package com.smokypeaks.server.utils;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class StaffTools {
    public static ItemStack createVanishTool() {
        return createTool(Material.LIME_DYE, "§aVanish Tool", "§7Click to toggle vanish");
    }

    public static ItemStack createFreezeTool() {
        return createTool(Material.PACKED_ICE, "§bFreeze Tool", "§7Click a player to freeze them");
    }

    public static ItemStack createInvSeeTool() {
        return createTool(Material.CHEST, "§6Inventory Inspector", "§7Click a player to view their inventory");
    }

    public static ItemStack createEnderSeeTool() {
        return createTool(Material.ENDER_CHEST, "§5Enderchest Inspector", "§7Click a player to view their enderchest");
    }

    public static ItemStack createRandomTpTool() {
        return createTool(Material.ENDER_PEARL, "§dRandom Teleport", "§7Click to teleport to a random player");
    }

    private static ItemStack createTool(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        meta.addEnchant(Enchantment.DURABILITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }
}

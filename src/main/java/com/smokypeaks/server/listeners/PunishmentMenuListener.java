// com.smokypeaks.server.listeners.PunishmentMenuListener.java
package com.smokypeaks.server.listeners;

import com.smokypeaks.Main;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class PunishmentMenuListener implements Listener {
    private final Main plugin;

    public PunishmentMenuListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();

        if (!title.contains("§c§l")) return;
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;

        if (title.equals("§c§lPunishment Menu")) {
            handleMainMenu(player, clicked);
        } else if (title.equals("§c§lConfirm Punishment")) {
            handleConfirmationMenu(player, clicked);
        } else {
            handleCategoryMenu(player, title.replace("§c§l", ""), clicked);
        }
    }

    private void handleConfirmationMenu(Player player, ItemStack clicked) {
        String name = clicked.getItemMeta().getDisplayName();
        if (name.equals("§a§lConfirm")) {
            plugin.getPunishmentManager().handleConfirmation(player, true);
        } else if (name.equals("§c§lCancel")) {
            plugin.getPunishmentManager().handleConfirmation(player, false);
        }
    }

    private void handleMainMenu(Player player, ItemStack clicked) {
        String name = clicked.getItemMeta().getDisplayName();
        if (name.contains("Bans")) {
            plugin.getPunishmentManager().openCategory(player, "bans");
        } else if (name.contains("Mutes")) {
            plugin.getPunishmentManager().openCategory(player, "mutes");
        } else if (name.contains("Warns")) {
            plugin.getPunishmentManager().openCategory(player, "warns");
        }
    }

    private void handleCategoryMenu(Player player, String category, ItemStack clicked) {
        String name = clicked.getItemMeta().getDisplayName();
        if (name.contains("Back")) {
            plugin.getPunishmentManager().openMenu(player, null);
        } else {
            plugin.getPunishmentManager().executePunishment(player, category,
                    name.replace("§c§l", "").replace("§e§l", "").replace("§a§l", ""));
        }
    }
}

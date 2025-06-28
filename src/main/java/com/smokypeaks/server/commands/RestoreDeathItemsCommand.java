package com.smokypeaks.server.commands;

import com.smokypeaks.Main;
import com.smokypeaks.global.permissions.StaffPermissions;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class RestoreDeathItemsCommand implements CommandExecutor, TabCompleter {
    private final Main plugin;

    public RestoreDeathItemsCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check permission
        if (!sender.hasPermission(StaffPermissions.Admin.RESTORE_ITEMS)) {
            sender.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        // Check args
        if (args.length == 0) {
            sender.sendMessage("§cUsage: /restoredeathitems <player>");
            return true;
        }

        // Find the target player
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found!");
            return true;
        }

        // Check if there are death items for this player
        if (!plugin.getDeathManager().hasDeathItems(target.getUniqueId())) {
            sender.sendMessage("§cNo death items found for " + target.getName());
            return true;
        }

        // Get the death items
        ItemStack[] deathItems = plugin.getDeathManager().getDeathItems(target.getUniqueId());
        if (deathItems == null || deathItems.length == 0) {
            sender.sendMessage("§cNo death items found for " + target.getName());
            return true;
        }

        // Give the items to the player
        restoreItems(target, deathItems);

        // Clear the death items from storage
        plugin.getDeathManager().clearDeathItems(target.getUniqueId());

        // Send messages
        sender.sendMessage("§6[Staff] §eRestored death items for " + target.getName());
        target.sendMessage("§6[Staff] §eYour items from your last death have been restored by an administrator.");

        // Log the action
        String staffName = sender instanceof Player ? ((Player) sender).getName() : "Console";
        plugin.getLogger().info(staffName + " restored death items for " + target.getName());

        return true;
    }

    /**
     * Restores items to a player's inventory, dropping any that don't fit
     * @param player The player to restore items to
     * @param items The items to restore
     */
    private void restoreItems(Player player, ItemStack[] items) {
        for (ItemStack item : items) {
            if (item != null) {
                // Try to add to inventory, drop if it doesn't fit
                if (player.getInventory().firstEmpty() == -1) {
                    // Inventory is full, drop at player's feet
                    player.getWorld().dropItem(player.getLocation(), item);
                } else {
                    player.getInventory().addItem(item);
                }
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> playerNames = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                playerNames.add(player.getName());
            }
            return playerNames;
        }
        return new ArrayList<>();
    }
}

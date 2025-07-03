// com.smokypeaks.server.commands.InvseeCommand.java
package com.smokypeaks.server.commands;

import com.smokypeaks.Main;
import com.smokypeaks.global.permissions.StaffPermissions;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class InvseeCommand implements CommandExecutor {
    private final Main plugin;

    public InvseeCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player staff)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        if (!staff.hasPermission(StaffPermissions.Staff.INVSEE)) {
            staff.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        if (args.length < 1) {
            staff.sendMessage("§cUsage: /" + label + " <player> [enderchest]");
            return true;
        }

        String targetName = args[0];
        boolean enderchest = args.length > 1 && args[1].equalsIgnoreCase("enderchest");

        Player targetPlayer = Bukkit.getPlayer(targetName);
        if (targetPlayer != null) {
            if (enderchest) {
                plugin.getInventoryManager().openEnderChest(staff, targetPlayer);
            } else {
                plugin.getInventoryManager().openInventory(staff, targetPlayer);
            }
        } else {
            OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
            if (offlineTarget.hasPlayedBefore()) {
                if (enderchest) {
                    plugin.getInventoryManager().openOfflineEnderChest(staff, offlineTarget);
                    staff.sendMessage("§a[Staff] §eOpened offline enderchest inventory for §f" + targetName);
                } else {
                    plugin.getInventoryManager().openOfflineInventory(staff, offlineTarget);
                    staff.sendMessage("§a[Staff] §eOpened offline inventory for §f" + targetName);
                }
            } else {
                staff.sendMessage("§c[Staff] §ePlayer not found or has never played before!");
            }
        }

        return true;
    }
}

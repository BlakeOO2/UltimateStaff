// com.smokypeaks.server.commands.EnderseeCommand.java
package com.smokypeaks.server.commands;

import com.smokypeaks.Main;
import com.smokypeaks.global.permissions.StaffPermissions;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EnderseeCommand implements CommandExecutor {
    private final Main plugin;

    public EnderseeCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player staff)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        if (!staff.hasPermission(StaffPermissions.Staff.ENDERSEE)) {
            staff.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        if (args.length < 1) {
            staff.sendMessage("§cUsage: /" + label + " <player>");
            return true;
        }

        String targetName = args[0];
        Player targetPlayer = Bukkit.getPlayer(targetName);

        if (targetPlayer != null) {
            plugin.getInventoryManager().openEnderChest(staff, targetPlayer);
        } else {
            OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
            if (offlineTarget.hasPlayedBefore()) {
                plugin.getInventoryManager().openOfflineEnderChest(staff, offlineTarget);
            } else {
                staff.sendMessage("§c[Staff] §ePlayer not found!");
            }
        }

        return true;
    }
}

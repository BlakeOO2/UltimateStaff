// com.smokypeaks.server.commands.StaffModeCommand.java
package com.smokypeaks.server.commands;

import com.smokypeaks.Main;
import com.smokypeaks.global.permissions.StaffPermissions;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StaffModeCommand implements CommandExecutor {
    private final Main plugin;

    public StaffModeCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        if (!player.hasPermission(StaffPermissions.Staff.MODE)) {
            player.sendMessage("§cYou don't have permission to use staff mode!");
            return true;
        }

        plugin.getStaffModeManager().toggleStaffMode(player);
        return true;
    }
}

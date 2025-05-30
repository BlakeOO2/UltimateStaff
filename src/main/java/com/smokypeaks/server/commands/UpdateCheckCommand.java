// com.smokypeaks.server.commands.UpdateCheckCommand.java
package com.smokypeaks.server.commands;

import com.smokypeaks.Main;
import com.smokypeaks.global.permissions.StaffPermissions;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class UpdateCheckCommand implements CommandExecutor {
    private final Main plugin;

    public UpdateCheckCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(StaffPermissions.Admin.UPDATE_NOTIFY)) {
            sender.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        sender.sendMessage("§6[UltimateStaff] §eChecking for updates...");
        plugin.getUpdateChecker().checkForUpdates(true);
        return true;
    }
}

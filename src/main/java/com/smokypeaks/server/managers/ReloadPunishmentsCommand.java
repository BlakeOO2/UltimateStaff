package com.smokypeaks.server.managers;


import com.smokypeaks.Main;
import com.smokypeaks.global.permissions.StaffPermissions;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadPunishmentsCommand implements CommandExecutor {
    private final Main plugin;

    public ReloadPunishmentsCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(StaffPermissions.Admin.RELOAD)) {
            sender.sendMessage("§cYou don't have permission to reload punishments!");
            return true;
        }

        plugin.reloadConfig();
        plugin.getPunishmentManager().reloadPunishments();
        sender.sendMessage("§aReloaded punishment configurations!");
        return true;
    }
}

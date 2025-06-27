// com.smokypeaks.bungee.commands.ReloadCommand.java
package com.smokypeaks.bungee.commands;

import com.smokypeaks.bungee.BungeeMain;
import com.smokypeaks.global.permissions.StaffPermissions;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

public class ReloadCommand extends Command {
    private final BungeeMain plugin;

    public ReloadCommand(BungeeMain plugin) {
        super("ultimatestaffreload", StaffPermissions.Admin.RELOAD, "usreload");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission(StaffPermissions.Admin.RELOAD)) {
            sender.sendMessage("§cYou don't have permission to use this command!");
            return;
        }

        sender.sendMessage("§6[UltimateStaff] §eReloading configuration...");
        plugin.reloadConfig();
        sender.sendMessage("§6[UltimateStaff] §aConfiguration reloaded successfully!");
    }
}

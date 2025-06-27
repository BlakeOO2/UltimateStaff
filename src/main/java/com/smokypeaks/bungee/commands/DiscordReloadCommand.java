// com.smokypeaks.bungee.commands.DiscordReloadCommand.java
package com.smokypeaks.bungee.commands;

import com.smokypeaks.bungee.BungeeMain;
import com.smokypeaks.bungee.discord.DiscordManager;
import com.smokypeaks.global.permissions.StaffPermissions;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

public class DiscordReloadCommand extends Command {
    private final BungeeMain plugin;

    public DiscordReloadCommand(BungeeMain plugin) {
        super("discordreload", StaffPermissions.Admin.DISCORD);
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission(StaffPermissions.Admin.DISCORD)) {
            sender.sendMessage("§cYou don't have permission to manage Discord integration!");
            return;
        }

        plugin.getSettingsManager().reloadConfig();
        plugin.getDiscordManager().shutdown();
        plugin.setDiscordManager(new DiscordManager(plugin));

        sender.sendMessage("§6[UltimateStaff] §eDiscord integration reloaded!");
    }
}

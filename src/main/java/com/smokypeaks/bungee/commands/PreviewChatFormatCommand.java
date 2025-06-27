// Add this command to show format previews
package com.smokypeaks.bungee.commands;

import com.smokypeaks.bungee.BungeeMain;
import com.smokypeaks.global.permissions.StaffPermissions;
import net.luckperms.api.model.user.User;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class PreviewChatFormatCommand extends Command {
    private final BungeeMain plugin;

    public PreviewChatFormatCommand(BungeeMain plugin) {
        super("chatpreview", StaffPermissions.Admin.CHAT);
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return;
        }

        User user = plugin.getLuckPerms().getUserManager().getUser(player.getUniqueId());
        if (user == null) return;

        String groupName = user.getPrimaryGroup();
        String serverName = player.getServer().getInfo().getName();
        String prefix = user.getCachedData().getMetaData().getPrefix();
        prefix = prefix != null ? prefix : "";

        // Preview Staff Chat format
        String staffFormat = plugin.getSettingsManager().getFormat("chat.staff", groupName)
                .replace("%server%", serverName)
                .replace("%prefix%", prefix)
                .replace("%name%", player.getName())
                .replace("%message%", "Example staff message")
                .replace("&", "§");

        // Preview Admin Chat format
        String adminFormat = plugin.getSettingsManager().getFormat("chat.admin", groupName)
                .replace("%server%", serverName)
                .replace("%prefix%", prefix)
                .replace("%name%", player.getName())
                .replace("%message%", "Example admin message")
                .replace("&", "§");

        player.sendMessage("§6Your chat formats:");
        player.sendMessage("§eStaff Chat: " + staffFormat);
        player.sendMessage("§eAdmin Chat: " + adminFormat);
    }
}

// com.smokypeaks.bungee.commands.ToggleChatSoundCommand.java
package com.smokypeaks.bungee.commands;

import com.smokypeaks.bungee.BungeeMain;
import com.smokypeaks.global.permissions.StaffPermissions;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class ToggleChatSoundCommand extends Command {
    private final BungeeMain plugin;
    private final String chatType;

    public ToggleChatSoundCommand(BungeeMain plugin, String chatType) {
        super(chatType + "sound",
                chatType.equals("staff") ? StaffPermissions.Staff.CHAT : StaffPermissions.Admin.CHAT);
        this.plugin = plugin;
        this.chatType = chatType;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return;
        }

        plugin.getSettingsManager().toggleSound(player.getUniqueId(), chatType);
        boolean enabled = plugin.getSettingsManager().isSoundEnabled(player.getUniqueId(), chatType);

        player.sendMessage(String.format("§6[UltimateStaff] §e%s chat sounds %s",
                chatType.substring(0, 1).toUpperCase() + chatType.substring(1),
                enabled ? "§aenabled" : "§cdisabled"));
    }
}

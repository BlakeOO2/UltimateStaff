// com.smokypeaks.bungee.listeners.StaffChatListener.java
package com.smokypeaks.bungee.listeners;

import com.smokypeaks.bungee.BungeeMain;
import com.smokypeaks.bungee.commands.StaffChatCommand;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class StaffChatListener implements Listener {
    private final BungeeMain plugin;
    private final StaffChatCommand staffChatCommand;

    public StaffChatListener(BungeeMain plugin, StaffChatCommand staffChatCommand) {
        this.plugin = plugin;
        this.staffChatCommand = staffChatCommand;
    }

    @EventHandler
    public void onChat(ChatEvent event) {
        if (event.isCancelled() || event.isCommand()) return;
        if (!(event.getSender() instanceof ProxiedPlayer player)) return;

        if (staffChatCommand.isInStaffChat(player.getUniqueId())) {
            event.setCancelled(true);
            staffChatCommand.broadcastStaffMessage(player, event.getMessage());
        }
    }
}
